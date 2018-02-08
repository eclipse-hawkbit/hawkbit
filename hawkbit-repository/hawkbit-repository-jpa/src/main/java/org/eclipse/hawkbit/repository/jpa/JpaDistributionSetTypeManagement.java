/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetTypeSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link DistributionSetTypeManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaDistributionSetTypeManagement implements DistributionSetTypeManagement {

    private final DistributionSetTypeRepository distributionSetTypeRepository;

    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;

    private final DistributionSetRepository distributionSetRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final NoCountPagingRepository criteriaNoCountDao;

    JpaDistributionSetTypeManagement(final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final DistributionSetRepository distributionSetRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final NoCountPagingRepository criteriaNoCountDao) {
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.distributionSetRepository = distributionSetRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.criteriaNoCountDao = criteriaNoCountDao;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType update(final DistributionSetTypeUpdate u) {
        final GenericDistributionSetTypeUpdate update = (GenericDistributionSetTypeUpdate) u;

        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(update.getId());

        update.getDescription().ifPresent(type::setDescription);
        update.getColour().ifPresent(type::setColour);

        if (hasModules(update)) {
            checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(update.getId());

            update.getMandatory().ifPresent(
                    mand -> softwareModuleTypeRepository.findAll(mand).forEach(type::addMandatoryModuleType));
            update.getOptional()
                    .ifPresent(opt -> softwareModuleTypeRepository.findAll(opt).forEach(type::addOptionalModuleType));
        }

        return distributionSetTypeRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType assignMandatorySoftwareModuleTypes(final long dsTypeId,
            final Collection<Long> softwareModulesTypeIds) {
        final Collection<JpaSoftwareModuleType> modules = softwareModuleTypeRepository.findAll(softwareModulesTypeIds);

        if (modules.size() < softwareModulesTypeIds.size()) {
            throw new EntityNotFoundException(SoftwareModuleType.class, softwareModulesTypeIds,
                    modules.stream().map(SoftwareModuleType::getId).collect(Collectors.toList()));
        }

        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);
        checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(dsTypeId);

        modules.forEach(type::addMandatoryModuleType);

        return distributionSetTypeRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType assignOptionalSoftwareModuleTypes(final long dsTypeId,
            final Collection<Long> softwareModulesTypeIds) {

        final Collection<JpaSoftwareModuleType> modules = softwareModuleTypeRepository.findAll(softwareModulesTypeIds);

        if (modules.size() < softwareModulesTypeIds.size()) {
            throw new EntityNotFoundException(SoftwareModuleType.class, softwareModulesTypeIds,
                    modules.stream().map(SoftwareModuleType::getId).collect(Collectors.toList()));
        }

        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);
        checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(dsTypeId);
        modules.forEach(type::addOptionalModuleType);

        return distributionSetTypeRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType unassignSoftwareModuleType(final long dsTypeId, final long softwareModuleTypeId) {
        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);

        checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(dsTypeId);

        type.removeModuleType(softwareModuleTypeId);

        return distributionSetTypeRepository.save(type);
    }

    @Override
    public Page<DistributionSetType> findByRsql(final Pageable pageable, final String rsqlParam) {
        return convertPage(findByCriteriaAPI(pageable,
                Arrays.asList(RSQLUtility.parse(rsqlParam, DistributionSetTypeFields.class, virtualPropertyReplacer),
                        DistributionSetTypeSpecification.isDeleted(false))),
                pageable);
    }

    @Override
    public Slice<DistributionSetType> findAll(final Pageable pageable) {
        return convertPage(criteriaNoCountDao.findAll(DistributionSetTypeSpecification.isDeleted(false), pageable,
                JpaDistributionSetType.class), pageable);
    }

    @Override
    public long count() {
        return distributionSetTypeRepository.countByDeleted(false);
    }

    @Override
    public Optional<DistributionSetType> getByName(final String name) {
        return Optional
                .ofNullable(distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byName(name)));
    }

    @Override
    public Optional<DistributionSetType> getByKey(final String key) {
        return Optional.ofNullable(distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byKey(key)));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType create(final DistributionSetTypeCreate c) {
        final JpaDistributionSetTypeCreate create = (JpaDistributionSetTypeCreate) c;

        return distributionSetTypeRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long typeId) {

        final JpaDistributionSetType toDelete = distributionSetTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, typeId));

        if (distributionSetRepository.countByTypeId(typeId) > 0) {
            toDelete.setDeleted(true);
            distributionSetTypeRepository.save(toDelete);
        } else {
            distributionSetTypeRepository.delete(typeId);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSetType> create(final Collection<DistributionSetTypeCreate> types) {
        return types.stream().map(this::create).collect(Collectors.toList());
    }

    private JpaDistributionSetType findDistributionSetTypeAndThrowExceptionIfNotFound(final Long setId) {
        return (JpaDistributionSetType) get(setId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, setId));
    }

    private static boolean hasModules(final GenericDistributionSetTypeUpdate update) {
        return update.getOptional().isPresent() || update.getMandatory().isPresent();
    }

    private void checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(final Long type) {
        if (distributionSetRepository.countByTypeId(type) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "distribution set type %s is already assigned to distribution sets and cannot be changed", type));
        }
    }

    private static Page<DistributionSetType> convertPage(final Page<JpaDistributionSetType> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    private static Slice<DistributionSetType> convertPage(final Slice<JpaDistributionSetType> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, 0);
    }

    private Page<JpaDistributionSetType> findByCriteriaAPI(final Pageable pageable,
            final List<Specification<JpaDistributionSetType>> specList) {

        if (CollectionUtils.isEmpty(specList)) {
            return distributionSetTypeRepository.findAll(pageable);
        }

        return distributionSetTypeRepository.findAll(SpecificationsBuilder.combineWithAnd(specList), pageable);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        final List<JpaDistributionSetType> setsFound = distributionSetTypeRepository.findAll(ids);

        if (setsFound.size() < ids.size()) {
            throw new EntityNotFoundException(DistributionSetType.class, ids,
                    setsFound.stream().map(DistributionSetType::getId).collect(Collectors.toList()));
        }

        distributionSetTypeRepository.delete(setsFound);
    }

    @Override
    public List<DistributionSetType> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(distributionSetTypeRepository.findAll(ids));
    }

    @Override
    public Optional<DistributionSetType> get(final long id) {
        return Optional.ofNullable(distributionSetTypeRepository.findOne(id));
    }

    @Override
    public boolean exists(final long id) {
        return distributionSetTypeRepository.exists(id);
    }

}

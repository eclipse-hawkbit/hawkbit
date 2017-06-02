/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

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
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
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

    JpaDistributionSetTypeManagement(final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final DistributionSetRepository distributionSetRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer) {
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.distributionSetRepository = distributionSetRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType updateDistributionSetType(final DistributionSetTypeUpdate u) {
        final GenericDistributionSetTypeUpdate update = (GenericDistributionSetTypeUpdate) u;

        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(update.getId());

        update.getDescription().ifPresent(type::setDescription);
        update.getColour().ifPresent(type::setColour);

        if (hasModules(update)) {
            checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(update.getId());

            update.getMandatory().ifPresent(
                    mand -> softwareModuleTypeRepository.findByIdIn(mand).forEach(type::addMandatoryModuleType));
            update.getOptional().ifPresent(
                    opt -> softwareModuleTypeRepository.findByIdIn(opt).forEach(type::addOptionalModuleType));
        }

        return distributionSetTypeRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType assignMandatorySoftwareModuleTypes(final Long dsTypeId,
            final Collection<Long> softwareModulesTypeIds) {
        final Collection<JpaSoftwareModuleType> modules = softwareModuleTypeRepository
                .findByIdIn(softwareModulesTypeIds);

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
    public DistributionSetType assignOptionalSoftwareModuleTypes(final Long dsTypeId,
            final Collection<Long> softwareModulesTypeIds) {

        final Collection<JpaSoftwareModuleType> modules = softwareModuleTypeRepository
                .findByIdIn(softwareModulesTypeIds);

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
    public DistributionSetType unassignSoftwareModuleType(final Long dsTypeId, final Long softwareModuleTypeId) {
        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);

        checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(dsTypeId);

        type.removeModuleType(softwareModuleTypeId);

        return distributionSetTypeRepository.save(type);
    }

    @Override
    public Page<DistributionSetType> findDistributionSetTypesAll(final String rsqlParam, final Pageable pageable) {
        final Specification<JpaDistributionSetType> spec = RSQLUtility.parse(rsqlParam, DistributionSetTypeFields.class,
                virtualPropertyReplacer);

        return convertDsTPage(distributionSetTypeRepository.findAll(spec, pageable));
    }

    @Override
    public Page<DistributionSetType> findDistributionSetTypesAll(final Pageable pageable) {
        return convertDsTPage(distributionSetTypeRepository.findByDeleted(pageable, false));
    }

    @Override
    public Long countDistributionSetTypesAll() {
        return distributionSetTypeRepository.countByDeleted(false);
    }

    @Override
    public Optional<DistributionSetType> findDistributionSetTypeByName(final String name) {
        return Optional
                .ofNullable(distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byName(name)));
    }

    @Override
    public Optional<DistributionSetType> findDistributionSetTypeById(final Long typeId) {
        return Optional
                .ofNullable(distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byId(typeId)));
    }

    @Override
    public Optional<DistributionSetType> findDistributionSetTypeByKey(final String key) {
        return Optional.ofNullable(distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byKey(key)));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType createDistributionSetType(final DistributionSetTypeCreate c) {
        final JpaDistributionSetTypeCreate create = (JpaDistributionSetTypeCreate) c;

        return distributionSetTypeRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteDistributionSetType(final Long typeId) {

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
    public List<DistributionSetType> createDistributionSetTypes(final Collection<DistributionSetTypeCreate> types) {
        return types.stream().map(this::createDistributionSetType).collect(Collectors.toList());
    }

    @Override
    public Long countDistributionSetsByType(final Long typeId) {
        if (!distributionSetTypeRepository.exists(typeId)) {
            throw new EntityNotFoundException(DistributionSetType.class, typeId);
        }

        return distributionSetRepository.countByTypeId(typeId);
    }

    private JpaDistributionSetType findDistributionSetTypeAndThrowExceptionIfNotFound(final Long setId) {
        return (JpaDistributionSetType) findDistributionSetTypeById(setId)
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

    private static Page<DistributionSetType> convertDsTPage(final Page<JpaDistributionSetType> findAll) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()));
    }

}

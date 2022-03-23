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
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetTypeSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.orm.jpa.vendor.Database;
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

    private final TargetTypeRepository targetTypeRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final Database database;

    private final QuotaManagement quotaManagement;

    JpaDistributionSetTypeManagement(final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final DistributionSetRepository distributionSetRepository, final TargetTypeRepository targetTypeRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final Database database,
            final QuotaManagement quotaManagement) {
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.distributionSetRepository = distributionSetRepository;
        this.targetTypeRepository = targetTypeRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
        this.quotaManagement = quotaManagement;
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

        if (hasModuleChanges(update)) {
            checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(update.getId());

            final Collection<Long> currentMandatorySmTypeIds = type.getMandatoryModuleTypes().stream()
                    .map(SoftwareModuleType::getId).collect(Collectors.toSet());
            final Collection<Long> currentOptionalSmTypeIds = type.getOptionalModuleTypes().stream()
                    .map(SoftwareModuleType::getId).collect(Collectors.toSet());
            final Collection<Long> currentSmTypeIds = Stream
                    .concat(currentMandatorySmTypeIds.stream(), currentOptionalSmTypeIds.stream())
                    .collect(Collectors.toSet());

            final Collection<Long> updatedMandatorySmTypeIds = update.getMandatory().orElse(currentMandatorySmTypeIds);
            final Collection<Long> updatedOptionalSmTypeIds = update.getOptional().orElse(currentOptionalSmTypeIds);
            final Collection<Long> updatedSmTypeIds = Stream
                    .concat(updatedMandatorySmTypeIds.stream(), updatedOptionalSmTypeIds.stream())
                    .collect(Collectors.toSet());

            addModuleTypes(currentMandatorySmTypeIds, updatedMandatorySmTypeIds, type::addMandatoryModuleType);
            addModuleTypes(currentOptionalSmTypeIds, updatedOptionalSmTypeIds, type::addOptionalModuleType);

            removeModuleTypes(currentSmTypeIds, updatedSmTypeIds, type::removeModuleType);
        }

        return distributionSetTypeRepository.save(type);
    }

    private void addModuleTypes(final Collection<Long> currentSmTypeIds, final Collection<Long> updatedSmTypeIds,
            final Function<SoftwareModuleType, JpaDistributionSetType> addModuleTypeCallback) {
        final Set<Long> smTypeIdsToAdd = updatedSmTypeIds.stream().filter(id -> !currentSmTypeIds.contains(id))
                .collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(smTypeIdsToAdd)) {
            softwareModuleTypeRepository.findAllById(smTypeIdsToAdd).forEach(addModuleTypeCallback::apply);
        }
    }

    private static void removeModuleTypes(final Collection<Long> currentSmTypeIds,
            final Collection<Long> updatedSmTypeIds,
            final LongFunction<JpaDistributionSetType> removeModuleTypeCallback) {
        final Set<Long> smTypeIdsToRemove = currentSmTypeIds.stream().filter(id -> !updatedSmTypeIds.contains(id))
                .collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(smTypeIdsToRemove)) {
            smTypeIdsToRemove.forEach(removeModuleTypeCallback::apply);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType assignMandatorySoftwareModuleTypes(final long dsTypeId,
            final Collection<Long> softwareModulesTypeIds) {
        final Collection<JpaSoftwareModuleType> modules = softwareModuleTypeRepository
                .findAllById(softwareModulesTypeIds);

        if (modules.size() < softwareModulesTypeIds.size()) {
            throw new EntityNotFoundException(SoftwareModuleType.class, softwareModulesTypeIds,
                    modules.stream().map(SoftwareModuleType::getId).collect(Collectors.toList()));
        }

        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);
        checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(dsTypeId);
        assertSoftwareModuleTypeQuota(dsTypeId, softwareModulesTypeIds.size());

        modules.forEach(type::addMandatoryModuleType);

        return distributionSetTypeRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType assignOptionalSoftwareModuleTypes(final long dsTypeId,
            final Collection<Long> softwareModulesTypeIds) {

        final Collection<JpaSoftwareModuleType> modules = softwareModuleTypeRepository
                .findAllById(softwareModulesTypeIds);

        if (modules.size() < softwareModulesTypeIds.size()) {
            throw new EntityNotFoundException(SoftwareModuleType.class, softwareModulesTypeIds,
                    modules.stream().map(SoftwareModuleType::getId).collect(Collectors.toList()));
        }

        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);
        checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(dsTypeId);
        assertSoftwareModuleTypeQuota(dsTypeId, softwareModulesTypeIds.size());

        modules.forEach(type::addOptionalModuleType);

        return distributionSetTypeRepository.save(type);
    }

    /**
     * Enforces the quota specifying the maximum number of
     * {@link SoftwareModuleType}s per {@link DistributionSetType}.
     * 
     * @param id
     *            of the distribution set type
     * @param requested
     *            number of software module types to check
     * 
     * @throws AssignmentQuotaExceededException
     *             if the software module type quota is exceeded
     */
    private void assertSoftwareModuleTypeQuota(final long id, final int requested) {
        QuotaHelper.assertAssignmentQuota(id, requested,
                quotaManagement.getMaxSoftwareModuleTypesPerDistributionSetType(), SoftwareModuleType.class,
                DistributionSetType.class, distributionSetTypeRepository::countSmTypesById);
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
        return JpaManagementHelper
                .findAllWithCountBySpec(distributionSetTypeRepository, pageable,
                        Arrays.asList(
                                RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetTypeFields.class,
                                        virtualPropertyReplacer, database),
                                DistributionSetTypeSpecification.isDeleted(false)));
    }

    @Override
    public Slice<DistributionSetType> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetTypeRepository, pageable,
                Collections.singletonList(DistributionSetTypeSpecification.isDeleted(false)));
    }

    @Override
    public long count() {
        return distributionSetTypeRepository.countByDeleted(false);
    }

    @Override
    public Optional<DistributionSetType> getByName(final String name) {
        return distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byName(name))
                .map(dst -> (DistributionSetType) dst);
    }

    @Override
    public Optional<DistributionSetType> getByKey(final String key) {
        return distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byKey(key))
                .map(dst -> (DistributionSetType) dst);
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

        unassignDsTypeFromTargetTypes(typeId);

        if (distributionSetRepository.countByTypeId(typeId) > 0) {
            toDelete.setDeleted(true);
            distributionSetTypeRepository.save(toDelete);
        } else {
            distributionSetTypeRepository.deleteById(typeId);
        }
    }

    private void unassignDsTypeFromTargetTypes(final long typeId) {
        final List<JpaTargetType> targetTypesByDsType = targetTypeRepository.findByDsType(typeId);
        targetTypesByDsType.forEach(targetType -> {
            targetType.removeDistributionSetType(typeId);
            targetTypeRepository.save(targetType);
        });
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

    private static boolean hasModuleChanges(final GenericDistributionSetTypeUpdate update) {
        return update.getOptional().isPresent() || update.getMandatory().isPresent();
    }

    private void checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(final Long type) {
        if (distributionSetRepository.countByTypeId(type) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "distribution set type %s is already assigned to distribution sets and cannot be changed", type));
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        final List<JpaDistributionSetType> setsFound = distributionSetTypeRepository.findAllById(ids);

        if (setsFound.size() < ids.size()) {
            throw new EntityNotFoundException(DistributionSetType.class, ids,
                    setsFound.stream().map(DistributionSetType::getId).collect(Collectors.toList()));
        }

        distributionSetTypeRepository.deleteAll(setsFound);
    }

    @Override
    public List<DistributionSetType> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(distributionSetTypeRepository.findAllById(ids));
    }

    @Override
    public Optional<DistributionSetType> get(final long id) {
        return distributionSetTypeRepository.findById(id).map(dst -> (DistributionSetType) dst);
    }

    @Override
    public boolean exists(final long id) {
        return distributionSetTypeRepository.existsById(id);
    }

}

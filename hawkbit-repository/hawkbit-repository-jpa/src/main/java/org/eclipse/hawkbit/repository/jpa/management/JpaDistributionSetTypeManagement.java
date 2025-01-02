/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
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

    public JpaDistributionSetTypeManagement(
            final DistributionSetTypeRepository distributionSetTypeRepository,
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
    public Optional<DistributionSetType> getByKey(final String key) {
        return distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byKey(key)).map(DistributionSetType.class::cast);
    }

    @Override
    public Optional<DistributionSetType> getByName(final String name) {
        return distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byName(name)).map(DistributionSetType.class::cast);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType assignOptionalSoftwareModuleTypes(final long id, final Collection<Long> softwareModulesTypeIds) {
        return assignSoftwareModuleTypes(id, softwareModulesTypeIds, false);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType assignMandatorySoftwareModuleTypes(final long id, final Collection<Long> softwareModuleTypeIds) {
        return assignSoftwareModuleTypes(id, softwareModuleTypeIds, true);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType unassignSoftwareModuleType(final long id, final long softwareModuleTypeId) {
        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(id);
        checkDistributionSetTypeNotAssigned(id);
        type.removeModuleType(softwareModuleTypeId);
        return distributionSetTypeRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSetType> create(final Collection<DistributionSetTypeCreate> types) {
        final List<JpaDistributionSetType> typesToCreate = types.stream()
                .map(JpaDistributionSetTypeCreate.class::cast)
                .map(JpaDistributionSetTypeCreate::build).toList();
        return Collections.unmodifiableList(
                distributionSetTypeRepository.saveAll(AccessController.Operation.CREATE, typesToCreate));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType create(final DistributionSetTypeCreate c) {
        final JpaDistributionSetType distributionSetType = ((JpaDistributionSetTypeCreate) c).build();
        return distributionSetTypeRepository.save(AccessController.Operation.CREATE, distributionSetType);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetType update(final DistributionSetTypeUpdate u) {
        final GenericDistributionSetTypeUpdate update = (GenericDistributionSetTypeUpdate) u;
        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(update.getId());
        update.getDescription().ifPresent(type::setDescription);
        update.getColour().ifPresent(type::setColour);

        if (hasModuleChanges(update)) {
            checkDistributionSetTypeNotAssigned(update.getId());

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

    @Override
    public long count() {
        return distributionSetTypeRepository.count(DistributionSetTypeSpecification.isNotDeleted());
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long id) {
        final JpaDistributionSetType toDelete = distributionSetTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, id));

        unassignDsTypeFromTargetTypes(id);

        if (distributionSetRepository.countByTypeId(id) > 0) {
            toDelete.setDeleted(true);
            distributionSetTypeRepository.save(AccessController.Operation.DELETE, toDelete);
        } else {
            distributionSetTypeRepository.deleteById(id);
        }
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        distributionSetTypeRepository.deleteAllById(ids);
    }

    @Override
    public List<DistributionSetType> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(distributionSetTypeRepository.findAllById(ids));
    }

    @Override
    public boolean exists(final long id) {
        return distributionSetTypeRepository.existsById(id);
    }

    @Override
    public Optional<DistributionSetType> get(final long id) {
        return distributionSetTypeRepository.findById(id).map(DistributionSetType.class::cast);
    }

    @Override
    public Slice<DistributionSetType> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetTypeRepository, pageable, List.of(
                DistributionSetTypeSpecification.isNotDeleted()));
    }

    @Override
    public Page<DistributionSetType> findByRsql(final Pageable pageable, final String rsqlParam) {
        return JpaManagementHelper.findAllWithCountBySpec(distributionSetTypeRepository, pageable, List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetTypeFields.class, virtualPropertyReplacer, database),
                DistributionSetTypeSpecification.isNotDeleted()));
    }

    private static void removeModuleTypes(
            final Collection<Long> currentSmTypeIds, final Collection<Long> updatedSmTypeIds,
            final LongFunction<JpaDistributionSetType> removeModuleTypeCallback) {
        final Set<Long> smTypeIdsToRemove = currentSmTypeIds.stream().filter(id -> !updatedSmTypeIds.contains(id))
                .collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(smTypeIdsToRemove)) {
            smTypeIdsToRemove.forEach(removeModuleTypeCallback::apply);
        }
    }

    private static boolean hasModuleChanges(final GenericDistributionSetTypeUpdate update) {
        return update.getOptional().isPresent() || update.getMandatory().isPresent();
    }

    private void addModuleTypes(
            final Collection<Long> currentSmTypeIds, final Collection<Long> updatedSmTypeIds,
            final Function<SoftwareModuleType, JpaDistributionSetType> addModuleTypeCallback) {
        final Set<Long> smTypeIdsToAdd = updatedSmTypeIds.stream().filter(id -> !currentSmTypeIds.contains(id)).collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(smTypeIdsToAdd)) {
            softwareModuleTypeRepository.findAllById(smTypeIdsToAdd).forEach(addModuleTypeCallback::apply);
        }
    }

    private DistributionSetType assignSoftwareModuleTypes(
            final long dsTypeId, final Collection<Long> softwareModulesTypeIds, final boolean mandatory) {
        final Collection<JpaSoftwareModuleType> foundModules = softwareModuleTypeRepository.findAllById(softwareModulesTypeIds);
        if (foundModules.size() < softwareModulesTypeIds.size()) {
            throw new EntityNotFoundException(
                    SoftwareModuleType.class, softwareModulesTypeIds, foundModules.stream().map(SoftwareModuleType::getId).toList());
        }

        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);

        checkDistributionSetTypeNotAssigned(dsTypeId);
        assertSoftwareModuleTypeQuota(dsTypeId, softwareModulesTypeIds.size());

        foundModules.forEach(mandatory ? type::addMandatoryModuleType : type::addOptionalModuleType);

        return distributionSetTypeRepository.save(type);
    }

    /**
     * Enforces the quota specifying the maximum number of
     * {@link SoftwareModuleType}s per {@link DistributionSetType}.
     *
     * @param id of the distribution set type
     * @param requested number of software module types to check
     * @throws AssignmentQuotaExceededException if the software module type quota is exceeded
     */
    private void assertSoftwareModuleTypeQuota(final long id, final int requested) {
        QuotaHelper.assertAssignmentQuota(id, requested,
                quotaManagement.getMaxSoftwareModuleTypesPerDistributionSetType(), SoftwareModuleType.class,
                DistributionSetType.class, distributionSetTypeRepository::countSmTypesById);
    }

    private void unassignDsTypeFromTargetTypes(final long typeId) {
        final List<JpaTargetType> targetTypesByDsType = targetTypeRepository.findByDsType(typeId);
        targetTypesByDsType.forEach(targetType -> {
            targetType.removeDistributionSetType(typeId);
            targetTypeRepository.save(targetType);
        });
    }

    private JpaDistributionSetType findDistributionSetTypeAndThrowExceptionIfNotFound(final Long setId) {
        return (JpaDistributionSetType) get(setId).orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, setId));
    }

    private void checkDistributionSetTypeNotAssigned(final Long id) {
        if (distributionSetRepository.countByTypeId(id) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "Distribution set type %s is already assigned to distribution sets and cannot be changed!", id));
        }
    }
}
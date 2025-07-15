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

import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_DELAY;
import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_MAX;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetTypeSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "distribution-set-type-management" }, matchIfMissing = true)
public class JpaDistributionSetTypeManagement
        extends AbstractJpaRepositoryManagement<JpaDistributionSetType, JpaDistributionSetTypeCreate, GenericDistributionSetTypeUpdate, DistributionSetTypeRepository, DistributionSetTypeFields>
        implements DistributionSetTypeManagement<JpaDistributionSetType, JpaDistributionSetTypeCreate, GenericDistributionSetTypeUpdate> {

    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;
    private final DistributionSetRepository distributionSetRepository;
    private final TargetTypeRepository targetTypeRepository;
    private final QuotaManagement quotaManagement;

    public JpaDistributionSetTypeManagement(
            final DistributionSetTypeRepository distributionSetTypeRepository,
            final EntityManager entityManager,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final DistributionSetRepository distributionSetRepository, final TargetTypeRepository targetTypeRepository,
            final QuotaManagement quotaManagement) {
        super(distributionSetTypeRepository, entityManager);
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.distributionSetRepository = distributionSetRepository;
        this.targetTypeRepository = targetTypeRepository;
        this.quotaManagement = quotaManagement;
    }

    @Override
    public JpaDistributionSetType update(final GenericDistributionSetTypeUpdate update) {
        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(update.getId());
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

        return super.update(update, type);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void delete(final long id) {
        final JpaDistributionSetType toDelete = jpaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, id));

        unassignDsTypeFromTargetTypes(id);

        if (distributionSetRepository.countByTypeId(id) > 0) {
            toDelete.setDeleted(true);
            jpaRepository.save(AccessController.Operation.DELETE, toDelete);
        } else {
            jpaRepository.deleteById(id);
        }
    }

    @Override
    public void delete0(final Collection<Long> ids) {
        ids.forEach(this::unassignDsTypeFromTargetTypes);
        super.delete0(ids);
    }

    @Override
    public Optional<JpaDistributionSetType> findByKey(final String key) {
        return jpaRepository.findOne(DistributionSetTypeSpecification.byKey(key));
    }

    @Override
    public Optional<JpaDistributionSetType> findByName(final String name) {
        return jpaRepository.findOne(DistributionSetTypeSpecification.byName(name));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public JpaDistributionSetType assignOptionalSoftwareModuleTypes(final long id, final Collection<Long> softwareModulesTypeIds) {
        return assignSoftwareModuleTypes(id, softwareModulesTypeIds, false);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public JpaDistributionSetType assignMandatorySoftwareModuleTypes(final long id, final Collection<Long> softwareModuleTypeIds) {
        return assignSoftwareModuleTypes(id, softwareModuleTypeIds, true);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public JpaDistributionSetType unassignSoftwareModuleType(final long id, final long softwareModuleTypeId) {
        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(id);
        checkDistributionSetTypeNotAssigned(id);
        type.removeModuleType(softwareModuleTypeId);
        return jpaRepository.save(type);
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

    private JpaDistributionSetType assignSoftwareModuleTypes(
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

        return jpaRepository.save(type);
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
                DistributionSetType.class, jpaRepository::countSmTypesById);
    }

    private void unassignDsTypeFromTargetTypes(final long typeId) {
        final List<JpaTargetType> targetTypesByDsType = targetTypeRepository.findByDsType(typeId);
        targetTypesByDsType.forEach(targetType -> {
            targetType.removeDistributionSetType(typeId);
            targetTypeRepository.save(targetType);
        });
    }

    private JpaDistributionSetType findDistributionSetTypeAndThrowExceptionIfNotFound(final Long id) {
        return jpaRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, id));
    }

    private void checkDistributionSetTypeNotAssigned(final Long id) {
        if (distributionSetRepository.countByTypeId(id) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "Distribution set type %s is already assigned to distribution sets and cannot be changed!", id));
        }
    }
}
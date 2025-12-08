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
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
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
import org.eclipse.hawkbit.repository.qfields.DistributionSetTypeFields;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.cache.Cache;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "distribution-set-type-management" }, matchIfMissing = true)
public class JpaDistributionSetTypeManagement
        extends AbstractJpaRepositoryManagement<JpaDistributionSetType, DistributionSetTypeManagement.Create, DistributionSetTypeManagement.Update, DistributionSetTypeRepository, DistributionSetTypeFields>
        implements DistributionSetTypeManagement<JpaDistributionSetType> {

    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;
    private final DistributionSetRepository distributionSetRepository;
    private final TargetTypeRepository targetTypeRepository;
    private final QuotaManagement quotaManagement;

    protected JpaDistributionSetTypeManagement(
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
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public void delete(final long id) {
        final JpaDistributionSetType toDelete = jpaRepository.getById(id);

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
    protected Optional<Cache> getCache() {
        return Optional.of(TenantAwareCacheManager.getInstance().getCache(JpaDistributionSetType.class.getSimpleName()));
    }

    @Override
    public Optional<JpaDistributionSetType> findByKey(final String key) {
        return jpaRepository.findOne(DistributionSetTypeSpecification.byKey(key));
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public JpaDistributionSetType assignOptionalSoftwareModuleTypes(final long id, final Collection<Long> softwareModulesTypeIds) {
        return assignSoftwareModuleTypes(id, softwareModulesTypeIds, false);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public JpaDistributionSetType assignMandatorySoftwareModuleTypes(final long id, final Collection<Long> softwareModuleTypeIds) {
        return assignSoftwareModuleTypes(id, softwareModuleTypeIds, true);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public JpaDistributionSetType unassignSoftwareModuleType(final long id, final long softwareModuleTypeId) {
        final JpaDistributionSetType type = jpaRepository.getById(id);
        checkDistributionSetTypeNotAssigned(id);
        type.removeModuleType(softwareModuleTypeRepository.getById(softwareModuleTypeId));
        return jpaRepository.save(type);
    }

    private JpaDistributionSetType assignSoftwareModuleTypes(
            final long dsTypeId, final Collection<Long> softwareModulesTypeIds, final boolean mandatory) {
        final Collection<JpaSoftwareModuleType> foundModules = softwareModuleTypeRepository.findAllById(softwareModulesTypeIds);
        if (foundModules.size() < softwareModulesTypeIds.size()) {
            throw new EntityNotFoundException(
                    SoftwareModuleType.class, softwareModulesTypeIds, foundModules.stream().map(SoftwareModuleType::getId).toList());
        }

        final JpaDistributionSetType type = jpaRepository.getById(dsTypeId);

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

    private void checkDistributionSetTypeNotAssigned(final Long id) {
        if (distributionSetRepository.countByTypeId(id) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "Distribution set type %s is already assigned to distribution sets and cannot be changed!", id));
        }
    }
}
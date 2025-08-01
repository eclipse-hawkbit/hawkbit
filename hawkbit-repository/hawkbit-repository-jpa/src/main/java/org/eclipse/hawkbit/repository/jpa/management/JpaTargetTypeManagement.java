/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
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
import java.util.function.ToLongFunction;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.TargetTypeFields;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.TargetTypeInUseException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetTypeSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link TargetTypeManagement}.
 */
@Transactional(readOnly = true)
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "target-type-management" }, matchIfMissing = true)
public class JpaTargetTypeManagement
        extends AbstractJpaRepositoryManagement<JpaTargetType, TargetTypeManagement.Create, TargetTypeManagement.Update, TargetTypeRepository, TargetTypeFields>
        implements TargetTypeManagement<JpaTargetType>{

    private final TargetRepository targetRepository;
    private final DistributionSetTypeRepository distributionSetTypeRepository;
    private final QuotaManagement quotaManagement;

    protected JpaTargetTypeManagement(
            final TargetTypeRepository targetTypeRepository, final EntityManager entityManager,
            final TargetRepository targetRepository,
            final DistributionSetTypeRepository distributionSetTypeRepository, final QuotaManagement quotaManagement) {
        super(targetTypeRepository, entityManager);
        this.targetRepository = targetRepository;
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.quotaManagement = quotaManagement;
    }

    @Override
    protected void delete0(final Collection<Long> ids) {
        for (final Long id : ids) {
            if (targetRepository.countByTargetTypeId(id) > 0) {
                throw new TargetTypeInUseException("Cannot delete target type that is in use: " + id);
            }
        }

        super.delete0(ids);
    }

    @Override
    public Optional<TargetType> getByKey(final String key) {
        return jpaRepository.findOne(TargetTypeSpecification.hasKey(key)).map(TargetType.class::cast);
    }

    @Override
    public Optional<TargetType> getByName(final String name) {
        return jpaRepository.findOne(TargetTypeSpecification.hasName(name)).map(TargetType.class::cast);
    }

    @Override
    public Slice<TargetType> findByName(final String name, final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(jpaRepository, List.of(TargetTypeSpecification.likeName(name)), pageable);
    }

    @Override
    public long countByName(final String name) {
        return jpaRepository.count(TargetTypeSpecification.hasName(name));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetType assignCompatibleDistributionSetTypes(final long id,
            final Collection<Long> distributionSetTypeIds) {
        final Collection<JpaDistributionSetType> dsTypes = distributionSetTypeRepository
                .findAllById(distributionSetTypeIds);

        if (dsTypes.size() < distributionSetTypeIds.size()) {
            throw new EntityNotFoundException(DistributionSetType.class, distributionSetTypeIds,
                    dsTypes.stream().map(DistributionSetType::getId).toList());
        }

        final JpaTargetType type = getByIdAndThrowIfNotFound(id);
        assertDistributionSetTypeQuota(id, distributionSetTypeIds.size(), typeId -> type.getDistributionSetTypes().size());
        dsTypes.forEach(type::addCompatibleDistributionSetType);

        return jpaRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetType unassignDistributionSetType(final long id, final long distributionSetTypeId) {
        final JpaTargetType type = getByIdAndThrowIfNotFound(id);
        assertDistributionSetTypeExists(distributionSetTypeId);

        type.removeDistributionSetType(distributionSetTypeId);

        return jpaRepository.save(type);
    }

    @SuppressWarnings("java:S2201") // the idea is just to check for distribution set type existence
    private void assertDistributionSetTypeExists(final Long typeId) {
        distributionSetTypeRepository
                .findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, typeId));
    }

    private JpaTargetType getByIdAndThrowIfNotFound(final Long id) {
        return jpaRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(TargetType.class, id));
    }

    /**
     * Enforces the quota specifying the maximum number of
     * {@link DistributionSetType}s per {@link TargetType}.
     *
     * @param id of the target type
     * @param requested number of distribution set types to check
     * @throws AssignmentQuotaExceededException if the software module type quota is exceeded
     */
    private void assertDistributionSetTypeQuota(final long id, final int requested, final ToLongFunction<Long> countFct) {
        QuotaHelper.assertAssignmentQuota(
                id, requested, quotaManagement.getMaxDistributionSetTypesPerTargetType(),
                DistributionSetType.class, TargetType.class, countFct);
    }
}
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.ToLongFunction;

import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.TargetTypeFields;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.builder.GenericTargetTypeUpdate;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.builder.TargetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.TargetTypeInUseException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetTypeCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetTypeSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link TargetTypeManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaTargetTypeManagement implements TargetTypeManagement {

    private final TargetTypeRepository targetTypeRepository;
    private final TargetRepository targetRepository;
    private final DistributionSetTypeRepository distributionSetTypeRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final Database database;
    private final QuotaManagement quotaManagement;

    /**
     * Constructor
     *
     * @param targetTypeRepository Target type repository
     * @param targetRepository Target repository
     * @param virtualPropertyReplacer replacer
     * @param database database
     */
    public JpaTargetTypeManagement(final TargetTypeRepository targetTypeRepository,
            final TargetRepository targetRepository, final DistributionSetTypeRepository distributionSetTypeRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final Database database,
            final QuotaManagement quotaManagement) {
        this.targetTypeRepository = targetTypeRepository;
        this.targetRepository = targetRepository;
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
        this.quotaManagement = quotaManagement;
    }

    @Override
    public Optional<TargetType> getByKey(final String key) {
        return targetTypeRepository.findOne(TargetTypeSpecification.hasKey(key)).map(TargetType.class::cast);
    }

    @Override
    public Optional<TargetType> getByName(final String name) {
        return targetTypeRepository.findOne(TargetTypeSpecification.hasName(name)).map(TargetType.class::cast);
    }

    @Override
    public long count() {
        return targetTypeRepository.count();
    }

    @Override
    public long countByName(final String name) {
        return targetTypeRepository.count(TargetTypeSpecification.hasName(name));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetType create(final TargetTypeCreate create) {
        final JpaTargetType typeCreate = ((JpaTargetTypeCreate) create).build();
        return targetTypeRepository.save(AccessController.Operation.CREATE, typeCreate);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<TargetType> create(final Collection<TargetTypeCreate> creates) {
        final List<JpaTargetType> typeCreate =
                creates.stream().map(create -> ((JpaTargetTypeCreate) create).build()).toList();
        return Collections.unmodifiableList(targetTypeRepository.saveAll(AccessController.Operation.CREATE, typeCreate));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Long id) {
        getByIdAndThrowIfNotFound(id);

        if (targetRepository.countByTargetTypeId(id) > 0) {
            throw new TargetTypeInUseException("Cannot delete target type that is in use");
        }

        targetTypeRepository.deleteById(id);
    }

    @Override
    public Slice<TargetType> findAll(final Pageable pageable) {
        return targetTypeRepository.findAllWithoutCount(pageable).map(TargetType.class::cast);
    }

    @Override
    public Page<TargetType> findByRsql(final String rsql, final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(targetTypeRepository, List.of(
                RsqlUtility.buildRsqlSpecification(
                        rsql, TargetTypeFields.class, virtualPropertyReplacer, database)), pageable
        );
    }

    @Override
    public Slice<TargetType> findByName(final String name, final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(targetTypeRepository, List.of(TargetTypeSpecification.likeName(name)), pageable
        );
    }

    @Override
    public Optional<TargetType> get(final long id) {
        return targetTypeRepository.findById(id).map(TargetType.class::cast);
    }

    @Override
    public List<TargetType> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(targetTypeRepository.findAllById(ids));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetType update(final TargetTypeUpdate update) {
        final GenericTargetTypeUpdate typeUpdate = (GenericTargetTypeUpdate) update;

        final JpaTargetType type = getByIdAndThrowIfNotFound(typeUpdate.getId());

        typeUpdate.getName().ifPresent((type::setName));
        typeUpdate.getDescription().ifPresent(type::setDescription);
        typeUpdate.getColour().ifPresent(type::setColour);

        return targetTypeRepository.save(type);
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
        assertDistributionSetTypeQuota(id, distributionSetTypeIds.size(), typeId -> type.getCompatibleDistributionSetTypes().size());
        dsTypes.forEach(type::addCompatibleDistributionSetType);

        return targetTypeRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetType unassignDistributionSetType(final long id, final long distributionSetTypeId) {
        final JpaTargetType type = getByIdAndThrowIfNotFound(id);
        assertDistributionSetTypeExists(distributionSetTypeId);

        type.removeDistributionSetType(distributionSetTypeId);

        return targetTypeRepository.save(type);
    }

    @SuppressWarnings("java:S2201") // the idea is just to check for distribution set type existence
    private void assertDistributionSetTypeExists(final Long typeId) {
        distributionSetTypeRepository
                .findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, typeId));
    }

    private JpaTargetType getByIdAndThrowIfNotFound(final Long id) {
        return targetTypeRepository
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
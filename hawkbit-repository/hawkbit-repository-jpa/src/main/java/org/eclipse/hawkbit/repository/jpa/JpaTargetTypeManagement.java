/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
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

import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.TargetTypeFields;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.builder.GenericTargetTypeUpdate;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.builder.TargetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.TargetTypeInUseException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetTypeCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
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
 *
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
     * @param targetTypeRepository
     *            Target type repository
     * @param targetRepository
     *            Target repository
     * @param virtualPropertyReplacer
     *            replacer
     * @param database
     *            database
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
    public Optional<TargetType> getByName(final String name) {
        return targetTypeRepository.findByName(name).map(TargetType.class::cast);
    }

    @Override
    public long count() {
        return targetTypeRepository.count();
    }

    @Override
    public long countByName(final String name) {
        return targetTypeRepository.countByName(name);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetType create(final TargetTypeCreate create) {
        final JpaTargetTypeCreate typeCreate = (JpaTargetTypeCreate) create;
        return targetTypeRepository.save(typeCreate.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<TargetType> create(final Collection<TargetTypeCreate> creates) {
        return creates.stream().map(this::create).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Long targetTypeId) {
        throwExceptionIfTargetTypeDoesNotExist(targetTypeId);

        if (targetRepository.countByTargetTypeId(targetTypeId) > 0) {
            throw new TargetTypeInUseException("Cannot delete target type that is in use");
        }

        targetTypeRepository.deleteById(targetTypeId);
    }

    @Override
    public Slice<TargetType> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(targetTypeRepository, pageable, null);
    }

    @Override
    public Page<TargetType> findByRsql(final Pageable pageable, final String rsqlParam) {
        return JpaManagementHelper.findAllWithCountBySpec(targetTypeRepository, pageable,
                Collections.singletonList(RSQLUtility.buildRsqlSpecification(rsqlParam, TargetTypeFields.class,
                        virtualPropertyReplacer, database)));
    }

    @Override
    public Slice<TargetType> findByName(final Pageable pageable, final String name) {
        return JpaManagementHelper.findAllWithoutCountBySpec(targetTypeRepository, pageable,
                Collections.singletonList(TargetTypeSpecification.likeName(name)));
    }

    @Override
    public Optional<TargetType> get(final long id) {
        return targetTypeRepository.findById(id).map(targetType -> targetType);
    }

    @Override
    public Optional<TargetType> findByTargetId(final long targetId) {
        return targetTypeRepository.findOne(TargetTypeSpecification.hasTarget(targetId)).map(TargetType.class::cast);
    }

    @Override
    public List<TargetType> findByTargetIds(final Collection<Long> targetIds) {
        return targetTypeRepository.findAll(TargetTypeSpecification.hasTarget(targetIds)).stream()
                .map(TargetType.class::cast).collect(Collectors.toList());
    }

    @Override
    public Optional<TargetType> findByTargetControllerId(final String controllerId) {
        return targetTypeRepository.findOne(TargetTypeSpecification.hasTargetControllerId(controllerId))
                .map(TargetType.class::cast);
    }

    @Override
    public List<TargetType> findByTargetControllerIds(final Collection<String> controllerIds) {
        return targetTypeRepository.findAll(TargetTypeSpecification.hasTargetControllerIdIn(controllerIds)).stream()
                .map(TargetType.class::cast).collect(Collectors.toList());
    }

    @Override
    public List<TargetType> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(targetTypeRepository.findAllById(ids));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetType update(final TargetTypeUpdate update) {
        final GenericTargetTypeUpdate typeUpdate = (GenericTargetTypeUpdate) update;

        final JpaTargetType type = findTargetTypeAndThrowExceptionIfNotFound(typeUpdate.getId());

        typeUpdate.getName().ifPresent((type::setName));
        typeUpdate.getDescription().ifPresent(type::setDescription);
        typeUpdate.getColour().ifPresent(type::setColour);

        return targetTypeRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetType assignCompatibleDistributionSetTypes(final long targetTypeId,
            final Collection<Long> distributionSetTypeIds) {
        final Collection<JpaDistributionSetType> dsTypes = distributionSetTypeRepository
                .findAllById(distributionSetTypeIds);

        if (dsTypes.size() < distributionSetTypeIds.size()) {
            throw new EntityNotFoundException(DistributionSetType.class, distributionSetTypeIds,
                    dsTypes.stream().map(DistributionSetType::getId).collect(Collectors.toList()));
        }

        final JpaTargetType type = findTargetTypeAndThrowExceptionIfNotFound(targetTypeId);
        assertDistributionSetTypeQuota(targetTypeId, distributionSetTypeIds.size());

        dsTypes.forEach(type::addCompatibleDistributionSetType);

        return targetTypeRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetType unassignDistributionSetType(final long targetTypeId, final long distributionSetTypeId) {
        final JpaTargetType type = findTargetTypeAndThrowExceptionIfNotFound(targetTypeId);
        findDsTypeAndThrowExceptionIfNotFound(distributionSetTypeId);
        type.removeDistributionSetType(distributionSetTypeId);

        return targetTypeRepository.save(type);
    }

    private JpaTargetType findTargetTypeAndThrowExceptionIfNotFound(final Long typeId) {
        return (JpaTargetType) get(typeId).orElseThrow(() -> new EntityNotFoundException(TargetType.class, typeId));
    }

    private JpaDistributionSetType findDsTypeAndThrowExceptionIfNotFound(final Long typeId) {
        return distributionSetTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, typeId));
    }

    private void throwExceptionIfTargetTypeDoesNotExist(final Long typeId) {
        if (!targetTypeRepository.existsById(typeId)) {
            throw new EntityNotFoundException(TargetType.class, typeId);
        }
    }

    /**
     * Enforces the quota specifying the maximum number of
     * {@link DistributionSetType}s per {@link TargetType}.
     *
     * @param id
     *            of the target type
     * @param requested
     *            number of distribution set types to check
     *
     * @throws AssignmentQuotaExceededException
     *             if the software module type quota is exceeded
     */
    private void assertDistributionSetTypeQuota(final long id, final int requested) {
        QuotaHelper.assertAssignmentQuota(id, requested, quotaManagement.getMaxDistributionSetTypesPerTargetType(),
                DistributionSetType.class, TargetType.class, targetTypeRepository::countDsSetTypesById);
    }
}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetMetadataFields;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetUpdate;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessControlService;
import org.eclipse.hawkbit.repository.jpa.acm.controller.AccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.DistributionSetAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.SoftwareModuleAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.TargetAccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;

/**
 * JPA implementation of {@link DistributionSetManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaDistributionSetManagement implements DistributionSetManagement {

    private final EntityManager entityManager;

    private final DistributionSetRepository distributionSetRepository;

    private final DistributionSetTagManagement distributionSetTagManagement;

    private final SystemManagement systemManagement;

    private final DistributionSetTypeManagement distributionSetTypeManagement;

    private final QuotaManagement quotaManagement;

    private final DistributionSetMetadataRepository distributionSetMetadataRepository;

    private final TargetFilterQueryRepository targetFilterQueryRepository;

    private final ActionRepository actionRepository;

    private final EventPublisherHolder eventPublisherHolder;

    private final TenantAware tenantAware;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final SoftwareModuleRepository softwareModuleRepository;

    private final DistributionSetTagRepository distributionSetTagRepository;

    private final AfterTransactionCommitExecutor afterCommit;

    private final DistributionSetAccessController distributionSetAccessController;

    private final SoftwareModuleAccessController softwareModuleAccessController;

    private final TargetAccessController targetAccessController;

    private final Database database;

    JpaDistributionSetManagement(final EntityManager entityManager,
            final DistributionSetRepository distributionSetRepository,
            final DistributionSetTagManagement distributionSetTagManagement, final SystemManagement systemManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement, final QuotaManagement quotaManagement,
            final DistributionSetMetadataRepository distributionSetMetadataRepository,
            final TargetFilterQueryRepository targetFilterQueryRepository, final ActionRepository actionRepository,
            final EventPublisherHolder eventPublisherHolder, final TenantAware tenantAware,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final SoftwareModuleRepository softwareModuleRepository,
            final DistributionSetTagRepository distributionSetTagRepository,
            final AfterTransactionCommitExecutor afterCommit, final AccessControlService accessControlService,
            final Database database) {
        this.entityManager = entityManager;
        this.distributionSetRepository = distributionSetRepository;
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.systemManagement = systemManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.quotaManagement = quotaManagement;
        this.distributionSetMetadataRepository = distributionSetMetadataRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.actionRepository = actionRepository;
        this.eventPublisherHolder = eventPublisherHolder;
        this.tenantAware = tenantAware;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.softwareModuleRepository = softwareModuleRepository;
        this.distributionSetTagRepository = distributionSetTagRepository;
        this.afterCommit = afterCommit;
        this.distributionSetAccessController = accessControlService.getDistributionSetAccessController();
        this.softwareModuleAccessController = accessControlService.getSoftwareModuleAccessController();
        this.targetAccessController = accessControlService.getTargetAccessController();
        this.database = database;
    }

    @Override
    public Optional<DistributionSet> getWithDetails(final long distId) {
        final Specification<JpaDistributionSet> specification = distributionSetAccessController
                .appendAccessRules(AccessController.Operation.READ, DistributionSetSpecification.byId(distId));
        return distributionSetRepository.findOne(specification).map(x -> x);
    }

    @Override
    public long countByTypeId(final long typeId) {
        if (!distributionSetTypeManagement.exists(typeId)) {
            throw new EntityNotFoundException(DistributionSetType.class, typeId);
        }
        final Specification<JpaDistributionSet> specification = distributionSetAccessController
                .appendAccessRules(AccessController.Operation.READ, DistributionSetSpecification.byType(typeId));
        return distributionSetRepository.count(specification);
    }

    @Override
    public List<Statistic> countRolloutsByStatusForDistributionSet(final Long dsId) {
        // TODO: how limiting access here?
        return distributionSetRepository.countRolloutsByStatusForDistributionSet(dsId).stream()
                .map(Statistic.class::cast).toList();
    }

    @Override
    public List<Statistic> countActionsByStatusForDistributionSet(final Long dsId) {
        // TODO: how limiting access here?
        return distributionSetRepository.countActionsByStatusForDistributionSet(dsId).stream()
                .map(Statistic.class::cast).toList();
    }

    @Override
    public Long countAutoAssignmentsForDistributionSet(final Long dsId) {
        getDistributionSetOrThrowExceptionIfNotFound(dsId);
        return distributionSetRepository.countAutoAssignmentsForDistributionSet(dsId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetTagAssignmentResult toggleTagAssignment(final Collection<Long> dsIds, final String tagName) {
        final List<JpaDistributionSet> sets = findDistributionSetListWithDetails(dsIds);

        if (sets.size() < dsIds.size()) {
            throw new EntityNotFoundException(DistributionSet.class, dsIds,
                    sets.stream().map(DistributionSet::getId).toList());
        }

        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, sets);

        final DistributionSetTag myTag = distributionSetTagManagement.getByName(tagName)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, tagName));

        final DistributionSetTagAssignmentResult result;

        final List<JpaDistributionSet> toBeChangedDSs = sets.stream().filter(set -> set.addTag(myTag))
                .collect(Collectors.toList());

        // un-assignment case
        if (toBeChangedDSs.isEmpty()) {
            for (final JpaDistributionSet set : sets) {
                if (set.removeTag(myTag)) {
                    toBeChangedDSs.add(set);
                }
            }
            result = new DistributionSetTagAssignmentResult(dsIds.size() - toBeChangedDSs.size(),
                    Collections.emptyList(),
                    Collections.unmodifiableList(
                            toBeChangedDSs.stream().map(distributionSetRepository::save).collect(Collectors.toList())),
                    myTag);
        } else {
            result = new DistributionSetTagAssignmentResult(dsIds.size() - toBeChangedDSs.size(),
                    Collections.unmodifiableList(
                            toBeChangedDSs.stream().map(distributionSetRepository::save).collect(Collectors.toList())),
                    Collections.emptyList(), myTag);
        }

        // no reason to persist the tag
        entityManager.detach(myTag);
        return result;
    }

    private List<JpaDistributionSet> findDistributionSetListWithDetails(final Collection<Long> distributionIdSet) {
        final Specification<JpaDistributionSet> specification = distributionSetAccessController.appendAccessRules(
                AccessController.Operation.READ, DistributionSetSpecification.byIds(distributionIdSet));
        return distributionSetRepository.findAll(specification);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet update(final DistributionSetUpdate u) {
        final GenericDistributionSetUpdate update = (GenericDistributionSetUpdate) u;

        final JpaDistributionSet set = (JpaDistributionSet) getValid(update.getId());

        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, set);

        update.getName().ifPresent(set::setName);
        update.getDescription().ifPresent(set::setDescription);
        update.getVersion().ifPresent(set::setVersion);

        if (update.isRequiredMigrationStep() != null
                && !update.isRequiredMigrationStep().equals(set.isRequiredMigrationStep())) {
            assertDistributionSetIsNotAssignedToTargets(update.getId());
            set.setRequiredMigrationStep(update.isRequiredMigrationStep());
        }

        return distributionSetRepository.save(set);
    }

    private JpaSoftwareModule findSoftwareModuleAndThrowExceptionIfNotFound(final Long moduleId) {
        final Specification<JpaSoftwareModule> specification = softwareModuleAccessController
                .appendAccessRules(AccessController.Operation.READ, SoftwareModuleSpecification.byId(moduleId));
        return softwareModuleRepository.findOne(specification)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, moduleId));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> distributionSetIDs) {
        final List<JpaDistributionSet> setsFound = getDistributionSets(distributionSetIDs);

        if (setsFound.size() < distributionSetIDs.size()) {
            throw new EntityNotFoundException(DistributionSet.class, distributionSetIDs,
                    setsFound.stream().map(DistributionSet::getId).toList());
        }
        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.DELETE, setsFound);

        final List<Long> assigned = distributionSetRepository
                .findAssignedToTargetDistributionSetsById(distributionSetIDs);
        assigned.addAll(distributionSetRepository.findAssignedToRolloutDistributionSetsById(distributionSetIDs));

        // soft delete assigned
        if (!assigned.isEmpty()) {
            final Long[] dsIds = assigned.toArray(new Long[assigned.size()]);
            distributionSetRepository.deleteDistributionSet(dsIds);
            targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(dsIds);
        }

        // mark the rest as hard delete
        final List<Long> toHardDelete = distributionSetIDs.stream().filter(setId -> !assigned.contains(setId)).toList();

        // hard delete the rest if exists
        if (!toHardDelete.isEmpty()) {
            targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(
                    toHardDelete.toArray(new Long[toHardDelete.size()]));
            // don't give the delete statement an empty list, JPA/Oracle cannot
            // handle the empty list
            distributionSetRepository.deleteByIdIn(toHardDelete);
        }

        afterCommit.afterCommit(() -> distributionSetIDs.forEach(dsId -> eventPublisherHolder.getEventPublisher()
                .publishEvent(new DistributionSetDeletedEvent(tenantAware.getCurrentTenant(), dsId,
                        JpaDistributionSet.class, eventPublisherHolder.getApplicationId()))));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet create(final DistributionSetCreate c) {
        final JpaDistributionSetCreate create = (JpaDistributionSetCreate) c;
        setDefaultTypeIfMissing(create);

        final JpaDistributionSet toCreateDs = create.build();
        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.CREATE, toCreateDs);
        return distributionSetRepository.save(toCreateDs);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> create(final Collection<DistributionSetCreate> creates) {
        final List<JpaDistributionSet> toCreate = creates.stream().map(JpaDistributionSetCreate.class::cast)
                .map(this::setDefaultTypeIfMissing).map(JpaDistributionSetCreate::build).toList();
        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.CREATE, toCreate);

        return Collections.unmodifiableList(distributionSetRepository.saveAll(toCreate));
    }

    private JpaDistributionSetCreate setDefaultTypeIfMissing(final JpaDistributionSetCreate create) {
        if (create.getType() == null) {
            create.type(systemManagement.getTenantMetadata().getDefaultDsType().getKey());
        }
        return create;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet assignSoftwareModules(final long setId, final Collection<Long> moduleIds) {
        final JpaDistributionSet set = (JpaDistributionSet) getValid(setId);

        final Specification<JpaSoftwareModule> specification = softwareModuleAccessController
                .appendAccessRules(AccessController.Operation.READ, SoftwareModuleSpecification.byIds(moduleIds));
        final Collection<JpaSoftwareModule> modules = softwareModuleRepository.findAll(specification);

        if (modules.size() < moduleIds.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, moduleIds,
                    modules.stream().map(SoftwareModule::getId).toList());
        }

        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, set);

        assertDistributionSetIsNotAssignedToTargets(setId);

        assertSoftwareModuleQuota(setId, modules.size());

        modules.forEach(set::addModule);

        return distributionSetRepository.save(set);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet unassignSoftwareModule(final long setId, final long moduleId) {
        final JpaDistributionSet set = (JpaDistributionSet) getValid(setId);
        final JpaSoftwareModule module = findSoftwareModuleAndThrowExceptionIfNotFound(moduleId);

        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, set);
        assertDistributionSetIsNotAssignedToTargets(setId);

        set.removeModule(module);

        return distributionSetRepository.save(set);
    }

    @Override
    public Slice<DistributionSet> findByDistributionSetFilter(final Pageable pageable,
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);
        specList.add(distributionSetAccessController.getAccessRules(AccessController.Operation.READ));
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, pageable, specList);
    }

    @Override
    public long countByDistributionSetFilter(@NotNull final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);
        specList.add(distributionSetAccessController.getAccessRules(AccessController.Operation.READ));

        return JpaManagementHelper.countBySpec(distributionSetRepository, specList);
    }

    @Override
    public Slice<DistributionSet> findByCompleted(final Pageable pageReq, final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specifications = buildSpecsByComplete(complete);
        specifications.add(distributionSetAccessController.getAccessRules(AccessController.Operation.READ));
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, pageReq, specifications);
    }

    @Override
    public long countByCompleted(final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specifications = buildSpecsByComplete(complete);
        specifications.add(distributionSetAccessController.getAccessRules(AccessController.Operation.READ));
        return JpaManagementHelper.countBySpec(distributionSetRepository, specifications);
    }

    private List<Specification<JpaDistributionSet>> buildSpecsByComplete(final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specifications = new ArrayList<>();
        specifications.add(DistributionSetSpecification.isDeleted(false));
        if (complete != null) {
            specifications.add(DistributionSetSpecification.isCompleted(complete));
        }
        return specifications;
    }

    @Override
    public Slice<DistributionSet> findByDistributionSetFilterOrderByLinkedTarget(final Pageable pageable,
            final DistributionSetFilter distributionSetFilter, final String assignedOrInstalled) {
        // remove default sort from pageable to not overwrite sorted spec
        final OffsetBasedPageRequest unsortedPage = new OffsetBasedPageRequest(pageable.getOffset(),
                pageable.getPageSize(), Sort.unsorted());

        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);
        specList.add(DistributionSetSpecification.orderedByLinkedTarget(assignedOrInstalled, pageable.getSort()));
        specList.add(distributionSetAccessController.getAccessRules(AccessController.Operation.READ));

        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, unsortedPage, specList);
    }

    @Override
    public Optional<DistributionSet> getByNameAndVersion(final String distributionName, final String version) {
        final Specification<JpaDistributionSet> spec = distributionSetAccessController.appendAccessRules(
                AccessController.Operation.READ,
                DistributionSetSpecification.equalsNameAndVersionIgnoreCase(distributionName, version));
        return distributionSetRepository.findOne(spec).map(DistributionSet.class::cast);

    }

    @Override
    public long count() {
        final Specification<JpaDistributionSet> spec = distributionSetAccessController.appendAccessRules(
                AccessController.Operation.READ, DistributionSetSpecification.isDeleted(Boolean.FALSE));
        return distributionSetRepository.count(spec);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSetMetadata> createMetaData(final long dsId, final Collection<MetaData> md) {
        final JpaDistributionSet distributionSet = getDistributionSetOrThrowExceptionIfNotFound(dsId);
        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, distributionSet);

        md.forEach(meta -> checkAndThrowIfDistributionSetMetadataAlreadyExists(
                new DsMetadataCompositeKey(dsId, meta.getKey())));

        assertMetaDataQuota(dsId, md.size());

        JpaManagementHelper.touch(entityManager, distributionSetRepository, distributionSet);

        return md.stream()
                .map(meta -> distributionSetMetadataRepository
                        .save(new JpaDistributionSetMetadata(meta.getKey(), distributionSet, meta.getValue())))
                .collect(Collectors.toUnmodifiableList());
    }

    private void assertMetaDataQuota(final Long dsId, final int requested) {
        QuotaHelper.assertAssignmentQuota(dsId, requested, quotaManagement.getMaxMetaDataEntriesPerDistributionSet(),
                DistributionSetMetadata.class, DistributionSet.class,
                distributionSetMetadataRepository::countByDistributionSetId);
    }

    private void assertSoftwareModuleQuota(final Long id, final int requested) {
        QuotaHelper.assertAssignmentQuota(id, requested, quotaManagement.getMaxSoftwareModulesPerDistributionSet(),
                SoftwareModule.class, DistributionSet.class, softwareModuleRepository::countByAssignedToId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetMetadata updateMetaData(final long dsId, final MetaData md) {
        final JpaDistributionSet distributionSet = getDistributionSetOrThrowExceptionIfNotFound(dsId);
        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, distributionSet);

        // check if exists otherwise throw entity not found exception
        final JpaDistributionSetMetadata toUpdate = (JpaDistributionSetMetadata) getMetaDataByDistributionSetId(dsId,
                md.getKey())
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetMetadata.class, dsId, md.getKey()));
        toUpdate.setValue(md.getValue());
        // touch it to update the lock revision because we are modifying the
        // DS indirectly
        JpaManagementHelper.touch(entityManager, distributionSetRepository, (JpaDistributionSet) getValid(dsId));
        return distributionSetMetadataRepository.save(toUpdate);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetaData(final long distributionSetId, final String key) {
        final JpaDistributionSet distributionSet = getDistributionSetOrThrowExceptionIfNotFound(distributionSetId);
        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, distributionSet);

        final JpaDistributionSetMetadata metadata = (JpaDistributionSetMetadata) getMetaDataByDistributionSetId(
                distributionSetId, key)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetMetadata.class, distributionSetId, key));

        JpaManagementHelper.touch(entityManager, distributionSetRepository,
                (JpaDistributionSet) metadata.getDistributionSet());
        distributionSetMetadataRepository.deleteById(metadata.getId());
    }

    @Override
    public Page<DistributionSetMetadata> findMetaDataByDistributionSetId(final Pageable pageable,
            final long distributionSetId) {
        getDistributionSetOrThrowExceptionIfNotFound(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetMetadataRepository, pageable,
                Collections.singletonList(byDsIdSpec(distributionSetId)));
    }

    private Specification<JpaDistributionSetMetadata> byDsIdSpec(final long dsId) {
        return (root, query, cb) -> cb
                .equal(root.get(JpaDistributionSetMetadata_.distributionSet).get(JpaDistributionSet_.id), dsId);
    }

    @Override
    public long countMetaDataByDistributionSetId(final long setId) {
        getDistributionSetOrThrowExceptionIfNotFound(setId);

        return distributionSetMetadataRepository.countByDistributionSetId(setId);
    }

    @Override
    public Page<DistributionSetMetadata> findMetaDataByDistributionSetIdAndRsql(final Pageable pageable,
            final long distributionSetId, final String rsqlParam) {
        getDistributionSetOrThrowExceptionIfNotFound(distributionSetId);

        final List<Specification<JpaDistributionSetMetadata>> specList = Arrays
                .asList(RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetMetadataFields.class,
                        virtualPropertyReplacer, database), byDsIdSpec(distributionSetId));

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetMetadataRepository, pageable, specList);
    }

    @Override
    public Optional<DistributionSetMetadata> getMetaDataByDistributionSetId(final long setId, final String key) {
        getDistributionSetOrThrowExceptionIfNotFound(setId);

        return distributionSetMetadataRepository.findById(new DsMetadataCompositeKey(setId, key)).map(x -> x);
    }

    @Override
    public Optional<DistributionSet> getByAction(final long actionId) {
        // corresponding target read permissions are given.
        actionRepository.findById(actionId).ifPresentOrElse(action -> {
            targetAccessController.assertOperationAllowed(AccessController.Operation.READ,
                    (JpaTarget) action.getTarget());
        }, () -> {
            throw new EntityNotFoundException(Action.class, actionId);
        });

        final Specification<JpaDistributionSet> specification = distributionSetAccessController
                .appendAccessRules(AccessController.Operation.READ, DistributionSetSpecification.byActionId(actionId));

        return distributionSetRepository.findOne(specification).map(x -> x);
    }

    @Override
    public boolean isInUse(final long setId) {
        getDistributionSetOrThrowExceptionIfNotFound(setId);

        return actionRepository.countByDistributionSetId(setId) > 0;
    }

    private static List<Specification<JpaDistributionSet>> buildDistributionSetSpecifications(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = Lists.newArrayListWithExpectedSize(10);

        Specification<JpaDistributionSet> spec;

        if (distributionSetFilter.getIsComplete() != null) {
            spec = DistributionSetSpecification.isCompleted(distributionSetFilter.getIsComplete());
            specList.add(spec);
        }

        if (distributionSetFilter.getIsDeleted() != null) {
            spec = DistributionSetSpecification.isDeleted(distributionSetFilter.getIsDeleted());
            specList.add(spec);
        }

        if (distributionSetFilter.getIsValid() != null) {
            spec = DistributionSetSpecification.isValid(distributionSetFilter.getIsValid());
            specList.add(spec);
        }

        if (distributionSetFilter.getTypeId() != null) {
            spec = DistributionSetSpecification.byType(distributionSetFilter.getTypeId());
            specList.add(spec);
        }

        if (StringUtils.hasText(distributionSetFilter.getSearchText())) {
            final String[] dsFilterNameAndVersionEntries = JpaManagementHelper
                    .getFilterNameAndVersionEntries(distributionSetFilter.getSearchText().trim());
            spec = DistributionSetSpecification.likeNameAndVersion(dsFilterNameAndVersionEntries[0],
                    dsFilterNameAndVersionEntries[1]);
            specList.add(spec);
        }

        if (hasTagsFilterActive(distributionSetFilter)) {
            spec = DistributionSetSpecification.hasTags(distributionSetFilter.getTagNames(),
                    distributionSetFilter.getSelectDSWithNoTag());
            specList.add(spec);
        }
        if (distributionSetFilter.getInstalledTargetId() != null) {
            spec = DistributionSetSpecification.installedTarget(distributionSetFilter.getInstalledTargetId());
            specList.add(spec);
        }
        if (distributionSetFilter.getAssignedTargetId() != null) {
            spec = DistributionSetSpecification.assignedTarget(distributionSetFilter.getAssignedTargetId());
            specList.add(spec);
        }
        return specList;
    }

    private void assertDistributionSetIsNotAssignedToTargets(final Long distributionSet) {
        if (actionRepository.countByDistributionSetId(distributionSet) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "Distribution set %s is already assigned to targets and cannot be changed", distributionSet));
        }
    }

    private static boolean hasTagsFilterActive(final DistributionSetFilter distributionSetFilter) {
        final boolean isNoTagActive = Boolean.TRUE.equals(distributionSetFilter.getSelectDSWithNoTag());
        final boolean isAtLeastOneTagActive = !CollectionUtils.isEmpty(distributionSetFilter.getTagNames());

        return isNoTagActive || isAtLeastOneTagActive;
    }

    private void checkAndThrowIfDistributionSetMetadataAlreadyExists(final DsMetadataCompositeKey metadataId) {
        if (distributionSetMetadataRepository.existsById(metadataId)) {
            throw new EntityAlreadyExistsException(
                    "Metadata entry with key '" + metadataId.getKey() + "' already exists");
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> assignTag(final Collection<Long> dsIds, final long dsTagId) {
        final List<JpaDistributionSet> allDs = findDistributionSetListWithDetails(dsIds);

        if (allDs.size() < dsIds.size()) {
            throw new EntityNotFoundException(DistributionSet.class, dsIds,
                    allDs.stream().map(DistributionSet::getId).toList());
        }

        final DistributionSetTag distributionSetTag = distributionSetTagManagement.get(dsTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, dsTagId));

        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, allDs);

        allDs.forEach(ds -> ds.addTag(distributionSetTag));

        final List<DistributionSet> result = Collections.unmodifiableList(distributionSetRepository.saveAll(allDs));

        // No reason to save the tag
        entityManager.detach(distributionSetTag);
        return result;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet unAssignTag(final long dsId, final long dsTagId) {
        final JpaDistributionSet set = (JpaDistributionSet) getWithDetails(dsId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, dsId));

        final DistributionSetTag distributionSetTag = distributionSetTagManagement.get(dsTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, dsTagId));

        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, set);

        set.removeTag(distributionSetTag);

        final JpaDistributionSet result = distributionSetRepository.save(set);

        // No reason to save the tag
        entityManager.detach(distributionSetTag);
        return result;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long setId) {
        final JpaDistributionSet ds = getDistributionSetOrThrowExceptionIfNotFound(setId);
        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, ds);

        delete(Collections.singletonList(setId));
    }

    private JpaDistributionSet getDistributionSetOrThrowExceptionIfNotFound(final Long setId) {
        return distributionSetRepository
                .findOne(distributionSetAccessController.appendAccessRules(AccessController.Operation.READ,
                        DistributionSetSpecification.byId(setId)))
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, setId));
    }

    @Override
    public List<DistributionSet> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(getDistributionSets(ids));
    }

    public List<JpaDistributionSet> getDistributionSets(final Collection<Long> ids) {
        final Specification<JpaDistributionSet> specification = distributionSetAccessController
                .appendAccessRules(AccessController.Operation.READ, DistributionSetSpecification.byIds(ids));
        return distributionSetRepository.findAll(specification);
    }

    @Override
    public Page<DistributionSet> findByTag(final Pageable pageable, final long tagId) {
        throwEntityNotFoundExceptionIfDsTagDoesNotExist(tagId);

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, pageable,
                Arrays.asList(DistributionSetSpecification.hasTag(tagId),
                        distributionSetAccessController.getAccessRules(AccessController.Operation.READ)));
    }

    private void throwEntityNotFoundExceptionIfDsTagDoesNotExist(final Long tagId) {
        if (!distributionSetTagRepository.existsById(tagId)) {
            throw new EntityNotFoundException(DistributionSetTag.class, tagId);
        }
    }

    @Override
    public Page<DistributionSet> findByRsqlAndTag(final Pageable pageable, final String rsqlParam, final long tagId) {
        throwEntityNotFoundExceptionIfDsTagDoesNotExist(tagId);

        final List<Specification<JpaDistributionSet>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetFields.class, virtualPropertyReplacer,
                        database),
                DistributionSetSpecification.hasTag(tagId), DistributionSetSpecification.isDeleted(false),
                distributionSetAccessController.getAccessRules(AccessController.Operation.READ));

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, pageable, specList);
    }

    @Override
    public Slice<DistributionSet> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, pageable,
                Arrays.asList(DistributionSetSpecification.isDeleted(false),
                        distributionSetAccessController.getAccessRules(AccessController.Operation.READ)));
    }

    @Override
    public Page<DistributionSet> findByRsql(final Pageable pageable, final String rsqlParam) {
        final List<Specification<JpaDistributionSet>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetFields.class, virtualPropertyReplacer,
                        database),
                DistributionSetSpecification.isDeleted(false),
                distributionSetAccessController.getAccessRules(AccessController.Operation.READ));

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, pageable, specList);
    }

    @Override
    public Optional<DistributionSet> get(final long id) {
        final Specification<JpaDistributionSet> specification = distributionSetAccessController
                .appendAccessRules(AccessController.Operation.READ, DistributionSetSpecification.byId(id));
        return distributionSetRepository.findOne(specification).map(d -> d);
    }

    @Override
    public boolean exists(final long id) {
        final Specification<JpaDistributionSet> specification = distributionSetAccessController
                .appendAccessRules(AccessController.Operation.READ, DistributionSetSpecification.byId(id));
        return distributionSetRepository.exists(specification);
    }

    @Override
    public DistributionSet getOrElseThrowException(final long id) {
        return getDistributionSetOrThrowExceptionIfNotFound(id);
    }

    @Override
    public DistributionSet getValidAndComplete(final long id) {
        final DistributionSet distributionSet = getValid(id);

        if (!distributionSet.isComplete()) {
            throw new IncompleteDistributionSetException("Distribution set of type "
                    + distributionSet.getType().getKey() + " is incomplete: " + distributionSet.getId());
        }

        return distributionSet;
    }

    @Override
    public DistributionSet getValid(final long id) {
        final DistributionSet distributionSet = getOrElseThrowException(id);

        if (!distributionSet.isValid()) {
            throw new InvalidDistributionSetException("Distribution set of type " + distributionSet.getType().getKey()
                    + " is invalid: " + distributionSet.getId());
        }

        return distributionSet;
    }

    @Override
    @Transactional
    public void invalidate(final DistributionSet set) {
        final JpaDistributionSet jpaSet = (JpaDistributionSet) set;
        distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, jpaSet);
        jpaSet.invalidate();
        distributionSetRepository.save(jpaSet);
    }

}

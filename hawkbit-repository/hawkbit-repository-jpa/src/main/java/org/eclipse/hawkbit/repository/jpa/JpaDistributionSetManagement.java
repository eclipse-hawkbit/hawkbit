/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetMetadataFields;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
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
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
            final AfterTransactionCommitExecutor afterCommit, final Database database) {
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
        this.database = database;
    }

    @Override
    public Optional<DistributionSet> getWithDetails(final long distid) {
        return distributionSetRepository.findOne(DistributionSetSpecification.byId(distid))
                .map(d -> (DistributionSet) d);
    }

    @Override
    public long countByTypeId(final long typeId) {
        if (!distributionSetTypeManagement.exists(typeId)) {
            throw new EntityNotFoundException(DistributionSetType.class, typeId);
        }

        return distributionSetRepository.countByTypeId(typeId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetTagAssignmentResult toggleTagAssignment(final Collection<Long> dsIds, final String tagName) {
        final List<JpaDistributionSet> sets = findDistributionSetListWithDetails(dsIds);

        if (sets.size() < dsIds.size()) {
            throw new EntityNotFoundException(DistributionSet.class, dsIds,
                    sets.stream().map(DistributionSet::getId).collect(Collectors.toList()));
        }

        final DistributionSetTag myTag = distributionSetTagManagement.getByName(tagName)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, tagName));

        DistributionSetTagAssignmentResult result;

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
        return distributionSetRepository.findAll(DistributionSetSpecification.byIds(distributionIdSet));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet update(final DistributionSetUpdate u) {
        final GenericDistributionSetUpdate update = (GenericDistributionSetUpdate) u;

        final JpaDistributionSet set = (JpaDistributionSet) getValid(update.getId());

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
        return softwareModuleRepository.findById(moduleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, moduleId));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> distributionSetIDs) {
        final List<DistributionSet> setsFound = get(distributionSetIDs);

        if (setsFound.size() < distributionSetIDs.size()) {
            throw new EntityNotFoundException(DistributionSet.class, distributionSetIDs,
                    setsFound.stream().map(DistributionSet::getId).collect(Collectors.toList()));
        }

        final List<Long> assigned = distributionSetRepository
                .findAssignedToTargetDistributionSetsById(distributionSetIDs);
        assigned.addAll(distributionSetRepository.findAssignedToRolloutDistributionSetsById(distributionSetIDs));

        // soft delete assigned
        if (!assigned.isEmpty()) {
            final Long[] dsIds = assigned.toArray(new Long[assigned.size()]);
            distributionSetRepository.deleteDistributionSet(dsIds);
            targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionType(dsIds);
        }

        // mark the rest as hard delete
        final List<Long> toHardDelete = distributionSetIDs.stream().filter(setId -> !assigned.contains(setId))
                .collect(Collectors.toList());

        // hard delete the rest if exists
        if (!toHardDelete.isEmpty()) {
            targetFilterQueryRepository
                    .unsetAutoAssignDistributionSetAndActionType(toHardDelete.toArray(new Long[toHardDelete.size()]));
            // don't give the delete statement an empty list, JPA/Oracle cannot
            // handle the empty list
            distributionSetRepository.deleteByIdIn(toHardDelete);
        }

        afterCommit.afterCommit(() -> distributionSetIDs.forEach(dsId -> eventPublisherHolder.getEventPublisher()
                .publishEvent(new DistributionSetDeletedEvent(tenantAware.getCurrentTenant(), dsId,
                        JpaDistributionSet.class.getName(), eventPublisherHolder.getApplicationId()))));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet create(final DistributionSetCreate c) {
        final JpaDistributionSetCreate create = (JpaDistributionSetCreate) c;
        if (create.getType() == null) {
            create.type(systemManagement.getTenantMetadata().getDefaultDsType().getKey());
        }

        return distributionSetRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> create(final Collection<DistributionSetCreate> creates) {
        return creates.stream().map(this::create).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet assignSoftwareModules(final long setId, final Collection<Long> moduleIds) {

        final Collection<JpaSoftwareModule> modules = softwareModuleRepository.findByIdIn(moduleIds);

        if (modules.size() < moduleIds.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, moduleIds,
                    modules.stream().map(SoftwareModule::getId).collect(Collectors.toList()));
        }

        assertDistributionSetIsNotAssignedToTargets(setId);

        final JpaDistributionSet set = (JpaDistributionSet) getValid(setId);

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

        assertDistributionSetIsNotAssignedToTargets(setId);

        set.removeModule(module);

        return distributionSetRepository.save(set);
    }

    @Override
    public Page<DistributionSet> findByDistributionSetFilter(final Pageable pageable,
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);
        return convertDsPage(findByCriteriaAPI(pageable, specList), pageable);
    }

    private static Page<DistributionSet> convertDsPage(final Page<JpaDistributionSet> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    private static Slice<DistributionSet> convertDsPage(final Slice<JpaDistributionSet> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, 0);
    }

    /**
     *
     * @param distributionSetFilter
     *            had details of filters to be applied
     * @return a single DistributionSet which is either installed or assigned to
     *         a specific target or {@code null}.
     */
    private Optional<JpaDistributionSet> findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);
        if (CollectionUtils.isEmpty(specList)) {
            return Optional.empty();
        }
        return distributionSetRepository.findOne(SpecificationsBuilder.combineWithAnd(specList));
    }

    @Override
    public Page<DistributionSet> findByCompleted(final Pageable pageReq, final Boolean complete) {

        List<Specification<JpaDistributionSet>> specList;
        if (complete != null) {
            specList = Arrays.asList(DistributionSetSpecification.isDeleted(false),
                    DistributionSetSpecification.isCompleted(complete));
        } else {
            specList = Collections.singletonList(DistributionSetSpecification.isDeleted(false));
        }

        return convertDsPage(findByCriteriaAPI(pageReq, specList), pageReq);
    }

    @Override
    public Page<DistributionSet> findByFilterAndAssignedInstalledDsOrderedByLinkTarget(final Pageable pageable,
            final DistributionSetFilterBuilder distributionSetFilterBuilder, final String assignedOrInstalled) {

        final DistributionSetFilter filterWithInstalledTargets = distributionSetFilterBuilder
                .setInstalledTargetId(assignedOrInstalled).setAssignedTargetId(null).build();
        final Optional<JpaDistributionSet> installedDS = findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
                filterWithInstalledTargets);

        final DistributionSetFilter filterWithAssignedTargets = distributionSetFilterBuilder.setInstalledTargetId(null)
                .setAssignedTargetId(assignedOrInstalled).build();
        final Optional<JpaDistributionSet> assignedDS = findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
                filterWithAssignedTargets);

        final DistributionSetFilter dsFilterWithNoTargetLinked = distributionSetFilterBuilder.setInstalledTargetId(null)
                .setAssignedTargetId(null).build();
        // first fine the distribution sets filtered by the given filter
        // parameters
        final Page<DistributionSet> findDistributionSetsByFilters = findByDistributionSetFilter(pageable,
                dsFilterWithNoTargetLinked);

        final List<DistributionSet> resultSet = new ArrayList<>(findDistributionSetsByFilters.getContent());
        int orderIndex = 0;
        if (installedDS.isPresent()) {
            final boolean remove = resultSet.remove(installedDS.get());
            if (!remove) {
                resultSet.remove(resultSet.size() - 1);
            }
            resultSet.add(orderIndex, installedDS.get());
            orderIndex++;
        }
        if (assignedDS.isPresent() && !assignedDS.equals(installedDS)) {
            final boolean remove = resultSet.remove(assignedDS.get());
            if (!remove) {
                resultSet.remove(resultSet.size() - 1);
            }
            resultSet.add(orderIndex, assignedDS.get());
        }

        return new PageImpl<>(resultSet, pageable, findDistributionSetsByFilters.getTotalElements());
    }

    @Override
    public Optional<DistributionSet> getByNameAndVersion(final String distributionName, final String version) {
        final Specification<JpaDistributionSet> spec = DistributionSetSpecification
                .equalsNameAndVersionIgnoreCase(distributionName, version);
        return distributionSetRepository.findOne(spec).map(d -> (DistributionSet) d);

    }

    @Override
    public long count() {
        final Specification<JpaDistributionSet> spec = DistributionSetSpecification.isDeleted(Boolean.FALSE);

        return distributionSetRepository.count(SpecificationsBuilder.combineWithAnd(Arrays.asList(spec)));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSetMetadata> createMetaData(final long dsId, final Collection<MetaData> md) {

        md.forEach(meta -> checkAndThrowIfDistributionSetMetadataAlreadyExists(
                new DsMetadataCompositeKey(dsId, meta.getKey())));

        assertMetaDataQuota(dsId, md.size());

        final JpaDistributionSet set = touch(dsId);

        return Collections.unmodifiableList(md.stream()
                .map(meta -> distributionSetMetadataRepository
                        .save(new JpaDistributionSetMetadata(meta.getKey(), set, meta.getValue())))
                .collect(Collectors.toList()));
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

        // check if exists otherwise throw entity not found exception
        final JpaDistributionSetMetadata toUpdate = (JpaDistributionSetMetadata) getMetaDataByDistributionSetId(dsId,
                md.getKey()).orElseThrow(
                        () -> new EntityNotFoundException(DistributionSetMetadata.class, dsId, md.getKey()));
        toUpdate.setValue(md.getValue());
        // touch it to update the lock revision because we are modifying the
        // DS indirectly
        touch(dsId);
        return distributionSetMetadataRepository.save(toUpdate);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetaData(final long distributionSetId, final String key) {
        final JpaDistributionSetMetadata metadata = (JpaDistributionSetMetadata) getMetaDataByDistributionSetId(
                distributionSetId, key).orElseThrow(
                        () -> new EntityNotFoundException(DistributionSetMetadata.class, distributionSetId, key));

        touch(metadata.getDistributionSet());
        distributionSetMetadataRepository.deleteById(metadata.getId());
    }

    /**
     * Method to get the latest distribution set based on DS ID after the
     * metadata changes for that distribution set.
     *
     * @param ds
     *            is the DS to touch
     */
    private JpaDistributionSet touch(final DistributionSet ds) {

        // merge base distribution set so optLockRevision gets updated and audit
        // log written because modifying metadata is modifying the base
        // distribution set itself for auditing purposes.
        final JpaDistributionSet result = entityManager.merge((JpaDistributionSet) ds);
        result.setLastModifiedAt(0L);

        return distributionSetRepository.save(result);
    }

    /**
     * Method to get the latest distribution set based on DS ID after the
     * metadata changes for that distribution set.
     *
     * @param distId
     *            of the DS to touch
     */
    private JpaDistributionSet touch(final Long distId) {
        return touch(getOrElseThrowException(distId));
    }

    @Override
    public Page<DistributionSetMetadata> findMetaDataByDistributionSetId(final Pageable pageable,
            final long distributionSetId) {
        throwExceptionIfDistributionSetDoesNotExist(distributionSetId);

        return convertMdPage(distributionSetMetadataRepository
                .findAll((Specification<JpaDistributionSetMetadata>) (root, query, cb) -> cb.equal(
                        root.get(JpaDistributionSetMetadata_.distributionSet).get(JpaDistributionSet_.id),
                        distributionSetId), pageable),
                pageable);
    }

    @Override
    public Page<DistributionSetMetadata> findMetaDataByDistributionSetIdAndRsql(final Pageable pageable,
            final long distributionSetId, final String rsqlParam) {

        throwExceptionIfDistributionSetDoesNotExist(distributionSetId);

        final Specification<JpaDistributionSetMetadata> spec = RSQLUtility.buildRsqlSpecification(rsqlParam,
                DistributionSetMetadataFields.class, virtualPropertyReplacer, database);

        return convertMdPage(
                distributionSetMetadataRepository
                        .findAll((Specification<JpaDistributionSetMetadata>) (root, query, cb) -> cb.and(
                                cb.equal(root.get(JpaDistributionSetMetadata_.distributionSet)
                                        .get(JpaDistributionSet_.id), distributionSetId),
                                spec.toPredicate(root, query, cb)), pageable),
                pageable);
    }

    private static Page<DistributionSetMetadata> convertMdPage(final Page<JpaDistributionSetMetadata> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Optional<DistributionSetMetadata> getMetaDataByDistributionSetId(final long setId, final String key) {
        throwExceptionIfDistributionSetDoesNotExist(setId);

        return distributionSetMetadataRepository.findById(new DsMetadataCompositeKey(setId, key))
                .map(dmd -> (DistributionSetMetadata) dmd);
    }

    @Override
    public Optional<DistributionSet> getByAction(final long actionId) {
        if (!actionRepository.existsById(actionId)) {
            throw new EntityNotFoundException(Action.class, actionId);
        }

        return Optional.ofNullable(distributionSetRepository.findByActionId(actionId));
    }

    @Override
    public boolean isInUse(final long setId) {
        throwExceptionIfDistributionSetDoesNotExist(setId);

        return actionRepository.countByDistributionSetId(setId) > 0;
    }

    private static List<Specification<JpaDistributionSet>> buildDistributionSetSpecifications(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = Lists.newArrayListWithExpectedSize(9);

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

        if (distributionSetFilter.getType() != null) {
            spec = DistributionSetSpecification.byType(distributionSetFilter.getType());
            specList.add(spec);
        }

        if (!StringUtils.isEmpty(distributionSetFilter.getSearchText())) {
            spec = DistributionSetSpecification.likeNameOrDescriptionOrVersion(distributionSetFilter.getSearchText());
            specList.add(spec);
        }

        if (!StringUtils.isEmpty(distributionSetFilter.getFilterString())) {
            final String[] dsFilterNameAndVersionEntries = getDsFilterNameAndVersionEntries(
                    distributionSetFilter.getFilterString().trim());
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

    // the format of filter string is 'name:version'. 'name' and 'version'
    // fields follow the starts_with semantic, that changes to equal for 'name'
    // field when the semicolon is present
    private static String[] getDsFilterNameAndVersionEntries(final String filterString) {
        final int semicolonIndex = filterString.indexOf(':');

        final String dsFilterName = semicolonIndex != -1 ? filterString.substring(0, semicolonIndex)
                : (filterString + "%");
        final String dsFilterVersion = semicolonIndex != -1 ? (filterString.substring(semicolonIndex + 1) + "%") : "%";

        return new String[] { !StringUtils.isEmpty(dsFilterName) ? dsFilterName : "%", dsFilterVersion };
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

    /**
     * executes findAll with the given {@link DistributionSet}
     * {@link Specification}s.
     *
     * @param pageable
     *            paging parameter
     * @param specList
     *            list of @link {@link Specification}
     * @return the page with the found {@link DistributionSet}
     */
    private Page<JpaDistributionSet> findByCriteriaAPI(final Pageable pageable,
            final List<Specification<JpaDistributionSet>> specList) {

        if (CollectionUtils.isEmpty(specList)) {
            return distributionSetRepository.findAll(pageable);
        }

        return distributionSetRepository.findAll(SpecificationsBuilder.combineWithAnd(specList), pageable);
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
                    allDs.stream().map(DistributionSet::getId).collect(Collectors.toList()));
        }

        final DistributionSetTag distributionSetTag = distributionSetTagManagement.get(dsTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, dsTagId));

        allDs.forEach(ds -> ds.addTag(distributionSetTag));

        final List<DistributionSet> result = Collections
                .unmodifiableList(allDs.stream().map(distributionSetRepository::save).collect(Collectors.toList()));

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
        throwExceptionIfDistributionSetDoesNotExist(setId);

        delete(Collections.singletonList(setId));
    }

    private void throwExceptionIfDistributionSetDoesNotExist(final Long setId) {
        if (!distributionSetRepository.existsById(setId)) {
            throw new EntityNotFoundException(DistributionSet.class, setId);
        }
    }

    @Override
    public List<DistributionSet> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(distributionSetRepository.findAllById(ids));
    }

    @Override
    public Page<DistributionSet> findByTag(final Pageable pageable, final long tagId) {
        throwEntityNotFoundExceptionIfDsTagDoesNotExist(tagId);

        return convertDsPage(distributionSetRepository.findByTag(pageable, tagId), pageable);

    }

    private void throwEntityNotFoundExceptionIfDsTagDoesNotExist(final Long tagId) {
        if (!distributionSetTagRepository.existsById(tagId)) {
            throw new EntityNotFoundException(DistributionSetTag.class, tagId);
        }
    }

    @Override
    public Page<DistributionSet> findByRsqlAndTag(final Pageable pageable, final String rsqlParam, final long tagId) {
        throwEntityNotFoundExceptionIfDsTagDoesNotExist(tagId);

        final Specification<JpaDistributionSet> spec = RSQLUtility.buildRsqlSpecification(rsqlParam,
                DistributionSetFields.class, virtualPropertyReplacer, database);

        return convertDsPage(findByCriteriaAPI(pageable, Arrays.asList(spec, DistributionSetSpecification.hasTag(tagId),
                DistributionSetSpecification.isDeleted(false))), pageable);
    }

    @Override
    public Slice<DistributionSet> findAll(final Pageable pageable) {
        return convertDsPage(
                distributionSetRepository.findAllWithoutCount(DistributionSetSpecification.isDeleted(false), pageable),
                pageable);
    }

    @Override
    public Page<DistributionSet> findByRsql(final Pageable pageable, final String rsqlParam) {
        final Specification<JpaDistributionSet> spec = RSQLUtility.buildRsqlSpecification(rsqlParam,
                DistributionSetFields.class, virtualPropertyReplacer, database);

        return convertDsPage(
                findByCriteriaAPI(pageable, Arrays.asList(spec, DistributionSetSpecification.isDeleted(false))),
                pageable);
    }

    @Override
    public Optional<DistributionSet> get(final long id) {
        return distributionSetRepository.findById(id).map(d -> d);
    }

    @Override
    public boolean exists(final long id) {
        return distributionSetRepository.existsById(id);
    }

    @Override
    public DistributionSet getOrElseThrowException(final long id) {
        return get(id).orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, id));
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

}

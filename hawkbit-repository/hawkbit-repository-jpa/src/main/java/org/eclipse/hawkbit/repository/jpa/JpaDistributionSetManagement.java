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
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetUpdate;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
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
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DistributionSetRepository distributionSetRepository;

    @Autowired
    private TagManagement tagManagement;

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private DistributionSetTypeManagement distributionSetTypeManagement;

    @Autowired
    private DistributionSetMetadataRepository distributionSetMetadataRepository;

    @Autowired
    private TargetFilterQueryRepository targetFilterQueryRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Autowired
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private DistributionSetTagRepository distributionSetTagRepository;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    @Override
    public Optional<DistributionSet> findDistributionSetByIdWithDetails(final Long distid) {
        return Optional.ofNullable(distributionSetRepository.findOne(DistributionSetSpecification.byId(distid)));
    }

    @Override
    public Optional<DistributionSet> findDistributionSetById(final Long distid) {
        return Optional.ofNullable(distributionSetRepository.findOne(distid));
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

        final DistributionSetTag myTag = tagManagement.findDistributionSetTag(tagName)
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
            result = new DistributionSetTagAssignmentResult(dsIds.size() - toBeChangedDSs.size(), 0,
                    toBeChangedDSs.size(), Collections.emptyList(),
                    Collections.unmodifiableList(
                            toBeChangedDSs.stream().map(distributionSetRepository::save).collect(Collectors.toList())),
                    myTag);
        } else {
            result = new DistributionSetTagAssignmentResult(dsIds.size() - toBeChangedDSs.size(), toBeChangedDSs.size(),
                    0,
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
    public DistributionSet updateDistributionSet(final DistributionSetUpdate u) {
        final GenericDistributionSetUpdate update = (GenericDistributionSetUpdate) u;

        final JpaDistributionSet set = findDistributionSetAndThrowExceptionIfNotFound(update.getId());

        update.getName().ifPresent(set::setName);
        update.getDescription().ifPresent(set::setDescription);
        update.getVersion().ifPresent(set::setVersion);

        if (update.isRequiredMigrationStep() != null
                && !update.isRequiredMigrationStep().equals(set.isRequiredMigrationStep())) {
            checkDistributionSetIsAssignedToTargets(update.getId());
            set.setRequiredMigrationStep(update.isRequiredMigrationStep());
        }

        if (update.getType() != null) {
            final DistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(update.getType());
            if (!type.getId().equals(set.getType().getId())) {
                checkDistributionSetIsAssignedToTargets(update.getId());

                set.setType(type);
            }
        }

        return distributionSetRepository.save(set);
    }

    private JpaDistributionSetType findDistributionSetTypeAndThrowExceptionIfNotFound(final String key) {
        return (JpaDistributionSetType) distributionSetTypeManagement.findDistributionSetTypeByKey(key)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, key));

    }

    private JpaDistributionSet findDistributionSetAndThrowExceptionIfNotFound(final Long setId) {
        return (JpaDistributionSet) findDistributionSetById(setId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, setId));
    }

    private JpaSoftwareModule findSoftwareModuleAndThrowExceptionIfNotFound(final Long moduleId) {
        return Optional.ofNullable(softwareModuleRepository.findOne(moduleId))
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, moduleId));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteDistributionSet(final Collection<Long> distributionSetIDs) {
        final List<DistributionSet> setsFound = findDistributionSetsById(distributionSetIDs);

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
            targetFilterQueryRepository.unsetAutoAssignDistributionSet(dsIds);
        }

        // mark the rest as hard delete
        final List<Long> toHardDelete = distributionSetIDs.stream().filter(setId -> !assigned.contains(setId))
                .collect(Collectors.toList());

        // hard delete the rest if exists
        if (!toHardDelete.isEmpty()) {
            // don't give the delete statement an empty list, JPA/Oracle cannot
            // handle the empty list
            distributionSetRepository.deleteByIdIn(toHardDelete);
        }

        afterCommit.afterCommit(() -> distributionSetIDs.forEach(
                dsId -> eventPublisher.publishEvent(new DistributionSetDeletedEvent(tenantAware.getCurrentTenant(),
                        dsId, JpaDistributionSet.class.getName(), applicationContext.getId()))));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet createDistributionSet(final DistributionSetCreate c) {
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
    public List<DistributionSet> createDistributionSets(final Collection<DistributionSetCreate> creates) {
        return creates.stream().map(this::createDistributionSet).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet assignSoftwareModules(final Long setId, final Collection<Long> moduleIds) {

        final Collection<JpaSoftwareModule> modules = softwareModuleRepository.findByIdIn(moduleIds);

        if (modules.size() < moduleIds.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, moduleIds,
                    modules.stream().map(SoftwareModule::getId).collect(Collectors.toList()));
        }

        checkDistributionSetIsAssignedToTargets(setId);

        final JpaDistributionSet set = findDistributionSetAndThrowExceptionIfNotFound(setId);
        modules.forEach(set::addModule);

        return distributionSetRepository.save(set);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet unassignSoftwareModule(final Long setId, final Long moduleId) {
        final JpaDistributionSet set = findDistributionSetAndThrowExceptionIfNotFound(setId);
        final JpaSoftwareModule module = findSoftwareModuleAndThrowExceptionIfNotFound(moduleId);

        checkDistributionSetIsAssignedToTargets(setId);

        set.removeModule(module);

        return distributionSetRepository.save(set);
    }

    @Override
    public Page<DistributionSet> findDistributionSetsByFilters(final Pageable pageable,
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);
        return convertDsPage(findByCriteriaAPI(pageable, specList), pageable);
    }

    private static Page<DistributionSet> convertDsPage(final Page<JpaDistributionSet> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    /**
     *
     * @param distributionSetFilter
     *            had details of filters to be applied
     * @return a single DistributionSet which is either installed or assigned to
     *         a specific target or {@code null}.
     */
    private DistributionSet findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);
        if (CollectionUtils.isEmpty(specList)) {
            return null;
        }
        return distributionSetRepository.findOne(SpecificationsBuilder.combineWithAnd(specList));
    }

    @Override
    public Page<DistributionSet> findDistributionSetsByDeletedAndOrCompleted(final Pageable pageReq,
            final Boolean deleted, final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specList = new ArrayList<>(2);

        if (deleted != null) {
            final Specification<JpaDistributionSet> spec = DistributionSetSpecification.isDeleted(deleted);
            specList.add(spec);
        }

        if (complete != null) {
            final Specification<JpaDistributionSet> spec = DistributionSetSpecification.isCompleted(complete);
            specList.add(spec);
        }

        return convertDsPage(findByCriteriaAPI(pageReq, specList), pageReq);
    }

    @Override
    public Page<DistributionSet> findDistributionSetsAll(final String rsqlParam, final Pageable pageReq,
            final Boolean deleted) {

        final Specification<JpaDistributionSet> spec = RSQLUtility.parse(rsqlParam, DistributionSetFields.class,
                virtualPropertyReplacer);

        final List<Specification<JpaDistributionSet>> specList = new ArrayList<>(2);
        if (deleted != null) {
            specList.add(DistributionSetSpecification.isDeleted(deleted));
        }
        specList.add(spec);

        return convertDsPage(findByCriteriaAPI(pageReq, specList), pageReq);
    }

    @Override
    public Page<DistributionSet> findDistributionSetsAll(final Pageable pageReq, final Boolean deleted) {

        final List<Specification<JpaDistributionSet>> specList = new ArrayList<>(1);
        if (deleted != null) {
            specList.add(DistributionSetSpecification.isDeleted(deleted));
        }

        return convertDsPage(findByCriteriaAPI(pageReq, specList), pageReq);
    }

    @Override
    public Page<DistributionSet> findDistributionSetsAllOrderedByLinkTarget(final Pageable pageable,
            final DistributionSetFilterBuilder distributionSetFilterBuilder, final String assignedOrInstalled) {

        final DistributionSetFilter filterWithInstalledTargets = distributionSetFilterBuilder
                .setInstalledTargetId(assignedOrInstalled).setAssignedTargetId(null).build();
        final DistributionSet installedDS = findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
                filterWithInstalledTargets);

        final DistributionSetFilter filterWithAssignedTargets = distributionSetFilterBuilder.setInstalledTargetId(null)
                .setAssignedTargetId(assignedOrInstalled).build();
        final DistributionSet assignedDS = findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
                filterWithAssignedTargets);

        final DistributionSetFilter dsFilterWithNoTargetLinked = distributionSetFilterBuilder.setInstalledTargetId(null)
                .setAssignedTargetId(null).build();
        // first fine the distribution sets filtered by the given filter
        // parameters
        final Page<DistributionSet> findDistributionSetsByFilters = findDistributionSetsByFilters(pageable,
                dsFilterWithNoTargetLinked);

        final List<DistributionSet> resultSet = new ArrayList<>(findDistributionSetsByFilters.getContent());
        int orderIndex = 0;
        if (installedDS != null) {
            final boolean remove = resultSet.remove(installedDS);
            if (!remove) {
                resultSet.remove(resultSet.size() - 1);
            }
            resultSet.add(orderIndex, installedDS);
            orderIndex++;
        }
        if (assignedDS != null && !assignedDS.equals(installedDS)) {
            final boolean remove = resultSet.remove(assignedDS);
            if (!remove) {
                resultSet.remove(resultSet.size() - 1);
            }
            resultSet.add(orderIndex, assignedDS);
        }

        return new PageImpl<>(resultSet, pageable, findDistributionSetsByFilters.getTotalElements());
    }

    @Override
    public Optional<DistributionSet> findDistributionSetByNameAndVersion(final String distributionName,
            final String version) {
        final Specification<JpaDistributionSet> spec = DistributionSetSpecification
                .equalsNameAndVersionIgnoreCase(distributionName, version);
        return Optional.ofNullable(distributionSetRepository.findOne(spec));

    }

    @Override
    public Long countDistributionSetsAll() {
        final Specification<JpaDistributionSet> spec = DistributionSetSpecification.isDeleted(Boolean.FALSE);

        return distributionSetRepository.count(SpecificationsBuilder.combineWithAnd(Arrays.asList(spec)));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSetMetadata> createDistributionSetMetadata(final Long dsId, final Collection<MetaData> md) {

        md.forEach(meta -> checkAndThrowAlreadyIfDistributionSetMetadataExists(
                new DsMetadataCompositeKey(dsId, meta.getKey())));

        final JpaDistributionSet set = touch(dsId);

        return Collections.unmodifiableList(md.stream()
                .map(meta -> distributionSetMetadataRepository
                        .save(new JpaDistributionSetMetadata(meta.getKey(), set, meta.getValue())))
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetMetadata updateDistributionSetMetadata(final Long dsId, final MetaData md) {

        // check if exists otherwise throw entity not found exception
        final JpaDistributionSetMetadata toUpdate = (JpaDistributionSetMetadata) findDistributionSetMetadata(dsId,
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
    public void deleteDistributionSetMetadata(final Long distributionSetId, final String key) {
        final JpaDistributionSetMetadata metadata = (JpaDistributionSetMetadata) findDistributionSetMetadata(
                distributionSetId, key).orElseThrow(
                        () -> new EntityNotFoundException(DistributionSetMetadata.class, distributionSetId, key));

        touch(metadata.getDistributionSet());
        distributionSetMetadataRepository.delete(metadata.getId());
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
        // log written because
        // modifying metadata is modifying the base distribution set itself for
        // auditing purposes.
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
        return touch(findDistributionSetById(distId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, distId)));
    }

    @Override
    public Page<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(final Long distributionSetId,
            final Pageable pageable) {
        throwExceptionIfDistributionSetDoesNotExist(distributionSetId);

        return convertMdPage(distributionSetMetadataRepository
                .findAll((Specification<JpaDistributionSetMetadata>) (root, query, cb) -> cb.equal(
                        root.get(JpaDistributionSetMetadata_.distributionSet).get(JpaDistributionSet_.id),
                        distributionSetId), pageable),
                pageable);
    }

    @Override
    public Page<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(final Long distributionSetId,
            final String rsqlParam, final Pageable pageable) {

        throwExceptionIfDistributionSetDoesNotExist(distributionSetId);

        final Specification<JpaDistributionSetMetadata> spec = RSQLUtility.parse(rsqlParam,
                DistributionSetMetadataFields.class, virtualPropertyReplacer);

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
    public Optional<DistributionSetMetadata> findDistributionSetMetadata(final Long setId, final String key) {
        throwExceptionIfDistributionSetDoesNotExist(setId);

        return Optional.ofNullable(distributionSetMetadataRepository.findOne(new DsMetadataCompositeKey(setId, key)));
    }

    @Override
    public Optional<DistributionSet> findDistributionSetByAction(final Long actionId) {
        if (!actionRepository.exists(actionId)) {
            throw new EntityNotFoundException(Action.class, actionId);
        }

        return Optional.ofNullable(distributionSetRepository.findByActionId(actionId));
    }

    @Override
    public boolean isDistributionSetInUse(final Long setId) {
        throwExceptionIfDistributionSetDoesNotExist(setId);

        return actionRepository.countByDistributionSetId(setId) > 0;
    }

    private static List<Specification<JpaDistributionSet>> buildDistributionSetSpecifications(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = Lists.newArrayListWithExpectedSize(7);

        Specification<JpaDistributionSet> spec;

        if (distributionSetFilter.getIsComplete() != null) {
            spec = DistributionSetSpecification.isCompleted(distributionSetFilter.getIsComplete());
            specList.add(spec);
        }

        if (distributionSetFilter.getIsDeleted() != null) {
            spec = DistributionSetSpecification.isDeleted(distributionSetFilter.getIsDeleted());
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

        if (isDSWithNoTagSelected(distributionSetFilter) || isTagsSelected(distributionSetFilter)) {
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

    private void checkDistributionSetIsAssignedToTargets(final Long distributionSet) {
        if (actionRepository.countByDistributionSetId(distributionSet) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "distribution set %s is already assigned to targets and cannot be changed", distributionSet));
        }
    }

    private static Boolean isDSWithNoTagSelected(final DistributionSetFilter distributionSetFilter) {
        return distributionSetFilter.getSelectDSWithNoTag() != null && distributionSetFilter.getSelectDSWithNoTag();
    }

    private static Boolean isTagsSelected(final DistributionSetFilter distributionSetFilter) {
        return !CollectionUtils.isEmpty(distributionSetFilter.getTagNames());
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

    private void checkAndThrowAlreadyIfDistributionSetMetadataExists(final DsMetadataCompositeKey metadataId) {
        if (distributionSetMetadataRepository.exists(metadataId)) {
            throw new EntityAlreadyExistsException(
                    "Metadata entry with key '" + metadataId.getKey() + "' already exists");
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> assignTag(final Collection<Long> dsIds, final Long dsTagId) {
        final List<JpaDistributionSet> allDs = findDistributionSetListWithDetails(dsIds);

        if (allDs.size() < dsIds.size()) {
            throw new EntityNotFoundException(DistributionSet.class, dsIds,
                    allDs.stream().map(DistributionSet::getId).collect(Collectors.toList()));
        }

        final DistributionSetTag distributionSetTag = tagManagement.findDistributionSetTagById(dsTagId)
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
    public DistributionSet unAssignTag(final Long dsId, final Long dsTagId) {
        final JpaDistributionSet set = (JpaDistributionSet) findDistributionSetByIdWithDetails(dsId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, dsId));

        final DistributionSetTag distributionSetTag = tagManagement.findDistributionSetTagById(dsTagId)
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
    public void deleteDistributionSet(final Long setId) {
        throwExceptionIfDistributionSetDoesNotExist(setId);

        deleteDistributionSet(Arrays.asList(setId));
    }

    private void throwExceptionIfDistributionSetDoesNotExist(final Long setId) {
        if (!distributionSetRepository.exists(setId)) {
            throw new EntityNotFoundException(DistributionSet.class, setId);
        }
    }

    @Override
    public List<DistributionSet> findDistributionSetsById(final Collection<Long> ids) {
        return Collections.unmodifiableList(distributionSetRepository.findAll(ids));
    }

    @Override
    public Page<DistributionSet> findDistributionSetsByTag(final Pageable pageable, final Long tagId) {
        throwEntityNotFoundExceptionIfDsTagDoesNotExist(tagId);

        return convertDsPage(distributionSetRepository.findByTag(pageable, tagId), pageable);

    }

    private void throwEntityNotFoundExceptionIfDsTagDoesNotExist(final Long tagId) {
        if (!distributionSetTagRepository.exists(tagId)) {
            throw new EntityNotFoundException(DistributionSetTag.class, tagId);
        }
    }

    @Override
    public Page<DistributionSet> findDistributionSetsByTag(final Pageable pageable, final String rsqlParam,
            final Long tagId) {
        throwEntityNotFoundExceptionIfDsTagDoesNotExist(tagId);

        final Specification<JpaDistributionSet> spec = RSQLUtility.parse(rsqlParam, DistributionSetFields.class,
                virtualPropertyReplacer);

        return convertDsPage(distributionSetRepository.findAll((Specification<JpaDistributionSet>) (root, query,
                cb) -> cb.and(DistributionSetSpecification.hasTag(tagId).toPredicate(root, query, cb),
                        spec.toPredicate(root, query, cb)),
                pageable), pageable);
    }

}

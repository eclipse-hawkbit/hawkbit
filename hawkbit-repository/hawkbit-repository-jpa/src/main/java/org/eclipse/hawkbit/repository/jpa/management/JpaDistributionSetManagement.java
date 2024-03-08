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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetMetadataFields;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.DeletedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetMetadataRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetFilterQueryRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
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
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.IMPLICIT_LOCK_ENABLED;

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
    private final TargetRepository targetRepository;
    private final TargetFilterQueryRepository targetFilterQueryRepository;
    private final ActionRepository actionRepository;
    private final TenantConfigHelper tenantConfigHelper;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final SoftwareModuleRepository softwareModuleRepository;
    private final DistributionSetTagRepository distributionSetTagRepository;
    private final Database database;
    private final RepositoryProperties repositoryProperties;

    public JpaDistributionSetManagement(
            final EntityManager entityManager,
            final DistributionSetRepository distributionSetRepository,
            final DistributionSetTagManagement distributionSetTagManagement, final SystemManagement systemManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement, final QuotaManagement quotaManagement,
            final DistributionSetMetadataRepository distributionSetMetadataRepository,
            final TargetRepository targetRepository,
            final TargetFilterQueryRepository targetFilterQueryRepository, final ActionRepository actionRepository,
            final TenantConfigHelper tenantConfigHelper,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final SoftwareModuleRepository softwareModuleRepository,
            final DistributionSetTagRepository distributionSetTagRepository,
            final Database database,
            final RepositoryProperties repositoryProperties) {
        this.entityManager = entityManager;
        this.distributionSetRepository = distributionSetRepository;
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.systemManagement = systemManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.quotaManagement = quotaManagement;
        this.distributionSetMetadataRepository = distributionSetMetadataRepository;
        this.targetRepository = targetRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.actionRepository = actionRepository;
        this.tenantConfigHelper = tenantConfigHelper;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.softwareModuleRepository = softwareModuleRepository;
        this.distributionSetTagRepository = distributionSetTagRepository;
        this.database = database;
        this.repositoryProperties = repositoryProperties;
    }

    @Override
    public Optional<DistributionSet> getWithDetails(final long id) {
        return distributionSetRepository.findById(id).map(DistributionSet.class::cast);
    }

    @Override
    public long countByTypeId(final long typeId) {
        if (!distributionSetTypeManagement.exists(typeId)) {
            throw new EntityNotFoundException(DistributionSetType.class, typeId);
        }

        return distributionSetRepository.count(DistributionSetSpecification.byType(typeId));
    }

    @Override
    public List<Statistic> countRolloutsByStatusForDistributionSet(final Long id) {
        assertDistributionSetExists(id);

        return distributionSetRepository.countRolloutsByStatusForDistributionSet(id).stream()
                .map(Statistic.class::cast).toList();
    }

    @Override
    public List<Statistic> countActionsByStatusForDistributionSet(final Long id) {
        assertDistributionSetExists(id);

        return distributionSetRepository.countActionsByStatusForDistributionSet(id).stream()
                .map(Statistic.class::cast).toList();
    }

    @Override
    public Long countAutoAssignmentsForDistributionSet(final Long id) {
        assertDistributionSetExists(id);

        return distributionSetRepository.countAutoAssignmentsForDistributionSet(id);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetTagAssignmentResult toggleTagAssignment(final Collection<Long> ids, final String tagName) {
        return updateTags(
                ids,
                () -> distributionSetTagManagement
                        .getByName(tagName)
                        .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, tagName)),
                (allDs, distributionSetTag) -> {
                    final List<JpaDistributionSet> toBeChangedDSs = allDs.stream().filter(set -> set.addTag(distributionSetTag))
                            .collect(Collectors.toList());

                    final DistributionSetTagAssignmentResult result;
                    // un-assignment case
                    if (toBeChangedDSs.isEmpty()) {
                        for (final JpaDistributionSet set : allDs) {
                            if (set.removeTag(distributionSetTag)) {
                                toBeChangedDSs.add(set);
                            }
                        }
                        result = new DistributionSetTagAssignmentResult(ids.size() - toBeChangedDSs.size(),
                                Collections.emptyList(),
                                Collections.unmodifiableList(
                                        toBeChangedDSs.stream().map(distributionSetRepository::save).collect(Collectors.toList())),
                                distributionSetTag);
                    } else {
                        result = new DistributionSetTagAssignmentResult(ids.size() - toBeChangedDSs.size(),
                                Collections.unmodifiableList(
                                        toBeChangedDSs.stream().map(distributionSetRepository::save).collect(Collectors.toList())),
                                Collections.emptyList(), distributionSetTag);
                    }
                    return result;
                });
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> assignTag(final Collection<Long> ids, final long dsTagId) {
        return updateTags(
                ids,
                () -> distributionSetTagManagement.get(dsTagId).orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, dsTagId)),
                (allDs, distributionSetTag) -> {
                    allDs.forEach(ds -> ds.addTag(distributionSetTag));
                    return Collections.unmodifiableList(distributionSetRepository.saveAll(allDs));
                });
    }

    private <T> T updateTags(
            final Collection<Long> dsIds, final Supplier<DistributionSetTag> tagSupplier,
            final BiFunction<List<JpaDistributionSet>, DistributionSetTag, T> updater) {
        final List<JpaDistributionSet> allDs = findDistributionSetListWithDetails(dsIds);
        if (allDs.size() < dsIds.size()) {
            throw new EntityNotFoundException(DistributionSet.class, dsIds,
                    allDs.stream().map(DistributionSet::getId).toList());
        }

        final DistributionSetTag distributionSetTag = tagSupplier.get();
        try {
            return updater.apply(allDs, distributionSetTag);
        } finally {
            // No reason to save the tag
            entityManager.detach(distributionSetTag);
        }
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

        // lock/unlock ONLY if locked flag is present!
        if (Boolean.TRUE.equals(update.locked())) {
            set.lock();
        } else if (Boolean.FALSE.equals(update.locked())) {
            set.unlock();
        }

        if (update.isRequiredMigrationStep() != null
                && !update.isRequiredMigrationStep().equals(set.isRequiredMigrationStep())) {
            assertDistributionSetIsNotAssignedToTargets(update.getId());
            set.setRequiredMigrationStep(update.isRequiredMigrationStep());
        }

        return distributionSetRepository.save(set);
    }

    private JpaSoftwareModule findSoftwareModuleAndThrowExceptionIfNotFound(final Long softwareModuleId) {
        return softwareModuleRepository.findById(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> distributionSetIDs) {
        getDistributionSets(distributionSetIDs); // throws EntityNotFoundException if any of these do not exists
        final List<JpaDistributionSet> setsFound = distributionSetRepository.findAll(
                AccessController.Operation.DELETE, distributionSetRepository.byIdsSpec(distributionSetIDs));
        if (setsFound.size() < distributionSetIDs.size()) {
            throw new InsufficientPermissionException("No DELETE access to some of distribution sets!");
        }

        final List<Long> assigned = distributionSetRepository
                .findAssignedToTargetDistributionSetsById(distributionSetIDs);
        assigned.addAll(distributionSetRepository.findAssignedToRolloutDistributionSetsById(distributionSetIDs));

        // soft delete assigned
        if (!assigned.isEmpty()) {
            final Long[] dsIds = assigned.toArray(new Long[0]);
            distributionSetRepository.deleteDistributionSet(dsIds);
            targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(dsIds);
        }

        // mark the rest as hard delete
        final List<Long> toHardDelete = distributionSetIDs.stream().filter(setId -> !assigned.contains(setId)).toList();

        // hard delete the rest if exists
        if (!toHardDelete.isEmpty()) {
            targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(
                    toHardDelete.toArray(new Long[0]));
            // don't give the delete statement an empty list, JPA/Oracle cannot
            // handle the empty list
            distributionSetRepository.deleteAllById(toHardDelete);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet create(final DistributionSetCreate c) {
        final JpaDistributionSetCreate create = (JpaDistributionSetCreate) c;
        setDefaultTypeIfMissing(create);

        return distributionSetRepository.save(AccessController.Operation.CREATE, create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> create(final Collection<DistributionSetCreate> creates) {
        final List<JpaDistributionSet> toCreate = creates.stream().map(JpaDistributionSetCreate.class::cast)
                .map(this::setDefaultTypeIfMissing).map(JpaDistributionSetCreate::build).toList();

        return Collections.unmodifiableList(distributionSetRepository.saveAll(AccessController.Operation.CREATE, toCreate));
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
    public DistributionSet assignSoftwareModules(final long id, final Collection<Long> softwareModuleId) {
        final JpaDistributionSet set = (JpaDistributionSet) getValid(id);
        assertDistributionSetIsNotAssignedToTargets(id);
        assertSoftwareModuleQuota(id, softwareModuleId.size());

        final Collection<JpaSoftwareModule> modules = softwareModuleRepository.findAllById(softwareModuleId);
        if (modules.size() < softwareModuleId.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId,
                    modules.stream().map(SoftwareModule::getId).toList());
        }

        modules.forEach(set::addModule);

        return distributionSetRepository.save(set);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet unassignSoftwareModule(final long id, final long moduleId) {
        final JpaDistributionSet set = (JpaDistributionSet) getValid(id);
        assertDistributionSetIsNotAssignedToTargets(id);

        final JpaSoftwareModule module = findSoftwareModuleAndThrowExceptionIfNotFound(moduleId);
        set.removeModule(module);

        return distributionSetRepository.save(set);
    }

    @Override
    public Slice<DistributionSet> findByDistributionSetFilter(final Pageable pageable,
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);

        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, pageable, specList);
    }

    @Override
    public long countByDistributionSetFilter(@NotNull final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);

        return JpaManagementHelper.countBySpec(distributionSetRepository, specList);
    }

    @Override
    public Slice<DistributionSet> findByCompleted(final Pageable pageReq, final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specifications = buildSpecsByComplete(complete);

        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, pageReq, specifications);
    }

    @Override
    public long countByCompleted(final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specifications = buildSpecsByComplete(complete);

        return JpaManagementHelper.countBySpec(distributionSetRepository, specifications);
    }

    private List<Specification<JpaDistributionSet>> buildSpecsByComplete(final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specifications = new ArrayList<>();
        specifications.add(DistributionSetSpecification.isNotDeleted());
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

        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, unsortedPage, specList);
    }

    @Override
    public Optional<DistributionSet> getByNameAndVersion(final String distributionName, final String version) {
        return distributionSetRepository
                .findOne(DistributionSetSpecification.equalsNameAndVersionIgnoreCase(distributionName, version))
                .map(DistributionSet.class::cast);

    }

    @Override
    public long count() {
        return distributionSetRepository.count(DistributionSetSpecification.isNotDeleted());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSetMetadata> createMetaData(final long id, final Collection<MetaData> md) {
        final JpaDistributionSet distributionSet = (JpaDistributionSet)getValid(id);
        assertMetaDataQuota(id, md.size());

        md.forEach(meta -> checkAndThrowIfDistributionSetMetadataAlreadyExists(
                new DsMetadataCompositeKey(id, meta.getKey())));

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
    public DistributionSetMetadata updateMetaData(final long id, final MetaData md) {
        // check if exists otherwise throw entity not found exception
        final JpaDistributionSetMetadata toUpdate = (JpaDistributionSetMetadata) getMetaDataByDistributionSetId(id,
                md.getKey())
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetMetadata.class, id, md.getKey()));
        toUpdate.setValue(md.getValue());

        // touch it to update the lock revision because we are modifying the
        // DS indirectly, it will, also check UPDATE access
        JpaManagementHelper.touch(entityManager, distributionSetRepository, (JpaDistributionSet) getValid(id));
        return distributionSetMetadataRepository.save(toUpdate);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetaData(final long id, final String key) {
        final JpaDistributionSetMetadata metadata = (JpaDistributionSetMetadata) getMetaDataByDistributionSetId(
                id, key)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetMetadata.class, id, key));

        // touch it to update the lock revision because we are modifying the
        // DS indirectly, it will, also check UPDATE access
        JpaManagementHelper.touch(entityManager, distributionSetRepository,
                (JpaDistributionSet) metadata.getDistributionSet());
        distributionSetMetadataRepository.deleteById(metadata.getId());
    }

    @Override
    public Page<DistributionSetMetadata> findMetaDataByDistributionSetId(final Pageable pageable,
            final long id) {
        assertDistributionSetExists(id);

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetMetadataRepository, pageable,
                Collections.singletonList(byDsIdSpec(id)));
    }

    private Specification<JpaDistributionSetMetadata> byDsIdSpec(final long dsId) {
        return (root, query, cb) -> cb
                .equal(root.get(JpaDistributionSetMetadata_.distributionSet).get(JpaDistributionSet_.id), dsId);
    }

    @Override
    public long countMetaDataByDistributionSetId(final long id) {
        assertDistributionSetExists(id);

        return distributionSetMetadataRepository.countByDistributionSetId(id);
    }

    @Override
    public Page<DistributionSetMetadata> findMetaDataByDistributionSetIdAndRsql(final Pageable pageable,
                                                                                final long id, final String rsqlParam) {
        assertDistributionSetExists(id);

        final List<Specification<JpaDistributionSetMetadata>> specList = Arrays
                .asList(RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetMetadataFields.class,
                        virtualPropertyReplacer, database), byDsIdSpec(id));

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetMetadataRepository, pageable, specList);
    }

    @Override
    public Optional<DistributionSetMetadata> getMetaDataByDistributionSetId(final long id, final String key) {
        assertDistributionSetExists(id);

        return distributionSetMetadataRepository
                .findById(new DsMetadataCompositeKey(id, key))
                .map(DistributionSetMetadata.class::cast);
    }

    @Override
    public Optional<DistributionSet> getByAction(final long actionId) {
        return actionRepository
                .findById(actionId)
                .map(action -> {
                    if (!targetRepository.exists(TargetSpecifications.hasId(action.getTarget().getId()))) {
                        throw new InsufficientPermissionException("Target not accessible (or not found)!");
                    }
                    return distributionSetRepository
                            .findOne(DistributionSetSpecification.byActionId(actionId))
                            .orElseThrow(() ->
                                    new InsufficientPermissionException("DistributionSet not accessible (or not found)!"));
                })
                .map(DistributionSet.class::cast)
                .or(() -> {
                    throw new EntityNotFoundException(Action.class, actionId);
                });
    }

    @Override
    public boolean isInUse(final long id) {
        assertDistributionSetExists(id);

        return actionRepository.countByDistributionSetId(id) > 0;
    }

    private static List<Specification<JpaDistributionSet>> buildDistributionSetSpecifications(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = new ArrayList<>(10);

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

        if (!ObjectUtils.isEmpty(distributionSetFilter.getSearchText())) {
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
    public DistributionSet unassignTag(final long id, final long dsTagId) {
        final JpaDistributionSet set = (JpaDistributionSet) getWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, id));

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
    public void lock(final long id) {
        final JpaDistributionSet distributionSet = getById(id);
        if (!distributionSet.isLocked()) {
            distributionSet.getModules().forEach(module -> {
                if (!module.isLocked()) {
                    final JpaSoftwareModule jpaSoftwareModule = (JpaSoftwareModule)module;
                    jpaSoftwareModule.lock();
                    softwareModuleRepository.save(jpaSoftwareModule);
                }
            });
            distributionSet.lock();
            distributionSetRepository.save(distributionSet);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void unlock(final long id) {
        final JpaDistributionSet distributionSet = getById(id);
        if (distributionSet.isLocked()) {
            distributionSet.unlock();
            distributionSetRepository.save(distributionSet);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long id) {
        delete(List.of(id));
    }

    @Override
    public List<DistributionSet> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(getDistributionSets(ids));
    }

    private List<JpaDistributionSet> getDistributionSets(final Collection<Long> ids) {
        final List<JpaDistributionSet> foundDs = distributionSetRepository.findAllById(ids);
        if (foundDs.size() != ids.size()) {
            throw new EntityNotFoundException(
                    DistributionSet.class, ids, foundDs.stream().map(JpaDistributionSet::getId).toList());
        }
        return foundDs;
    }

    @Override
    public Page<DistributionSet> findByTag(final Pageable pageable, final long tagId) {
        assertDsTagExists(tagId);

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, pageable, List.of(
                DistributionSetSpecification.hasTag(tagId)));
    }

    @Override
    public Page<DistributionSet> findByRsqlAndTag(final Pageable pageable, final String rsqlParam, final long tagId) {
        assertDsTagExists(tagId);

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, pageable, List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetFields.class, virtualPropertyReplacer,
                        database),
                DistributionSetSpecification.hasTag(tagId), DistributionSetSpecification.isNotDeleted()));
    }

    @Override
    public Slice<DistributionSet> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, pageable, List.of(
                DistributionSetSpecification.isNotDeleted()));
    }

    @Override
    public Page<DistributionSet> findByRsql(final Pageable pageable, final String rsqlParam) {
        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, pageable, List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetFields.class, virtualPropertyReplacer,
                        database),
                DistributionSetSpecification.isNotDeleted()));
    }

    @Override
    public Optional<DistributionSet> get(final long id) {
        return distributionSetRepository.findById(id).map(DistributionSet.class::cast);
    }

    @Override
    public boolean exists(final long id) {
        return distributionSetRepository.existsById(id);
    }

    @Override
    public DistributionSet getOrElseThrowException(final long id) {
        return getById(id);
    }

    @Override
    public DistributionSet getValidAndComplete(final long id) {
        final DistributionSet distributionSet = getValid(id);

        if (!distributionSet.isComplete()) {
            throw new IncompleteDistributionSetException("Distribution set of type "
                    + distributionSet.getType().getKey() + " is incomplete: " + distributionSet.getId());
        }

        if (distributionSet.isDeleted()) {
            throw new DeletedException(DistributionSet.class, id);
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
    public void invalidate(final DistributionSet distributionSet) {
        final JpaDistributionSet jpaSet = (JpaDistributionSet) distributionSet;
        jpaSet.invalidate();
        distributionSetRepository.save(jpaSet);
    }

    // check if shall implicitly lock a distribution set
    boolean isImplicitLockApplicable(final DistributionSet distributionSet) {
        final JpaDistributionSet jpaDistributionSet = (JpaDistributionSet) distributionSet;
        if (jpaDistributionSet.isLocked()) {
            // already locked
            return false;
        }

        if (!tenantConfigHelper.getConfigValue(IMPLICIT_LOCK_ENABLED, Boolean.class)) {
            // implicit lock disabled
            return false;
        }

        final List<String> skipForTags = repositoryProperties.getSkipImplicitLockForTags();
        if (!ObjectUtils.isEmpty(skipForTags)) {
            final Set<DistributionSetTag> tags = ((JpaDistributionSet)jpaDistributionSet).getTags();
            if (!ObjectUtils.isEmpty(tags)) {
                for (final DistributionSetTag tag : tags) {
                    if (skipForTags.contains(tag.getName())) {
                        // has a skip tag
                        return false;
                    }
                }
            }
        }

        // finally - implicit lock is applicable
        return true;
    }

    private JpaDistributionSet getById(final long id) {
        return distributionSetRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, id));
    }

    private void assertDistributionSetExists(final long id) {
        if (!distributionSetRepository.existsById(id)) {
          throw new EntityNotFoundException(DistributionSet.class, id);
        }
    }

    private void assertDsTagExists(final Long tagId) {
        if (!distributionSetTagRepository.existsById(tagId)) {
            throw new EntityNotFoundException(DistributionSetTag.class, tagId);
        }
    }
}
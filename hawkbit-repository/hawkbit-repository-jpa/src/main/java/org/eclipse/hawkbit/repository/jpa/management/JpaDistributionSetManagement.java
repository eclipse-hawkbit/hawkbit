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
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.IMPLICIT_LOCK_ENABLED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericTagUpdate;
import org.eclipse.hawkbit.repository.exception.DeletedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetTagCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetFilterQueryRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "distribution-set-management" }, matchIfMissing = true)
public class JpaDistributionSetManagement
        extends AbstractJpaRepositoryManagement<JpaDistributionSet, JpaDistributionSetCreate, GenericDistributionSetUpdate, DistributionSetRepository, DistributionSetFields>
        implements DistributionSetManagement<JpaDistributionSet, JpaDistributionSetCreate, GenericDistributionSetUpdate> {

    private final DistributionSetTagManagement<JpaDistributionSetTag, JpaDistributionSetTagCreate, GenericTagUpdate> distributionSetTagManagement;
    private final DistributionSetTypeManagement<JpaDistributionSetType, JpaDistributionSetTypeCreate, GenericDistributionSetTypeUpdate> distributionSetTypeManagement;
    private final SoftwareModuleRepository softwareModuleRepository;
    private final DistributionSetTagRepository distributionSetTagRepository;
    private final TargetRepository targetRepository;
    private final TargetFilterQueryRepository targetFilterQueryRepository;
    private final ActionRepository actionRepository;
    private final SystemManagement systemManagement;
    private final QuotaManagement quotaManagement;
    private final TenantConfigHelper tenantConfigHelper;
    private final RepositoryProperties repositoryProperties;

    @SuppressWarnings("java:S107")
    public JpaDistributionSetManagement(
            final DistributionSetRepository jpaRepository,
            final EntityManager entityManager,
            final DistributionSetTagManagement<JpaDistributionSetTag, JpaDistributionSetTagCreate, GenericTagUpdate> distributionSetTagManagement,
            final DistributionSetTypeManagement<JpaDistributionSetType, JpaDistributionSetTypeCreate, GenericDistributionSetTypeUpdate> distributionSetTypeManagement,
            final SoftwareModuleRepository softwareModuleRepository,
            final DistributionSetTagRepository distributionSetTagRepository,
            final TargetRepository targetRepository,
            final TargetFilterQueryRepository targetFilterQueryRepository,
            final ActionRepository actionRepository,
            final SystemManagement systemManagement, final QuotaManagement quotaManagement,
            final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final RepositoryProperties repositoryProperties) {
        super(jpaRepository, entityManager);
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.softwareModuleRepository = softwareModuleRepository;
        this.distributionSetTagRepository = distributionSetTagRepository;
        this.targetRepository = targetRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.actionRepository = actionRepository;
        this.systemManagement = systemManagement;
        this.quotaManagement = quotaManagement;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement);
        this.repositoryProperties = repositoryProperties;
    }

    public JpaDistributionSet create(final JpaDistributionSetCreate create) {
        return super.create(setDefaultTypeIfMissing(create));
    }

    public List<JpaDistributionSet> create(final Collection<JpaDistributionSetCreate> create) {
        create.forEach(this::setDefaultTypeIfMissing);
        return super.create(create);
    }

    public JpaDistributionSet update(final GenericDistributionSetUpdate update) {
        final JpaDistributionSet distributionSet = getValid0(update.getId());

        // lock/unlock ONLY if locked flag is present!
        if (Boolean.TRUE.equals(update.locked())) {
            if (!distributionSet.isLocked()) {
                lockSoftwareModules(distributionSet);
                distributionSet.lock();
            }
        } else if (Boolean.FALSE.equals(update.locked())) {
            if (distributionSet.isLocked()) {
                distributionSet.unlock();
            }
        }

        if (update.isRequiredMigrationStep() != null && !update.isRequiredMigrationStep().equals(distributionSet.isRequiredMigrationStep())) {
            assertDistributionSetIsNotAssignedToTargets(update.getId());
        }

        return super.update(update, distributionSet);
    }

    @Override
    protected Collection<JpaDistributionSet> softDelete(final Collection<JpaDistributionSet> toDelete) {
        // soft delete assigned
        final List<Long> ids = toDelete.stream().map(JpaDistributionSet::getId).toList();
        final Set<Long> assigned = new HashSet<>(jpaRepository.findAssignedToTargetDistributionSetsById(ids));
        assigned.addAll(jpaRepository.findAssignedToRolloutDistributionSetsById(ids));
        return toDelete.stream().filter(distributionSet -> assigned.contains(distributionSet.getId())).toList();
    }

    protected void delete0(final Collection<Long> distributionSetIDs) {
        if (ObjectUtils.isEmpty(distributionSetIDs)) {
            return; // super checks but if empty we don't want to unassign from target filters
        }

        // if delete fail (because of permission denied) transaction will be rolled back
        targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(distributionSetIDs.toArray(new Long[0]));
        super.delete(distributionSetIDs);
    }

    @Override
    public Optional<JpaDistributionSet> getWithDetails(final long id) {
        return jpaRepository.findOne(jpaRepository.byIdSpec(id), JpaDistributionSet_.GRAPH_DISTRIBUTION_SET_DETAIL);
    }

    @Override
    @Transactional
    public void invalidate(final JpaDistributionSet distributionSet) {
        distributionSet.invalidate();
        jpaRepository.save(distributionSet);
    }

    @Override
    public JpaDistributionSet getOrElseThrowException(final long id) {
        return getById(id);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public JpaDistributionSet assignSoftwareModules(final long id, final Collection<Long> softwareModuleId) {
        final JpaDistributionSet set = getValid0(id);
        assertDistributionSetIsNotAssignedToTargets(id);
        assertSoftwareModuleQuota(id, softwareModuleId.size());

        final Collection<JpaSoftwareModule> modules = softwareModuleRepository.findAllById(softwareModuleId);
        if (modules.size() < softwareModuleId.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId,
                    modules.stream().map(SoftwareModule::getId).toList());
        }

        modules.forEach(set::addModule);

        return jpaRepository.save(set);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public JpaDistributionSet unassignSoftwareModule(final long id, final long moduleId) {
        final JpaDistributionSet set = getValid0(id);
        assertDistributionSetIsNotAssignedToTargets(id);

        final JpaSoftwareModule module = findSoftwareModuleAndThrowExceptionIfNotFound(moduleId);
        set.removeModule(module);

        return jpaRepository.save(set);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public List<JpaDistributionSet> assignTag(final Collection<Long> ids, final long dsTagId) {
        return updateTag(ids, dsTagId, (tag, distributionSet) -> {
            if (distributionSet.getTags().contains(tag)) {
                return distributionSet;
            } else {
                distributionSet.addTag(tag);
                return jpaRepository.save(distributionSet);
            }
        });
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public List<JpaDistributionSet> unassignTag(final Collection<Long> ids, final long dsTagId) {
        return updateTag(ids, dsTagId, (tag, distributionSet) -> {
            if (distributionSet.getTags().contains(tag)) {
                distributionSet.removeTag(tag);
                return jpaRepository.save(distributionSet);
            } else {
                return distributionSet;
            }
        });
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void createMetadata(final long id, final Map<String, String> md) {
        final JpaDistributionSet distributionSet = getValid0(id);

        // get the modifiable metadata map
        final Map<String, String> metadata = distributionSet.getMetadata();
        md.keySet().forEach(key -> {
            if (metadata.containsKey(key)) {
                throw new EntityAlreadyExistsException("Metadata entry with key '" + key + "' already exists");
            }
        });
        metadata.putAll(md);

        assertMetaDataQuota(id, metadata.size());

        jpaRepository.save(distributionSet);
    }

    @Override
    public Map<String, String> getMetadata(final long id) {
        assertDistributionSetExists(id);
        return getMap(id, JpaDistributionSet_.metadata);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void updateMetadata(final long id, final String key, final String value) {
        final JpaDistributionSet distributionSet = getValid0(id);

        // get the modifiable metadata map
        final Map<String, String> metadata = distributionSet.getMetadata();
        if (!metadata.containsKey(key)) {
            throw new EntityNotFoundException("DistributionSet metadata", id + ":" + key);
        }
        metadata.put(key, value);

        jpaRepository.save(distributionSet);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void deleteMetadata(final long id, final String key) {
        final JpaDistributionSet distributionSet = getValid0(id);

        // get the modifiable metadata map
        final Map<String, String> metadata = distributionSet.getMetadata();
        if (metadata.remove(key) == null) {
            throw new EntityNotFoundException("DistributionSet metadata", id + ":" + key);
        }

        jpaRepository.save(distributionSet);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void lock(final long id) {
        final JpaDistributionSet distributionSet = getById(id);
        if (!distributionSet.isLocked()) {
            lockSoftwareModules(distributionSet);
            distributionSet.lock();
            jpaRepository.save(distributionSet);
        }
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void unlock(final long id) {
        final JpaDistributionSet distributionSet = getById(id);
        if (distributionSet.isLocked()) {
            distributionSet.unlock();
            jpaRepository.save(distributionSet);
        }
    }

    @Override
    public Optional<JpaDistributionSet> findByAction(final long actionId) {
        return actionRepository
                .findById(actionId)
                .map(action -> {
                    if (!targetRepository.exists(TargetSpecifications.hasId(action.getTarget().getId()))) {
                        throw new InsufficientPermissionException("Target not accessible (or not found)!");
                    }
                    return jpaRepository
                            .findOne(DistributionSetSpecification.byIdFetch(action.getDistributionSet().getId()))
                            .orElseThrow(() ->
                                    new InsufficientPermissionException("DistributionSet not accessible (or not found)!"));
                })
                .or(() -> {
                    throw new EntityNotFoundException(Action.class, actionId);
                });
    }

    @Override
    public Optional<JpaDistributionSet> findByNameAndVersion(final String distributionName, final String version) {
        return jpaRepository.findOne(DistributionSetSpecification.equalsNameAndVersionIgnoreCase(distributionName, version));
    }

    @Override
    public JpaDistributionSet getValidAndComplete(final long id) {
        final JpaDistributionSet distributionSet = getValid0(id);

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
    public JpaDistributionSet getValid(final long id) {
        return getValid0(id);
    }

    @Override
    public Slice<JpaDistributionSet> findByCompleted(final Boolean complete, final Pageable pageReq) {
        final List<Specification<JpaDistributionSet>> specifications = buildSpecsByComplete(complete);

        return JpaManagementHelper.findAllWithoutCountBySpec(jpaRepository, specifications, pageReq);
    }

    @Override
    public long countByCompleted(final Boolean complete) {
        return JpaManagementHelper.countBySpec(jpaRepository, buildSpecsByComplete(complete));
    }

    @Override
    public Slice<JpaDistributionSet> findByDistributionSetFilter(final DistributionSetFilter distributionSetFilter, final Pageable pageable) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(distributionSetFilter);
        return JpaManagementHelper.findAllWithoutCountBySpec(jpaRepository, specList, pageable);
    }

    @Override
    public long countByDistributionSetFilter(@NotNull final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(distributionSetFilter);
        return JpaManagementHelper.countBySpec(jpaRepository, specList);
    }

    @Override
    public Page<JpaDistributionSet> findByTag(final long tagId, final Pageable pageable) {
        assertDsTagExists(tagId);
        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository, List.of(DistributionSetSpecification.hasTag(tagId)), pageable);
    }

    @Override
    public Page<JpaDistributionSet> findByRsqlAndTag(final String rsql, final long tagId, final Pageable pageable) {
        assertDsTagExists(tagId);
        return JpaManagementHelper.findAllWithCountBySpec(
                jpaRepository,
                List.of(
                        RsqlUtility.getInstance().buildRsqlSpecification(rsql, DistributionSetFields.class),
                        DistributionSetSpecification.hasTag(tagId), DistributionSetSpecification.isNotDeleted()),
                pageable);
    }

    @Override
    public long countByTypeId(final long typeId) {
        if (!distributionSetTypeManagement.exists(typeId)) {
            throw new EntityNotFoundException(DistributionSetType.class, typeId);
        }
        return jpaRepository.count(DistributionSetSpecification.byType(typeId));
    }

    @Override
    public boolean isInUse(final long id) {
        assertDistributionSetExists(id);
        return actionRepository.countByDistributionSetId(id) > 0;
    }

    @Override
    public List<Statistic> countRolloutsByStatusForDistributionSet(final Long id) {
        assertDistributionSetExists(id);
        return jpaRepository.countRolloutsByStatusForDistributionSet(id).stream().map(Statistic.class::cast).toList();
    }

    @Override
    public List<Statistic> countActionsByStatusForDistributionSet(final Long id) {
        assertDistributionSetExists(id);
        return jpaRepository.countActionsByStatusForDistributionSet(id).stream().map(Statistic.class::cast).toList();
    }

    @Override
    public Long countAutoAssignmentsForDistributionSet(final Long id) {
        assertDistributionSetExists(id);
        return jpaRepository.countAutoAssignmentsForDistributionSet(id);
    }

    // check if it shall implicitly lock a distribution set
    boolean isImplicitLockApplicable(final DistributionSet distributionSet) {
        final JpaDistributionSet jpaDistributionSet = (JpaDistributionSet) distributionSet;
        if (jpaDistributionSet.isLocked()) {
            // already locked
            return false;
        }

        if (Boolean.FALSE.equals(tenantConfigHelper.getConfigValue(IMPLICIT_LOCK_ENABLED, Boolean.class))) {
            // implicit lock disabled
            return false;
        }

        final List<String> skipForTags = repositoryProperties.getSkipImplicitLockForTags();
        if (!ObjectUtils.isEmpty(skipForTags)) {
            final Set<DistributionSetTag> tags = jpaDistributionSet.getTags();
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

    private static List<Specification<JpaDistributionSet>> buildDistributionSetSpecifications(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = new ArrayList<>(10);

        if (distributionSetFilter.getIsComplete() != null) {
            specList.add(DistributionSetSpecification.isCompleted(distributionSetFilter.getIsComplete()));
        }
        if (distributionSetFilter.getIsDeleted() != null) {
            specList.add(DistributionSetSpecification.isDeleted(distributionSetFilter.getIsDeleted()));
        }
        if (distributionSetFilter.getIsValid() != null) {
            specList.add(DistributionSetSpecification.isValid(distributionSetFilter.getIsValid()));
        }
        if (distributionSetFilter.getTypeId() != null) {
            specList.add(DistributionSetSpecification.byType(distributionSetFilter.getTypeId()));
        }
        if (!ObjectUtils.isEmpty(distributionSetFilter.getSearchText())) {
            final String[] dsFilterNameAndVersionEntries = JpaManagementHelper
                    .getFilterNameAndVersionEntries(distributionSetFilter.getSearchText().trim());
            specList.add(DistributionSetSpecification.likeNameAndVersion(dsFilterNameAndVersionEntries[0], dsFilterNameAndVersionEntries[1]));
        }
        if (hasTagsFilterActive(distributionSetFilter)) {
            specList.add(DistributionSetSpecification.hasTags(
                    distributionSetFilter.getTagNames(), distributionSetFilter.getSelectDSWithNoTag()));
        }
        return specList;
    }

    private static boolean hasTagsFilterActive(final DistributionSetFilter distributionSetFilter) {
        final boolean isNoTagActive = Boolean.TRUE.equals(distributionSetFilter.getSelectDSWithNoTag());
        final boolean isAtLeastOneTagActive = !CollectionUtils.isEmpty(distributionSetFilter.getTagNames());
        return isNoTagActive || isAtLeastOneTagActive;
    }

    private static Collection<Long> notFound(final Collection<Long> distributionSetIds, final List<JpaDistributionSet> foundDistributionSets) {
        final Map<Long, JpaDistributionSet> foundDistributionSetMap = foundDistributionSets.stream()
                .collect(Collectors.toMap(JpaDistributionSet::getId, Function.identity()));
        return distributionSetIds.stream().filter(id -> !foundDistributionSetMap.containsKey(id)).toList();
    }

    private JpaDistributionSet getValid0(final long id) {
        final JpaDistributionSet distributionSet = getById(id);
        if (!distributionSet.isValid()) {
            throw new InvalidDistributionSetException(
                    "Distribution set of type " + distributionSet.getType().getKey() + " is invalid: " + distributionSet.getId());
        }
        return distributionSet;
    }

    private List<JpaDistributionSet> updateTag(
            final Collection<Long> dsIds, final long dsTagId,
            final BiFunction<DistributionSetTag, JpaDistributionSet, JpaDistributionSet> updater) {
        final DistributionSetTag tag = distributionSetTagManagement.get(dsTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, dsTagId));
        final List<JpaDistributionSet> allDs = dsIds.size() == 1 ?
                jpaRepository.findById(dsIds.iterator().next())
                        .map(List::of)
                        .orElseGet(Collections::emptyList) :
                jpaRepository.findAll(DistributionSetSpecification.byIdsFetch(dsIds));
        if (allDs.size() < dsIds.size()) {
            throw new EntityNotFoundException(DistributionSet.class, notFound(dsIds, allDs));
        }

        try {
            // apply update and collect modified targets
            return allDs.stream().map(distributionSet -> updater.apply(tag, distributionSet)).toList();
        } finally {
            // No reason to save the tag
            entityManager.detach(tag);
        }
    }

    private Map<String, String> getMap(final long id, final MapAttribute<JpaDistributionSet, String, String> mapAttribute) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

        final Root<JpaDistributionSet> targetRoot = query.from(JpaDistributionSet.class);
        query.where(cb.equal(targetRoot.get(AbstractJpaBaseEntity_.ID), id));

        final MapJoin<JpaDistributionSet, String, String> mapJoin = targetRoot.join(mapAttribute);
        query.multiselect(mapJoin.key(), mapJoin.value());
        query.orderBy(cb.asc(mapJoin.key()));

        return entityManager
                .createQuery(query)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(entry -> (String) entry[0], entry -> (String) entry[1], (v1, v2) -> v1, LinkedHashMap::new));
    }

    private JpaSoftwareModule findSoftwareModuleAndThrowExceptionIfNotFound(final Long softwareModuleId) {
        return softwareModuleRepository.findById(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));
    }

    private JpaDistributionSetCreate setDefaultTypeIfMissing(final JpaDistributionSetCreate create) {
        if (create.getType() == null) {
            create.type(systemManagement.getTenantMetadata().getDefaultDsType().getKey());
        }
        return create;
    }

    private List<Specification<JpaDistributionSet>> buildSpecsByComplete(final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specifications = new ArrayList<>();
        specifications.add(DistributionSetSpecification.isNotDeleted());
        if (complete != null) {
            specifications.add(DistributionSetSpecification.isCompleted(complete));
        }
        return specifications;
    }

    private void assertMetaDataQuota(final Long dsId, final int requested) {
        final int limit = quotaManagement.getMaxMetaDataEntriesPerDistributionSet();
        QuotaHelper.assertAssignmentQuota(dsId, requested, limit, "Metadata", DistributionSet.class.getSimpleName(), null);
    }

    private void assertSoftwareModuleQuota(final Long id, final int requested) {
        QuotaHelper.assertAssignmentQuota(id, requested, quotaManagement.getMaxSoftwareModulesPerDistributionSet(),
                SoftwareModule.class, DistributionSet.class, softwareModuleRepository::countByAssignedToId);
    }

    private void assertDistributionSetIsNotAssignedToTargets(final Long distributionSet) {
        if (actionRepository.countByDistributionSetId(distributionSet) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "Distribution set %s is already assigned to targets and cannot be changed", distributionSet));
        }
    }

    private void lockSoftwareModules(final JpaDistributionSet distributionSet) {
        distributionSet.getModules().forEach(module -> {
            if (!module.isLocked()) {
                final JpaSoftwareModule jpaSoftwareModule = (JpaSoftwareModule) module;
                jpaSoftwareModule.lock();
                softwareModuleRepository.save(jpaSoftwareModule);
            }
        });
    }

    private JpaDistributionSet getById(final long id) {
        return jpaRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, id));
    }

    private void assertDistributionSetExists(final long id) {
        if (!jpaRepository.existsById(id)) {
            throw new EntityNotFoundException(DistributionSet.class, id);
        }
    }

    private void assertDsTagExists(final Long tagId) {
        if (!distributionSetTagRepository.existsById(tagId)) {
            throw new EntityNotFoundException(DistributionSetTag.class, tagId);
        }
    }
}
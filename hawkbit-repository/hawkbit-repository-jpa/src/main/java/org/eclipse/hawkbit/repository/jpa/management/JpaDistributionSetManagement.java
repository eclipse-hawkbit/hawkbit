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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ql.Node;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.exception.DeletedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetFilterQueryRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.eclipse.hawkbit.repository.qfields.DistributionSetFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "distribution-set-management" }, matchIfMissing = true)
@Slf4j
public class JpaDistributionSetManagement
        extends
        AbstractJpaRepositoryWithMetadataManagement<JpaDistributionSet, DistributionSetManagement.Create, DistributionSetManagement.Update, DistributionSetRepository, DistributionSetFields, String, String>
        implements DistributionSetManagement<JpaDistributionSet> {

    private final DistributionSetTagManagement<JpaDistributionSetTag> distributionSetTagManagement;
    private final SoftwareModuleRepository softwareModuleRepository;
    private final DistributionSetTagRepository distributionSetTagRepository;
    private final TargetFilterQueryRepository targetFilterQueryRepository;
    private final QuotaManagement quotaManagement;
    private final RepositoryProperties repositoryProperties;

    @SuppressWarnings("java:S107")
    protected JpaDistributionSetManagement(
            final DistributionSetRepository jpaRepository,
            final EntityManager entityManager,
            final DistributionSetTagManagement<JpaDistributionSetTag> distributionSetTagManagement,
            final SoftwareModuleRepository softwareModuleRepository,
            final DistributionSetTagRepository distributionSetTagRepository,
            final TargetFilterQueryRepository targetFilterQueryRepository,
            final QuotaManagement quotaManagement,
            final RepositoryProperties repositoryProperties) {
        super(jpaRepository, entityManager);
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.softwareModuleRepository = softwareModuleRepository;
        this.distributionSetTagRepository = distributionSetTagRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.quotaManagement = quotaManagement;
        this.repositoryProperties = repositoryProperties;
    }

    private static final String COMPLETE = "complete";

    @Override
    @SuppressWarnings("java:S3776") // java:S3776 - just too complex
    public Page<JpaDistributionSet> findByRsql(final String rsql, final Pageable pageable) {
        if (rsql != null && rsql.toLowerCase().contains(COMPLETE)) {
            // limited support for 'complete' - could be removed in future
            final Node node = QLSupport.getInstance().parse(rsql);
            final Specification<JpaDistributionSet> notDeleted = (root, query, cb) -> cb.equal(root.get(DELETED), false);
            final List<Specification<JpaDistributionSet>> specList = new ArrayList<>();
            specList.add(notDeleted);
            final AtomicReference<Node.Comparison> completedComparison = new AtomicReference<>();
            if (node instanceof Node.Comparison comparison && COMPLETE.equalsIgnoreCase(comparison.getKey())) {
                // all not deleted, won't add anything to spec
                completedComparison.set(comparison);
            } else if (node instanceof Node.Logical logical && logical.getOp() == Node.Logical.Operator.AND) {
                final List<Node> sanitizedChildren = new ArrayList<>();
                logical.getChildren().forEach(child -> {
                    if (child instanceof Node.Comparison comparison && COMPLETE.equalsIgnoreCase(comparison.getKey())) {
                        if (completedComparison.get() != null) {
                            throw new RSQLParameterSyntaxException("Multiple 'complete' comparisons are not supported");
                        }
                        completedComparison.set(comparison);
                    } else {
                        sanitizedChildren.add(child);
                    }
                });
                specList.add(QLSupport.getInstance().buildSpec(
                        sanitizedChildren.size() == 1
                                ? sanitizedChildren.get(0)
                                : new Node.Logical(Node.Logical.Operator.AND, sanitizedChildren),
                        DistributionSetFields.class));
            }
            if (completedComparison.get() != null) { // really a comparison
                log.warn("Usage of 'complete' is limited and may be removed: {}", node);
                final boolean completed = completeComparison(completedComparison);
                return filter(JpaManagementHelper.findAllWithCountBySpec(jpaRepository, specList, pageable), completed);
            }
        }

        return super.findByRsql(rsql, pageable);
    }

    @Override
    public JpaDistributionSet update(final Update update) {
        final JpaDistributionSet updated = super.update(update);
        if (Boolean.TRUE.equals(update.getLocked())) {
            lockSoftwareModules(updated);
        }
        return updated;
    }

    @Override
    public Map<Long, JpaDistributionSet> update(final Collection<Update> updates) {
        final Map<Long, JpaDistributionSet> updated = super.update(updates);
        for (final Update update : updates) {
            final JpaDistributionSet updatedSet = updated.get(update.getId());
            if (Boolean.TRUE.equals(update.getLocked())) {
                lockSoftwareModules(updatedSet);
            }
        }
        return updated;
    }

    @Override
    protected Collection<JpaDistributionSet> softDelete(final Collection<JpaDistributionSet> toDelete) {
        // soft delete assigned
        final List<Long> ids = toDelete.stream().map(JpaDistributionSet::getId).toList();
        final Set<Long> assigned = new HashSet<>(jpaRepository.findAssignedToTargetDistributionSetsById(ids));
        assigned.addAll(jpaRepository.findAssignedToRolloutDistributionSetsById(ids));
        return toDelete.stream().filter(distributionSet -> assigned.contains(distributionSet.getId())).toList();
    }

    @Override
    protected void delete0(final Collection<Long> distributionSetIDs) {
        if (ObjectUtils.isEmpty(distributionSetIDs)) {
            return; // super checks but if empty we don't want to unassign from target filters
        }

        // if delete fail (because of permission denied) transaction will be rolled back
        targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(distributionSetIDs.toArray(new Long[0]));
        super.delete0(distributionSetIDs);
    }

    @Override
    public JpaDistributionSet getWithDetails(final long id) {
        return jpaRepository.findOne(jpaRepository.byIdSpec(id), JpaDistributionSet_.GRAPH_DISTRIBUTION_SET_DETAIL)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, id));
    }

    // implicitly lock a distribution set if not already locked and implicit lock is enabled and not to skip
    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public boolean shouldLockImplicitly(final DistributionSet distributionSet) {
        final JpaDistributionSet jpaDistributionSet = toJpaDistributionSet(distributionSet);
        if (jpaDistributionSet.isLocked()) {
            // already locked
            return false;
        }

        if (Boolean.FALSE.equals(TenantConfigHelper.getAsSystem(IMPLICIT_LOCK_ENABLED, Boolean.class))) {
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

        // finally - implicitly lock
        return true;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public JpaDistributionSet lock(final DistributionSet distributionSet) {
        final JpaDistributionSet jpaDistributionSet = toJpaDistributionSet(distributionSet);
        if (distributionSet.isLocked()) {
            return jpaDistributionSet;
        } else {
            if (!distributionSet.isComplete()) {
                throw new IncompleteDistributionSetException("Could not be locked while incomplete!");
            }
            lockSoftwareModules(jpaDistributionSet);
            jpaDistributionSet.setLocked(true);
            return jpaRepository.save(jpaDistributionSet);
        }
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public JpaDistributionSet unlock(final DistributionSet distributionSet) {
        final JpaDistributionSet jpaDistributionSet = toJpaDistributionSet(distributionSet);
        if (jpaDistributionSet.isLocked()) {
            jpaDistributionSet.setLocked(false);
            return jpaRepository.save(jpaDistributionSet);
        } else {
            return jpaDistributionSet;
        }
    }

    @Override
    @Transactional
    public JpaDistributionSet invalidate(final DistributionSet distributionSet) {
        final JpaDistributionSet jpaDistributionSet = toJpaDistributionSet(distributionSet);
        jpaDistributionSet.invalidate();
        return jpaRepository.save(jpaDistributionSet);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public JpaDistributionSet assignSoftwareModules(final long id, final Collection<Long> softwareModuleId) {
        final JpaDistributionSet set = getValid0(id);
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

        final JpaSoftwareModule module = softwareModuleRepository.getById(moduleId);
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
    public JpaDistributionSet findByNameAndVersion(final String distributionName, final String version) {
        return jpaRepository.findOne(DistributionSetSpecification.equalsNameAndVersionIgnoreCase(distributionName, version))
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, Map.entry(distributionName, version)));
    }

    @Override
    public JpaDistributionSet getValidAndComplete(final long id) {
        final JpaDistributionSet distributionSet = getValid0(id);
        if (!distributionSet.isComplete()) {
            throw new IncompleteDistributionSetException(
                    "Distribution set of type " + distributionSet.getType().getKey() + " is incomplete: " + distributionSet.getId());
        }
        if (distributionSet.isDeleted()) {
            throw new DeletedException(DistributionSet.class, id);
        }
        return distributionSet;
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
                        QLSupport.getInstance().buildSpec(rsql, DistributionSetFields.class),
                        DistributionSetSpecification.hasTag(tagId), DistributionSetSpecification.isNotDeleted()),
                pageable);
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

    /**
     * Asserts the meta-data quota for the software module with the given ID.
     *
     * @param requested Number of meta-data entries to be created.
     */
    @Override
    protected void assertMetadataQuota(final long requested) {
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerDistributionSet();
        QuotaHelper.assertAssignmentQuota(requested, maxMetaData, String.class, DistributionSet.class);
    }

    private static boolean completeComparison(final AtomicReference<Node.Comparison> completeComparison) {
        final Node.Comparison comparison = completeComparison.get();
        if (comparison.getOp() == Node.Comparison.Operator.EQ) {
            return Boolean.parseBoolean(String.valueOf(comparison.getValue()));
        } else if (comparison.getOp() == Node.Comparison.Operator.NE) {
            return !Boolean.parseBoolean(String.valueOf(comparison.getValue()));
        } else {
            throw new RSQLParameterSyntaxException("Unsupported operator for 'complete': " + comparison.getOp());
        }
    }

    private static Page<JpaDistributionSet> filter(final Page<JpaDistributionSet> page, final boolean completed) {
        final List<JpaDistributionSet> filtered = page.getContent().stream()
                .filter(ds -> ds.isComplete() == completed)
                .toList();
        return new PageImpl<>(filtered, page.getPageable(), page.getTotalElements());
    }

    private static Collection<Long> notFound(final Collection<Long> distributionSetIds, final List<JpaDistributionSet> foundDistributionSets) {
        final Map<Long, JpaDistributionSet> foundDistributionSetMap = foundDistributionSets.stream()
                .collect(Collectors.toMap(JpaDistributionSet::getId, Function.identity()));
        return distributionSetIds.stream().filter(id -> !foundDistributionSetMap.containsKey(id)).toList();
    }

    private JpaDistributionSet getValid0(final long id) {
        final JpaDistributionSet distributionSet = jpaRepository.getById(id);
        if (!distributionSet.isValid()) {
            throw new InvalidDistributionSetException(
                    "Distribution set of type " + distributionSet.getType().getKey() + " is invalid: " + distributionSet.getId());
        }
        return distributionSet;
    }

    private List<JpaDistributionSet> updateTag(
            final Collection<Long> dsIds, final long dsTagId,
            final BiFunction<DistributionSetTag, JpaDistributionSet, JpaDistributionSet> updater) {
        final DistributionSetTag tag = distributionSetTagManagement.get(dsTagId);
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

    private void assertSoftwareModuleQuota(final Long id, final int requested) {
        QuotaHelper.assertAssignmentQuota(id, requested, quotaManagement.getMaxSoftwareModulesPerDistributionSet(),
                SoftwareModule.class, DistributionSet.class, softwareModuleRepository::countByAssignedToId);
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

    private JpaDistributionSet toJpaDistributionSet(final DistributionSet distributionSet) {
        if (distributionSet instanceof JpaDistributionSet jpaDistributionSet) {
            return jpaDistributionSet;
        } else {
            return jpaRepository.getById(distributionSet.getId());
        }
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
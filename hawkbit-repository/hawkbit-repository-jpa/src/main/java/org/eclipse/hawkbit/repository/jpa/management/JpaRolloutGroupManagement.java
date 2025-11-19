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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup_;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.qfields.RolloutGroupFields;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link RolloutGroupManagement}.
 */
@Validated
@Transactional(readOnly = true)
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "rollout-group-management" }, matchIfMissing = true)
public class JpaRolloutGroupManagement implements RolloutGroupManagement {

    private final RolloutGroupRepository rolloutGroupRepository;
    private final RolloutRepository rolloutRepository;
    private final ActionRepository actionRepository;
    private final TargetRepository targetRepository;
    private final EntityManager entityManager;

    @SuppressWarnings("java:S107")
    protected JpaRolloutGroupManagement(final RolloutGroupRepository rolloutGroupRepository,
            final RolloutRepository rolloutRepository, final ActionRepository actionRepository,
            final TargetRepository targetRepository, final EntityManager entityManager) {
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.rolloutRepository = rolloutRepository;
        this.actionRepository = actionRepository;
        this.targetRepository = targetRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Page<RolloutGroup> findByRolloutWithDetailedStatus(final long rolloutId, final Pageable pageable) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        final Page<JpaRolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutId(rolloutId, pageable);
        final List<Long> rolloutGroupIds = rolloutGroups.getContent().stream().map(RolloutGroup::getId).toList();
        if (rolloutGroupIds.isEmpty()) {
            // groups might have been already deleted, so return empty list.
            return new PageImpl<>(Collections.emptyList());
        }

        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRolloutGroup(rolloutGroupIds);
        for (final JpaRolloutGroup rolloutGroup : rolloutGroups) {
            final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                    allStatesForRollout.get(rolloutGroup.getId()), (long) rolloutGroup.getTotalTargets(),
                    rolloutGroup.getRollout().getActionType());
            rolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        }

        return JpaManagementHelper.convertPage(rolloutGroups, pageable);
    }

    @Override
    public RolloutGroup get(final long rolloutGroupId) {
        return rolloutGroupRepository.findById(rolloutGroupId).map(RolloutGroup.class::cast)
                .orElseThrow(() -> new EntityNotFoundException(RolloutGroup.class, rolloutGroupId));
    }

    @Override
    public RolloutGroup getWithDetailedStatus(final long rolloutGroupId) {
        final JpaRolloutGroup jpaRolloutGroup = (JpaRolloutGroup) rolloutGroupRepository.findById(rolloutGroupId).map(RolloutGroup.class::cast)
                .orElseThrow(() -> new EntityNotFoundException(RolloutGroup.class, rolloutGroupId));

        List<TotalTargetCountActionStatus> rolloutStatusCountItems = RolloutStatusCache.getRolloutGroupStatus(rolloutGroupId);
        if (CollectionUtils.isEmpty(rolloutStatusCountItems)) {
            rolloutStatusCountItems = actionRepository.getStatusCountByRolloutGroupId(rolloutGroupId);
            RolloutStatusCache.putRolloutGroupStatus(rolloutGroupId, rolloutStatusCountItems);
        }

        final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                rolloutStatusCountItems, (long) jpaRolloutGroup.getTotalTargets(), jpaRolloutGroup.getRollout().getActionType());
        jpaRolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        return jpaRolloutGroup;
    }

    @Override
    public Page<RolloutGroup> findByRolloutAndRsql(final long rolloutId, final String rsql, final Pageable pageable) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        final List<Specification<JpaRolloutGroup>> specList = Arrays.asList(
                QLSupport.getInstance().buildSpec(rsql, RolloutGroupFields.class),
                (root, query, cb) -> cb.equal(root.get(JpaRolloutGroup_.rollout).get(AbstractJpaBaseEntity_.id), rolloutId));

        return JpaManagementHelper.findAllWithCountBySpec(rolloutGroupRepository, specList, pageable);
    }

    @Override
    public Page<RolloutGroup> findByRolloutAndRsqlWithDetailedStatus(final long rolloutId, final String rsql, final Pageable pageable) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        final Page<RolloutGroup> rolloutGroups = findByRolloutAndRsql(rolloutId, rsql, pageable);
        final List<Long> rolloutGroupIds = rolloutGroups.getContent().stream().map(RolloutGroup::getId).toList();
        if (rolloutGroupIds.isEmpty()) {
            // groups might already have been deleted, so return empty list.
            return new PageImpl<>(Collections.emptyList());
        }

        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRolloutGroup(rolloutGroupIds);
        for (final RolloutGroup rolloutGroup : rolloutGroups) {
            final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                    allStatesForRollout.get(rolloutGroup.getId()), (long) rolloutGroup.getTotalTargets(),
                    rolloutGroup.getRollout().getActionType());
            ((JpaRolloutGroup) rolloutGroup).setTotalTargetCountStatus(totalTargetCountStatus);
        }

        return JpaManagementHelper.convertPage(rolloutGroups, pageable);
    }

    @Override
    public Page<RolloutGroup> findByRollout(final long rolloutId, final Pageable pageable) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        return JpaManagementHelper.convertPage(rolloutGroupRepository.findByRolloutId(rolloutId, pageable), pageable);
    }

    @Override
    public Page<Target> findTargetsOfRolloutGroup(final long rolloutGroupId, final Pageable page) {
        final JpaRolloutGroup rolloutGroup = rolloutGroupRepository.getById(rolloutGroupId);

        if (isRolloutStatusReady(rolloutGroup)) {
            // in case of status ready the action has not been created yet and
            // the relation information between target and rollout-group is
            // stored in the #TargetRolloutGroup.
            return JpaManagementHelper.findAllWithCountBySpec(
                    targetRepository,
                    Collections.singletonList(TargetSpecifications.isInRolloutGroup(rolloutGroupId)), page
            );
        }
        return JpaManagementHelper.findAllWithCountBySpec(targetRepository,
                Collections.singletonList(TargetSpecifications.isInActionRolloutGroup(rolloutGroupId)), page
        );
    }

    @Override
    public Page<Target> findTargetsOfRolloutGroupByRsql(final long rolloutGroupId, final String rsql, final Pageable pageable) {
        throwExceptionIfRolloutGroupDoesNotExist(rolloutGroupId);

        final List<Specification<JpaTarget>> specList = Arrays.asList(
                QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                (root, query, cb) -> {
                    final ListJoin<JpaTarget, RolloutTargetGroup> rolloutTargetJoin = root.join(JpaTarget_.rolloutTargetGroup);
                    return cb.equal(rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup).get(AbstractJpaBaseEntity_.id), rolloutGroupId);
                });

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, specList, pageable);
    }

    @Override
    public long countTargetsOfRolloutsGroup(final long rolloutGroupId) {
        throwExceptionIfRolloutGroupDoesNotExist(rolloutGroupId);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        final Root<RolloutTargetGroup> countQueryFrom = countQuery.from(RolloutTargetGroup.class);
        countQuery.select(cb.count(countQueryFrom))
                .where(getRolloutGroupTargetWithRolloutGroupJoinCondition(rolloutGroupId, cb, countQueryFrom));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private static boolean isRolloutStatusReady(final RolloutGroup rolloutGroup) {
        return rolloutGroup != null && (RolloutStatus.READY == rolloutGroup.getRollout().getStatus());
    }

    private void throwEntityNotFoundExceptionIfRolloutDoesNotExist(final Long rolloutId) {
        if (!rolloutRepository.existsById(rolloutId)) {
            throw new EntityNotFoundException(Rollout.class, rolloutId);
        }
    }

    private Map<Long, List<TotalTargetCountActionStatus>> getStatusCountItemForRolloutGroup(final List<Long> groupIds) {
        final Map<Long, List<TotalTargetCountActionStatus>> fromCache = RolloutStatusCache
                .getRolloutGroupStatus(groupIds);

        final List<Long> rolloutGroupIds = groupIds.stream().filter(id -> !fromCache.containsKey(id)).toList();
        if (!rolloutGroupIds.isEmpty()) {
            final List<TotalTargetCountActionStatus> resultList = actionRepository
                    .getStatusCountByRolloutGroupIds(rolloutGroupIds);
            final Map<Long, List<TotalTargetCountActionStatus>> fromDb = resultList.stream()
                    .collect(Collectors.groupingBy(TotalTargetCountActionStatus::getId));

            RolloutStatusCache.putRolloutGroupStatus(fromDb);

            fromCache.putAll(fromDb);
        }

        return fromCache;
    }

    private Predicate getRolloutGroupTargetWithRolloutGroupJoinCondition(final long rolloutGroupId,
            final CriteriaBuilder cb, final Root<RolloutTargetGroup> targetRoot) {
        return cb.equal(targetRoot.get(RolloutTargetGroup_.rolloutGroup).get(AbstractJpaBaseEntity_.id), rolloutGroupId);
    }

    private void throwExceptionIfRolloutGroupDoesNotExist(final Long rolloutGroupId) {
        if (!rolloutGroupRepository.existsById(rolloutGroupId)) {
            throw new EntityNotFoundException(RolloutGroup.class, rolloutGroupId);
        }
    }
}
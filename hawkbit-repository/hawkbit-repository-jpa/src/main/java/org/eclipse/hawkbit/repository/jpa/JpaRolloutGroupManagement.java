/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup_;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link RolloutGroupManagement}.
 */
@Validated
@Transactional(readOnly = true)
public class JpaRolloutGroupManagement implements RolloutGroupManagement {

    private final RolloutGroupRepository rolloutGroupRepository;

    private final RolloutRepository rolloutRepository;

    private final ActionRepository actionRepository;

    private final TargetRepository targetRepository;

    private final EntityManager entityManager;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final RolloutStatusCache rolloutStatusCache;

    private final Database database;

    JpaRolloutGroupManagement(final RolloutGroupRepository rolloutGroupRepository,
            final RolloutRepository rolloutRepository, final ActionRepository actionRepository,
            final TargetRepository targetRepository, final EntityManager entityManager,
            final VirtualPropertyReplacer virtualPropertyReplacer, final RolloutStatusCache rolloutStatusCache,
            final Database database) {

        this.rolloutGroupRepository = rolloutGroupRepository;
        this.rolloutRepository = rolloutRepository;
        this.actionRepository = actionRepository;
        this.targetRepository = targetRepository;
        this.entityManager = entityManager;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.rolloutStatusCache = rolloutStatusCache;
        this.database = database;
    }

    @Override
    public Optional<RolloutGroup> get(final long rolloutGroupId) {
        return rolloutGroupRepository.findById(rolloutGroupId).map(rg -> (RolloutGroup) rg);
    }

    @Override
    public Page<RolloutGroup> findByRollout(final Pageable pageable, final long rolloutId) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        return JpaManagementHelper.convertPage(rolloutGroupRepository.findByRolloutId(rolloutId, pageable), pageable);
    }

    @Override
    public Page<RolloutGroup> findByRolloutAndRsql(final Pageable pageable, final long rolloutId,
            final String rsqlParam) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        final List<Specification<JpaRolloutGroup>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, RolloutGroupFields.class, virtualPropertyReplacer,
                        database),
                (root, query, cb) -> cb.equal(root.get(JpaRolloutGroup_.rollout).get(JpaRollout_.id), rolloutId));

        return JpaManagementHelper.findAllWithCountBySpec(rolloutGroupRepository, pageable, specList);
    }

    private void throwEntityNotFoundExceptionIfRolloutDoesNotExist(final Long rolloutId) {
        if (!rolloutRepository.existsById(rolloutId)) {
            throw new EntityNotFoundException(Rollout.class, rolloutId);
        }
    }

    @Override
    public Page<RolloutGroup> findByRolloutWithDetailedStatus(final Pageable pageable, final long rolloutId) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        final Page<JpaRolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutId(rolloutId, pageable);
        final List<Long> rolloutGroupIds = rolloutGroups.getContent().stream().map(RolloutGroup::getId)
                .collect(Collectors.toList());

        if (rolloutGroupIds.isEmpty()) {
            // groups might already deleted, so return empty list.
            return new PageImpl<>(Collections.emptyList());
        }

        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRolloutGroup(
                rolloutGroupIds);

        for (final JpaRolloutGroup rolloutGroup : rolloutGroups) {
            final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                    allStatesForRollout.get(rolloutGroup.getId()), Long.valueOf(rolloutGroup.getTotalTargets()),
                    rolloutGroup.getRollout().getActionType());
            rolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        }

        return JpaManagementHelper.convertPage(rolloutGroups, pageable);
    }

    @Override
    public Optional<RolloutGroup> getWithDetailedStatus(final long rolloutGroupId) {
        final Optional<RolloutGroup> rolloutGroup = get(rolloutGroupId);

        if (!rolloutGroup.isPresent()) {
            return rolloutGroup;
        }

        final JpaRolloutGroup jpaRolloutGroup = (JpaRolloutGroup) rolloutGroup.get();

        List<TotalTargetCountActionStatus> rolloutStatusCountItems = rolloutStatusCache
                .getRolloutGroupStatus(rolloutGroupId);

        if (CollectionUtils.isEmpty(rolloutStatusCountItems)) {
            rolloutStatusCountItems = actionRepository.getStatusCountByRolloutGroupId(rolloutGroupId);
            rolloutStatusCache.putRolloutGroupStatus(rolloutGroupId, rolloutStatusCountItems);
        }

        final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(rolloutStatusCountItems,
                Long.valueOf(jpaRolloutGroup.getTotalTargets()), jpaRolloutGroup.getRollout().getActionType());
        jpaRolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        return rolloutGroup;

    }

    private Map<Long, List<TotalTargetCountActionStatus>> getStatusCountItemForRolloutGroup(final List<Long> groupIds) {
        final Map<Long, List<TotalTargetCountActionStatus>> fromCache = rolloutStatusCache
                .getRolloutGroupStatus(groupIds);

        final List<Long> rolloutGroupIds = groupIds.stream().filter(id -> !fromCache.containsKey(id))
                .collect(Collectors.toList());

        if (!rolloutGroupIds.isEmpty()) {
            final List<TotalTargetCountActionStatus> resultList = actionRepository
                    .getStatusCountByRolloutGroupId(rolloutGroupIds);
            final Map<Long, List<TotalTargetCountActionStatus>> fromDb = resultList.stream()
                    .collect(Collectors.groupingBy(TotalTargetCountActionStatus::getId));

            rolloutStatusCache.putRolloutGroupStatus(fromDb);

            fromCache.putAll(fromDb);
        }

        return fromCache;
    }

    @Override
    public Page<Target> findTargetsOfRolloutGroupByRsql(final Pageable pageable, final long rolloutGroupId,
            final String rsqlParam) {
        throwExceptionIfRolloutGroupDoesNotExist(rolloutGroupId);

        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, TargetFields.class, virtualPropertyReplacer, database),
                (root, query, cb) -> {
                    final ListJoin<JpaTarget, RolloutTargetGroup> rolloutTargetJoin = root
                            .join(JpaTarget_.rolloutTargetGroup);
                    return cb.equal(rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id),
                            rolloutGroupId);
                });

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageable, specList);
    }

    @Override
    public Page<Target> findTargetsOfRolloutGroup(final Pageable page, final long rolloutGroupId) {
        final JpaRolloutGroup rolloutGroup = rolloutGroupRepository.findById(rolloutGroupId)
                .orElseThrow(() -> new EntityNotFoundException(RolloutGroup.class, rolloutGroupId));

        if (isRolloutStatusReady(rolloutGroup)) {
            // in case of status ready the action has not been created yet and
            // the relation information between target and rollout-group is
            // stored in the #TargetRolloutGroup.
            return JpaManagementHelper.findAllWithCountBySpec(targetRepository, page,
                    Collections.singletonList(TargetSpecifications.isInRolloutGroup(rolloutGroupId)));
        }
        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, page,
                Collections.singletonList(TargetSpecifications.isInActionRolloutGroup(rolloutGroupId)));
    }

    private static boolean isRolloutStatusReady(final RolloutGroup rolloutGroup) {
        return rolloutGroup != null && (RolloutStatus.READY == rolloutGroup.getRollout().getStatus());
    }

    @Override
    public Page<TargetWithActionStatus> findAllTargetsOfRolloutGroupWithActionStatus(final Pageable pageRequest,
            final long rolloutGroupId) {

        throwExceptionIfRolloutGroupDoesNotExist(rolloutGroupId);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final Root<RolloutTargetGroup> targetRoot = query.distinct(true).from(RolloutTargetGroup.class);
        final Join<RolloutTargetGroup, JpaTarget> targetJoin = targetRoot.join(RolloutTargetGroup_.target);
        final ListJoin<RolloutTargetGroup, JpaAction> actionJoin = targetRoot.join(RolloutTargetGroup_.actions,
                JoinType.LEFT);

        final CriteriaQuery<Object[]> multiselect = query
                .multiselect(targetJoin, actionJoin.get(JpaAction_.status),
                        actionJoin.get(JpaAction_.lastActionStatusCode))
                .where(getRolloutGroupTargetWithRolloutGroupJoinCondition(rolloutGroupId, cb, targetRoot))
                .orderBy(getOrderBy(pageRequest, cb, targetJoin, actionJoin));
        final List<TargetWithActionStatus> targetWithActionStatus = entityManager.createQuery(multiselect)
                .setFirstResult((int) pageRequest.getOffset()).setMaxResults(pageRequest.getPageSize()).getResultList()
                .stream().map(this::getTargetWithActionStatusFromQuery).collect(Collectors.toList());

        return new PageImpl<>(targetWithActionStatus, pageRequest, 0);
    }

    private Predicate getRolloutGroupTargetWithRolloutGroupJoinCondition(final long rolloutGroupId, final CriteriaBuilder cb,
            final Root<RolloutTargetGroup> targetRoot) {
        return cb.equal(targetRoot.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id), //
                rolloutGroupId);
    }

    private TargetWithActionStatus getTargetWithActionStatusFromQuery(final Object[] o) {
        return new TargetWithActionStatus((Target) o[0], (Action.Status) o[1],
                (Integer) o[2]);
    }

    private List<Order> getOrderBy(final Pageable pageRequest, final CriteriaBuilder cb,
            final Join<RolloutTargetGroup, JpaTarget> targetJoin,
            final ListJoin<RolloutTargetGroup, JpaAction> actionJoin) {

        return pageRequest.getSort().get().flatMap(order -> {
            final List<Order> orders;
            final String property = order.getProperty();
            // we consider status, last_action_status_code as property from JpaAction ...
            if ("status".equals(property) || "lastActionStatusCode".equals(property)) {
                orders = QueryUtils.toOrders(Sort.by(order.getDirection(), property), actionJoin, cb);
            }
            // ... and every other property from JpaTarget
            else {
                orders = QueryUtils.toOrders(Sort.by(order.getDirection(), property), targetJoin, cb);
            }
            return orders.stream();
        }).collect(Collectors.toList());
    }

    @Override
    public long countTargetsOfRolloutsGroup(final long rolloutGroupId) {
        throwExceptionIfRolloutGroupDoesNotExist(rolloutGroupId);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        final Root<RolloutTargetGroup> countQueryFrom = countQuery.from(RolloutTargetGroup.class);
        countQuery.select(cb.count(countQueryFrom)).where(getRolloutGroupTargetWithRolloutGroupJoinCondition(rolloutGroupId, cb, countQueryFrom));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private void throwExceptionIfRolloutGroupDoesNotExist(final Long rolloutGroupId) {
        if (!rolloutGroupRepository.existsById(rolloutGroupId)) {
            throw new EntityNotFoundException(RolloutGroup.class, rolloutGroupId);
        }
    }

    @Override
    public long countByRollout(final long rolloutId) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        return rolloutGroupRepository.countByRolloutId(rolloutId);
    }

}

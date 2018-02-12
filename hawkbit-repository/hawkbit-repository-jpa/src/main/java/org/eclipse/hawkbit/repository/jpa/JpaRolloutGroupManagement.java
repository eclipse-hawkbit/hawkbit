/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

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
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link RolloutGroupManagement}.
 */
@Validated
@Transactional(readOnly = true)
public class JpaRolloutGroupManagement implements RolloutGroupManagement {

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Autowired
    private RolloutStatusCache rolloutStatusCache;

    @Override
    public Optional<RolloutGroup> get(final long rolloutGroupId) {
        return Optional.ofNullable(rolloutGroupRepository.findOne(rolloutGroupId));
    }

    @Override
    public Page<RolloutGroup> findByRollout(final Pageable pageable, final long rolloutId) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        return convertPage(rolloutGroupRepository.findByRolloutId(rolloutId, pageable), pageable);
    }

    private static Page<RolloutGroup> convertPage(final Page<JpaRolloutGroup> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    private static Page<Target> convertTPage(final Page<JpaTarget> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Page<RolloutGroup> findByRolloutAndRsql(final Pageable pageable, final long rolloutId,
            final String rsqlParam) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        final Specification<JpaRolloutGroup> specification = RSQLUtility.parse(rsqlParam, RolloutGroupFields.class,
                virtualPropertyReplacer);

        return convertPage(
                rolloutGroupRepository
                        .findAll(
                                (root, query,
                                        criteriaBuilder) -> criteriaBuilder.and(criteriaBuilder.equal(
                                                root.get(JpaRolloutGroup_.rollout).get(JpaRollout_.id), rolloutId),
                                                specification.toPredicate(root, query, criteriaBuilder)),
                                pageable),
                pageable);
    }

    private void throwEntityNotFoundExceptionIfRolloutDoesNotExist(final Long rolloutId) {
        if (!rolloutRepository.exists(rolloutId)) {
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
                    allStatesForRollout.get(rolloutGroup.getId()), Long.valueOf(rolloutGroup.getTotalTargets()));
            rolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        }

        return convertPage(rolloutGroups, pageable);
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
                Long.valueOf(jpaRolloutGroup.getTotalTargets()));
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

        final Specification<JpaTarget> rsqlSpecification = RSQLUtility.parse(rsqlParam, TargetFields.class,
                virtualPropertyReplacer);

        return convertTPage(targetRepository.findAll((root, query, criteriaBuilder) -> {
            final ListJoin<JpaTarget, RolloutTargetGroup> rolloutTargetJoin = root.join(JpaTarget_.rolloutTargetGroup);
            return criteriaBuilder.and(rsqlSpecification.toPredicate(root, query, criteriaBuilder),
                    criteriaBuilder.equal(
                            rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id),
                            rolloutGroupId));
        }, pageable), pageable);
    }

    @Override
    public Page<Target> findTargetsOfRolloutGroup(final Pageable page, final long rolloutGroupId) {
        final JpaRolloutGroup rolloutGroup = rolloutGroupRepository.findById(rolloutGroupId)
                .orElseThrow(() -> new EntityNotFoundException(RolloutGroup.class, rolloutGroupId));

        if (isRolloutStatusReady(rolloutGroup)) {
            // in case of status ready the action has not been created yet and
            // the relation information between target and rollout-group is
            // stored in the #TargetRolloutGroup.
            return targetRepository.findByRolloutTargetGroupRolloutGroupId(rolloutGroupId, page);
        }
        return targetRepository.findByActionsRolloutGroupId(rolloutGroupId, page);
    }

    private static boolean isRolloutStatusReady(final RolloutGroup rolloutGroup) {
        return rolloutGroup != null && RolloutStatus.READY.equals(rolloutGroup.getRollout().getStatus());
    }

    @Override
    public Page<TargetWithActionStatus> findAllTargetsOfRolloutGroupWithActionStatus(final Pageable pageRequest,
            final long rolloutGroupId) {
        throwExceptionIfRolloutGroupDoesNotExist(rolloutGroupId);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);

        final Root<RolloutTargetGroup> targetRoot = query.distinct(true).from(RolloutTargetGroup.class);
        final Join<RolloutTargetGroup, JpaTarget> targetJoin = targetRoot.join(RolloutTargetGroup_.target);
        final ListJoin<RolloutTargetGroup, JpaAction> actionJoin = targetRoot.join(RolloutTargetGroup_.actions,
                JoinType.LEFT);

        final Root<RolloutTargetGroup> countQueryFrom = countQuery.distinct(true).from(RolloutTargetGroup.class);
        countQueryFrom.join(RolloutTargetGroup_.target);
        countQueryFrom.join(RolloutTargetGroup_.actions, JoinType.LEFT);
        countQuery.select(cb.count(countQueryFrom)).where(cb
                .equal(countQueryFrom.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id), rolloutGroupId));
        final Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        final CriteriaQuery<Object[]> multiselect = query.multiselect(targetJoin, actionJoin.get(JpaAction_.status))
                .where(cb.equal(targetRoot.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id),
                        rolloutGroupId));
        final List<TargetWithActionStatus> targetWithActionStatus = entityManager.createQuery(multiselect)
                .setFirstResult(pageRequest.getOffset()).setMaxResults(pageRequest.getPageSize()).getResultList()
                .stream().map(o -> new TargetWithActionStatus((Target) o[0], (Action.Status) o[1]))
                .collect(Collectors.toList());

        return new PageImpl<>(targetWithActionStatus, pageRequest, totalCount);
    }

    @Override
    public long countTargetsOfRolloutsGroup(final long rolloutGroupId) {
        throwExceptionIfRolloutGroupDoesNotExist(rolloutGroupId);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        final Root<RolloutTargetGroup> countQueryFrom = countQuery.from(RolloutTargetGroup.class);
        countQuery.select(cb.count(countQueryFrom)).where(cb
                .equal(countQueryFrom.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id), rolloutGroupId));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private void throwExceptionIfRolloutGroupDoesNotExist(final Long rolloutGroupId) {
        if (!rolloutGroupRepository.exists(rolloutGroupId)) {
            throw new EntityNotFoundException(RolloutGroup.class, rolloutGroupId);
        }
    }

    @Override
    public long countByRollout(final long rolloutId) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        return rolloutGroupRepository.countByRolloutId(rolloutId);
    }

}

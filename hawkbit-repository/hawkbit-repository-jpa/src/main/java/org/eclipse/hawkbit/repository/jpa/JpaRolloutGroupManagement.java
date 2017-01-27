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
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link RolloutGroupManagement}.
 */
@Validated
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public class JpaRolloutGroupManagement implements RolloutGroupManagement {

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Override
    public RolloutGroup findRolloutGroupById(final Long rolloutGroupId) {
        return rolloutGroupRepository.findOne(rolloutGroupId);
    }

    @Override
    public Page<RolloutGroup> findRolloutGroupsByRolloutId(final Long rolloutId, final Pageable pageable) {
        return convertPage(rolloutGroupRepository.findByRolloutId(rolloutId, pageable), pageable);
    }

    private static Page<RolloutGroup> convertPage(final Page<JpaRolloutGroup> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    private static Page<Target> convertTPage(final Page<JpaTarget> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Page<RolloutGroup> findRolloutGroupsAll(final Long rolloutId, final String rsqlParam,
            final Pageable pageable) {

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

    @Override
    public Page<RolloutGroup> findAllRolloutGroupsWithDetailedStatus(final Long rolloutId, final Pageable pageable) {
        final Page<JpaRolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutId(rolloutId, pageable);
        final List<Long> rolloutGroupIds = rolloutGroups.getContent().stream().map(rollout -> rollout.getId())
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
    public RolloutGroup findRolloutGroupWithDetailedStatus(final Long rolloutGroupId) {
        final JpaRolloutGroup rolloutGroup = (JpaRolloutGroup) findRolloutGroupById(rolloutGroupId);
        final List<TotalTargetCountActionStatus> rolloutStatusCountItems = actionRepository
                .getStatusCountByRolloutGroupId(rolloutGroupId);

        final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(rolloutStatusCountItems,
                Long.valueOf(rolloutGroup.getTotalTargets()));
        rolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        return rolloutGroup;

    }

    private Map<Long, List<TotalTargetCountActionStatus>> getStatusCountItemForRolloutGroup(
            final List<Long> rolloutGroupIds) {
        final List<TotalTargetCountActionStatus> resultList = actionRepository
                .getStatusCountByRolloutGroupId(rolloutGroupIds);
        return resultList.stream().collect(Collectors.groupingBy(TotalTargetCountActionStatus::getId));
    }

    @Override
    public Page<Target> findRolloutGroupTargets(final Long rolloutGroupId, final String rsqlParam,
            final Pageable pageable) {

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
    public Page<Target> findRolloutGroupTargets(final Long rolloutGroupId, final Pageable page) {
        final JpaRolloutGroup rolloutGroup = Optional.ofNullable(rolloutGroupRepository.findOne(rolloutGroupId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Rollout Group with given ID " + rolloutGroupId + " not found."));

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
    public Page<TargetWithActionStatus> findAllTargetsWithActionStatus(final PageRequest pageRequest,
            final Long rolloutGroupId) {

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
    public Long countTargetsOfRolloutsGroup(@NotNull final Long rolloutGroupId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        final Root<RolloutTargetGroup> countQueryFrom = countQuery.from(RolloutTargetGroup.class);
        countQuery.select(cb.count(countQueryFrom)).where(cb
                .equal(countQueryFrom.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id), rolloutGroupId));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

}

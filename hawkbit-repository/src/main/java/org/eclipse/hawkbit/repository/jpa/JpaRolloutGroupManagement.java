/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action_;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup_;
import org.eclipse.hawkbit.repository.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.model.RolloutTargetGroup_;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.repository.model.Target_;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * RolloutGroupManagement to control rollout groups. This service secures all
 * the functionality based on the {@link PreAuthorize} annotation on methods.
 */
@Validated
@Service
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

    @Override
    public RolloutGroup findRolloutGroupById(final Long rolloutGroupId) {
        return rolloutGroupRepository.findOne(rolloutGroupId);
    }

    @Override
    public Page<RolloutGroup> findRolloutGroupsByRolloutId(final Long rolloutId, final Pageable page) {
        return rolloutGroupRepository.findByRolloutId(rolloutId, page);
    }

    @Override
    public Page<RolloutGroup> findRolloutGroupsAll(final Rollout rollout,
            final Specification<RolloutGroup> specification, final Pageable page) {
        return rolloutGroupRepository.findAll((root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get(RolloutGroup_.rollout), rollout),
                specification.toPredicate(root, query, criteriaBuilder)), page);
    }

    @Override
    public Page<RolloutGroup> findAllRolloutGroupsWithDetailedStatus(final Long rolloutId, final Pageable page) {
        final Page<RolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutId(rolloutId, page);
        final List<Long> rolloutGroupIds = rolloutGroups.getContent().stream().map(rollout -> rollout.getId())
                .collect(Collectors.toList());
        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRolloutGroup(
                rolloutGroupIds);

        for (final RolloutGroup rolloutGroup : rolloutGroups) {
            final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                    allStatesForRollout.get(rolloutGroup.getId()), rolloutGroup.getTotalTargets());
            rolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        }

        return rolloutGroups;
    }

    @Override
    public RolloutGroup findRolloutGroupWithDetailedStatus(final Long rolloutGroupId) {
        final RolloutGroup rolloutGroup = findRolloutGroupById(rolloutGroupId);
        final List<TotalTargetCountActionStatus> rolloutStatusCountItems = actionRepository
                .getStatusCountByRolloutGroupId(rolloutGroupId);

        final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(rolloutStatusCountItems,
                rolloutGroup.getTotalTargets());
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
    public Page<Target> findRolloutGroupTargets(final RolloutGroup rolloutGroup,
            final Specification<Target> specification, final Pageable page) {
        return targetRepository.findAll((root, query, criteriaBuilder) -> {
            final ListJoin<Target, RolloutTargetGroup> rolloutTargetJoin = root.join(Target_.rolloutTargetGroup);
            return criteriaBuilder.and(specification.toPredicate(root, query, criteriaBuilder),
                    criteriaBuilder.equal(rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup), rolloutGroup));
        }, page);
    }

    @Override
    public Page<Target> findRolloutGroupTargets(final RolloutGroup rolloutGroup, final Pageable page) {
        if (isRolloutStatusReady(rolloutGroup)) {
            // in case of status ready the action has not been created yet and
            // the relation information between target and rollout-group is
            // stored in the #TargetRolloutGroup.
            return targetRepository.findByRolloutTargetGroupRolloutGroupId(rolloutGroup.getId(), page);
        }
        return targetRepository.findByActionsRolloutGroup(rolloutGroup, page);
    }

    private static boolean isRolloutStatusReady(final RolloutGroup rolloutGroup) {
        return rolloutGroup != null && RolloutStatus.READY.equals(rolloutGroup.getRollout().getStatus());
    }

    @Override
    public Page<TargetWithActionStatus> findAllTargetsWithActionStatus(final PageRequest pageRequest,
            final RolloutGroup rolloutGroup) {

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);

        final Root<RolloutTargetGroup> targetRoot = query.distinct(true).from(RolloutTargetGroup.class);
        final Join<RolloutTargetGroup, Target> targetJoin = targetRoot.join(RolloutTargetGroup_.target);
        final ListJoin<RolloutTargetGroup, Action> actionJoin = targetRoot.join(RolloutTargetGroup_.actions,
                JoinType.LEFT);

        final Root<RolloutTargetGroup> countQueryFrom = countQuery.distinct(true).from(RolloutTargetGroup.class);
        countQueryFrom.join(RolloutTargetGroup_.target);
        countQueryFrom.join(RolloutTargetGroup_.actions, JoinType.LEFT);
        countQuery.select(cb.count(countQueryFrom))
                .where(cb.equal(countQueryFrom.get(RolloutTargetGroup_.rolloutGroup), rolloutGroup));
        final Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        final CriteriaQuery<Object[]> multiselect = query.multiselect(targetJoin, actionJoin.get(Action_.status))
                .where(cb.equal(targetRoot.get(RolloutTargetGroup_.rolloutGroup), rolloutGroup));
        final List<TargetWithActionStatus> targetWithActionStatus = entityManager.createQuery(multiselect)
                .setFirstResult(pageRequest.getOffset()).setMaxResults(pageRequest.getPageSize()).getResultList()
                .stream().map(o -> new TargetWithActionStatus((Target) o[0], (Action.Status) o[1]))
                .collect(Collectors.toList());
        return new PageImpl<>(targetWithActionStatus, pageRequest, totalCount);
    }
}

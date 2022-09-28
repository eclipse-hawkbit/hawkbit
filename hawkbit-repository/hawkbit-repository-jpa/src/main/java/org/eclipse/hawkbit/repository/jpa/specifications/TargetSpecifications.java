/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

/**
 * Specifications class for {@link Target}s. The class provides Spring Data JPQL
 * Specifications.
 *
 */
public final class TargetSpecifications {
    private TargetSpecifications() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link Target}s including
     * {@link TargetTag}s.
     *
     * @param controllerIDs
     *            to search for
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> byControllerIdWithTagsInJoin(final Collection<String> controllerIDs) {
        return (targetRoot, query, cb) -> {
            final Predicate predicate = targetRoot.get(JpaTarget_.controllerId).in(controllerIDs);
            targetRoot.fetch(JpaTarget_.tags, JoinType.LEFT);
            query.distinct(true);
            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by controllerId
     *
     * @param controllerID
     *            to search for
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasControllerId(final String controllerID) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTarget_.controllerId), controllerID);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by controllerId
     *
     * @param controllerIDs
     *            to search for
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasControllerIdIn(final Collection<String> controllerIDs) {
        return (targetRoot, query, cb) -> targetRoot.get(JpaTarget_.controllerId).in(controllerIDs);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by controllerId
     *
     * @param id
     *            to search for
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasId(final Long id) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTarget_.id), id);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by controllerId
     *
     * @param ids
     *            to search for
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasIdIn(final Collection<Long> ids) {
        return (targetRoot, query, cb) -> targetRoot.get(JpaTarget_.id).in(ids);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that have the
     * request controller attributes flag set
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasRequestControllerAttributesTrue() {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTarget_.requestControllerAttributes), true);
    }

    /**
     * {@link Specification} for retrieving {@link JpaTarget}s including
     * {@link JpaTarget#getAssignedDistributionSet()}.
     *
     * @param controllerIDs
     *            to search for
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> byControllerIdWithAssignedDsInJoin(final Collection<String> controllerIDs) {
        return (targetRoot, query, cb) -> {

            final Predicate predicate = targetRoot.get(JpaTarget_.controllerId).in(controllerIDs);
            targetRoot.fetch(JpaTarget_.assignedDistributionSet, JoinType.LEFT);
            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "equal to any
     * given {@link TargetUpdateStatus}".
     *
     * @param updateStatus
     *            to be filtered on
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTargetUpdateStatus(final Collection<TargetUpdateStatus> updateStatus) {
        return (targetRoot, query, cb) -> targetRoot.get(JpaTarget_.updateStatus).in(updateStatus);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "equal to given
     * {@link TargetUpdateStatus}".
     *
     * @param updateStatus
     *            to be filtered on
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTargetUpdateStatus(final TargetUpdateStatus updateStatus) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTarget_.updateStatus), updateStatus);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "not equal to
     * given {@link TargetUpdateStatus}".
     *
     * @param updateStatus
     *            to be filtered on
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> notEqualToTargetUpdateStatus(final TargetUpdateStatus updateStatus) {
        return (targetRoot, query, cb) -> cb.not(cb.equal(targetRoot.get(JpaTarget_.updateStatus), updateStatus));
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are overdue. A
     * target is overdue if it did not respond during the configured
     * intervals:<br>
     * <em>poll_itvl + overdue_itvl</em>
     *
     * @param overdueTimestamp
     *            the calculated timestamp to compare with the last respond of a
     *            target (lastTargetQuery).<br>
     *            The <code>overdueTimestamp</code> has to be calculated with
     *            the following expression:<br>
     *            <em>overdueTimestamp = nowTimestamp - poll_itvl -
     *            overdue_itvl</em>
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> isOverdue(final long overdueTimestamp) {
        return (targetRoot, query, cb) -> cb.lessThanOrEqualTo(targetRoot.get(JpaTarget_.lastTargetQuery),
                overdueTimestamp);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "like attribute
     * value".
     *
     * @param searchText
     *            to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> likeAttributeValue(final String searchText) {
        return (targetRoot, query, cb) -> {
            final String searchTextToLower = searchText.toLowerCase();
            final MapJoin<JpaTarget, String, String> attributeMap = targetRoot.join(JpaTarget_.controllerAttributes,
                    JoinType.LEFT);
            query.distinct(true);
            return cb.like(cb.lower(attributeMap.value()), searchTextToLower);
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "like
     * controllerId or like name".
     *
     * @param searchText
     *            to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> likeControllerIdOrName(final String searchText) {
        return (targetRoot, query, cb) -> {
            final String searchTextToLower = searchText.toLowerCase();
            return cb.or(cb.like(cb.lower(targetRoot.get(JpaTarget_.controllerId)), searchTextToLower),
                    cb.like(cb.lower(targetRoot.get(JpaTarget_.name)), searchTextToLower));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "like
     * controllerId".
     *
     * @param distributionId
     *            to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasInstalledOrAssignedDistributionSet(@NotNull final Long distributionId) {
        return hasInstalledDistributionSet(distributionId).or(hasAssignedDistributionSet(distributionId));
    }

    /**
     * Finds all targets by given {@link Target#getControllerId()}s and which
     * are not yet assigned to given {@link DistributionSet}.
     *
     * @param tIDs
     *            to search for.
     * @param distributionId
     *            set that is not yet assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasControllerIdAndAssignedDistributionSetIdNot(final List<String> tIDs,
            @NotNull final Long distributionId) {
        return (targetRoot, query, cb) -> cb.and(targetRoot.get(JpaTarget_.controllerId).in(tIDs),
                cb.or(cb.notEqual(targetRoot.<JpaDistributionSet> get(JpaTarget_.assignedDistributionSet)
                        .get(JpaDistributionSet_.id), distributionId),
                        cb.isNull(targetRoot.<JpaDistributionSet> get(JpaTarget_.assignedDistributionSet))));
    }

    /**
     * {@link Specification} for retrieving {@link Target}s based on a
     * {@link TargetTag} name.
     *
     * @param tagName
     *            to search for
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTagName(final String tagName) {
        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTarget, JpaTargetTag> join = targetRoot.join(JpaTarget_.tags);
            return cb.equal(join.get(JpaTargetTag_.name), tagName);
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "has no tag
     * names"or "has at least on of the given tag names".
     *
     * @param tagNames
     *            to be filtered on
     * @param selectTargetWithNoTag
     *            flag to get targets with no tag assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTags(final String[] tagNames, final Boolean selectTargetWithNoTag) {
        return (targetRoot, query, cb) -> {
            final Predicate predicate = getHasTagsPredicate(targetRoot, cb, selectTargetWithNoTag, tagNames);
            query.distinct(true);
            return predicate;
        };
    }

    private static Predicate getHasTagsPredicate(final Root<JpaTarget> targetRoot, final CriteriaBuilder cb,
            final Boolean selectTargetWithNoTag, final String[] tagNames) {
        final SetJoin<JpaTarget, JpaTargetTag> tags = targetRoot.join(JpaTarget_.tags, JoinType.LEFT);
        final Path<String> exp = tags.get(JpaTargetTag_.name);

        final List<Predicate> hasTagsPredicates = new ArrayList<>();
        if (isNoTagActive(selectTargetWithNoTag)) {
            hasTagsPredicates.add(exp.isNull());
        }
        if (isAtLeastOneTagActive(tagNames)) {
            hasTagsPredicates.add(exp.in(tagNames));
        }

        return hasTagsPredicates.stream().reduce(cb::or)
                .orElseThrow(() -> new RuntimeException("Neither NO_TAG, nor TAG target tag filter was provided!"));
    }

    private static boolean isNoTagActive(final Boolean selectTargetWithNoTag) {
        return Boolean.TRUE.equals(selectTargetWithNoTag);
    }

    private static boolean isAtLeastOneTagActive(final String[] tagNames) {
        return tagNames != null && tagNames.length > 0;
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by assigned
     * distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set which must be assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasAssignedDistributionSet(final Long distributionSetId) {
        return (targetRoot, query, cb) -> cb.equal(
                targetRoot.<JpaDistributionSet> get(JpaTarget_.assignedDistributionSet).get(JpaDistributionSet_.id),
                distributionSetId);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that don't have the
     * given distribution set in their action history
     *
     * @param distributionSetId
     *            the ID of the distribution set which must not be assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasNotDistributionSetInActions(final Long distributionSetId) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, JpaAction> actionsJoin = targetRoot.join(JpaTarget_.actions, JoinType.LEFT);
            actionsJoin.on(cb.equal(actionsJoin.get(JpaAction_.distributionSet).get(JpaDistributionSet_.id),
                    distributionSetId));

            return cb.isNull(actionsJoin.get(JpaAction_.id));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are compatible
     * with given {@link DistributionSetType}. Compatibility is evaluated by
     * checking the {@link TargetType} of a target. Targets that don't have a
     * {@link TargetType} are compatible with all {@link DistributionSetType}
     *
     * @param distributionSetTypeId
     *            the ID of the distribution set type which must be compatible
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> isCompatibleWithDistributionSetType(final Long distributionSetTypeId) {
        return (targetRoot, query, cb) -> {
            // Since the targetRoot is changed by joining we need to get the
            // isNull predicate first
            final Predicate targetTypeIsNull = getTargetTypeIsNullPredicate(targetRoot);

            return cb.or(targetTypeIsNull, cb.equal(getDsTypeIdPath(targetRoot), distributionSetTypeId));
        };
    }

    private static Predicate getTargetTypeIsNullPredicate(final Root<JpaTarget> targetRoot) {
        return targetRoot.get(JpaTarget_.targetType).isNull();
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are NOT
     * compatible with given {@link DistributionSetType}. Compatibility is
     * evaluated by checking the {@link TargetType} of a target. Targets that
     * don't have a {@link TargetType} are compatible with all
     * {@link DistributionSetType}
     *
     * @param distributionSetTypeId
     *            the ID of the distribution set type which must be incompatible
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> notCompatibleWithDistributionSetType(final Long distributionSetTypeId) {
        return (targetRoot, query, cb) -> {
            // Since the targetRoot is changed by joining we need to get the
            // isNotNull predicate first
            final Predicate targetTypeNotNull = targetRoot.get(JpaTarget_.targetType).isNotNull();

            final Subquery<Long> compatibilitySubQuery = query.subquery(Long.class);
            final Root<JpaTarget> subQueryTargetRoot = compatibilitySubQuery.from(JpaTarget.class);

            compatibilitySubQuery.select(subQueryTargetRoot.get(JpaTarget_.id))
                    .where(cb.and(cb.equal(targetRoot.get(JpaTarget_.id), subQueryTargetRoot.get(JpaTarget_.id)),
                            cb.equal(getDsTypeIdPath(subQueryTargetRoot), distributionSetTypeId)));

            return cb.and(targetTypeNotNull, cb.not(cb.exists(compatibilitySubQuery)));
        };
    }

    private static Path<Long> getDsTypeIdPath(final Root<JpaTarget> root) {
        final Join<JpaTarget, JpaTargetType> targetTypeJoin = root.join(JpaTarget_.targetType, JoinType.LEFT);
        targetTypeJoin.fetch(JpaTargetType_.distributionSetTypes);
        final SetJoin<JpaTargetType, JpaDistributionSetType> dsTypeTargetTypeJoin = targetTypeJoin
                .join(JpaTargetType_.distributionSetTypes, JoinType.LEFT);

        return dsTypeTargetTypeJoin.get(JpaDistributionSetType_.id);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are in a given
     * {@link RolloutGroup}
     *
     * @param group
     *            the {@link RolloutGroup}
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> isInRolloutGroup(final Long group) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, RolloutTargetGroup> targetGroupJoin = targetRoot
                    .join(JpaTarget_.rolloutTargetGroup);
            return cb.equal(targetGroupJoin.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id), group);
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are in an
     * action for a given {@link RolloutGroup}
     *
     * @param group
     *            the {@link RolloutGroup}
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> isInActionRolloutGroup(final Long group) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, JpaAction> targetActionJoin = targetRoot.join(JpaTarget_.actions);
            return cb.equal(targetActionJoin.get(JpaAction_.rolloutGroup).get(JpaRolloutGroup_.id), group);
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are not in the
     * given {@link RolloutGroup}s
     *
     * @param groups
     *            the {@link RolloutGroup}s
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> isNotInRolloutGroups(final Collection<Long> groups) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, RolloutTargetGroup> rolloutTargetJoin = targetRoot
                    .join(JpaTarget_.rolloutTargetGroup, JoinType.LEFT);
            final Predicate inRolloutGroups = rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup)
                    .get(JpaRolloutGroup_.id).in(groups);
            rolloutTargetJoin.on(inRolloutGroups);
            return cb.isNull(rolloutTargetJoin.get(RolloutTargetGroup_.target));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that have no Action
     * of the {@link RolloutGroup}.
     *
     * @param group
     *            the {@link RolloutGroup}
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasNoActionInRolloutGroup(final Long group) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, RolloutTargetGroup> rolloutTargetJoin = targetRoot
                    .join(JpaTarget_.rolloutTargetGroup, JoinType.INNER);
            rolloutTargetJoin.on(
                    cb.equal(rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id), group));

            final ListJoin<JpaTarget, JpaAction> actionsJoin = targetRoot.join(JpaTarget_.actions, JoinType.LEFT);
            actionsJoin.on(cb.equal(actionsJoin.get(JpaAction_.rolloutGroup).get(JpaRolloutGroup_.id), group));

            return cb.isNull(actionsJoin.get(JpaAction_.id));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by assigned
     * distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set which must be assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasInstalledDistributionSet(final Long distributionSetId) {
        return (targetRoot, query, cb) -> cb.equal(
                targetRoot.get(JpaTarget_.installedDistributionSet).get(JpaDistributionSet_.id), distributionSetId);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by tag.
     *
     * @param tagId
     *            the ID of the tag that should be to be assigned to target
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTag(final Long tagId) {

        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTarget, JpaTargetTag> tags = targetRoot.join(JpaTarget_.tags, JoinType.LEFT);
            return cb.equal(tags.get(JpaTargetTag_.id), tagId);
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by target type id
     *
     * @param typeId
     *            the id of the target type
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTargetType(final long typeId) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTarget_.targetType).get(JpaTargetType_.id),
                typeId);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by target type id is
     * equal to null
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasNoTargetType() {
        return (targetRoot, query, cb) -> cb.isNull(targetRoot.get(JpaTarget_.targetType));
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that don't have
     * target type assigned
     *
     * @param typeId
     *            the id of the target type
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTargetTypeNot(final Long typeId) {
        return (targetRoot, query, cb) -> cb.or(getTargetTypeIsNullPredicate(targetRoot),
                cb.notEqual(targetRoot.get(JpaTarget_.targetType).get(JpaTargetType_.id), typeId));
    }

    /**
     * Can be added to specification chain to order result by provided
     * distribution set
     *
     * Order: 1. Targets with DS installed, 2. Targets with DS assigned, 3.
     * Based on requested sorting or id if <code>null</code>.
     *
     * NOTE: Other specs, pagables and sort objects may alter the queries
     * orderBy entry too, possibly invalidating the applied order, keep in mind
     * when using this
     *
     * @param distributionSetIdForOrder
     *            distribution set to consider
     * @param sort
     *            the sorting requested
     * @return specification that applies order by ds, may be overwritten
     */
    public static Specification<JpaTarget> orderedByLinkedDistributionSet(final long distributionSetIdForOrder,
            final Sort sort) {
        return (targetRoot, query, cb) -> {
            // Enhance query with custom select based sort
            final Expression<Object> selectCase = cb.selectCase()
                    .when(cb.equal(targetRoot.get(JpaTarget_.installedDistributionSet).get(JpaDistributionSet_.id),
                            distributionSetIdForOrder), 1)
                    .when(cb.equal(targetRoot.get(JpaTarget_.assignedDistributionSet).get(JpaDistributionSet_.id),
                            distributionSetIdForOrder), 2)
                    .otherwise(100);
            final List<Order> orders = new ArrayList<>();
            orders.add(cb.asc(selectCase));
            if (sort == null || sort.isEmpty()) {
                orders.add(cb.desc(targetRoot.get(JpaTarget_.id)));
            } else {
                orders.addAll(QueryUtils.toOrders(sort, targetRoot, cb));
            }
            query.orderBy(orders);

            // Spec only provides order, so no further filtering
            return query.getRestriction();
        };
    }

}

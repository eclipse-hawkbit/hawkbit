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
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.data.jpa.domain.Specification;

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
            targetRoot.fetch(JpaTarget_.assignedDistributionSet);
            return predicate;
        };
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
    public static Specification<JpaTarget> hasTargetUpdateStatus(final Collection<TargetUpdateStatus> updateStatus) {
        return (targetRoot, query, cb) -> targetRoot.get(JpaTarget_.updateStatus).in(updateStatus);
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
     * {@link Specification} for retrieving {@link Target}s by "like
     * controllerId or like name or like description".
     *
     * @param searchText
     *            to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> likeIdOrNameOrDescription(final String searchText) {
        return (targetRoot, query, cb) -> {
            final String searchTextToLower = searchText.toLowerCase();
            return cb.or(cb.like(cb.lower(targetRoot.get(JpaTarget_.controllerId)), searchTextToLower),
                    cb.like(cb.lower(targetRoot.get(JpaTarget_.name)), searchTextToLower),
                    cb.like(cb.lower(targetRoot.get(JpaTarget_.description)), searchTextToLower));
        };
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
     * controllerId or like name or like description or like attribute value".
     *
     * @param searchText
     *            to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> likeIdOrNameOrDescriptionOrAttributeValue(final String searchText) {
        return Specification.where(likeIdOrNameOrDescription(searchText)).or(likeAttributeValue(searchText));
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
        return (targetRoot, query, cb) -> cb.or(
                cb.equal(targetRoot.get(JpaTarget_.installedDistributionSet).get(JpaDistributionSet_.id),
                        distributionId),
                cb.equal(targetRoot.<JpaDistributionSet> get(JpaTarget_.assignedDistributionSet)
                        .get(JpaDistributionSet_.id), distributionId));
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
     *            the ID of the distribution set which must be assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTag(final Long tagId) {

        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTarget, JpaTargetTag> tags = targetRoot.join(JpaTarget_.tags, JoinType.LEFT);
            return cb.equal(tags.get(JpaTargetTag_.id), tagId);
        };
    }
}

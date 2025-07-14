/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaNamedEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup_;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link Target}s. The class provides Spring Data JPQL Specifications.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TargetSpecifications {

    /**
     * {@link Specification} for retrieving {@link Target}s including {@link TargetTag}s.
     *
     * @param controllerIDs to search for
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
     * @param controllerID to search for
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasControllerId(final String controllerID) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTarget_.controllerId), controllerID);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by controllerId
     *
     * @param controllerIDs to search for
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasControllerIdIn(final Collection<String> controllerIDs) {
        return (targetRoot, query, cb) -> targetRoot.get(JpaTarget_.controllerId).in(controllerIDs);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by controllerId
     *
     * @param id to search for
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasId(final Long id) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(AbstractJpaBaseEntity_.id), id);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by controllerId
     *
     * @param ids to search for
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasIdIn(final Collection<Long> ids) {
        return (targetRoot, query, cb) -> targetRoot.get(AbstractJpaBaseEntity_.id).in(ids);
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
     * {@link Specification} for retrieving {@link JpaTarget}s including {@link JpaTarget#getAssignedDistributionSet()}.
     *
     * @param controllerIDs to search for
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
     * {@link Specification} for retrieving {@link Target}s by "equal to any given {@link TargetUpdateStatus}".
     *
     * @param updateStatus to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTargetUpdateStatus(final Collection<TargetUpdateStatus> updateStatus) {
        return (targetRoot, query, cb) -> targetRoot.get(JpaTarget_.updateStatus).in(updateStatus);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "equal to given {@link TargetUpdateStatus}".
     *
     * @param updateStatus to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTargetUpdateStatus(final TargetUpdateStatus updateStatus) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTarget_.updateStatus), updateStatus);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "not equal to given {@link TargetUpdateStatus}".
     *
     * @param updateStatus to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> notEqualToTargetUpdateStatus(final TargetUpdateStatus updateStatus) {
        return (targetRoot, query, cb) -> cb.not(cb.equal(targetRoot.get(JpaTarget_.updateStatus), updateStatus));
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are overdue. A target is overdue if it did not respond during the configured
     * intervals:<br>
     * <em>poll_itvl + overdue_itvl</em>
     *
     * @param overdueTimestamp the calculated timestamp to compare with the last response of a target (lastTargetQuery).<br>
     *         The <code>overdueTimestamp</code> has to be calculated with the following expression:<br>
     *         <em>overdueTimestamp = nowTimestamp - poll_itvl - overdue_itvl</em>
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> isOverdue(final long overdueTimestamp) {
        return (targetRoot, query, cb) ->
                cb.lessThanOrEqualTo(targetRoot.get(JpaTarget_.lastTargetQuery), overdueTimestamp);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "like controllerId or like name".
     *
     * @param searchText to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> likeControllerIdOrName(final String searchText) {
        return (targetRoot, query, cb) -> {
            final String searchTextToLower = searchText.toLowerCase();
            return cb.or(
                    cb.like(cb.lower(targetRoot.get(JpaTarget_.controllerId)), searchTextToLower),
                    cb.like(cb.lower(targetRoot.get(AbstractJpaNamedEntity_.name)), searchTextToLower));
        };
    }

    public  static Specification<JpaTarget> eqTargetGroup(final String targetGroup) {
        return (targetRoot, query, criteriaBuilder) -> {
            final String groupTextToLower = targetGroup.toLowerCase();
            return criteriaBuilder.equal(criteriaBuilder.lower(targetRoot.get(JpaTarget_.group)), groupTextToLower);
        };
    }

    public static Specification<JpaTarget> likeTargetGroup(final String targetGroupSearch) {
        return (targetRoot, query, criteriaBuilder) ->  {
            final String searchTextToLower = targetGroupSearch.toLowerCase();
            return criteriaBuilder.or(
                    criteriaBuilder.equal(criteriaBuilder.lower(targetRoot.get(JpaTarget_.group)), searchTextToLower),
                    criteriaBuilder.like(criteriaBuilder.lower(targetRoot.get(JpaTarget_.group)), searchTextToLower.concat("%")));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "like controllerId".
     *
     * @param distributionId to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasInstalledOrAssignedDistributionSet(@NotNull final Long distributionId) {
        return hasInstalledDistributionSet(distributionId).or(hasAssignedDistributionSet(distributionId));
    }

    /**
     * Finds all targets by given {@link Target#getControllerId()}s and which are not yet assigned to given {@link DistributionSet}.
     *
     * @param tIDs to search for.
     * @param distributionId set that is not yet assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasControllerIdAndAssignedDistributionSetIdNot(final List<String> tIDs,
            @NotNull final Long distributionId) {
        return (targetRoot, query, cb) -> cb.and(targetRoot.get(JpaTarget_.controllerId).in(tIDs),
                cb.or(
                        cb.notEqual(targetRoot.get(JpaTarget_.assignedDistributionSet).get(AbstractJpaBaseEntity_.id), distributionId),
                        cb.isNull(targetRoot.get(JpaTarget_.assignedDistributionSet))));
    }

    /**
     * {@link Specification} for retrieving {@link Target}s based on a {@link TargetTag} name.
     *
     * @param tagName to search for
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTagName(final String tagName) {
        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTarget, JpaTargetTag> join = targetRoot.join(JpaTarget_.tags);
            return cb.equal(join.get(AbstractJpaNamedEntity_.name), tagName);
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "has no tag names"or "has at least on of the given tag names".
     *
     * @param tagNames to be filtered on
     * @param selectTargetWithNoTag flag to get targets with no tag assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTags(final String[] tagNames, final Boolean selectTargetWithNoTag) {
        return (targetRoot, query, cb) -> {
            final Predicate predicate = getHasTagsPredicate(targetRoot, cb, selectTargetWithNoTag, tagNames);
            query.distinct(true);
            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by assigned distribution set.
     *
     * @param distributionSetId the ID of the distribution set which must be assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasAssignedDistributionSet(final Long distributionSetId) {
        return (targetRoot, query, cb) ->
                cb.equal(targetRoot.get(JpaTarget_.assignedDistributionSet).get(AbstractJpaBaseEntity_.id), distributionSetId);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that don't have the given distribution set in their action history
     *
     * @param distributionSetId the ID of the distribution set which must not be assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasNotDistributionSetInActions(final Long distributionSetId) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, JpaAction> actionsJoin = targetRoot.join(JpaTarget_.actions, JoinType.LEFT);
            actionsJoin.on(cb.equal(actionsJoin.get(JpaAction_.distributionSet).get(AbstractJpaBaseEntity_.id), distributionSetId));
            return cb.isNull(actionsJoin.get(AbstractJpaBaseEntity_.id));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are compatible with given {@link DistributionSetType}. Compatibility is
     * evaluated by checking the {@link TargetType} of a target. Targets that don't have a {@link TargetType} are compatible with all
     * {@link DistributionSetType}
     *
     * @param distributionSetTypeId the ID of the distribution set type which must be compatible
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> isCompatibleWithDistributionSetType(final Long distributionSetTypeId) {
        return (targetRoot, query, cb) -> {
            // Since the targetRoot is changed by joining we need to get the isNull predicate first
            final Predicate targetTypeIsNull = getTargetTypeIsNullPredicate(targetRoot);
            return cb.or(targetTypeIsNull, cb.equal(getDsTypeIdPath(targetRoot), distributionSetTypeId));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are NOT compatible with given {@link DistributionSetType}. Compatibility is
     * evaluated by checking the {@link TargetType} of a target. Targets that don't have a {@link TargetType} are compatible with all
     * {@link DistributionSetType}
     *
     * @param distributionSetTypeId the ID of the distribution set type which must be incompatible
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> notCompatibleWithDistributionSetType(final Long distributionSetTypeId) {
        return (targetRoot, query, cb) -> {
            // Since the targetRoot is changed by joining we need to get the isNotNull predicate first
            final Predicate targetTypeNotNull = targetRoot.get(JpaTarget_.targetType).isNotNull();

            final Subquery<Long> compatibilitySubQuery = query.subquery(Long.class);
            final Root<JpaTarget> subQueryTargetRoot = compatibilitySubQuery.from(JpaTarget.class);

            compatibilitySubQuery.select(subQueryTargetRoot.get(AbstractJpaBaseEntity_.id))
                    .where(cb.and(
                            cb.equal(targetRoot.get(AbstractJpaBaseEntity_.id), subQueryTargetRoot.get(AbstractJpaBaseEntity_.id)),
                            cb.equal(getDsTypeIdPath(subQueryTargetRoot), distributionSetTypeId)));

            return cb.and(targetTypeNotNull, cb.not(cb.exists(compatibilitySubQuery)));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are in a given {@link RolloutGroup}
     *
     * @param group the {@link RolloutGroup}
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> isInRolloutGroup(final Long group) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, RolloutTargetGroup> targetGroupJoin = targetRoot.join(JpaTarget_.rolloutTargetGroup);
            return cb.equal(targetGroupJoin.get(RolloutTargetGroup_.rolloutGroup).get(AbstractJpaBaseEntity_.id), group);
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are in an action for a given {@link RolloutGroup}
     *
     * @param group the {@link RolloutGroup}
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> isInActionRolloutGroup(final Long group) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, JpaAction> targetActionJoin = targetRoot.join(JpaTarget_.actions);
            return cb.equal(targetActionJoin.get(JpaAction_.rolloutGroup).get(AbstractJpaBaseEntity_.id), group);
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that are not in the given {@link RolloutGroup}s
     *
     * @param groups the {@link RolloutGroup}s
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> isNotInRolloutGroups(final Collection<Long> groups) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, RolloutTargetGroup> rolloutTargetJoin = targetRoot.join(JpaTarget_.rolloutTargetGroup, JoinType.LEFT);
            rolloutTargetJoin.on(rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup).get(AbstractJpaBaseEntity_.id).in(groups));
            return cb.isNull(rolloutTargetJoin.get(RolloutTargetGroup_.target));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that have no Action of the {@link RolloutGroup}.
     *
     * @param group the {@link RolloutGroup}
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasNoActionInRolloutGroup(final Long group) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, RolloutTargetGroup> rolloutTargetJoin = targetRoot.join(JpaTarget_.rolloutTargetGroup, JoinType.INNER);
            rolloutTargetJoin.on(cb.equal(rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup).get(AbstractJpaBaseEntity_.id), group));

            final ListJoin<JpaTarget, JpaAction> actionsJoin = targetRoot.join(JpaTarget_.actions, JoinType.LEFT);
            actionsJoin.on(cb.equal(actionsJoin.get(JpaAction_.rolloutGroup).get(AbstractJpaBaseEntity_.id), group));

            return cb.isNull(actionsJoin.get(AbstractJpaBaseEntity_.id));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by assigned distribution set.
     *
     * @param distributionSetId the ID of the distribution set which must be assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasInstalledDistributionSet(final Long distributionSetId) {
        return (targetRoot, query, cb) -> cb.equal(
                targetRoot.get(JpaTarget_.installedDistributionSet).get(AbstractJpaBaseEntity_.id), distributionSetId);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by tag.
     *
     * @param tagId the ID of the tag that should be to be assigned to target
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTag(final Long tagId) {
        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTarget, JpaTargetTag> tags = targetRoot.join(JpaTarget_.tags, JoinType.LEFT);
            return cb.equal(tags.get(AbstractJpaBaseEntity_.id), tagId);
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by target type id
     *
     * @param typeId the id of the target type
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTargetType(final long typeId) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTarget_.targetType).get(AbstractJpaBaseEntity_.id), typeId);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by target type id is equal to null
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasNoTargetType() {
        return (targetRoot, query, cb) -> cb.isNull(targetRoot.get(JpaTarget_.targetType));
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that don't have target type assigned
     *
     * @param typeId the id of the target type
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasTargetTypeNot(final Long typeId) {
        return (targetRoot, query, cb) -> cb.or(getTargetTypeIsNullPredicate(targetRoot),
                cb.notEqual(targetRoot.get(JpaTarget_.targetType).get(AbstractJpaBaseEntity_.id), typeId));
    }

    public static Specification<JpaTarget> failedActionsForRollout(final String rolloutId) {
        return (targetRoot, query, cb) -> {
            final Join<JpaTarget, Action> targetActions = targetRoot.join("actions");
            return cb.and(
                    cb.equal(targetActions.get("rollout").get("id"), rolloutId),
                    cb.equal(targetActions.get("status"), Action.Status.ERROR));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s that have no overriding actions - i.e. no actions from newer rollouts
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<JpaTarget> hasNoOverridingActionsAndNotInRollout(final long rolloutId) {
        return (targetRoot, query, cb) -> {
            final ListJoin<JpaTarget, JpaAction> actionsJoin = targetRoot.join(JpaTarget_.actions, JoinType.LEFT);
            actionsJoin.on(cb.ge(actionsJoin.get(JpaAction_.rollout).get(AbstractJpaBaseEntity_.id), rolloutId));
            return cb.isNull(actionsJoin.get(AbstractJpaBaseEntity_.id));
        };
    }

    private static Predicate getHasTagsPredicate(final Root<JpaTarget> targetRoot, final CriteriaBuilder cb,
            final Boolean selectTargetWithNoTag, final String[] tagNames) {
        final SetJoin<JpaTarget, JpaTargetTag> tags = targetRoot.join(JpaTarget_.tags, JoinType.LEFT);
        final Path<String> exp = tags.get(AbstractJpaNamedEntity_.name);

        final List<Predicate> hasTagsPredicates = new ArrayList<>();
        if (isNoTagActive(selectTargetWithNoTag)) {
            hasTagsPredicates.add(exp.isNull());
        }
        if (isAtLeastOneTagActive(tagNames)) {
            hasTagsPredicates.add(exp.in((Object[]) tagNames));
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

    private static Predicate getTargetTypeIsNullPredicate(final Root<JpaTarget> targetRoot) {
        return targetRoot.get(JpaTarget_.targetType).isNull();
    }

    private static Path<Long> getDsTypeIdPath(final Root<JpaTarget> root) {
        final Join<JpaTarget, JpaTargetType> targetTypeJoin = root.join(JpaTarget_.targetType, JoinType.LEFT);
        return targetTypeJoin.join(JpaTargetType_.distributionSetTypes, JoinType.LEFT).get(AbstractJpaBaseEntity_.id);
    }
}
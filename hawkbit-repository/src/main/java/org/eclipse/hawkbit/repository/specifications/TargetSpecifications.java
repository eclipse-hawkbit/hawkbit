/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.specifications;

import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSet_;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetInfo_;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTag_;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Target_;
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
     * {@link TargetTag}s and {@link TargetStatus}.
     *
     * @param controllerIDs
     *            to search for
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<Target> byControllerIdWithStatusAndTagsInJoin(final Collection<String> controllerIDs) {
        return (targetRoot, query, cb) -> {
            final Predicate predicate = targetRoot.get(Target_.controllerId).in(controllerIDs);
            targetRoot.fetch(Target_.tags, JoinType.LEFT);
            query.distinct(true);
            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s including {
     * {@link TargetStatus}.
     *
     * @param controllerIDs
     *            to search for
     *
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<Target> byControllerIdWithStatusAndAssignedInJoin(
            final Collection<String> controllerIDs) {
        return (targetRoot, query, cb) -> {

            final Predicate predicate = targetRoot.get(Target_.controllerId).in(controllerIDs);
            targetRoot.fetch(Target_.assignedDistributionSet);
            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by "equal to given
     * {@link TargetUpdateStatus}".
     * 
     * @param updateStatus
     *            to be filtered on
     * @param fetch
     *            {@code true} to fetch the {@link TargetInfo} otherwise only
     *            join it.
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<Target> hasTargetUpdateStatus(final Collection<TargetUpdateStatus> updateStatus,
            final boolean fetch) {
        return (targetRoot, query, cb) -> {
            if (!query.getResultType().isAssignableFrom(Long.class)) {
                if (fetch) {
                    targetRoot.fetch(Target_.targetInfo);
                } else {
                    targetRoot.join(Target_.targetInfo);
                }
                return targetRoot.get(Target_.targetInfo).get(TargetInfo_.updateStatus).in(updateStatus);
            }
            final Join<Target, TargetInfo> targetInfoJoin = targetRoot.join(Target_.targetInfo);
            return targetInfoJoin.get(TargetInfo_.updateStatus).in(updateStatus);
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by
     * "like controllerId or like description or like ip address".
     * 
     * @param searchText
     *            to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<Target> likeNameOrDescriptionOrIp(final String searchText) {
        return (targetRoot, query, cb) -> {
            final String searchTextToLower = searchText.toLowerCase();
            return cb.or(cb.like(cb.lower(targetRoot.get(Target_.name)), searchTextToLower),
                    cb.like(cb.lower(targetRoot.get(Target_.description)), searchTextToLower));
        };
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by
     * "like controllerId".
     * 
     * @param distributionId
     *            to be filtered on
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<Target> hasInstalledOrAssignedDistributionSet(@NotNull final Long distributionId) {
        return (targetRoot, query, cb) -> {
            final Join<Target, TargetInfo> targetInfoJoin = targetRoot.join(Target_.targetInfo);
            return cb.or(
                    cb.equal(targetInfoJoin.get(TargetInfo_.installedDistributionSet).get(DistributionSet_.id),
                            distributionId),
                    cb.equal(targetRoot.<DistributionSet> get(Target_.assignedDistributionSet).get(DistributionSet_.id),
                            distributionId));
        };
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
    public static Specification<Target> hasControllerIdAndAssignedDistributionSetIdNot(final List<String> tIDs,
            @NotNull final Long distributionId) {
        return (targetRoot, query, cb) -> cb
                .and(targetRoot.get(Target_.controllerId).in(tIDs),
                        cb.or(cb.notEqual(targetRoot.<DistributionSet> get(Target_.assignedDistributionSet)
                                .get(DistributionSet_.id), distributionId),
                                cb.isNull(targetRoot.<DistributionSet> get(Target_.assignedDistributionSet))));
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by
     * "has no tag names"or "has at least on of the given tag names".
     * 
     * @param tagNames
     *            to be filtered on
     * @param selectTargetWithNoTag
     *            flag to get targets with no tag assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<Target> hasTags(final String[] tagNames, final Boolean selectTargetWithNoTag) {
        return (targetRoot, query, cb) -> {
            final Predicate predicate = getPredicate(targetRoot, cb, selectTargetWithNoTag, tagNames);
            query.distinct(true);
            return predicate;
        };
    }

    private static Predicate getPredicate(final Root<Target> targetRoot, final CriteriaBuilder cb,
            final Boolean selectTargetWithNoTag, final String[] tagNames) {
        final SetJoin<Target, TargetTag> tags = targetRoot.join(Target_.tags, JoinType.LEFT);
        final Path<String> exp = tags.get(TargetTag_.name);
        if (selectTargetWithNoTag) {
            if (tagNames != null) {
                return cb.or(exp.isNull(), exp.in(tagNames));
            } else {
                return exp.isNull();
            }
        } else {
            return exp.in(tagNames);
        }
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by assigned
     * distribution set.
     * 
     * @param distributionSetId
     *            the ID of the distribution set which must be assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<Target> hasAssignedDistributionSet(final Long distributionSetId) {
        return (targetRoot, query, cb) -> cb.equal(
                targetRoot.<DistributionSet> get(Target_.assignedDistributionSet).get(DistributionSet_.id),
                distributionSetId);
    }

    /**
     * {@link Specification} for retrieving {@link Target}s by assigned
     * distribution set.
     * 
     * @param distributionSetId
     *            the ID of the distribution set which must be assigned
     * @return the {@link Target} {@link Specification}
     */
    public static Specification<Target> hasInstalledDistributionSet(final Long distributionSetId) {
        return (targetRoot, query, cb) -> {
            final Join<Target, TargetInfo> targetInfoJoin = targetRoot.join(Target_.targetInfo);
            return cb.equal(targetInfoJoin.get(TargetInfo_.installedDistributionSet).get(DistributionSet_.id),
                    distributionSetId);
        };
    }
}

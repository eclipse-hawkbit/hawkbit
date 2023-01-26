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
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.util.CollectionUtils;

/**
 * Specifications class for {@link DistributionSet}s. The class provides Spring
 * Data JPQL Specifications.
 *
 */
public final class DistributionSetSpecification {
    private DistributionSetSpecification() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by its
     * DELETED attribute.
     *
     * @param isDeleted
     *            TRUE/FALSE are compared to the attribute DELETED. If NULL the
     *            attribute is ignored
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> isDeleted(final Boolean isDeleted) {
        return (dsRoot, query, cb) -> cb.equal(dsRoot.<Boolean> get(JpaDistributionSet_.deleted), isDeleted);

    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by its
     * COMPLETED attribute.
     *
     * @param isCompleted
     *            TRUE/FALSE are compared to the attribute COMPLETED. If NULL
     *            the attribute is ignored
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> isCompleted(final Boolean isCompleted) {
        return (dsRoot, query, cb) -> cb.equal(dsRoot.<Boolean> get(JpaDistributionSet_.complete), isCompleted);

    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by its
     * VALID attribute.
     *
     * @param isValid
     *            TRUE/FALSE are compared to the attribute VALID. If NULL the
     *            attribute is ignored
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> isValid(final Boolean isValid) {
        return (dsRoot, query, cb) -> cb.equal(dsRoot.<Boolean> get(JpaDistributionSet_.valid), isValid);

    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} with given
     * {@link DistributionSet#getId()}.
     *
     * @param distid
     *            to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> byId(final Long distid) {
        return (dsRoot, query, cb) -> {
            final Predicate predicate = cb.equal(dsRoot.<Long> get(JpaDistributionSet_.id), distid);
            dsRoot.fetch(JpaDistributionSet_.modules, JoinType.LEFT);
            dsRoot.fetch(JpaDistributionSet_.type, JoinType.LEFT);
            query.distinct(true);

            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} with given
     * {@link DistributionSet#getId()}s.
     *
     * @param distids
     *            to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> byIds(final Collection<Long> distids) {
        return (dsRoot, query, cb) -> {
            final Predicate predicate = dsRoot.<Long> get(JpaDistributionSet_.id).in(distids);
            dsRoot.fetch(JpaDistributionSet_.modules, JoinType.LEFT);
            dsRoot.fetch(JpaDistributionSet_.tags, JoinType.LEFT);
            dsRoot.fetch(JpaDistributionSet_.type, JoinType.LEFT);
            query.distinct(true);
            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by "like
     * name and like version".
     *
     * @param name
     *            to be filtered on
     * @param version
     *            to be filtered on
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> likeNameAndVersion(final String name, final String version) {
        return (dsRoot, query, cb) -> cb.and(
                cb.like(cb.lower(dsRoot.<String> get(JpaDistributionSet_.name)), name.toLowerCase()),
                cb.like(cb.lower(dsRoot.<String> get(JpaDistributionSet_.version)), version.toLowerCase()));
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by "has at
     * least one of the given tag names".
     *
     * @param tagNames
     *            to be filtered on
     * @param selectDSWithNoTag
     *            flag to select distribution sets with no tag
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> hasTags(final Collection<String> tagNames,
            final Boolean selectDSWithNoTag) {
        return (dsRoot, query, cb) -> {
            final Predicate predicate = getHasTagsPredicate(dsRoot, cb, selectDSWithNoTag, tagNames);
            query.distinct(true);
            return predicate;
        };
    }

    private static Predicate getHasTagsPredicate(final Root<JpaDistributionSet> dsRoot, final CriteriaBuilder cb,
            final Boolean selectDSWithNoTag, final Collection<String> tagNames) {
        final SetJoin<JpaDistributionSet, JpaDistributionSetTag> tags = dsRoot.join(JpaDistributionSet_.tags,
                JoinType.LEFT);
        final Path<String> exp = tags.get(JpaDistributionSetTag_.name);

        final List<Predicate> hasTagsPredicates = new ArrayList<>();
        if (isNoTagActive(selectDSWithNoTag)) {
            hasTagsPredicates.add(exp.isNull());
        }
        if (isAtLeastOneTagActive(tagNames)) {
            hasTagsPredicates.add(exp.in(tagNames));
        }

        return hasTagsPredicates.stream().reduce(cb::or).orElseThrow(
                () -> new RuntimeException("Neither NO_TAG, nor TAG distribution set tag filter was provided!"));
    }

    private static boolean isNoTagActive(final Boolean selectDSWithNoTag) {
        return Boolean.TRUE.equals(selectDSWithNoTag);
    }

    private static boolean isAtLeastOneTagActive(final Collection<String> tagNames) {
        return !CollectionUtils.isEmpty(tagNames);
    }

    /**
     * returns query criteria {@link Specification} comparing case insensitive
     * "NAME == AND VERSION ==".
     *
     * @param name
     *            to be filtered on
     * @param version
     *            to be filtered on
     * @return the {@link Specification}
     */
    public static Specification<JpaDistributionSet> equalsNameAndVersionIgnoreCase(final String name,
            final String version) {
        return (dsRoot, query, cb) -> cb.and(
                cb.equal(cb.lower(dsRoot.<String> get(JpaDistributionSet_.name)), name.toLowerCase()),
                cb.equal(cb.lower(dsRoot.<String> get(JpaDistributionSet_.version)), version.toLowerCase()));

    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} with given
     * {@link DistributionSet#getType()}.
     *
     * @param typeId
     *            id of distribution set type to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> byType(final Long typeId) {
        return (dsRoot, query, cb) -> cb.equal(dsRoot.get(JpaDistributionSet_.type).get(JpaDistributionSetType_.id),
                typeId);

    }

    /**
     * @param installedTargetId
     *            the targetID which is installed to a distribution set to
     *            search for.
     * @return the specification to search for a distribution set which is
     *         installed to the given targetId
     */
    public static Specification<JpaDistributionSet> installedTarget(final String installedTargetId) {
        return (dsRoot, query, cb) -> {
            final ListJoin<JpaDistributionSet, JpaTarget> installedTargetJoin = dsRoot
                    .join(JpaDistributionSet_.installedAtTargets, JoinType.INNER);
            return cb.equal(installedTargetJoin.get(JpaTarget_.controllerId), installedTargetId);
        };
    }

    /**
     * @param assignedTargetId
     *            the targetID which is assigned to a distribution set to search
     *            for.
     * @return the specification to search for a distribution set which is
     *         assigned to the given targetId
     */
    public static Specification<JpaDistributionSet> assignedTarget(final String assignedTargetId) {
        return (dsRoot, query, cb) -> {
            final ListJoin<JpaDistributionSet, JpaTarget> assignedTargetJoin = dsRoot
                    .join(JpaDistributionSet_.assignedToTargets, JoinType.INNER);
            return cb.equal(assignedTargetJoin.get(JpaTarget_.controllerId), assignedTargetId);
        };
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by tag.
     *
     * @param tagId
     *            the ID of the distribution set which must be assigned
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> hasTag(final Long tagId) {

        return (dsRoot, query, cb) -> {
            final SetJoin<JpaDistributionSet, JpaDistributionSetTag> tags = dsRoot.join(JpaDistributionSet_.tags,
                    JoinType.LEFT);
            return cb.equal(tags.get(JpaDistributionSetTag_.id), tagId);
        };
    }

    /**
     * Can be added to specification chain to order result by provided target
     *
     * Order: 1. Distribution set installed on target, 2. Distribution set(s)
     * assigned to target, 3. Based on requested sorting or id if
     * <code>null</code>.
     *
     * NOTE: Other specs, pagables and sort objects may alter the queries
     * orderBy entry too, possibly invalidating the applied order, keep in mind
     * when using this
     *
     * @param linkedControllerId
     *            controller id to get installed/assigned DS for
     * @param sort
     * @return specification that applies order by target, may be overwritten
     */
    public static Specification<JpaDistributionSet> orderedByLinkedTarget(final String linkedControllerId,
            final Sort sort) {
        return (dsRoot, query, cb) -> {
            final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);

            final Expression<Object> assignedInstalledCase = cb.selectCase()
                    .when(cb.equal(targetRoot.get(JpaTarget_.installedDistributionSet), dsRoot), 1)
                    .when(cb.equal(targetRoot.get(JpaTarget_.assignedDistributionSet), dsRoot), 2).otherwise(3);

            final List<Order> orders = new ArrayList<>();
            orders.add(cb.asc(assignedInstalledCase));
            if (sort == null || sort.isEmpty()) {
                orders.add(cb.asc(dsRoot.get(JpaDistributionSet_.id)));
            } else {
                orders.addAll(QueryUtils.toOrders(sort, dsRoot, cb));
            }
            query.orderBy(orders);

            return cb.equal(targetRoot.get(JpaTarget_.controllerId), linkedControllerId);
        };
    }

}

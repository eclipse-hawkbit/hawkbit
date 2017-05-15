/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import java.util.Collection;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.SetJoin;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.springframework.data.jpa.domain.Specification;
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
        return (targetRoot, query, cb) -> cb.equal(targetRoot.<Boolean> get(JpaDistributionSet_.deleted), isDeleted);

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
        return (targetRoot, query, cb) -> cb.equal(targetRoot.<Boolean> get(JpaDistributionSet_.complete), isCompleted);

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
        return (targetRoot, query, cb) -> {
            final Predicate predicate = cb.equal(targetRoot.<Long> get(JpaDistributionSet_.id), distid);
            targetRoot.fetch(JpaDistributionSet_.modules, JoinType.LEFT);
            targetRoot.fetch(JpaDistributionSet_.type, JoinType.LEFT);
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
        return (targetRoot, query, cb) -> {
            final Predicate predicate = targetRoot.<Long> get(JpaDistributionSet_.id).in(distids);
            targetRoot.fetch(JpaDistributionSet_.modules, JoinType.LEFT);
            targetRoot.fetch(JpaDistributionSet_.tags, JoinType.LEFT);
            targetRoot.fetch(JpaDistributionSet_.type, JoinType.LEFT);
            query.distinct(true);
            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by "like
     * name or like description or like version".
     * 
     * @param subString
     *            to be filtered on
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> likeNameOrDescriptionOrVersion(final String subString) {
        return (targetRoot, query, cb) -> cb.or(
                cb.like(cb.lower(targetRoot.<String> get(JpaDistributionSet_.name)), subString.toLowerCase()),
                cb.like(cb.lower(targetRoot.<String> get(JpaDistributionSet_.version)), subString.toLowerCase()),
                cb.like(cb.lower(targetRoot.<String> get(JpaDistributionSet_.description)), subString.toLowerCase()));
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
        return (targetRoot, query, cb) -> {
            final SetJoin<JpaDistributionSet, JpaDistributionSetTag> tags = targetRoot.join(JpaDistributionSet_.tags,
                    JoinType.LEFT);
            final Predicate predicate = getPredicate(tags, tagNames, selectDSWithNoTag, cb);
            query.distinct(true);
            return predicate;
        };
    }

    private static Predicate getPredicate(final SetJoin<JpaDistributionSet, JpaDistributionSetTag> tags,
            final Collection<String> tagNames, final Boolean selectDSWithNoTag, final CriteriaBuilder cb) {
        tags.get(JpaDistributionSetTag_.name);
        final Path<String> exp = tags.get(JpaDistributionSetTag_.name);
        if (selectDSWithNoTag != null && selectDSWithNoTag) {
            if (!CollectionUtils.isEmpty(tagNames)) {
                return cb.or(exp.isNull(), exp.in(tagNames));
            } else {
                return exp.isNull();
            }
        } else {
            return exp.in(tagNames);
        }
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
        return (targetRoot, query, cb) -> cb.and(
                cb.equal(cb.lower(targetRoot.<String> get(JpaDistributionSet_.name)), name.toLowerCase()),
                cb.equal(cb.lower(targetRoot.<String> get(JpaDistributionSet_.version)), version.toLowerCase()));

    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} with given
     * {@link DistributionSet#getType()}.
     *
     * @param type
     *            to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> byType(final DistributionSetType type) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.<JpaDistributionSetType> get(JpaDistributionSet_.type),
                type);

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

        return (targetRoot, query, cb) -> {
            final SetJoin<JpaDistributionSet, JpaDistributionSetTag> tags = targetRoot.join(JpaDistributionSet_.tags,
                    JoinType.LEFT);
            return cb.equal(tags.get(JpaDistributionSetTag_.id), tagId);
        };
    }

}

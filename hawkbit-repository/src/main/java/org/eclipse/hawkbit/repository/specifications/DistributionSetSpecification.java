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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTag_;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSet_;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetInfo_;
import org.eclipse.hawkbit.repository.model.Target_;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link DistributionSet}s. The class provides Spring
 * Data JPQL Specifications
 *
 *
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
    public static Specification<DistributionSet> isDeleted(final Boolean isDeleted) {
        final Specification<DistributionSet> spec = new Specification<DistributionSet>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSet> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                final Predicate predicate = cb.equal(targetRoot.<Boolean> get(DistributionSet_.deleted), isDeleted);
                return predicate;
            }
        };

        return spec;
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
    public static Specification<DistributionSet> isCompleted(final Boolean isCompleted) {
        final Specification<DistributionSet> spec = new Specification<DistributionSet>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSet> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                final Predicate predicate = cb.equal(targetRoot.<Boolean> get(DistributionSet_.complete), isCompleted);
                return predicate;
            }
        };

        return spec;
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} with given
     * {@link DistributionSet#getId()}.
     *
     * @param distid
     *            to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<DistributionSet> byId(final Long distid) {
        final Specification<DistributionSet> spec = new Specification<DistributionSet>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSet> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {

                final Predicate predicate = cb.equal(targetRoot.<Long> get(DistributionSet_.id), distid);
                targetRoot.fetch(DistributionSet_.modules, JoinType.LEFT);
                targetRoot.fetch(DistributionSet_.tags, JoinType.LEFT);
                targetRoot.fetch(DistributionSet_.type, JoinType.LEFT);
                query.distinct(true);

                return predicate;
            }
        };
        return spec;
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} with given
     * {@link DistributionSet#getId()}s.
     *
     * @param distids
     *            to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<DistributionSet> byIds(final Collection<Long> distids) {
        final Specification<DistributionSet> spec = new Specification<DistributionSet>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSet> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {

                final Predicate predicate = targetRoot.<Long> get(DistributionSet_.id).in(distids);
                targetRoot.fetch(DistributionSet_.modules, JoinType.LEFT);
                targetRoot.fetch(DistributionSet_.tags, JoinType.LEFT);
                targetRoot.fetch(DistributionSet_.type, JoinType.LEFT);
                return predicate;
            }
        };
        return spec;
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by
     * "like name or like description or like version".
     * 
     * @param subString
     *            to be filtered on
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<DistributionSet> likeNameOrDescriptionOrVersion(final String subString) {
        final Specification<DistributionSet> spec = new Specification<DistributionSet>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSet> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                final Predicate predicate = cb.or(
                        cb.like(cb.lower(targetRoot.<String> get(DistributionSet_.name)), subString.toLowerCase()),
                        cb.like(cb.lower(targetRoot.<String> get(DistributionSet_.version)), subString.toLowerCase()),
                        cb.like(cb.lower(targetRoot.<String> get(DistributionSet_.description)),
                                subString.toLowerCase()));
                return predicate;
            }
        };

        return spec;
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by
     * "has at least one of the given tag names".
     * 
     * @param tagNames
     *            to be filtered on
     * @param selectDSWithNoTag
     *            flag to select distribution sets with no tag
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<DistributionSet> hasTags(final Collection<String> tagNames,
            final Boolean selectDSWithNoTag) {
        final Specification<DistributionSet> spec = new Specification<DistributionSet>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSet> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                final SetJoin<DistributionSet, DistributionSetTag> tags = targetRoot.join(DistributionSet_.tags,
                        JoinType.LEFT);
                final Predicate predicate = getPredicate(tags, tagNames, selectDSWithNoTag, cb);
                query.distinct(true);
                return predicate;
            }
        };

        return spec;
    }

    private static Predicate getPredicate(final SetJoin<DistributionSet, DistributionSetTag> tags,
            final Collection<String> tagNames, final Boolean selectDSWithNoTag, final CriteriaBuilder cb) {
        tags.get(DistributionSetTag_.name);
        final Path<String> exp = tags.get(DistributionSetTag_.name);
        if (selectDSWithNoTag != null && selectDSWithNoTag) {
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
     * returns query criteria {@link Specification} comparing case insensitive
     * "NAME == AND VERSION ==".
     * 
     * @param name
     *            to be filtered on
     * @param version
     *            to be filtered on
     * @return the {@link Specification}
     */
    public static Specification<DistributionSet> equalsNameAndVersionIgnoreCase(final String name, final String version) {
        final Specification<DistributionSet> spec = new Specification<DistributionSet>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSet> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                final Predicate predicate = cb.and(
                        // formatter:off
                        cb.equal(cb.lower(targetRoot.<String> get(DistributionSet_.name)), name.toLowerCase()),
                        cb.equal(cb.lower(targetRoot.<String> get(DistributionSet_.version)), version.toLowerCase()));
                // formatter:on
                return predicate;
            }
        };

        return spec;
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} with given
     * {@link DistributionSet#getType()}.
     *
     * @param type
     *            to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<DistributionSet> byType(final DistributionSetType type) {
        final Specification<DistributionSet> spec = new Specification<DistributionSet>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSet> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                final Predicate predicate = cb.equal(targetRoot.<DistributionSetType> get(DistributionSet_.type), type);
                return predicate;
            }
        };

        return spec;
    }

    /**
     * @param installedTargetId
     *            the targetID which is installed to a distribution set to
     *            search for.
     * @return the specification to search for a distribution set which is
     *         installed to the given targetId
     */
    public static Specification<DistributionSet> installedTarget(final String installedTargetId) {
        final Specification<DistributionSet> spec = new Specification<DistributionSet>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSet> dsRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                final ListJoin<DistributionSet, TargetInfo> installedTargetJoin = dsRoot.join(
                        DistributionSet_.installedAtTargets, JoinType.INNER);
                final Join<TargetInfo, Target> targetJoin = installedTargetJoin.join(TargetInfo_.target);
                return cb.equal(targetJoin.get(Target_.controllerId), installedTargetId);
            }
        };

        return spec;
    }

    /**
     * @param assignedTargetId
     *            the targetID which is assigned to a distribution set to search
     *            for.
     * @return the specification to search for a distribution set which is
     *         assigned to the given targetId
     */
    public static Specification<DistributionSet> assignedTarget(final String assignedTargetId) {
        final Specification<DistributionSet> spec = new Specification<DistributionSet>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSet> dsRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                final ListJoin<DistributionSet, Target> assignedTargetJoin = dsRoot.join(
                        DistributionSet_.assignedToTargets, JoinType.INNER);
                return cb.equal(assignedTargetJoin.get(Target_.controllerId), assignedTargetId);
            }
        };
        return spec;
    }

}

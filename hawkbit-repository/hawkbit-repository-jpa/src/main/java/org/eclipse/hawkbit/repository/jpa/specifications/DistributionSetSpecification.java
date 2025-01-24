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
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaNamedEntity_;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaNamedVersionedEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

/**
 * Specifications class for {@link DistributionSet}s. The class provides Spring Data JPQL Specifications.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistributionSetSpecification {

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s with DELETED attribute <code>false</code> - i.e. is not deleted.
     *
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> isNotDeleted() {
        return isDeleted(false);
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by its DELETED attribute.
     *
     * @param isDeleted TRUE/FALSE are compared to the attribute DELETED. If NULL the attribute is ignored
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> isDeleted(final Boolean isDeleted) {
        return (dsRoot, query, cb) -> cb.equal(dsRoot.get(JpaDistributionSet_.deleted), isDeleted);
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by its COMPLETED attribute.
     *
     * @param isCompleted TRUE/FALSE are compared to the attribute COMPLETED. If NULL the attribute is ignored
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> isCompleted(final Boolean isCompleted) {
        return (dsRoot, query, cb) -> cb.equal(dsRoot.get(JpaDistributionSet_.complete), isCompleted);
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by its VALID attribute.
     *
     * @param isValid TRUE/FALSE are compared to the attribute VALID. If NULL the attribute is ignored
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> isValid(final Boolean isValid) {
        return (dsRoot, query, cb) -> cb.equal(dsRoot.get(JpaDistributionSet_.valid), isValid);
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} with given {@link DistributionSet#getId()}.
     *
     * @param distid to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> byIdFetch(final Long distid) {
        return (dsRoot, query, cb) -> {
            final Predicate predicate = cb.equal(dsRoot.get(AbstractJpaBaseEntity_.id), distid);
            dsRoot.fetch(JpaDistributionSet_.modules, JoinType.LEFT);
            dsRoot.fetch(JpaDistributionSet_.type, JoinType.LEFT);
            query.distinct(true);

            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} with given {@link DistributionSet#getId()}s.
     *
     * @param distids to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> byIdsFetch(final Collection<Long> distids) {
        return (dsRoot, query, cb) -> {
            final Predicate predicate = dsRoot.get(AbstractJpaBaseEntity_.id).in(distids);
            dsRoot.fetch(JpaDistributionSet_.modules, JoinType.LEFT);
            dsRoot.fetch(JpaDistributionSet_.tags, JoinType.LEFT);
            dsRoot.fetch(JpaDistributionSet_.type, JoinType.LEFT);
            query.distinct(true);
            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by "like name and like version".
     *
     * @param name to be filtered on
     * @param version to be filtered on
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> likeNameAndVersion(final String name, final String version) {
        return (dsRoot, query, cb) -> cb.and(
                cb.like(cb.lower(dsRoot.get(AbstractJpaNamedEntity_.name)), name.toLowerCase()),
                cb.like(cb.lower(dsRoot.get(AbstractJpaNamedVersionedEntity_.version)), version.toLowerCase()));
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by "has at least one of the given tag names".
     *
     * @param tagNames to be filtered on
     * @param selectDSWithNoTag flag to select distribution sets with no tag
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> hasTags(final Collection<String> tagNames, final Boolean selectDSWithNoTag) {
        return (dsRoot, query, cb) -> {
            final Predicate predicate = getHasTagsPredicate(dsRoot, cb, selectDSWithNoTag, tagNames);
            query.distinct(true);
            return predicate;
        };
    }

    /**
     * returns query criteria {@link Specification} comparing case insensitive "NAME == AND VERSION ==".
     *
     * @param name to be filtered on
     * @param version to be filtered on
     * @return the {@link Specification}
     */
    public static Specification<JpaDistributionSet> equalsNameAndVersionIgnoreCase(final String name, final String version) {
        return (dsRoot, query, cb) -> cb.and(
                cb.equal(cb.lower(dsRoot.get(AbstractJpaNamedEntity_.name)), name.toLowerCase()),
                cb.equal(cb.lower(dsRoot.get(AbstractJpaNamedVersionedEntity_.version)), version.toLowerCase()));
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} with given {@link DistributionSet#getType()}.
     *
     * @param typeId id of distribution set type to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> byType(final Long typeId) {
        return (dsRoot, query, cb) -> cb.equal(dsRoot.get(JpaDistributionSet_.type).get(AbstractJpaBaseEntity_.id), typeId);
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet} for given id collection of {@link DistributionSet#getType()}.
     *
     * @param typeIds id collection of distribution set type to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> hasType(final Collection<Long> typeIds) {
        return (dsRoot, query, cb) -> dsRoot.get(JpaDistributionSet_.type).get(AbstractJpaBaseEntity_.id).in(typeIds);
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSet}s by tag.
     *
     * @param tagId the ID of the distribution set which must be assigned
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSet> hasTag(final Long tagId) {
        return (dsRoot, query, cb) -> {
            final SetJoin<JpaDistributionSet, JpaDistributionSetTag> tags = dsRoot.join(JpaDistributionSet_.tags, JoinType.LEFT);
            return cb.equal(tags.get(AbstractJpaBaseEntity_.id), tagId);
        };
    }

    private static Predicate getHasTagsPredicate(
            final Root<JpaDistributionSet> dsRoot, final CriteriaBuilder cb,
            final Boolean selectDSWithNoTag, final Collection<String> tagNames) {
        final SetJoin<JpaDistributionSet, JpaDistributionSetTag> tags = dsRoot.join(JpaDistributionSet_.tags, JoinType.LEFT);
        final Path<String> exp = tags.get(AbstractJpaNamedEntity_.name);

        final List<Predicate> hasTagsPredicates = new ArrayList<>();
        if (isNoTagActive(selectDSWithNoTag)) {
            hasTagsPredicates.add(exp.isNull());
        }
        if (isAtLeastOneTagActive(tagNames)) {
            hasTagsPredicates.add(exp.in(tagNames));
        }

        return hasTagsPredicates.stream().reduce(cb::or)
                .orElseThrow(() -> new RuntimeException("Neither NO_TAG, nor TAG distribution set tag filter was provided!"));
    }

    private static boolean isNoTagActive(final Boolean selectDSWithNoTag) {
        return Boolean.TRUE.equals(selectDSWithNoTag);
    }

    private static boolean isAtLeastOneTagActive(final Collection<String> tagNames) {
        return !CollectionUtils.isEmpty(tagNames);
    }
}
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

import jakarta.persistence.criteria.Predicate;

import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout_;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

/**
 * Specifications class for {@link Rollout}s. The class provides Spring Data
 * JPQL Specifications.
 */
public final class RolloutSpecification {

    private RolloutSpecification() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link Rollout}s by its <code>deleted</code> attribute.
     *
     * @param isDeleted true/false are compared to the attribute <code>deleted</code>. If NULL the attribute is ignored
     * @return the {@link Rollout} {@link Specification}
     */
    public static Specification<JpaRollout> isDeleted(final Boolean isDeleted, final Sort sort) {
        return (root, query, cb) -> {
            final Predicate predicate = cb.equal(root.<Boolean> get(JpaRollout_.deleted), isDeleted);
            query.orderBy(QueryUtils.toOrders(sort, root, cb));
            return predicate;
        };
    }

    /**
     * Builds a {@link Specification} to search a rollout by name.
     *
     * @param searchText search string
     * @param isDeleted <code>true</code> if deleted rollouts should be included in
     *         the search. Otherwise <code>false</code>
     * @return criteria specification with a query for name of a rollout
     */
    public static Specification<JpaRollout> likeName(final String searchText, final boolean isDeleted) {
        return (rolloutRoot, query, criteriaBuilder) -> {
            final String searchTextToLower = searchText.toLowerCase();
            return criteriaBuilder.and(
                    criteriaBuilder.like(criteriaBuilder.lower(rolloutRoot.get(JpaRollout_.name)), searchTextToLower),
                    criteriaBuilder.equal(rolloutRoot.get(JpaRollout_.deleted), isDeleted));
        };
    }

}

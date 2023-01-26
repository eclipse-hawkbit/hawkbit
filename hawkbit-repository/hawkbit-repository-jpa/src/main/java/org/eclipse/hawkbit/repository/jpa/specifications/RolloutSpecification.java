/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import javax.persistence.criteria.Predicate;

import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout_;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

/**
 * Specifications class for {@link Rollout}s. The class provides Spring Data
 * JPQL Specifications.
 *
 */
public final class RolloutSpecification {
    private RolloutSpecification() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link Rollout}s by its DELETED
     * attribute. Includes fetch for stuff that is required for {@link Rollout}
     * queries.
     *
     * @param isDeleted
     *            TRUE/FALSE are compared to the attribute DELETED. If NULL the
     *            attribute is ignored
     * @return the {@link Rollout} {@link Specification}
     */
    public static Specification<JpaRollout> isDeletedWithDistributionSet(final Boolean isDeleted, final Sort sort) {
        return (root, query, cb) -> {

            final Predicate predicate = cb.equal(root.<Boolean> get(JpaRollout_.deleted), isDeleted);
            root.fetch(JpaRollout_.distributionSet);
            query.orderBy(QueryUtils.toOrders(sort, root, cb));
            return predicate;
        };

    }

    /**
     * Builds a {@link Specification} to search a rollout by name.
     *
     * @param searchText
     *            search string
     * @param isDeleted
     *            <code>true</code> if deleted rollouts should be included in
     *            the search. Otherwise <code>false</code>
     * @return criteria specification with a query for name of a rollout
     *
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

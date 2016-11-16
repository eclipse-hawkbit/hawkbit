/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link TargetFilterQuery}s. The class provides
 * Spring Data JPQL Specifications.
 *
 */
public final class TargetFilterQuerySpecification {
    private TargetFilterQuerySpecification() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link JpaTargetFilterQuery}s based
     * on is {@link JpaTargetFilterQuery#getQuery()}.
     *
     * @param queryValue
     *            the query of the filter
     * @return the {@link JpaTargetFilterQuery} {@link Specification}
     */
    public static Specification<JpaTargetFilterQuery> equalsQuery(final String queryValue) {
        return (targetFilterQueryRoot, query, cb) -> cb.equal(targetFilterQueryRoot.get(JpaTargetFilterQuery_.query),
                queryValue);
    }

    /**
     * {@link Specification} for retrieving {@link JpaTargetFilterQuery}s based
     * on is {@link JpaTargetFilterQuery#getName()}.
     * 
     * @param searchText
     *            of the filter
     * @return the {@link JpaTargetFilterQuery} {@link Specification}
     */
    public static Specification<JpaTargetFilterQuery> likeName(final String searchText) {
        return (targetFilterQueryRoot, query, cb) -> {
            final String searchTextToLower = searchText.toLowerCase();
            return cb.like(cb.lower(targetFilterQueryRoot.get(JpaTargetFilterQuery_.name)), searchTextToLower);
        };
    }

    /**
     * {@link Specification} for retrieving {@link JpaTargetFilterQuery}s based
     * on is {@link JpaTargetFilterQuery#getName()}.
     *
     * @param distributionSet
     *            of the filter
     * @return the {@link JpaTargetFilterQuery} {@link Specification}
     */
    public static Specification<JpaTargetFilterQuery> byAutoAssignDS(final DistributionSet distributionSet) {
        return (targetFilterQueryRoot, query, cb) -> cb
                .equal(targetFilterQueryRoot.get(JpaTargetFilterQuery_.autoAssignDistributionSet), distributionSet);
    }

    /**
     * {@link Specification} for retrieving {@link JpaTargetFilterQuery}s based
     * on is {@link JpaTargetFilterQuery#getName()}.
     *
     * @return the {@link JpaTargetFilterQuery} {@link Specification}
     */
    public static Specification<JpaTargetFilterQuery> withAutoAssignDS() {
        return (targetFilterQueryRoot, query, cb) -> cb
                .isNotNull(targetFilterQueryRoot.get(JpaTargetFilterQuery_.autoAssignDistributionSet));
    }
}

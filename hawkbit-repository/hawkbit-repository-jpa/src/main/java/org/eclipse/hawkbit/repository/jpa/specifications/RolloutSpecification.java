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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout_;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

/**
 * Specifications class for {@link Rollout}s. The class provides Spring Data JPQL Specifications.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RolloutSpecification {

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
}
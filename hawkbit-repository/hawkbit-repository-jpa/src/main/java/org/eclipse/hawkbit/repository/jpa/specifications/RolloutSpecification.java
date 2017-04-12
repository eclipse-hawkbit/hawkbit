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
import org.springframework.data.jpa.domain.Specification;

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
    public static Specification<JpaRollout> isDeletedWithDistributionSet(final Boolean isDeleted) {
        return (root, query, cb) -> {

            final Predicate predicate = cb.equal(root.<Boolean> get(JpaRollout_.deleted), isDeleted);
            root.fetch(JpaRollout_.distributionSet);
            return predicate;
        };

    }

}

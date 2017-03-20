/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import javax.persistence.criteria.Join;

import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link Tag}s. The class provides Spring Data JPQL
 * Specifications.
 *
 */
public final class TagSpecification {
    private TagSpecification() {
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
    public static Specification<JpaTargetTag> ofTarget(final String controllerId) {
        return (targetRoot, query, criteriaBuilder) -> {
            final Join<JpaTargetTag, JpaTarget> tagJoin = targetRoot.join(JpaTargetTag_.assignedToTargets);

            query.distinct(true);

            return criteriaBuilder.equal(tagJoin.get(JpaTarget_.controllerId), controllerId);
        };

    }

}

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

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
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
     * {@link Specification} for retrieving {@link TargetTag}s by assigned
     * {@link Target}.
     * 
     * @param controllerId
     *            of the target
     * 
     * @return the {@link JpaTargetTag} {@link Specification}
     */
    public static Specification<JpaTargetTag> ofTarget(final String controllerId) {
        return (targetRoot, query, criteriaBuilder) -> {
            final Join<JpaTargetTag, JpaTarget> tagJoin = targetRoot.join(JpaTargetTag_.assignedToTargets);

            query.distinct(true);

            return criteriaBuilder.equal(tagJoin.get(JpaTarget_.controllerId), controllerId);
        };

    }

    /**
     * {@link Specification} for retrieving {@link DistributionSetTag}s by
     * assigned {@link DistributionSet}.
     * 
     * @param dsId
     *            of the distribution set
     * 
     * @return the {@link JpaDistributionSetTag} {@link Specification}
     */
    public static Specification<JpaDistributionSetTag> ofDistributionSet(final Long dsId) {
        return (dsRoot, query, criteriaBuilder) -> {
            final Join<JpaDistributionSetTag, JpaDistributionSet> tagJoin = dsRoot
                    .join(JpaDistributionSetTag_.assignedToDistributionSet);

            query.distinct(true);

            return criteriaBuilder.equal(tagJoin.get(JpaDistributionSet_.id), dsId);
        };

    }

}

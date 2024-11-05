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

import jakarta.persistence.criteria.Join;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link org.eclipse.hawkbit.repository.model.Tag}s.
 * The class provides Spring Data JPQL Specifications.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagSpecification {

    /**
     * {@link Specification} for retrieving {@link DistributionSetTag}s by
     * assigned {@link DistributionSet}.
     *
     * @param dsId of the distribution set
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
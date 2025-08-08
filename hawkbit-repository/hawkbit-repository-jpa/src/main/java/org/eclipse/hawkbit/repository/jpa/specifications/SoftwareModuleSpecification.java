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

import jakarta.persistence.criteria.ListJoin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule_;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link SoftwareModule}s. The class provides Spring Data JPQL Specifications
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoftwareModuleSpecification {

    public static Specification<JpaSoftwareModule> byAssignedToDs(final Long dsId) {
        return (swRoot, query, cb) -> {
            final ListJoin<JpaSoftwareModule, JpaDistributionSet> join = swRoot.join(JpaSoftwareModule_.assignedTo);
            return cb.equal(join.get(AbstractJpaBaseEntity_.ID), dsId);
        };
    }
}
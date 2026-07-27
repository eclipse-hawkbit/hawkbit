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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaNamedEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAutoAssignment;
import org.eclipse.hawkbit.repository.jpa.model.JpaAutoAssignment_;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link AutoAssignment}s. The class provides Spring Data JPQL Specifications.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AutoAssignmentSpecification {

    /**
     * {@link Specification} for retrieving a {@link JpaAutoAssignment} by its {@link JpaAutoAssignment#getName()}.
     *
     * @param name the name of the auto assignment (unique per tenant)
     * @return the {@link JpaAutoAssignment} {@link Specification}
     */
    public static Specification<JpaAutoAssignment> byName(final String name) {
        return (autoAssignmentRoot, query, cb) -> cb.equal(autoAssignmentRoot.get(AbstractJpaNamedEntity_.name), name);
    }

    /**
     * {@link Specification} for retrieving {@link JpaAutoAssignment}s based
     * on is {@link JpaAutoAssignment#getName()}.
     *
     * @param distributionSet of the filter
     * @return the {@link JpaAutoAssignment} {@link Specification}
     */
    public static Specification<JpaAutoAssignment> byDistributionSet(final DistributionSet distributionSet) {
        return (autoAssignmentRoot, query, cb) -> cb
                .equal(autoAssignmentRoot.get(JpaAutoAssignment_.distributionSet), distributionSet);
    }

    /**
     * {@link Specification} for retrieving {@link JpaAutoAssignment}s based
     * on is {@link JpaAutoAssignment#getName()}.
     *
     * @return the {@link JpaAutoAssignment} {@link Specification}
     */
    public static Specification<JpaAutoAssignment> activeAutoAssignment() {
        return (autoAssignmentRoot, query, cb) -> cb.and(
                cb.isNotNull(autoAssignmentRoot.get(JpaAutoAssignment_.distributionSet)),
                cb.or(cb.equal(autoAssignmentRoot.get(JpaAutoAssignment_.status), AutoAssignment.AutoAssignStatus.RUNNING),
                        cb.equal(autoAssignmentRoot.get(JpaAutoAssignment_.status), AutoAssignment.AutoAssignStatus.READY)));
    }

    /**
     * {@link Specification} for retrieving all {@link JpaAutoAssignment}s regardless of their auto-assign status.
     *
     * @return the {@link JpaAutoAssignment} {@link Specification}
     */
    public static Specification<JpaAutoAssignment> getAll() {
        return (autoAssignmentRoot, query, cb) -> cb
                .isNotNull(autoAssignmentRoot.get(JpaAutoAssignment_.distributionSet));
    }
}

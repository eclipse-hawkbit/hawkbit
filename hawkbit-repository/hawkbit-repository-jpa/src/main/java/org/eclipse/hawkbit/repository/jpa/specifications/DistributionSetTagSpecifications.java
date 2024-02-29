/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag_;
import org.springframework.data.jpa.domain.Specification;

import jakarta.validation.constraints.NotEmpty;

/**
 * Utility class for {@link JpaDistributionSetTag}s {@link Specification}s. The class provides
 * Spring Data JPQL Specifications.
 */
public final class DistributionSetTagSpecifications {

    private DistributionSetTagSpecifications() {
        // utility class
    }

    public static Specification<JpaDistributionSetTag> byName(@NotEmpty final String name) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaDistributionSetTag_.name), name);
    }
}

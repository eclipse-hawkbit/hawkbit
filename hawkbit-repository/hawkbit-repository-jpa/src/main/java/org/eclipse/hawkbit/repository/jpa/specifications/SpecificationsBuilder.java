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

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Helper class to easily combine {@link Specification} instances.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpecificationsBuilder {

    /**
     * Combine all given specification with and. The first specification is the
     * where clause.
     *
     * @param specList all specification which will combine
     * @return <null> if the given specification list is empty
     */
    public static <T> Specification<T> combineWithAnd(final List<Specification<T>> specList) {
        if (specList.isEmpty()) {
            return null;
        }
        Specification<T> specs = specList.get(0);
        for (final Specification<T> specification : specList.subList(1, specList.size())) {
            specs = specs.and(specification);
        }
        return specs;
    }
}
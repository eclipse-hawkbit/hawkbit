/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;

/**
 * Helper class to easily combine {@link Specification} instances.
 *
 */
public final class SpecificationsBuilder {

    private SpecificationsBuilder() {

    }

    /**
     * Combine all given specification with and. The first specification is the
     * where clause.
     *
     * @param specList
     *            all specification which will combine
     * @return <null> if the given specification list is empty
     */
    public static <T> Specifications<T> combineWithAnd(final List<Specification<T>> specList) {
        if (specList.isEmpty()) {
            return null;
        }
        Specifications<T> specs = Specifications.where(specList.get(0));
        for (final Specification<T> specification : specList.subList(1, specList.size())) {
            specs = specs.and(specification);
        }
        return specs;
    }

}

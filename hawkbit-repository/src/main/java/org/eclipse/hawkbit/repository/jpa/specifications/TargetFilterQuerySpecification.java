/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery_;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link TargetFilterQuery}s. The class provides
 * Spring Data JPQL Specifications.
 *
 */
public class TargetFilterQuerySpecification {
    private TargetFilterQuerySpecification() {
        // utility class
    }

    public static Specification<JpaTargetFilterQuery> likeName(final String searchText) {
        return (targetFilterQueryRoot, query, cb) -> {
            final String searchTextToLower = searchText.toLowerCase();
            return cb.like(cb.lower(targetFilterQueryRoot.get(JpaTargetFilterQuery_.name)), searchTextToLower);
        };
    }
}

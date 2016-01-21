/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.specifications;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery_;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 *
 */
public class TargetFilterQuerySpecification {
    private TargetFilterQuerySpecification() {
        // utility class
    }

    public static Specification<TargetFilterQuery> likeName(final String searchText) {
        final Specification<TargetFilterQuery> spec = new Specification<TargetFilterQuery>() {
            @Override
            public Predicate toPredicate(final Root<TargetFilterQuery> targetFilterQueryRoot,
                    final CriteriaQuery<?> query, final CriteriaBuilder cb) {
                final String searchTextToLower = searchText.toLowerCase();
                final Predicate predicate = cb.like(cb.lower(targetFilterQueryRoot.get(TargetFilterQuery_.name)),
                        searchTextToLower);
                return predicate;
            }
        };
        return spec;
    }
}

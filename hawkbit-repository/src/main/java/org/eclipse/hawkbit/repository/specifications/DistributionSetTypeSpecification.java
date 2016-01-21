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

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSetType_;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 *
 *
 *
 *
 */
public final class DistributionSetTypeSpecification {
    private DistributionSetTypeSpecification() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSetType}s by its
     * DELETED attribute.
     * 
     * @param isDeleted
     *            TRUE/FALSE are compared to the attribute DELETED. If NULL the
     *            attribute is ignored
     * @return the {@link DistributionSetType} {@link Specification}
     */
    public static Specification<DistributionSetType> isDeleted(final Boolean isDeleted) {
        final Specification<DistributionSetType> spec = new Specification<DistributionSetType>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSetType> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                final Predicate predicate = cb.equal(targetRoot.<Boolean> get(DistributionSetType_.deleted), isDeleted);
                return predicate;
            }
        };

        return spec;
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSetType} with
     * given {@link DistributionSetType#getId()} including fetching the elements
     * list.
     *
     * @param distid
     *            to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<DistributionSetType> byId(final Long distid) {
        final Specification<DistributionSetType> spec = new Specification<DistributionSetType>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSetType> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {

                final Predicate predicate = cb.equal(targetRoot.<Long> get(DistributionSetType_.id), distid);
                return predicate;
            }
        };
        return spec;
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSetType} with
     * given {@link DistributionSetType#getName())} including fetching the
     * elements list.
     *
     * @param name
     *            to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<DistributionSetType> byName(final String name) {
        final Specification<DistributionSetType> spec = new Specification<DistributionSetType>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSetType> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {

                final Predicate predicate = cb.equal(targetRoot.<String> get(DistributionSetType_.name), name);
                return predicate;
            }
        };
        return spec;
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSetType} with
     * given {@link DistributionSetType#getKey()} including fetching the
     * elements list.
     *
     * @param key
     *            to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<DistributionSetType> byKey(final String key) {
        final Specification<DistributionSetType> spec = new Specification<DistributionSetType>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSetType> targetRoot, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {

                final Predicate predicate = cb.equal(targetRoot.<String> get(DistributionSetType_.key), key);
                return predicate;
            }
        };
        return spec;
    }

}

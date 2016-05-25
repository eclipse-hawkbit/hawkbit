/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link DistributionSetType}s. The class provides
 * Spring Data JPQL Specifications.
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
    public static Specification<JpaDistributionSetType> isDeleted(final Boolean isDeleted) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.<Boolean> get(JpaDistributionSetType_.deleted),
                isDeleted);
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
    public static Specification<JpaDistributionSetType> byId(final Long distid) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.<Long> get(JpaDistributionSetType_.id), distid);
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
    public static Specification<JpaDistributionSetType> byName(final String name) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.<String> get(JpaDistributionSetType_.name), name);
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
    public static Specification<JpaDistributionSetType> byKey(final String key) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.<String> get(JpaDistributionSetType_.key), key);
    }

}

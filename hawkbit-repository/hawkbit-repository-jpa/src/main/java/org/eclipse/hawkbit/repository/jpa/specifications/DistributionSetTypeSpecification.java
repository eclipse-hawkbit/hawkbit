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
     * {@link Specification} for retrieving {@link DistributionSetType}s with
     * DELETED attribute <code>false</code> - i.e. is not deleted.
     *
     * @return the {@link DistributionSetType} {@link Specification}
     */
    public static Specification<JpaDistributionSetType> isNotDeleted() {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.<Boolean> get(JpaDistributionSetType_.deleted), false);
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
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaDistributionSetType_.id), distid);
    }

    /**
     * {@link Specification} for retrieving {@link DistributionSetType} with
     * given {@link DistributionSetType#getName()} including fetching the
     * elements list.
     *
     * @param name
     *            to search
     * @return the {@link DistributionSet} {@link Specification}
     */
    public static Specification<JpaDistributionSetType> byName(final String name) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaDistributionSetType_.name), name);
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
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaDistributionSetType_.key), key);
    }

}

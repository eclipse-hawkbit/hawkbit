/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import jakarta.persistence.criteria.SetJoin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaNamedEntity_;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaTypeEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType_;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link TargetType}s. The class provides Spring Data JPQL Specifications.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TargetTypeSpecification {

    /**
     * {@link Specification} for retrieving {@link TargetType}s based on a {@link DistributionSetType} name.
     *
     * @param dsTypeId to search for
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasDsSetType(final Long dsTypeId) {
        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTargetType, JpaDistributionSetType> join = targetRoot.join(JpaTargetType_.distributionSetTypes);
            return cb.equal(join.get(AbstractJpaBaseEntity_.id), dsTypeId);
        };
    }

    /**
     * {@link Specification} for retrieving {@link TargetType} with given {@link TargetType#getKey()} including fetching the elements list.
     *
     * @param key to search
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasKey(final String key) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(AbstractJpaTypeEntity_.key), key);
    }

    /**
     * {@link Specification} for retrieving {@link TargetType} with given {@link TargetType#getName()} including fetching the elements list.
     *
     * @param name to search
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasName(final String name) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(AbstractJpaNamedEntity_.name), name);
    }
}
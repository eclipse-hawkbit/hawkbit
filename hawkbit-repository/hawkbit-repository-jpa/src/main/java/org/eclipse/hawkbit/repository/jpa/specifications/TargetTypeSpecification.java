/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.SetJoin;
import java.util.Collection;

/**
 * Specifications class for {@link TargetType}s. The class provides Spring Data JPQL
 * Specifications.
 *
 */
public final class TargetTypeSpecification {

    private TargetTypeSpecification() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link TargetType}s by controllerId
     *
     * @param id
     *            to search for
     *
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasId(final Long id) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTargetType_.id), id);
    }

    /**
     * {@link Specification} for retrieving {@link TargetType}s by controllerId
     *
     * @param id
     *            to search for
     *
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasTarget(final long id) {
        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTargetType, JpaTarget> join = targetRoot.join(JpaTargetType_.targets);
            return cb.equal(join.get(JpaTarget_.id), id);
        };
    }

    /**
     * {@link Specification} for retrieving {@link TargetType}s by controllerId
     *
     * @param ids
     *            to search for
     *
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasTarget(final Collection<Long> ids) {
        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTargetType, JpaTarget> join = targetRoot.join(JpaTargetType_.targets);
            return join.get(JpaTarget_.id).in(ids);
        };
    }

    /**
     * {@link Specification} for retrieving {@link TargetType}s by controllerId
     *
     * @param controllerId
     *            to search for
     *
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasTargetControllerId(final String controllerId) {
        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTargetType, JpaTarget> join = targetRoot.join(JpaTargetType_.targets);
            return cb.equal(join.get(JpaTarget_.controllerId), controllerId);
        };
    }

    /**
     * {@link Specification} for retrieving {@link TargetType}s by controllerId
     *
     * @param controllerIds
     *            to search for
     *
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasTargetControllerIdIn(final Collection<String> controllerIds) {
        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTargetType, JpaTarget> join = targetRoot.join(JpaTargetType_.targets);
            return join.get(JpaTarget_.controllerId).in(controllerIds);
        };
    }

    /**
     * {@link Specification} for retrieving {@link TargetType}s by controllerId
     *
     * @param ids
     *            to search for
     *
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasIdIn(final Collection<Long> ids) {
        return (targetRoot, query, cb) -> targetRoot.get(JpaTargetType_.id).in(ids);
    }

    /**
     * {@link Specification} for retrieving {@link TargetType}s based on a
     * {@link DistributionSetType} name.
     *
     * @param dsTypeId
     *            to search for
     *
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasDsSetType(final Long dsTypeId) {
        return (targetRoot, query, cb) -> {
            final SetJoin<JpaTargetType, JpaDistributionSetType> join = targetRoot.join(JpaTargetType_.distributionSetTypes);
            return cb.equal(join.get(JpaDistributionSetType_.id), dsTypeId);
        };
    }


    /**
     * {@link Specification} for retrieving {@link TargetType} with
     * given {@link TargetType#getName()} including fetching the
     * elements list.
     *
     * @param name
     *            to search
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> hasName(final String name) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTargetType_.name), name);
    }

    /**
     * {@link Specification} for retrieving {@link TargetType}s by "like name".
     *
     * @param name
     *            to be filtered on
     * @return the {@link TargetType} {@link Specification}
     */
    public static Specification<JpaTargetType> likeName(final String name) {
        return (targetRoot, query, cb) -> cb.like(cb.lower(targetRoot.get(JpaTargetType_.name)), name.toLowerCase());
    }
}

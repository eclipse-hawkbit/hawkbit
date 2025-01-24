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

import jakarta.persistence.criteria.ListJoin;

import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaNamedEntity_;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaNamedVersionedEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link SoftwareModule}s. The class provides Spring
 * Data JPQL Specifications
 */
public final class SoftwareModuleSpecification {

    private SoftwareModuleSpecification() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModule} with given
     * {@link DistributionSet#getId()}.
     *
     * @param swModuleId to search
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<JpaSoftwareModule> byId(final Long swModuleId) {
        return (swRoot, query, cb) -> cb.equal(swRoot.get(AbstractJpaBaseEntity_.id), swModuleId);
    }

    public static Specification<JpaSoftwareModule> byAssignedToDs(final Long dsId) {
        return (swRoot, query, cb) -> {
            final ListJoin<JpaSoftwareModule, JpaDistributionSet> join = swRoot.join(JpaSoftwareModule_.assignedTo);
            return cb.equal(join.get(AbstractJpaBaseEntity_.ID), dsId);
        };
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModule}s with
     * DELETED attribute <code>false</code> - i.e. is not deleted.
     *
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<JpaSoftwareModule> isNotDeleted() {
        return (swRoot, query, cb) -> cb.equal(swRoot.get(JpaSoftwareModule_.deleted), false);
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModule}s by "like
     * name and like version".
     *
     * @param name to be filtered on
     * @param version to be filtered on
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<JpaSoftwareModule> likeNameAndVersion(final String name, final String version) {
        return (smRoot, query, cb) -> cb.and(
                cb.like(cb.lower(smRoot.get(AbstractJpaNamedEntity_.name)), name.toLowerCase()),
                cb.like(cb.lower(smRoot.get(AbstractJpaNamedVersionedEntity_.version)), version.toLowerCase()));
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModule}s by "like
     * name or like version".
     *
     * @param type to be filtered on
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<JpaSoftwareModule> equalType(final Long type) {
        return (smRoot, query, cb) -> cb.equal(
                smRoot.get(JpaSoftwareModule_.type).get(AbstractJpaBaseEntity_.id), type);
    }

    /**
     * {@link Specification} for fetching {@link SoftwareModule}s type.
     *
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<JpaSoftwareModule> fetchType() {
        return (smRoot, query, cb) -> {
            if (!query.getResultType().isAssignableFrom(Long.class)) {
                smRoot.fetch(JpaSoftwareModule_.type);
            }
            return cb.conjunction();
        };
    }
}
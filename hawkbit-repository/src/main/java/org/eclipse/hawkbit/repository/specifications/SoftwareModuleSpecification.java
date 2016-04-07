/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.specifications;

import javax.persistence.criteria.Predicate;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModule_;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link SoftwareModule}s. The class provides Spring
 * Data JPQL Specifications
 *
 */
public final class SoftwareModuleSpecification {
    private SoftwareModuleSpecification() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModule}s by its ID
     * attribute.
     * 
     * @param moduleId
     *            to search for
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<SoftwareModule> byId(final Long moduleId) {
        return (targetRoot, query, cb) -> {
            final Predicate predicate = cb.equal(targetRoot.<Long> get(SoftwareModule_.id), moduleId);
            targetRoot.fetch(SoftwareModule_.type);
            return predicate;
        };
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModule}s where its
     * DELETED attribute is false.
     * 
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<SoftwareModule> isDeletedFalse() {
        return (swRoot, query, cb) -> cb.equal(swRoot.<Boolean> get(SoftwareModule_.deleted), Boolean.FALSE);
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModule}s by
     * "like name or like version".
     * 
     * @param subString
     *            to be filtered on
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<SoftwareModule> likeNameOrVersion(final String subString) {
        return (targetRoot, query, cb) -> cb.or(
                cb.like(cb.lower(targetRoot.<String> get(SoftwareModule_.name)), subString.toLowerCase()),
                cb.like(cb.lower(targetRoot.<String> get(SoftwareModule_.version)), subString.toLowerCase()));
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModule}s by
     * "like name or like version".
     * 
     * @param type
     *            to be filtered on
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<SoftwareModule> equalType(final SoftwareModuleType type) {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.<SoftwareModuleType> get(SoftwareModule_.type), type);
    }

}

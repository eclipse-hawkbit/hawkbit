/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule_;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
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
     * {@link Specification} for retrieving {@link SoftwareModule}s where its
     * DELETED attribute is false.
     * 
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<JpaSoftwareModule> isDeletedFalse() {
        return (swRoot, query, cb) -> cb.equal(swRoot.<Boolean> get(JpaSoftwareModule_.deleted), Boolean.FALSE);
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModule}s by "like
     * name or like version".
     * 
     * @param subString
     *            to be filtered on
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<JpaSoftwareModule> likeNameOrVersion(final String subString) {
        return (targetRoot, query, cb) -> cb.or(
                cb.like(cb.lower(targetRoot.<String> get(JpaSoftwareModule_.name)), subString.toLowerCase()),
                cb.like(cb.lower(targetRoot.<String> get(JpaSoftwareModule_.version)), subString.toLowerCase()));
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModule}s by "like
     * name or like version".
     * 
     * @param type
     *            to be filtered on
     * @return the {@link SoftwareModule} {@link Specification}
     */
    public static Specification<JpaSoftwareModule> equalType(final Long type) {
        return (targetRoot, query, cb) -> cb.equal(
                targetRoot.<JpaSoftwareModuleType> get(JpaSoftwareModule_.type).get(JpaSoftwareModuleType_.id), type);
    }

}

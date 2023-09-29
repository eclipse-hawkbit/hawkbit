/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType_;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

/**
 * Specifications class for {@link SoftwareModuleType}s. The class provides
 * Spring Data JPQL Specifications.
 */
public class SoftwareModuleTypeSpecification {

    private SoftwareModuleTypeSpecification() {
        // utility class
    }

    public static Specification<JpaSoftwareModuleType> byId(final Long typeId) {
        return (root, query, cb) -> cb.equal(root.get(JpaSoftwareModuleType_.id), typeId);
    }

    public static Specification<JpaSoftwareModuleType> byIds(final Collection<Long> typeIds) {
        return (root, query, cb) -> root.get(JpaSoftwareModuleType_.id).in(typeIds);
    }

    public static Specification<JpaSoftwareModuleType> byKey(final String key) {
        return (root, query, cb) -> cb.equal(root.get(JpaSoftwareModuleType_.key), key);
    }

    public static Specification<JpaSoftwareModuleType> byName(final String name) {
        return (root, query, cb) -> cb.equal(root.get(JpaSoftwareModuleType_.name), name);
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModuleType}s by its
     * DELETED attribute.
     * 
     * @param isDeleted
     *            TRUE/FALSE are compared to the attribute DELETED. If NULL the
     *            attribute is ignored
     * @return the {@link SoftwareModuleType} {@link Specification}
     */
    public static Specification<JpaSoftwareModuleType> isDeleted(final Boolean isDeleted) {
        return (root, query, cb) -> cb.equal(root.get(JpaSoftwareModuleType_.deleted), isDeleted);
    }
}

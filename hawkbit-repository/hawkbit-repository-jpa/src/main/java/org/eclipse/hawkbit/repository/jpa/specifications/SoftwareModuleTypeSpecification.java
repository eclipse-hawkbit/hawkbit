/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType_;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link SoftwareModuleType}s. The class provides
 * Spring Data JPQL Specifications.
 */
public class SoftwareModuleTypeSpecification {

    private SoftwareModuleTypeSpecification() {
        // utility class
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

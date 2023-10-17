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

/**
 * Specifications class for {@link SoftwareModuleType}s. The class provides
 * Spring Data JPQL Specifications.
 */
public class SoftwareModuleTypeSpecification {

    private SoftwareModuleTypeSpecification() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link SoftwareModuleType}s with
     * DELETED attribute <code>false</code> - i.e. is not deleted.
     *
     * @return the {@link SoftwareModuleType} {@link Specification}
     */
    public static Specification<JpaSoftwareModuleType> isNotDeleted() {
        return (root, query, cb) -> cb.equal(root.get(JpaSoftwareModuleType_.deleted), false);
    }
}

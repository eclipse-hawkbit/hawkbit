/**
 * Copyright (c) 2026 Bosch Digital GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType_;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SoftwareModuleTypeSpecification {

    public static Specification<JpaSoftwareModuleType> isDeleted(final Boolean isDeleted) {
        return (root, query, cb) ->
                cb.equal(root.<Boolean> get(JpaSoftwareModuleType_.deleted), isDeleted);
    }
}

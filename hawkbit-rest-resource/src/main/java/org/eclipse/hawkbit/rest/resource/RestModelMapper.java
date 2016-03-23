/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.rest.resource.model.BaseEntityRest;
import org.eclipse.hawkbit.rest.resource.model.NamedEntityRest;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 * 
 *
 *
 *
 */
final class RestModelMapper {

    // private constructor, utility class
    private RestModelMapper() {

    }

    static void mapBaseToBase(final BaseEntityRest response, final TenantAwareBaseEntity base) {
        response.setCreatedBy(base.getCreatedBy());
        response.setLastModifiedBy(base.getLastModifiedBy());
        if (base.getCreatedAt() != null) {
            response.setCreatedAt(base.getCreatedAt());
        }
        if (base.getLastModifiedAt() != null) {
            response.setLastModifiedAt(base.getLastModifiedAt());
        }
    }

    static void mapNamedToNamed(final NamedEntityRest response, final NamedEntity base) {
        mapBaseToBase(response, base);

        response.setName(base.getName());
        response.setDescription(base.getDescription());
    }
}

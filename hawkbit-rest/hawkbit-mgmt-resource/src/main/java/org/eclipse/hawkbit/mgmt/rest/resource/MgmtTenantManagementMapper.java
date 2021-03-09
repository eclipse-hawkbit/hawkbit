/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValue;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtTenantManagementMapper {

    private MgmtTenantManagementMapper() {
        // Utility class
    }

    static Map<String, MgmtSystemTenantConfigurationValue> toResponse(
            final TenantConfigurationManagement tenantConfigurationManagement,
            final TenantConfigurationProperties tenantConfigurationProperties) {

        return tenantConfigurationProperties.getConfigurationKeys().stream()
                .collect(Collectors.toMap(TenantConfigurationKey::getKeyName, key -> toResponse(key.getKeyName(),
                        tenantConfigurationManagement.getConfigurationValue(key.getKeyName()))));
    }

    static MgmtSystemTenantConfigurationValue toResponse(final String key,
            final TenantConfigurationValue<?> repoConfValue) {
        final MgmtSystemTenantConfigurationValue restConfValue = new MgmtSystemTenantConfigurationValue();

        restConfValue.setValue(repoConfValue.getValue());
        restConfValue.setGlobal(repoConfValue.isGlobal());
        restConfValue.setCreatedAt(repoConfValue.getCreatedAt());
        restConfValue.setCreatedBy(repoConfValue.getCreatedBy());
        restConfValue.setLastModifiedAt(repoConfValue.getLastModifiedAt());
        restConfValue.setLastModifiedBy(repoConfValue.getLastModifiedBy());

        restConfValue.add(
                linkTo(methodOn(MgmtTenantManagementResource.class).getTenantConfigurationValue(key)).withSelfRel());

        return restConfValue;
    }
}

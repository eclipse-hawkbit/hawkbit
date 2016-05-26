/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValue;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public class MgmtSystemMapper {

    private MgmtSystemMapper() {
        // Utility class
    }

    /**
     * @param tenantConfigurationManagement
     *            instance of TenantConfigurationManagement
     * @return a map of all existing configuration values
     */
    public static Map<String, MgmtSystemTenantConfigurationValue> toResponse(
            final TenantConfigurationManagement tenantConfigurationManagement) {

        final Map<String, MgmtSystemTenantConfigurationValue> configurationMap = new HashMap<>();

        for (final TenantConfigurationKey key : TenantConfigurationKey.values()) {
            configurationMap.put(key.getKeyName(),
                    toResponse(key.getKeyName(), tenantConfigurationManagement.getConfigurationValue(key)));
        }

        return configurationMap;
    }

    /**
     * maps a TenantConfigurationValue from the repository model to a
     * MgmtSystemTenantConfigurationValue, the RESTful model.
     * 
     * @param repoConfValue
     *            configuration value as repository model
     * @return configuration value as RESTful model
     */
    public static MgmtSystemTenantConfigurationValue toResponse(final String key,
            final TenantConfigurationValue<?> repoConfValue) {
        final MgmtSystemTenantConfigurationValue restConfValue = new MgmtSystemTenantConfigurationValue();

        restConfValue.setValue(repoConfValue.getValue());
        restConfValue.setGlobal(repoConfValue.isGlobal());
        restConfValue.setCreatedAt(repoConfValue.getCreatedAt());
        restConfValue.setCreatedBy(repoConfValue.getCreatedBy());
        restConfValue.setLastModifiedAt(repoConfValue.getLastModifiedAt());
        restConfValue.setLastModifiedBy(repoConfValue.getLastModifiedBy());

        restConfValue.add(linkTo(methodOn(MgmtSystemResource.class).getConfigurationValue(key)).withRel("self"));

        return restConfValue;
    }
}

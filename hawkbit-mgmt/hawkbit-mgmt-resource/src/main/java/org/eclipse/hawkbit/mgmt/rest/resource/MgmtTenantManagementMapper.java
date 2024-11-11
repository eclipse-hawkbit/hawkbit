/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtTenantManagementMapper {

    public static String DEFAULT_DISTRIBUTION_SET_TYPE_KEY = "default.ds.type";

    private MgmtTenantManagementMapper() {
        // Utility class
    }

    public static MgmtSystemTenantConfigurationValue toResponseTenantConfigurationValue(String key, TenantConfigurationValue<?> repoConfValue) {
        final MgmtSystemTenantConfigurationValue restConfValue = new MgmtSystemTenantConfigurationValue();
        restConfValue.setValue(repoConfValue.getValue());
        restConfValue.setGlobal(repoConfValue.isGlobal());
        restConfValue.setCreatedAt(repoConfValue.getCreatedAt());
        restConfValue.setCreatedBy(repoConfValue.getCreatedBy());
        restConfValue.setLastModifiedAt(repoConfValue.getLastModifiedAt());
        restConfValue.setLastModifiedBy(repoConfValue.getLastModifiedBy());
        restConfValue.add(linkTo(methodOn(MgmtTenantManagementResource.class).getTenantConfigurationValue(key))
                .withSelfRel().expand());
        return restConfValue;
    }

    public static MgmtSystemTenantConfigurationValue toResponseDefaultDsType(Long defaultDistributionSetType) {
        final MgmtSystemTenantConfigurationValue restConfValue = new MgmtSystemTenantConfigurationValue();
        restConfValue.setValue(defaultDistributionSetType);
        restConfValue.setGlobal(Boolean.FALSE);
        restConfValue.add(linkTo(methodOn(MgmtTenantManagementResource.class).getTenantConfigurationValue(DEFAULT_DISTRIBUTION_SET_TYPE_KEY))
                .withSelfRel().expand());
        return restConfValue;
    }
}
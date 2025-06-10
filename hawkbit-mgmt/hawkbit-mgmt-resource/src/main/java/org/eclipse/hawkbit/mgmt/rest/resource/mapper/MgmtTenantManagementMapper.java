/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValue;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtTenantManagementResource;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MgmtTenantManagementMapper {

    public static final String DEFAULT_DISTRIBUTION_SET_TYPE_KEY = "default.ds.type";

    public static MgmtSystemTenantConfigurationValue toResponseTenantConfigurationValue(
            final String key, final TenantConfigurationValue<?> repoConfValue) {
        final MgmtSystemTenantConfigurationValue restConfValue = new MgmtSystemTenantConfigurationValue();
        restConfValue.setValue(repoConfValue.getValue());
        restConfValue.setGlobal(repoConfValue.isGlobal());
        restConfValue.setCreatedAt(repoConfValue.getCreatedAt());
        restConfValue.setCreatedBy(repoConfValue.getCreatedBy());
        restConfValue.setLastModifiedAt(repoConfValue.getLastModifiedAt());
        restConfValue.setLastModifiedBy(repoConfValue.getLastModifiedBy());
        restConfValue.add(WebMvcLinkBuilder.linkTo(methodOn(MgmtTenantManagementResource.class).getTenantConfigurationValue(key)).withSelfRel().expand());
        return restConfValue;
    }

    public static MgmtSystemTenantConfigurationValue toResponseDefaultDsType(final Long defaultDistributionSetType) {
        final MgmtSystemTenantConfigurationValue restConfValue = new MgmtSystemTenantConfigurationValue();
        restConfValue.setValue(defaultDistributionSetType);
        restConfValue.setGlobal(Boolean.FALSE);
        restConfValue.add(linkTo(methodOn(MgmtTenantManagementResource.class).getTenantConfigurationValue(DEFAULT_DISTRIBUTION_SET_TYPE_KEY))
                .withSelfRel().expand());
        return restConfValue;
    }
}
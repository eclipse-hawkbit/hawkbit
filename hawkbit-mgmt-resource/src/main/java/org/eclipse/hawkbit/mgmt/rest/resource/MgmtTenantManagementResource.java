/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.io.Serializable;
import java.util.Map;

import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValue;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValueRequest;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTenantManagementRestApi;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource for handling tenant specific configuration operations.
 */
@RestController
public class MgmtTenantManagementResource implements MgmtTenantManagementRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtTenantManagementResource.class);

    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final TenantConfigurationProperties tenantConfigurationProperties;

    @Autowired
    MgmtTenantManagementResource(final TenantConfigurationManagement tenantConfigurationManagement,
            final TenantConfigurationProperties tenantConfigurationProperties) {
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.tenantConfigurationProperties = tenantConfigurationProperties;
    }

    @Override
    public ResponseEntity<Map<String, MgmtSystemTenantConfigurationValue>> getTenantConfiguration() {
        return ResponseEntity.ok(
                MgmtTenantManagementMapper.toResponse(tenantConfigurationManagement, tenantConfigurationProperties));
    }

    @Override
    public ResponseEntity<Void> deleteTenantConfigurationValue(@PathVariable("keyName") final String keyName) {

        tenantConfigurationManagement.deleteConfiguration(keyName);

        LOG.debug("{} config value deleted, return status {}", keyName, HttpStatus.OK);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtSystemTenantConfigurationValue> getTenantConfigurationValue(
            @PathVariable("keyName") final String keyName) {

        LOG.debug("{} config value getted, return status {}", keyName, HttpStatus.OK);
        return ResponseEntity.ok(MgmtTenantManagementMapper.toResponse(keyName,
                tenantConfigurationManagement.getConfigurationValue(keyName)));
    }

    @Override
    public ResponseEntity<MgmtSystemTenantConfigurationValue> updateTenantConfigurationValue(
            @PathVariable("keyName") final String keyName,
            @RequestBody final MgmtSystemTenantConfigurationValueRequest configurationValueRest) {

        final TenantConfigurationValue<? extends Serializable> updatedValue = tenantConfigurationManagement
                .addOrUpdateConfiguration(keyName, configurationValueRest.getValue());
        return ResponseEntity.ok(MgmtTenantManagementMapper.toResponse(keyName, updatedValue));
    }

}

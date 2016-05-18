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

import java.util.Map;

import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValue;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValueRequest;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSystemRestApi;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling tenant specific configuration operations.
 */
@RestController
public class MgmtSystemResource implements MgmtSystemRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtSystemResource.class);

    @Autowired
    private TenantConfigurationManagement tenantConfigurationManagement;

    @Override
    public ResponseEntity<ResourceSupport> getSystem() {
        final ResourceSupport resourceSupport = new ResourceSupport();
        resourceSupport.add(linkTo(methodOn(MgmtSystemResource.class).getSystemConfiguration()).withRel("configs"));
        return ResponseEntity.ok(resourceSupport);
    }

    /**
     * @return a Map of all configuration values.
     */
    @Override
    public ResponseEntity<Map<String, MgmtSystemTenantConfigurationValue>> getSystemConfiguration() {
        return new ResponseEntity<>(MgmtSystemMapper.toResponse(tenantConfigurationManagement), HttpStatus.OK);
    }

    /**
     * Handles the DELETE request of deleting a tenant specific configuration
     * value within SP.
     *
     * @param keyName
     *            the Name of the configuration key
     * @return If the given configuration value exists and could be deleted Http
     *         OK. In any failure the JsonResponseExceptionHandler is handling
     *         the response.
     */
    @Override
    public ResponseEntity<Void> deleteConfigurationValue(@PathVariable("keyName") final String keyName) {

        final TenantConfigurationKey configKey = TenantConfigurationKey.fromKeyName(keyName);

        tenantConfigurationManagement.deleteConfiguration(configKey);

        LOG.debug("{} config value deleted, return status {}", keyName, HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Handles the GET request of deleting a tenant specific configuration value
     * within SP.
     *
     * @param keyName
     *            the Name of the configuration key
     * @return If the given configuration value exists and could be get Http OK.
     *         In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Override
    public ResponseEntity<MgmtSystemTenantConfigurationValue> getConfigurationValue(
            @PathVariable("keyName") final String keyName) {

        final TenantConfigurationKey configKey = TenantConfigurationKey.fromKeyName(keyName);

        LOG.debug("{} config value getted, return status {}", keyName, HttpStatus.OK);
        return new ResponseEntity<>(MgmtSystemMapper.toResponse(configKey.getKeyName(),
                tenantConfigurationManagement.getConfigurationValue(configKey)), HttpStatus.OK);
    }

    /**
     * Handles the GET request of deleting a tenant specific configuration value
     * within SP.
     *
     * @param keyName
     *            the Name of the configuration key
     * @param configurationValueRest
     *            the new value for the configuration
     * @return If the given configuration value exists and could be get Http OK.
     *         In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Override
    public ResponseEntity<MgmtSystemTenantConfigurationValue> updateConfigurationValue(
            @PathVariable("keyName") final String keyName,
            @RequestBody final MgmtSystemTenantConfigurationValueRequest configurationValueRest) {

        final TenantConfigurationKey configKey = TenantConfigurationKey.fromKeyName(keyName);

        final TenantConfigurationValue<Object> updatedValue = tenantConfigurationManagement

                .addOrUpdateConfiguration(configKey, configurationValueRest.getValue());
        return new ResponseEntity<>(MgmtSystemMapper.toResponse(keyName, updatedValue), HttpStatus.OK);
    }

}
/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.Map;

import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValue;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValueRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * REST Resource for handling tenant specific configuration operations.
 *
 */
@RequestMapping(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING)
public interface MgmtTenantManagementRestApi {

    /**
     * Handles the GET request for receiving all tenant specific configuration
     * values.
     * 
     * @return a map of all configuration values.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/configs", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Map<String, MgmtSystemTenantConfigurationValue>> getTenantConfiguration();

    /**
     * Handles the DELETE request of deleting a tenant specific configuration
     * value.
     *
     * @param keyName
     *            the Name of the configuration key
     * @return if the given configuration value exists and could be deleted HTTP
     *         OK. In any failure the JsonResponseExceptionHandler is handling
     *         the response.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/configs/{keyName}", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> deleteTenantConfigurationValue(@PathVariable("keyName") String keyName);

    /**
     * Handles the GET request of receiving a tenant specific configuration
     * value.
     *
     * @param keyName
     *            the name of the configuration key
     * @return if the given configuration value exists and could be get HTTP OK.
     *         In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/configs/{keyName}", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSystemTenantConfigurationValue> getTenantConfigurationValue(
            @PathVariable("keyName") String keyName);

    /**
     * Handles the PUT request for updating a tenant specific configuration
     * value.
     *
     * @param keyName
     *            the name of the configuration key
     * @param configurationValueRest
     *            the new value for the configuration
     * @return if the given configuration value exists and could be get HTTP OK.
     *         In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/configs/{keyName}", consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSystemTenantConfigurationValue> updateTenantConfigurationValue(
            @PathVariable("keyName") String keyName, MgmtSystemTenantConfigurationValueRequest configurationValueRest);

}

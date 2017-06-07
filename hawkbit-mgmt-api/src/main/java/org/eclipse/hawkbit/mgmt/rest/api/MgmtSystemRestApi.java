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
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * REST Resource handling tenant specific configuration operations.
 *
 *
 */
@RequestMapping(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING)
public interface MgmtSystemRestApi {

    @RequestMapping(method = RequestMethod.GET, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<ResourceSupport> getSystem();

    /**
     * @return a Map of all configuration values.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/configs", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Map<String, MgmtSystemTenantConfigurationValue>> getSystemConfiguration();

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
    @RequestMapping(method = RequestMethod.DELETE, value = "/configs/{keyName}", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> deleteConfigurationValue(@PathVariable("keyName") String keyName);

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
    @RequestMapping(method = RequestMethod.GET, value = "/configs/{keyName}", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSystemTenantConfigurationValue> getConfigurationValue(@PathVariable("keyName") String keyName);

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
    @RequestMapping(method = RequestMethod.PUT, value = "/configs/{keyName}", consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSystemTenantConfigurationValue> updateConfigurationValue(@PathVariable("keyName") String keyName,
            MgmtSystemTenantConfigurationValueRequest configurationValueRest);

}

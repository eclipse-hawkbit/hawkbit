/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.TENANT_ORDER;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.DeleteResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PutResponses;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValue;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValueRequest;
import org.eclipse.hawkbit.rest.OpenApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * REST Resource for handling tenant specific configuration operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(
        name = "System Configuration", description = "REST API for handling tenant specific configuration operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = TENANT_ORDER)))
public interface MgmtTenantManagementRestApi {

    String SYSTEM_V1 = MgmtRestConstants.REST_V1 + "/system";

    /**
     * Handles the GET request for receiving all tenant specific configuration values.
     *
     * @return a map of all configuration values.
     */
    @Operation(summary = "Return all tenant specific configuration values",
            description = "The GET request returns a list of all possible configuration keys for the tenant. Required Permission: READ_TENANT_CONFIGURATION")
    @GetIfExistResponses
    @GetMapping(value = SYSTEM_V1 + "/configs", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Map<String, MgmtSystemTenantConfigurationValue>> getTenantConfiguration();

    /**
     * Handles the GET request of receiving a tenant specific configuration value.
     *
     * @param keyName the name of the configuration key
     * @return if the given configuration value exists and could be get HTTP OK. In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Operation(summary = "Return a tenant specific configuration value", description = "The GET request returns the " +
            "configuration value of a specific configuration key for the tenant. " +
            "Required Permission: READ_TENANT_CONFIGURATION")
    @GetResponses
    @GetMapping(value = SYSTEM_V1 + "/configs/{keyName}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSystemTenantConfigurationValue> getTenantConfigurationValue(@PathVariable("keyName") String keyName);

    /**
     * Handles the PUT request for updating a tenant specific configuration value.
     *
     * @param keyName the name of the configuration key
     * @param configurationValueRest the new value for the configuration
     */
    @Operation(summary = "Update a tenant specific configuration value.",
            description = "The PUT request changes a configuration value of a specific configuration key for the tenant. " +
                    "Required Permission: TENANT_CONFIGURATION")
    @PutResponses
    @PutMapping(value = SYSTEM_V1 + "/configs/{keyName}",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateTenantConfigurationValue(
            @PathVariable("keyName") String keyName,
            @RequestBody MgmtSystemTenantConfigurationValueRequest configurationValueRest);

    /**
     * Handles the PUT request for updating a batch of tenant specific configurations
     *
     * @param configurationValueMap a Map of name - value pairs for the configurations
     */
    @Operation(summary = "Batch update of tenant configuration.",
            description = "The PUT request updates the whole configuration for the tenant. Required Permission: TENANT_CONFIGURATION")
    @PutResponses
    @PutMapping(value = SYSTEM_V1 + "/configs",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateTenantConfiguration(@RequestBody Map<String, Object> configurationValueMap);

    /**
     * Handles the DELETE request of deleting a tenant specific configuration value.
     *
     * @param keyName the Name of the configuration key
     */
    @Operation(summary = "Delete a tenant specific configuration value",
            description = "The DELETE request removes a tenant specific configuration value for the tenant. " +
                    "Afterwards the global default value is used. Required Permission: TENANT_CONFIGURATION")
    @DeleteResponses
    @DeleteMapping(value = SYSTEM_V1 + "/configs/{keyName}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTenantConfigurationValue(@PathVariable("keyName") String keyName);
}
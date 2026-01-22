/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.BASIC_AUTH_ORDER;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.auth.MgmtUserInfo;
import org.eclipse.hawkbit.rest.OpenApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * REST API for handling basic authentication user validation
 */
@SuppressWarnings("squid:S1609")
@Tag(name = "Basic Authentication", description = "API for basic authentication user validation.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = BASIC_AUTH_ORDER)))
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
public interface MgmtBasicAuthRestApi {

    String USERINFO_V1 = MgmtRestConstants.REST_V1 + "/userinfo";

    /**
     * Handles the GET request of basic auth.
     *
     * @return the userinfo with status OK.
     */
    @GetResponses
    @GetMapping(value = USERINFO_V1, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtUserInfo> validateBasicAuth();
}
/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app.mgmt;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Feature: Integration Test - Management API<br/>
 * Story: OpenAPI required permissions
 */
class MgmtOpenApiRequiredPermissionsTest extends AbstractSecurityTest {

    /**
     * Verifies that the required permissions, declared via @RequiredPermissions, are appended (once) to the OpenAPI
     * operation descriptions - including the alternatives, the conditionally required ones and the permissions that imply them.
     */
    @ParameterizedTest
    @ValueSource(strings = { "/v3/api-docs/Management API", "/v3/api-docs" })
    void requiredPermissionsAreDocumented(final String apiDocsUrl) throws Exception {
        mvc.perform(get(apiDocsUrl))
                .andExpect(status().isOk())
                // single permission, implied by others - and appended once only
                .andExpect(jsonPath("$.paths['/rest/v1/softwaremoduletypes'].get.description", equalTo(
                        "Handles the GET request of retrieving all software module types. Required Permission: READ_SOFTWARE_MODULE_TYPE " +
                                "(or implied by CREATE_SOFTWARE_MODULE, DELETE_SOFTWARE_MODULE, READ_SOFTWARE_MODULE, UPDATE_SOFTWARE_MODULE)")))
                // multiple permissions + conditionally required
                .andExpect(jsonPath("$.paths['/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}/targets'].get.description",
                        endsWith(" Required Permissions: READ_ROLLOUT, READ_TARGET. " +
                                "Conditionally Required Permissions: READ_TARGET_SECURITY_TOKEN")))
                // alternatives
                .andExpect(jsonPath("$.paths['/rest/v1/distributionsets'].post.description",
                        containsString(" Required Permissions: CREATE_DISTRIBUTION_SET, one of (READ_DISTRIBUTION_SET, READ_TARGET, " +
                                "READ_TENANT_CONFIGURATION (or implied by TENANT_CONFIGURATION)). " +
                                "Conditionally Required Permissions: READ_SOFTWARE_MODULE")))
                // authentication only
                .andExpect(jsonPath("$.paths['/rest/v1/userinfo'].get.description",
                        endsWith("Required Permission: none (authentication only)")));
    }
}

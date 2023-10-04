/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValueRequest;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Spring MVC Tests against the MgmtTenantManagementResource.
 *
 */
@Feature("Component Tests - Management API")
@Story("Tenant Management Resource")
public class MgmtTenantManagementResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String KEY_MULTI_ASSIGNMENTS = "multi.assignments.enabled";

    private static final String KEY_AUTO_CLOSE = "repository.actions.autoclose.enabled";
    private static final String ROLLOUT_APPROVAL_ENABLED = "rollout.approval.enabled";

    private static final String AUTHENTICATION_GATEWAYTOKEN_ENABLED = "authentication.gatewaytoken.enabled";

    private static final String AUTHENTICATION_GATEWAYTOKEN_KEY = "authentication.gatewaytoken.key";

    @Test
    @Description("Handles GET request for receiving all tenant specific configurations.")
    public void getTenantConfigurations() throws Exception {
        mvc.perform(get(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Handles GET request for receiving a tenant specific configuration.")
    public void getTenantConfiguration() throws Exception {
        mvc.perform(get(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}/",
                        TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Handles PUT request for settings values in tenant specific configuration.")
    public void putTenantConfiguration() throws Exception {
        final MgmtSystemTenantConfigurationValueRequest bodyPut = new MgmtSystemTenantConfigurationValueRequest();
        bodyPut.setValue("exampleToken");
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(bodyPut);

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}/",
                        TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY).content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("The 'multi.assignments.enabled' property must not be changed to false.")
    public void deactivateMultiAssignment() throws Exception {
        final String bodyActivate = new JSONObject().put("value", true).toString();
        final String bodyDeactivate = new JSONObject().put("value", false).toString();

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", KEY_MULTI_ASSIGNMENTS)
                .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", KEY_MULTI_ASSIGNMENTS)
                .content(bodyDeactivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Description("The Batch configuration should be applied")
    public void changeBatchConfiguration() throws Exception {
        JSONObject configuration = new JSONObject();
        configuration.put(ROLLOUT_APPROVAL_ENABLED, true);
        configuration.put(AUTHENTICATION_GATEWAYTOKEN_ENABLED, true);
        configuration.put(AUTHENTICATION_GATEWAYTOKEN_KEY, "1234");

        String body = configuration.toString();

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs")
                        .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("The Batch configuration should not be applied")
    public void changeBatchConfigurationFail() throws Exception {
        JSONObject configuration = new JSONObject();
        configuration.put(ROLLOUT_APPROVAL_ENABLED, true);
        configuration.put(AUTHENTICATION_GATEWAYTOKEN_ENABLED, "wrong");
        configuration.put(AUTHENTICATION_GATEWAYTOKEN_KEY, "1234");

        String body = configuration.toString();

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs")
                        .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("The 'repository.actions.autoclose.enabled' property must not be modified if Multi-Assignments is enabled.")
    public void autoCloseCannotBeModifiedIfMultiAssignmentIsEnabled() throws Exception {
        final String bodyActivate = new JSONObject().put("value", true).toString();
        final String bodyDeactivate = new JSONObject().put("value", false).toString();

        // enable Multi-Assignments
        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", KEY_MULTI_ASSIGNMENTS)
                .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // try to enable Auto-Close
        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", KEY_AUTO_CLOSE)
                .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        // try to disable Auto-Close
        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", KEY_AUTO_CLOSE)
                .content(bodyDeactivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Description("Handles DELETE request deleting a tenant specific configuration.")
    public void deleteTenantConfiguration() throws Exception {
        mvc.perform(delete(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}/",
                        TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }
}

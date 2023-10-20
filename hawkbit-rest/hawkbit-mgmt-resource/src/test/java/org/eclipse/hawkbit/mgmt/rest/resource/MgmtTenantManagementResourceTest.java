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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValueRequest;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
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
    private static final String DEFAULT_DISTRIBUTION_SET_TYPE_KEY = "default.ds.type";

    @Test
    @Description("Handles GET request for receiving all tenant specific configurations.")
    public void getTenantConfigurations() throws Exception {
        mvc.perform(get(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                //check for TenantMetadata additional properties
                .andExpect(jsonPath("$.['" + DEFAULT_DISTRIBUTION_SET_TYPE_KEY + "']").exists())
                .andExpect(jsonPath("$.['" + DEFAULT_DISTRIBUTION_SET_TYPE_KEY + "'].value", equalTo(getActualDefaultDsType())));

    }

    @Test
    @Description("Handles GET request for receiving a tenant specific configuration.")
    public void getTenantConfiguration() throws Exception {
        //Test TenantConfiguration property
        mvc.perform(get(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}/",
                        TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Handles GET request for receiving (TenantMetadata - DefaultDsType) a tenant specific configuration.")
    public void getTenantMetadata() throws Exception {
        //Test TenantMetadata property
        mvc.perform(get(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}/",
                DEFAULT_DISTRIBUTION_SET_TYPE_KEY))
            .andDo(MockMvcResultPrinter.print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value", equalTo(getActualDefaultDsType())));
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
    @Description("Handles PUT request for settings values (TenantMetadata - DefaultDsType) in tenant specific configuration, which is TenantMetadata")
    public void putTenantMetadata() throws Exception {
        final MgmtSystemTenantConfigurationValueRequest bodyPut = new MgmtSystemTenantConfigurationValueRequest();

        String updatedTestDefaultDsType = createTestDistributionSetType();
        bodyPut.setValue(updatedTestDefaultDsType);

        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(bodyPut);

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}/",
                DEFAULT_DISTRIBUTION_SET_TYPE_KEY).content(json)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultPrinter.print())
            .andExpect(status().isOk());

        //check if after Rest success, value is really changed in TenantMetadata
        assertEquals(updatedTestDefaultDsType, getActualDefaultDsType(), "Rest endpoint for updating the Default DistributionSetType completed successfully, but the actual value was not changed.");
    }

    private String createTestDistributionSetType() {
        DistributionSetType testDefaultDsType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create()
            .key("test123").name("TestName123").description("TestDefaultDsType"));
        testDefaultDsType = distributionSetTypeManagement
            .update(entityFactory.distributionSetType().update(testDefaultDsType.getId()).description("TestDefaultDsType"));
        return String.valueOf(testDefaultDsType.getId());
    }

    @Test
    @Description("Update DefaultDistributionSetType Fails if given DistributionSetType ID does not exist.")
    public void putTenantMetadataFails() throws Exception{
        String oldDefaultDsType = getActualDefaultDsType();
        //try an invalid input
        String newDefaultDsType = new JSONObject().put("value", true).toString();
        assertDefaultDsTypeUpdateFails(newDefaultDsType, oldDefaultDsType);
        //try and invalid input
        newDefaultDsType = new JSONObject().put("value", "someInvalidInput").toString();
        assertDefaultDsTypeUpdateFails(newDefaultDsType, oldDefaultDsType);
    }

    private void assertDefaultDsTypeUpdateFails(String newDefaultDsType, String oldDefaultDsType) throws Exception {

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}/",
                DEFAULT_DISTRIBUTION_SET_TYPE_KEY).content(newDefaultDsType)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultPrinter.print())
            .andExpect(status().isBadRequest());
        assertEquals(oldDefaultDsType, getActualDefaultDsType(), "Rest endpoint for updating DefaultDistributionType failed, but actual value changed unexpectedly.");
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
    @Description("The Batch configuration should not be applied, because of invalid TenantConfiguration props")
    public void changeBatchConfigurationFail() throws Exception {
        //in this scenario
        //  some TenantConfiguration are not valid,
        //  TenantMetadata - DefaultDSType ID is valid,
        //in the end batch configuration update must fail, and thus, not a single config should be actually changed
        String     testValidDistributionSetType = createTestDistributionSetType();
        boolean oldRolloutApprovalConfig = (Boolean) tenantConfigurationManagement.getConfigurationValue(ROLLOUT_APPROVAL_ENABLED).getValue();
        String oldAuthGatewayToken = (String) tenantConfigurationManagement.getConfigurationValue(AUTHENTICATION_GATEWAYTOKEN_KEY).getValue();
        //test TenantConfiguration with invalid config value, and a valid TenantMetadata - Default DistributionSetType id
        assertBatchConfigurationFails(!oldRolloutApprovalConfig, "invalid-config-value", oldAuthGatewayToken + "randomSuffix0", testValidDistributionSetType);
    }

    @Test
    @Description("The Batch configuration should not be applied, because of invalid TenantMetadata (DefaultDistributionSetType)")
    public void changeBatchConfigurationFail2() throws Exception {
        //in this scenario
        //  all TenantConfiguration have valid and new values - using old values, inverted
        //  TenantMetadata - DefaultDSType ID is invalid
        //in the end batch configuration update must fail, and thus, not a single config should be actually changed.
        boolean oldRolloutApprovalConfig = (Boolean) tenantConfigurationManagement.getConfigurationValue(ROLLOUT_APPROVAL_ENABLED).getValue();
        boolean oldAuthGatewayTokenEnabled = (Boolean) tenantConfigurationManagement.getConfigurationValue(AUTHENTICATION_GATEWAYTOKEN_ENABLED).getValue();
        String oldAuthGatewayToken = (String) tenantConfigurationManagement.getConfigurationValue(AUTHENTICATION_GATEWAYTOKEN_KEY).getValue();

        //invalid TenantMetadata Default DistributionSetType, it is expected to be a number wrapped as String. Testing just any random String
        //not a single configuration should be changed after the failure
        Object     testInvalidDistributionSetType = "someInvalidInput";
        assertBatchConfigurationFails(!oldRolloutApprovalConfig, !oldAuthGatewayTokenEnabled, oldAuthGatewayToken + "randomSuffix1", testInvalidDistributionSetType);

        //invalid TenantMetadata Default DistributionSetType, it is expected to be a number wrapped as String. Testing not a String at all
        //not a single configuration should be changed after the failure
        testInvalidDistributionSetType = true;
        assertBatchConfigurationFails(!oldRolloutApprovalConfig, !oldAuthGatewayTokenEnabled, oldAuthGatewayToken + "randomSuffix2", testInvalidDistributionSetType);
    }

    private void assertBatchConfigurationFails(Object newRolloutApprovalEnabled, Object newAuthGatewayTokenEnabled, Object newGatewayToken, Object newDistributionSetTypeId) throws Exception {
        String oldDefaultDsType = getActualDefaultDsType();
        boolean oldRolloutApprovalConfig = (Boolean) tenantConfigurationManagement.getConfigurationValue(ROLLOUT_APPROVAL_ENABLED).getValue();
        boolean oldAuthGatewayTokenEnabled = (Boolean) tenantConfigurationManagement.getConfigurationValue(AUTHENTICATION_GATEWAYTOKEN_ENABLED).getValue();
        String oldAuthGatewayToken = (String) tenantConfigurationManagement.getConfigurationValue(AUTHENTICATION_GATEWAYTOKEN_KEY).getValue();

        JSONObject configuration = new JSONObject();
        //if new values are not passed.. use old values inverted.. or append random Strings
        configuration.put(ROLLOUT_APPROVAL_ENABLED, newRolloutApprovalEnabled);
        configuration.put(AUTHENTICATION_GATEWAYTOKEN_ENABLED, newAuthGatewayTokenEnabled);
        configuration.put(AUTHENTICATION_GATEWAYTOKEN_KEY, newGatewayToken);
        configuration.put(DEFAULT_DISTRIBUTION_SET_TYPE_KEY, newDistributionSetTypeId);
        String body = configuration.toString();

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs")
                .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
            .andExpect(status().isBadRequest());
        //Check if TenantMetadata and TenantConfiguration is not changed as Batch config failed
        assertEquals(oldDefaultDsType, getActualDefaultDsType(), "Batch configuration update Failed, but TenantMetadata - DistributionSetType was actually changed.");
        assertEquals(oldRolloutApprovalConfig, tenantConfigurationManagement.getConfigurationValue(ROLLOUT_APPROVAL_ENABLED).getValue(), "Batch configuration update Failed, but TenantConfiguration was actually changed.");
        assertEquals(oldAuthGatewayTokenEnabled, tenantConfigurationManagement.getConfigurationValue(AUTHENTICATION_GATEWAYTOKEN_ENABLED).getValue(), "Batch configuration update Failed, but TenantConfiguration was actually changed.");
        assertEquals(oldAuthGatewayToken, tenantConfigurationManagement.getConfigurationValue(AUTHENTICATION_GATEWAYTOKEN_KEY).getValue(), "Batch configuration update Failed, but TenantConfiguration was actually changed.");
    }

    @Test
    @Description("The Batch configuration should be applied")
    public void changeBatchConfiguration() throws Exception {
        String     updatedDistributionSetType = createTestDistributionSetType();
        boolean updatedRolloutApprovalEnabled = true;
        boolean updatedAuthGatewayTokenEnabled = true;
        String updatedAuthGatewayTokenKey = "54321";
        JSONObject configuration = new JSONObject();
        configuration.put(ROLLOUT_APPROVAL_ENABLED, updatedRolloutApprovalEnabled);
        configuration.put(AUTHENTICATION_GATEWAYTOKEN_ENABLED, updatedAuthGatewayTokenEnabled);
        configuration.put(AUTHENTICATION_GATEWAYTOKEN_KEY, updatedAuthGatewayTokenKey);
        configuration.put(DEFAULT_DISTRIBUTION_SET_TYPE_KEY, updatedDistributionSetType);

        String body = configuration.toString();

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs")
                .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
            .andExpect(status().isOk());

        //assert all changes were applied after Rest Success
        assertEquals(updatedDistributionSetType, getActualDefaultDsType(), "Change BatchConfiguration was successful but TenantMetadata - Default DistributionSetType was not actually changed.");
        assertEquals(updatedRolloutApprovalEnabled, tenantConfigurationManagement.getConfigurationValue(ROLLOUT_APPROVAL_ENABLED).getValue(), "Change BatchConfiguration was successful but TenantConfiguration property was not actually changed.");
        assertEquals(updatedAuthGatewayTokenEnabled, tenantConfigurationManagement.getConfigurationValue(AUTHENTICATION_GATEWAYTOKEN_ENABLED).getValue(), "Change BatchConfiguration was successful but TenantConfiguration property was not actually changed.");
        assertEquals(updatedAuthGatewayTokenKey, tenantConfigurationManagement.getConfigurationValue(AUTHENTICATION_GATEWAYTOKEN_KEY).getValue(), "Change BatchConfiguration was successful but TenantConfiguration property was not actually changed.");
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

    @Test
    @Description("Tests DELETE request must Fail for TenantMetadata properties.")
    public void deleteTenantMetadataFail() throws Exception {
        mvc.perform(delete(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}/",
                DEFAULT_DISTRIBUTION_SET_TYPE_KEY))
            .andDo(MockMvcResultPrinter.print())
            .andExpect(status().isBadRequest());
    }

    private String getActualDefaultDsType() {
        return String.valueOf(systemManagement.getTenantMetadata().getDefaultDsType().getId());
    }
}

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

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtTenantManagementRestApi.SYSTEM_V1;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.callAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.getAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_GATEWAY_SECURITY_TOKEN_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.mgmt.json.model.system.MgmtSystemTenantConfigurationValueRequest;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Spring MVC Tests against the MgmtTenantManagementResource.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: AccessContext Management Resource
 */
public class MgmtTenantManagementResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String DEFAULT_DISTRIBUTION_SET_TYPE_KEY = "default.ds.type";

    /**
     * Handles GET request for receiving all tenant specific configurations.
     */
    @Test
    void getTenantConfigurations() throws Exception {
        mvc.perform(get(SYSTEM_V1 + "/configs"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                // check for TenantMetadata additional properties
                .andExpect(jsonPath("$.['" + DEFAULT_DISTRIBUTION_SET_TYPE_KEY + "']").exists())
                .andExpect(jsonPath("$.['" + DEFAULT_DISTRIBUTION_SET_TYPE_KEY + "'].value", equalTo(getActualDefaultDsType().intValue())));
    }

    /**
     * Handles GET request for receiving a tenant specific configuration.
     */
    @Test
    void getTenantConfiguration() throws Exception {
        // test TenantConfiguration property
        mvc.perform(get(SYSTEM_V1 + "/configs/{keyName}", AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Handles GET request for receiving (TenantMetadata - DefaultDsType) a tenant specific configuration.
     */
    @Test
    void getTenantMetadata() throws Exception {
        // test TenantMetadata property
        mvc.perform(get(SYSTEM_V1 + "/configs/{keyName}", DEFAULT_DISTRIBUTION_SET_TYPE_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value", equalTo(getActualDefaultDsType().intValue())));
    }

    /**
     * Handles PUT request for settings values in tenant specific configuration.
     */
    @Test
    void putTenantConfiguration() throws Exception {
        final MgmtSystemTenantConfigurationValueRequest bodyPut = new MgmtSystemTenantConfigurationValueRequest();
        bodyPut.setValue("exampleToken");
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(bodyPut);

        mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
    }

    /**
     * Handles PUT request for settings values (TenantMetadata - DefaultDsType) in tenant specific configuration, which is TenantMetadata
     */
    @Test
    void putTenantMetadata() throws Exception {
        final MgmtSystemTenantConfigurationValueRequest bodyPut = new MgmtSystemTenantConfigurationValueRequest();

        final long updatedTestDefaultDsType = createTestDistributionSetType();
        bodyPut.setValue(updatedTestDefaultDsType);

        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(bodyPut);

        mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", DEFAULT_DISTRIBUTION_SET_TYPE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        //check if after Rest success, value is really changed in TenantMetadata
        assertEquals(updatedTestDefaultDsType, getActualDefaultDsType(),
                "Rest endpoint for updating the Default DistributionSetType completed successfully, but the actual value was not changed.");
    }

    /**
     * Update DefaultDistributionSetType Fails if given DistributionSetType ID does not exist.
     */
    @Test
    void putTenantMetadataFails() throws Exception {
        final long oldDefaultDsType = getActualDefaultDsType();
        // try an invalid input
        String newDefaultDsType = new JSONObject().put("value", true).toString();
        assertDefaultDsTypeUpdateBadRequestFails(newDefaultDsType, oldDefaultDsType, status().isBadRequest());
        // try an invalid input
        newDefaultDsType = new JSONObject().put("value", "someInvalidInput").toString();
        assertDefaultDsTypeUpdateBadRequestFails(newDefaultDsType, oldDefaultDsType, status().isBadRequest());
        // try valid input, but the given DistributionSetType Id does not exist..
        newDefaultDsType = new JSONObject().put("value", 99999).toString();
        assertDefaultDsTypeUpdateBadRequestFails(newDefaultDsType, oldDefaultDsType, status().isNotFound());
    }

    /**
     * The 'multi.assignments.enabled' property must not be changed to false.
     */
    @Test
    void deactivateMultiAssignment() throws Exception {
        final String bodyActivate = new JSONObject().put("value", true).toString();
        final String bodyDeactivate = new JSONObject().put("value", false).toString();

        mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", MULTI_ASSIGNMENTS_ENABLED)
                        .content(bodyActivate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", MULTI_ASSIGNMENTS_ENABLED)
                        .content(bodyDeactivate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());
    }

    /**
     * The Batch configuration should not be applied, because of invalid TenantConfiguration props
     */
    @Test
    void changeBatchConfigurationShouldFailOnInvalidTenantConfiguration() throws Exception {
        // in this scenario
        //   some TenantConfiguration are not valid,
        //   TenantMetadata - DefaultDSType ID is valid,
        // in the end batch configuration update must fail, and thus, not a single config should be actually changed
        long testValidDistributionSetType = createTestDistributionSetType();
        boolean oldRolloutApprovalConfig = (Boolean) tenantConfigurationManagement().getConfigurationValue(ROLLOUT_APPROVAL_ENABLED).getValue();
        String oldAuthGatewayToken = (String) tenantConfigurationManagement().getConfigurationValue(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY)
                .getValue();
        // test TenantConfiguration with invalid config value, and a valid TenantMetadata - Default DistributionSetType id
        assertBatchConfigurationFails(!oldRolloutApprovalConfig, "invalid-config-value", oldAuthGatewayToken + "randomSuffix0",
                testValidDistributionSetType, status().isBadRequest());
    }

    /**
     * The Batch configuration should not be applied, because of invalid TenantMetadata (DefaultDistributionSetType)
     */
    @Test
    void changeBatchConfigurationShouldOnInvalidTenantMetadata() throws Exception {
        // in this scenario
        //   all TenantConfiguration have valid and new values - using old values, inverted
        //   TenantMetadata - DefaultDSType ID is invalid
        // in the end batch configuration update must fail, and thus, not a single config should be actually changed.
        final boolean oldRolloutApprovalConfig = (Boolean) tenantConfigurationManagement()
                .getConfigurationValue(ROLLOUT_APPROVAL_ENABLED).getValue();
        final boolean oldAuthGatewayTokenEnabled = (Boolean) tenantConfigurationManagement()
                .getConfigurationValue(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_ENABLED).getValue();
        final String oldAuthGatewayToken = (String) tenantConfigurationManagement()
                .getConfigurationValue(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY).getValue();

        // invalid TenantMetadata Default DistributionSetType, it is expected to be a number. Testing invalid type - string
        // not a single configuration should be changed after the failure
        Object testInvalidDistributionSetType = "someInvalidInput";
        assertBatchConfigurationFails(!oldRolloutApprovalConfig, !oldAuthGatewayTokenEnabled, oldAuthGatewayToken + "randomSuffix1",
                testInvalidDistributionSetType, status().isBadRequest());

        // invalid TenantMetadata Default DistributionSetType, it is expected to be a number. Testing invalid type - bool
        // not a single configuration should be changed after the failure
        testInvalidDistributionSetType = true;
        assertBatchConfigurationFails(!oldRolloutApprovalConfig, !oldAuthGatewayTokenEnabled, oldAuthGatewayToken + "randomSuffix2",
                testInvalidDistributionSetType, status().isBadRequest());

        // valid TenantMetadata Default DistributionSetType, it is expected to be a number. Testing valid type - but given DistributionSetType Id does not exist.
        // not a single configuration should be changed after the failure
        testInvalidDistributionSetType = 9999;
        assertBatchConfigurationFails(!oldRolloutApprovalConfig, !oldAuthGatewayTokenEnabled, oldAuthGatewayToken + "randomSuffix2",
                testInvalidDistributionSetType, status().isNotFound());
    }

    /**
     * The Batch configuration should be applied
     */
    @Test
    void changeBatchConfiguration() throws Exception {
        final long updatedDistributionSetType = createTestDistributionSetType();
        final boolean updatedRolloutApprovalEnabled = true;
        final boolean updatedAuthGatewayTokenEnabled = true;
        final String updatedAuthGatewayTokenKey = "54321";
        final JSONObject configuration = new JSONObject();
        configuration.put(ROLLOUT_APPROVAL_ENABLED, updatedRolloutApprovalEnabled);
        configuration.put(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_ENABLED, updatedAuthGatewayTokenEnabled);
        configuration.put(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY, updatedAuthGatewayTokenKey);
        configuration.put(DEFAULT_DISTRIBUTION_SET_TYPE_KEY, updatedDistributionSetType);

        final String body = configuration.toString();

        mvc.perform(put(SYSTEM_V1 + "/configs").content(body).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        // assert all changes were applied after Rest Success
        assertEquals(updatedDistributionSetType, getActualDefaultDsType(),
                "Change BatchConfiguration was successful but TenantMetadata - Default DistributionSetType was not actually changed.");
        assertEquals(updatedRolloutApprovalEnabled, tenantConfigurationManagement().getConfigurationValue(ROLLOUT_APPROVAL_ENABLED).getValue(),
                "Change BatchConfiguration was successful but TenantConfiguration property was not actually changed.");
        assertEquals(updatedAuthGatewayTokenEnabled,
                tenantConfigurationManagement().getConfigurationValue(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_ENABLED).getValue(),
                "Change BatchConfiguration was successful but TenantConfiguration property was not actually changed.");
        assertEquals(updatedAuthGatewayTokenKey,
                tenantConfigurationManagement().getConfigurationValue(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY).getValue(),
                "Change BatchConfiguration was successful but TenantConfiguration property was not actually changed.");
    }

    /**
     * The 'repository.actions.autoclose.enabled' property must not be modified if Multi-Assignments is enabled.
     */
    @Test
    void autoCloseCannotBeModifiedIfMultiAssignmentIsEnabled() throws Exception {
        final String bodyActivate = new JSONObject().put("value", true).toString();
        final String bodyDeactivate = new JSONObject().put("value", false).toString();

        // enable Multi-Assignments
        mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", MULTI_ASSIGNMENTS_ENABLED)
                        .content(bodyActivate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        // try to enable Auto-Close
        mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED)
                        .content(bodyActivate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        // try to disable Auto-Close
        mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED)
                        .content(bodyDeactivate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());
    }

    /**
     * Handles DELETE request deleting a tenant specific configuration.
     */
    @Test
    void deleteTenantConfiguration() throws Exception {
        mvc.perform(delete(SYSTEM_V1 + "/configs/{keyName}", AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
    }

    /**
     * Tests DELETE request must Fail for TenantMetadata properties.
     */
    @Test
    void deleteTenantMetadataFail() throws Exception {
        mvc.perform(delete(SYSTEM_V1 + "/configs/{keyName}", DEFAULT_DISTRIBUTION_SET_TYPE_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Handles GET request for receiving all tenant specific configurations depending on read gateway token permissions.
     */
    @Test
    void getTenantConfigurationReadGWToken() throws Exception {
        getAs(withUser("tenant_admin", SpPermission.TENANT_CONFIGURATION), () -> {
            tenantConfigurationManagement().addOrUpdateConfiguration(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY, "123");
            return null;
        });

        // TODO - should be able to read with TENANT_CONFIGURATION but somehow here the role hierarchy doesn't play
        // checked in mgmt / update server runtime PreAuthorizeEnabledTest
        callAs(withUser("tenant_admin", SpPermission.READ_TENANT_CONFIGURATION, SpPermission.READ_GATEWAY_SECURITY_TOKEN), () -> {
            mvc.perform(get(SYSTEM_V1 + "/configs"))
                    .andDo(MockMvcResultPrinter.print())
                    .andDo(m -> System.out.println("-> 1: " + m.getResponse().getContentAsString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.['" + AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY + "']").exists())
                    .andExpect(jsonPath("$.['" + AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY + "'].value", equalTo("123")));
            return null;
        });

        callAs(withUser("tenant_read", SpPermission.READ_TENANT_CONFIGURATION), () -> {
            mvc.perform(get(SYSTEM_V1 + "/configs"))
                    .andDo(MockMvcResultPrinter.print())
                    .andDo(m -> System.out.println("-> 2: " + m.getResponse().getContentAsString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.['" + AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY + "']").doesNotExist());
            return null;
        });
    }

    private Long createTestDistributionSetType() {
        final DistributionSetType testDefaultDsType = distributionSetTypeManagement.create(DistributionSetTypeManagement.Create.builder()
                .key("test123").name("TestName123").description("TestDefaultDsType").build());
        return distributionSetTypeManagement
                .update(DistributionSetTypeManagement.Update.builder().id(testDefaultDsType.getId()).description("TestDefaultDsType").build())
                .getId();
    }

    private void assertDefaultDsTypeUpdateBadRequestFails(
            final String newDefaultDsType, final long oldDefaultDsType, final ResultMatcher resultMatchers)
            throws Exception {
        mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", DEFAULT_DISTRIBUTION_SET_TYPE_KEY)
                        .content(newDefaultDsType)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(resultMatchers);
        assertEquals(oldDefaultDsType, getActualDefaultDsType(),
                "Rest endpoint for updating DefaultDistributionType failed, but actual value changed unexpectedly.");
    }

    private void assertBatchConfigurationFails(
            final Object newRolloutApprovalEnabled, final Object newAuthGatewayTokenEnabled, final Object newGatewayToken,
            final Object newDistributionSetTypeId, final ResultMatcher resultMatchers) throws Exception {
        long oldDefaultDsType = getActualDefaultDsType();
        boolean oldRolloutApprovalConfig = (Boolean) tenantConfigurationManagement().getConfigurationValue(ROLLOUT_APPROVAL_ENABLED).getValue();
        boolean oldAuthGatewayTokenEnabled = (Boolean) tenantConfigurationManagement()
                .getConfigurationValue(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_ENABLED)
                .getValue();
        String oldAuthGatewayToken = (String) tenantConfigurationManagement().getConfigurationValue(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY)
                .getValue();

        final JSONObject configuration = new JSONObject();
        configuration.put(ROLLOUT_APPROVAL_ENABLED, newRolloutApprovalEnabled);
        configuration.put(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_ENABLED, newAuthGatewayTokenEnabled);
        configuration.put(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY, newGatewayToken);
        configuration.put(DEFAULT_DISTRIBUTION_SET_TYPE_KEY, newDistributionSetTypeId);
        String body = configuration.toString();

        mvc.perform(put(SYSTEM_V1 + "/configs").content(body).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(resultMatchers);
        //Check if TenantMetadata and TenantConfiguration is not changed as Batch config failed
        assertEquals(oldDefaultDsType, getActualDefaultDsType(),
                "Batch configuration update Failed, but TenantMetadata - DistributionSetType was actually changed.");
        assertEquals(oldRolloutApprovalConfig, tenantConfigurationManagement().getConfigurationValue(ROLLOUT_APPROVAL_ENABLED).getValue(),
                "Batch configuration update Failed, but TenantConfiguration was actually changed.");
        assertEquals(oldAuthGatewayTokenEnabled,
                tenantConfigurationManagement().getConfigurationValue(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_ENABLED).getValue(),
                "Batch configuration update Failed, but TenantConfiguration was actually changed.");
        assertEquals(oldAuthGatewayToken,
                tenantConfigurationManagement().getConfigurationValue(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY).getValue(),
                "Batch configuration update Failed, but TenantConfiguration was actually changed.");
    }

    private Long getActualDefaultDsType() {
        return systemManagement.getTenantMetadata().getDefaultDsType().getId();
    }
}
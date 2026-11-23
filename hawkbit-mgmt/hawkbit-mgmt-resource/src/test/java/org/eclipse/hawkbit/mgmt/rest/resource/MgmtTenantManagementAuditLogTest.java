/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtTenantManagementRestApi.SYSTEM_V1;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ROLLOUT_APPROVAL_ENABLED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.hawkbit.audit.AuditLoggingAspect;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

/**
 * Audit logging of the MgmtTenantManagementResource. Separate class since the audit aspect (registered by the security
 * auto-configuration) is not part of the common management API test context.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Tenant configuration audit log
 */
@Import(AuditLoggingAspect.class)
@ExtendWith(OutputCaptureExtension.class)
class MgmtTenantManagementAuditLogTest extends AbstractManagementApiIntegrationTest {

    private static final String AUDIT_LOGGER = "AUDIT.TENANTCONFIGURATION";

    /**
     * The gateway token value shall not be written to the audit log - neither by single value nor by batch update.
     */
    @Test
    void gatewayTokenIsMaskedInAuditLog(final CapturedOutput output) throws Exception {
        final String token = "auditMaskedGatewayToken";
        // tests run with root level WARN, audit is logged with INFO
        final LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
        loggingSystem.setLogLevel(AUDIT_LOGGER, LogLevel.INFO);
        try {
            mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new JSONObject().put("value", token + "1").toString()))
                    .andExpect(status().isNoContent());
            mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", ROLLOUT_APPROVAL_ENABLED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new JSONObject().put("value", true).toString()))
                    .andExpect(status().isNoContent());
            mvc.perform(put(SYSTEM_V1 + "/configs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new JSONObject()
                                    .put(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY, token + "2")
                                    .put(ROLLOUT_APPROVAL_ENABLED, true).toString()))
                    .andExpect(status().isNoContent());
        } finally {
            loggingSystem.setLogLevel(AUDIT_LOGGER, null);
        }

        assertThat(output.toString().lines().filter(line -> line.contains("Method: updateTenantConfiguration")))
                .hasSize(3)
                .noneMatch(line -> line.contains(token))
                // single value - masked only for the gateway token key
                .anyMatch(line -> line.contains("[" + AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY + ", ***]"))
                .anyMatch(line -> line.contains("[" + ROLLOUT_APPROVAL_ENABLED + ", MgmtSystemTenantConfigurationValueRequest(value=true)]"))
                // batch - only the gateway token entry is masked
                .anyMatch(line -> line.contains(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY + "=***")
                        && line.contains(ROLLOUT_APPROVAL_ENABLED + "=true"));
    }

    /**
     * A not convertible gateway token value shall not be written to the audit log via the error message of a failed update.
     */
    @Test
    void notConvertibleGatewayTokenIsMaskedInAuditLog(final CapturedOutput output) throws Exception {
        final String token = "auditMaskedNotConvertibleGatewayToken";
        // object (map) values can't be converted to the String gateway token
        final JSONObject notConvertible = new JSONObject().put("token", token);
        mvc.perform(put(SYSTEM_V1 + "/configs/{keyName}", AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new JSONObject().put("value", notConvertible).toString()))
                .andExpect(status().is4xxClientError());
        mvc.perform(put(SYSTEM_V1 + "/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new JSONObject().put(AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY, notConvertible).toString()))
                .andExpect(status().is4xxClientError());

        // failures are audit logged with ERROR, so no log level change is needed
        assertThat(output.toString().lines().filter(line -> line.contains("Method: updateTenantConfiguration")))
                .hasSize(2)
                .noneMatch(line -> line.contains(token))
                .allMatch(line -> line.contains("Cannot convert the value ***"));
    }
}

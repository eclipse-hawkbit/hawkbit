/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.mgmt;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTenantManagementRestApi;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.util.ObjectUtils;

/**
 * Management Api Interface
 */
@Slf4j
@AllArgsConstructor
public class MgmtApi {

    private static final String AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY = "authentication.gatewaytoken.key";
    private static final String AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED = "authentication.gatewaytoken.enabled";
    private static final String AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED = "authentication.targettoken.enabled";
    private static final Random RND = new SecureRandom();
    @NonNull
    private final Tenant tenant;
    @NonNull
    private final HawkbitClient hawkbitClient;

    public static String randomToken() {
        final byte[] rnd = new byte[24];
        RND.nextBytes(rnd);
        return Base64.getEncoder().encodeToString(rnd);
    }

    // if gateway toke is configured then the gateway auth is enabled key is set
    // so all devices use gateway token authentication
    // otherwise target token authentication is enabled. Then all devices shall be registerd
    // and the target token shall be set to the one from the DDI controller instance
    public void setupTargetAuthentication() {
        final MgmtTenantManagementRestApi mgmtTenantManagementRestApi =
                hawkbitClient.mgmtService(MgmtTenantManagementRestApi.class, tenant);
        final String gatewayToken = tenant.getGatewayToken();
        if (ObjectUtils.isEmpty(gatewayToken)) {
            if (!((Boolean) Objects.requireNonNull(mgmtTenantManagementRestApi
                    .getTenantConfigurationValue(AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED)
                    .getBody()).getValue())) {
                mgmtTenantManagementRestApi.updateTenantConfiguration(
                        Map.of(AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED, true)
                );
            }
        } else {
            if (!((Boolean) Objects.requireNonNull(mgmtTenantManagementRestApi
                    .getTenantConfigurationValue(AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED)
                    .getBody()).getValue())) {
                mgmtTenantManagementRestApi.updateTenantConfiguration(
                        Map.of(AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED, true)
                );
            }
            if (!gatewayToken.equals(
                    Objects.requireNonNull(mgmtTenantManagementRestApi
                            .getTenantConfigurationValue(AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY)
                            .getBody()).getValue())) {
                mgmtTenantManagementRestApi.updateTenantConfiguration(
                        Map.of(AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, gatewayToken)
                );
            }
        }
    }

    // returns target token
    public String setupTargetToken(final String controllerId, String securityTargetToken) {
        if (ObjectUtils.isEmpty(tenant.getGatewayToken())) {
            final MgmtTargetRestApi mgmtTargetRestApi = hawkbitClient.mgmtService(MgmtTargetRestApi.class, tenant);
            try {
                // test if target exist, if not - throws 404
                final MgmtTarget target = Objects.requireNonNull(mgmtTargetRestApi.getTarget(controllerId).getBody());
                if (ObjectUtils.isEmpty(securityTargetToken)) {
                    if (ObjectUtils.isEmpty(target.getSecurityToken())) {
                        // generate random to set to tha existing target without configured security token
                        securityTargetToken = randomToken();
                        mgmtTargetRestApi.updateTarget(controllerId,
                                new MgmtTargetRequestBody().setSecurityToken(securityTargetToken));
                    } else {
                        securityTargetToken = target.getSecurityToken();
                    }
                } else if (!securityTargetToken.equals(target.getSecurityToken())) {
                    // update target's with the security token (since it doesn't match)
                    mgmtTargetRestApi.updateTarget(controllerId,
                            new MgmtTargetRequestBody().setSecurityToken(securityTargetToken));
                }
            } catch (final FeignException.NotFound e) {
                if (ObjectUtils.isEmpty(securityTargetToken)) {
                    securityTargetToken = randomToken();
                }
                // create target with the security token
                mgmtTargetRestApi.createTargets(List.of(
                        new MgmtTargetRequestBody()
                                .setControllerId(controllerId)
                                .setSecurityToken(securityTargetToken)));
            }
        }

        return securityTargetToken;
    }

    public void deleteController(final String controllerId) {
        hawkbitClient.mgmtService(MgmtTargetRestApi.class, tenant).deleteTarget(controllerId);
    }
}
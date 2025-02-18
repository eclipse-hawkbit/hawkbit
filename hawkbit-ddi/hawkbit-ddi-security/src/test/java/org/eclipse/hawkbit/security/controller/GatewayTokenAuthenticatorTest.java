/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.security.SecurityContextSerializer;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature("Unit Tests - Security")
@Story("Gateway token authentication")
@ExtendWith(MockitoExtension.class)
class GatewayTokenAuthenticatorTest {

    private static final String CONTROLLER_ID = "controllerId_gwtoken";
    private static final String GATEWAY_TOKEN = "test-gw-token";
    private static final String UNKNOWN_TOKEN = "unknown";

    private static final TenantConfigurationValue<String> CONFIG_VALUE_GW_TOKEN = TenantConfigurationValue
            .<String> builder().value(GATEWAY_TOKEN).build();
    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_ENABLED = TenantConfigurationValue
            .<Boolean> builder().value(true).build();
    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_DISABLED = TenantConfigurationValue
            .<Boolean> builder().value(false).build();

    private Authenticator authenticator;

    @Mock
    private TenantConfigurationManagement tenantConfigurationManagementMock;
    @Mock
    private UserAuthoritiesResolver authoritiesResolver;
    @Mock
    private SecurityContextSerializer securityContextSerializer;

    @BeforeEach
    void before() {
        final SecurityContextTenantAware tenantAware = new SecurityContextTenantAware(authoritiesResolver, securityContextSerializer);
        authenticator = new GatewayTokenAuthenticator(
                tenantConfigurationManagementMock, tenantAware,
                new SystemSecurityContext(tenantAware));
    }

    @Test
    @Description("Tests successful authentication with gateway token")
    void testWithGwToken() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(GATEWAY_TOKEN);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class))
                .thenReturn(CONFIG_VALUE_GW_TOKEN);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_ENABLED);

        assertThat(authenticator.authenticate(securityToken))
                .isNotNull()
                .hasFieldOrPropertyWithValue("principal", CONTROLLER_ID);
    }

    @Test
    @Description("Tests that if gateway token doesn't match, the authentication fails")
    void testWithBadGwToken() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(UNKNOWN_TOKEN);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY, String.class))
                .thenReturn(CONFIG_VALUE_GW_TOKEN);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_ENABLED);

        assertThat(authenticator.authenticate(securityToken)).isNull();
    }

    @Test
    @Description("Tests that if gateway token miss, the authentication fails")
    void testWithoutGwToken() {
        assertThat(authenticator.authenticate(new ControllerSecurityToken("DEFAULT", CONTROLLER_ID))).isNull();
    }

    @Test
    @Description("Tests that if disabled, the authentication fails")
    void testWithGwTokenButDisabled() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(GATEWAY_TOKEN);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_DISABLED);

        assertThat(authenticator.authenticate(securityToken)).isNull();
    }

    private static ControllerSecurityToken prepareSecurityToken(final String gwToken) {
        final ControllerSecurityToken securityToken = new ControllerSecurityToken("DEFAULT", CONTROLLER_ID);
        securityToken.putHeader(ControllerSecurityToken.AUTHORIZATION_HEADER, GatewayTokenAuthenticator.GATEWAY_SECURITY_TOKEN_AUTH_SCHEME + gwToken);
        return securityToken;
    }
}
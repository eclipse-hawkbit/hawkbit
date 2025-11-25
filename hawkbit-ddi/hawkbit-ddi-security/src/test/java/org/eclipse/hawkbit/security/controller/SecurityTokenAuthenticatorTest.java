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
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_TARGET_SECURITY_TOKEN_ENABLED;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Gateway token auth
 */
@ExtendWith(MockitoExtension.class)
class SecurityTokenAuthenticatorTest {

    private static final String CONTROLLER_ID = "controllerId_token";
    private static final String SECURITY_TOKEN = "test-sec-token";
    private static final String UNKNOWN_TOKEN = "unknown";

    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_ENABLED = TenantConfigurationValue
            .<Boolean> builder().value(true).build();
    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_DISABLED = TenantConfigurationValue
            .<Boolean> builder().value(false).build();

    private Authenticator authenticator;

    @Mock
    private TenantConfigurationManagement tenantConfigurationManagementMock;
    @Mock
    private ControllerManagement controllerManagementMock;

    @BeforeEach
    void before() {
        TenantConfigHelper.setTenantConfigurationManagement(tenantConfigurationManagementMock);
        authenticator = new SecurityTokenAuthenticator(controllerManagementMock);
    }

    /**
     * Tests successful auth with gateway token
     */
    @Test
    void testWithSecToken() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(SECURITY_TOKEN);
        when(tenantConfigurationManagementMock.getConfigurationValue(AUTHENTICATION_TARGET_SECURITY_TOKEN_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_ENABLED);

        final Target target = Mockito.mock(Target.class);
        when(target.getControllerId()).thenReturn(CONTROLLER_ID);
        when(target.getSecurityToken()).thenReturn(SECURITY_TOKEN);
        when(controllerManagementMock.findByControllerId(CONTROLLER_ID)).thenReturn(Optional.of(target));

        assertThat(authenticator.authenticate(securityToken))
                .isNotNull()
                .hasFieldOrPropertyWithValue("principal", CONTROLLER_ID);
    }

    /**
     * Tests that if gateway token doesn't match, the auth fails
     */
    @Test
    void testWithBadSecToken() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(UNKNOWN_TOKEN);
        when(tenantConfigurationManagementMock.getConfigurationValue(AUTHENTICATION_TARGET_SECURITY_TOKEN_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_ENABLED);

        assertThat(authenticator.authenticate(securityToken)).isNull();
    }

    /**
     * Tests that if gateway token miss, the auth fails
     */
    @Test
    void testWithoutSecToken() {
        assertThat(authenticator.authenticate(new ControllerSecurityToken("DEFAULT", CONTROLLER_ID))).isNull();
    }

    /**
     * Tests that if disabled, the auth fails
     */
    @Test
    void testWithSecTokenButDisabled() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(SECURITY_TOKEN);
        when(tenantConfigurationManagementMock.getConfigurationValue(AUTHENTICATION_TARGET_SECURITY_TOKEN_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_DISABLED);

        assertThat(authenticator.authenticate(securityToken)).isNull();
    }

    private static ControllerSecurityToken prepareSecurityToken(final String secToken) {
        final ControllerSecurityToken securityToken = new ControllerSecurityToken("DEFAULT", CONTROLLER_ID);
        securityToken.putHeader(ControllerSecurityToken.AUTHORIZATION_HEADER, SecurityTokenAuthenticator.TARGET_SECURITY_TOKEN_AUTH_SCHEME + secToken);
        return securityToken;
    }
}
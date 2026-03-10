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
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_HEADER_AUTHORITY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_HEADER_ENABLED;
import static org.mockito.Mockito.when;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Security header authenticator
 */
@ExtendWith(MockitoExtension.class)
class SecurityHeaderAuthenticatorTest {

    private static final String CA_COMMON_NAME = "ca-cn";
    private static final String CA_COMMON_NAME_VALUE = "box1";

    private static final String X_AUTHORITY_1 = "X-Authority-1";

    private static final String SINGLE_AUTHORITY = "hash1";
    private static final String SECOND_AUTHORITY = "hash2";
    private static final String THIRD_AUTHORITY = "hash3";
    private static final String UNKNOWN_AUTHORITY = "unknown";

    private static final String MULTI_AUTHORITY = "HASH1;hash2,HASH3,HASH1";

    private static final TenantConfigurationValue<String> CONFIG_VALUE_SINGLE_AUTHORITY = TenantConfigurationValue.<String> builder()
            .value(SINGLE_AUTHORITY).build();
    private static final TenantConfigurationValue<String> CONFIG_VALUE_MULTI_AUTHORITY = TenantConfigurationValue.<String> builder()
            .value(MULTI_AUTHORITY).build();
    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_ENABLED = TenantConfigurationValue.<Boolean> builder()
            .value(true).build();
    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_DISABLED = TenantConfigurationValue.<Boolean> builder()
            .value(false).build();

    private Authenticator authenticator;

    @Mock
    private TenantConfigurationManagement tenantConfigurationManagementMock;

    @BeforeEach
    void before() {
        TenantConfigHelper.setTenantConfigurationManagement(tenantConfigurationManagementMock);
        final DdiSecurityProperties.Rp rp = new DdiSecurityProperties.Rp();
        rp.setControllerIdHeader(CA_COMMON_NAME);
        authenticator = new SecurityHeaderAuthenticator(rp);
    }

    /**
     * Tests successful authentication with multiple a single hashes
     */
    @Test
    void testWithSingleTrustedAuthority() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(SINGLE_AUTHORITY);
        when(tenantConfigurationManagementMock.getConfigurationValue(AUTHENTICATION_HEADER_AUTHORITY, String.class))
                .thenReturn(CONFIG_VALUE_SINGLE_AUTHORITY);
        when(tenantConfigurationManagementMock.getConfigurationValue(AUTHENTICATION_HEADER_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_ENABLED);

        assertThat(authenticator.authenticate(securityToken))
                .isNotNull()
                .hasFieldOrPropertyWithValue("principal", CA_COMMON_NAME_VALUE);
    }

    /**
     * Tests successful authentication with multiple hashes
     */
    @Test
    void testWithMultipleTrustedAuthority() {
        when(tenantConfigurationManagementMock.getConfigurationValue(AUTHENTICATION_HEADER_AUTHORITY, String.class))
                .thenReturn(CONFIG_VALUE_MULTI_AUTHORITY);
        when(tenantConfigurationManagementMock.getConfigurationValue(AUTHENTICATION_HEADER_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_ENABLED);

        assertThat(authenticator.authenticate(prepareSecurityToken(SINGLE_AUTHORITY)))
                .isNotNull()
                .hasFieldOrPropertyWithValue("principal", CA_COMMON_NAME_VALUE);
        assertThat(authenticator.authenticate(prepareSecurityToken(SECOND_AUTHORITY)))
                .isNotNull()
                .hasFieldOrPropertyWithValue("principal", CA_COMMON_NAME_VALUE);
        assertThat(authenticator.authenticate(prepareSecurityToken(THIRD_AUTHORITY)))
                .isNotNull()
                .hasFieldOrPropertyWithValue("principal", CA_COMMON_NAME_VALUE);
    }

    /**
     * Tests that if the hash is unknown, the authentication fails
     */
    @Test
    void testWithUnTrustedAuthority() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(UNKNOWN_AUTHORITY);
        when(tenantConfigurationManagementMock.getConfigurationValue(AUTHENTICATION_HEADER_AUTHORITY, String.class))
                .thenReturn(CONFIG_VALUE_MULTI_AUTHORITY);
        when(tenantConfigurationManagementMock.getConfigurationValue(AUTHENTICATION_HEADER_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_ENABLED);

        assertThat(authenticator.authenticate(securityToken)).isNull();
    }

    /**
     * Tests that if CN doesn't match the CN in the security token, the authentication fails
     */
    @Test
    void testWithNonMatchingCN() {
        final ControllerSecurityToken securityToken = new ControllerSecurityToken("DEFAULT", "otherControllerID");
        securityToken.putHeader(CA_COMMON_NAME, CA_COMMON_NAME_VALUE);
        securityToken.putHeader(X_AUTHORITY_1, SINGLE_AUTHORITY);

        assertThat(authenticator.authenticate(securityToken)).isNull();
    }

    /**
     * Tests that if the hash miss, the authentication fails
     */
    @Test
    void testWithoutHash() {
        assertThat(authenticator.authenticate(new ControllerSecurityToken("DEFAULT", CA_COMMON_NAME_VALUE))).isNull();
    }

    /**
     * Tests that if disabled, the authentication fails
     */
    @Test
    void testWithSingleTrustedAuthorityButDisabled() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(SINGLE_AUTHORITY);
        when(tenantConfigurationManagementMock.getConfigurationValue(AUTHENTICATION_HEADER_ENABLED, Boolean.class)).thenReturn(CONFIG_VALUE_DISABLED);

        assertThat(authenticator.authenticate(securityToken)).isNull();
    }

    private static ControllerSecurityToken prepareSecurityToken(final String issuerHashHeaderValue) {
        final ControllerSecurityToken securityToken = new ControllerSecurityToken("DEFAULT", CA_COMMON_NAME_VALUE);
        securityToken.putHeader(CA_COMMON_NAME, CA_COMMON_NAME_VALUE);
        securityToken.putHeader(X_AUTHORITY_1, issuerHashHeaderValue);
        return securityToken;
    }
}
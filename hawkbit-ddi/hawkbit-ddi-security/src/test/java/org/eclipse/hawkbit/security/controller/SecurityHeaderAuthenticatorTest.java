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
@Story("Security header authenticator")
@ExtendWith(MockitoExtension.class)
class SecurityHeaderAuthenticatorTest {

    private static final String CA_COMMON_NAME = "ca-cn";
    private static final String CA_COMMON_NAME_VALUE = "box1";

    private static final String X_SSL_ISSUER_HASH_1 = "X-Ssl-Issuer-Hash-1";

    private static final String SINGLE_HASH = "hash1";
    private static final String SECOND_HASH = "hash2";
    private static final String THIRD_HASH = "hash3";
    private static final String UNKNOWN_HASH = "unknown";

    private static final String MULTI_HASH = "HASH1;hash2,HASH3,HASH1";

    private static final TenantConfigurationValue<String> CONFIG_VALUE_SINGLE_HASH = TenantConfigurationValue
            .<String> builder().value(SINGLE_HASH).build();
    private static final TenantConfigurationValue<String> CONFIG_VALUE_MULTI_HASH = TenantConfigurationValue
            .<String> builder().value(MULTI_HASH).build();
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
        authenticator = new SecurityHeaderAuthenticator(
                tenantConfigurationManagementMock, tenantAware,
                new SystemSecurityContext(tenantAware), CA_COMMON_NAME, "X-Ssl-Issuer-Hash-%d"
        );
    }

    @Test
    @Description("Tests successful authentication with multiple a single hashes")
    void testWithSingleKnownHash() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(SINGLE_HASH);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, String.class))
                .thenReturn(CONFIG_VALUE_SINGLE_HASH);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_ENABLED);

        assertThat(authenticator.authenticate(securityToken))
                .isNotNull()
                .hasFieldOrPropertyWithValue("principal", CA_COMMON_NAME_VALUE);
    }

    @Test
    @Description("Tests successful authentication with multiple hashes")
    void testWithMultipleKnownHashes() {
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, String.class))
                .thenReturn(CONFIG_VALUE_MULTI_HASH);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_ENABLED);

        assertThat(authenticator.authenticate(prepareSecurityToken(SINGLE_HASH)))
                .isNotNull()
                .hasFieldOrPropertyWithValue("principal", CA_COMMON_NAME_VALUE);
        assertThat(authenticator.authenticate(prepareSecurityToken(SECOND_HASH)))
                .isNotNull()
                .hasFieldOrPropertyWithValue("principal", CA_COMMON_NAME_VALUE);
        assertThat(authenticator.authenticate(prepareSecurityToken(THIRD_HASH)))
                .isNotNull()
                .hasFieldOrPropertyWithValue("principal", CA_COMMON_NAME_VALUE);
    }

    @Test
    @Description("Tests that if the hash is unknown, the authentication fails")
    void testWithUnknownHash() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(UNKNOWN_HASH);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, String.class))
                .thenReturn(CONFIG_VALUE_MULTI_HASH);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_ENABLED);

        assertThat(authenticator.authenticate(securityToken)).isNull();
    }

    @Test
    @Description("Tests that if CN doesn't match the CN in the security token, the authentication fails")
    void testWithNonMatchingCN() {
        final ControllerSecurityToken securityToken = new ControllerSecurityToken("DEFAULT", "otherControllerID");
        securityToken.putHeader(CA_COMMON_NAME, CA_COMMON_NAME_VALUE);
        securityToken.putHeader(X_SSL_ISSUER_HASH_1, SINGLE_HASH);

        assertThat(authenticator.authenticate(securityToken)).isNull();
    }

    @Test
    @Description("Tests that if the hash miss, the authentication fails")
    void testWithoutHash() {
        assertThat(authenticator.authenticate(new ControllerSecurityToken("DEFAULT", CA_COMMON_NAME_VALUE))).isNull();
    }

    @Test
    @Description("Tests that if disabled, the authentication fails")
    void testWithSingleKnownHashButDisabled() {
        final ControllerSecurityToken securityToken = prepareSecurityToken(SINGLE_HASH);
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_ENABLED, Boolean.class))
                .thenReturn(CONFIG_VALUE_DISABLED);

        assertThat(authenticator.authenticate(securityToken)).isNull();
    }

    private static ControllerSecurityToken prepareSecurityToken(final String issuerHashHeaderValue) {
        final ControllerSecurityToken securityToken = new ControllerSecurityToken("DEFAULT", CA_COMMON_NAME_VALUE);
        securityToken.putHeader(CA_COMMON_NAME, CA_COMMON_NAME_VALUE);
        securityToken.putHeader(X_SSL_ISSUER_HASH_1, issuerHashHeaderValue);
        return securityToken;
    }
}
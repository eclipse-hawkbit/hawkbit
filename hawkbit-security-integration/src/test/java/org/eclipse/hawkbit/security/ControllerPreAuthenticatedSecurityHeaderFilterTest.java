/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collection;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Feature("Unit Tests - Security")
@Story("Issuer hash based authentication")
@ExtendWith(MockitoExtension.class)
public class ControllerPreAuthenticatedSecurityHeaderFilterTest {

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

    private ControllerPreAuthenticatedSecurityHeaderFilter underTest;

    @Mock
    private TenantConfigurationManagement tenantConfigurationManagementMock;
    @Mock
    private UserAuthoritiesResolver authoritiesResolver;
    @Mock
    private SecurityContextSerializer securityContextSerializer;

    @BeforeEach
    public void before() {
        final SecurityContextTenantAware tenantAware = new SecurityContextTenantAware(authoritiesResolver, securityContextSerializer);
        underTest = new ControllerPreAuthenticatedSecurityHeaderFilter(CA_COMMON_NAME, "X-Ssl-Issuer-Hash-%d",
                tenantConfigurationManagementMock, tenantAware, new SystemSecurityContext(tenantAware));
    }

    @Test
    @Description("Tests the filter for issuer hash based authentication with a single known hash")
    public void testIssuerHashBasedAuthenticationWithSingleKnownHash() {
        final DmfTenantSecurityToken securityToken = prepareSecurityToken(SINGLE_HASH);
        // use single known hash
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, String.class))
                .thenReturn(CONFIG_VALUE_SINGLE_HASH);
        assertThat(underTest.getPreAuthenticatedPrincipal(securityToken)).isNotNull();
    }

    @Test
    @Description("Tests the filter for issuer hash based authentication with multiple known hashes")
    public void testIssuerHashBasedAuthenticationWithMultipleKnownHashes() {
        // use multiple known hashes
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, String.class))
                .thenReturn(CONFIG_VALUE_MULTI_HASH);
        assertThat(underTest.getPreAuthenticatedPrincipal(prepareSecurityToken(SINGLE_HASH))).isNotNull();
        assertThat(underTest.getPreAuthenticatedPrincipal(prepareSecurityToken(SECOND_HASH))).isNotNull();
        assertThat(underTest.getPreAuthenticatedPrincipal(prepareSecurityToken(THIRD_HASH))).isNotNull();
    }

    @Test
    @Description("Tests the filter for issuer hash based authentication with unknown hash")
    public void testIssuerHashBasedAuthenticationWithUnknownHash() {
        final DmfTenantSecurityToken securityToken = prepareSecurityToken(UNKNOWN_HASH);
        // use single known hash
        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, String.class))
                .thenReturn(CONFIG_VALUE_MULTI_HASH);
        assertThat(underTest.getPreAuthenticatedPrincipal(securityToken)).isNull();
    }

    @Test
    @Description("Tests different values for issuer hash header and inspects the credentials")
    public void useDifferentValuesForIssuerHashHeader() {
        final DmfTenantSecurityToken securityToken1 = prepareSecurityToken(SINGLE_HASH);
        final DmfTenantSecurityToken securityToken2 = prepareSecurityToken(SECOND_HASH);

        final HeaderAuthentication expected1 = new HeaderAuthentication(CA_COMMON_NAME_VALUE, SINGLE_HASH);
        final HeaderAuthentication expected2 = new HeaderAuthentication(CA_COMMON_NAME_VALUE, SECOND_HASH);

        when(tenantConfigurationManagementMock.getConfigurationValue(
                TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME, String.class))
                .thenReturn(CONFIG_VALUE_MULTI_HASH);

        final Collection<HeaderAuthentication> credentials1 = (Collection<HeaderAuthentication>) underTest
                .getPreAuthenticatedCredentials(securityToken1);
        final Collection<HeaderAuthentication> credentials2 = (Collection<HeaderAuthentication>) underTest
                .getPreAuthenticatedCredentials(securityToken2);

        final Object principal1 = underTest.getPreAuthenticatedPrincipal(securityToken1);
        final Object principal2 = underTest.getPreAuthenticatedPrincipal(securityToken2);

        assertThat(credentials1).contains(expected1);
        assertThat(credentials2).contains(expected2);

        assertThat(expected1).as("hash1 expected in principal!").isEqualTo(principal1);
        assertThat(expected2).as("hash2 expected in principal!").isEqualTo(principal2);

    }

    private static DmfTenantSecurityToken prepareSecurityToken(final String issuerHashHeaderValue) {
        final DmfTenantSecurityToken securityToken = new DmfTenantSecurityToken("DEFAULT", CA_COMMON_NAME_VALUE);
        securityToken.putHeader(CA_COMMON_NAME, CA_COMMON_NAME_VALUE);
        securityToken.putHeader(X_SSL_ISSUER_HASH_1, issuerHashHeaderValue);
        return securityToken;
    }

}

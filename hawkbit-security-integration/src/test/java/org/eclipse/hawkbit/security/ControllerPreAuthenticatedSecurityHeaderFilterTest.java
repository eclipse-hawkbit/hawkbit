/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;


//import static org.junit.Assert.*;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken.FileResource;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Security")
@Stories("Issuer hash based authentication")
@RunWith(MockitoJUnitRunner.class)
public class ControllerPreAuthenticatedSecurityHeaderFilterTest {

    private ControllerPreAuthenticatedSecurityHeaderFilter underTest;

    @Mock
    private TenantConfigurationManagement tenantConfigurationManagementMock;

    @Mock
    private TenantSecurityToken tenantSecurityTokenMock;

    private SecurityContextTenantAware tenantAware = new SecurityContextTenantAware();

    private static final String CA_COMMON_NAME = "ca-cn";

    private static final String X_SSL_ISSUER_HASH_1 = "X-Ssl-Issuer-Hash-1";

    private static final String SINGLE_HASH = "hash1";

    private static final String MULTI_HASH = "hash1;hash2;hash3";

    private static final TenantConfigurationValue<String> CONFIG_VALUE_SINGLE_HASH = TenantConfigurationValue
            .<String>builder().value(SINGLE_HASH).build();

    private static final TenantConfigurationValue<String> CONFIG_VALUE_MULTI_HASH = TenantConfigurationValue
            .<String>builder().value(MULTI_HASH).build();

    @Before
    public void before() {
        underTest = new ControllerPreAuthenticatedSecurityHeaderFilter(CA_COMMON_NAME, "X-Ssl-Issuer-Hash-%d",
                tenantConfigurationManagementMock,
                tenantAware, new SystemSecurityContext(tenantAware));
    }

    @Test
    @Description("Tests the filter for issuer hash based authentication with a single known hash")
    public void testIssuerHashBasedAuthenticationWithSingleKnownHash() {
        // prepare security token
        final TenantSecurityToken securityToken = prepareSecurityToken();
        securityToken.getHeaders().put(X_SSL_ISSUER_HASH_1, SINGLE_HASH);
        // use single known hash
        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME), eq(String.class)))
                        .thenReturn(CONFIG_VALUE_SINGLE_HASH);
        assertThat(underTest.getPreAuthenticatedPrincipal(securityToken)).isNotNull();
    }

    @Test
    @Description("Tests the filter for issuer hash based authentication with multiple known hashes")
    public void testIssuerHashBasedAuthenticationWithMultipleKnownHashes() {
        // prepare security token
        final TenantSecurityToken securityToken = prepareSecurityToken();
        securityToken.getHeaders().put(X_SSL_ISSUER_HASH_1, SINGLE_HASH);
        // use multiple known hashes
        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME), eq(String.class)))
                        .thenReturn(CONFIG_VALUE_MULTI_HASH);
        assertThat(underTest.getPreAuthenticatedPrincipal(securityToken)).isNotNull();
    }

    @Test
    @Description("Tests the filter for issuer hash based authentication with unknown hash")
    public void testIssuerHashBasedAuthenticationWithUnknownHash() {
        // prepare security token
        final TenantSecurityToken securityToken = prepareSecurityToken();
        securityToken.getHeaders().put(X_SSL_ISSUER_HASH_1, "unknown");
        // use single known hash
        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME), eq(String.class)))
                        .thenReturn(CONFIG_VALUE_MULTI_HASH);
        assertThat(underTest.getPreAuthenticatedPrincipal(securityToken)).isNull();
        ;
    }

    @Test
    @Description("Tests different values for issuer hash header and inspects the credentials")
    public void useDifferentValuesForIssuerHashHeader() {

        // prepare security token
        TenantSecurityToken securityToken = prepareSecurityToken();
        securityToken.getHeaders().put(X_SSL_ISSUER_HASH_1, "hash1");

        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME), eq(String.class)))
                        .thenReturn(CONFIG_VALUE_MULTI_HASH);

        HeaderAuthentication expected = new HeaderAuthentication("box1", "hash1");
        Collection<HeaderAuthentication> credentials = (Collection<HeaderAuthentication>) underTest
                .getPreAuthenticatedCredentials(securityToken);
        assertThat(credentials.contains(expected)).isTrue();

        Object principal = underTest.getPreAuthenticatedPrincipal(securityToken);
        assertEquals("hash1 expected in principal!", expected, principal);

        securityToken = prepareSecurityToken();
        securityToken.getHeaders().put(X_SSL_ISSUER_HASH_1, "hash2");
        expected = new HeaderAuthentication("box1", "hash2");
        credentials = (Collection<HeaderAuthentication>) underTest.getPreAuthenticatedCredentials(securityToken);
        assertThat(credentials.contains(expected)).isTrue();

        principal = underTest.getPreAuthenticatedPrincipal(securityToken);
        assertEquals("hash2 expected in principal!", expected, principal);

    }

    private static TenantSecurityToken prepareSecurityToken() {
        final TenantSecurityToken securityToken = new TenantSecurityToken("DEFAULT", "box1",
                FileResource.createFileResourceBySha1("12345"));
        securityToken.getHeaders().put(CA_COMMON_NAME, "box1");

        return securityToken;
    }

}

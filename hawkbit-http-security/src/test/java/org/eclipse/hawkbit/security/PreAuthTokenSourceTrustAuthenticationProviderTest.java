/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Unit Tests - Security")
@Story("PreAuthToken Source TrustAuthentication Provider Test")
@ExtendWith(MockitoExtension.class)
public class PreAuthTokenSourceTrustAuthenticationProviderTest {

    private static final String REQUEST_SOURCE_IP = "127.0.0.1";

    private final PreAuthTokenSourceTrustAuthenticationProvider underTestWithoutSourceIpCheck = new PreAuthTokenSourceTrustAuthenticationProvider();
    private final PreAuthTokenSourceTrustAuthenticationProvider underTestWithSourceIpCheck = new PreAuthTokenSourceTrustAuthenticationProvider(
            REQUEST_SOURCE_IP);

    @Mock
    private TenantAwareWebAuthenticationDetails webAuthenticationDetailsMock;

    @Test
    @Description("Testing in case the containing controllerId in the URI request path does not accord with the controllerId in the request header.")
    public void principalAndCredentialsNotTheSameThrowsAuthenticationException() {
        final String principal = "controllerIdURL";
        final String credentials = "controllerIdHeader";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Collections.singletonList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        assertThatExceptionOfType(BadCredentialsException.class).as("Should not work with wrong credentials")
                .isThrownBy(() -> underTestWithoutSourceIpCheck.authenticate(token));
    }

    @Test
    @Description("Testing that the controllerId within the URI request path is the same with the controllerId within the request header and no source IP check is in place.")
    public void principalAndCredentialsAreTheSameWithNoSourceIpCheckIsSuccessful() {
        final String principal = "controllerId";
        final String credentials = "controllerId";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Collections.singletonList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        final Authentication authenticate = underTestWithoutSourceIpCheck.authenticate(token);
        assertThat(authenticate.isAuthenticated()).isTrue();
    }

    @Test
    @Description("Testing that the controllerId in the URI request match with the controllerId in the request header but the request are not coming from a trustful source.")
    public void principalAndCredentialsAreTheSameButSourceIpRequestNotMatching2() {
        final String remoteAddress = "192.168.1.1";
        final String principal = "controllerId";
        final String credentials = "controllerId";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Collections.singletonList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        when(webAuthenticationDetailsMock.getRemoteAddress()).thenReturn(remoteAddress);

        assertThatExceptionOfType(InsufficientAuthenticationException.class).as("as source is not trusted.")
                .isThrownBy(() -> underTestWithSourceIpCheck.authenticate(token));
    }

    @Test
    @Description("Testing that the controllerId in the URI request match with the controllerId in the request header and the source IP is matching the allowed remote IP address.")
    public void principalAndCredentialsAreTheSameAndSourceIpIsTrusted() {
        final String principal = "controllerId";
        final String credentials = "controllerId";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Collections.singletonList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        when(webAuthenticationDetailsMock.getRemoteAddress()).thenReturn(REQUEST_SOURCE_IP);

        // test, should throw authentication exception
        final Authentication authenticate = underTestWithSourceIpCheck.authenticate(token);
        assertThat(authenticate.isAuthenticated()).isTrue();
    }

    @Test
    @Description("Testing that the controllerId in the URI request match with the controllerId in the request header and the source IP matches one of the allowed remote IP addresses.")
    public void principalAndCredentialsAreTheSameAndSourceIpIsWithinList() {
        final String[] trustedIPAddresses = new String[] { "192.168.1.1", "192.168.1.2", REQUEST_SOURCE_IP,
                "192.168.1.3" };
        final String principal = "controllerId";
        final String credentials = "controllerId";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Collections.singletonList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        when(webAuthenticationDetailsMock.getRemoteAddress()).thenReturn(REQUEST_SOURCE_IP);

        final PreAuthTokenSourceTrustAuthenticationProvider underTestWithList = new PreAuthTokenSourceTrustAuthenticationProvider(
                trustedIPAddresses);

        // test, should throw authentication exception
        final Authentication authenticate = underTestWithList.authenticate(token);
        assertThat(authenticate.isAuthenticated()).isTrue();
    }

    @Test
    @Description("Testing that the controllerId in the URI request match with the controllerId in the request header and the source IP does not match any of the allowed remote IP addresses.")
    public void principalAndCredentialsAreTheSameSourceIpListNotMatches() {
        final String[] trustedIPAddresses = new String[] { "192.168.1.1", "192.168.1.2", "192.168.1.3" };
        final String principal = "controllerId";
        final String credentials = "controllerId";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Collections.singletonList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        when(webAuthenticationDetailsMock.getRemoteAddress()).thenReturn(REQUEST_SOURCE_IP);

        final PreAuthTokenSourceTrustAuthenticationProvider underTestWithList = new PreAuthTokenSourceTrustAuthenticationProvider(
                trustedIPAddresses);

        assertThatExceptionOfType(InsufficientAuthenticationException.class)
                .isThrownBy(() -> underTestWithList.authenticate(token));
    }
}

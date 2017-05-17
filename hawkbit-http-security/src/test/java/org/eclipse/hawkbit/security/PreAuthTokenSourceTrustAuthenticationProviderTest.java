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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Security")
@Stories("PreAuthToken Source TrustAuthentication Provider Test")
@RunWith(MockitoJUnitRunner.class)
// TODO: create description annotations
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
                Arrays.asList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        // test, should throw authentication exception
        try {
            underTestWithoutSourceIpCheck.authenticate(token);
            fail("Should not work with wrong credentials");
        } catch (final BadCredentialsException e) {

        }

    }

    @Test
    @Description("Testing that the controllerId within the URI request path is the same with the controllerId within the request header and no source IP check is in place.")
    public void principalAndCredentialsAreTheSameWithNoSourceIpCheckIsSuccessful() {
        final String principal = "controllerId";
        final String credentials = "controllerId";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Arrays.asList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        final Authentication authenticate = underTestWithoutSourceIpCheck.authenticate(token);
        assertThat(authenticate.isAuthenticated()).isTrue();
    }

    @Test
    @Description("Testing that the controllerId in the URI request match with the controllerId in the request header but the request are not coming from a trustful source.")
    public void priniciapAndCredentialsAreTheSameButSourceIpRequestNotMatching() {
        final String remoteAddress = "192.168.1.1";
        final String principal = "controllerId";
        final String credentials = "controllerId";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Arrays.asList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        when(webAuthenticationDetailsMock.getRemoteAddress()).thenReturn(remoteAddress);

        // test, should throw authentication exception

        try {
            underTestWithSourceIpCheck.authenticate(token);
            fail("as source is not trusted.");
        } catch (final InsufficientAuthenticationException e) {

        }
    }

    @Test
    @Description("Testing that the controllerId in the URI request match with the controllerId in the request header and the source Ip is matching the allowed remote IP address.")
    public void priniciapAndCredentialsAreTheSameAndSourceIpIsTrusted() {
        final String principal = "controllerId";
        final String credentials = "controllerId";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Arrays.asList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        when(webAuthenticationDetailsMock.getRemoteAddress()).thenReturn(REQUEST_SOURCE_IP);

        // test, should throw authentication exception
        final Authentication authenticate = underTestWithSourceIpCheck.authenticate(token);
        assertThat(authenticate.isAuthenticated()).isTrue();
    }

    @Test
    public void priniciapAndCredentialsAreTheSameAndSourceIpIsWithinList() {
        final String[] trustedIPAddresses = new String[] { "192.168.1.1", "192.168.1.2", REQUEST_SOURCE_IP,
                "192.168.1.3" };
        final String principal = "controllerId";
        final String credentials = "controllerId";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Arrays.asList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        when(webAuthenticationDetailsMock.getRemoteAddress()).thenReturn(REQUEST_SOURCE_IP);

        final PreAuthTokenSourceTrustAuthenticationProvider underTestWithList = new PreAuthTokenSourceTrustAuthenticationProvider(
                trustedIPAddresses);

        // test, should throw authentication exception
        final Authentication authenticate = underTestWithList.authenticate(token);
        assertThat(authenticate.isAuthenticated()).isTrue();
    }

    @Test(expected = InsufficientAuthenticationException.class)
    public void principalAndCredentialsAreTheSameSourceIpListNotMatches() {
        final String[] trustedIPAddresses = new String[] { "192.168.1.1", "192.168.1.2", "192.168.1.3" };
        final String principal = "controllerId";
        final String credentials = "controllerId";
        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Arrays.asList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        when(webAuthenticationDetailsMock.getRemoteAddress()).thenReturn(REQUEST_SOURCE_IP);

        final PreAuthTokenSourceTrustAuthenticationProvider underTestWithList = new PreAuthTokenSourceTrustAuthenticationProvider(
                trustedIPAddresses);

        // test, should throw authentication exception
        final Authentication authenticate = underTestWithList.authenticate(token);
        try {
            assertThat(authenticate.isAuthenticated()).isTrue();
            fail("as source is not trusted.");
        } catch (final InsufficientAuthenticationException e) {

        }
    }
}

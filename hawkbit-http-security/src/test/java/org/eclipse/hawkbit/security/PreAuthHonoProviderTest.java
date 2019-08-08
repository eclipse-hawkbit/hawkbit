/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@Feature("Unit Tests - Security")
@Story("PreAuth Hono Provider Test")
@RunWith(MockitoJUnitRunner.class)
public class PreAuthHonoProviderTest {

    private final PreAuthHonoAuthenticationProvider testProvider = new PreAuthHonoAuthenticationProvider();

    @Mock
    private TenantAwareWebAuthenticationDetails webAuthenticationDetailsMock;

    @Test
    @Description("Testing that the provided credentials are incorrect.")
    public void invalidCredentialsThrowsAuthenticationException() {

        HeaderAuthentication principal = new HeaderAuthentication("deviceId", "wrongPassword");

        HonoPasswordSecret secret = new HonoPasswordSecret(null, null, "sha-256", "salt", "hash");
        HonoCredentials credentials = new HonoCredentials("deviceId", "hashed-password", "authId", true, Collections.singletonList(secret));

        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Collections.singletonList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        // test, should throw authentication exception
        try {
            testProvider.authenticate(token);
            fail("Should not work with wrong credentials");
        } catch (final BadCredentialsException e) {
        }
    }

    @Test
    @Description("Testing that the provided credentials are correct.")
    public void credentialsAreCorrect() {

        HeaderAuthentication principal = new HeaderAuthentication("deviceId", "password");

        HonoPasswordSecret secret = new HonoPasswordSecret(null, null, "sha-256", "salt", "7a37b85c8918eac19a9089c0fa5a2ab4dce3f90528dcdeec108b23ddf3607b99");
        HonoCredentials credentials = new HonoCredentials("deviceId", "hashed-password", "authId", true, Collections.singletonList(secret));

        final PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(principal,
                Collections.singletonList(credentials));
        token.setDetails(webAuthenticationDetailsMock);

        // test, should throw authentication exception
        final Authentication authenticate = testProvider.authenticate(token);
        assertThat(authenticate.isAuthenticated()).isTrue();
    }
}

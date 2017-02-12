/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpSession;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.eclipse.hawkbit.ui.SpringSecurityAtmosphereInterceptor;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Management UI")
@Stories("Push Security")
@RunWith(MockitoJUnitRunner.class)
// TODO: create description annotations
public class SpringSecurityAtmosphereInterceptorTest {

    @Mock
    private AtmosphereResource atmosphereResourceMock;
    @Mock
    private AtmosphereRequest atmosphereRequestMock;
    @Mock
    private SecurityContext sessionSecurityContextMock;
    @Mock
    private HttpSession httpSessionMock;

    private final SpringSecurityAtmosphereInterceptor underTest = new SpringSecurityAtmosphereInterceptor();

    @After
    public void after() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void inspectRetrievesSetsSecurityContextFromRequestToThreadLocal() {

        when(atmosphereResourceMock.getRequest()).thenReturn(atmosphereRequestMock);
        when(atmosphereRequestMock.getSession()).thenReturn(httpSessionMock);
        when(httpSessionMock.getAttribute(Mockito.eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)))
                .thenReturn(sessionSecurityContextMock);
        underTest.inspect(atmosphereResourceMock);
        // verify
        assertThat(SecurityContextHolder.getContext()).isEqualTo(sessionSecurityContextMock);
    }

    @Test
    public void afterAtmosphereRequestSecurityContextGetsCleared() {
        SecurityContextHolder.setContext(sessionSecurityContextMock);

        underTest.postInspect(atmosphereResourceMock);

        assertThat(SecurityContextHolder.getContext()).isNotEqualTo(sessionSecurityContextMock);
    }
}

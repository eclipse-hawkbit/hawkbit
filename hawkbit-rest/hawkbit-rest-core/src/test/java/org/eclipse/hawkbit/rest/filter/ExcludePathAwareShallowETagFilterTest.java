/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Security")
@Stories("Exclude path aware shallow ETag filter")
@RunWith(MockitoJUnitRunner.class)
public class ExcludePathAwareShallowETagFilterTest {

    @Mock
    private HttpServletRequest servletRequestMock;

    @Mock
    private HttpServletResponse servletResponseMock;

    @Mock
    private FilterChain filterChainMock;

    @Test
    public void excludePathDoesNotCalculateETag() throws ServletException, IOException {
        final String knownContextPath = "/bumlux/test";
        final String knownUri = knownContextPath + "/exclude/download";
        final String antPathExclusion = "/exclude/**";

        // mock
        when(servletRequestMock.getContextPath()).thenReturn(knownContextPath);
        when(servletRequestMock.getRequestURI()).thenReturn(knownUri);

        final ExcludePathAwareShallowETagFilter filterUnderTest = new ExcludePathAwareShallowETagFilter(
                antPathExclusion);

        filterUnderTest.doFilterInternal(servletRequestMock, servletResponseMock, filterChainMock);

        // verify no eTag header is set and response has not been changed
        assertThat(servletResponseMock.getHeader("ETag"))
                .as("ETag header should not be set during downloading, too expensive").isNull();
        // the servlet response must be the same mock!
        verify(filterChainMock, times(1)).doFilter(servletRequestMock, servletResponseMock);
    }

    @Test
    public void pathNotExcludedETagIsCalculated() throws ServletException, IOException {
        final String knownContextPath = "/bumlux/test";
        final String knownUri = knownContextPath + "/include/download";
        final String antPathExclusion = "/exclude/**";

        // mock
        when(servletRequestMock.getContextPath()).thenReturn(knownContextPath);
        when(servletRequestMock.getRequestURI()).thenReturn(knownUri);

        final ExcludePathAwareShallowETagFilter filterUnderTest = new ExcludePathAwareShallowETagFilter(
                antPathExclusion);

        final ArgumentCaptor<HttpServletResponse> responseArgumentCaptor = ArgumentCaptor
                .forClass(HttpServletResponse.class);

        filterUnderTest.doFilterInternal(servletRequestMock, servletResponseMock, filterChainMock);

        // the servlet response must be the same mock!
        verify(filterChainMock, times(1)).doFilter(Mockito.eq(servletRequestMock), responseArgumentCaptor.capture());
        assertThat(mockingDetails(responseArgumentCaptor.getValue()).isMock()).isFalse();
    }
}

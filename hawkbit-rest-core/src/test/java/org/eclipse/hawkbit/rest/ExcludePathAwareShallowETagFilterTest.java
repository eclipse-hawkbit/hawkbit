/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Exclude path aware shallow ETag filter
 */
@ExtendWith(MockitoExtension.class)
class ExcludePathAwareShallowETagFilterTest {

    @Mock
    private HttpServletRequest servletRequestMock;

    @Mock
    private HttpServletResponse servletResponseMock;

    @Mock
    private FilterChain filterChainMock;

    @Test
    void excludePathDoesNotCalculateETag() throws ServletException, IOException {
        final String knownContextPath = "/bumlux/test";
        final String knownUri = knownContextPath + "/exclude/download";
        final String antPathExclusion = "/exclude/**";

        // mock
        when(servletRequestMock.getContextPath()).thenReturn(knownContextPath);
        when(servletRequestMock.getRequestURI()).thenReturn(knownUri);

        final RestConfiguration.ExcludePathAwareShallowETagFilter filterUnderTest = new RestConfiguration.ExcludePathAwareShallowETagFilter(
                antPathExclusion);

        filterUnderTest.doFilterInternal(servletRequestMock, servletResponseMock, filterChainMock);

        // verify no eTag header is set and response has not been changed
        assertThat(servletResponseMock.getHeader("ETag"))
                .as("ETag header should not be set during downloading, too expensive").isNull();
        // the servlet response must be the same mock!
        verify(filterChainMock, times(1)).doFilter(servletRequestMock, servletResponseMock);
    }

    @Test
    void pathNotExcludedETagIsCalculated() throws ServletException, IOException {
        final String knownContextPath = "/bumlux/test";
        final String knownUri = knownContextPath + "/include/download";
        final String antPathExclusion = "/exclude/**";

        // mock
        when(servletRequestMock.getContextPath()).thenReturn(knownContextPath);
        when(servletRequestMock.getRequestURI()).thenReturn(knownUri);

        final RestConfiguration.ExcludePathAwareShallowETagFilter filterUnderTest = new RestConfiguration.ExcludePathAwareShallowETagFilter(
                antPathExclusion);

        final ArgumentCaptor<HttpServletResponse> responseArgumentCaptor = ArgumentCaptor
                .forClass(HttpServletResponse.class);

        filterUnderTest.doFilterInternal(servletRequestMock, servletResponseMock, filterChainMock);

        // the servlet response must be the same mock!
        verify(filterChainMock, times(1)).doFilter(Mockito.eq(servletRequestMock), responseArgumentCaptor.capture());
        assertThat(mockingDetails(responseArgumentCaptor.getValue()).isMock()).isFalse();
    }
}

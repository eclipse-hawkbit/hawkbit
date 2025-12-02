/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.artifact.model.ArtifactStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Feature: Component Tests - Management API<br/>
 * Story: File streaming
 */
class FileStreamingUtilTest {

    private static final String CONTENT = "This is some very long string which is intended to test";
    private static final byte[] CONTENT_BYTES = CONTENT.getBytes(StandardCharsets.UTF_8);

    private static final Supplier<ArtifactStream> TEST_ARTIFACT =
            () -> new ArtifactStream(new ByteArrayInputStream(CONTENT_BYTES), CONTENT_BYTES.length, "sha1-111");

    @Test
    void shouldProcessRangeHeaderForMultipartRequests() throws IOException {
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final ServletOutputStream outputStream = Mockito.mock(ServletOutputStream.class);

        Mockito.when(servletResponse.getOutputStream()).thenReturn(outputStream);
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(servletRequest.getHeader("Range")).thenReturn("bytes=0-10,11-15,16-");
        long lastModified = System.currentTimeMillis();

        final ResponseEntity<InputStream> responseEntity = FileStreamingUtil.writeFileResponse(
                TEST_ARTIFACT.get(), "test.file", lastModified, servletResponse, servletRequest, null);

        assertThat(responseEntity).isNotNull();
        verify(servletResponse).setDateHeader(HttpHeaders.LAST_MODIFIED, lastModified);
        final ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Integer> lenCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(outputStream).print(stringCaptor.capture());
        assertThat(stringCaptor.getValue()).contains("--THIS_STRING_SEPARATES_MULTIPART--");
        verify(outputStream, times(3)).write(any(), anyInt(), lenCaptor.capture());
        assertThat(lenCaptor.getAllValues()).containsExactly(11, 5, 39); // Range lengths
    }

    @Test
    void shouldValidateRangeHeaderForMultipartRequests() throws IOException {
        long lastModified = System.currentTimeMillis();
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        final ServletOutputStream outputStream = Mockito.mock(ServletOutputStream.class);
        Mockito.when(servletResponse.getOutputStream()).thenReturn(outputStream);

        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(servletRequest.getHeader("Range")).thenReturn("bytes=0-10***,9-15,16-");

        final ResponseEntity<InputStream> responseEntity = FileStreamingUtil.writeFileResponse(
                TEST_ARTIFACT.get(), "test.file", lastModified, servletResponse, servletRequest, null);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        verify(outputStream, times(0)).print(anyString());
        verify(outputStream, times(0)).write(any(), anyInt(), anyInt());
    }
}

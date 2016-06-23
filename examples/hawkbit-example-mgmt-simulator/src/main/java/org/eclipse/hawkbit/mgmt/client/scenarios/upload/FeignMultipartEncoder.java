/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.scenarios.upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;

/**
 * A feign encoder implementation which handles {@link MultipartFile} body.
 */
public class FeignMultipartEncoder implements Encoder {

    private final List<HttpMessageConverter<?>> converters = new RestTemplate().getMessageConverters();
    private final HttpHeaders multipartHeaders = new HttpHeaders();
    private final HttpHeaders jsonHeaders = new HttpHeaders();

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public FeignMultipartEncoder() {
        multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public void encode(final Object object, final Type bodyType, final RequestTemplate template) {
        encodeMultipartFormRequest(object, template);
    }

    private void encodeMultipartFormRequest(final Object value, final RequestTemplate template) {
        if (value == null) {
            throw new EncodeException("Cannot encode request with null value.");
        }
        if (!isMultipartFile(value)) {
            throw new EncodeException("Only multipart can be handled by this encoder");
        }
        encodeRequest(encodeMultipartFile((MultipartFile) value), multipartHeaders, template);
    }

    @SuppressWarnings("unchecked")
    private void encodeRequest(final Object value, final HttpHeaders requestHeaders, final RequestTemplate template) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final HttpOutputMessage dummyRequest = new HttpOutputMessageImpl(outputStream, requestHeaders);
        try {
            final Class<?> requestType = value.getClass();
            final MediaType requestContentType = requestHeaders.getContentType();
            for (final HttpMessageConverter<?> messageConverter : converters) {
                if (messageConverter.canWrite(requestType, requestContentType)) {
                    ((HttpMessageConverter<Object>) messageConverter).write(value, requestContentType, dummyRequest);
                    break;
                }
            }
        } catch (final IOException ex) {
            throw new EncodeException("Cannot encode request.", ex);
        }
        final HttpHeaders headers = dummyRequest.getHeaders();
        if (headers != null) {
            for (final Entry<String, List<String>> entry : headers.entrySet()) {
                template.header(entry.getKey(), entry.getValue());
            }
        }
        /*
         * we should use a template output stream... this will cause issues if
         * files are too big, since the whole request will be in memory.
         */
        template.body(outputStream.toByteArray(), UTF_8);
    }

    private MultiValueMap<String, Object> encodeMultipartFile(final MultipartFile file) {
        try {
            final MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
            multiValueMap.add("file", new MultipartFileResource(file.getName(), file.getSize(), file.getInputStream()));
            return multiValueMap;
        } catch (final IOException ex) {
            throw new EncodeException("Cannot encode request.", ex);
        }
    }

    private static boolean isMultipartFile(final Object object) {
        return object instanceof MultipartFile;
    }

    private static final class HttpOutputMessageImpl implements HttpOutputMessage {

        private final OutputStream body;
        private final HttpHeaders headers;

        private HttpOutputMessageImpl(final OutputStream body, final HttpHeaders headers) {
            this.body = body;
            this.headers = headers;
        }

        @Override
        public OutputStream getBody() throws IOException {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

    }

    /**
     * Dummy resource class. Wraps file content and its original name.
     */
    static class MultipartFileResource extends InputStreamResource {

        private final String filename;
        private final long size;

        public MultipartFileResource(final String filename, final long size, final InputStream inputStream) {
            super(inputStream);
            this.size = size;
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return this.filename;
        }

        @Override
        public long contentLength() throws IOException {
            return size;
        }

    }
}

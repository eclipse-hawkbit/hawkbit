/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.fasterxml.jackson.annotation.JsonInclude;
import feign.Contract;
import feign.RequestTemplate;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.core.io.InputStreamResource;
import org.springframework.hateoas.mediatype.hal.HalJacksonModule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Boot 4 (Jackson 3 / HAL) compatible Feign codecs for the hawkBit UI.
 */
final class HawkbitClientCodecs {

    public static final Encoder DEFAULT_ENCODER = new Encoder() {

        private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .build();

        @Override
        public void encode(final Object object, final Type bodyType, final RequestTemplate template) {
            final JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructType(bodyType);
            template.body(OBJECT_MAPPER.writerFor(javaType).writeValueAsBytes(object), Util.UTF_8);
        }
    };

    /**
     * A decorator for the {@link ResponseEntityDecoder} that extends it whit hal-json and octet streams support.
     */
    public static final Decoder DEFAULT_DECODER = new Decoder() {

        private static final String OCTET_STREAM = "[application/octet-stream]";
        private static final String OCTET_STREAM_UTF8 = "[application/octet-stream;charset=UTF-8]";
        private static final String TEXT_PLAIN = "[text/plain]";
        private static final String TEXT_PLAIN_UTF8 = "[text/plain;charset=UTF-8]";

        private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .addModule(new HalJacksonModule())
                .build();

        private final ResponseEntityDecoder delegate = new ResponseEntityDecoder((response, type) -> {
            if (response.status() == 404 || response.status() == 204) {
                return Util.emptyValueOf(type);
            } else if (response.body() == null) {
                return null;
            } else {
                final Reader reader = markSupportedReader(response.body().asReader(response.charset()));
                reader.mark(1);
                if (reader.read() == -1) {
                    return null;
                }
                reader.reset();
                return OBJECT_MAPPER.readValue(reader, OBJECT_MAPPER.constructType(type));
            }
        });

        @Override
        public Object decode(final Response response, final Type type) throws IOException {
            if (type instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() == ResponseEntity.class) {
                final String contentType = String.valueOf(response.headers().get(HttpHeaders.CONTENT_TYPE));
                if (contentType.equals(OCTET_STREAM) || contentType.equals(OCTET_STREAM_UTF8)) {
                    final byte[] bodyData = Util.toByteArray(response.body().asInputStream());
                    final InputStream convertedInputStream = response.toBuilder().body(bodyData).build().body().asInputStream();
                    if (parameterizedType.getActualTypeArguments()[0] instanceof Class<?> clazz
                            && InputStreamResource.class.isAssignableFrom(clazz)) {
                        return new ResponseEntity<>(new InputStreamResource(convertedInputStream), HttpStatus.valueOf(response.status()));
                    } else {
                        return new ResponseEntity<>(convertedInputStream, HttpStatus.valueOf(response.status()));
                    }
                } else if (contentType.equals(TEXT_PLAIN) || contentType.equals(TEXT_PLAIN_UTF8)) {
                    final byte[] bodyData = Util.toByteArray(response.body().asInputStream());
                    return new ResponseEntity<>(new String(bodyData), HttpStatus.valueOf(response.status()));
                }
            }
            return delegate.decode(response, type);
        }

        private static @NonNull Reader markSupportedReader(final Reader reader) {
            return reader.markSupported() ? reader : new BufferedReader(reader, 1);
        }
    };

    public static final Contract DEFAULT_CONTRACT = new SpringMvcContract();
}
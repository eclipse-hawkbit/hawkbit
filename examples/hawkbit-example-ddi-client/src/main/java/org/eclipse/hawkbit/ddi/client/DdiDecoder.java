/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.client;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Response;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;

/**
 * Decoder for DDI client.
 *
 */
public class DdiDecoder implements Decoder {

    private static final String OCTET_STREAM = "[application/octet-stream]";

    private final ObjectMapper mapper;

    public DdiDecoder() {
        mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new Jackson2HalModule());
    }

    @Override
    public Object decode(final Response response, final Type type) throws IOException {

        final Map<String, Collection<String>> header = response.headers();
        final String contentType = String.valueOf(header.get("Content-Type"));
        if (contentType.equals(OCTET_STREAM)) {
            return ResponseEntity.ok(response.body().asInputStream());
        }
        final ResponseEntityDecoder responseEntityDecoder = new ResponseEntityDecoder(new JacksonDecoder(mapper));
        return responseEntityDecoder.decode(response, type);
    }

}

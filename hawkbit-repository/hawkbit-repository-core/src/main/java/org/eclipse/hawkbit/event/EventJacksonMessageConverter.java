/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.util.MimeType;

public class EventJacksonMessageConverter extends MappingJackson2MessageConverter {

    public static final MimeType APPLICATION_REMOTE_EVENT_JSON = new MimeType("application", "remote-event-json");


    public EventJacksonMessageConverter() {
        super(APPLICATION_REMOTE_EVENT_JSON);
        ObjectMapper objectMapper = new ObjectMapper();
        EventType.getNamedTypes().forEach(objectMapper::registerSubtypes);
        setObjectMapper(objectMapper);
    }

    @Override
    protected Object convertToInternal(final Object payload, final MessageHeaders headers, final Object conversionHint) {
        return super.convertToInternal(payload, headers, conversionHint);
    }

    @Override
    protected Object convertFromInternal(final Message<?> message, final Class<?> targetClass, final Object conversionHint) {
        return super.convertFromInternal(message, targetClass, conversionHint);
    }
}

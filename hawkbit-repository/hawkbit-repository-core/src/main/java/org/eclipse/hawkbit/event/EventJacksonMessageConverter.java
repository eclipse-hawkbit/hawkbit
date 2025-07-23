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
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.util.MimeType;

public class EventJacksonMessageConverter extends MappingJackson2MessageConverter {

    public EventJacksonMessageConverter() {
        super(new MimeType("application", "remote-event-json"));
        ObjectMapper objectMapper = new ObjectMapper();
        EventType.getNamedTypes().forEach(objectMapper::registerSubtypes);
        setObjectMapper(objectMapper);
    }
}

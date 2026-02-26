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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.util.MimeType;
import tools.jackson.databind.json.JsonMapper;

public class EventJacksonMessageConverter extends JacksonJsonMessageConverter {

    public static final MimeType APPLICATION_REMOTE_EVENT_JSON = new MimeType("application", "remote-event-json");

    public EventJacksonMessageConverter(final JsonMapper mapper) {
        super(mapper, APPLICATION_REMOTE_EVENT_JSON);
    }

    @Override
    @SuppressWarnings("java:S1185") // intentionally override in order to extend visibility
    @NullMarked
    @Nullable
    protected Object convertToInternal(final Object payload, @Nullable final MessageHeaders headers, @Nullable final Object conversionHint) {
        return super.convertToInternal(payload, headers, conversionHint);
    }

    @Override
    @SuppressWarnings("java:S1185") // intentionally override in order to extend visibility
    @NullMarked
    @Nullable
    protected Object convertFromInternal(final Message<?> message, final Class<?> targetClass, @Nullable final Object conversionHint) {
        return super.convertFromInternal(message, targetClass, conversionHint);
    }
}
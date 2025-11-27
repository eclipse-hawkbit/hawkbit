/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.Map;

import org.eclipse.hawkbit.event.EventJacksonMessageConverter;
import org.eclipse.hawkbit.event.EventProtoStuffMessageConverter;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Test the remote entity events.
 */
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
public abstract class AbstractRemoteEventTest extends AbstractJpaIntegrationTest {

    private EventProtoStuffMessageConverter eventProtoStuffMessageConverter = new EventProtoStuffMessageConverter();

    private EventJacksonMessageConverter jacksonMessageConverter = new EventJacksonMessageConverter();


    @SuppressWarnings("unchecked")
    protected <T extends TenantAwareEvent> T createJacksonEvent(final T event) {
        final Message<?> message = createJsonMessage(event);
        return (T) jacksonMessageConverter.fromMessage(message, event.getClass());
    }

    @SuppressWarnings("unchecked")
    protected <T extends TenantAwareEvent> T createProtoStuffEvent(final T event) {
        final Message<?> message = createProtoStuffMessage(event);
        return (T) eventProtoStuffMessageConverter.fromMessage(message, event.getClass());
    }

    private Message<?> createProtoStuffMessage(final TenantAwareEvent event) {
        return eventProtoStuffMessageConverter.toMessage(
                event, new MutableMessageHeaders(Map.of(MessageHeaders.CONTENT_TYPE,
                        EventProtoStuffMessageConverter.APPLICATION_BINARY_PROTOSTUFF))
        );
    }

    private Message<?> createJsonMessage(final Object event) {
        return jacksonMessageConverter.toMessage(event, new MutableMessageHeaders(Map.of(MessageHeaders.CONTENT_TYPE,
                EventJacksonMessageConverter.APPLICATION_REMOTE_EVENT_JSON)));
    }

}
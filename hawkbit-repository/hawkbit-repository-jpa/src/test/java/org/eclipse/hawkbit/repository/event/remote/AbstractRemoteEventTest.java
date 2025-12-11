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

import static org.springframework.messaging.MessageHeaders.CONTENT_TYPE;

import java.util.Map;

import org.eclipse.hawkbit.event.EventJacksonMessageConverter;
import org.eclipse.hawkbit.event.EventProtoStuffMessageConverter;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MutableMessageHeaders;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test the remote entity events.
 */
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
public abstract class AbstractRemoteEventTest extends AbstractJpaIntegrationTest {

    private final EventProtoStuffMessageConverter eventProtoStuffMessageConverter = new EventProtoStuffMessageConverter();
    private EventJacksonMessageConverter jacksonMessageConverter;

    @Autowired
    void setJsonMapper(final JsonMapper jsonMapper) {
        jacksonMessageConverter = new EventJacksonMessageConverter(jsonMapper);
    }

    @SuppressWarnings("unchecked")
    protected <T extends TenantAwareEvent> T createJacksonEvent(final T event) {
        return (T) jacksonMessageConverter.fromMessage(
                jacksonMessageConverter.toMessage
                        (event, new MutableMessageHeaders(Map.of(CONTENT_TYPE, EventJacksonMessageConverter.APPLICATION_REMOTE_EVENT_JSON))),
                event.getClass());
    }

    @SuppressWarnings("unchecked")
    protected <T extends TenantAwareEvent> T createProtoStuffEvent(final T event) {
        return (T) eventProtoStuffMessageConverter.fromMessage(
                eventProtoStuffMessageConverter.toMessage(
                        event, new MutableMessageHeaders(Map.of(CONTENT_TYPE, EventProtoStuffMessageConverter.APPLICATION_BINARY_PROTOSTUFF))),
                event.getClass());
    }
}
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

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.hawkbit.event.EventProtoStuffMessageConverter;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.MimeTypeUtils;

/**
 * Test the remote entity events.
 */
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
@Import(AbstractRemoteEventTest.EventProtoStuffTestConfig.class)
public abstract class AbstractRemoteEventTest extends AbstractJpaIntegrationTest {

    @Autowired
    private EventProtoStuffMessageConverter eventProtoStuffMessageConverter;

    private AbstractMessageConverter jacksonMessageConverter;

    @BeforeEach
    public void setup() {
        this.jacksonMessageConverter = new MappingJackson2MessageConverter();
    }

    @SuppressWarnings("unchecked")
    protected <T extends TenantAwareEvent> T createJacksonEvent(final T event) {
        final Message<String> message = createJsonMessage(event);
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

    private Message<String> createJsonMessage(final Object event) {
        try {
            String json = new ObjectMapper().writeValueAsString(event);
            return MessageBuilder.withPayload(json)
                    .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                    .build();
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }
        return null;
    }

    @TestConfiguration
    static class EventProtoStuffTestConfig {
        @Bean
        public MessageConverter eventProtoBufConverter() {
            return new EventProtoStuffMessageConverter();
        }
    }
}
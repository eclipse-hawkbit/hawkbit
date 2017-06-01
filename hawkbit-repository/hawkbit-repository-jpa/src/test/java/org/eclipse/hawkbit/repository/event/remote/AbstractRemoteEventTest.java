/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import static org.junit.Assert.fail;

import java.util.Map;

import org.eclipse.hawkbit.event.BusProtoStuffMessageConverter;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.Before;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.bus.jackson.BusJacksonAutoConfiguration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

/**
 * Test the remote entity events.
 */
public abstract class AbstractRemoteEventTest extends AbstractJpaIntegrationTest {

    @Autowired
    private BusProtoStuffMessageConverter busProtoStuffMessageConverter;

    private AbstractMessageConverter jacksonMessageConverter;

    @Before
    public void setup() throws Exception {
        final BusJacksonAutoConfiguration autoConfiguration = new BusJacksonAutoConfiguration();
        this.jacksonMessageConverter = autoConfiguration.busJsonConverter();
        ReflectionTestUtils.setField(jacksonMessageConverter, "packagesToScan",
                new String[] { "org.eclipse.hawkbit.repository.event.remote",
                        ClassUtils.getPackageName(RemoteApplicationEvent.class) });
        ((InitializingBean) jacksonMessageConverter).afterPropertiesSet();

    }

    private Message<?> createProtoStuffMessage(final TenantAwareEvent event) {
        final Map<String, Object> headers = Maps.newLinkedHashMap();
        headers.put(MessageHeaders.CONTENT_TYPE, BusProtoStuffMessageConverter.APPLICATION_BINARY_PROTOSTUFF);
        return busProtoStuffMessageConverter.toMessage(event, new MutableMessageHeaders(headers));
    }

    private Message<String> createJsonMessage(final Object event) {
        final Map<String, MimeType> headers = Maps.newLinkedHashMap();
        headers.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
        try {
            final String json = new ObjectMapper().writeValueAsString(event);
            final Message<String> message = MessageBuilder.withPayload(json).copyHeaders(headers).build();
            return message;
        } catch (final JsonProcessingException e) {
            fail(e.getMessage());
        }
        return null;
    }

    protected Message<?> createMessageWithImmutableHeader(final TenantAwareEvent event) {
        final Map<String, Object> headers = Maps.newLinkedHashMap();
        return busProtoStuffMessageConverter.toMessage(event, new MessageHeaders(headers));
    }

    protected TenantAwareEvent createJacksonEvent(final TenantAwareEvent event) {
        final Message<?> message = createJsonMessage(event);
        return (TenantAwareEvent) jacksonMessageConverter.fromMessage(message, event.getClass());
    }

    protected TenantAwareEvent createProtoStuffEvent(final TenantAwareEvent event) {
        final Message<?> message = createProtoStuffMessage(event);
        return (TenantAwareEvent) busProtoStuffMessageConverter.fromMessage(message, event.getClass());
    }
}

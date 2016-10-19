/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.junit.Before;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.bus.jackson.BusJacksonAutoConfiguration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Entity Events")
public abstract class AbstractRemoteEntityEventTest<E extends BaseEntity> extends AbstractJpaIntegrationTest {

    private AbstractMessageConverter abstractMessageConverter;

    @Before
    public void setup() throws Exception {
        final BusJacksonAutoConfiguration autoConfiguration = new BusJacksonAutoConfiguration();
        this.abstractMessageConverter = autoConfiguration.busJsonConverter();
        final String[] allRemoteEventsFromPackage = new String[] { "org.eclipse.hawkbit.repository.event.remote",
                ClassUtils.getPackageName(RemoteApplicationEvent.class) };
        ReflectionTestUtils.setField(abstractMessageConverter, "packagesToScan", allRemoteEventsFromPackage);
        ((InitializingBean) abstractMessageConverter).afterPropertiesSet();

    }

    protected void assertAndCreateRemoteEvent(final Class<? extends RemoteEntityEvent<?>> eventType) {

        final BaseEntity baseEntity = createEntity();

        RemoteEntityEvent<?> event;
        try {
            event = eventType.getDeclaredConstructor(Object.class, String.class).newInstance(baseEntity, "Node");
            assertThat(event.getEntity()).isSameAs(baseEntity);

            final Message<?> message = createMessage(event);
            final RemoteEntityEvent<?> underTestCreatedEvent = (RemoteEntityEvent<?>) abstractMessageConverter
                    .fromMessage(message, eventType);
            assertThat(underTestCreatedEvent.getEntity()).isEqualTo(baseEntity);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | JsonProcessingException e) {
            fail("Exception should not happen " + e.getMessage());
        }
    }

    private Message<String> createMessage(final RemoteEntityEvent<?> event) throws JsonProcessingException {
        final Map<String, MimeType> headers = Maps.newLinkedHashMap();
        headers.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
        final String json = new ObjectMapper().writeValueAsString(event);
        return MessageBuilder.withPayload(json).copyHeaders(headers).build();
    }

    protected abstract E createEntity();
}

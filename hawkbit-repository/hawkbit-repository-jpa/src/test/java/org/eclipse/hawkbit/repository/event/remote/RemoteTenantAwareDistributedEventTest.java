/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.Map;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.junit.Before;
import org.junit.Test;
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

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Distributet Events Tests")
public class RemoteTenantAwareDistributedEventTest extends AbstractJpaIntegrationTest {

    private AbstractMessageConverter abstractMessageConverter;

    @Before
    public void setup() throws Exception {
        final BusJacksonAutoConfiguration autoConfiguration = new BusJacksonAutoConfiguration();
        this.abstractMessageConverter = autoConfiguration.busJsonConverter();
        ReflectionTestUtils.setField(abstractMessageConverter, "packagesToScan",
                new String[] { "org.eclipse.hawkbit.repository.event.remote",
                        ClassUtils.getPackageName(RemoteApplicationEvent.class) });
        ((InitializingBean) abstractMessageConverter).afterPropertiesSet();

    }

    @Test
    @Description("Verifies that the target info reloading by remote events works")
    public void reloadTargetInfoByRemoteEvent() throws JsonProcessingException {
        final Target createdTarget = targetManagement.createTarget(entityFactory.generateTarget("12345"));
        final TargetInfo targetInfo = createdTarget.getTargetInfo();
        final TargetInfoUpdateEvent infoUpdateEvent = new TargetInfoUpdateEvent(targetInfo, "Node");
        assertThat(targetInfo).isSameAs(infoUpdateEvent.getEntity());

        final Message<?> message = createMessage(infoUpdateEvent);

        final TargetInfoUpdateEvent remoteEvent = (TargetInfoUpdateEvent) abstractMessageConverter.fromMessage(message,
                TargetInfoUpdateEvent.class);

        final TargetInfo jpaTargetInfo = remoteEvent.getEntity();
        assertThat(jpaTargetInfo.getTarget().getControllerId()).isEqualTo(targetInfo.getTarget().getControllerId());
        assertThat(jpaTargetInfo.getUpdateStatus()).isEqualTo(targetInfo.getUpdateStatus());
    }

    @Test
    @Description("Verifies that the download progress reloading by remote events works")
    public void reloadDownloadProgessByRemoteEvent() throws JsonProcessingException {
        final DownloadProgressEvent downloadProgressEvent = new DownloadProgressEvent("DEFAULT", 3L, "Node");

        final Message<?> message = createMessage(downloadProgressEvent);

        final DownloadProgressEvent remoteEvent = (DownloadProgressEvent) abstractMessageConverter.fromMessage(message,
                DownloadProgressEvent.class);
        assertThat(downloadProgressEvent).isEqualTo(remoteEvent);
    }

    private Message<String> createMessage(final Object event) throws JsonProcessingException {
        final Map<String, MimeType> headers = Maps.newLinkedHashMap();
        headers.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
        final String json = new ObjectMapper().writeValueAsString(event);
        final Message<String> message = MessageBuilder.withPayload(json).copyHeaders(headers).build();
        return message;
    }
}

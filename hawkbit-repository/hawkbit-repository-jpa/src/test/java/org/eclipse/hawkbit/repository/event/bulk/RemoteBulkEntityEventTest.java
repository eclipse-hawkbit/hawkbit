/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.bulk;

import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.event.remote.bulk.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.repository.event.remote.bulk.TargetTagCreatedBulkEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.hamcrest.Matchers;
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

/**
 * Test the bulk remote events.
 */
@Features("Component Tests - Repository")
@Stories("Entity Events")
public class RemoteBulkEntityEventTest extends AbstractJpaIntegrationTest {

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
    @Description("Verifies that the target tag entity reloading by remote events works")
    public void reloadTargetTagByBulkEntityRemoteEvent() throws JsonProcessingException {
        final List<TargetTag> targetTags = tagManagement.createTargetTags(
                Arrays.asList(entityFactory.generateTargetTag("tag1"), entityFactory.generateTargetTag("tag2")));
        final TargetTag targetTag = targetTags.get(0);

        final TargetTagCreatedBulkEvent bulkEvent = new TargetTagCreatedBulkEvent(targetTag, "Node");
        assertThat(bulkEvent.getEntity(), Matchers.containsInAnyOrder(new TargetTag[] { targetTag }));

        final Message<?> message = createMessage(bulkEvent);

        final TargetTagCreatedBulkEvent remoteEvent = (TargetTagCreatedBulkEvent) abstractMessageConverter
                .fromMessage(message, TargetTagCreatedBulkEvent.class);
        assertThat(remoteEvent.getEntities(), Matchers.containsInAnyOrder(new TargetTag[] { targetTag }));
    }

    @Test
    @Description("Verifies that the target tag entities reloading by remote events works")
    public void reloadTargetTagsByBulkEntityRemoteEvent() throws JsonProcessingException {
        final List<TargetTag> targetTags = tagManagement.createTargetTags(
                Arrays.asList(entityFactory.generateTargetTag("tag1"), entityFactory.generateTargetTag("tag2")));
        final TargetTag targetTag = targetTags.get(0);

        final TargetTagCreatedBulkEvent bulkEvent = new TargetTagCreatedBulkEvent(targetTag.getTenant(),
                targetTag.getClass(), targetTags, "Node");
        assertThat(bulkEvent.getEntities(), Matchers.containsInAnyOrder(targetTags.toArray()));

        final Message<?> message = createMessage(bulkEvent);

        final TargetTagCreatedBulkEvent remoteEvent = (TargetTagCreatedBulkEvent) abstractMessageConverter
                .fromMessage(message, TargetTagCreatedBulkEvent.class);
        assertThat(remoteEvent.getEntities(), Matchers.containsInAnyOrder(targetTags.toArray()));
    }

    @Test
    @Description("Verifies that the ds tag entities reloading by remote events works")
    public void reloadDsTagsByBulkEntityRemoteEvent() throws JsonProcessingException {
        final List<DistributionSetTag> dsTags = tagManagement.createDistributionSetTags(Arrays.asList(
                entityFactory.generateDistributionSetTag("tag1"), entityFactory.generateDistributionSetTag("tag2")));
        final DistributionSetTag dsTag = dsTags.get(0);

        final DistributionSetTagCreatedBulkEvent createdBulkEvent = new DistributionSetTagCreatedBulkEvent(
                dsTag.getTenant(), dsTag.getClass(), dsTags, "Node");
        assertThat(createdBulkEvent.getEntities(), Matchers.containsInAnyOrder(dsTags.toArray()));

        final Message<?> message = createMessage(createdBulkEvent);

        final DistributionSetTagCreatedBulkEvent remoteEvent = (DistributionSetTagCreatedBulkEvent) abstractMessageConverter
                .fromMessage(message, DistributionSetTagCreatedBulkEvent.class);
        assertThat(remoteEvent.getEntities(), Matchers.containsInAnyOrder(dsTags.toArray()));

    }

    @Test
    @Description("Verifies that the target tag entity reloading by remote events works")
    public void reloadDsTagByBulkEntityRemoteEvent() throws JsonProcessingException {
        final List<DistributionSetTag> dsTags = tagManagement.createDistributionSetTags(Arrays.asList(
                entityFactory.generateDistributionSetTag("tag1"), entityFactory.generateDistributionSetTag("tag2")));
        final DistributionSetTag dsTag = dsTags.get(0);

        final DistributionSetTagCreatedBulkEvent bulkEvent = new DistributionSetTagCreatedBulkEvent(dsTag, "Node");
        assertThat(bulkEvent.getEntity(), Matchers.containsInAnyOrder(new DistributionSetTag[] { dsTag }));

        final Message<?> message = createMessage(bulkEvent);

        final DistributionSetTagCreatedBulkEvent remoteEvent = (DistributionSetTagCreatedBulkEvent) abstractMessageConverter
                .fromMessage(message, DistributionSetTagCreatedBulkEvent.class);
        assertThat(remoteEvent.getEntities(), Matchers.containsInAnyOrder(new DistributionSetTag[] { dsTag }));
    }

    private Message<String> createMessage(final Object event) throws JsonProcessingException {
        final Map<String, MimeType> headers = Maps.newLinkedHashMap();
        headers.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
        final String json = new ObjectMapper().writeValueAsString(event);
        final Message<String> message = MessageBuilder.withPayload(json).copyHeaders(headers).build();
        return message;
    }
}

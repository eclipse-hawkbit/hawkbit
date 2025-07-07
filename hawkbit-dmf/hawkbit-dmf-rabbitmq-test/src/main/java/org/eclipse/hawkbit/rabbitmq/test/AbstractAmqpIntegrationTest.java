/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rabbitmq.test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.eclipse.hawkbit.repository.jpa.JpaRepositoryConfiguration;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.RabbitAvailable;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@RabbitAvailable
@ContextConfiguration(classes = { JpaRepositoryConfiguration.class, AmqpTestConfiguration.class, TestConfiguration.class })
// Dirty context is necessary to create a new vhost and recreate all necessary beans after every test class.
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.cloud.bus.enabled=true" })
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
public abstract class AbstractAmqpIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ConnectionFactory connectionFactory;
    @Autowired
    private RabbitAdmin rabbitAdmin;

    private RabbitTemplate dmfClient;

    @BeforeEach
    public void setup() {
        dmfClient = createDmfClient();
    }

    protected abstract String getExchange();

    protected RabbitTemplate getDmfClient() {
        return dmfClient;
    }

    private static final Duration AT_LEAST = Duration.ofMillis(Integer.getInteger("hawkbit.it.amqp.await.atLeastMs", 100));
    private static final Duration POLL_INTERVAL = Duration.ofMillis(Integer.getInteger("hawkbit.it.amqp.await.pollIntervalMs", 200));
    private static final Duration TIMEOUT = Duration.ofMillis(Integer.getInteger("hawkbit.it.amqp.await.timeoutMs", 5000));

    @Override
    protected ConditionFactory await() {
        return Awaitility.await().atLeast(AT_LEAST).pollInterval(POLL_INTERVAL).atMost(TIMEOUT);
    }

    protected Message createMessage(final Object payload, final MessageProperties messageProperties) {
        if (payload == null) {
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return new Message("".getBytes(), messageProperties);
        }
        return getDmfClient().getMessageConverter().toMessage(payload, messageProperties);
    }

    protected RabbitAdmin getRabbitAdmin() {
        return rabbitAdmin;
    }

    protected String getVirtualHost() {
        return connectionFactory.getVirtualHost();
    }

    private RabbitTemplate createDmfClient() {
        final RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setReceiveTimeout(TimeUnit.SECONDS.toMillis(3));
        template.setReplyTimeout(TimeUnit.SECONDS.toMillis(3));
        template.setExchange(getExchange());
        return template;
    }
}
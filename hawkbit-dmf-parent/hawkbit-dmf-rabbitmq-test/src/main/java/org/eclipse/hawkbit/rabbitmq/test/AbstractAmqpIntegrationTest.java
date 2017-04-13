/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rabbitmq.test;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionFactory;

@SpringApplicationConfiguration(classes = { RepositoryApplicationConfiguration.class, AmqpTestConfiguration.class })
// Dirty context is necessary to create a new vhost and recreate all necessary
// beans after every test class.
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractAmqpIntegrationTest extends AbstractIntegrationTest {

    @Rule
    @Autowired
    public BrokerRunning brokerRunning;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    private RabbitTemplate dmfClient;

    @Before
    public void setup() {
        dmfClient = createDmfClient();
        dmfClient.setExchange(getExchange());
    }

    protected abstract String getExchange();

    protected RabbitTemplate getDmfClient() {
        return dmfClient;
    }

    protected ConditionFactory createConditionFactory() {
        return Awaitility.await().atMost(2, SECONDS);
    }

    protected Message createMessage(final Object payload, final MessageProperties messageProperties) {
        if (payload == null) {
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return new Message(null, messageProperties);
        }
        return getDmfClient().getMessageConverter().toMessage(payload, messageProperties);
    }

    protected int getQueueMessageCount(String queueName) {
        return Integer
                .parseInt(rabbitAdmin.getQueueProperties(queueName).get(RabbitAdmin.QUEUE_MESSAGE_COUNT).toString());
    }

    protected RabbitAdmin getRabbitAdmin() {
        return rabbitAdmin;
    }

    RabbitTemplate createDmfClient() {
        final RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setReceiveTimeout(TimeUnit.SECONDS.toMillis(3));
        return template;
    }

    protected ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

}

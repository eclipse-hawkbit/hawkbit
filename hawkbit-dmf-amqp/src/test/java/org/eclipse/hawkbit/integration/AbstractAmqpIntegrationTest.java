/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.AmqpTestConfiguration;
import org.eclipse.hawkbit.amqp.AmqpProperties;
import org.eclipse.hawkbit.amqp.DmfApiConfiguration;
import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.integration.listener.DeadletterListener;
import org.eclipse.hawkbit.integration.listener.TestRabbitListener;
import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;

import com.google.common.base.Preconditions;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionFactory;

@SpringApplicationConfiguration(classes = { RepositoryApplicationConfiguration.class, AmqpTestConfiguration.class,
        DmfApiConfiguration.class })
// TODO Für jeden Test eigenen vhost siehe datenbank schema --> cleaning
public abstract class AbstractAmqpIntegrationTest extends AbstractIntegrationTest {

    @ClassRule
    // TODO: hostname muss konfigurierbar sein
    public static BrokerRunning brokerRunning = BrokerRunning.isRunning();

    @Autowired
    private RabbitAdmin dmfAdmin;

    @Autowired
    private AmqpProperties amqpProperties;

    @Autowired
    private RabbitListenerTestHarness harness;

    private final RabbitTemplate dmfClient = createDmfClient();

    private DeadletterListener deadletterListener;

    @Before
    public void setup() {
        purgeQueues();
        deadletterListener = harness.getSpy(DeadletterListener.LISTENER_ID);
        assertThat(deadletterListener).isNotNull();
        Mockito.reset(deadletterListener);
    }

    @After
    public void clear() {
        purgeQueues();
    }

    private void purgeQueues() {
        dmfAdmin.purgeQueue(amqpProperties.getReceiverQueue(), false);
        dmfAdmin.purgeQueue(amqpProperties.getDeadLetterQueue(), false);
        dmfAdmin.purgeQueue(AmqpTestConfiguration.REPLY_TO_QUEUE, true);
    }

    private ConnectionFactory rabbitConnectionFactory() {
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        // TODO: hostname muss konfigurierbar sein
        connectionFactory.setHost("localhost");
        return connectionFactory;
    }

    private RabbitTemplate createDmfClient() {
        final RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setExchange(AmqpSettings.DMF_EXCHANGE);
        return template;
    }

    protected RabbitTemplate getDmfClient() {
        return dmfClient;
    }

    protected DeadletterListener getDeadletterListener() {
        return deadletterListener;
    }

    protected RabbitAdmin getDmfAdmin() {
        return dmfAdmin;
    }

    public RabbitListenerTestHarness getHarness() {
        return harness;
    }

    protected ConditionFactory createConditionFactory() {
        return Awaitility.await().atMost(2, SECONDS);
    }

    protected Message verifyTestRabbitListener(TestRabbitListener rabbitListener, int expectedMessages) {
        Preconditions.checkArgument(expectedMessages > 0);
        createConditionFactory().until(() -> {
            Mockito.verify(rabbitListener, Mockito.times(expectedMessages)).handleMessage(Mockito.any());
        });
        return rabbitListener.getMessage();
    }

    protected void verifyDeadLetterMessages(int expectedMessages) {
        verifyTestRabbitListener(getDeadletterListener(), expectedMessages);
    }

}

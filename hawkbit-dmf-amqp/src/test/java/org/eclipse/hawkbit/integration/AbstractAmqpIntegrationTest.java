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

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.eclipse.hawkbit.AmqpTestConfiguration;
import org.eclipse.hawkbit.AmqpVHostService;
import org.eclipse.hawkbit.amqp.AmqpProperties;
import org.eclipse.hawkbit.amqp.DmfApiConfiguration;
import org.eclipse.hawkbit.integration.listener.DeadletterListener;
import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;

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
    private RabbitTemplate dmfClient;

    @Autowired
    private RabbitTemplate authenticationClient;

    @Autowired
    private AmqpVHostService amqpVHostHelper;

    @Autowired
    private RabbitAdmin dmfAdmin;

    @Autowired
    private AmqpProperties amqpProperties;

    @Autowired
    private RabbitListenerTestHarness harness;

    private DeadletterListener deadletterListener;

    @Before
    public void setup() throws MalformedURLException, URISyntaxException {
        purgeQueues();
        deadletterListener = harness.getSpy(DeadletterListener.LISTENER_ID);
        assertThat(deadletterListener).isNotNull();
        Mockito.reset(deadletterListener);
    }

    @After
    public void clear() {
        purgeQueues();
    }

    @AfterClass
    public static void cleanup() {
        AmqpVHostService.deleteCurrentVhost();
    }

    private void purgeQueues() {
        dmfAdmin.purgeQueue(amqpProperties.getReceiverQueue(), false);
        dmfAdmin.purgeQueue(amqpProperties.getDeadLetterQueue(), false);
        dmfAdmin.purgeQueue(AmqpTestConfiguration.REPLY_TO_QUEUE, true);

    }

    protected abstract String getAmqpSettings();

    protected DeadletterListener getDeadletterListener() {
        return deadletterListener;
    }

    protected RabbitAdmin getDmfAdmin() {
        return dmfAdmin;
    }

    protected RabbitTemplate getDmfClient() {
        return dmfClient;
    }

    protected RabbitTemplate getAuthenticationClient() {
        return authenticationClient;
    }

    public RabbitListenerTestHarness getHarness() {
        return harness;
    }

    protected ConditionFactory createConditionFactory() {
        return Awaitility.await().atMost(2, SECONDS);
    }

    protected void verifyDeadLetterMessages(final int expectedMessages) {
        createConditionFactory().until(() -> {
            Mockito.verify(getDeadletterListener(), Mockito.times(expectedMessages)).handleMessage(Mockito.any());
        });
    }

    protected String getCurrentVhost() {
        return amqpVHostHelper.getCurrentVhost();
    }

}

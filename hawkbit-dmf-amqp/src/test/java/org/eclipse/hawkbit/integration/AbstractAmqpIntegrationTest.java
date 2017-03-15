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
import org.eclipse.hawkbit.RabbitMqSetupService;
import org.eclipse.hawkbit.amqp.DmfApiConfiguration;
import org.eclipse.hawkbit.integration.listener.DeadletterListener;
import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.junit.Before;
import org.junit.ClassRule;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionFactory;

@SpringApplicationConfiguration(classes = { RepositoryApplicationConfiguration.class, AmqpTestConfiguration.class,
        DmfApiConfiguration.class })
// Dirty context is necessary to create a new vhost and recreate all necessary
// beans after every test class.
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractAmqpIntegrationTest extends AbstractIntegrationTest {

    @ClassRule
    public static BrokerRunning brokerRunning = BrokerRunning.isRunning();
    static {
        brokerRunning.setHostName(RabbitMqSetupService.getHostname());
    }

    @Autowired
    @Qualifier("dmfClient")
    private RabbitTemplate dmfClient;

    @Autowired
    private RabbitListenerTestHarness harness;

    private DeadletterListener deadletterListener;

    @Before
    public void setup() throws MalformedURLException, URISyntaxException {
        deadletterListener = harness.getSpy(DeadletterListener.LISTENER_ID);
        assertThat(deadletterListener).isNotNull();
        Mockito.reset(deadletterListener);
        dmfClient.setExchange(getExchange());
    }

    protected abstract String getExchange();

    protected DeadletterListener getDeadletterListener() {
        return deadletterListener;
    }

    protected RabbitTemplate getDmfClient() {
        return dmfClient;
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

}

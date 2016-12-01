/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

/**
 * {@link RabbitListenerContainerFactory} that can be configured through
 * hawkBit's {@link AmqpProperties}.
 *
 */
public class ConfigurableRabbitListenerContainerFactory extends SimpleRabbitListenerContainerFactory {
    private final AmqpProperties amqpProperties;

    /**
     * Constructor.
     * 
     * @param rabbitConnectionFactory
     *            for the container factory
     * @param amqpProperties
     *            to configure the container factory *
     */
    public ConfigurableRabbitListenerContainerFactory(final AmqpProperties amqpProperties,
            final ConnectionFactory rabbitConnectionFactory) {
        this.amqpProperties = amqpProperties;
        setErrorHandler(new ConditionalRejectingErrorHandler(
                new DelayedRequeueExceptionStrategy(amqpProperties.getRequeueDelay())));
        setDefaultRequeueRejected(true);
        setConnectionFactory(rabbitConnectionFactory);
        setMissingQueuesFatal(amqpProperties.isMissingQueuesFatal());
        setConcurrentConsumers(amqpProperties.getInitialConcurrentConsumers());
        setMaxConcurrentConsumers(amqpProperties.getMaxConcurrentConsumers());
        setPrefetchCount(amqpProperties.getPrefetchCount());

    }

    @Override
    // Exception squid:UnusedProtectedMethod - called by
    // AbstractRabbitListenerContainerFactory
    @SuppressWarnings("squid:UnusedProtectedMethod")
    protected void initializeContainer(final SimpleMessageListenerContainer instance) {
        super.initializeContainer(instance);
        instance.setDeclarationRetries(amqpProperties.getDeclarationRetries());
    }
}

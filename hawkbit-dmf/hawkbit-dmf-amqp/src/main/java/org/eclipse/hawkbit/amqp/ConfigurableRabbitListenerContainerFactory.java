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
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.util.ErrorHandler;

/**
 * {@link RabbitListenerContainerFactory} that can be configured through
 * hawkBit's {@link AmqpProperties}.
 *
 */
public class ConfigurableRabbitListenerContainerFactory extends SimpleRabbitListenerContainerFactory {
    private final int declarationRetries;

    /**
     * Constructor.
     * 
     * @param missingQueuesFatal
     *            the missingQueuesFatal to set.
     * @see SimpleMessageListenerContainer#setMissingQueuesFatal
     * @param declarationRetries
     *            The number of retries
     * @param errorHandler
     *            the error handler which should be use
     */
    public ConfigurableRabbitListenerContainerFactory(final boolean missingQueuesFatal, final int declarationRetries,
            final ErrorHandler errorHandler) {
        this.declarationRetries = declarationRetries;

        setErrorHandler(errorHandler);
        setMissingQueuesFatal(missingQueuesFatal);
    }

    @Override
    // Exception squid:UnusedProtectedMethod - called by
    // AbstractRabbitListenerContainerFactory
    @SuppressWarnings("squid:UnusedProtectedMethod")
    protected void initializeContainer(final SimpleMessageListenerContainer instance) {
        super.initializeContainer(instance);
        instance.setDeclarationRetries(declarationRetries);
    }
}

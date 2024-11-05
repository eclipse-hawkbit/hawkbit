/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.util.ErrorHandler;

/**
 * {@link RabbitListenerContainerFactory} that can be configured through
 * hawkBit's {@link AmqpProperties}.
 */
public class ConfigurableRabbitListenerContainerFactory extends SimpleRabbitListenerContainerFactory {

    private final int declarationRetries;

    /**
     * Constructor.
     *
     * @param missingQueuesFatal the missingQueuesFatal to set.
     * @param declarationRetries The number of retries
     * @param errorHandler the error handler which should be use
     * @see SimpleMessageListenerContainer#setMissingQueuesFatal
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
    protected void initializeContainer(final SimpleMessageListenerContainer instance,
            final RabbitListenerEndpoint endpoint) {
        super.initializeContainer(instance, endpoint);
        instance.setDeclarationRetries(declarationRetries);
    }
}
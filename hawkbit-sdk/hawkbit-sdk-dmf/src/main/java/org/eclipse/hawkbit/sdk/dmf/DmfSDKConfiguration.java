/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.dmf;

import java.time.Duration;
import java.util.Map;

import org.eclipse.hawkbit.sdk.dmf.amqp.DmfReceiverService;
import org.eclipse.hawkbit.sdk.dmf.amqp.DmfSenderService;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The spring AMQP configuration to use a AMQP for communication with SP update server.
 */
@Configuration
@EnableConfigurationProperties(DmfProperties.class)
@ConditionalOnProperty(prefix = DmfProperties.CONFIGURATION_PREFIX, name = "enabled", matchIfMissing = true)
public class DmfSDKConfiguration {

    @Bean
    DeviceManagement deviceManagement() {
        return new DeviceManagement();
    }

    @Bean
    DmfSenderService dmfSenderService(
            final RabbitTemplate rabbitTemplate,
            final DmfProperties dmfProperties) {
        return new DmfSenderService(rabbitTemplate, dmfProperties);
    }

    @Bean
    DmfReceiverService dmfReceiverService(
            final RabbitTemplate rabbitTemplate,
            final DmfSenderService dmfSenderService,
            final DeviceManagement deviceManagement,
            final DmfProperties dmfProperties) {
        return new DmfReceiverService(rabbitTemplate, dmfSenderService, deviceManagement, dmfProperties);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // It is necessary to define rabbitTemplate as a Bean and set
        // Jackson2JsonMessageConverter explicitly here in order to convert only
        // OUTCOMING messages to json. In case of INCOMING messages,
        // Jackson2JsonMessageConverter can not handle messages with NULL
        // payload (e.g. REQUEST_ATTRIBUTES_UPDATE), so the
        // SimpleMessageConverter is used instead per default.
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        return rabbitTemplate;
    }

    /**
     * Creates the receiver queue from update server for receiving message from update server.
     */
    @Bean
    Queue receiverConnectorQueueFromHawkBit(final DmfProperties dmfProperties) {
        return QueueBuilder.nonDurable(dmfProperties.getReceiverConnectorQueueFromSp()).autoDelete()
                .withArguments(Map.of(
                        "x-message-ttl", Duration.ofDays(1).toMillis(),
                        "x-max-length", 100_000))
                .build();
    }

    /**
     * Creates the receiver exchange for sending messages to update server.
     */
    @Bean
    FanoutExchange exchangeQueueToConnector(final DmfProperties dmfProperties) {
        return new FanoutExchange(dmfProperties.getSenderForSpExchange(), false, true);
    }

    /**
     * Create the Binding
     *
     * @return the binding and create the queue and exchange
     */
    @Bean
    Binding bindReceiverQueueToSpExchange(final DmfProperties dmfProperties) {
        return BindingBuilder.bind(receiverConnectorQueueFromHawkBit(dmfProperties))
                .to(exchangeQueueToConnector(dmfProperties));
    }

    @Configuration
    @ConditionalOnProperty(prefix = DmfProperties.CONFIGURATION_PREFIX, name = "customVhost")
    protected static class CachingConnectionFactoryInitializer {

        CachingConnectionFactoryInitializer(
                final CachingConnectionFactory connectionFactory, final DmfProperties dmfProperties) {
            connectionFactory.setVirtualHost(dmfProperties.getCustomVhost());
        }
    }

    @ConditionalOnProperty(prefix = DmfProperties.CONFIGURATION_PREFIX, name = "healthCheckEnabled")
    HealthService healthService(final DeviceManagement deviceManagement, final DmfSenderService dmfSenderService) {
        return new HealthService(deviceManagement, dmfSenderService);
    }
}
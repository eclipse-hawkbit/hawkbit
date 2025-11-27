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

import java.net.URI;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.utils.IpUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.StringUtils;

/**
 * A default implementation for the sender service. The service sends all amqp
 * message to the configured spring rabbitmq connections. The exchange is
 * extracted from the uri.
 */
@Slf4j
public class DefaultAmqpMessageSenderService extends BaseAmqpService implements AmqpMessageSenderService {

    /**
     * Constructor.
     *
     * @param rabbitTemplate the AMQP template
     */
    public DefaultAmqpMessageSenderService(final RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    @Override
    public void sendMessage(final Message message, final URI sendTo) {
        if (!IpUtil.isAmqpUri(sendTo)) {
            return;
        }

        final String exchange = sendTo.getPath().substring(1);
        final String correlationId = UUID.randomUUID().toString();

        if (isCorrelationIdEmpty(message)) {
            message.getMessageProperties().setCorrelationId(correlationId);
        }

        if (log.isTraceEnabled()) {
            log.trace("Sending message {} to exchange {} with correlationId {}", message, exchange, correlationId);
        } else {
            log.debug("Sending message to exchange {} with correlationId {}", exchange, correlationId);
        }

        getRabbitTemplate().send(exchange, "", message, new CorrelationData(correlationId));
    }

    protected static boolean isCorrelationIdEmpty(final Message message) {
        return !StringUtils.hasLength(message.getMessageProperties().getCorrelationId());
    }
}
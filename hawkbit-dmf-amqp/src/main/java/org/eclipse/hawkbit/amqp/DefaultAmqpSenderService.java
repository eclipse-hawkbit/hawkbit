/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.amqp;

import java.net.URI;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 *
 */
public class DefaultAmqpSenderService extends BaseAmqpService implements AmqpSenderService {

    /**
     * @param messageConverter
     * @param defaultTemplate
     */
    public DefaultAmqpSenderService(final RabbitTemplate defaultTemplate) {
        super(defaultTemplate.getMessageConverter(), defaultTemplate);
    }

    @Override
    public void sendMessage(final Message message, final URI uri) {
        spInternalConnectorTemplate.send(getExchangeFromAmqpUri(uri), message);
    }

}

/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;

/**
 * Class that composes a meaningful error message and enhances it with properties from failed message
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AmqpErrorMessageComposer {

    /**
     * Constructs an error message based on failed message content
     *
     * @param throwable the throwable containing failed message content
     * @return meaningful error message
     */
    public static String constructErrorMessage(final Throwable throwable) {
        final String mainErrorMsg = throwable.getCause().getMessage();
        if (throwable instanceof ListenerExecutionFailedException) {
            Collection<Message> failedMessages = ((ListenerExecutionFailedException) throwable).getFailedMessages();
            // since the intended message content is always on top of the collection, we only extract the first one
            final Message failedMessage = failedMessages.iterator().next();
            final byte[] amqpFailedMsgBody = failedMessage.getBody();
            final Map<String, Object> amqpFailedMsgHeaders = failedMessage.getMessageProperties().getHeaders();

            final String amqpFailedMsgConcatenatedHeaders = amqpFailedMsgHeaders.keySet().stream()
                    .map(key -> key + "=" + amqpFailedMsgHeaders.get(key))
                    .collect(Collectors.joining(", ", "{", "}"));
            return mainErrorMsg + new String(amqpFailedMsgBody) + amqpFailedMsgConcatenatedHeaders;
        }
        return mainErrorMsg;
    }
}
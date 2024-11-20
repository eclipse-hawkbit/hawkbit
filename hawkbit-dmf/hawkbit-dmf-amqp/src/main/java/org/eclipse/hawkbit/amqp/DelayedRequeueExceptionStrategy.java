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

import java.util.concurrent.TimeUnit;

import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAddressException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAttributeException;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.messaging.MessageHandlingException;

/**
 * Custom {@link FatalExceptionStrategy} that markes defined hawkBit internal
 * exceptions not to be requeued. In addition it throttles in case of a requeue
 * by means of blocking the processing thread for a certain amount of time. That
 * avoids a back and forth between broker and hawkBit at maximum speed.
 */
@Slf4j
public class DelayedRequeueExceptionStrategy extends ConditionalRejectingErrorHandler.DefaultExceptionStrategy {

    private final long delay;

    /**
     * @param delay in {@link TimeUnit#MILLISECONDS} before requeue.
     */
    public DelayedRequeueExceptionStrategy(final long delay) {
        this.delay = delay;
    }

    @Override
    protected boolean isUserCauseFatal(final Throwable cause) {
        if (invalidMessage(cause)) {
            return true;
        }

        log.error("Found a message that has to be requeued. Processing with delay of {}ms: ", delay, cause);

        try {
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (final InterruptedException e) {
            log.error("Delay interrupted!", e);
            Thread.currentThread().interrupt();
        }

        return false;
    }

    private static boolean invalidMessage(final Throwable cause) {
        return doesNotExist(cause) || quotaHit(cause) || invalidContent(cause) || invalidState(cause);
    }

    private static boolean invalidState(final Throwable cause) {
        return cause instanceof CancelActionNotAllowedException;
    }

    private static boolean quotaHit(final Throwable cause) {
        return cause instanceof AssignmentQuotaExceededException;
    }

    private static boolean doesNotExist(final Throwable cause) {
        return cause instanceof TenantNotExistException || cause instanceof EntityNotFoundException;
    }

    private static boolean invalidContent(final Throwable cause) {
        return isRepositoryException(cause) || isMessageException(cause);
    }

    private static boolean isRepositoryException(final Throwable cause) {
        return cause instanceof ConstraintViolationException || cause instanceof InvalidTargetAttributeException;
    }

    private static boolean isMessageException(final Throwable cause) {
        return cause instanceof InvalidTargetAddressException ||
                cause instanceof MessageConversionException ||
                cause instanceof MessageHandlingException;
    }
}

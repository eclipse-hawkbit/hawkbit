/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintViolationException;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAddressException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAttributeException;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.util.ObjectUtils;

/**
 * Custom {@link FatalExceptionStrategy} that marks defined hawkBit internal exceptions not to be re-queued.
 */
@ToString
@Slf4j
class RequeueExceptionStrategy implements FatalExceptionStrategy {

    private final List<FatalExceptionStrategy> fatalExceptionStrategies = new ArrayList<>();

    @SuppressWarnings("unchecked")
    RequeueExceptionStrategy(final List<FatalExceptionStrategy> fatalExceptionStrategies, final List<String> fatalExceptionTypes) {
        this.fatalExceptionStrategies.add(new TypeBasedFatalExceptionStrategy(
                // default, see DefaultExceptionStrategy
                MessageConversionException.class,
                org.springframework.messaging.converter.MessageConversionException.class,
                MethodArgumentResolutionException.class, NoSuchMethodException.class, ClassCastException.class,
                // invalid state
                CancelActionNotAllowedException.class,
                // quota hit
                AssignmentQuotaExceededException.class,
                // does not exist
                TenantNotExistException.class, EntityNotFoundException.class,
                // is invalid content, repository exception
                ConstraintViolationException.class, InvalidTargetAttributeException.class,
                // is invalid content, message exception
                InvalidTargetAddressException.class, MessageHandlingException.class
        ));
        if (!ObjectUtils.isEmpty(fatalExceptionTypes)) {
            // add explicitly configured fatal exception types
            fatalExceptionTypes.forEach(type -> {
                try {
                    final Class<?> clazz = Class.forName(type);
                    if (Throwable.class.isAssignableFrom(clazz)) {
                        this.fatalExceptionStrategies.add(new TypeBasedFatalExceptionStrategy((Class<? extends Throwable>) clazz));
                    } else {
                        log.warn("Fatal exception type {} is not a Throwable", type);
                    }
                } catch (final ClassNotFoundException e) {
                    log.warn("Could not find class for fatal exception type {}", type);
                }
            });
        }
        this.fatalExceptionStrategies.addAll(fatalExceptionStrategies);
        log.info("RequeueExceptionStrategy created: {}", this);
    }

    @Override
    public boolean isFatal(final Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            // default exception from DefaultExceptionStrategy
            if (isCauseFatal(cause)) {
                return true;
            }
        }

        if (log.isDebugEnabled()) {
            log.warn("Found a message that has to be re-queued", t);
        } else {
            log.warn("Found a message that has to be re-queued: {}", t.getMessage());
        }

        return false;
    }

    protected boolean isCauseFatal(final Throwable cause) {
        for (final FatalExceptionStrategy handler : fatalExceptionStrategies) {
            if (handler.isFatal(cause)) {
                return true;
            }
        }

        return false;
    }

    @ToString
    public static class TypeBasedFatalExceptionStrategy implements FatalExceptionStrategy {

        private final Class<? extends Throwable>[] types;

        @SafeVarargs
        public TypeBasedFatalExceptionStrategy(final Class<? extends Throwable>... types) {
            this.types = types;
        }

        @Override
        public boolean isFatal(final Throwable cause) {
            for (final Class<? extends Throwable> type : types) {
                if (type.isAssignableFrom(cause.getClass())) {
                    return true;
                }
            }
            return false;
        }
    }
}
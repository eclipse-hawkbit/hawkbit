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

import java.util.Optional;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.amqp.support.converter.MessageConversionException;

/**
 * An error handler for message conversion exception resulting from AMQP.
 */
public class MessageConversionExceptionHandler extends AbstractAmqpErrorHandler<MessageConversionException> {

    @Override
    public Class<MessageConversionException> getExceptionClass() {
        return MessageConversionException.class;
    }

    @Override
    public String getErrorMessage(Throwable throwable) {
        final String errorMessage = super.getErrorMessage(throwable);
        // since the detailed error message lies in the first parent of current throwable we retrieve it and append it to the errorMessage
        final Optional<String> detailedErrorMessage = getFirstAncestralErrorMessage(throwable.getCause());
        return detailedErrorMessage.isPresent() ? (detailedErrorMessage.get() + errorMessage) : errorMessage;
    }

    private Optional<String> getFirstAncestralErrorMessage(final Throwable throwable) {
        if (throwable.getCause() instanceof InvalidFormatException) {
            return Optional.of(throwable.getCause().getMessage());
        }
        return Optional.empty();
    }
}

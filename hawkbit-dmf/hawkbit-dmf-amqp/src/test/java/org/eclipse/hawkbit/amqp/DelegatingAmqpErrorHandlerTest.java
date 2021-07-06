/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.ErrorHandler;

import java.util.ArrayList;
import java.util.List;

@Feature("Unit Tests - Delegating Conditional Error Handler")
@Story("Delegating Conditional Error Handler")
public class DelegatingAmqpErrorHandlerTest {

    @Test
    @Description("Verifies that with a list of conditional error handlers, the error is delegated to specific handler.")
    public void verifyDelegationHandling(){
        List<AmqpErrorHandler> handlers = new ArrayList<>();
        handlers.add(new IllegalArgumentExceptionHandler());
        handlers.add(new IndexOutOfBoundsExceptionHandler());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new DelegatingConditionalErrorHandler(handlers, new DefaultErrorHandler())
                        .handleError(new Throwable(new IllegalArgumentException())),
                "Expected handled exception to be of type IllegalArgumentException");
    }

    @Test
    @Description("Verifies that default handler is used if no handlers are defined for the specific exception.")
    public void verifyDefaultDelegationHandling(){
        List<AmqpErrorHandler> handlers = new ArrayList<>();
        handlers.add(new IllegalArgumentExceptionHandler());
        handlers.add(new IndexOutOfBoundsExceptionHandler());
        Assertions.assertThrows(RuntimeException.class,
                () -> new DelegatingConditionalErrorHandler(handlers, new DefaultErrorHandler())
                        .handleError(new Throwable(new NullPointerException())),
                "Expected handled exception to be of type RuntimeException");
    }

    // Test class
    public class IllegalArgumentExceptionHandler implements AmqpErrorHandler {

        @Override
        public void doHandle(final Throwable t, final AmqpErrorHandlerChain chain) {
            if (t.getCause() instanceof IllegalArgumentException) {
                throw new IllegalArgumentException(t.getMessage());
            } else {
                chain.handle(t);
            }
        }
    }

    // Test class
    public class IndexOutOfBoundsExceptionHandler implements AmqpErrorHandler {

        @Override
        public void doHandle(final Throwable t, final AmqpErrorHandlerChain chain) {
            if (t.getCause() instanceof IndexOutOfBoundsException) {
                throw new IndexOutOfBoundsException(t.getMessage());
            } else {
                chain.handle(t);
            }
        }
    }

    // Test class
    public class DefaultErrorHandler implements ErrorHandler {

        @Override
        public void
        handleError(Throwable t) {
            throw new RuntimeException(t);
        }
    }
}

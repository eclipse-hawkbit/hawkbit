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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.support.converter.MessageConversionException;

@Feature("Unit Tests - Requeue Exception Strategy")
@Story("Requeue Exception Strategy")
class RequestExceptionStrategyTest {

    private final FatalExceptionStrategy requeueExceptionStrategy = new RequeueExceptionStrategy(
            List.of(new RequeueExceptionStrategy.TypeBasedFatalExceptionStrategy(
                    IllegalArgumentException.class, IndexOutOfBoundsException.class)), null);

    @Test
    @Description("Verifies that default handler is used if no handlers are defined for the specific exception.")
    void verifyDefaultFatal() {
        assertThat(requeueExceptionStrategy.isFatal(new MessageConversionException("t"))).as("Non Fatal error").isTrue();
        assertThat(requeueExceptionStrategy.isFatal(new Throwable(new MessageConversionException("t")))).as("Non Fatal error").isTrue();
    }

    @Test
    @Description("Verifies additional fatal exception types are fatal.")
    void verifyAdditionalFatal() {
        assertThat(requeueExceptionStrategy.isFatal(new IllegalArgumentException())).isTrue();
        assertThat(requeueExceptionStrategy.isFatal(new IndexOutOfBoundsException())).isTrue();
    }

    @Test
    @Description("Verifies additional fatal exception types are fatal.")
    void verifyAdditionalWrappedFatal() {
        assertThat(requeueExceptionStrategy.isFatal(new Throwable(new IllegalArgumentException()))).isTrue();
        assertThat(requeueExceptionStrategy.isFatal(new Throwable(new IndexOutOfBoundsException()))).isTrue();
    }

    @Test
    @Description("Verifies that default handler is used if no handlers are defined for the specific exception.")
    void verifyNonFatal() {
        assertThat(requeueExceptionStrategy.isFatal(new NullPointerException())).isFalse();
        assertThat(requeueExceptionStrategy.isFatal(new Throwable(new NullPointerException()))).isFalse();
    }
}
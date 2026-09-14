/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.Serial;

import org.eclipse.hawkbit.throttle.ThrottledException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies {@link ThrottledException} maps to HTTP 429, both when thrown directly and when
 * wrapped by an outer exception (as the transaction manager does when a connection cannot be
 * opened) — Spring matches {@code @ExceptionHandler} against the cause chain.
 */
class ResponseExceptionHandlerThrottleTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new RestConfiguration.ResponseExceptionHandler())
            .build();

    @Test
    void directThrottledExceptionMapsTo429() throws Exception {
        mockMvc.perform(get("/direct"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void wrappedThrottledExceptionMapsTo429() throws Exception {
        mockMvc.perform(get("/wrapped"))
                .andExpect(status().isTooManyRequests());
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/direct")
        String direct() {
            throw new ThrottledException("throttled");
        }

        @GetMapping("/wrapped")
        String wrapped() {
            throw new Wrapper(new ThrottledException("throttled"));
        }
    }

    private static final class Wrapper extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 1L;

        private Wrapper(final Throwable cause) {
            super(cause);
        }
    }
}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.result.PrintingResultHandler;
import org.springframework.util.CollectionUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public abstract class MockMvcResultPrinter {

    /**
     * Print {@link MvcResult} details to logger.
     */
    public static ResultHandler print() {
        return new ConsolePrintingResultHandler();
    }

    /**
     * An {@link PrintingResultHandler} that writes to logger
     */
    private static class ConsolePrintingResultHandler extends PrintingResultHandler {

        public ConsolePrintingResultHandler() {
            super(new ResultValuePrinter() {

                @Override
                public void printHeading(final String heading) {
                    log.debug(String.format("%20s:", heading));
                }

                @Override
                public void printValue(final String label, final Object v) {
                    Object value = v;

                    if (value != null && value.getClass().isArray()) {
                        value = CollectionUtils.arrayToList(value);
                    }
                    log.debug(String.format("%20s = %s", label, value));
                }
            });
        }
    }
}

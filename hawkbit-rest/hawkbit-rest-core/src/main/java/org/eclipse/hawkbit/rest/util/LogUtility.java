/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// generic utility for logging REST method call for auditing purposes
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogUtility {

    public static final Logger LOGGER = LoggerFactory.getLogger("REST");
    public static final Logger DEPRECATED_LOGGER = LoggerFactory.getLogger("REST.DEPRECATED");

    public static void logRequest(final String message) {
        logRequest(message, LOGGER);
    }

    public static void logRequestDeprecated(final String message) {
        logRequest(message == null ? "Deprecated request log" : message, DEPRECATED_LOGGER);
    }

    private static void logRequest(final String message, final Logger logger) {
        try {
            if (logger.isDebugEnabled()) {
                final HttpServletRequest httpServletRequest = RequestResponseContextHolder.getHttpServletRequest();
                logger.debug(
                        "[{}/{} -> {} {}] {}",
                        Optional.ofNullable(httpServletRequest.getHeader("Referer"))
                                .map(referer -> "Referer: " + referer)
                                .orElseGet(() -> "Origin: " + httpServletRequest.getHeader("Origin")), httpServletRequest.getRemoteHost(),
                        httpServletRequest.getMethod(), httpServletRequest.getRequestURI(),
                        message == null ? "Request log" : message);
            }
        } catch (final Exception e) {
            logger.error("Unexpected logging exception!", e);
        }
    }
}
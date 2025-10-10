/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.util;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogUtility {

    public static final Logger LOGGER = LoggerFactory.getLogger("DEPRECATED_USAGE");

    public static void logDeprecated(final String message) {
        try {
            if (LOGGER.isDebugEnabled()) {
                final HttpServletRequest httpServletRequest = RequestResponseContextHolder.getHttpServletRequest();
                final String remoteHost = httpServletRequest.getRemoteHost();
                final String refererOrOrigin = Optional.ofNullable(httpServletRequest.getHeader("Referer"))
                        .map(referer -> "Referer: " + referer)
                        .orElseGet(() -> "Origin: " + httpServletRequest.getHeader("Origin"));
                LOGGER.debug("[DEPRECATED] [{}/{}] {}", remoteHost, refererOrOrigin, message);
            }
        } catch (final Exception e) {
            LOGGER.error("[DEPRECATED] Unexpected logging exception!", e);
        }
    }
}

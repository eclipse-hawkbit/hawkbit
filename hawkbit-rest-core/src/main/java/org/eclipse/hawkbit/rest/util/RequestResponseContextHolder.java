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

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.NoArgsConstructor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Gives access to the request and response for the rest resources.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class RequestResponseContextHolder {

    public static HttpServletRequest getHttpServletRequest() {
        return Objects
                .requireNonNull(
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes(),
                        "Request attribute is unavailable")
                .getRequest();
    }

    public static HttpServletResponse getHttpServletResponse() {
        return Objects
                .requireNonNull(
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes(),
                        "Request attribute is unavailable")
                .getResponse();
    }
}
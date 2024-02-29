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

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Store the request and response for the rest resources.
 */
public class RequestResponseContextHolder {

    private HttpServletRequest httpServletRequest;

    private HttpServletResponse httpServletResponse;

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    @Autowired
    public void setHttpServletRequest(final HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    @Resource(name = HttpResponseFactoryBean.FACTORY_BEAN_NAME)
    public void setHttpServletResponse(final HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }
}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.util;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

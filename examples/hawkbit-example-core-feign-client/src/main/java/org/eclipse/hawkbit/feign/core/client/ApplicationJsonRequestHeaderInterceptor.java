/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.feign.core.client;

import org.springframework.http.MediaType;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * An feign request interceptor to set the defined {@code Accept} and
 * {@code Content-Type} headers for each request to {@code application/json}.
 */
public class ApplicationJsonRequestHeaderInterceptor implements RequestInterceptor {

    @Override
    public void apply(final RequestTemplate template) {
        template.header("Accept", MediaType.APPLICATION_JSON_VALUE);
        template.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    }

}

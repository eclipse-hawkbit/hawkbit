/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.client;

import org.springframework.http.MediaType;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * 
 *
 */
public class DdiAcceptedRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(final RequestTemplate template) {
        template.header("Accept", MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE,
                MediaType.TEXT_PLAIN_VALUE);
    }
}

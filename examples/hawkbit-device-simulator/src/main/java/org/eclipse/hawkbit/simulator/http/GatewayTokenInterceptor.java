/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.http;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * A feign interceptor to apply the gateway-token header to each http-request.
 * 
 * @author Michael Hirsch
 *
 */
public class GatewayTokenInterceptor implements RequestInterceptor {

    private final String gatewayToken;

    /**
     * @param gatewayToken
     *            the gatwway token to be used in the http-header
     */
    public GatewayTokenInterceptor(final String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }

    @Override
    public void apply(final RequestTemplate template) {
        template.header("Authorization", "GatewayToken " + gatewayToken);
    }
}

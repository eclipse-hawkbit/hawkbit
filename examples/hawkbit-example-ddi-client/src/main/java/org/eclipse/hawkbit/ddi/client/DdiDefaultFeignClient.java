/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.client;

import org.apache.commons.lang.Validate;
import org.eclipse.hawkbit.ddi.client.resource.RootControllerResourceClient;

import feign.Feign;
import feign.Feign.Builder;
import feign.Logger;
import feign.Logger.Level;
import feign.jackson.JacksonEncoder;

/**
 * TODO
 *
 */
public class DdiDefaultFeignClient {

    private RootControllerResourceClient rootControllerResourceClient;

    private final Builder feignBuilder;
    private final String baseUrl;
    private final String tenant;

    public DdiDefaultFeignClient(final String baseUrl, final String tenant) {
        feignBuilder = Feign.builder().contract(new IgnoreMultipleConsumersProducersSpringMvcContract())
                .requestInterceptor(new ApplicationJsonRequestHeaderInterceptor()).logLevel(Level.FULL)
                .logger(new Logger.ErrorLogger()).encoder(new JacksonEncoder()).decoder(new DdiDecoder());
        Validate.notNull(baseUrl, "A baseUrl has to be set");
        Validate.notNull(tenant, "A tenant has to be set");
        this.baseUrl = baseUrl;
        this.tenant = tenant;
    }

    public Builder getFeignBuilder() {
        return feignBuilder;
    }

    public RootControllerResourceClient getRootControllerResourceClient() {

        if (rootControllerResourceClient == null) {
            String rootControllerResourcePath = this.baseUrl + RootControllerResourceClient.PATH;
            rootControllerResourcePath = rootControllerResourcePath.replace("{tenant}", tenant);
            rootControllerResourceClient = feignBuilder.target(RootControllerResourceClient.class,
                    rootControllerResourcePath);
        }
        return rootControllerResourceClient;
    }

}

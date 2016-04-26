/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.client;

import org.eclipse.hawkbit.ddi.client.resource.RootControllerResourceClient;

import feign.Feign;
import feign.Feign.Builder;
import feign.Logger;
import feign.Logger.Level;
import feign.jackson.JacksonEncoder;

/**
 * @author Jonathan Knoblauch
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

        if (baseUrl == null) {
            throw new IllegalStateException("A baseUrl has to be set");
        }

        if (tenant == null) {
            throw new IllegalStateException("A tenant has to be set");
        }

        this.baseUrl = baseUrl;
        this.tenant = tenant;

    }

    public Builder getFeignBuilder() {
        return feignBuilder;
    }

    public RootControllerResourceClient getRootControllerResourceClient() {

        // TODO tenant null throw exception
        if (rootControllerResourceClient == null) {

            String rootControllerResourcePath = this.baseUrl + RootControllerResourceClient.PATH;
            rootControllerResourcePath = rootControllerResourcePath.replace("{tenant}", tenant);

            rootControllerResourceClient = feignBuilder.target(RootControllerResourceClient.class,
                    rootControllerResourcePath);
        }
        return rootControllerResourceClient;
    }

}

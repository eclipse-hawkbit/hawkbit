/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ddi.client;

import org.eclipse.hawkbit.ddi.client.resource.RootControllerResourceClient;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;

import feign.Feign;
import feign.Feign.Builder;
import feign.Logger;
import feign.Logger.Level;
import feign.jackson.JacksonDecoder;
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
                .logger(new Logger.ErrorLogger()).encoder(new JacksonEncoder())
                .decoder(new ResponseEntityDecoder(new JacksonDecoder()));
        this.baseUrl = baseUrl;
        this.tenant = tenant;
    }

    public Builder getFeignBuilder() {
        return feignBuilder;
    }

    public RootControllerResourceClient getRootControllerResourceClient() {

        String rootControllerResourcePath = this.baseUrl + RootControllerResourceClient.PATH;
        rootControllerResourcePath = rootControllerResourcePath.replace("{tenant}", tenant);
        // TODO tenant null throw exception
        if (rootControllerResourceClient == null) {
            rootControllerResourceClient = feignBuilder.target(RootControllerResourceClient.class,
                    rootControllerResourcePath);
        }
        return rootControllerResourceClient;
    }

}

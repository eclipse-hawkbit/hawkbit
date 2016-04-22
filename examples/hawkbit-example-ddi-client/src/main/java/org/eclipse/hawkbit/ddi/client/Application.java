/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ddi.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import feign.Contract;

/**
 * @author Jonathan Knoblauch
 *
 */
@SpringBootApplication
@EnableFeignClients
public class Application {

    @Autowired
    private DdiExampleClient ddiClient;

    public static void main(final String[] args) {
        new SpringApplicationBuilder().showBanner(false).sources(Application.class).run(args);

        // TODO .encoder(new JacksonEncoder())
        // .decoder(new ResponseEntityDecoder(new JacksonDecoder()));

    }

    // @Bean
    // public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
    // return new BasicAuthRequestInterceptor(configuration.getUsername(),
    // configuration.getPassword());
    // }

    @Bean
    public ApplicationJsonRequestHeaderInterceptor jsonHeaderInterceptor() {
        return new ApplicationJsonRequestHeaderInterceptor();
    }

    @Bean
    public Contract feignContract() {
        return new IgnoreMultipleConsumersProducersSpringMvcContract();
    }

}

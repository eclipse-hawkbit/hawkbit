/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client;

import org.eclipse.hawkbit.feign.core.client.FeignClientConfiguration;
import org.eclipse.hawkbit.mgmt.client.scenarios.CreateStartedRolloutExample;
import org.eclipse.hawkbit.mgmt.client.scenarios.GettingStartedDefaultScenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import feign.auth.BasicAuthRequestInterceptor;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(ClientConfigurationProperties.class)
@Configuration
@AutoConfigureAfter(FeignClientConfiguration.class)
@Import(FeignClientConfiguration.class)
public class Application implements CommandLineRunner {

    @Autowired
    private ClientConfigurationProperties configuration;

    @Autowired
    private GettingStartedDefaultScenario gettingStarted;

    @Autowired
    private CreateStartedRolloutExample gettingStartedRolloutScenario;

    public static void main(final String[] args) {
        new SpringApplicationBuilder().showBanner(false).sources(Application.class).run(args);
    }

    @Override
    public void run(final String... args) throws Exception {
        if (containsArg("--createrollout", args)) {
            // run the create and start rollout example
            gettingStartedRolloutScenario.run();
        } else {
            // run the getting started scenario which creates a setup of
            // distribution set and software modules to be used
            gettingStarted.run();
        }
    }

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor(configuration.getUsername(), configuration.getPassword());
    }

    @Bean
    public GettingStartedDefaultScenario gettingStartedDefaultScenario() {
        return new GettingStartedDefaultScenario();
    }

    @Bean
    public CreateStartedRolloutExample createStartedRolloutExample() {
        return new CreateStartedRolloutExample();
    }

    private boolean containsArg(final String containsArg, final String... args) {
        for (final String arg : args) {
            if (arg.equalsIgnoreCase(containsArg)) {
                return true;
            }
        }
        return false;
    }
}
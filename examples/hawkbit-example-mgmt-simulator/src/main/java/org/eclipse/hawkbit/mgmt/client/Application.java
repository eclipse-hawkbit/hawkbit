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
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtRolloutClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtTargetClientResource;
import org.eclipse.hawkbit.mgmt.client.scenarios.ConfigurableScenario;
import org.eclipse.hawkbit.mgmt.client.scenarios.CreateStartedRolloutExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;

@SpringBootApplication
@EnableFeignClients("org.eclipse.hawkbit.mgmt.client.resource")
@EnableConfigurationProperties(ClientConfigurationProperties.class)
@Configuration
@AutoConfigureAfter(FeignClientConfiguration.class)
@Import(FeignClientConfiguration.class)
public class Application implements CommandLineRunner {

    @Autowired
    private ConfigurableScenario configuredScenario;

    @Autowired
    private CreateStartedRolloutExample gettingStartedRolloutScenario;

    public static void main(final String[] args) {
        new SpringApplicationBuilder().bannerMode(Mode.OFF).sources(Application.class).run(args);
    }

    @Override
    public void run(final String... args) throws Exception {
        if (containsArg("--createrollout", args)) {
            // run the create and start rollout example
            gettingStartedRolloutScenario.run();
        } else {
            // run the configured scenario from properties
            configuredScenario.run();
        }
    }

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor(final ClientConfigurationProperties configuration) {
        return new BasicAuthRequestInterceptor(configuration.getUsername(), configuration.getPassword());
    }

    @Bean
    public ConfigurableScenario configurableScenario(final MgmtDistributionSetClientResource distributionSetResource,
            final MgmtSoftwareModuleClientResource softwareModuleResource,
            final MgmtTargetClientResource targetResource, final MgmtRolloutClientResource rolloutResource,
            final ClientConfigurationProperties clientConfigurationProperties) {
        return new ConfigurableScenario(distributionSetResource, softwareModuleResource, targetResource,
                rolloutResource, clientConfigurationProperties);
    }

    @Bean
    public CreateStartedRolloutExample createStartedRolloutExample() {
        return new CreateStartedRolloutExample();
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    private static boolean containsArg(final String containsArg, final String... args) {
        for (final String arg : args) {
            if (arg.equalsIgnoreCase(containsArg)) {
                return true;
            }
        }
        return false;
    }
}

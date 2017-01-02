/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import org.atmosphere.container.JSR356AsyncSupport;
import org.atmosphere.cpr.ApplicationConfig;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.vaadin.spring.boot.internal.VaadinServletConfiguration;
import com.vaadin.spring.boot.internal.VaadinServletConfigurationProperties;

/**
 * {@link VaadinServletConfiguration} that sets the context path for
 * {@link JSR356AsyncSupport} that registers
 * {@link SpringSecurityAtmosphereInterceptor} for spring security integration.
 */
@Configuration
@EnableConfigurationProperties(VaadinServletConfigurationProperties.class)
@Import(VaadinServletConfiguration.class)
public class AsyncVaadinServletConfiguration extends VaadinServletConfiguration {

    @Override
    @Bean
    protected ServletRegistrationBean vaadinServletRegistration() {
        return createServletRegistrationBean();
    }

    @Override
    protected void addInitParameters(final ServletRegistrationBean servletRegistrationBean) {
        super.addInitParameters(servletRegistrationBean);

        servletRegistrationBean.addInitParameter(ApplicationConfig.JSR356_MAPPING_PATH, "/UI");
        servletRegistrationBean.addInitParameter(ApplicationConfig.ATMOSPHERE_INTERCEPTORS,
                SpringSecurityAtmosphereInterceptor.class.getName());

    }
}

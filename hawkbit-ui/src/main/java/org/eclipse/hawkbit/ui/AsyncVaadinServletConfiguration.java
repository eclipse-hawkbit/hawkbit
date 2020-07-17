/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import javax.servlet.ServletException;

import org.atmosphere.container.JSR356AsyncSupport;
import org.atmosphere.cpr.ApplicationConfig;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.vaadin.server.VaadinServlet;
import com.vaadin.spring.boot.internal.VaadinServletConfiguration;
import com.vaadin.spring.boot.internal.VaadinServletConfigurationProperties;
import com.vaadin.spring.server.SpringVaadinServlet;

/**
 * {@link VaadinServletConfiguration} that sets the context path for
 * {@link JSR356AsyncSupport} that registers
 * {@link SpringSecurityAtmosphereInterceptor} for spring security integration.
 */
@Configuration
@EnableConfigurationProperties(VaadinServletConfigurationProperties.class)
@Import(VaadinServletConfiguration.class)
public class AsyncVaadinServletConfiguration extends VaadinServletConfiguration {

    /**
     * @param uiProperties
     *          UiProperties
     * @param i18n
     *         VaadinMessageSource
     *
     * @return Localized system message provider
     */
    @Bean
    public LocalizedSystemMessagesProvider localizedSystemMessagesProvider(final UiProperties uiProperties,
            final VaadinMessageSource i18n) {
        return new LocalizedSystemMessagesProvider(uiProperties, i18n);
    }

    /**
     * @param localizedSystemMessagesProvider
     *          LocalizedSystemMessagesProvider
     *
     * @return Vaadin servlet service
     */
    @Bean
    public VaadinServlet vaadinServlet(final LocalizedSystemMessagesProvider localizedSystemMessagesProvider) {
        return new SpringVaadinServlet() {
            @Override
            public void servletInitialized() throws ServletException {
                super.servletInitialized();
                getService().setSystemMessagesProvider(localizedSystemMessagesProvider);
            }
        };
    }

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

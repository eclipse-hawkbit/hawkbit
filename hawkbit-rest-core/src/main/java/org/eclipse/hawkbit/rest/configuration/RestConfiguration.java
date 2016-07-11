/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.configuration;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.rest.util.FilterHttpResponse;
import org.eclipse.hawkbit.rest.util.HttpResponseFactoryBean;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.web.context.WebApplicationContext;

/**
 * Configuration for Rest api.
 */
@Configuration
@EnableHypermediaSupport(type = { HypermediaType.HAL })
public class RestConfiguration {

    /**
     * Create filter for {@link HttpServletResponse}.
     */
    @Bean
    public FilterHttpResponse filterHttpResponse() {
        return new FilterHttpResponse();
    }

    /**
     * Create factory bean for {@link HttpServletResponse}.
     */
    @Bean
    public FactoryBean<HttpServletResponse> httpResponseFactoryBean() {
        return new HttpResponseFactoryBean();
    }

    /**
     * Create factory bean for {@link HttpServletResponse}.
     */
    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST)
    public RequestResponseContextHolder requestResponseContextHolder() {
        return new RequestResponseContextHolder();
    }
}

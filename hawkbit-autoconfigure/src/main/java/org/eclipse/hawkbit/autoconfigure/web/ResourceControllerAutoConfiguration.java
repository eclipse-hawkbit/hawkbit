/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.web;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.ddi.EnableDdiApi;
import org.eclipse.hawkbit.mgmt.annotation.EnableMgmtApi;
import org.eclipse.hawkbit.rest.util.FilterHttpResponse;
import org.eclipse.hawkbit.rest.util.HttpResponseFactoryBean;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.eclipse.hawkbit.system.annotation.EnableSystemApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.web.context.WebApplicationContext;

/**
 * Auto-Configuration for enabling the REST-Resources.
 *
 */
@Configuration
@ConditionalOnClass({ EnableDdiApi.class, EnableMgmtApi.class, EnableSystemApi.class })
@Import({ EnableDdiApi.class, EnableMgmtApi.class, EnableSystemApi.class })
public class ResourceControllerAutoConfiguration {

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
    public HttpResponseFactoryBean httpResponseFactoryBean() {
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

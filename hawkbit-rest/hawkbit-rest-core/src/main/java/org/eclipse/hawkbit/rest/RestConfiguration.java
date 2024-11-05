/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import org.eclipse.hawkbit.rest.exception.ResponseExceptionHandler;
import org.eclipse.hawkbit.rest.filter.ExcludePathAwareShallowETagFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Configuration for Rest api.
 */
@Configuration
@EnableHypermediaSupport(type = { HypermediaType.HAL })
public class RestConfiguration {

    /**
     * {@link ControllerAdvice} for mapping {@link RuntimeException}s from the
     * repository to {@link HttpStatus} codes.
     */
    @Bean
    ResponseExceptionHandler responseExceptionHandler() {
        return new ResponseExceptionHandler();
    }

    /**
     * Filter registration bean for spring etag filter.
     *
     * @return the spring filter registration bean for registering an etag
     *         filter in the filter chain
     */
    @Bean
    FilterRegistrationBean<ExcludePathAwareShallowETagFilter> eTagFilter() {

        final FilterRegistrationBean<ExcludePathAwareShallowETagFilter> filterRegBean = new FilterRegistrationBean<>();
        // Exclude the URLs for downloading artifacts, so no eTag is generated
        // in the ShallowEtagHeaderFilter, just using the SH1 hash of the
        // artifact itself as 'ETag', because otherwise the file will be copied
        // in memory!
        filterRegBean.setFilter(new ExcludePathAwareShallowETagFilter(
                "/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download",
                "/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/**",
                "/api/v1/downloadserver/**"));

        return filterRegBean;
    }
}

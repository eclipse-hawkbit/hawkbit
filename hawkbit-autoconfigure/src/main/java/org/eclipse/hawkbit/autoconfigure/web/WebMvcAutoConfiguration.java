/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * A configuration bean which disables the {@code useSuffixPatternMatch} feature
 * from Spring because it will truncate the dot in a REST URL which leads to
 * problem in case a controllerId contains dots and is a path parameter or
 * filename ending.
 */
@Configuration
public class WebMvcAutoConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void configurePathMatch(final PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(false);
        configurer.setUseRegisteredSuffixPatternMatch(false);
    }

    @Override
    public void configureContentNegotiation(final ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false);
    }
}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import org.eclipse.hawkbit.im.authentication.PermissionService;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * Enables UI components for the Management UI.
 *
 */
@Configuration
@ComponentScan
@Import(AsyncVaadinServletConfiguration.class)
@EnableConfigurationProperties(UiProperties.class)
@PropertySource("classpath:/hawkbit-ui-defaults.properties")
public class MgmtUiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SpPermissionChecker spPermissionChecker(final PermissionService permissionService) {
        return new SpPermissionChecker(permissionService);
    }

    @Bean
    @ConditionalOnMissingBean
    VaadinMessageSource messageSourceVaadin(final MessageSource source) {
        return new VaadinMessageSource(source);
    }

}

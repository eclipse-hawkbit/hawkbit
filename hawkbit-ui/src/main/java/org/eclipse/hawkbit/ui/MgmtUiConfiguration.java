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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Enables UI components for the Management UI.
 *
 */
@Configuration
@ComponentScan
@Import(AsyncVaadinServletConfiguration.class)
public class MgmtUiConfiguration {

    /**
     * @param permissionService
     *            to use in the checker
     * @return {@link SpPermissionChecker} bean
     */
    @Bean
    public SpPermissionChecker spPermissionChecker(final PermissionService permissionService) {
        return new SpPermissionChecker(permissionService);
    }

}

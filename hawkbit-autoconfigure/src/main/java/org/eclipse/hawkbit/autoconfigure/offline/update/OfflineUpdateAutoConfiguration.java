/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.offline.update;

import org.eclipse.hawkbit.offline.update.OfflineUpdateApiConfiguration;
import org.eclipse.hawkbit.offline.update.repository.OfflineUpdateDeploymentManagement;
import org.eclipse.hawkbit.offline.update.repository.OfflineUpdateDeploymentManagementImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-Configuration for enabling the offline update REST-Resources.
 */
@Configuration
@ConditionalOnProperty(prefix = "offlineUpdate", name = "enabled", matchIfMissing = true)
@ConditionalOnClass(OfflineUpdateApiConfiguration.class)
@Import(OfflineUpdateApiConfiguration.class)
public class OfflineUpdateAutoConfiguration {

    /**
     * Creates {@link OfflineUpdateDeploymentManagementImpl} bean.
     *
     * @return a new implementation of
     *         {@link OfflineUpdateDeploymentManagement}.
     */
    @Bean
    @ConditionalOnMissingBean
    OfflineUpdateDeploymentManagement offlineUpdateDeploymentManagement() {
        return new OfflineUpdateDeploymentManagementImpl();
    }
}

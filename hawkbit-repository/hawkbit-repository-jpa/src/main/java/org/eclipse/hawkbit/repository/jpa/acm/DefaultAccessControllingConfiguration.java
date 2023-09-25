/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import org.eclipse.hawkbit.repository.acm.context.ContextRunner;
import org.eclipse.hawkbit.repository.jpa.acm.context.DefaultContextRunner;
import org.eclipse.hawkbit.repository.jpa.acm.controller.DefaultAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.DistributionSetAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.DistributionSetTypeAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.TargetAccessController;
import org.eclipse.hawkbit.repository.jpa.acm.controller.TargetTypeAccessController;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Configuration class to load missing instances of
 * {@link org.eclipse.hawkbit.repository.jpa.acm.controller.AccessController}
 * for each managed entity.
 *
 * A central {@link AccessControlService} manager bean will take care of
 * collecting and coordinating all relevant access controlling components.
 */
public class DefaultAccessControllingConfiguration {

    @Bean
    AccessControlService accessControlManagement(final TenantAware tenantAware, final ContextRunner contextRunner,
            final TargetAccessController targetAccessController,
            final TargetTypeAccessController targetTypeAccessController,
            final DistributionSetAccessController distributionSetAccessController,
            final DistributionSetTypeAccessController distributionSetTypeAccessController) {
        return new AccessControlService(tenantAware, contextRunner, targetAccessController, targetTypeAccessController,
                distributionSetAccessController, distributionSetTypeAccessController);
    }

    @Bean
    @ConditionalOnMissingBean
    ContextRunner contextRunner() {
        return new DefaultContextRunner();
    }

    @Bean
    @ConditionalOnMissingBean
    TargetAccessController targetAccessControlManager() {
        return DefaultAccessController.targetAccessController();
    }

    @Bean
    @ConditionalOnMissingBean
    TargetTypeAccessController targetTypeAccessControlManager() {
        return DefaultAccessController.targetTypeAccessController();
    }

    @Bean
    @ConditionalOnMissingBean
    DistributionSetAccessController distributionSetAccessController() {
        return DefaultAccessController.distributionSetAccessController();
    }

    @Bean
    @ConditionalOnMissingBean
    DistributionSetTypeAccessController distributionSetTypeAccessController() {
        return DefaultAccessController.distributionSetTypeAccessController();
    }

}

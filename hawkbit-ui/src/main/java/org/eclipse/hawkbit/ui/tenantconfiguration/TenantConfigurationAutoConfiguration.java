/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import com.vaadin.spring.annotation.ViewScope;
import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.ui.MgmtUiConfiguration;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Enables UI components for the Tenant Configuration view
 */
@Configuration
@ConditionalOnClass(MgmtUiConfiguration.class)
public class TenantConfigurationAutoConfiguration {

    @Bean
    @ViewScope
    @Order(value = 1)
    DefaultDistributionSetTypeLayout defaultDistributionSetTypeLayout(final VaadinMessageSource i18n,
            final TenantConfigurationManagement tenantConfigurationManagement, final SystemManagement systemManagement,
            final SecurityTokenGenerator securityTokenGenerator, final SpPermissionChecker permissionCheckerChecker,
            final DistributionSetTypeManagement distributionSetTypeManagement) {
        return new DefaultDistributionSetTypeLayout(i18n, tenantConfigurationManagement, systemManagement,
                securityTokenGenerator, permissionCheckerChecker, distributionSetTypeManagement);
    }

    @Bean
    @ConditionalOnMissingBean
    @ViewScope
    @Order(value = 2)
    RepositoryConfigurationView repositoryConfigurationView(final VaadinMessageSource i18n,
            final UiProperties uiProperties, final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemManagement systemManagement, final SecurityTokenGenerator securityTokenGenerator) {
        return new RepositoryConfigurationView(i18n, uiProperties, tenantConfigurationManagement, systemManagement,
                securityTokenGenerator);
    }

    @Bean
    @ConditionalOnMissingBean
    @ViewScope
    @Order(value = 3)
    RolloutConfigurationView rolloutConfigurationView(final VaadinMessageSource i18n, final UiProperties uiProperties,
            final TenantConfigurationManagement tenantConfigurationManagement, final SystemManagement systemManagement,
            final SecurityTokenGenerator securityTokenGenerator) {
        return new RolloutConfigurationView(i18n, uiProperties, tenantConfigurationManagement, systemManagement,
                securityTokenGenerator);
    }

    @Bean
    @ConditionalOnMissingBean
    @ViewScope
    @Order(value = 4)
    AuthenticationConfigurationView authConfigurationView(final VaadinMessageSource i18n,
            final UiProperties uiProperties, final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemManagement systemManagement, final SecurityTokenGenerator securityTokenGenerator) {
        return new AuthenticationConfigurationView(i18n, uiProperties, tenantConfigurationManagement, systemManagement,
                securityTokenGenerator);
    }

    @Bean
    @ConditionalOnMissingBean
    @ViewScope
    @Order(value = 5)
    PollingConfigurationView pollingConfigurationView(final VaadinMessageSource i18n,
            final ControllerPollProperties uiProperties,
            final TenantConfigurationManagement tenantConfigurationManagement, final SystemManagement systemManagement,
            final SecurityTokenGenerator securityTokenGenerator) {
        return new PollingConfigurationView(i18n, uiProperties, tenantConfigurationManagement, systemManagement,
                securityTokenGenerator);
    }

}

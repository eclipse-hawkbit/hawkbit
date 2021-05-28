/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import java.util.List;

import org.eclipse.hawkbit.im.authentication.PermissionService;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetToProxyTargetMapper;
import org.eclipse.hawkbit.ui.common.data.suppliers.TargetFilterStateDataSupplier;
import org.eclipse.hawkbit.ui.common.data.suppliers.TargetFilterStateDataSupplierImpl;
import org.eclipse.hawkbit.ui.common.data.suppliers.TargetManagementStateDataSupplier;
import org.eclipse.hawkbit.ui.common.data.suppliers.TargetManagementStateDataSupplierImpl;
import org.eclipse.hawkbit.ui.error.HawkbitUIErrorHandler;
import org.eclipse.hawkbit.ui.error.extractors.ConstraintViolationErrorExtractor;
import org.eclipse.hawkbit.ui.error.extractors.UiErrorDetailsExtractor;
import org.eclipse.hawkbit.ui.error.extractors.UploadErrorExtractor;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.vaadin.spring.servlet.Vaadin4SpringServlet;

import com.vaadin.server.ErrorHandler;
import com.vaadin.server.SystemMessagesProvider;
import com.vaadin.server.VaadinServlet;
import com.vaadin.spring.annotation.UIScope;

/**
 * Enables UI components for the Management UI.
 *
 */
@Configuration
@ComponentScan
@EnableConfigurationProperties(UiProperties.class)
@PropertySource("classpath:/hawkbit-ui-defaults.properties")
public class MgmtUiConfiguration {

    /**
     * Permission checker for UI.
     * 
     * @param permissionService
     *            PermissionService
     *
     * @return Permission checker for UI
     */
    @Bean
    @ConditionalOnMissingBean
    SpPermissionChecker spPermissionChecker(final PermissionService permissionService) {
        return new SpPermissionChecker(permissionService);
    }

    /**
     * Utility for Vaadin messages source.
     * 
     * @param source
     *            Delegate MessageSource
     *
     * @return Vaadin messages source utility
     */
    @Bean
    @ConditionalOnMissingBean
    VaadinMessageSource messageSourceVaadin(final MessageSource source) {
        return new VaadinMessageSource(source);
    }

    /**
     * Localized system message provider bean.
     * 
     * @param uiProperties
     *            UiProperties
     * @param i18n
     *            VaadinMessageSource
     *
     * @return Localized system message provider
     */
    @Bean
    @ConditionalOnMissingBean
    SystemMessagesProvider systemMessagesProvider(final UiProperties uiProperties, final VaadinMessageSource i18n) {
        return new LocalizedSystemMessagesProvider(uiProperties, i18n);
    }

    /**
     * UI Error handler bean.
     * 
     * @return UI Error handler
     */
    @Bean
    @ConditionalOnMissingBean
    ErrorHandler uiErrorHandler(final VaadinMessageSource i18n,
            final List<UiErrorDetailsExtractor> uiErrorDetailsExtractor) {
        return new HawkbitUIErrorHandler(i18n, uiErrorDetailsExtractor);
    }

    @Bean
    UiErrorDetailsExtractor uploadErrorExtractor() {
        return new UploadErrorExtractor();
    }

    @Bean
    UiErrorDetailsExtractor constraintViolationErrorExtractor(final VaadinMessageSource i18n) {
        return new ConstraintViolationErrorExtractor(i18n);
    }

    /**
     * Vaadin4Spring servlet bean.
     *
     * @return Vaadin servlet for Spring
     */
    @Bean
    public VaadinServlet vaadinServlet() {
        return new Vaadin4SpringServlet();
    }

    @Bean
    public TargetToProxyTargetMapper targetToProxyTargetMapper(final VaadinMessageSource i18n) {
        return new TargetToProxyTargetMapper(i18n);
    }

    @Bean
    @ConditionalOnMissingBean
    @UIScope
    public TargetManagementStateDataSupplier targetManagementStateDataSupplier(final TargetManagement targetManagement,
            final TargetToProxyTargetMapper targetToProxyTargetMapper) {
        return new TargetManagementStateDataSupplierImpl(targetManagement, targetToProxyTargetMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @UIScope
    public TargetFilterStateDataSupplier targetFilterStateDataSupplier(final TargetManagement targetManagement,
            final TargetToProxyTargetMapper targetToProxyTargetMapper) {
        return new TargetFilterStateDataSupplierImpl(targetManagement, targetToProxyTargetMapper);
    }
}

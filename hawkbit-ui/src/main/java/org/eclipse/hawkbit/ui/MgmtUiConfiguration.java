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
import org.eclipse.hawkbit.ui.error.extractors.ArtifactEncryptionErrorExtractor;
import org.eclipse.hawkbit.ui.error.extractors.ConstraintViolationErrorExtractor;
import org.eclipse.hawkbit.ui.error.extractors.EntityNotFoundErrorExtractor;
import org.eclipse.hawkbit.ui.error.extractors.IncompatibleTargetTypeErrorExtractor;
import org.eclipse.hawkbit.ui.error.extractors.InsufficientPermissionErrorExtractor;
import org.eclipse.hawkbit.ui.error.extractors.InvalidDistributionSetErrorExtractor;
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
     * @param i18n
     *            VaadinMessageSource
     * @param uiErrorDetailsExtractor
     *            ui error details extractors
     *
     * @return UI Error handler
     */
    @Bean
    @ConditionalOnMissingBean
    ErrorHandler uiErrorHandler(final VaadinMessageSource i18n,
            final List<UiErrorDetailsExtractor> uiErrorDetailsExtractors) {
        return new HawkbitUIErrorHandler(i18n, uiErrorDetailsExtractors);
    }

    /**
     * UI Upload Error details extractor bean.
     *
     * @return UI Upload Error details extractor
     */
    @Bean
    UiErrorDetailsExtractor uploadErrorExtractor() {
        return new UploadErrorExtractor();
    }

    /**
     * UI ConstraintViolation Error details extractor bean.
     *
     * @param i18n
     *            VaadinMessageSource
     * @return UI ConstraintViolation Error details extractor
     */
    @Bean
    UiErrorDetailsExtractor constraintViolationErrorExtractor(final VaadinMessageSource i18n) {
        return new ConstraintViolationErrorExtractor(i18n);
    }

    /**
     * UI Entity not found Error details extractor bean.
     *
     * @param i18n
     *            VaadinMessageSource
     * @return UI EntityNotFound Error details extractor
     */
    @Bean
    UiErrorDetailsExtractor entityNotFoundErrorExtractor(final VaadinMessageSource i18n) {
        return new EntityNotFoundErrorExtractor(i18n);
    }

    /**
     * UI incompatible Target Type error details extractor bean.
     *
     * @param i18n
     *            VaadinMessageSource
     * @return UI IncompatibleTargetType Error details extractor
     */
    @Bean
    UiErrorDetailsExtractor incompatibleTargetTypeErrorExtractor(final VaadinMessageSource i18n) {
        return new IncompatibleTargetTypeErrorExtractor(i18n);
    }

    /**
     * UI Insufficient Permission Error details extractor bean.
     *
     * @param i18n
     *            VaadinMessageSource
     * @return UI InsufficientPermission Error details extractor
     */
    @Bean
    UiErrorDetailsExtractor insufficientPermissionErrorExtractor(final VaadinMessageSource i18n) {
        return new InsufficientPermissionErrorExtractor(i18n);
    }

    /**
     * Details extractor bean for action not possible because of distribution
     * set is invalid.
     *
     * @param i18n
     *            VaadinMessageSource
     * @return UI invalid distributionset Error details extractor
     */
    @Bean
    UiErrorDetailsExtractor invalidDistributionSetErrorExtractor(final VaadinMessageSource i18n) {
        return new InvalidDistributionSetErrorExtractor(i18n);
    }

    /**
     * UI Artifact ecnryption operations Error details extractor bean.
     *
     * @param i18n
     *            VaadinMessageSource
     * @return UI Artifact ecnryption operations Error details extractor
     */
    @Bean
    UiErrorDetailsExtractor artifactEncryptionErrorExtractor(final VaadinMessageSource i18n) {
        return new ArtifactEncryptionErrorExtractor(i18n);
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

    /**
     * UI target entity mapper bean.
     *
     * @param i18n
     *            VaadinMessageSource
     * @return UI target entity mapper
     */
    @Bean
    public TargetToProxyTargetMapper targetToProxyTargetMapper(final VaadinMessageSource i18n) {
        return new TargetToProxyTargetMapper(i18n);
    }

    /**
     * UI Management target data supplier bean.
     *
     * @param targetManagement
     *            TargetManagement
     * @param targetToProxyTargetMapper
     *            UI target entity mapper
     * @return UI target data supplier for Management view
     */
    @Bean
    @ConditionalOnMissingBean
    @UIScope
    public TargetManagementStateDataSupplier targetManagementStateDataSupplier(final TargetManagement targetManagement,
            final TargetToProxyTargetMapper targetToProxyTargetMapper) {
        return new TargetManagementStateDataSupplierImpl(targetManagement, targetToProxyTargetMapper);
    }

    /**
     * UI Filter target data supplier bean.
     *
     * @param targetManagement
     *            TargetManagement
     * @param targetToProxyTargetMapper
     *            UI target entity mapper
     * @return UI target data supplier for Filter view
     */
    @Bean
    @ConditionalOnMissingBean
    @UIScope
    public TargetFilterStateDataSupplier targetFilterStateDataSupplier(final TargetManagement targetManagement,
            final TargetToProxyTargetMapper targetToProxyTargetMapper) {
        return new TargetFilterStateDataSupplierImpl(targetManagement, targetToProxyTargetMapper);
    }
}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.aspects.ExceptionMappingAspectHandler;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.model.helper.CacheManagerHolder;
import org.eclipse.hawkbit.repository.model.helper.PollConfigurationHelper;
import org.eclipse.hawkbit.repository.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.model.helper.SystemManagementHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantAwareHolder;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * General configuration for the SP Repository.
 *
 *
 *
 *
 *
 */
@EnableJpaRepositories(basePackages = { "org.eclipse.hawkbit.repository" })
@EnableTransactionManagement
@EnableJpaAuditing
@EnableAspectJAutoProxy
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class RepositoryApplicationConfiguration extends JpaBaseConfiguration {

    /**
     * @return simple default {@link AsyncUncaughtExceptionHandler}
     *         implementation
     * @Bean public SimpleAsyncUncaughtExceptionHandler
     *       simpleAsyncUncaughtExceptionHandler() { return new
     *       SimpleAsyncUncaughtExceptionHandler(); }
     *
     *       /**
     * @return the {@link PollConfigurationHelper} singleton bean which holds
     *         the polling and polling overdue configuration and make it
     *         accessible in beans which cannot not be autowired or retrieve
     *         environment variables due {@link EnvironmentAware} interface.
     */
    @Bean
    public PollConfigurationHelper pollConfigurationHelper() {
        return PollConfigurationHelper.getInstance();
    }

    /**
     * @return the {@link SystemManagementHolder} singleton bean which holds the
     *         current {@link SystemManagement} service and make it accessible
     *         in beans which cannot access the service directly, e.g. JPA
     *         entities.
     */
    @Bean
    public SystemManagementHolder systemManagementHolder() {
        return SystemManagementHolder.getInstance();
    }

    /**
     * @return the {@link TenantAwareHolder} singleton bean which holds the
     *         current {@link TenantAware} service and make it accessible in
     *         beans which cannot access the service directly, e.g. JPA
     *         entities.
     */
    @Bean
    public TenantAwareHolder tenantAwareHolder() {
        return TenantAwareHolder.getInstance();
    }

    /**
     * @return the {@link SecurityTokenGeneratorHolder} singleton bean which
     *         holds the current {@link SecurityTokenGenerator} service and make
     *         it accessible in beans which cannot access the service via
     *         injection
     */
    @Bean
    public SecurityTokenGeneratorHolder securityTokenGeneratorHolder() {
        return SecurityTokenGeneratorHolder.getInstance();
    }

    /**
     * @return the singleton instance of the {@link CacheManagerHolder}
     */
    @Bean
    public CacheManagerHolder cacheManagerHolder() {
        return CacheManagerHolder.getInstance();
    }

    /**
     * 
     * @return the singleton instance of the
     *         {@link AfterTransactionCommitExecutorHolder}
     */
    @Bean
    public AfterTransactionCommitExecutorHolder afterTransactionCommitExecutorHolder() {
        return AfterTransactionCommitExecutorHolder.getInstance();
    }

    /**
     * Defines the validation processor bean.
     *
     * @return the {@link MethodValidationPostProcessor}
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }

    /**
     * @return {@link ExceptionMappingAspectHandler} aspect bean
     */
    @Bean
    public ExceptionMappingAspectHandler createRepositoryExceptionHandlerAdvice() {
        return new ExceptionMappingAspectHandler();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration#
     * createJpaVendorAdapter()
     */
    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new EclipseLinkJpaVendorAdapter();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration#
     * getVendorProperties()
     */
    @Override
    protected Map<String, Object> getVendorProperties() {

        final Map<String, Object> properties = new HashMap<>();
        // Turn off dynamic weaving to disable LTW lookup in static weaving mode
        properties.put("eclipselink.weaving", "false");
        // needed for reports
        properties.put("eclipselink.jdbc.allow-native-sql-queries", "true");
        // flyway
        properties.put("eclipselink.ddl-generation", "none");

        properties.put("eclipselink.persistence-context.flush-mode", "auto");

        return properties;
    }

    /**
     * {@link MultiTenantJpaTransactionManager} bean.
     *
     * @see org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration#transactionManager()
     * @return a new {@link PlatformTransactionManager}
     */
    @Override
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new MultiTenantJpaTransactionManager();
    }

}

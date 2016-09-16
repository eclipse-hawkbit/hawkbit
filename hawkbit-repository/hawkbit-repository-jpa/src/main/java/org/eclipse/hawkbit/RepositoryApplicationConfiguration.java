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

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.ReportManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManager;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManagerHolder;
import org.eclipse.hawkbit.repository.jpa.JpaArtifactManagement;
import org.eclipse.hawkbit.repository.jpa.JpaControllerManagement;
import org.eclipse.hawkbit.repository.jpa.JpaDeploymentManagement;
import org.eclipse.hawkbit.repository.jpa.JpaDistributionSetManagement;
import org.eclipse.hawkbit.repository.jpa.JpaEntityFactory;
import org.eclipse.hawkbit.repository.jpa.JpaReportManagement;
import org.eclipse.hawkbit.repository.jpa.JpaRolloutGroupManagement;
import org.eclipse.hawkbit.repository.jpa.JpaRolloutManagement;
import org.eclipse.hawkbit.repository.jpa.JpaSoftwareManagement;
import org.eclipse.hawkbit.repository.jpa.JpaSystemManagement;
import org.eclipse.hawkbit.repository.jpa.JpaTagManagement;
import org.eclipse.hawkbit.repository.jpa.JpaTargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.jpa.JpaTargetManagement;
import org.eclipse.hawkbit.repository.jpa.JpaTenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.JpaTenantStatsManagement;
import org.eclipse.hawkbit.repository.jpa.aspects.ExceptionMappingAspectHandler;
import org.eclipse.hawkbit.repository.jpa.configuration.MultiTenantJpaTransactionManager;
import org.eclipse.hawkbit.repository.jpa.event.JpaEventEntityManager;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.CacheManagerHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityInterceptorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.SystemManagementHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.TenantAwareHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.support.Repositories;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * General configuration for hawkBit's Repository.
 *
 */
@EnableJpaRepositories(basePackages = { "org.eclipse.hawkbit.repository.jpa" })
@EnableTransactionManagement
@EnableJpaAuditing
@EnableAspectJAutoProxy
@Configuration
@ComponentScan
@EnableConfigurationProperties(RepositoryProperties.class)
@EnableScheduling
@EntityScan("org.eclipse.hawkbit.repository.jpa.model")
@RemoteApplicationEventScan("org.eclipse.hawkbit.repository.event.remote")
public class RepositoryApplicationConfiguration extends JpaBaseConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * @return the {@link SystemSecurityContext} singleton bean which make it
     *         accessible in beans which cannot access the service directly,
     *         e.g. JPA entities.
     */
    @Bean
    public SystemSecurityContextHolder systemSecurityContextHolder() {
        return SystemSecurityContextHolder.getInstance();
    }

    /**
     * @return the {@link TenantConfigurationManagement} singleton bean which
     *         make it accessible in beans which cannot access the service
     *         directly, e.g. JPA entities.
     */
    @Bean
    public TenantConfigurationManagementHolder tenantConfigurationManagementHolder() {
        return TenantConfigurationManagementHolder.getInstance();
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
     * @return the singleton instance of the {@link EntityInterceptorHolder}
     */
    @Bean
    public EntityInterceptorHolder entityInterceptorHolder() {
        return EntityInterceptorHolder.getInstance();
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

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new EclipseLinkJpaVendorAdapter();
    }

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
        properties.put("eclipselink.logging.logger", "JavaLogger");

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

    /**
     * {@link JpaSystemManagement} bean.
     * 
     * @return a new {@link SystemManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public SystemManagement systemManagement() {
        return new JpaSystemManagement();
    }

    /**
     * {@link JpaReportManagement} bean.
     * 
     * @return a new {@link ReportManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public ReportManagement reportManagement() {
        return new JpaReportManagement();
    }

    /**
     * {@link JpaDistributionSetManagement} bean.
     * 
     * @return a new {@link DistributionSetManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public DistributionSetManagement distributionSetManagement() {
        return new JpaDistributionSetManagement();
    }

    /**
     * {@link JpaTenantStatsManagement} bean.
     * 
     * @return a new {@link TenantStatsManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantStatsManagement tenantStatsManagement() {
        return new JpaTenantStatsManagement();
    }

    /**
     * {@link JpaTenantConfigurationManagement} bean.
     * 
     * @return a new {@link TenantConfigurationManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantConfigurationManagement tenantConfigurationManagement() {
        return new JpaTenantConfigurationManagement();
    }

    /**
     * {@link JpaTenantConfigurationManagement} bean.
     * 
     * @return a new {@link TenantConfigurationManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public TargetManagement targetManagement() {
        return new JpaTargetManagement();
    }

    /**
     * {@link JpaTargetFilterQueryManagement} bean.
     * 
     * @return a new {@link TargetFilterQueryManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public TargetFilterQueryManagement targetFilterQueryManagement() {
        return new JpaTargetFilterQueryManagement();
    }

    /**
     * {@link JpaTagManagement} bean.
     * 
     * @return a new {@link TagManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public TagManagement tagManagement() {
        return new JpaTagManagement();
    }

    /**
     * {@link JpaSoftwareManagement} bean.
     * 
     * @return a new {@link SoftwareManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public SoftwareManagement softwareManagement() {
        return new JpaSoftwareManagement();
    }

    /**
     * {@link JpaRolloutManagement} bean.
     * 
     * @return a new {@link RolloutManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public RolloutManagement rolloutManagement() {
        return new JpaRolloutManagement();
    }

    /**
     * {@link JpaRolloutGroupManagement} bean.
     * 
     * @return a new {@link RolloutGroupManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public RolloutGroupManagement rolloutGroupManagement() {
        return new JpaRolloutGroupManagement();
    }

    /**
     * {@link JpaDeploymentManagement} bean.
     * 
     * @return a new {@link DeploymentManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public DeploymentManagement deploymentManagement() {
        return new JpaDeploymentManagement();
    }

    /**
     * {@link JpaControllerManagement} bean.
     * 
     * @return a new {@link ControllerManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    public ControllerManagement controllerManagement() {
        return new JpaControllerManagement();
    }

    /**
     * {@link JpaArtifactManagement} bean.
     * 
     * @return a new {@link ArtifactManagement}
     */

    @Bean
    @ConditionalOnMissingBean
    public ArtifactManagement artifactManagement() {
        return new JpaArtifactManagement();
    }

    /**
     * {@link JpaEntityFactory} bean.
     * 
     * @return a new {@link EntityFactory}
     */
    @Bean
    @ConditionalOnMissingBean
    public EntityFactory entityFactory() {
        return new JpaEntityFactory();
    }

    /**
     * {@link EventEntityManagerHolder} bean.
     * 
     * @return a new {@link EventEntityManagerHolder}
     */
    @Bean
    @ConditionalOnMissingBean
    public EventEntityManagerHolder eventEntityManagerHolder() {
        return EventEntityManagerHolder.getInstance();
    }

    /**
     * {@link EventEntityManager} bean.
     * 
     * @param aware
     *            the tenant aware
     * @return a new {@link EventEntityManager}
     */
    @Bean
    @ConditionalOnMissingBean
    public EventEntityManager eventEntityManager(final TenantAware aware) {
        return new JpaEventEntityManager(aware, new Repositories(applicationContext));
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;

    }

}

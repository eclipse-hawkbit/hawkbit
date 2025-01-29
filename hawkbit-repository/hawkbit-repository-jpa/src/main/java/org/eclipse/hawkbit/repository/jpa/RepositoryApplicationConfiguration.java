/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;

import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.cache.TenancyCacheManager;
import org.eclipse.hawkbit.repository.ArtifactEncryption;
import org.eclipse.hawkbit.repository.ArtifactEncryptionSecretsStore;
import org.eclipse.hawkbit.repository.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.BaseRepositoryTypeProvider;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.PropertiesQuotaManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryDefaultConfiguration;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutExecutor;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataBuilder;
import org.eclipse.hawkbit.repository.builder.TargetBuilder;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.builder.TargetTypeBuilder;
import org.eclipse.hawkbit.repository.event.ApplicationEventFilter;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManager;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManagerHolder;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.aspects.ExceptionMappingAspectHandler;
import org.eclipse.hawkbit.repository.jpa.autoassign.AutoAssignChecker;
import org.eclipse.hawkbit.repository.jpa.autoassign.AutoAssignScheduler;
import org.eclipse.hawkbit.repository.jpa.autocleanup.AutoActionCleanup;
import org.eclipse.hawkbit.repository.jpa.autocleanup.AutoCleanupScheduler;
import org.eclipse.hawkbit.repository.jpa.autocleanup.CleanupTask;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleMetadataBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetTypeBuilder;
import org.eclipse.hawkbit.repository.jpa.event.JpaEventEntityManager;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitDefaultServiceExecutor;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.management.JpaArtifactManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaConfirmationManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaControllerManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaDeploymentManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaDistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaDistributionSetManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaDistributionSetTagManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaDistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaRolloutGroupManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaRolloutManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaSoftwareModuleManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaSoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaSystemManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaTargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaTargetManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaTargetTagManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaTargetTypeManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaTenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaTenantStatsManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityInterceptorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.TenantAwareHolder;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetMetadataRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.HawkbitBaseRepository;
import org.eclipse.hawkbit.repository.jpa.repository.LocalArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleMetadataRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetFilterQueryRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetMetadataRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TenantConfigurationRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.rollout.RolloutScheduler;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.PauseRolloutGroupAction;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupActionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupConditionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupEvaluationManager;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.StartNextGroupRolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.ThresholdRolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.ThresholdRolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.jpa.rsql.DefaultRsqlVisitorFactory;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlParserValidationOracle;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.repository.rsql.RsqlConfigHolder;
import org.eclipse.hawkbit.repository.rsql.RsqlValidationOracle;
import org.eclipse.hawkbit.repository.rsql.RsqlVisitorFactory;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * General configuration for hawkBit's Repository.
 */
@EnableJpaRepositories(value = "org.eclipse.hawkbit.repository.jpa.repository", repositoryFactoryBeanClass = CustomBaseRepositoryFactoryBean.class)
@EnableTransactionManagement
@EnableJpaAuditing
@EnableAspectJAutoProxy
@Configuration
@EnableScheduling
@EnableRetry
@EntityScan("org.eclipse.hawkbit.repository.jpa.model")
@PropertySource("classpath:/hawkbit-jpa-defaults.properties")
@Import({ JpaConfiguration.class, RepositoryDefaultConfiguration.class, DataSourceAutoConfiguration.class, SystemManagementCacheKeyGenerator.class })
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class RepositoryApplicationConfiguration {

    /**
     * Defines the validation processor bean.
     *
     * @return the {@link MethodValidationPostProcessor}
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        final MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        // ValidatorFactory shall NOT be closed because after closing the generated Validator
        // methods shall not be called - we need the validator in future
        processor.setValidator(Validation.byDefaultProvider()
                .configure()
                .addProperty(org.hibernate.validator.BaseHibernateValidatorConfiguration.ALLOW_PARALLEL_METHODS_DEFINE_PARAMETER_CONSTRAINTS, "true")
                .buildValidatorFactory()
                .getValidator());
        return processor;
    }

    @Bean
    public BeanPostProcessor entityManagerBeanPostProcessor(
            @Autowired(required = false) final AccessController<JpaArtifact> artifactAccessController,
            @Autowired(required = false) final AccessController<JpaSoftwareModuleType> softwareModuleTypeAccessController,
            @Autowired(required = false) final AccessController<JpaSoftwareModule> softwareModuleAccessController,
            @Autowired(required = false) final AccessController<JpaDistributionSetType> distributionSetTypeAccessController,
            @Autowired(required = false) final AccessController<JpaDistributionSet> distributionSetAccessController,
            @Autowired(required = false) final AccessController<JpaTargetType> targetTypeAccessControlManager,
            @Autowired(required = false) final AccessController<JpaTarget> targetAccessControlManager,
            @Autowired(required = false) final AccessController<JpaAction> actionAccessController) {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(@NonNull final Object bean, @NonNull final String beanName)
                    throws BeansException {
                if (bean instanceof LocalArtifactRepository repo) {
                    return repo.withACM(artifactAccessController);
                } else if (bean instanceof SoftwareModuleTypeRepository repo) {
                    return repo.withACM(softwareModuleTypeAccessController);
                } else if (bean instanceof SoftwareModuleRepository repo) {
                    return repo.withACM(softwareModuleAccessController);
                } else if (bean instanceof DistributionSetTypeRepository repo) {
                    return repo.withACM(distributionSetTypeAccessController);
                } else if (bean instanceof DistributionSetRepository repo) {
                    return repo.withACM(distributionSetAccessController);
                } else if (bean instanceof TargetTypeRepository repo) {
                    return repo.withACM(targetTypeAccessControlManager);
                } else if (bean instanceof TargetRepository repo) {
                    return repo.withACM(targetAccessControlManager);
                } else if (bean instanceof ActionRepository repo) {
                    return repo.withACM(actionAccessController);
                }
                return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    PauseRolloutGroupAction pauseRolloutGroupAction(final RolloutManagement rolloutManagement,
            final RolloutGroupRepository rolloutGroupRepository, final SystemSecurityContext systemSecurityContext) {
        return new PauseRolloutGroupAction(rolloutManagement, rolloutGroupRepository, systemSecurityContext);
    }

    @Bean
    @ConditionalOnMissingBean
    StartNextGroupRolloutGroupSuccessAction startNextRolloutGroupAction(
            final RolloutGroupRepository rolloutGroupRepository, final DeploymentManagement deploymentManagement,
            final SystemSecurityContext systemSecurityContext) {
        return new StartNextGroupRolloutGroupSuccessAction(rolloutGroupRepository, deploymentManagement,
                systemSecurityContext);
    }

    @Bean
    @ConditionalOnMissingBean
    ThresholdRolloutGroupErrorCondition thresholdRolloutGroupErrorCondition(final ActionRepository actionRepository) {
        return new ThresholdRolloutGroupErrorCondition(actionRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    ThresholdRolloutGroupSuccessCondition thresholdRolloutGroupSuccessCondition(
            final ActionRepository actionRepository) {
        return new ThresholdRolloutGroupSuccessCondition(actionRepository);
    }

    @Bean
    RolloutGroupEvaluationManager evaluationManager(
            final List<RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupErrorCondition>> errorConditionEvaluators,
            final List<RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupSuccessCondition>> successConditionEvaluators,
            final List<RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupErrorAction>> errorActionEvaluators,
            final List<RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupSuccessAction>> successActionEvaluators) {
        return new RolloutGroupEvaluationManager(
                errorConditionEvaluators, successConditionEvaluators, errorActionEvaluators, successActionEvaluators);
    }

    @Bean
    @ConditionalOnMissingBean
    SystemManagementCacheKeyGenerator systemManagementCacheKeyGenerator(final TenantAware tenantAware) {
        return new SystemManagementCacheKeyGenerator(tenantAware);
    }

    @Bean
    @ConditionalOnMissingBean
    AfterTransactionCommitDefaultServiceExecutor afterTransactionCommitDefaultServiceExecutor() {
        return new AfterTransactionCommitDefaultServiceExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    RsqlValidationOracle rsqlValidationOracle() {
        return new RsqlParserValidationOracle();
    }

    @Bean
    @ConditionalOnMissingBean
    QuotaManagement staticQuotaManagement(final HawkbitSecurityProperties securityProperties) {
        return new PropertiesQuotaManagement(securityProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    RolloutStatusCache rolloutStatusCache(final TenantAware tenantAware) {
        return new RolloutStatusCache(tenantAware);
    }

    @Bean
    @ConditionalOnMissingBean
    ApplicationEventFilter applicationEventFilter(final RepositoryProperties repositoryProperties) {
        return e -> e instanceof TargetPollEvent && !repositoryProperties.isPublishTargetPollEvent();
    }

    /**
     * @param distributionSetTypeManagement to loading the {@link DistributionSetType}
     * @param softwareManagement for loading {@link DistributionSet#getModules()}
     * @return DistributionSetBuilder bean
     */
    @Bean
    DistributionSetBuilder distributionSetBuilder(final DistributionSetTypeManagement distributionSetTypeManagement,
            final SoftwareModuleManagement softwareManagement) {
        return new JpaDistributionSetBuilder(distributionSetTypeManagement, softwareManagement);
    }

    @Bean
    TargetBuilder targetBuilder(final TargetTypeManagement targetTypeManagement) {
        return new JpaTargetBuilder(targetTypeManagement);
    }

    /**
     * @param dsTypeManagement for loading {@link TargetType#getCompatibleDistributionSetTypes()}
     * @return TargetTypeBuilder bean
     */
    @Bean
    TargetTypeBuilder targetTypeBuilder(final DistributionSetTypeManagement dsTypeManagement) {
        return new JpaTargetTypeBuilder(dsTypeManagement);
    }

    @Bean
    SoftwareModuleMetadataBuilder softwareModuleMetadataBuilder(
            final SoftwareModuleManagement softwareModuleManagement) {
        return new JpaSoftwareModuleMetadataBuilder(softwareModuleManagement);
    }

    /**
     * @param softwareModuleTypeManagement for loading {@link DistributionSetType#getMandatoryModuleTypes()}
     *         and {@link DistributionSetType#getOptionalModuleTypes()}
     * @return DistributionSetTypeBuilder bean
     */
    @Bean
    DistributionSetTypeBuilder distributionSetTypeBuilder(
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        return new JpaDistributionSetTypeBuilder(softwareModuleTypeManagement);
    }

    /**
     * @param softwareModuleTypeManagement for loading {@link SoftwareModule#getType()}
     * @return SoftwareModuleBuilder bean
     */
    @Bean
    SoftwareModuleBuilder softwareModuleBuilder(final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        return new JpaSoftwareModuleBuilder(softwareModuleTypeManagement);
    }

    /**
     * @param distributionSetManagement for loading {@link Rollout#getDistributionSet()}
     * @return RolloutBuilder bean
     */
    @Bean
    RolloutBuilder rolloutBuilder(final DistributionSetManagement distributionSetManagement) {
        return new JpaRolloutBuilder(distributionSetManagement);
    }

    /**
     * @param distributionSetManagement for loading
     *         {@link TargetFilterQuery#getAutoAssignDistributionSet()}
     * @return TargetFilterQueryBuilder bean
     */
    @Bean
    TargetFilterQueryBuilder targetFilterQueryBuilder(final DistributionSetManagement distributionSetManagement) {
        return new JpaTargetFilterQueryBuilder(distributionSetManagement);
    }

    /**
     * @return the {@link SystemSecurityContext} singleton bean which make it
     *         accessible in beans which cannot access the service directly, e.g.
     *         JPA entities.
     */
    @Bean
    SystemSecurityContextHolder systemSecurityContextHolder() {
        return SystemSecurityContextHolder.getInstance();
    }

    /**
     * @return the {@link TenantConfigurationManagement} singleton bean which make
     *         it accessible in beans which cannot access the service directly, e.g.
     *         JPA entities.
     */
    @Bean
    TenantConfigurationManagementHolder tenantConfigurationManagementHolder() {
        return TenantConfigurationManagementHolder.getInstance();
    }

    /**
     * @return the {@link TenantAwareHolder} singleton bean which holds the current
     *         {@link TenantAware} service and make it accessible in beans which
     *         cannot access the service directly, e.g. JPA entities.
     */
    @Bean
    TenantAwareHolder tenantAwareHolder() {
        return TenantAwareHolder.getInstance();
    }

    /**
     * @return the {@link SecurityTokenGeneratorHolder} singleton bean which holds
     *         the current {@link SecurityTokenGenerator} service and make it
     *         accessible in beans which cannot access the service via injection
     */
    @Bean
    SecurityTokenGeneratorHolder securityTokenGeneratorHolder() {
        return SecurityTokenGeneratorHolder.getInstance();
    }

    /**
     * @return the singleton instance of the {@link EntityInterceptorHolder}
     */
    @Bean
    EntityInterceptorHolder entityInterceptorHolder() {
        return EntityInterceptorHolder.getInstance();
    }

    /**
     * @return the singleton instance of the {@link AfterTransactionCommitExecutorHolder}
     */
    @Bean
    AfterTransactionCommitExecutorHolder afterTransactionCommitExecutorHolder() {
        return AfterTransactionCommitExecutorHolder.getInstance();
    }

    /**
     * @return {@link ExceptionMappingAspectHandler} aspect bean
     */
    @Bean
    ExceptionMappingAspectHandler createRepositoryExceptionHandlerAdvice() {
        return new ExceptionMappingAspectHandler();
    }

    /**
     * Default {@link BaseRepositoryTypeProvider} bean always provides the NoCountBaseRepository
     *
     * @return a {@link BaseRepositoryTypeProvider} bean
     */
    @Bean
    @ConditionalOnMissingBean
    BaseRepositoryTypeProvider baseRepositoryTypeProvider() {
        return new HawkbitBaseRepository.RepositoryTypeProvider();
    }

    /**
     * {@link JpaSystemManagement} bean.
     *
     * @return a new {@link SystemManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    SystemManagement systemManagement(
            final TargetRepository targetRepository, final TargetTypeRepository targetTypeRepository,
            final TargetTagRepository targetTagRepository, final TargetFilterQueryRepository targetFilterQueryRepository,
            final SoftwareModuleRepository softwareModuleRepository, final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final DistributionSetRepository distributionSetRepository, final DistributionSetTypeRepository distributionSetTypeRepository,
            final DistributionSetTagRepository distributionSetTagRepository, final RolloutRepository rolloutRepository,
            final TenantConfigurationRepository tenantConfigurationRepository, final TenantMetaDataRepository tenantMetaDataRepository,
            final TenantStatsManagement systemStatsManagement, final SystemManagementCacheKeyGenerator currentTenantCacheKeyGenerator,
            final SystemSecurityContext systemSecurityContext, final TenantAware tenantAware, final PlatformTransactionManager txManager,
            final TenancyCacheManager cacheManager, final RolloutStatusCache rolloutStatusCache,
            final EntityManager entityManager, final RepositoryProperties repositoryProperties,
            final JpaProperties properties) {
        return new JpaSystemManagement(targetRepository, targetTypeRepository, targetTagRepository,
                targetFilterQueryRepository, softwareModuleRepository, softwareModuleTypeRepository, distributionSetRepository,
                distributionSetTypeRepository, distributionSetTagRepository, rolloutRepository, tenantConfigurationRepository,
                tenantMetaDataRepository, systemStatsManagement, currentTenantCacheKeyGenerator, systemSecurityContext,
                tenantAware, txManager, cacheManager, rolloutStatusCache, entityManager, repositoryProperties, properties);
    }

    /**
     * {@link JpaDistributionSetManagement} bean.
     *
     * @return a new {@link DistributionSetManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    DistributionSetManagement distributionSetManagement(
            final EntityManager entityManager,
            final DistributionSetRepository distributionSetRepository,
            final DistributionSetTagManagement distributionSetTagManagement, final SystemManagement systemManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement, final QuotaManagement quotaManagement,
            final DistributionSetMetadataRepository distributionSetMetadataRepository,
            final TargetRepository targetRepository,
            final TargetFilterQueryRepository targetFilterQueryRepository, final ActionRepository actionRepository,
            final SystemSecurityContext systemSecurityContext, final TenantConfigurationManagement tenantConfigurationManagement,
            final VirtualPropertyReplacer virtualPropertyReplacer, final SoftwareModuleRepository softwareModuleRepository,
            final DistributionSetTagRepository distributionSetTagRepository,
            final JpaProperties properties, final RepositoryProperties repositoryProperties) {
        return new JpaDistributionSetManagement(entityManager, distributionSetRepository, distributionSetTagManagement,
                systemManagement, distributionSetTypeManagement, quotaManagement, distributionSetMetadataRepository,
                targetRepository, targetFilterQueryRepository, actionRepository,
                TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement),
                virtualPropertyReplacer, softwareModuleRepository, distributionSetTagRepository,
                properties.getDatabase(), repositoryProperties);
    }

    /**
     * {@link JpaDistributionSetManagement} bean.
     *
     * @return a new {@link DistributionSetManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    DistributionSetTypeManagement distributionSetTypeManagement(
            final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final DistributionSetRepository distributionSetRepository, final TargetTypeRepository targetTypeRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final JpaProperties properties,
            final QuotaManagement quotaManagement) {
        return new JpaDistributionSetTypeManagement(distributionSetTypeRepository, softwareModuleTypeRepository,
                distributionSetRepository, targetTypeRepository, virtualPropertyReplacer, properties.getDatabase(),
                quotaManagement);
    }

    /**
     * {@link JpaTargetTypeManagement} bean.
     *
     * @return a new {@link TargetTypeManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TargetTypeManagement targetTypeManagement(final TargetTypeRepository targetTypeRepository,
            final TargetRepository targetRepository, final DistributionSetTypeRepository distributionSetTypeRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final JpaProperties properties,
            final QuotaManagement quotaManagement) {
        return new JpaTargetTypeManagement(targetTypeRepository, targetRepository, distributionSetTypeRepository,
                virtualPropertyReplacer, properties.getDatabase(), quotaManagement);
    }

    /**
     * {@link JpaTenantStatsManagement} bean.
     *
     * @return a new {@link TenantStatsManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TenantStatsManagement tenantStatsManagement(
            final TargetRepository targetRepository, final LocalArtifactRepository artifactRepository, final ActionRepository actionRepository,
            final TenantAware tenantAware) {
        return new JpaTenantStatsManagement(targetRepository, artifactRepository, actionRepository, tenantAware);
    }

    /**
     * {@link JpaTenantConfigurationManagement} bean.
     *
     * @return a new {@link TenantConfigurationManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TenantConfigurationManagement tenantConfigurationManagement(
            final TenantConfigurationRepository tenantConfigurationRepository,
            final TenantConfigurationProperties tenantConfigurationProperties,
            final CacheManager cacheManager, final AfterTransactionCommitExecutor afterCommitExecutor,
            final ApplicationContext applicationContext) {
        return new JpaTenantConfigurationManagement(tenantConfigurationRepository, tenantConfigurationProperties,
                cacheManager, afterCommitExecutor, applicationContext);
    }

    /**
     * {@link JpaTargetManagement} bean.
     *
     * @return a new {@link JpaTargetManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TargetManagement targetManagement(final EntityManager entityManager, final QuotaManagement quotaManagement,
            final TargetRepository targetRepository, final TargetMetadataRepository targetMetadataRepository,
            final RolloutGroupRepository rolloutGroupRepository,
            final TargetFilterQueryRepository targetFilterQueryRepository,
            final TargetTypeRepository targetTypeRepository, final TargetTagRepository targetTagRepository,
            final EventPublisherHolder eventPublisherHolder, final TenantAware tenantAware,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final JpaProperties properties, final DistributionSetManagement distributionSetManagement) {
        return new JpaTargetManagement(entityManager, distributionSetManagement, quotaManagement, targetRepository,
                targetTypeRepository, targetMetadataRepository, rolloutGroupRepository, targetFilterQueryRepository,
                targetTagRepository, eventPublisherHolder, tenantAware, virtualPropertyReplacer,
                properties.getDatabase());
    }

    /**
     * {@link JpaTargetFilterQueryManagement} bean.
     *
     * @param targetFilterQueryRepository holding {@link TargetFilterQuery} entities
     * @param targetManagement managing {@link Target} entities
     * @param virtualPropertyReplacer for RSQL handling
     * @param distributionSetManagement for auto assign DS access
     * @param quotaManagement to access quotas
     * @param properties JPA properties
     * @return a new {@link TargetFilterQueryManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TargetFilterQueryManagement targetFilterQueryManagement(
            final TargetFilterQueryRepository targetFilterQueryRepository, final TargetManagement targetManagement,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final DistributionSetManagement distributionSetManagement, final QuotaManagement quotaManagement,
            final JpaProperties properties, final TenantConfigurationManagement tenantConfigurationManagement,
            final RepositoryProperties repositoryProperties,
            final SystemSecurityContext systemSecurityContext, final ContextAware contextAware, final AuditorAware<String> auditorAware) {
        return new JpaTargetFilterQueryManagement(targetFilterQueryRepository, targetManagement,
                virtualPropertyReplacer, distributionSetManagement, quotaManagement, properties.getDatabase(),
                tenantConfigurationManagement, repositoryProperties, systemSecurityContext, contextAware, auditorAware);
    }

    /**
     * {@link JpaTargetTagManagement} bean.
     *
     * @return a new {@link TargetTagManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TargetTagManagement targetTagManagement(final TargetTagRepository targetTagRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final JpaProperties properties) {
        return new JpaTargetTagManagement(targetTagRepository, virtualPropertyReplacer,
                properties.getDatabase());
    }

    /**
     * {@link JpaDistributionSetTagManagement} bean.
     *
     * @return a new {@link JpaDistributionSetTagManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    DistributionSetTagManagement distributionSetTagManagement(
            final DistributionSetTagRepository distributionSetTagRepository,
            final DistributionSetRepository distributionSetRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final JpaProperties properties) {
        return new JpaDistributionSetTagManagement(
                distributionSetTagRepository, distributionSetRepository, virtualPropertyReplacer, properties.getDatabase());
    }

    /**
     * {@link JpaSoftwareModuleManagement} bean.
     *
     * @return a new {@link SoftwareModuleManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    SoftwareModuleManagement softwareModuleManagement(final EntityManager entityManager,
            final DistributionSetRepository distributionSetRepository,
            final SoftwareModuleRepository softwareModuleRepository,
            final SoftwareModuleMetadataRepository softwareModuleMetadataRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository, final AuditorAware<String> auditorProvider,
            final ArtifactManagement artifactManagement, final QuotaManagement quotaManagement,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final JpaProperties properties) {
        return new JpaSoftwareModuleManagement(entityManager, distributionSetRepository, softwareModuleRepository,
                softwareModuleMetadataRepository, softwareModuleTypeRepository, auditorProvider, artifactManagement,
                quotaManagement, virtualPropertyReplacer,
                properties.getDatabase());
    }

    /**
     * {@link JpaSoftwareModuleTypeManagement} bean.
     *
     * @return a new {@link SoftwareModuleTypeManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    SoftwareModuleTypeManagement softwareModuleTypeManagement(
            final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final SoftwareModuleRepository softwareModuleRepository,
            final JpaProperties properties) {
        return new JpaSoftwareModuleTypeManagement(distributionSetTypeRepository, softwareModuleTypeRepository,
                virtualPropertyReplacer, softwareModuleRepository, properties.getDatabase());
    }

    @Bean
    @ConditionalOnMissingBean
    RolloutHandler rolloutHandler(final TenantAware tenantAware, final RolloutManagement rolloutManagement,
            final RolloutExecutor rolloutExecutor, final LockRegistry lockRegistry,
            final PlatformTransactionManager txManager, final ContextAware contextAware) {
        return new JpaRolloutHandler(tenantAware, rolloutManagement, rolloutExecutor, lockRegistry, txManager, contextAware);
    }

    @Bean
    @ConditionalOnMissingBean
    RolloutExecutor rolloutExecutor(
            final ActionRepository actionRepository, final RolloutGroupRepository rolloutGroupRepository,
            final RolloutTargetGroupRepository rolloutTargetGroupRepository,
            final RolloutRepository rolloutRepository, final TargetManagement targetManagement,
            final DeploymentManagement deploymentManagement, final RolloutGroupManagement rolloutGroupManagement,
            final RolloutManagement rolloutManagement, final QuotaManagement quotaManagement,
            final RolloutGroupEvaluationManager evaluationManager, final RolloutApprovalStrategy rolloutApprovalStrategy,
            final EntityManager entityManager, final PlatformTransactionManager txManager,
            final AfterTransactionCommitExecutor afterCommit, final EventPublisherHolder eventPublisherHolder,
            final TenantAware tenantAware, final RepositoryProperties repositoryProperties) {
        return new JpaRolloutExecutor(actionRepository, rolloutGroupRepository, rolloutTargetGroupRepository,
                rolloutRepository, targetManagement, deploymentManagement, rolloutGroupManagement, rolloutManagement,
                quotaManagement, evaluationManager, rolloutApprovalStrategy, entityManager, txManager, afterCommit,
                eventPublisherHolder, tenantAware, repositoryProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    RolloutManagement rolloutManagement(
            final RolloutRepository rolloutRepository,
            final RolloutGroupRepository rolloutGroupRepository,
            final RolloutApprovalStrategy rolloutApprovalStrategy,
            final StartNextGroupRolloutGroupSuccessAction startNextRolloutGroupAction,
            final RolloutStatusCache rolloutStatusCache,
            final ActionRepository actionRepository,
            final TargetManagement targetManagement,
            final DistributionSetManagement distributionSetManagement,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final QuotaManagement quotaManagement,
            final AfterTransactionCommitExecutor afterCommit, final EventPublisherHolder eventPublisherHolder,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final SystemSecurityContext systemSecurityContext, final ContextAware contextAware, final JpaProperties properties,
            final RepositoryProperties repositoryProperties) {
        return new JpaRolloutManagement(rolloutRepository, rolloutGroupRepository, rolloutApprovalStrategy,
                startNextRolloutGroupAction, rolloutStatusCache, actionRepository, targetManagement,
                distributionSetManagement, tenantConfigurationManagement, quotaManagement, afterCommit,
                eventPublisherHolder, virtualPropertyReplacer, systemSecurityContext, contextAware, properties.getDatabase(),
                repositoryProperties);
    }

    /**
     * {@link DefaultRolloutApprovalStrategy} bean.
     *
     * @return a new {@link RolloutApprovalStrategy}
     */
    @Bean
    @ConditionalOnMissingBean
    RolloutApprovalStrategy rolloutApprovalStrategy(final UserAuthoritiesResolver userAuthoritiesResolver,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemSecurityContext systemSecurityContext) {
        return new DefaultRolloutApprovalStrategy(userAuthoritiesResolver, tenantConfigurationManagement,
                systemSecurityContext);
    }

    /**
     * {@link JpaRolloutGroupManagement} bean.
     *
     * @return a new {@link RolloutGroupManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    RolloutGroupManagement rolloutGroupManagement(final RolloutGroupRepository rolloutGroupRepository,
            final RolloutRepository rolloutRepository, final ActionRepository actionRepository,
            final TargetRepository targetRepository, final EntityManager entityManager,
            final VirtualPropertyReplacer virtualPropertyReplacer, final RolloutStatusCache rolloutStatusCache,
            final JpaProperties properties) {
        return new JpaRolloutGroupManagement(rolloutGroupRepository, rolloutRepository, actionRepository,
                targetRepository, entityManager, virtualPropertyReplacer, rolloutStatusCache, properties.getDatabase());
    }

    /**
     * {@link JpaDeploymentManagement} bean.
     *
     * @return a new {@link DeploymentManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    DeploymentManagement deploymentManagement(final EntityManager entityManager,
            final ActionRepository actionRepository,
            final DistributionSetManagement distributionSetManagement, final TargetRepository targetRepository,
            final ActionStatusRepository actionStatusRepository, final AuditorAware<String> auditorProvider,
            final EventPublisherHolder eventPublisherHolder, final AfterTransactionCommitExecutor afterCommit,
            final VirtualPropertyReplacer virtualPropertyReplacer, final PlatformTransactionManager txManager,
            final TenantConfigurationManagement tenantConfigurationManagement, final QuotaManagement quotaManagement,
            final SystemSecurityContext systemSecurityContext, final TenantAware tenantAware, final AuditorAware<String> auditorAware,
            final JpaProperties properties, final RepositoryProperties repositoryProperties) {
        return new JpaDeploymentManagement(entityManager, actionRepository, distributionSetManagement, targetRepository, actionStatusRepository,
                auditorProvider,
                eventPublisherHolder, afterCommit, virtualPropertyReplacer, txManager, tenantConfigurationManagement,
                quotaManagement, systemSecurityContext, tenantAware, auditorAware, properties.getDatabase(), repositoryProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    ConfirmationManagement confirmationManagement(final TargetRepository targetRepository,
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository,
            final RepositoryProperties repositoryProperties, final QuotaManagement quotaManagement,
            final EntityManager entityManager, final EntityFactory entityFactory) {
        return new JpaConfirmationManagement(targetRepository, actionRepository, actionStatusRepository,
                repositoryProperties, quotaManagement, entityManager, entityFactory);
    }

    /**
     * {@link JpaControllerManagement} bean.
     *
     * @return a new {@link ControllerManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    ControllerManagement controllerManagement(
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository, final QuotaManagement quotaManagement,
            final RepositoryProperties repositoryProperties,
            final TargetRepository targetRepository, final TargetTypeManagement targetTypeManagement,
            final DeploymentManagement deploymentManagement, final ConfirmationManagement confirmationManagement,
            final SoftwareModuleRepository softwareModuleRepository, final SoftwareModuleMetadataRepository softwareModuleMetadataRepository,
            final DistributionSetManagement distributionSetManagement,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final PlatformTransactionManager txManager, final EntityFactory entityFactory, final EntityManager entityManager,
            final AfterTransactionCommitExecutor afterCommit, final EventPublisherHolder eventPublisherHolder,
            final SystemSecurityContext systemSecurityContext, final TenantAware tenantAware,
            final ScheduledExecutorService executorService) {
        return new JpaControllerManagement(actionRepository, actionStatusRepository, quotaManagement, repositoryProperties,
                targetRepository, targetTypeManagement, deploymentManagement, confirmationManagement, softwareModuleRepository,
                softwareModuleMetadataRepository, distributionSetManagement, tenantConfigurationManagement, txManager,
                entityFactory, entityManager, afterCommit, eventPublisherHolder, systemSecurityContext, tenantAware,
                executorService);
    }

    @Bean
    @ConditionalOnMissingBean
    ArtifactManagement artifactManagement(
            final EntityManager entityManager, final PlatformTransactionManager txManager,
            final LocalArtifactRepository localArtifactRepository, final SoftwareModuleRepository softwareModuleRepository,
            final Optional<ArtifactRepository> artifactRepository,
            final QuotaManagement quotaManagement, final TenantAware tenantAware) {
        return new JpaArtifactManagement(
                entityManager, txManager, localArtifactRepository, softwareModuleRepository, artifactRepository.orElse(null),
                quotaManagement, tenantAware);
    }

    /**
     * {@link JpaEntityFactory} bean.
     *
     * @return a new {@link EntityFactory}
     */
    @Bean
    @ConditionalOnMissingBean
    EntityFactory entityFactory(
            final TargetBuilder targetBuilder, final TargetTypeBuilder targetTypeBuilder,
            final TargetFilterQueryBuilder targetFilterQueryBuilder,
            final SoftwareModuleBuilder softwareModuleBuilder, final SoftwareModuleMetadataBuilder softwareModuleMetadataBuilder,
            final DistributionSetBuilder distributionSetBuilder, final DistributionSetTypeBuilder distributionSetTypeBuilder,
            final RolloutBuilder rolloutBuilder) {
        return new JpaEntityFactory(targetBuilder, targetTypeBuilder, targetFilterQueryBuilder, softwareModuleBuilder,
                softwareModuleMetadataBuilder, distributionSetBuilder, distributionSetTypeBuilder, rolloutBuilder);
    }

    /**
     * {@link EventEntityManagerHolder} bean.
     *
     * @return a new {@link EventEntityManagerHolder}
     */
    @Bean
    @ConditionalOnMissingBean
    EventEntityManagerHolder eventEntityManagerHolder() {
        return EventEntityManagerHolder.getInstance();
    }

    /**
     * {@link EventEntityManager} bean.
     *
     * @param aware the tenant aware
     * @param entityManager the entity manager
     * @return a new {@link EventEntityManager}
     */
    @Bean
    @ConditionalOnMissingBean
    EventEntityManager eventEntityManager(final TenantAware aware, final EntityManager entityManager) {
        return new JpaEventEntityManager(aware, entityManager);
    }

    /**
     * {@link AutoAssignChecker} bean.
     *
     * @param targetFilterQueryManagement to get all target filter queries
     * @param targetManagement to get targets
     * @param deploymentManagement to assign distribution sets to targets
     * @param transactionManager to run transactions
     * @return a new {@link AutoAssignChecker}
     */
    @Bean
    @ConditionalOnMissingBean
    AutoAssignExecutor autoAssignExecutor(final TargetFilterQueryManagement targetFilterQueryManagement,
            final TargetManagement targetManagement, final DeploymentManagement deploymentManagement,
            final PlatformTransactionManager transactionManager, final ContextAware contextAware) {
        return new AutoAssignChecker(targetFilterQueryManagement, targetManagement, deploymentManagement,
                transactionManager, contextAware);
    }

    /**
     * {@link AutoAssignScheduler} bean.
     * <p/>
     * Note: does not activate in test profile, otherwise it is hard to test the
     * auto assign functionality.
     *
     * @param systemManagement to find all tenants
     * @param systemSecurityContext to run as system
     * @param autoAssignExecutor to run a check as tenant
     * @param lockRegistry to lock the tenant for auto assignment
     * @return a new {@link AutoAssignChecker}
     */
    @Bean
    @ConditionalOnMissingBean
    // don't active the auto assign scheduler in test, otherwise it is hard to test
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.autoassign.scheduler", name = "enabled", matchIfMissing = true)
    AutoAssignScheduler autoAssignScheduler(final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext, final AutoAssignExecutor autoAssignExecutor,
            final LockRegistry lockRegistry) {
        return new AutoAssignScheduler(systemManagement, systemSecurityContext, autoAssignExecutor, lockRegistry);
    }

    /**
     * {@link AutoActionCleanup} bean.
     *
     * @param deploymentManagement Deployment management service
     * @param configManagement Tenant configuration service
     * @return a new {@link AutoActionCleanup} bean
     */
    @Bean
    CleanupTask actionCleanup(final DeploymentManagement deploymentManagement,
            final TenantConfigurationManagement configManagement) {
        return new AutoActionCleanup(deploymentManagement, configManagement);
    }

    /**
     * {@link AutoCleanupScheduler} bean.
     *
     * @param systemManagement to find all tenants
     * @param systemSecurityContext to run as system
     * @param lockRegistry to lock the tenant for auto assignment
     * @param cleanupTasks a list of cleanup tasks
     * @return a new {@link AutoCleanupScheduler} bean
     */
    @Bean
    @ConditionalOnMissingBean
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.autocleanup.scheduler", name = "enabled", matchIfMissing = true)
    AutoCleanupScheduler autoCleanupScheduler(final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext, final LockRegistry lockRegistry,
            final List<CleanupTask> cleanupTasks) {
        return new AutoCleanupScheduler(systemManagement, systemSecurityContext, lockRegistry, cleanupTasks);
    }

    /**
     * {@link RolloutScheduler} bean.
     * <p/>
     * Note: does not activate in test profile, otherwise it is hard to test the
     * rollout handling functionality.
     *
     * @param systemManagement to find all tenants
     * @param rolloutHandler to run the rollout handler
     * @param systemSecurityContext to run as system
     * @return a new {@link RolloutScheduler} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.rollout.scheduler", name = "enabled", matchIfMissing = true)
    RolloutScheduler rolloutScheduler(final SystemManagement systemManagement,
                                      final RolloutHandler rolloutHandler, final SystemSecurityContext systemSecurityContext, @Value("${hawkbit.rollout.executor.thread-pool.size:1}") int threadPoolSize) {
        return new RolloutScheduler(rolloutHandler, systemManagement, systemSecurityContext,  threadPoolSize);
    }

    /**
     * Creates the {@link RsqlVisitorFactory} bean.
     *
     * @return A new {@link RsqlVisitorFactory} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    RsqlVisitorFactory rsqlVisitorFactory() {
        return new DefaultRsqlVisitorFactory();
    }

    /**
     * Obtains the {@link RsqlConfigHolder} bean.
     *
     * @return The {@link RsqlConfigHolder} singleton.
     */
    @Bean
    RsqlConfigHolder rsqlVisitorFactoryHolder() {
        return RsqlConfigHolder.getInstance();
    }

    /**
     * {@link JpaDistributionSetInvalidationManagement} bean.
     *
     * @return a new {@link JpaDistributionSetInvalidationManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    JpaDistributionSetInvalidationManagement distributionSetInvalidationManagement(
            final DistributionSetManagement distributionSetManagement, final RolloutManagement rolloutManagement,
            final DeploymentManagement deploymentManagement,
            final TargetFilterQueryManagement targetFilterQueryManagement, final ActionRepository actionRepository,
            final PlatformTransactionManager txManager, final RepositoryProperties repositoryProperties,
            final TenantAware tenantAware, final LockRegistry lockRegistry,
            final SystemSecurityContext systemSecurityContext) {
        return new JpaDistributionSetInvalidationManagement(distributionSetManagement, rolloutManagement,
                deploymentManagement, targetFilterQueryManagement, actionRepository, txManager, repositoryProperties,
                tenantAware, lockRegistry, systemSecurityContext);
    }

    /**
     * Default artifact encryption service bean that internally uses
     * {@link ArtifactEncryption} and {@link ArtifactEncryptionSecretsStore} beans
     * for {@link SoftwareModule} artifacts encryption/decryption
     *
     * @return a {@link ArtifactEncryptionService} bean
     */
    @Bean
    @ConditionalOnMissingBean
    ArtifactEncryptionService artifactEncryptionService() {
        return ArtifactEncryptionService.getInstance();
    }
}

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

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;

import io.micrometer.core.instrument.MeterRegistry;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.PropertiesQuotaManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConfiguration;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutExecutor;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.artifact.encryption.ArtifactEncryption;
import org.eclipse.hawkbit.repository.artifact.encryption.ArtifactEncryptionSecretsStore;
import org.eclipse.hawkbit.repository.artifact.encryption.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.builder.RolloutBuilder;
import org.eclipse.hawkbit.repository.builder.TargetBuilder;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryBuilder;
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
import org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.jpa.cluster.DistributedLockRepository;
import org.eclipse.hawkbit.repository.jpa.cluster.LockProperties;
import org.eclipse.hawkbit.repository.jpa.event.JpaEventEntityManager;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitDefaultServiceExecutor;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.utils.ExceptionMapper;
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
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.LocalArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.rollout.RolloutScheduler;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.PauseRolloutGroupAction;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupActionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupConditionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupEvaluationManager;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.StartNextGroupRolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.ThresholdRolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.ThresholdRolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.MethodAuthorizationDeniedHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * General configuration for hawkBit's Repository.
 */
@EnableJpaRepositories(value = "org.eclipse.hawkbit.repository.jpa.repository", repositoryFactoryBeanClass = HawkbitBaseRepositoryFactoryBean.class)
@EnableTransactionManagement
@EnableJpaAuditing
@EnableAspectJAutoProxy
@Configuration
@EnableScheduling
@EnableRetry
@EntityScan("org.eclipse.hawkbit.repository.jpa.model")
@ComponentScan("org.eclipse.hawkbit.repository.jpa.management")
@PropertySource("classpath:/hawkbit-jpa-defaults.properties")
@Import({ JpaConfiguration.class, RepositoryConfiguration.class, LockProperties.class, DataSourceAutoConfiguration.class,
        SystemManagementCacheKeyGenerator.class })
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class JpaRepositoryConfiguration {

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
                .addProperty(org.hibernate.validator.BaseHibernateValidatorConfiguration.ALLOW_PARALLEL_METHODS_DEFINE_PARAMETER_CONSTRAINTS,
                        "true")
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
    @ConditionalOnProperty(name = "hawkbit.lock", havingValue = "distributed", matchIfMissing = true)
    @ConditionalOnMissingBean
    LockRepository lockRepository(final DataSource dataSource, final LockProperties lockProperties,
            final PlatformTransactionManager txManager) {
        final DefaultLockRepository repository = new DistributedLockRepository(dataSource, lockProperties, txManager);
        repository.setPrefix("SP_");
        return repository;
    }

    @Bean
    @ConditionalOnMissingBean
    public LockRegistry lockRegistry(final Optional<LockRepository> lockRepository) {
        return lockRepository.<LockRegistry> map(JdbcLockRegistry::new).orElseGet(DefaultLockRegistry::new);
    }

    @Bean
    @ConditionalOnMissingBean
    PauseRolloutGroupAction pauseRolloutGroupAction(
            final RolloutManagement rolloutManagement, final RolloutGroupRepository rolloutGroupRepository,
            final SystemSecurityContext systemSecurityContext) {
        return new PauseRolloutGroupAction(rolloutManagement, rolloutGroupRepository, systemSecurityContext);
    }

    @Bean
    @ConditionalOnMissingBean
    StartNextGroupRolloutGroupSuccessAction startNextRolloutGroupAction(
            final RolloutGroupRepository rolloutGroupRepository, final DeploymentManagement deploymentManagement,
            final SystemSecurityContext systemSecurityContext) {
        return new StartNextGroupRolloutGroupSuccessAction(rolloutGroupRepository, deploymentManagement, systemSecurityContext);
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

    @Bean
    TargetBuilder targetBuilder(final TargetTypeManagement<? extends TargetType> targetTypeManagement) {
        return new JpaTargetBuilder(targetTypeManagement);
    }

    /**
     * @param distributionSetManagement for loading {@link Rollout#getDistributionSet()}
     * @return RolloutBuilder bean
     */
    @Bean
    RolloutBuilder rolloutBuilder(final DistributionSetManagement<? extends DistributionSet> distributionSetManagement) {
        return new JpaRolloutBuilder(distributionSetManagement);
    }

    /**
     * @param distributionSetManagement for loading
     *         {@link TargetFilterQuery#getAutoAssignDistributionSet()}
     * @return TargetFilterQueryBuilder bean
     */
    @Bean
    TargetFilterQueryBuilder targetFilterQueryBuilder(final DistributionSetManagement<? extends DistributionSet> distributionSetManagement) {
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

    @Bean
    @ConditionalOnMissingBean
    RolloutHandler rolloutHandler(final TenantAware tenantAware, final RolloutManagement rolloutManagement,
            final RolloutExecutor rolloutExecutor, final LockRegistry lockRegistry,
            final PlatformTransactionManager txManager, final ContextAware contextAware, final Optional<MeterRegistry> meterRegistry) {
        return new JpaRolloutHandler(tenantAware, rolloutManagement, rolloutExecutor, lockRegistry, txManager, contextAware, meterRegistry);
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
            final AfterTransactionCommitExecutor afterCommit,
            final TenantAware tenantAware, final RepositoryProperties repositoryProperties) {
        return new JpaRolloutExecutor(actionRepository, rolloutGroupRepository, rolloutTargetGroupRepository,
                rolloutRepository, targetManagement, deploymentManagement, rolloutGroupManagement, rolloutManagement,
                quotaManagement, evaluationManager, rolloutApprovalStrategy, entityManager, txManager, afterCommit,
                tenantAware, repositoryProperties);
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
        return new DefaultRolloutApprovalStrategy(userAuthoritiesResolver, tenantConfigurationManagement, systemSecurityContext);
    }

    /**
     * {@link JpaEntityFactory} bean.
     *
     * @return a new {@link EntityFactory}
     */
    @Bean
    @ConditionalOnMissingBean
    EntityFactory entityFactory(
            final TargetBuilder targetBuilder, final TargetFilterQueryBuilder targetFilterQueryBuilder,
            final RolloutBuilder rolloutBuilder) {
        return new JpaEntityFactory(targetBuilder, targetFilterQueryBuilder, rolloutBuilder);
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
            final LockRegistry lockRegistry, final Optional<MeterRegistry> meterRegistry) {
        return new AutoAssignScheduler(systemManagement, systemSecurityContext, autoAssignExecutor, lockRegistry, meterRegistry);
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
    RolloutScheduler rolloutScheduler(
            final SystemManagement systemManagement, final RolloutHandler rolloutHandler, final SystemSecurityContext systemSecurityContext,
            @Value("${hawkbit.rollout.executor.thread-pool.size:1}") final int threadPoolSize, final Optional<MeterRegistry> meterRegistry) {
        return new RolloutScheduler(rolloutHandler, systemManagement, systemSecurityContext, threadPoolSize, meterRegistry);
    }

    /**
     * Register the {@link RsqlUtility} bean to force Spring to inject values and auto-wired fields.
     *
     * @return The {@link RsqlUtility} singleton.
     */
    @Bean
    RsqlUtility rsqlUtility() {
        return RsqlUtility.getInstance();
    }

    /**
     * Default artifact encryption service bean that internally uses {@link ArtifactEncryption} and
     * {@link ArtifactEncryptionSecretsStore} beans for {@link SoftwareModule} artifacts encryption/decryption
     *
     * @return a {@link ArtifactEncryptionService} bean
     */
    @Bean
    @ConditionalOnMissingBean
    ArtifactEncryptionService artifactEncryptionService() {
        return ArtifactEncryptionService.getInstance();
    }

    @Bean
    ManagementExceptionThrowingMethodAuthorizationDeniedHandler managementExceptionThrowingMethodAuthorizationDeniedHandler() {
        return new ManagementExceptionThrowingMethodAuthorizationDeniedHandler();
    }

    public static class ManagementExceptionThrowingMethodAuthorizationDeniedHandler implements MethodAuthorizationDeniedHandler {

        @Override
        public Object handleDeniedInvocation(final MethodInvocation methodInvocation, final AuthorizationResult authorizationResult) {
            throw ExceptionMapper.mapRe(
                    authorizationResult instanceof AuthorizationDeniedException denied
                            ? denied
                            : new AuthorizationDeniedException("Access Denied", authorizationResult));
        }
    }
}
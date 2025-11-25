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
import org.eclipse.hawkbit.artifact.encryption.ArtifactEncryption;
import org.eclipse.hawkbit.artifact.encryption.ArtifactEncryptionSecretsStorage;
import org.eclipse.hawkbit.artifact.encryption.ArtifactEncryptionService;
import org.eclipse.hawkbit.ql.Node.Comparison;
import org.eclipse.hawkbit.ql.QueryField;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.ql.jpa.QLSupport.NodeTransformer;
import org.eclipse.hawkbit.ql.jpa.QLSupport.QueryParser;
import org.eclipse.hawkbit.ql.rsql.RsqlParser;
import org.eclipse.hawkbit.repository.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.PropertiesQuotaManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConfiguration;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutExecutor;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.event.ApplicationEventFilter;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManager;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManagerHolder;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.aspects.ExceptionMappingAspectHandler;
import org.eclipse.hawkbit.repository.jpa.autocleanup.AutoActionCleanup;
import org.eclipse.hawkbit.repository.jpa.autocleanup.AutoCleanupScheduler;
import org.eclipse.hawkbit.repository.jpa.cluster.DistributedLockRepository;
import org.eclipse.hawkbit.repository.jpa.cluster.LockProperties;
import org.eclipse.hawkbit.repository.jpa.event.JpaEventEntityManager;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityInterceptorHolder;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.PauseRolloutGroupAction;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupActionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupConditionEvaluator;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupEvaluationManager;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.StartNextGroupRolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.ThresholdRolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.ThresholdRolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.jpa.scheduler.AutoAssignScheduler;
import org.eclipse.hawkbit.repository.jpa.scheduler.JpaRolloutHandler;
import org.eclipse.hawkbit.repository.jpa.scheduler.RolloutScheduler;
import org.eclipse.hawkbit.repository.jpa.utils.ExceptionMapper;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ComponentScan({ "org.eclipse.hawkbit.repository.jpa.management", "org.eclipse.hawkbit.repository.jpa.scheduler" })
@PropertySource("classpath:/hawkbit-jpa-defaults.properties")
@Import({
        RepositoryConfiguration.class,
        JpaConfiguration.class, LockProperties.class, SystemManagementCacheKeyGenerator.class,
        DataSourceAutoConfiguration.class })
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class JpaRepositoryConfiguration {

    /**
     * Defines the validation processor bean.
     *
     * @return the {@link MethodValidationPostProcessor}
     */
    @Bean
    public static MethodValidationPostProcessor methodValidationPostProcessor() {
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
                if (bean instanceof ArtifactRepository repo) {
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
            final RolloutManagement rolloutManagement, final RolloutGroupRepository rolloutGroupRepository) {
        return new PauseRolloutGroupAction(rolloutManagement, rolloutGroupRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    StartNextGroupRolloutGroupSuccessAction startNextRolloutGroupAction(
            final RolloutGroupRepository rolloutGroupRepository, final DeploymentManagement deploymentManagement) {
        return new StartNextGroupRolloutGroupSuccessAction(rolloutGroupRepository, deploymentManagement);
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
    SystemManagementCacheKeyGenerator systemManagementCacheKeyGenerator() {
        return new SystemManagementCacheKeyGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    QuotaManagement staticQuotaManagement(final HawkbitSecurityProperties securityProperties) {
        return new PropertiesQuotaManagement(securityProperties);
    }

    // register as bean in order to be registered event listeners
    @Bean
    RolloutStatusCache rolloutStatusCache() {
        return new RolloutStatusCache();
    }

    @Bean
    @ConditionalOnMissingBean
    ApplicationEventFilter applicationEventFilter(final RepositoryProperties repositoryProperties) {
        return e -> e instanceof TargetPollEvent && !repositoryProperties.isPublishTargetPollEvent();
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
     * @return {@link ExceptionMappingAspectHandler} aspect bean
     */
    @Bean
    ExceptionMappingAspectHandler createRepositoryExceptionHandlerAdvice() {
        return new ExceptionMappingAspectHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    RolloutHandler rolloutHandler(final RolloutManagement rolloutManagement,
            final RolloutExecutor rolloutExecutor, final LockRegistry lockRegistry,
            final PlatformTransactionManager txManager, final Optional<MeterRegistry> meterRegistry) {
        return new JpaRolloutHandler(rolloutManagement, rolloutExecutor, lockRegistry, txManager, meterRegistry);
    }

    /**
     * {@link DefaultRolloutApprovalStrategy} bean.
     *
     * @return a new {@link RolloutApprovalStrategy}
     */
    @Bean
    @ConditionalOnMissingBean
    RolloutApprovalStrategy rolloutApprovalStrategy() {
        return new DefaultRolloutApprovalStrategy();
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
     * @param entityManager the entity manager
     * @return a new {@link EventEntityManager}
     */
    @Bean
    @ConditionalOnMissingBean
    EventEntityManager eventEntityManager(final EntityManager entityManager) {
        return new JpaEventEntityManager(entityManager);
    }

    /**
     * {@link AutoAssignScheduler} bean.
     * <p/>
     * Note: does not activate in test profile, otherwise it is hard to test the auto assign functionality.
     */
    @Bean
    @ConditionalOnMissingBean
    // don't active the auto assign scheduler in test, otherwise it is hard to test
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.autoassign.scheduler", name = "enabled", matchIfMissing = true)
    AutoAssignScheduler autoAssignScheduler(
            final SystemManagement systemManagement, final AutoAssignExecutor autoAssignExecutor,
            final LockRegistry lockRegistry, final Optional<MeterRegistry> meterRegistry) {
        return new AutoAssignScheduler(systemManagement, autoAssignExecutor, lockRegistry, meterRegistry);
    }

    /**
     * {@link AutoActionCleanup} bean.
     *
     * @param deploymentManagement Deployment management service
     * @param configManagement Tenant configuration service
     * @return a new {@link AutoActionCleanup} bean
     */
    @Bean
    AutoCleanupScheduler.CleanupTask actionCleanup(
            final DeploymentManagement deploymentManagement, final TenantConfigurationManagement configManagement) {
        return new AutoActionCleanup(deploymentManagement, configManagement);
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.autocleanup.scheduler", name = "enabled", matchIfMissing = true)
    AutoCleanupScheduler autoCleanupScheduler(
            final List<AutoCleanupScheduler.CleanupTask> cleanupTasks,
            final SystemManagement systemManagement, final LockRegistry lockRegistry) {
        return new AutoCleanupScheduler(cleanupTasks, systemManagement, lockRegistry);
    }

    /**
     * {@link RolloutScheduler} bean.
     * <p/>
     * Note: does not activate in test profile, otherwise it is hard to test the rollout handling functionality.
     */
    @Bean
    @ConditionalOnMissingBean
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.rollout.scheduler", name = "enabled", matchIfMissing = true)
    RolloutScheduler rolloutScheduler(
            final SystemManagement systemManagement, final RolloutHandler rolloutHandler,
            @Value("${hawkbit.rollout.executor.thread-pool.size:1}") final int threadPoolSize, final Optional<MeterRegistry> meterRegistry) {
        return new RolloutScheduler(rolloutHandler, systemManagement, threadPoolSize, meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public VirtualPropertyResolver virtualPropertyResolver() {
        return new VirtualPropertyResolver();
    }

    @Bean
    @ConditionalOnBean(VirtualPropertyResolver.class)
    public NodeTransformer virtualPropertyReplacerTransformer(final VirtualPropertyResolver resolver) {
        return new NodeTransformer.Abstract() {

            @Override
            protected <T extends Enum<T> & QueryField> Object transformValueElement(
                    final Object value, final Comparison comparison, final Class<T> queryFieldType) {
                return value instanceof String strValue ? resolver.replace(strValue) : value;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    QueryParser queryParser() {
        return RsqlParser::parse;
    }

    /**
     * Register the {@link QLSupport} bean to force Spring to inject values and auto-wired fields.
     *
     * @return The {@link QLSupport} singleton.
     */
    @Bean
    QLSupport qlSupport() {
        return QLSupport.getInstance();
    }

    /**
     * Default artifact encryption service bean that internally uses {@link ArtifactEncryption} and
     * {@link ArtifactEncryptionSecretsStorage} beans for {@link SoftwareModule} artifacts encryption/decryption
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
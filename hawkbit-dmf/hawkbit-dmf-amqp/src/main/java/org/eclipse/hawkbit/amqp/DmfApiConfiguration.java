/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.amqp;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.ErrorHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring configuration for AMQP based DMF communication for indirect device integration.
 */
@Slf4j
@ComponentScan
@Import(DmfAmqpDeclarationConfiguration.class)
@EnableConfigurationProperties(AmqpProperties.class)
@ConditionalOnProperty(prefix = "hawkbit.dmf", name = "enabled", matchIfMissing = true)
@PropertySource("classpath:/hawkbit-dmf-defaults.properties")
public class DmfApiConfiguration {

    private final AmqpProperties amqpProperties;
    private final ConnectionFactory rabbitConnectionFactory;

    public DmfApiConfiguration(
            final AmqpProperties amqpProperties,
            final ConnectionFactory rabbitConnectionFactory) {
        this.amqpProperties = amqpProperties;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
    }

    @Bean
    public FatalExceptionStrategy sqlFatalSQLExceptionStrategy(final AmqpProperties amqpProperties) {
        return new SqlFatalExceptionStrategy(amqpProperties.getFatalSqlExceptionPolicy());
    }

    /**
     * Creates a custom error handler bean.
     *
     * @param fatalExceptionStrategies list of {@link FatalExceptionStrategy} handlers. isFatal will be called for causes,
     *         up to the first fatal, so the implementation don't need to iterate over the causes.
     * @return the delegating error handler bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorHandler errorHandler(
            final List<FatalExceptionStrategy> fatalExceptionStrategies,
            @Value("${hawkbit.dmf.rabbitmq.fatal-exception-types:}") final List<String> fatalExceptionTypes) {
        return new ConditionalRejectingErrorHandler(new RequeueExceptionStrategy(fatalExceptionStrategies, fatalExceptionTypes));
    }

    /**
     * @return {@link RabbitTemplate} with automatic retry, published confirms and {@link Jackson2JsonMessageConverter}.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(final JsonMapper jsonMapper) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(rabbitConnectionFactory);
        rabbitTemplate.setMessageConverter(new JacksonJsonMessageConverter(jsonMapper));

        // the same policy the previously used default ExponentialBackOffPolicy applied
        rabbitTemplate.setRetryTemplate(new RetryTemplate(RetryPolicy.builder()
                .delay(Duration.ofMillis(100))
                .multiplier(2)
                .maxDelay(Duration.ofSeconds(30))
                .build()));

        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Message with {} confirmed by broker.", correlationData);
            } else {
                log.error("Broker is unable to handle message with {} : {}", correlationData, cause);
            }
        });

        return rabbitTemplate;
    }

    /**
     * Create AMQP handler service bean.
     *
     * @param rabbitTemplate for converting messages
     * @param amqpMessageDispatcherService to sending events to DMF client
     * @param controllerManagement for target repo access
     * @return handler service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AmqpMessageHandlerService amqpMessageHandlerService(
            final RabbitTemplate rabbitTemplate,
            final AmqpMessageDispatcherService amqpMessageDispatcherService,
            final ControllerManagement controllerManagement,
            final ConfirmationManagement confirmationManagement) {
        return new AmqpMessageHandlerService(
                rabbitTemplate, amqpMessageDispatcherService, controllerManagement, confirmationManagement);
    }

    /**
     * Create default amqp sender service bean.
     *
     * @return the default amqp sender service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AmqpMessageSenderService amqpSenderServiceBean(final RabbitTemplate rabbitTemplate) {
        return new DefaultAmqpMessageSenderService(rabbitTemplate);
    }

    /**
     * Create RabbitListenerContainerFactory bean if no listenerContainerFactory bean found
     *
     * @return RabbitListenerContainerFactory bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "listenerContainerFactory")
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> listenerContainerFactory(
            final SimpleRabbitListenerContainerFactoryConfigurer configurer, final ErrorHandler errorHandler) {
        final ConfigurableRabbitListenerContainerFactory factory = new ConfigurableRabbitListenerContainerFactory(
                amqpProperties.isMissingQueuesFatal(), amqpProperties.getDeclarationRetries(), errorHandler);
        configurer.configure(factory, rabbitConnectionFactory);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(AmqpMessageDispatcherService.class)
    AmqpMessageDispatcherService amqpMessageDispatcherService(
            final RabbitTemplate rabbitTemplate,
            final AmqpMessageSenderService amqpSenderService, final ArtifactUrlResolver artifactUrlHandler,
            final SystemManagement systemManagement,
            final TargetManagement<? extends Target> targetManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement,
            final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement, final DeploymentManagement deploymentManagement) {
        return new AmqpMessageDispatcherService(rabbitTemplate, amqpSenderService, artifactUrlHandler,
                systemManagement, targetManagement, softwareModuleManagement, distributionSetManagement,
                deploymentManagement);
    }

    @ToString
    private static class SqlFatalExceptionStrategy implements FatalExceptionStrategy {

        private final boolean fatalByDefault;
        private final List<Integer> unlessErrorCodeIn;
        private final List<String> unlessSqlStateIn;
        private final List<Pattern> unlessMessageMatches;

        public SqlFatalExceptionStrategy(final AmqpProperties.FatalSqlExceptionPolicy fatalSqlExceptions) {
            this.fatalByDefault = fatalSqlExceptions.isByDefault();
            this.unlessErrorCodeIn = fatalSqlExceptions.getUnlessErrorCodeIn();
            this.unlessSqlStateIn = fatalSqlExceptions.getUnlessSqlStateIn();
            this.unlessMessageMatches = fatalSqlExceptions.getUnlessMessageMatches();
        }

        @Override
        public boolean isFatal(final Throwable t) {
            if (t instanceof SQLException sqlException) {
                if (unlessErrorCodeIn.contains(sqlException.getErrorCode())) {
                    return !fatalByDefault;
                } else if (unlessSqlStateIn.contains(sqlException.getSQLState())) {
                    return !fatalByDefault;
                } else {
                    for (final Pattern pattern : unlessMessageMatches) {
                        if (pattern.matcher(sqlException.getMessage()).matches()) {
                            return !fatalByDefault;
                        }
                    }
                    return fatalByDefault;
                }
            }
            return false;
        }
    }
}
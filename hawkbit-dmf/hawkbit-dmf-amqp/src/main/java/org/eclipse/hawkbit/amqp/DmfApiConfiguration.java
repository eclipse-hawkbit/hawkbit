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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver;
import org.eclipse.hawkbit.context.SystemSecurityContext;
import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.ErrorHandler;

/**
 * Spring configuration for AMQP based DMF communication for indirect device integration.
 */
@Slf4j
@ComponentScan
@EnableConfigurationProperties({ AmqpProperties.class, AmqpDeadletterProperties.class })
@ConditionalOnProperty(prefix = "hawkbit.dmf", name = "enabled", matchIfMissing = true)
@PropertySource("classpath:/hawkbit-dmf-defaults.properties")
public class DmfApiConfiguration {

    private final AmqpProperties amqpProperties;
    private final AmqpDeadletterProperties amqpDeadletterProperties;
    private final ConnectionFactory rabbitConnectionFactory;

    public DmfApiConfiguration(
            final AmqpProperties amqpProperties, final AmqpDeadletterProperties amqpDeadletterProperties,
            final ConnectionFactory rabbitConnectionFactory) {
        this.amqpProperties = amqpProperties;
        this.amqpDeadletterProperties = amqpDeadletterProperties;
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
     * Create a {@link RabbitAdmin} and ignore declaration exceptions.
     * {@link RabbitAdmin#setIgnoreDeclarationExceptions(boolean)}
     *
     * @return the bean
     */
    @Bean
    public RabbitAdmin rabbitAdmin() {
        final RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitConnectionFactory);
        rabbitAdmin.setIgnoreDeclarationExceptions(true);
        return rabbitAdmin;
    }

    /**
     * @return {@link RabbitTemplate} with automatic retry, published confirms and {@link Jackson2JsonMessageConverter}.
     */
    @Bean
    public RabbitTemplate rabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(rabbitConnectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        final RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());
        rabbitTemplate.setRetryTemplate(retryTemplate);

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
     * Create the DMF API receiver queue for retrieving DMF messages.
     *
     * @return the receiver queue
     */
    @Bean
    public Queue dmfReceiverQueue() {
        return new Queue(
                amqpProperties.getReceiverQueue(),
                true, false, false,
                amqpDeadletterProperties.getDeadLetterExchangeArgs(amqpProperties.getDeadLetterExchange()));
    }

    /**
     * Create the DMF API receiver queue for auth requests called by 3rd
     * party artifact storages for download authorization by devices.
     *
     * @return the receiver queue
     */
    @Bean
    public Queue authenticationReceiverQueue() {
        return QueueBuilder.nonDurable(amqpProperties.getAuthenticationReceiverQueue())
                .autoDelete()
                .withArguments(getTTLMaxArgsAuthenticationQueue())
                .build();
    }

    /**
     * Create DMF exchange.
     *
     * @return the fanout exchange
     */
    @Bean
    public FanoutExchange dmfSenderExchange() {
        return new FanoutExchange(AmqpSettings.DMF_EXCHANGE);
    }

    /**
     * Create the Binding {@link DmfApiConfiguration#dmfReceiverQueue()} to
     * {@link DmfApiConfiguration#dmfSenderExchange()}.
     *
     * @return the binding and create the queue and exchange
     */
    @Bean
    public Binding bindDmfSenderExchangeToDmfQueue() {
        return BindingBuilder.bind(dmfReceiverQueue()).to(dmfSenderExchange());
    }

    /**
     * Create dead letter queue.
     *
     * @return the queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return amqpDeadletterProperties.createDeadletterQueue(amqpProperties.getDeadLetterQueue());
    }

    /**
     * Create the dead letter fanout exchange.
     *
     * @return the fanout exchange
     */
    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(amqpProperties.getDeadLetterExchange());
    }

    /**
     * Create the Binding deadLetterQueue to deadLetterExchange.
     *
     * @return the binding
     */
    @Bean
    public Binding bindDeadLetterQueueToDeadLetterExchange() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
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
    public AmqpMessageSenderService amqpSenderServiceBean() {
        return new DefaultAmqpMessageSenderService(rabbitTemplate());
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
            final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement, final DeploymentManagement deploymentManagement,
            final RepositoryProperties repositoryProperties) {
        return new AmqpMessageDispatcherService(rabbitTemplate, amqpSenderService, artifactUrlHandler,
                systemManagement, targetManagement, softwareModuleManagement, distributionSetManagement,
                deploymentManagement, repositoryProperties);
    }

    private static Map<String, Object> getTTLMaxArgsAuthenticationQueue() {
        final Map<String, Object> args = new HashMap<>(2);
        args.put("x-message-ttl", Duration.ofSeconds(30).toMillis());
        args.put("x-max-length", 1_000);
        return args;
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
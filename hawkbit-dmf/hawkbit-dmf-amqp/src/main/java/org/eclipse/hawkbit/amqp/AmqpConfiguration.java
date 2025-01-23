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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.repository.urlhandler.ArtifactUrlHandler;
import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.ErrorHandler;

/**
 * Spring configuration for AMQP based DMF communication for indirect device integration.
 */
@Slf4j
@EnableConfigurationProperties({ AmqpProperties.class, AmqpDeadletterProperties.class })
@ConditionalOnProperty(prefix = "hawkbit.dmf.rabbitmq", name = "enabled", matchIfMissing = true)
@PropertySource("classpath:/hawkbit-dmf-defaults.properties")
public class AmqpConfiguration {

    private final AmqpProperties amqpProperties;
    private final AmqpDeadletterProperties amqpDeadletterProperties;
    private final ConnectionFactory rabbitConnectionFactory;
    private ServiceMatcher serviceMatcher;

    public AmqpConfiguration(final AmqpProperties amqpProperties, final AmqpDeadletterProperties amqpDeadletterProperties, final ConnectionFactory rabbitConnectionFactory) {
        this.amqpProperties = amqpProperties;
        this.amqpDeadletterProperties = amqpDeadletterProperties;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
    }

    @Autowired(required = false) // spring setter injection
    public void setServiceMatcher(final ServiceMatcher serviceMatcher) {
        this.serviceMatcher = serviceMatcher;
    }

    /**
     * Creates a custom error handler bean.
     *
     * @param handlers list of {@link AmqpErrorHandler} handlers
     * @return the delegating error handler bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorHandler errorHandler(final List<AmqpErrorHandler> handlers) {
        return new DelegatingConditionalErrorHandler(
                handlers,
                new ConditionalRejectingErrorHandler(new DelayedRequeueExceptionStrategy(amqpProperties.getRequeueDelay())));
    }

    /**
     * Error handler bean for all target attributes related fatal errors
     *
     * @return the invalid target attribute exception handler bean
     */
    @Bean
    public AmqpErrorHandler invalidTargetAttributeConditionalExceptionHandler() {
        return new InvalidTargetAttributeExceptionHandler();
    }

    /**
     * Error handler bean for entity not found errors
     *
     * @return the entity not found exception handler bean
     */
    @Bean
    public AmqpErrorHandler entityNotFoundExceptionHandler() {
        return new EntityNotFoundExceptionHandler();
    }

    /**
     * Error handler bean for amqp message conversion errors
     *
     * @return the amqp message conversion exception handler bean
     */
    @Bean
    public AmqpErrorHandler messageConversionExceptionHandler() {
        return new MessageConversionExceptionHandler();
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
     * Create the DMF API receiver queue for authentication requests called by 3rd
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
     * Create the Binding {@link AmqpConfiguration#dmfReceiverQueue()} to
     * {@link AmqpConfiguration#dmfSenderExchange()}.
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
     * @param entityFactory to create entities
     * @return handler service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AmqpMessageHandlerService amqpMessageHandlerService(
            final RabbitTemplate rabbitTemplate,
            final AmqpMessageDispatcherService amqpMessageDispatcherService,
            final ControllerManagement controllerManagement, final EntityFactory entityFactory,
            final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final ConfirmationManagement confirmationManagement) {
        return new AmqpMessageHandlerService(
                rabbitTemplate, amqpMessageDispatcherService, controllerManagement,
                entityFactory, systemSecurityContext, tenantConfigurationManagement, confirmationManagement);
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
            final AmqpMessageSenderService amqpSenderService, final ArtifactUrlHandler artifactUrlHandler,
            final SystemSecurityContext systemSecurityContext, final SystemManagement systemManagement,
            final TargetManagement targetManagement, final DistributionSetManagement distributionSetManagement,
            final SoftwareModuleManagement softwareModuleManagement, final DeploymentManagement deploymentManagement,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        return new AmqpMessageDispatcherService(rabbitTemplate, amqpSenderService, artifactUrlHandler,
                systemSecurityContext, systemManagement, targetManagement, serviceMatcher, distributionSetManagement,
                softwareModuleManagement, deploymentManagement, tenantConfigurationManagement);
    }

    private static Map<String, Object> getTTLMaxArgsAuthenticationQueue() {
        final Map<String, Object> args = new HashMap<>(2);
        args.put("x-message-ttl", Duration.ofSeconds(30).toMillis());
        args.put("x-max-length", 1_000);
        return args;
    }
}
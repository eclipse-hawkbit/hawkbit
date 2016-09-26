/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.ui;

import org.eclipse.hawkbit.DistributedResourceBundleMessageSource;
import org.eclipse.hawkbit.ui.HawkbitEventProvider;
import org.eclipse.hawkbit.ui.UIEventProvider;
import org.eclipse.hawkbit.ui.push.DelayedEventBusPushStrategy;
import org.eclipse.hawkbit.ui.push.EventPushStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.SessionScope;
import org.vaadin.spring.annotation.EnableVaadinExtensions;
import org.vaadin.spring.events.annotation.EnableEventBus;
import org.vaadin.spring.security.annotation.EnableVaadinSecurity;

/**
 * The hawkbit-ui autoconfiguration.
 */
@Configuration
@EnableVaadinSecurity
@EnableVaadinExtensions
@EnableEventBus
public class UIAutoConfiguration {

    /**
     * A message source bean to add distributed message sources.
     * 
     * @return the message bean.
     */
    @Bean(name = "messageSource")
    public DistributedResourceBundleMessageSource messageSource() {
        return new DistributedResourceBundleMessageSource();
    }

    /**
     * A event provider bean which hold the supported events for the UI.
     * 
     * @return the provider bean
     */
    @Bean
    @ConditionalOnMissingBean
    public UIEventProvider eventProvider() {
        return new HawkbitEventProvider();
    }

    /**
     * The session scoped event push strategy. Session scope is necessary, that
     * every UI has a own strategy. The bean is no proxy, because this bean is
     * bounded to the vaadin session.
     * 
     * @param applicationContext
     *            the context to add the listener
     * 
     * @return the provider bean
     */
    @Bean
    @ConditionalOnMissingBean
    @SessionScope(proxyMode = ScopedProxyMode.NO)
    public EventPushStrategy eventPushStrategy(final ConfigurableApplicationContext applicationContext) {
        final DelayedEventBusPushStrategy delayedEventBusPushStrategy = new DelayedEventBusPushStrategy();
        applicationContext.addApplicationListener(delayedEventBusPushStrategy);
        return delayedEventBusPushStrategy;
    }

}

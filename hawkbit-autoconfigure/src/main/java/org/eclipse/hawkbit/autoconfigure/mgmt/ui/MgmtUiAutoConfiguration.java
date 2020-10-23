/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.mgmt.ui;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.hawkbit.DistributedResourceBundleMessageSource;
import org.eclipse.hawkbit.ui.MgmtUiConfiguration;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.push.DelayedEventBusPushStrategy;
import org.eclipse.hawkbit.ui.push.EventPushStrategy;
import org.eclipse.hawkbit.ui.push.HawkbitEventPermissionChecker;
import org.eclipse.hawkbit.ui.push.HawkbitEventProvider;
import org.eclipse.hawkbit.ui.push.UIEventPermissionChecker;
import org.eclipse.hawkbit.ui.push.UIEventProvider;
import org.eclipse.hawkbit.ui.utils.SpringContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.vaadin.spring.annotation.EnableVaadinExtensions;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.annotation.EnableEventBus;

import com.vaadin.spring.annotation.UIScope;

/**
 * The Management UI auto configuration.
 */
@Configuration
@EnableVaadinExtensions
@EnableEventBus
@ConditionalOnClass(MgmtUiConfiguration.class)
@Import(MgmtUiConfiguration.class)
public class MgmtUiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RedirectController uiRedirectController() {
        return new RedirectController();
    }

    /**
     * A message source bean to add distributed message sources.
     * 
     * @return the message bean.
     */
    @Bean(name = "messageSource")
    DistributedResourceBundleMessageSource messageSource() {
        return new DistributedResourceBundleMessageSource();
    }

    /**
     * A event provider bean which hold the supported events for the UI.
     * 
     * @return the provider bean
     */
    @Bean
    @ConditionalOnMissingBean
    UIEventProvider eventProvider() {
        return new HawkbitEventProvider();
    }

    /**
     * A event permission checker bean which verifies supported events for the
     * UI.
     * 
     * @return the permission checker bean
     */
    @Bean
    @ConditionalOnMissingBean
    UIEventPermissionChecker eventPermissionChecker(final SpPermissionChecker permChecker) {
        return new HawkbitEventPermissionChecker(permChecker);
    }

    /**
     * Bean for creating a singleton instance of the {@link SpringContextHolder}
     * 
     * @return the singleton instance of the {@link SpringContextHolder}
     */
    @Bean
    SpringContextHolder springContextHolder() {
        return SpringContextHolder.getInstance();
    }

    /**
     * The UI scoped event push strategy. Session scope is necessary, that every
     * UI has an own strategy.
     * 
     * @param applicationContext
     *            the context to add the listener to
     * @param executorService
     *            the general scheduler service
     * @param eventBus
     *            the ui event bus
     * @param eventProvider
     *            the event provider
     * @param eventPermissionChecker
     *            the event permission checker
     * @param uiProperties
     *            the ui properties
     * @return the push strategy bean
     */
    @Bean
    @ConditionalOnMissingBean
    @UIScope
    EventPushStrategy eventPushStrategy(final ConfigurableApplicationContext applicationContext,
            final ScheduledExecutorService executorService, final UIEventBus eventBus,
            final UIEventProvider eventProvider, final UIEventPermissionChecker eventPermissionChecker,
            final UiProperties uiProperties) {
        final DelayedEventBusPushStrategy delayedEventBusPushStrategy = new DelayedEventBusPushStrategy(executorService,
                eventBus, eventProvider, eventPermissionChecker, uiProperties.getEvent().getPush().getDelay());
        applicationContext.addApplicationListener(delayedEventBusPushStrategy);

        return delayedEventBusPushStrategy;
    }
}

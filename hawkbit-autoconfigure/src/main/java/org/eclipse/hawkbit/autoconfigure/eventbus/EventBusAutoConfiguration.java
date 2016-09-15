/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.eventbus;

import java.util.concurrent.Executor;

import org.eclipse.hawkbit.repository.jpa.model.helper.EventBusHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

/**
 * Auto configuration for the event bus.
 *
 */
@Configuration
public class EventBusAutoConfiguration {

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor executor;

    /**
     * Server internal eventBus that allows parallel event processing if the
     * subscriber is marked as so.
     *
     * @return eventbus bean
     */
    @Bean
    @ConditionalOnMissingBean
    public EventBus eventBus() {
        return new AsyncEventBus(executor);
    }

    /**
     * Server internal eventBus that allows parallel event processing if the
     * subscriber is marked as so.
     *
     * @return eventbus bean
     */
    @Bean(name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    @ConditionalOnMissingBean
    public SimpleApplicationEventMulticaster applicationEventMulticaster() {
        final SimpleApplicationEventMulticaster simpleApplicationEventMulticaster = new SimpleApplicationEventMulticaster();
        simpleApplicationEventMulticaster.setTaskExecutor(executor);
        return simpleApplicationEventMulticaster;
    }

    /**
     * @return the singleton instance of the {@link EventBusHolder}
     */
    @Bean
    public EventBusHolder eventBusHolder() {
        return EventBusHolder.getInstance();
    }

}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.event;

import java.util.concurrent.Executor;

import org.eclipse.hawkbit.repository.jpa.model.helper.EventPublisherHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * Auto configuration for the event bus.
 *
 */
@Configuration
public class EventPublisherAutoConfiguration {

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor executor;

    /**
     * Server internal event publisher that allows parallel event processing if
     * the event listener is marked as so.
     *
     * @return publisher bean
     */
    @Bean(name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
    @ConditionalOnMissingBean
    public SimpleApplicationEventMulticaster applicationEventMulticaster() {
        final SimpleApplicationEventMulticaster simpleApplicationEventMulticaster = new SimpleApplicationEventMulticaster();
        simpleApplicationEventMulticaster.setTaskExecutor(executor);
        return simpleApplicationEventMulticaster;
    }

    /**
     * @return the singleton instance of the {@link EventPublisherHolder}
     */
    @Bean
    public EventPublisherHolder eventBusHolder() {
        return EventPublisherHolder.getInstance();
    }

}

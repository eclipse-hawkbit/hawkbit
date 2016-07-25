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

import org.eclipse.hawkbit.eventbus.EventBusSubscriberProcessor;
import org.eclipse.hawkbit.eventbus.EventSubscriber;
import org.eclipse.hawkbit.repository.jpa.model.helper.EventBusHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

/**
 *
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
     * @return the {@link EventBusSubscriberProcessor} to find classes annotated
     *         with {@link EventSubscriber}.
     */
    @Bean
    @ConditionalOnMissingBean
    public EventBusSubscriberProcessor eventBusSubscriberProcessor() {
        return new EventBusSubscriberProcessor();
    }

    /**
     * @return the singleton instance of the {@link EventBusHolder}
     */
    @Bean
    public EventBusHolder eventBusHolder() {
        return EventBusHolder.getInstance();
    }

}

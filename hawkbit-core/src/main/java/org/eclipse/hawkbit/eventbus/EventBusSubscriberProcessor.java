/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * An {@link BeanPostProcessor} implementation which registers all beans as a
 * event bus subscriber if the classes are annotated with
 * {@link EventSubscriber} and have at least one method annotated with the
 * guava's {@link Subscribe} annoation.
 * 
 *
 */

public class EventBusSubscriberProcessor implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusSubscriberProcessor.class);

    @Autowired
    private EventBus eventBus;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.config.BeanPostProcessor#
     * postProcessBeforeInitialization (java.lang.Object, java.lang.String)
     */
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        return bean;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.config.BeanPostProcessor#
     * postProcessAfterInitialization( java.lang.Object, java.lang.String)
     */
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        final Class<? extends Object> beanClass = bean.getClass();
        final EventSubscriber eventSubscriber = beanClass.getAnnotation(EventSubscriber.class);
        if (eventSubscriber != null) {
            LOGGER.trace("Found bean {} with {} annotation ", bean.getClass().getName(),
                    EventSubscriber.class.getSimpleName());
            final Method[] declaredMethods = beanClass.getDeclaredMethods();
            for (final Method method : declaredMethods) {
                final Subscribe subscriber = method.getAnnotation(Subscribe.class);
                if (subscriber != null) {
                    LOGGER.trace("Found method {} for bean {} with {} annotation", method.getName(),
                            bean.getClass().getName(), Subscribe.class.getSimpleName());
                    eventBus.register(bean);
                    return bean;
                }
            }
        }
        if (eventSubscriber != null) {
            LOGGER.debug("Found bean {} with {} annotation but without any method with necessary {} annotation",
                    bean.getClass().getName(), EventSubscriber.class.getSimpleName(), Subscribe.class.getSimpleName());
        }
        return bean;
    }

    /**
     * package private setter for testing purposes.
     * 
     * @param eventBus
     *            the event bus
     */
    void setEventBus(final EventBus eventBus) {
        this.eventBus = eventBus;
    }
}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Singleton for the spring application context.
 */
public final class SpringContextHolder {

    private static final SpringContextHolder SINGLETON = new SpringContextHolder();

    @Autowired
    private ApplicationContext context;

    /**
     * Private Constructor.
     */
    private SpringContextHolder() {
        // Utility class
    }

    /**
     * @return the spring context holder singleton instance
     */
    public static SpringContextHolder getInstance() {
        return SINGLETON;
    }

    /**
     * method to return a certain bean by its name.
     * 
     * @param beanName
     *            name of the beand which should be returned from the
     *            application context
     * @return the requested bean
     */
    public Object getBean(final String beanName) {
        return context.getBean(beanName);
    }

    /**
     * method to return a certain bean by its class.
     * 
     * @param beanClazz
     *            class of the bean which should be returned from the
     *            application context
     * @return the requested bean
     */
    public <T> T getBean(final Class<T> beanClazz) {
        return context.getBean(beanClazz);
    }

    /**
     * method to return a certain bean by its class and name.
     * 
     * @param beanName
     *            name of the beand which should be returned from the
     *            application context
     * @param beanClazz
     *            class of the bean which should be returned from the
     *            application context
     * @return the requested bean
     */
    public <T> T getBean(final String beanName, final Class<T> beanClazz) {
        return context.getBean(beanName, beanClazz);
    }
}

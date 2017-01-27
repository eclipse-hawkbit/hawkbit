/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import org.springframework.context.ApplicationContext;

/**
 * Singleton for the spring application context.
 *
 *
 *
 */
public final class SpringContextHelper {

    private static ApplicationContext context;

    /**
     * Private Constructor.
     */
    private SpringContextHelper() {
        super();
    }

    public static void setContext(final ApplicationContext context) {
        SpringContextHelper.context = context;
    }

    /**
     * method to return a certain bean by its name.
     * 
     * @param beanName
     *            name of the beand which should be returned from the
     *            application context
     * @return the requested bean
     */
    public static Object getBean(final String beanName) {
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
    public static <T> T getBean(final Class<T> beanClazz) {
        return context.getBean(beanClazz);
    }

}

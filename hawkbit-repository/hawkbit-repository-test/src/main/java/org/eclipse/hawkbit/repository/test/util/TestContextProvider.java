/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

public class TestContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static ConfigurableApplicationContext getContext() {
        return (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void setApplicationContext(final ApplicationContext context) {
        applicationContext = context;
    }
}

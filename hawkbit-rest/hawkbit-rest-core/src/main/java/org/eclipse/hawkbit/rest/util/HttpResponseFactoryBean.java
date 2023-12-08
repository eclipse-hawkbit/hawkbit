/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NamedBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 
 * Factory bean to autowire the {@link HttpServletResponse}.
 * 
 */
public class HttpResponseFactoryBean implements FactoryBean<HttpServletResponse>, ApplicationContextAware, NamedBean {

    public static final String FACTORY_BEAN_NAME = "httpResponseFactoryBean";

    private ApplicationContext applicationContext;

    @Override
    public HttpServletResponse getObject() {
        return applicationContext.getBean(FilterHttpResponse.class).getHttpServletReponse();
    }

    @Override
    public Class<?> getObjectType() {
        return HttpServletResponse.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public String getBeanName() {
        return FACTORY_BEAN_NAME;
    }

}

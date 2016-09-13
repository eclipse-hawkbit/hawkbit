/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.feign.core.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;

import feign.MethodMetadata;
import feign.Param;

/**
 * Own implementation of the {@link SpringMvcContract} which catches the
 * {@link IllegalStateException} which occurs due multiple produces and consumes
 * values in the request-mapping
 * annoation.https://github.com/spring-cloud/spring-cloud-netflix/issues/808
 */
public class IgnoreMultipleConsumersProducersSpringMvcContract extends SpringMvcContract {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(IgnoreMultipleConsumersProducersSpringMvcContract.class);

    @Override
    protected void processAnnotationOnMethod(final MethodMetadata data, final Annotation methodAnnotation,
            final Method method) {
        try {
            super.processAnnotationOnMethod(data, methodAnnotation, method);
        } catch (final IllegalStateException e) {
            // ignore illegalstateexception here because it's thrown because of
            // multiple consumers and produces, see
            // https://github.com/spring-cloud/spring-cloud-netflix/issues/808
            LOGGER.trace(e.getMessage(), e);

            // This line from super is mandatory to avoid that access to the
            // expander causes a nullpointer.
            data.indexToExpander(new LinkedHashMap<Integer, Param.Expander>());
        }
    }
}

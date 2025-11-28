/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.aspects;

import jakarta.transaction.TransactionManager;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.eclipse.hawkbit.repository.jpa.utils.ExceptionMapper;
import org.springframework.core.Ordered;

/**
 * {@link Aspect} catches persistence exceptions and wraps them to custom
 * specific exceptions Additionally it checks and prevents access to certain
 * packages. Logging aspect which logs the call stack
 */
@Slf4j
@Aspect
public class ExceptionMappingAspectHandler implements Ordered {

    /**
     * Catches exceptions the {@link TransactionManager} and wrap them to custom exceptions.
     *
     * @param e the thrown and caught exception
     * @throws Throwable the mapped exception
     */
    @AfterThrowing(pointcut = "execution( * org.eclipse.hawkbit.repository.jpa.management.*Management.*(..))", throwing = "e")
    // Exception for squid:S00112, squid:S1162
    // It is a AspectJ proxy which deals with exceptions.
    @SuppressWarnings({ "squid:S00112", "squid:S1162" })
    public void catchAndWrapJpaExceptionsService(final Exception e) throws Throwable {
        throw ExceptionMapper.map(e);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
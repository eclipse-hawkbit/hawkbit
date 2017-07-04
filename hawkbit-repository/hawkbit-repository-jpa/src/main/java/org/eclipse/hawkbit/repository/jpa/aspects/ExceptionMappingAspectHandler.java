/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.aspects;

import java.util.List;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.eclipse.hawkbit.exception.GenericSpServerException;
import org.eclipse.hawkbit.repository.exception.ConcurrentModificationException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * {@link Aspect} catches persistence exceptions and wraps them to custom
 * specific exceptions Additionally it checks and prevents access to certain
 * packages. Logging aspect which logs the call stack
 */
@Aspect
public class ExceptionMappingAspectHandler implements Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionMappingAspectHandler.class);

    private static final Map<String, String> EXCEPTION_MAPPING = Maps.newHashMapWithExpectedSize(4);

    /**
     * this is required to enable a certain order of exception and to select the
     * most specific mappable exception according to the type hierarchy of the
     * exception.
     */
    private static final List<Class<?>> MAPPED_EXCEPTION_ORDER = Lists.newArrayListWithExpectedSize(4);

    static {

        MAPPED_EXCEPTION_ORDER.add(DuplicateKeyException.class);
        MAPPED_EXCEPTION_ORDER.add(DataIntegrityViolationException.class);
        MAPPED_EXCEPTION_ORDER.add(OptimisticLockingFailureException.class);
        MAPPED_EXCEPTION_ORDER.add(AccessDeniedException.class);

        EXCEPTION_MAPPING.put(DuplicateKeyException.class.getName(), EntityAlreadyExistsException.class.getName());
        EXCEPTION_MAPPING.put(DataIntegrityViolationException.class.getName(),
                EntityAlreadyExistsException.class.getName());

        EXCEPTION_MAPPING.put(OptimisticLockingFailureException.class.getName(),
                ConcurrentModificationException.class.getName());
        EXCEPTION_MAPPING.put(AccessDeniedException.class.getName(), InsufficientPermissionException.class.getName());
    }

    /**
     * catch exceptions of the {@link TransactionManager} and wrap them to
     * custom exceptions.
     *
     * @param ex
     *            the thrown and catched exception
     * @throws Throwable
     */
    @AfterThrowing(pointcut = "execution( * org.eclipse.hawkbit.repository.jpa.*Management.*(..))", throwing = "ex")
    // Exception for squid:S00112, squid:S1162
    // It is a AspectJ proxy which deals with exceptions.
    @SuppressWarnings({ "squid:S00112", "squid:S1162" })
    public void catchAndWrapJpaExceptionsService(final Exception ex) throws Throwable {

        // Workarround for EclipseLink merge where it does not throw
        // ConstraintViolationException directly in case of existing entity
        // update
        if (ex instanceof TransactionSystemException) {
            throw replaceWithCauseIfConstraintViolationException((TransactionSystemException) ex);
        }

        for (final Class<?> mappedEx : MAPPED_EXCEPTION_ORDER) {

            if (!mappedEx.isAssignableFrom(ex.getClass())) {
                continue;
            }

            if (EXCEPTION_MAPPING.containsKey(mappedEx.getName())) {
                throw (Exception) Class.forName(EXCEPTION_MAPPING.get(mappedEx.getName()))
                        .getConstructor(Throwable.class).newInstance(ex);
            }

            LOG.error("there is no mapping configured for exception class {}", mappedEx.getName());
            throw new GenericSpServerException(ex);
        }

        throw ex;
    }

    private static Exception replaceWithCauseIfConstraintViolationException(final TransactionSystemException rex) {
        Throwable exception = rex;
        do {
            final Throwable cause = exception.getCause();
            if (cause instanceof javax.validation.ConstraintViolationException) {
                return (Exception) cause;
            }
            exception = cause;
        } while (exception != null);

        return rex;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}

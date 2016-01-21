/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.aspects;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;

/**
 * {@link Aspect} catches persistence exceptions and wraps them to custom
 * specific exceptions Additionally it checks and prevents access to certain
 * packages. Logging aspect which logs the call stack
 *
 *
 *
 */
@Aspect
public class SpExceptionMappingAspect implements Ordered {
    private static final Logger LOG = LoggerFactory.getLogger(SpExceptionMappingAspect.class);

    private static final Map<String, String> EXCEPTION_MAPPING = new HashMap<String, String>();

    /**
     * this is required to enable a certain order of exception and to select the
     * most specific mappable exception according to the type hierarchy of the
     * exception.
     */
    private static final List<Class<?>> MAPPED_EXCEPTION_ORDER = new ArrayList<Class<?>>();

    static {
        MAPPED_EXCEPTION_ORDER.add(DuplicateKeyException.class);
        MAPPED_EXCEPTION_ORDER.add(DataIntegrityViolationException.class);
        MAPPED_EXCEPTION_ORDER.add(ConcurrencyFailureException.class);
        MAPPED_EXCEPTION_ORDER.add(AccessDeniedException.class);

        EXCEPTION_MAPPING.put(DuplicateKeyException.class.getName(), EntityAlreadyExistsException.class.getName());

        EXCEPTION_MAPPING.put(ConcurrencyFailureException.class.getName(),
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
    @AfterThrowing(pointcut = "( execution( * org.springframework.transaction..*.*(..)) "
            + " || execution( * org.eclipse.hawkbit.repository.*.*(..)) "
            + " || execution( * org.eclipse.hawkbit.controller.*.*(..)) "
            + " || execution( * org.eclipse.hawkbit.service.*.*(..)) )", throwing = "ex")
    public void catchAndWrapJpaExceptionsService(final Exception ex) throws Throwable {
        final Class<? extends Exception> exClass = ex.getClass();
        Exception newEx = ex;
        LOG.trace("exception occured", ex);
        for (final Class<?> mappedEx : MAPPED_EXCEPTION_ORDER) {

            if (mappedEx.isAssignableFrom(exClass)) {
                if (!EXCEPTION_MAPPING.containsKey(mappedEx.getName())) {
                    LOG.error("there is no mapping configured for exception class {}", mappedEx.getName());
                    newEx = new GenericSpServerException(ex);
                } else {
                    newEx = (Exception) Class.forName(EXCEPTION_MAPPING.get(mappedEx.getName()))
                            .getConstructor(Throwable.class).newInstance(ex);
                }
                break;
            }
        }
        LOG.trace("mapped exception {} to {}", ex.getClass(), newEx.getClass());
        throw newEx;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.core.Ordered#getOrder()
     */
    @Override
    public int getOrder() {
        return 1;
    }
}

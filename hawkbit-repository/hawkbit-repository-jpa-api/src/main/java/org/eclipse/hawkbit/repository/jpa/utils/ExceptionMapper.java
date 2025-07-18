/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.exception.GenericSpServerException;
import org.eclipse.hawkbit.repository.exception.ConcurrentModificationException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.transaction.TransactionSystemException;

/**
 * Maps the exception thrown by the management classes to management exceptions
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Slf4j
public class ExceptionMapper {

    private static final Map<String, String> EXCEPTION_MAPPING = new HashMap<>(4);

    /**
     * this is required to enable a certain order of exception and to select the most specific mappable exception according to the type
     * hierarchy of the exception.
     */
    private static final List<Class<?>> MAPPED_EXCEPTION_ORDER = new ArrayList<>(4);

    static {
        MAPPED_EXCEPTION_ORDER.add(DuplicateKeyException.class);
        MAPPED_EXCEPTION_ORDER.add(OptimisticLockingFailureException.class);
        MAPPED_EXCEPTION_ORDER.add(AccessDeniedException.class);

        EXCEPTION_MAPPING.put(DuplicateKeyException.class.getName(), EntityAlreadyExistsException.class.getName());

        EXCEPTION_MAPPING.put(OptimisticLockingFailureException.class.getName(), ConcurrentModificationException.class.getName());
        EXCEPTION_MAPPING.put(AccessDeniedException.class.getName(), InsufficientPermissionException.class.getName());
        EXCEPTION_MAPPING.put(AuthorizationDeniedException.class.getName(), InsufficientPermissionException.class.getName());
    }

    public static RuntimeException mapRe(final Exception e) {
        final Exception mapped = map(e);
        if (RuntimeException.class.isAssignableFrom(mapped.getClass())) {
            return (RuntimeException) mapped;
        } else {
            return new GenericSpServerException(mapped);
        }
    }

    /**
     * Maps exceptions of the TransactionManager and PreAuthorize and wrap them to custom exceptions.
     *
     * @param e the thrown and catch exception
     * @return the mapped exception
     */
    public static Exception map(final Exception e) {
        if (log.isTraceEnabled()) {
            log.trace("Handling exception {}", e.getClass().getName(), e);
        } else {
            log.debug("Handling exception {}", e.getClass().getName());
        }

        // Workaround for EclipseLink merge where it does not throw ConstraintViolationException directly in case of existing entity update
        if (e instanceof TransactionSystemException transactionSystemException) {
            return replaceWithCauseIfConstraintViolationException(transactionSystemException);
        }

        for (final Class<?> mappedEx : MAPPED_EXCEPTION_ORDER) {
            if (!mappedEx.isAssignableFrom(e.getClass())) {
                continue;
            }

            if (EXCEPTION_MAPPING.containsKey(mappedEx.getName())) {
                try {
                    return (Exception) Class.forName(EXCEPTION_MAPPING.get(mappedEx.getName())).getConstructor(Throwable.class).newInstance(e);
                } catch (final ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                    log.error(ex.getMessage(), ex);
                    return e;
                }
            }

            log.error("there is no mapping configured for exception class {}", mappedEx.getName());
            return new GenericSpServerException(e);
        }

        return e;
    }

    private static Exception replaceWithCauseIfConstraintViolationException(final TransactionSystemException rex) {
        Throwable exception = rex;
        do {
            final Throwable cause = exception.getCause();
            if (cause instanceof jakarta.validation.ConstraintViolationException) {
                return (Exception) cause;
            }
            exception = cause;
        } while (exception != null);

        return rex;
    }
}
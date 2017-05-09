/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.aspects;

import java.sql.SQLException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.orm.jpa.JpaVendorAdapter;
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
    @Autowired
    private JpaVendorAdapter jpaVendorAdapter;

    private final SQLStateSQLExceptionTranslator sqlStateExceptionTranslator = new SQLStateSQLExceptionTranslator();

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
    @AfterThrowing(pointcut = "( execution( * org.springframework.transaction..*.*(..)) "
            + " || execution( * org.eclipse.hawkbit.repository.*.*(..)) )", throwing = "ex")
    // Exception for squid:S00112, squid:S1162
    // It is a AspectJ proxy which deals with exceptions.
    @SuppressWarnings({ "squid:S00112", "squid:S1162" })
    public void catchAndWrapJpaExceptionsService(final Exception ex) throws Throwable {

        LOG.trace("exception occured", ex);
        Exception translatedAccessException = translateEclipseLinkExceptionIfPossible(ex);

        if (translatedAccessException == null && ex instanceof TransactionSystemException) {
            final TransactionSystemException systemException = (TransactionSystemException) ex;
            translatedAccessException = translateEclipseLinkExceptionIfPossible(
                    (Exception) systemException.getOriginalException());
        }

        if (translatedAccessException == null) {
            translatedAccessException = ex;
        }

        Exception mappingException = translatedAccessException;

        LOG.trace("translated excpetion is", translatedAccessException);
        for (final Class<?> mappedEx : MAPPED_EXCEPTION_ORDER) {

            if (mappedEx.isAssignableFrom(translatedAccessException.getClass())) {
                if (!EXCEPTION_MAPPING.containsKey(mappedEx.getName())) {
                    LOG.error("there is no mapping configured for exception class {}", mappedEx.getName());
                    mappingException = new GenericSpServerException(ex);
                } else {
                    mappingException = (Exception) Class.forName(EXCEPTION_MAPPING.get(mappedEx.getName()))
                            .getConstructor(Throwable.class).newInstance(ex);
                }
                break;
            }
        }

        LOG.trace("mapped exception {} to {}", translatedAccessException.getClass(), mappingException.getClass());
        throw mappingException;
    }

    private DataAccessException translateEclipseLinkExceptionIfPossible(final Exception exception) {
        final DataAccessException translatedAccessException = jpaVendorAdapter.getJpaDialect()
                .translateExceptionIfPossible((RuntimeException) exception);
        return translateSQLStateExceptionIfPossible(translatedAccessException);

    }

    /**
     * There is no EclipseLinkExceptionTranslator. So we have to check and
     * translate the exception by the sql error code. Luckily, there we can use
     * {@link SQLStateSQLExceptionTranslator} if we can get a
     * {@link SQLException}.
     *
     * @param accessException
     *            the base access exception from jpa
     * @return the translated accessException
     */
    private DataAccessException translateSQLStateExceptionIfPossible(final DataAccessException accessException) {
        if (!(accessException instanceof JpaSystemException)) {
            return accessException;
        }

        final SQLException ex = findSqlException((JpaSystemException) accessException);

        if (ex == null) {
            return accessException;
        }

        return sqlStateExceptionTranslator.translate(null, null, ex);
    }

    private static SQLException findSqlException(final JpaSystemException jpaSystemException) {
        Throwable exception = jpaSystemException.getCause();
        while (exception != null) {
            final Throwable cause = exception.getCause();
            if (cause instanceof SQLException) {
                return (SQLException) cause;
            }
            exception = cause;
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}

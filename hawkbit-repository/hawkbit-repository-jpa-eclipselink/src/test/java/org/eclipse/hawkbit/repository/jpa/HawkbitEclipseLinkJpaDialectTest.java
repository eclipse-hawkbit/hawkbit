/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Mapping tests for {@link HawkbitEclipseLinkJpaDialect}.
 * <p/>
 * Feature: Unit Tests - Repository<br/>
 * Story: Exception handling
 */
class HawkbitEclipseLinkJpaDialectTest {

    private final HawkbitEclipseLinkJpaDialect hawkBitEclipseLinkJpaDialectUnderTest = new HawkbitEclipseLinkJpaDialect();

    /**
     * Use Case: PersistenceException that can be mapped by EclipseLinkJpaDialect into corresponding DataAccessException.
     */
    @Test
    void jpaOptimisticLockExceptionIsConcurrencyFailureException() {
        assertThat(hawkBitEclipseLinkJpaDialectUnderTest.translateExceptionIfPossible(mock(OptimisticLockException.class)))
                .isInstanceOf(ConcurrencyFailureException.class);
    }

    /**
     * Use Case: PersistenceException that could not be mapped by EclipseLinkJpaDialect directly but
     * instead is wrapped into JpaSystemException. Cause of PersistenceException is an SQLException.
     */
    @Test
    void jpaSystemExceptionWithSqlDeadLockExceptionIsConcurrencyFailureException() {
        final PersistenceException persEception = mock(PersistenceException.class);
        when(persEception.getCause()).thenReturn(new SQLException("simulated transaction ER_LOCK_DEADLOCK", "40001"));

        assertThat(hawkBitEclipseLinkJpaDialectUnderTest.translateExceptionIfPossible(persEception))
                .isInstanceOf(ConcurrencyFailureException.class);
    }

    /**
     * Use Case: PersistenceException that could not be mapped by EclipseLinkJpaDialect directly but instead is wrapped
     *  into JpaSystemException. Cause of PersistenceException is not an SQLException.
     */
    @Test
    void jpaSystemExceptionWithNumberFormatExceptionIsNull() {
        final PersistenceException persEception = mock(PersistenceException.class);
        when(persEception.getCause()).thenReturn(new NumberFormatException());

        assertThat(hawkBitEclipseLinkJpaDialectUnderTest.translateExceptionIfPossible(persEception))
                .isInstanceOf(UncategorizedDataAccessException.class);
    }

    /**
     * Use Case: RuntimeException that could not be mapped by EclipseLinkJpaDialect directly.
     * Cause of RuntimeException is an SQLException.
     */
    @Test
    void runtimeExceptionWithSqlDeadLockExceptionIsConcurrencyFailureException() {
        final RuntimeException persEception = mock(RuntimeException.class);
        when(persEception.getCause()).thenReturn(new SQLException("simulated transaction ER_LOCK_DEADLOCK", "40001"));

        assertThat(hawkBitEclipseLinkJpaDialectUnderTest.translateExceptionIfPossible(persEception))
                .isInstanceOf(ConcurrencyFailureException.class);
    }

    /**
     * Use Case: RuntimeException that could not be mapped by EclipseLinkJpaDialect directly. Cause of 
     * RuntimeException is not an SQLException.
     */
    @Test
    void runtimeExceptionWithNumberFormatExceptionIsNull() {
        final RuntimeException persEception = mock(RuntimeException.class);
        when(persEception.getCause()).thenReturn(new NumberFormatException());

        assertThat(hawkBitEclipseLinkJpaDialectUnderTest.translateExceptionIfPossible(persEception)).isNull();
    }
}
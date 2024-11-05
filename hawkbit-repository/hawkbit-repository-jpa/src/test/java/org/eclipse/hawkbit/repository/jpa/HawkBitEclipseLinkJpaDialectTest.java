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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Mapping tests for {@link HawkBitEclipseLinkJpaDialect}.
 */
@Feature("Unit Tests - Repository")
@Story("Exception handling")
public class HawkBitEclipseLinkJpaDialectTest {

    private final HawkBitEclipseLinkJpaDialect hawkBitEclipseLinkJpaDialectUnderTest = new HawkBitEclipseLinkJpaDialect();

    @Test
    @Description("Use Case: PersistenceException that can be mapped by EclipseLinkJpaDialect into corresponding DataAccessException.")
    public void jpaOptimisticLockExceptionIsConcurrencyFailureException() {
        assertThat(
                hawkBitEclipseLinkJpaDialectUnderTest.translateExceptionIfPossible(mock(OptimisticLockException.class)))
                .isInstanceOf(ConcurrencyFailureException.class);
    }

    @Test
    @Description("Use Case: PersistenceException that could not be mapped by EclipseLinkJpaDialect directly but "
            + "instead is wrapped into JpaSystemException. Cause of PersistenceException is an SQLException.")
    public void jpaSystemExceptionWithSqlDeadLockExceptionIsConcurrencyFailureException() {
        final PersistenceException persEception = mock(PersistenceException.class);
        when(persEception.getCause()).thenReturn(new SQLException("simulated transaction ER_LOCK_DEADLOCK", "40001"));

        assertThat(hawkBitEclipseLinkJpaDialectUnderTest.translateExceptionIfPossible(persEception))
                .isInstanceOf(ConcurrencyFailureException.class);
    }

    @Test
    @Description("Use Case: PersistenceException that could not be mapped by EclipseLinkJpaDialect directly but instead is wrapped"
            + " into JpaSystemException. Cause of PersistenceException is not an SQLException.")
    public void jpaSystemExceptionWithNumberFormatExceptionIsNull() {
        final PersistenceException persEception = mock(PersistenceException.class);
        when(persEception.getCause()).thenReturn(new NumberFormatException());

        assertThat(hawkBitEclipseLinkJpaDialectUnderTest.translateExceptionIfPossible(persEception))
                .isInstanceOf(UncategorizedDataAccessException.class);
    }

    @Test
    @Description("Use Case: RuntimeException that could not be mapped by EclipseLinkJpaDialect directly. Cause of "
            + "RuntimeException is an SQLException.")
    public void runtimeExceptionWithSqlDeadLockExceptionIsConcurrencyFailureException() {
        final RuntimeException persEception = mock(RuntimeException.class);
        when(persEception.getCause()).thenReturn(new SQLException("simulated transaction ER_LOCK_DEADLOCK", "40001"));

        assertThat(hawkBitEclipseLinkJpaDialectUnderTest.translateExceptionIfPossible(persEception))
                .isInstanceOf(ConcurrencyFailureException.class);
    }

    @Test
    @Description("Use Case: RuntimeException that could not be mapped by EclipseLinkJpaDialect directly. Cause of "
            + "RuntimeException is not an SQLException.")
    public void runtimeExceptionWithNumberFormatExceptionIsNull() {
        final RuntimeException persEception = mock(RuntimeException.class);
        when(persEception.getCause()).thenReturn(new NumberFormatException());

        assertThat(hawkBitEclipseLinkJpaDialectUnderTest.translateExceptionIfPossible(persEception)).isNull();
    }

}

/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import static org.assertj.core.api.Assertions.assertThat;
import jakarta.persistence.OptimisticLockException;
import org.eclipse.hawkbit.exception.GenericSpServerException;
import org.eclipse.hawkbit.ql.QueryException;
import org.eclipse.hawkbit.repository.exception.ConcurrentModificationException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationResult;

class ExceptionMapperTest {

    @Test
    void duplicateKeyMappedToEntityAlreadyExists() {
        final DuplicateKeyException cause = new DuplicateKeyException("dup");

        assertMappedTo(ExceptionMapper.mapRe(cause), cause, EntityAlreadyExistsException.class);
    }

    @Test
    void optimisticLockMappedToConcurrentModification() {
        final OptimisticLockingFailureException cause = new OptimisticLockingFailureException("conflict");

        assertMappedTo(ExceptionMapper.mapRe(cause), cause, ConcurrentModificationException.class);
    }

    @Test
    void jpaOptimisticLockSubclassMappedToConcurrentModification() {
        final JpaOptimisticLockingFailureException cause = new JpaOptimisticLockingFailureException(new OptimisticLockException("conflict"));

        assertMappedTo(ExceptionMapper.mapRe(cause), cause, ConcurrentModificationException.class);
    }

    @Test
    void accessDeniedMappedToInsufficientPermission() {
        final var cause = new AccessDeniedException("denied");

        assertMappedTo(ExceptionMapper.mapRe(cause), cause, InsufficientPermissionException.class);
    }

    @Test
    void authorizationDeniedMappedToInsufficientPermission() {
        final var cause = new AuthorizationDeniedException("denied", (AuthorizationResult) () -> false);

        assertMappedTo(ExceptionMapper.mapRe(cause), cause, InsufficientPermissionException.class);
    }


    @Test
    void queryExceptionGenericMappedToGenericSpServerException() {
        final QueryException qe = new QueryException(QueryException.ErrorCode.GENERIC, "generic error");

        assertMappedTo(ExceptionMapper.map(qe), qe, GenericSpServerException.class);
    }

    @Test
    void unknownExceptionReturnedUnchanged() {
        final var unknown = new IllegalStateException("unexpected");

        assertThat(ExceptionMapper.mapRe(unknown)).isSameAs(unknown);
    }

    private static void assertMappedTo(final Exception actualResult, final Exception expectedCause, final Class<?> expectedType) {
        assertThat(actualResult)
                .isInstanceOf(expectedType)
                .hasCause(expectedCause);
    }
}

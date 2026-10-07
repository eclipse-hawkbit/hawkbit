/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Transaction isolation handling against the real persistence stack - custom isolation levels are applied on the transaction's own
 * connection and a following transaction starts with the default isolation again.
 */
class TransactionIsolationTest extends AbstractJpaIntegrationTest {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private DataSource dataSource;

    @Test
    void customIsolationIsAppliedAndNotLeakedToFollowingTransactions() {
        final int defaultIsolation = isolationIn(TransactionDefinition.ISOLATION_DEFAULT);
        assertThat(defaultIsolation).isNotEqualTo(Connection.TRANSACTION_SERIALIZABLE);

        assertThat(isolationIn(TransactionDefinition.ISOLATION_SERIALIZABLE)).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);

        assertThat(isolationIn(TransactionDefinition.ISOLATION_DEFAULT)).isEqualTo(defaultIsolation);
    }

    /**
     * Verifies the database really runs the transaction with the requested isolation level: a commit of a concurrent transaction is
     * visible to a re-read with READ_COMMITTED and is not with REPEATABLE_READ. The level tested is the one the database does not use by
     * default.
     */
    @Test
    void customIsolationIsEnforcedByTheDatabase() {
        final long id = testdataFactory.createTarget("isolation").getId();
        final boolean repeatableRead = isolationIn(TransactionDefinition.ISOLATION_DEFAULT) == Connection.TRANSACTION_READ_COMMITTED;
        final TransactionTemplate template = new TransactionTemplate(txManager);
        template.setIsolationLevel(repeatableRead
                ? TransactionDefinition.ISOLATION_REPEATABLE_READ
                : TransactionDefinition.ISOLATION_READ_COMMITTED);
        final TransactionTemplate concurrent = new TransactionTemplate(txManager);
        concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        final String[] descriptions = template.execute(status -> {
            final String before = description(id);
            concurrent.executeWithoutResult(s -> entityManager
                    .createNativeQuery("UPDATE sp_target SET description = 'changed' WHERE id = " + Jpa.nativeQueryParamPrefix() + "id")
                    .setParameter("id", id)
                    .executeUpdate());
            return new String[] { before, description(id) };
        });

        assertThat(descriptions).isNotNull();
        assertThat(descriptions[0]).isNotEqualTo("changed");
        assertThat(descriptions[1]).isEqualTo(repeatableRead ? descriptions[0] : "changed");
    }

    private String description(final long id) {
        return (String) entityManager
                .createNativeQuery("SELECT description FROM sp_target WHERE id = " + Jpa.nativeQueryParamPrefix() + "id")
                .setParameter("id", id)
                .getSingleResult();
    }

    private int isolationIn(final int isolationLevel) {
        final TransactionTemplate template = new TransactionTemplate(txManager);
        template.setIsolationLevel(isolationLevel);
        // the transaction's own connection - JdbcTemplate joins it (provider independent, EntityManager#unwrap(Connection) is EclipseLink only)
        final Integer isolation = template.execute(status -> new JdbcTemplate(dataSource).execute(Connection::getTransactionIsolation));
        assertThat(isolation).isNotNull();
        return isolation;
    }
}

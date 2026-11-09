/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Callable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatusCode;
import org.eclipse.hawkbit.repository.model.ActionStatusCode;
import org.eclipse.hawkbit.repository.test.util.DisposableSqlTestDatabaseExtension;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Test for {@link ActionStatusCodeRepository}.
 */
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
@ExtendWith(DisposableSqlTestDatabaseExtension.class)
class ActionStatusCodeRepositoryTest extends AbstractJpaIntegrationTest {

    @Autowired
    private ActionStatusCodeRepository actionStatusCodeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    void savesAndFindsByCode() {
        final JpaActionStatusCode entity = new JpaActionStatusCode();
        entity.setCode(1234);
        entity.setName("DOWNLOAD_HASH_MISMATCH");
        entity.setSeverity(ActionStatusCode.Severity.ERROR);
        actionStatusCodeRepository.save(entity);

        assertThat(actionStatusCodeRepository.findByCode(1234)).hasValueSatisfying(c -> {
            assertThat(c.getName()).isEqualTo("DOWNLOAD_HASH_MISMATCH");
            assertThat(c.getSeverity()).isEqualTo(ActionStatusCode.Severity.ERROR);
            assertThat(c.getTenant()).isNotBlank();
        });
    }

    @Test
    void sameCodeTwiceInTenantViolatesUniqueConstraint() {
        // save first entity with code 5678
        final JpaActionStatusCode entity1 = new JpaActionStatusCode();
        entity1.setCode(5678);
        entity1.setName("FIRST");
        entity1.setSeverity(ActionStatusCode.Severity.INFO);
        actionStatusCodeRepository.save(entity1);

        // attempt to save second entity with same code in a transaction - should violate unique constraint
        final JpaActionStatusCode entity2 = new JpaActionStatusCode();
        entity2.setCode(5678); // same code
        entity2.setName("SECOND");
        entity2.setSeverity(ActionStatusCode.Severity.WARN);

        // EclipseLink throws PersistenceException, Hibernate throws EntityAlreadyExistsException,
        // Spring may translate to DataIntegrityViolationException
        assertThatThrownBy(() -> new TransactionTemplate(txManager).executeWithoutResult(status -> {
            actionStatusCodeRepository.save(entity2);
            entityManager.flush();
        })).satisfiesAnyOf(
                ex -> assertThat(ex).isInstanceOf(PersistenceException.class).hasMessageContaining("UK_ACTION_STATUS_CODE"),
                ex -> assertThat(ex).isInstanceOf(EntityAlreadyExistsException.class),
                ex -> assertThat(ex).isInstanceOf(DataIntegrityViolationException.class)
        );
    }

    @Test
    void existsByCodeReturnsTrueForExistingCode() {
        final JpaActionStatusCode entity = new JpaActionStatusCode();
        entity.setCode(5678);
        entity.setName("EXISTS_TEST");
        entity.setSeverity(ActionStatusCode.Severity.INFO);
        actionStatusCodeRepository.save(entity);

        assertThat(actionStatusCodeRepository.existsByCode(5678)).isTrue();
        assertThat(actionStatusCodeRepository.existsByCode(9999)).isFalse();
    }

    @Test
    void tenantDeleteRemovesCatalogue() throws Exception {
        final String tenant = "t1";
        // create entity in tenant t1
        runAsTenant(tenant, () -> {
            final JpaActionStatusCode entity = new JpaActionStatusCode();
            entity.setCode(9999);
            entity.setName("TENANT_DELETE_TEST");
            entity.setSeverity(ActionStatusCode.Severity.DEBUG);
            return actionStatusCodeRepository.save(entity);
        });

        // verify it exists
        final long countBefore = runAsTenant(tenant, () -> actionStatusCodeRepository.count());
        assertThat(countBefore).isEqualTo(1);

        // delete tenant
        systemManagement.deleteTenant(tenant);

        // verify it's gone
        final long countAfter = runAsTenant(tenant, () -> actionStatusCodeRepository.count());
        assertThat(countAfter).isZero();
    }

    private <T> T runAsTenant(final String tenant, final Callable<T> callable) throws Exception {
        return SecurityContextSwitch.callAs(SecurityContextSwitch.withTenantAndUserAndAllPermissions(tenant, "user"), callable);
    }
}

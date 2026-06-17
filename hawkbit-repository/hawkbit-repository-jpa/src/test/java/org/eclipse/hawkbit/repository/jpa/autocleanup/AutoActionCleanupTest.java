/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.autocleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_AUTO_EXPIRY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_AUTO_STATUS;

import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.Jpa;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Test class for {@link AutoActionCleanup}.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Action cleanup handler
 */
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
class AutoActionCleanupTest extends AbstractJpaIntegrationTest {

    @Autowired
    private AutoActionCleanup autoActionCleanup;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager txManager;

    /**
     * Verifies that running actions are not cleaned up.
     */
    @Test
    void runningActionsAreNotCleanedUp() {
        // cleanup config for this test case
        setupCleanupConfiguration(0, Action.Status.CANCELED, Action.Status.ERROR);

        final Target trg1 = testdataFactory.createTarget("trg1");
        final Target trg2 = testdataFactory.createTarget("trg2");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");

        assignDistributionSet(ds1.getId(), trg1.getControllerId());
        assignDistributionSet(ds2.getId(), trg2.getControllerId());

        assertThat(actionRepository.count()).isEqualTo(2);

        waitNextMillis();
        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(2);
    }

    /**
     * Verifies that nothing is cleaned up if the cleanup is disabled.
     */
    @Test
    void cleanupDisabled() {
        // cleanup config for this test case
        setupCleanupConfiguration(-1L, Action.Status.CANCELED);

        final Target trg1 = testdataFactory.createTarget("trg1");
        final Target trg2 = testdataFactory.createTarget("trg2");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");

        final Long action1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), trg1.getControllerId()));
        assignDistributionSet(ds2.getId(), trg2.getControllerId());

        setActionToCanceled(action1);

        assertThat(actionRepository.count()).isEqualTo(2);

        waitNextMillis();
        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(2);
    }

    /**
     * Verifies that canceled and failed actions are cleaned up.
     */
    @Test
    void canceledAndFailedActionsAreCleanedUp() {
        // cleanup config for this test case
        setupCleanupConfiguration(0, Action.Status.CANCELED, Action.Status.ERROR);

        final Target trg1 = testdataFactory.createTarget("trg1");
        final Target trg2 = testdataFactory.createTarget("trg2");
        final Target trg3 = testdataFactory.createTarget("trg3");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");

        final Long action1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), trg1.getControllerId()));
        final Long action2 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg2.getControllerId()));
        final Long action3 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg3.getControllerId()));

        assertThat(actionRepository.count()).isEqualTo(3);

        setActionToCanceled(action1);
        setActionToFailed(action2);

        assertThat(actionRepository.count()).isEqualTo(3);

        waitNextMillis();
        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(1);
        assertThat(actionRepository.findById(action3)).isPresent();
    }

    /**
     * Verifies that canceled actions are cleaned up.
     */
    @Test
    void canceledActionsAreCleanedUp() {
        // cleanup config for this test case
        setupCleanupConfiguration(0, Action.Status.CANCELED);

        final Target trg1 = testdataFactory.createTarget("trg1");
        final Target trg2 = testdataFactory.createTarget("trg2");
        final Target trg3 = testdataFactory.createTarget("trg3");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");

        final Long action1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), trg1.getControllerId()));
        final Long action2 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg2.getControllerId()));
        final Long action3 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg3.getControllerId()));

        assertThat(actionRepository.count()).isEqualTo(3);

        setActionToCanceled(action1);
        setActionToFailed(action2);

        assertThat(actionRepository.count()).isEqualTo(3);

        waitNextMillis();
        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(2);
        assertThat(actionRepository.findById(action2)).isPresent();
        assertThat(actionRepository.findById(action3)).isPresent();
    }

    /**
     * Verifies that canceled and failed actions are cleaned up once they expired.
     */
    @Test
    void canceledAndFailedActionsAreCleanedUpWhenExpired() {
        final long expiryMs = 3_600_000L; // 1 hour - large to avoid timing issues
        setupCleanupConfiguration(expiryMs, Action.Status.CANCELED, Action.Status.ERROR);

        final Target trg1 = testdataFactory.createTarget("trg1");
        final Target trg2 = testdataFactory.createTarget("trg2");
        final Target trg3 = testdataFactory.createTarget("trg3");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");

        final Long action1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), trg1.getControllerId()));
        final Long action2 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg2.getControllerId()));
        final Long action3 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg3.getControllerId()));

        assertThat(actionRepository.count()).isEqualTo(3);

        setActionToCanceled(action1);
        setActionToFailed(action2);

        // actions have not expired yet (last_modified_at is recent)
        waitNextMillis();
        autoActionCleanup.run();
        assertThat(actionRepository.count()).isEqualTo(3);

        // simulate expiry by backdating last_modified_at via native SQL to bypass @LastModifiedDate
        final long expired = System.currentTimeMillis() - expiryMs - 1000;
        final char prefix = Jpa.nativeQueryParamPrefix();
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                            "UPDATE sp_action SET last_modified_at = " + prefix + "ts WHERE id IN (" + prefix + "id1, " + prefix + "id2)")
                    .setParameter("ts", expired)
                    .setParameter("id1", action1)
                    .setParameter("id2", action2)
                    .executeUpdate();
            entityManager.flush();
        });

        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(1);
        assertThat(actionRepository.findById(action3)).isPresent();
    }

    private void setActionToCanceled(final Long id) {
        deploymentManagement.cancelAction(id);
        deploymentManagement.forceQuitAction(id);
    }

    private void setActionToFailed(final Long id) {
        controllerManagement.addUpdateActionStatus(ActionStatusCreate.builder().actionId(id).status(Status.ERROR).build());
    }

    private void setupCleanupConfiguration(final long expiry, final Status... status) {
        tenantConfigurationManagement().addOrUpdateConfiguration(ACTION_CLEANUP_AUTO_EXPIRY, expiry);
        tenantConfigurationManagement().addOrUpdateConfiguration(
                ACTION_CLEANUP_AUTO_STATUS, Arrays.stream(status).map(Status::toString).collect(Collectors.joining(",")));
    }
}
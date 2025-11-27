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

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
        setupCleanupConfiguration( 0, Action.Status.CANCELED, Action.Status.ERROR);

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
    @SuppressWarnings("squid:S2925")
    void canceledAndFailedActionsAreCleanedUpWhenExpired() throws InterruptedException {
        // cleanup config for this test case
        setupCleanupConfiguration(500, Action.Status.CANCELED, Action.Status.ERROR);

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

        waitNextMillis();
        autoActionCleanup.run();

        // actions have not expired yet
        assertThat(actionRepository.count()).isEqualTo(3);

        // wait for expiry to elapse
        Thread.sleep(800);

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
/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autocleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ACTION_EXPIRY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ACTION_STATUS;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ENABLED;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test class for {@link AutoActionCleanup}.
 *
 */
@Feature("Component Tests - Repository")
@Story("Action cleanup handler")
public class AutoActionCleanupTest extends AbstractJpaIntegrationTest {

    @Autowired
    private AutoActionCleanup autoActionCleanup;

    @Test
    @Description("Verifies that running actions are not cleaned up.")
    public void runningActionsAreNotCleanedUp() {

        // cleanup config for this test case
        setupCleanupConfiguration(true, 0, Action.Status.CANCELED, Action.Status.ERROR);

        final Target trg1 = testdataFactory.createTarget("trg1");
        final Target trg2 = testdataFactory.createTarget("trg2");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");

        deploymentManagement.assignDistributionSet(ds1.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg1.getControllerId()));
        deploymentManagement.assignDistributionSet(ds2.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg2.getControllerId()));

        assertThat(actionRepository.count()).isEqualTo(2);

        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(2);

    }

    @Test
    @Description("Verifies that nothing is cleaned up if the cleanup is disabled.")
    public void cleanupDisabled() {

        // cleanup config for this test case
        setupCleanupConfiguration(false, 0, Action.Status.CANCELED);

        final Target trg1 = testdataFactory.createTarget("trg1");
        final Target trg2 = testdataFactory.createTarget("trg2");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");

        final Long action1 = deploymentManagement.assignDistributionSet(ds1.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg1.getControllerId())).getActionIds().get(0);
        deploymentManagement.assignDistributionSet(ds2.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg2.getControllerId()));

        setActionToCanceled(action1);

        assertThat(actionRepository.count()).isEqualTo(2);

        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(2);

    }

    @Test
    @Description("Verifies that canceled and failed actions are cleaned up.")
    public void canceledAndFailedActionsAreCleanedUp() throws Exception{

        // cleanup config for this test case
        setupCleanupConfiguration(true, 0, Action.Status.CANCELED, Action.Status.ERROR);

        final Target trg1 = testdataFactory.createTarget("trg1");
        final Target trg2 = testdataFactory.createTarget("trg2");
        final Target trg3 = testdataFactory.createTarget("trg3");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");

        final Long action1 = deploymentManagement.assignDistributionSet(ds1.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg1.getControllerId())).getActionIds().get(0);
        final Long action2 = deploymentManagement.assignDistributionSet(ds2.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg2.getControllerId())).getActionIds().get(0);
        final Long action3 = deploymentManagement.assignDistributionSet(ds2.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg3.getControllerId())).getActionIds().get(0);

        assertThat(actionRepository.count()).isEqualTo(3);

        setActionToCanceled(action1);
        setActionToFailed(action2);

        assertThat(actionRepository.count()).isEqualTo(3);

        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(1);
        assertThat(actionRepository.getById(action3)).isPresent();

    }

    @Test
    @Description("Verifies that canceled actions are cleaned up.")
    public void canceledActionsAreCleanedUp() {

        // cleanup config for this test case
        setupCleanupConfiguration(true, 0, Action.Status.CANCELED);

        final Target trg1 = testdataFactory.createTarget("trg1");
        final Target trg2 = testdataFactory.createTarget("trg2");
        final Target trg3 = testdataFactory.createTarget("trg3");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");

        final Long action1 = deploymentManagement.assignDistributionSet(ds1.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg1.getControllerId())).getActionIds().get(0);
        final Long action2 = deploymentManagement.assignDistributionSet(ds2.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg2.getControllerId())).getActionIds().get(0);
        final Long action3 = deploymentManagement.assignDistributionSet(ds2.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg3.getControllerId())).getActionIds().get(0);

        assertThat(actionRepository.count()).isEqualTo(3);

        setActionToCanceled(action1);
        setActionToFailed(action2);

        assertThat(actionRepository.count()).isEqualTo(3);

        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(2);
        assertThat(actionRepository.getById(action2)).isPresent();
        assertThat(actionRepository.getById(action3)).isPresent();

    }

    @Test
    @Description("Verifies that canceled and failed actions are cleaned up once they expired.")
    @SuppressWarnings("squid:S2925")
    public void canceledAndFailedActionsAreCleanedUpWhenExpired() throws InterruptedException {

        // cleanup config for this test case
        setupCleanupConfiguration(true, 500, Action.Status.CANCELED, Action.Status.ERROR);

        final Target trg1 = testdataFactory.createTarget("trg1");
        final Target trg2 = testdataFactory.createTarget("trg2");
        final Target trg3 = testdataFactory.createTarget("trg3");

        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");

        final Long action1 = deploymentManagement.assignDistributionSet(ds1.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg1.getControllerId())).getActionIds().get(0);
        final Long action2 = deploymentManagement.assignDistributionSet(ds2.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg2.getControllerId())).getActionIds().get(0);
        final Long action3 = deploymentManagement.assignDistributionSet(ds2.getId(), ActionType.FORCED, 0,
                Collections.singletonList(trg3.getControllerId())).getActionIds().get(0);

        assertThat(actionRepository.count()).isEqualTo(3);

        setActionToCanceled(action1);
        setActionToFailed(action2);

        autoActionCleanup.run();

        // actions have not expired yet
        assertThat(actionRepository.count()).isEqualTo(3);

        // wait for expiry to elapse
        Thread.sleep(800);

        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(1);
        assertThat(actionRepository.getById(action3)).isPresent();

    }

    private void setActionToCanceled(final Long id) {
        deploymentManagement.cancelAction(id);
        deploymentManagement.forceQuitAction(id);
    }

    private void setActionToFailed(final Long id) {
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(id).status(Status.ERROR));
    }

    private void setupCleanupConfiguration(final boolean cleanupEnabled, final long expiry, final Status... status) {
        tenantConfigurationManagement.addOrUpdateConfiguration(ACTION_CLEANUP_ENABLED, cleanupEnabled);
        tenantConfigurationManagement.addOrUpdateConfiguration(ACTION_CLEANUP_ACTION_EXPIRY, expiry);
        tenantConfigurationManagement.addOrUpdateConfiguration(ACTION_CLEANUP_ACTION_STATUS,
                Arrays.stream(status).map(Status::toString).collect(Collectors.joining(",")));
    }

}

/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.event.remote.ActionStatusUpdateEvent;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Junit tests for ActionStatusUpdateHandlerService.
 */
@Feature("Component Tests - Repository")
@Story("Rollout Status Handler")
public class ActionStatusUpdateHandlerServiceTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that the status update(finished state) for a distribution id is updated in the action database.")
    public void verifyStatusUpdate() {

        String tenant = "test";

        // generate data in database
        JpaDistributionSet ds = generateDistributionSet();
        JpaTarget target = (JpaTarget) testdataFactory.createTarget();
        JpaAction action = generateAction(101L, ds, target, tenant);

        ActionStatusUpdateHandlerService rolloutStatusHandlerService = new ActionStatusUpdateHandlerService(
                this.controllerManagement, this.entityFactory, this.systemSecurityContext);

        // initiate the test
        ActionStatusUpdateEvent targetStatus = new ActionStatusUpdateEvent("default", action.getId(), Status.FINISHED,
                new ArrayList<>());
        rolloutStatusHandlerService.handle(targetStatus);

        // verify if intended dataSetId is really installed
        Long installedId = this.targetRepository.findById(target.getId()).get().getInstalledDistributionSet().getId();
        assertThat(installedId).isEqualTo(ds.getId()).as("last installedId must be updated in the database");

        // verify that action is database is marked inactive.
        Optional<JpaAction> activeAction = this.actionRepository.findById(action.getId());
        assertThat(activeAction.isPresent()).isTrue().as("action must be present in the database after status update");
        assertThat(activeAction.get().isActive()).isFalse()
                .as("on finished status update, the action must be marked in active");
        assertThat(activeAction.get().getStatus()).isEqualTo(Status.FINISHED)
                .as("saved action must have finished status");
    }

    private JpaDistributionSet generateDistributionSet() {
        JpaSoftwareModule swModule = (JpaSoftwareModule) testdataFactory.createSoftwareModuleApp();
        JpaDistributionSetType type = (JpaDistributionSetType) testdataFactory.findOrCreateDefaultTestDsType();
        return (JpaDistributionSet) testdataFactory.createDistributionSet("test ds", "v1", type,
                Arrays.asList(swModule));
    }

    private JpaAction generateAction(final Long actionId, final DistributionSet distributionSet, final Target target,
            final String tenant) {
        final JpaAction action = new JpaAction();
        action.setId(actionId);
        action.setActive(true);
        action.setDistributionSet(distributionSet);
        action.setActionType(ActionType.FORCED);
        action.setTenant(tenant);
        action.setStatus(Status.SCHEDULED);
        action.setTarget(target);
        return actionRepository.save(action);
    }

}

/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;

import java.util.Set;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "hawkbit.acm.access-controller.enabled=true")
class ActionAccessControllerTest extends AbstractJpaIntegrationTest {

    private TargetType targetType1;
    private TargetType targetType2;
    private Target target1;
    private Target target2;
    private Action action11;
    private Action action21;
    private Action action22;

    @BeforeEach
    void setUp() {
        targetType1 = testdataFactory.createTargetType("Type1", Set.of());
        targetType2 = testdataFactory.createTargetType("Type2", Set.of());
        target1 = testdataFactory.createTarget("controller1", "Controller 1", targetType1);
        target2 = testdataFactory.createTarget("controller2", "Controller 2", targetType2);
        final DistributionSet ds = testdataFactory.createDistributionSet();
        action11 = createAction(target1, ds);
        action21 = createAction(target2, ds);
        action22 = createAction(target2, ds);
    }

    @Test
    void filterByControllerId() {
        runAs(withUser("user", READ_TARGET + "/controllerId==" + target1.getControllerId()), () -> {
            assertThat(deploymentManagement.findAction(action11.getId())).isPresent();
            assertThat(deploymentManagement.findAction(action21.getId())).isEmpty();
            assertThat(deploymentManagement.findAction(action22.getId())).isEmpty();
            assertThat(deploymentManagement.findActionsAll(UNPAGED).getContent()).hasSize(1);
        });
        runAs(withUser("user", READ_TARGET + "/controllerId==" + target2.getControllerId()), () -> {
            assertThat(deploymentManagement.findAction(action11.getId())).isEmpty();
            assertThat(deploymentManagement.findAction(action21.getId())).isPresent();
            assertThat(deploymentManagement.findAction(action22.getId())).isPresent();
            assertThat(deploymentManagement.findActionsAll(UNPAGED).getContent()).hasSize(2);
        });
    }

    @Test
    void filterByTargetTypeId() {
        runAs(withUser("user", READ_TARGET + "/type.id==" + targetType1.getId()), () -> {
            assertThat(deploymentManagement.findAction(action11.getId())).isPresent();
            assertThat(deploymentManagement.findAction(action21.getId())).isEmpty();
            assertThat(deploymentManagement.findAction(action22.getId())).isEmpty();
            assertThat(deploymentManagement.findActionsAll(UNPAGED).getContent()).hasSize(1);
        });
        runAs(withUser("user", READ_TARGET + "/type.id=in=" + targetType2.getId()), () -> {
            assertThat(deploymentManagement.findAction(action11.getId())).isEmpty();
            assertThat(deploymentManagement.findAction(action21.getId())).isPresent();
            assertThat(deploymentManagement.findAction(action22.getId())).isPresent();
            assertThat(deploymentManagement.findActionsAll(UNPAGED).getContent()).hasSize(2);
        });
    }

    private Action createAction(final Target target, final DistributionSet ds) {
        final JpaAction generateAction = new JpaAction();
        generateAction.setActionType(Action.ActionType.FORCED);
        generateAction.setTarget(target);
        generateAction.setDistributionSet(ds);
        generateAction.setStatus(Action.Status.RUNNING);
        generateAction.setInitiatedBy("DEFAULT");
        generateAction.setWeight(1000);

        return actionRepository.save(generateAction);
    }
}
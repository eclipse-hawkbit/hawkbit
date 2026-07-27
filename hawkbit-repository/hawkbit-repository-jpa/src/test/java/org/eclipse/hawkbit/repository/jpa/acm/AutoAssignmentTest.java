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
import static org.eclipse.hawkbit.auth.SpPermission.DELETE_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.callAs;

import java.util.Optional;

import org.eclipse.hawkbit.repository.AutoAssignHandler;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.jpa.scheduler.AutoAssignScheduler;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AutoAssignmentTest extends AbstractAccessControllerManagementTest {

    @Autowired
    AutoAssignHandler autoAssignHandler;

    @Test
    void verifyOnlyUpdatableTargetsArePartOfAutoAssignmentByScheduler() throws Exception {
        // auto assign scheduler apply stored access control context and the context is correctly applied
        verifyOnlyUpdatableTargetsArePartOfAutoAssignment(
                () -> new AutoAssignScheduler(systemManagement, autoAssignHandler, 1, Optional.empty()).autoAssignScheduler());
    }

    @Test
    void verifyOnlyUpdatableTargetsArePartOfAutoAssignment() throws Exception {
        verifyOnlyUpdatableTargetsArePartOfAutoAssignment(autoAssignHandler::handleAll);
    }

    @Test
    void verifyOnlyUpdatableTargetsWillGetAssignmentOnSingleCheck() throws Exception {
        verifyOnlyUpdatableTargetsArePartOfAutoAssignment(() -> {
            autoAssignHandler.handleSingleTarget(target1Type1.getControllerId());
            autoAssignHandler.handleSingleTarget(target2Type2.getControllerId());
            autoAssignHandler.handleSingleTarget(target3Type2.getControllerId());
        });
    }

    private void verifyOnlyUpdatableTargetsArePartOfAutoAssignment(final Runnable assigner) throws Exception {
        final AutoAssignment autoAssignment = callAs(withAuthorities(
                READ_TARGET + "/controllerid==*",
                UPDATE_TARGET + "/type.id==" + targetType2.getId(), // only updatable (i.e. of targetType2) shall be assigned
                DELETE_TARGET + "/type.id==" + targetType1.getId(),
                READ_DISTRIBUTION_SET + "/type.id==" + dsType2.getId()),
                () -> autoAssignmentManagement
                        .create(AutoAssignmentManagement.Create.builder().name("testAutoAssignment").targetFilterQuery("controllerid==*").distributionSet(ds2Type2).build()));

        // do the assignment
        assigner.run();

        assertThat(targetManagement.findByAssignedDistributionSet(autoAssignment.getDistributionSet().getId(), UNPAGED)
                .map(Identifiable::getId).toList())
                .as("Only updatable targets should be part of the rollout")
                // all targets are distribution set type 2 compatible, but since user has UPDATE_TARGET only for targets of type 2
                // only target2 and target3 shall be assigned
                .containsExactlyInAnyOrder(target2Type2.getId(), target3Type2.getId());
    }
}
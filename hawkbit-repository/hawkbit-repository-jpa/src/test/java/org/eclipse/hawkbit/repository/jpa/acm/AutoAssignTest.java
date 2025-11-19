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
import static org.eclipse.hawkbit.auth.SpPermission.CREATE_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.DELETE_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.callAs;

import java.util.Optional;

import org.eclipse.hawkbit.repository.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.jpa.scheduler.AutoAssignScheduler;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;

class AutoAssignTest extends AbstractAccessControllerManagementTest {

    @Autowired
    AutoAssignExecutor autoAssignExecutor;

    @Autowired
    LockRegistry lockRegistry;

    @Test
    void verifyOnlyUpdatableTargetsArePartOfAutoAssignmentByScheduler() throws Exception {
        // auto assign scheduler apply stored access control context and the context is correctly applied
        verifyOnlyUpdatableTargetsArePartOfAutoAssignment(
                () -> new AutoAssignScheduler(systemManagement, autoAssignExecutor, lockRegistry, Optional.empty()).autoAssignScheduler());
    }

    @Test
    void verifyOnlyUpdatableTargetsArePartOfAutoAssignment() throws Exception {
        verifyOnlyUpdatableTargetsArePartOfAutoAssignment(autoAssignExecutor::checkAllTargets);
    }

    @Test
    void verifyOnlyUpdatableTargetsWillGetAssignmentOnSingleCheck() throws Exception {
        verifyOnlyUpdatableTargetsArePartOfAutoAssignment(() -> {
            autoAssignExecutor.checkSingleTarget(target1Type1.getControllerId());
            autoAssignExecutor.checkSingleTarget(target2Type2.getControllerId());
            autoAssignExecutor.checkSingleTarget(target3Type2.getControllerId());
        });
    }

    private void verifyOnlyUpdatableTargetsArePartOfAutoAssignment(final Runnable assigner) throws Exception {
        final TargetFilterQuery targetFilterQuery = callAs(withAuthorities(
                        CREATE_TARGET,
                        READ_TARGET + "/controllerid==*",
                        UPDATE_TARGET + "/type.id==" + targetType2.getId(), // only updatable (i.e. of targetType2) shall be assigned
                        DELETE_TARGET + "/type.id==" + targetType1.getId(),
                        READ_DISTRIBUTION_SET + "/type.id==" + dsType2.getId()),
                () -> {
                    final TargetFilterQuery targetFilter = targetFilterQueryManagement
                            .create(TargetFilterQueryManagement.Create.builder().name("testAutoAssignment").query("controllerid==*").build());
                    return targetFilterQueryManagement.updateAutoAssignDS(
                            new AutoAssignDistributionSetUpdate(targetFilter.getId()).ds(ds2Type2.getId()));
                });

        // do the assignment
        assigner.run();

        assertThat(targetManagement.findByAssignedDistributionSet(targetFilterQuery.getAutoAssignDistributionSet().getId(), UNPAGED)
                .map(Identifiable::getId).toList())
                .as("Only updatable targets should be part of the rollout")
                // all targets are distribution set type 2 compatible, but since user has UPDATE_TARGET only for targets of type 2
                // only target2 and target3 shall be assigned
                .containsExactly(target2Type2.getId(), target3Type2.getId());
    }
}
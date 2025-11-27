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
import static org.eclipse.hawkbit.auth.SpPermission.READ_ROLLOUT;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.repository.model.TotalTargetCountStatus.Status.NOTSTARTED;
import static org.eclipse.hawkbit.repository.model.TotalTargetCountStatus.Status.RUNNING;
import static org.eclipse.hawkbit.repository.model.TotalTargetCountStatus.Status.SCHEDULED;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.junit.jupiter.api.Test;

class RolloutManagementTest extends AbstractAccessControllerManagementTest {

    /**
     * Test is verifying that rollout details are showing details without restrictions
     */
    @Test
    void verifyRolloutDetailsAreShowingRealCount() {
        final Rollout rollout = testdataFactory.createRolloutByVariables(randomString(16), "description", 4, "id==*", ds2Type2, "50", "80");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        runAs(withAuthorities(READ_TARGET + "/type.id==" + targetType1.getId(), READ_ROLLOUT), () -> {
            assertThat(targetManagement.findAll(UNPAGED).stream().map(Identifiable::getId).toList())
                    .as("Only readable targets should be returned").containsExactly(target1Type1.getId());
            assertThat(rolloutManagement.getWithDetailedStatus(rollout.getId())).satisfies(rolloutWithDetails -> {
                assertThat(rolloutWithDetails.getTotalTargets()).as("All targets shall be returned").isEqualTo(3);
                assertThat(rolloutWithDetails.getRolloutGroupsCreated()).isEqualTo(rollout.getRolloutGroupsCreated());
                assertThat(rolloutWithDetails.getTotalTargetCountStatus().getFinishedPercent()).isZero();
                assertThat(rolloutWithDetails.getTotalTargetCountStatus().getTotalTargetCountByStatus(RUNNING)).isEqualTo(1);
                assertThat(rolloutWithDetails.getTotalTargetCountStatus().getTotalTargetCountByStatus(SCHEDULED)).isEqualTo(2);
                assertThat(rolloutWithDetails.getTotalTargetCountStatus().getTotalTargetCountByStatus(NOTSTARTED)).isZero();
            });
        });
    }
}
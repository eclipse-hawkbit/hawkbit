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
import static org.eclipse.hawkbit.auth.SpPermission.CREATE_ROLLOUT;
import static org.eclipse.hawkbit.auth.SpPermission.HANDLE_ROLLOUT;
import static org.eclipse.hawkbit.auth.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_ROLLOUT;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.hawkbit.context.System;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.jpa.scheduler.RolloutScheduler;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;

class RolloutExecutionTest extends AbstractAccessControllerManagementTest {

    @Test
    void verifyOnlyUpdatableTargetsArePartOfRolloutExecutedByScheduler() {
        verify(new RolloutScheduler(rolloutHandler, systemManagement, 1, Optional.empty())::runningRolloutScheduler);
    }

    @Test
    void verifyOnlyUpdatableTargetsArePartOfRollout() {
        verify(() -> System.asSystem(rolloutHandler::handleAll));
    }

    private void verify(final Runnable run) {
        final Target[] expectedTargets = new Target[] { target1Type1 };
        runAs(withAuthorities(
                        READ_TARGET, UPDATE_TARGET + "/type.id==" + targetType1.getId(),
                        READ_DISTRIBUTION_SET,
                        CREATE_ROLLOUT, READ_ROLLOUT, HANDLE_ROLLOUT),
                () -> extracted(run, expectedTargets));
        runAs(withAuthorities(
                        READ_TARGET, UPDATE_TARGET + "/type.id==" + targetType2.getId(),
                        READ_DISTRIBUTION_SET,
                        CREATE_ROLLOUT, READ_ROLLOUT, HANDLE_ROLLOUT),
                () -> extracted(run, target2Type2, target3Type2));
    }

    private void extracted(final Runnable run, final Target... expectedTargets) {
        assertThat(targetManagement.findAll(UNPAGED))
                .as("targetManagement#findAll operation should see all created targets")
                .hasSize(3);

        final Rollout rollout = testdataFactory.createRolloutByVariables(randomString(16), "description", 5, "id==*", ds2Type2, "50", "80");
        rolloutManagement.start(rollout.getId());

        run.run();

        assertThat(rolloutGroupManagement.findByRollout(rollout.getId(), UNPAGED).getContent().stream()
                .flatMap(rolloutGroup -> rolloutGroupManagement.findTargetsOfRolloutGroup(rolloutGroup.getId(), UNPAGED).stream()))
                .extracting(Identifiable::getId)
                .as("Only updatable targets should be part of the rollout")
                .containsExactly(Arrays.stream(expectedTargets).map(Identifiable::getId).toArray(Long[]::new));
    }
}
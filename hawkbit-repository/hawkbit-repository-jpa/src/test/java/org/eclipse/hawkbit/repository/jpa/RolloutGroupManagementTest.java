/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("Rollout Management")
class RolloutGroupManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(rolloutGroupManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(rolloutGroupManagement.getWithDetailedStatus(NOT_EXIST_IDL)).isNotPresent();

    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = RolloutDeletedEvent.class, count = 0),
            @Expect(type = RolloutGroupCreatedEvent.class, count = 5),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 5),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = RolloutUpdatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 125),
            @Expect(type = RolloutCreatedEvent.class, count = 1) })

    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        testdataFactory.createRollout("xxx");

        verifyThrownExceptionBy(() -> rolloutGroupManagement.countByRollout(NOT_EXIST_IDL), "Rollout");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.countTargetsOfRolloutsGroup(NOT_EXIST_IDL),
                "RolloutGroup");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(PAGE, NOT_EXIST_IDL),
                "Rollout");
        verifyThrownExceptionBy(
                () -> rolloutGroupManagement.findAllTargetsOfRolloutGroupWithActionStatus(PAGE, NOT_EXIST_IDL),
                "RolloutGroup");
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findByRolloutAndRsql(PAGE, NOT_EXIST_IDL, "name==*"),
                "Rollout");

        verifyThrownExceptionBy(() -> rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE, NOT_EXIST_IDL),
                "RolloutGroup");
        verifyThrownExceptionBy(
                () -> rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(PAGE, NOT_EXIST_IDL, "name==*"),
                "RolloutGroup");
    }

    @Test
    @Description("Verifies that the returned result considers the provided sort parameters.")
    void findAllTargetsOfRolloutGroupWithActionStatusConsidersSorting() {
        final String prefix = RandomStringUtils.randomAlphanumeric(5);
        final Rollout rollout = testdataFactory.createRollout(prefix);
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId())
                .getContent();
        final RolloutGroup rolloutGroup = rolloutGroups.get(0);
        rolloutManagement.handleRollouts();
        rolloutManagement.start(rollout.getId());
        rolloutManagement.handleRollouts();
        rolloutManagement.pauseRollout(rollout.getId());
        rolloutManagement.handleRollouts();
        final List<Target> targets = rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE, rolloutGroup.getId())
                .getContent();
        Target targetCancelled = targets.get(0);
        final Action actionCancelled = deploymentManagement.findActionsByTarget(targetCancelled.getControllerId(), PAGE)
                .getContent().get(0);
        deploymentManagement.cancelAction(actionCancelled.getId());
        deploymentManagement.forceQuitAction(actionCancelled.getId());
        targetCancelled = reloadTarget(targetCancelled);
        Target targetCancelling = targets.get(1);
        final Action actionCancelling = deploymentManagement
                .findActionsByTarget(targetCancelling.getControllerId(), PAGE).getContent().get(0);
        deploymentManagement.cancelAction(actionCancelling.getId());
        targetCancelling = reloadTarget(targetCancelling);

        final List<TargetWithActionStatus> targetsWithActionStatus = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(PageRequest.of(0, 500, Sort.by(Direction.DESC, "status")),
                        rolloutGroup.getId())
                .getContent();
        assertThat(targetsWithActionStatus.get(0).getTarget()).isEqualTo(targetCancelling);
        assertThat(targetsWithActionStatus.get(1).getTarget()).isEqualTo(targetCancelled);

        final List<TargetWithActionStatus> targetsWithActionStatusOrderedByNameDesc = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(PageRequest.of(0, 500, Sort.by(Direction.DESC, "name")),
                        rolloutGroup.getId())
                .getContent();
        assertThatListIsSortedByTargetName(targetsWithActionStatusOrderedByNameDesc, Direction.DESC);

        final List<TargetWithActionStatus> targetsWithActionStatusOrderedByNameAsc = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(PageRequest.of(0, 500, Sort.by(Direction.ASC, "name")),
                        rolloutGroup.getId())
                .getContent();
        assertThatListIsSortedByTargetName(targetsWithActionStatusOrderedByNameAsc, Direction.ASC);
    }

    private void assertThatListIsSortedByTargetName(final List<TargetWithActionStatus> targets,
            final Direction sortDirection) {
        String previousName = null;
        for (final TargetWithActionStatus targetWithActionStatus : targets) {
            final String actualName = targetWithActionStatus.getTarget().getName();
            if (previousName != null) {
                if (Direction.ASC == sortDirection) {
                    assertThat(actualName).isGreaterThan(previousName);
                } else {
                    assertThat(actualName).isLessThan(previousName);
                }
            }
            previousName = actualName;
        }
    }

    private Target reloadTarget(final Target targetCancelled) {
        return targetManagement.get(targetCancelled.getId()).orElseThrow();
    }

}

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;

class DeploymentManagementTest extends AbstractAccessControllerManagementTest {

    @Test
    void verifyAssignments() {
        runAs(withAuthorities(
                        READ_TARGET,
                        UPDATE_TARGET + "/type.id==" + targetType1.getId(), // has update permission for type1
                        READ_DISTRIBUTION_SET),
                () -> assertThat(deploymentManagement.assignDistributionSets(List.of(new DeploymentRequest(
                                target1Type1.getControllerId(), ds1Type1.getId(), Action.ActionType.FORCED, 0,
                                null, null, null, null, false)))
                        .get(0).getAssignedEntity().stream().map(Action::getTarget)
                        .map(Target::getId))
                        .hasSize(1)
                        .containsExactly(target1Type1.getId()));

        runAs(withAuthorities(
                        READ_TARGET,
                        UPDATE_TARGET + "/type.id==" + targetType2.getId(), // has no update permission for type1
                        READ_DISTRIBUTION_SET),
                () -> assertThat(deploymentManagement.assignDistributionSets(List.of(new DeploymentRequest(
                        target1Type1.getControllerId(), ds1Type1.getId(), Action.ActionType.FORCED, 0,
                        null, null, null, null, false)))).isEmpty());
    }

    @Test
    void verifyActionVisibility() {
        final String controllerId = target1Type1.getControllerId();
        verify(
                assignedId -> {
                    assertThat(deploymentManagement.findActionsAll(UNPAGED)).isEmpty();
                    assertThat(deploymentManagement.findAction(assignedId)).isEmpty();
                    assertThatThrownBy(() -> deploymentManagement.findActionWithDetails(assignedId))
                            .isInstanceOf(InsufficientPermissionException.class);
                    assertThat(deploymentManagement.findActions("id==*", UNPAGED)).isEmpty();
                    assertThatThrownBy(() -> deploymentManagement.findActionsByTarget(controllerId, UNPAGED))
                            .isInstanceOf(EntityNotFoundException.class);
                    assertThatThrownBy(() -> deploymentManagement.findActionsByTarget("id==*", controllerId, UNPAGED))
                            .isInstanceOf(EntityNotFoundException.class);
                    assertThatThrownBy(() -> deploymentManagement.findActionStatusByAction(assignedId, UNPAGED))
                            .isInstanceOf(EntityNotFoundException.class);
                    assertThatThrownBy(() -> deploymentManagement.findActiveActionsByTarget(controllerId, UNPAGED))
                            .isInstanceOf(EntityNotFoundException.class);
                    assertThatThrownBy(() -> deploymentManagement.findActiveActionsWithHighestWeight(controllerId, 99))
                            .isInstanceOf(EntityNotFoundException.class);
                    final Long targetId = target1Type1.getId();
                    assertThatThrownBy(() -> deploymentManagement.hasPendingCancellations(targetId)).isInstanceOf(
                            EntityNotFoundException.class);
                },
                assignedId -> {
                    assertThat(deploymentManagement.findActionsAll(UNPAGED)).hasSize(1).allMatch(this::isActionOfTarget1Type1);
                    assertThat(deploymentManagement.findAction(assignedId)).hasValueSatisfying(this::assertActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActionWithDetails(assignedId)).hasValueSatisfying(this::assertActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActions("id==*", UNPAGED)).hasSize(1).allMatch(this::isActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActionsByTarget(controllerId, UNPAGED)).hasSize(1)
                            .allMatch(this::isActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActionsByTarget("id==*", controllerId, UNPAGED))
                            .hasSize(1).allMatch(this::isActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActionStatusByAction(assignedId, UNPAGED))
                            .hasSize(1).allMatch(actionStatus -> actionStatus.getStatus().equals(Action.Status.RUNNING));
                    assertThat(deploymentManagement.findActiveActionsByTarget(controllerId, UNPAGED))
                            .hasSize(1).allMatch(this::isActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActiveActionsWithHighestWeight(controllerId, 99))
                            .hasSize(1).allMatch(this::isActionOfTarget1Type1);
                    assertThat(deploymentManagement.hasPendingCancellations(target1Type1.getId())).isFalse();
                },
                null);
    }

    @Test
    void verifyCancellation() {
        verify(
                assignedId -> assertThatThrownBy(() -> deploymentManagement.cancelAction(assignedId))
                        .isInstanceOf(EntityNotFoundException.class),
                assignedId -> assertThatThrownBy(() -> deploymentManagement.cancelAction(assignedId))
                        .isInstanceOf(InsufficientPermissionException.class),
                assignedId -> assertThat(deploymentManagement.cancelAction(assignedId).getId()).isEqualTo(assignedId));
    }

    @Test
    void verifyCancellationByDistributionSetId() {
        verify(
                assignedId -> {
                    deploymentManagement.cancelActionsForDistributionSet(ActionCancellationType.FORCE, ds1Type1);
                    assertThat(deploymentManagement.findAction(assignedId)).isEmpty();
                },
                assignedId -> assertThat(deploymentManagement.findAction(assignedId))
                        .hasValueSatisfying(action -> assertThat(action.getStatus()).isEqualTo(Action.Status.RUNNING)),
                null);
    }

    @Test
    void verifyForceActionIsNotAllowed() {
        verify(
                assignedId -> assertThatThrownBy(() -> deploymentManagement.forceTargetAction(assignedId))
                        .isInstanceOf(EntityNotFoundException.class),
                assignedId -> assertThatThrownBy(() -> deploymentManagement.forceTargetAction(assignedId))
                        .isInstanceOf(InsufficientPermissionException.class),
                assignedId -> assertThat(deploymentManagement.forceTargetAction(assignedId).getActionType())
                        .isEqualTo(Action.ActionType.FORCED));
    }

    private void verify(final Consumer<Long> noRead, final Consumer<Long> noUpdate, final Consumer<Long> readAndUpdate) {
        final Long assignedId = systemSecurityContext.runAsSystem(() -> {
            final List<Action> assignedEntity = assignDistributionSet(ds1Type1.getId(), target1Type1.getControllerId()).getAssignedEntity();
            assertThat(assignedEntity).hasSize(1).allMatch(action -> action.getTarget().getId().equals(target1Type1.getId()));
            return assignedEntity.get(0);
        }).getId();

        if (noRead != null) {
            // no read permission
            runAs(withAuthorities(READ_TARGET + "/type.id==" + targetType2.getId(), UPDATE_TARGET + "/type.id==" + targetType2.getId()),
                    () -> noRead.accept(assignedId));
        }

        if (noUpdate != null) {
            // read but no update permission
            runAs(withAuthorities(READ_TARGET + "/type.id==" + targetType1.getId(), UPDATE_TARGET + "/type.id==" + targetType2.getId()),
                    () -> noUpdate.accept(assignedId));
        }

        if (readAndUpdate != null) {
            // read and update permissions
            runAs(withAuthorities(READ_TARGET + "/type.id==" + targetType1.getId(), UPDATE_TARGET + "/type.id==" + targetType1.getId()),
                    () -> readAndUpdate.accept(assignedId));
        }
    }

    private void assertActionOfTarget1Type1(final Action action) {
        assertThat(action.getTarget().getId()).isEqualTo(target1Type1.getId());
    }

    private boolean isActionOfTarget1Type1(final Action action) {
        return action.getTarget().getId().equals(target1Type1.getId());
    }
}
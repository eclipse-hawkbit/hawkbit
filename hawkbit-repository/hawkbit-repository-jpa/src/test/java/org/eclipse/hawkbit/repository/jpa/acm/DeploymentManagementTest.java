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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.hawkbit.auth.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.hawkbit.context.SystemSecurityContext;
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
                actionId -> {
                    assertThat(deploymentManagement.findActionsAll(UNPAGED)).isEmpty();
                    assertThat(deploymentManagement.findAction(actionId)).isEmpty();
                    assertThatThrownBy(() -> deploymentManagement.findActionWithDetails(actionId))
                            .isInstanceOf(InsufficientPermissionException.class);
                    assertThat(deploymentManagement.findActions("id==*", UNPAGED)).isEmpty();
                    assertThatThrownBy(() -> deploymentManagement.findActionsByTarget(controllerId, UNPAGED))
                            .isInstanceOf(EntityNotFoundException.class);
                    assertThatThrownBy(() -> deploymentManagement.findActionsByTarget("id==*", controllerId, UNPAGED))
                            .isInstanceOf(EntityNotFoundException.class);
                    assertThatThrownBy(() -> deploymentManagement.findActionStatusByAction(actionId, UNPAGED))
                            .isInstanceOf(EntityNotFoundException.class);
                    assertThatThrownBy(() -> deploymentManagement.findActiveActionsByTarget(controllerId, UNPAGED))
                            .isInstanceOf(EntityNotFoundException.class);
                    assertThatThrownBy(() -> deploymentManagement.findActiveActionsWithHighestWeight(controllerId, 99))
                            .isInstanceOf(EntityNotFoundException.class);
                    final Long targetId = target1Type1.getId();
                    assertThatThrownBy(() -> deploymentManagement.hasPendingCancellations(targetId)).isInstanceOf(
                            EntityNotFoundException.class);
                },
                actionId -> {
                    assertThat(deploymentManagement.findActionsAll(UNPAGED)).hasSize(1).allMatch(this::isActionOfTarget1Type1);
                    assertThat(deploymentManagement.findAction(actionId)).hasValueSatisfying(this::assertActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActionWithDetails(actionId)).hasValueSatisfying(this::assertActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActions("id==*", UNPAGED)).hasSize(1).allMatch(this::isActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActionsByTarget(controllerId, UNPAGED)).hasSize(1)
                            .allMatch(this::isActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActionsByTarget("id==*", controllerId, UNPAGED))
                            .hasSize(1).allMatch(this::isActionOfTarget1Type1);
                    assertThat(deploymentManagement.findActionStatusByAction(actionId, UNPAGED))
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
    void verifyDeleteActionById() {
        verify(
                null,
                actionId -> assertThatExceptionOfType(InsufficientPermissionException.class)
                        .isThrownBy(() -> deploymentManagement.deleteAction(actionId)),
                actionId -> assertThatNoException().isThrownBy(() -> deploymentManagement.deleteAction(actionId)));
    }

    @Test
    void verifyDeleteActionsById() {
        verify(
                null,
                actionId -> {
                    final List<Long> actionIds = List.of(actionId);
                    assertThatExceptionOfType(InsufficientPermissionException.class)
                            .isThrownBy(() -> deploymentManagement.deleteActionsByIds(actionIds));
                },
                actionId -> assertThatNoException().isThrownBy(() -> deploymentManagement.deleteActionsByIds(List.of(actionId))));
    }

    @Test
    void verifyDeleteActionByRSQL() {
        verify(
                null,
                actionId -> {
                    assertThatNoException().isThrownBy(() -> deploymentManagement.deleteActionsByRsql("id==" + actionId));
                    // no exception but the action shall not be deleted
                    assertThat(deploymentManagement.findAction(actionId)).isPresent();
                },
                actionId -> {
                    assertThatNoException().isThrownBy(() -> deploymentManagement.deleteActionsByRsql("id==" + actionId));
                    // the action shall be deleted
                    assertThat(deploymentManagement.findAction(actionId)).isEmpty();
                });
    }

    @Test
    void verifyDeleteTargetActionsById() {
        verify(
                null,
                actionId -> {
                    final String controllerId = deploymentManagement.findAction(actionId)
                            .map(Action::getTarget).map(Target::getControllerId).orElseThrow();
                    final List<Long> actionIds = List.of(actionId, -1L);
                    assertThatNoException().isThrownBy(() -> deploymentManagement.deleteTargetActionsByIds(controllerId, actionIds));
                    // no exception but the action shall not be deleted
                    assertThat(deploymentManagement.findAction(actionId)).isPresent();
                },
                actionId -> {
                    final String controllerId = deploymentManagement.findAction(actionId)
                            .map(Action::getTarget).map(Target::getControllerId).orElseThrow();
                    assertThatNoException().isThrownBy(
                            () -> deploymentManagement.deleteTargetActionsByIds(controllerId, List.of(actionId, -1L)));
                    // the action shall be deleted
                    assertThat(deploymentManagement.findAction(actionId)).isEmpty();
                });
    }

    @Test
    void verifyCancellation() {
        verify(
                actionId -> assertThatThrownBy(() -> deploymentManagement.cancelAction(actionId))
                        .isInstanceOf(EntityNotFoundException.class),
                actionId -> assertThatThrownBy(() -> deploymentManagement.cancelAction(actionId))
                        .isInstanceOf(InsufficientPermissionException.class),
                actionId -> assertThat(deploymentManagement.cancelAction(actionId).getId()).isEqualTo(actionId));
    }

    @Test
    void verifyCancellationByDistributionSetId() {
        verify(
                actionId -> {
                    deploymentManagement.cancelActionsForDistributionSet(ActionCancellationType.FORCE, ds1Type1);
                    assertThat(deploymentManagement.findAction(actionId)).isEmpty();
                },
                actionId -> assertThat(deploymentManagement.findAction(actionId))
                        .hasValueSatisfying(action -> assertThat(action.getStatus()).isEqualTo(Action.Status.RUNNING)),
                null);
    }

    @Test
    void verifyForceActionIsNotAllowed() {
        verify(
                actionId -> assertThatThrownBy(() -> deploymentManagement.forceTargetAction(actionId))
                        .isInstanceOf(EntityNotFoundException.class),
                actionId -> assertThatThrownBy(() -> deploymentManagement.forceTargetAction(actionId))
                        .isInstanceOf(InsufficientPermissionException.class),
                actionId -> assertThat(deploymentManagement.forceTargetAction(actionId).getActionType())
                        .isEqualTo(Action.ActionType.FORCED));
    }

    private void verify(final Consumer<Long> noRead, final Consumer<Long> readNoUpdate, final Consumer<Long> readAndUpdate) {
        final Long actionId = SystemSecurityContext.runAsSystem(() -> {
            final List<Action> actions = assignDistributionSet(ds1Type1.getId(), target1Type1.getControllerId()).getAssignedEntity();
            assertThat(actions).hasSize(1).allMatch(action -> action.getTarget().getId().equals(target1Type1.getId()));
            return actions.get(0);
        }).getId();

        if (noRead != null) {
            // no read permission
            runAs(withAuthorities(READ_TARGET + "/type.id==" + targetType2.getId(), UPDATE_TARGET + "/type.id==" + targetType2.getId()),
                    () -> noRead.accept(actionId));
        }

        if (readNoUpdate != null) {
            // read but no update permission
            runAs(withAuthorities(READ_TARGET + "/type.id==" + targetType1.getId(), UPDATE_TARGET + "/type.id==" + targetType2.getId()),
                    () -> readNoUpdate.accept(actionId));
        }

        if (readAndUpdate != null) {
            // read and update permissions
            runAs(withAuthorities(READ_TARGET + "/type.id==" + targetType1.getId(), UPDATE_TARGET + "/type.id==" + targetType1.getId()),
                    () -> readAndUpdate.accept(actionId));
        }
    }

    private void assertActionOfTarget1Type1(final Action action) {
        assertThat(action.getTarget().getId()).isEqualTo(target1Type1.getId());
    }

    private boolean isActionOfTarget1Type1(final Action action) {
        return action.getTarget().getId().equals(target1Type1.getId());
    }
}
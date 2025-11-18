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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.hawkbit.im.authentication.SpPermission.CREATE_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.CREATE_TARGET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.DELETE_TARGET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.HANDLE_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetManagement.Update;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.junit.jupiter.api.Test;

class TargetManagementTest extends AbstractAccessControllerManagementTest {

    @Test
    void verifyCreation() {
        // permissions to read all and create only type1 targets
        runAs(withAuthorities(READ_TARGET, CREATE_TARGET + "/type.id==" + targetType1.getId()), () -> {
            testdataFactory.createTarget("controller_1_create", "Controller-1_create", targetType1);
            assertThatThrownBy(() -> testdataFactory.createTarget("controller_2_create", "Controller-2_create", targetType2))
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked", "java:S5961" }) // better to keep together
    @Test
    void verifyRead() {
        assertThat(assignDistributionSet(ds2Type2, List.of(target1Type1, target2Type2, target3Type2)).getAssigned()).isEqualTo(3);
        final Target target1Type1 = targetManagement.get(super.target1Type1.getId());
        final Target target2Type2 = targetManagement.get(super.target2Type2.getId());
        runAs(withAuthorities(READ_TARGET + "/type.id==" + targetType1.getId(), READ_DISTRIBUTION_SET), () -> {
            Assertions.<Target> assertThat(targetManagement.findAll(UNPAGED)).containsExactly(target1Type1);
            assertThat(targetManagement.count()).isEqualTo(1);

            Assertions.<Target> assertThat(targetManagement.get(List.of(target1Type1.getId()))).containsExactly(target1Type1);
            final List<Long> allTargetIds = List.of(target1Type1.getId(), target2Type2.getId());
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> targetManagement.get(allTargetIds));

            assertThat(((TargetManagement) targetManagement).find(target1Type1.getId())).hasValue(target1Type1);
            assertThat(targetManagement.find(target2Type2.getId())).isEmpty();

            assertThat(targetManagement.findByControllerId(List.of(target1Type1.getControllerId()))).containsExactly(target1Type1);

            assertThat(targetManagement.getByControllerId(target1Type1.getControllerId())).isEqualTo(target1Type1);
            assertThat(targetManagement.findByControllerId(target1Type1.getControllerId())).hasValue(target1Type1);
            final String target2Type2ControllerId = target2Type2.getControllerId();
            assertThatThrownBy(() -> targetManagement.getByControllerId(target2Type2ControllerId))
                    .isInstanceOf(InsufficientPermissionException.class);
            assertThatThrownBy(() -> targetManagement.findByControllerId(target2Type2ControllerId))
                    .isInstanceOf(InsufficientPermissionException.class);

            assertThat(targetManagement.getControllerAttributes(target1Type1.getControllerId())).isEmpty();
            assertThatThrownBy(() -> targetManagement.getControllerAttributes(target2Type2ControllerId))
                    .isInstanceOf(InsufficientPermissionException.class);

            assertThat(targetManagement.getByControllerId(target1Type1.getControllerId())).isEqualTo(target1Type1);
            assertThat(targetManagement.findByControllerId(target1Type1.getControllerId())).hasValue(target1Type1);
            assertThatThrownBy(() -> targetManagement.getByControllerId(target2Type2ControllerId))
                    .isInstanceOf(InsufficientPermissionException.class);
            assertThatThrownBy(() -> targetManagement.findByControllerId(target2Type2ControllerId))
                    .isInstanceOf(InsufficientPermissionException.class);

            assertThat(targetManagement.countByRsql("id==*")).isEqualTo(1);
            assertThat(targetManagement.countByRsql("id==%s".formatted(target1Type1.getControllerId()))).isEqualTo(1);
            assertThat(targetManagement.countByRsql("id==%s".formatted(target2Type2.getControllerId()))).isZero();
            Assertions.<Target> assertThat(targetManagement.findByRsql("id==*", UNPAGED))
                    .hasSize(1)
                    .containsExactly(target1Type1)
                    .doesNotContain(target2Type2);
            Assertions.<Target> assertThat(
                            targetManagement.findByRsql("id==%s".formatted(target1Type1.getControllerId()), UNPAGED))
                    .hasSize(1)
                    .containsExactly(target1Type1)
                    .doesNotContain(target2Type2);
            assertThat(targetManagement.findByRsql("id==%s".formatted(target2Type2.getControllerId()), UNPAGED)).isEmpty();

            assertThat(targetManagement.findByAssignedDistributionSet(ds2Type2.getId(), UNPAGED))
                    .extracting(Identifiable::getId).containsExactly(target1Type1.getId());
            assertThat(targetManagement.findByAssignedDistributionSetAndRsql(ds2Type2.getId(), "id==*", UNPAGED))
                    .extracting(Identifiable::getId).containsExactly(target1Type1.getId());

            assertThat(targetManagement.countByRsqlAndCompatible("id==*", ds2Type2.getType().getId())).isEqualTo(1);

            assertThat(targetManagement.getMetadata(target1Type1.getControllerId())).isEmpty();
            final String target2Type2ControllerId1 = target2Type2.getControllerId();
            assertThatThrownBy(() -> targetManagement.getMetadata(target2Type2ControllerId1))
                    .isInstanceOf(InsufficientPermissionException.class);

            // find by tags
            assertThat(targetManagement.findByTag(targetTag2.getId(), UNPAGED))
                    .extracting(Identifiable::getId)
                    .containsOnly(target1Type1.getId());
            assertThat(targetManagement.findByRsqlAndTag("id==*", targetTag2.getId(), UNPAGED))
                    .extracting(Identifiable::getId)
                    .containsOnly(target1Type1.getId());
        });
    }

    @Test
    void verifyReadCompatibleRelated() {
        prepareFinishedUpdates(ds2Type2, target1Type1, target2Type2);
        final Target target1Type1 = targetManagement.get(super.target1Type1.getId());
        runAs(withAuthorities(
                READ_TARGET + "/type.id==" + targetType2.getId(), // we want to have 2 updatable targets
                READ_DISTRIBUTION_SET), () -> {
            assertThat(targetManagement.findByInstalledDistributionSet(ds2Type2.getId(), UNPAGED))
                    .extracting(Identifiable::getId).containsExactly(target2Type2.getId());
            assertThat(targetManagement.findByInstalledDistributionSetAndRsql(ds2Type2.getId(), "id==*", UNPAGED))
                    .extracting(Identifiable::getId).containsExactly(target2Type2.getId());
        });
        runAs(withAuthorities(
                READ_TARGET, UPDATE_TARGET + "/type.id==" + targetType2.getId(), // we want to have 2 updatable targets
                READ_DISTRIBUTION_SET), () -> {
            assertThat(targetManagement.findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(ds2Type2.getId(), "id==*", UNPAGED))
                    .extracting(Identifiable::getId).containsExactly(target3Type2.getId());

            assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
                    target1Type1.getControllerId(), ds2Type2.getId(), "id==*")).isFalse();
            assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
                    target2Type2.getControllerId(), ds2Type2.getId(), "id==*")).isFalse();
            assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
                    target3Type2.getControllerId(), ds2Type2.getId(), "id==*")).isTrue();
        });
    }

    @Test
    void verifyReadRolloutRelated() {
        final Target target1Type1 = targetManagement.get(super.target1Type1.getId());
        runAs(withAuthorities(
                READ_TARGET, UPDATE_TARGET + "/type.id==" + targetType1.getId(),
                READ_DISTRIBUTION_SET,
                CREATE_ROLLOUT, READ_ROLLOUT, HANDLE_ROLLOUT), () -> {

            final Rollout rollout = testdataFactory.createRolloutByVariables("testRollout", "testDescription", 3, "id==*", ds2Type2, "50", "5");
            final List<Long> groups = rolloutGroupManagement.findByRollout(rollout.getId(), UNPAGED).getContent().stream().
                    map(Identifiable::getId).toList();
            assertThat(groups.stream().flatMap(
                    group -> rolloutGroupManagement.findTargetsOfRolloutGroup(group, UNPAGED).get()).toList())
                    .containsExactly(target1Type1);

            // as system in context - doesn't apply scopes
            final Rollout runAsSystem = systemSecurityContext.runAsSystem(
                    () -> testdataFactory.createRolloutByVariables(
                            "testRolloutAsSystem", "testDescriptionAsSystem", 3, "id==*", ds2Type2, "50", "5"));
        });
    }

    @Test
    void verifyUpdate() {
        final TargetTag testTag = testdataFactory.createTargetTags(1, "testTag").get(0);
        final Long testTagId = testTag.getId();
        runAs(withAuthorities(
                        READ_TARGET, UPDATE_TARGET + "/type.id==" + targetType1.getId(),
                        READ_DISTRIBUTION_SET),
                () -> {
                    assertThat(targetManagement.update(
                            Update.builder().id(target1Type1.getId()).name("myNewName").build()))
                            .extracting(NamedEntity::getName).isEqualTo("myNewName");

                    final Update targetUpdate = Update.builder().id(target2Type2.getId()).name("myNewName").build();
                    assertThatThrownBy(() -> targetManagement.update(targetUpdate)).isInstanceOf(InsufficientPermissionException.class);

                    final String metadataKey = "key.dot";
                    final Map<String, String> metadata = Map.of(metadataKey, "value.dot");
                    final String target1Type1ControllerId = target1Type1.getControllerId();
                    targetManagement.createMetadata(target1Type1ControllerId, metadata);
                    final String target2Type2ControllerId = target2Type2.getControllerId();
                    assertThatThrownBy(() -> targetManagement.createMetadata(target2Type2ControllerId, metadata))
                            .isInstanceOf(InsufficientPermissionException.class);
                    final String newValue = "newValue";
                    targetManagement.createMetadata(target1Type1ControllerId, metadataKey, newValue);
                    assertThat(targetManagement.getMetadata(target1Type1ControllerId)).containsEntry(metadataKey, newValue);
                    assertThatThrownBy(() -> targetManagement.createMetadata(target2Type2ControllerId, metadataKey, newValue))
                            .isInstanceOf(InsufficientPermissionException.class);
                    targetManagement.deleteMetadata(target1Type1ControllerId, metadataKey);
                    assertThatThrownBy(() -> targetManagement.deleteMetadata(target2Type2ControllerId, metadataKey))
                            .isInstanceOf(EntityNotFoundException.class);

                    // tag assignments
                    assertThat(targetManagement.assignTag(List.of(target1Type1.getControllerId()), testTagId))
                            .extracting(Identifiable::getId).containsExactly(target1Type1.getId());
                    final List<String> target2Type2ControllerIdList = List.of(target2Type2.getControllerId());
                    assertThatThrownBy(() -> targetManagement.assignTag(target2Type2ControllerIdList, testTagId))
                            .isInstanceOf(InsufficientPermissionException.class);
                    assertThatThrownBy(() -> targetManagement.assignTag(target2Type2ControllerIdList, testTagId))
                            .isInstanceOf(InsufficientPermissionException.class);

                    assertThat(targetManagement.unassignTag(List.of(target1Type1.getControllerId()), testTagId)).element(0)
                            .extracting(Identifiable::getId).isEqualTo(target1Type1.getId());
                    assertThatThrownBy(() -> targetManagement.unassignTag(target2Type2ControllerIdList, testTagId))
                            .isInstanceOf(InsufficientPermissionException.class);
                    assertThatThrownBy(() -> targetManagement.unassignTag(target2Type2ControllerIdList, testTagId))
                            .isInstanceOf(InsufficientPermissionException.class);

                    // type
                    final Long targetType2Id = targetType2.getId();
                    assertThatThrownBy(() -> targetManagement.assignType(target2Type2ControllerId, targetType2Id))
                            .isInstanceOf(InsufficientPermissionException.class);
                    assertThatThrownBy(() -> targetManagement.unassignType(target2Type2ControllerId))
                            .isInstanceOf(InsufficientPermissionException.class);
                    final String noPermissionsTestDataControllerId = target2Type2.getControllerId();
                    assertThatThrownBy(() -> targetManagement.unassignType(noPermissionsTestDataControllerId))
                            .isInstanceOf(InsufficientPermissionException.class);

                    final Long ds2Type2Id = ds2Type2.getId();
                    assertThat(assignDistributionSet(ds2Type2Id, target1Type1.getControllerId()).getAssigned()).isEqualTo(1);
                    assertThatThrownBy(() -> assignDistributionSet(ds2Type2Id, target2Type2ControllerId)).isInstanceOf(AssertionError.class);
                    // bunch assignment skips denied since at least one target without update permissions is present
                    assertThat(assignDistributionSet(
                            ds3Type2.getId(), List.of(target1Type1.getControllerId(), target2Type2ControllerId), Action.ActionType.FORCED)
                            .getAssigned()).isEqualTo(1);

                    assertThat(targetManagement.findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(ds1Type1.getId(), "id==*", UNPAGED))
                            .extracting(Identifiable::getId).containsExactly(target1Type1.getId());
                    assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
                            target1Type1.getControllerId(), ds1Type1.getId(), "id==*")).isTrue();
                    assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
                            target2Type2.getControllerId(), ds1Type1.getId(), "id==*")).isFalse();
                });
    }

    @Test
    void verifyDeletion() {
        runAs(withAuthorities(READ_TARGET, DELETE_TARGET + "/type.id==" + targetType1.getId()), () -> {
            final List<Long> allTargetIds = List.of(target1Type1.getId(), target2Type2.getId());
            assertThatThrownBy(() -> targetManagement.delete(allTargetIds)).isInstanceOf(InsufficientPermissionException.class);
            final List<Long> target2Type2IdList = List.of(target2Type2.getId());
            assertThatThrownBy(() -> targetManagement.delete(target2Type2IdList)).isInstanceOf(InsufficientPermissionException.class);

            targetManagement.delete(List.of(target1Type1.getId()));
        });
    }

    private void prepareFinishedUpdates(final DistributionSet ds, final Target... targets) {
        final List<String> controllerIds = Arrays.stream(targets).map(Target::getControllerId).toList();
        getFirstAssignedTarget(assignDistributionSet(ds.getId(), controllerIds, Action.ActionType.FORCED));

        controllerIds.stream().map(controllerId -> deploymentManagement.findActiveActionsByTarget(controllerId, PAGE).getContent().get(0))
                .forEach(action -> {
                    if (action.getStatus() == Action.Status.WAIT_FOR_CONFIRMATION) {
                        confirmationManagement.confirmAction(action.getId(), null, null);
                    }

                    action = controllerManagement.addUpdateActionStatus(
                            Action.ActionStatusCreate.builder().actionId(action.getId()).status(Action.Status.RUNNING).build());

                    controllerManagement.addUpdateActionStatus(
                            Action.ActionStatusCreate.builder().actionId(action.getId()).status(Action.Status.FINISHED).build());
                });
    }
}
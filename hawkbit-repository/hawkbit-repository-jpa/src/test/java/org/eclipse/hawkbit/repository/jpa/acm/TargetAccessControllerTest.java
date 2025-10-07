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
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.TargetManagement.Create;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.autoassign.AutoAssignChecker;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

/**
 * Feature: Component Tests - Access Control<br/>
 * Story: Test Target Access Controller
 */
@TestPropertySource(properties = "hawkbit.acm.access-controller.enabled=true")
class TargetAccessControllerTest extends AbstractJpaIntegrationTest {

    @Autowired
    AutoAssignChecker autoAssignChecker;

    /**
     * Verifies read access rules for targets
     */
    @Test
    void verifyTargetReadOperations() {
        final Target permittedTarget = targetManagement
                .create(Create.builder().controllerId("device01").updateStatus(TargetUpdateStatus.REGISTERED).build());

        final Target hiddenTarget = targetManagement
                .create(Create.builder().controllerId("device02").updateStatus(TargetUpdateStatus.REGISTERED).build());

        runAs(withUser("user", READ_TARGET + "/controllerId==" + permittedTarget.getControllerId()), () -> {
            // verify targetManagement#findAll
            assertThat(targetManagement.findAll(Pageable.unpaged()).get().map(Identifiable::getId).toList())
                    .containsOnly(permittedTarget.getId());

            // verify targetManagement#findByRsql
            assertThat(targetManagement.findByRsql("id==*", Pageable.unpaged()).get().map(Identifiable::getId).toList())
                    .containsOnly(permittedTarget.getId());

            // verify targetManagement#getByControllerID
            assertThat(targetManagement.getByControllerId(permittedTarget.getControllerId())).isNotNull();
            final String hiddenTargetControllerId = hiddenTarget.getControllerId();
            assertThatThrownBy(() -> targetManagement.getByControllerId(hiddenTargetControllerId))
                    .as("Missing read permissions for hidden target.")
                    .isInstanceOf(InsufficientPermissionException.class);
            assertThatThrownBy(() -> targetManagement.findByControllerId(hiddenTargetControllerId))
                    .as("Missing read permissions for hidden target.")
                    .isInstanceOf(InsufficientPermissionException.class);

            // verify targetManagement#getByControllerId
            assertThat(targetManagement
                    .findByControllerId(List.of(permittedTarget.getControllerId(), hiddenTargetControllerId))
                    .stream().map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());

            // verify targetManagement#get
            assertThat(targetManagement.find(permittedTarget.getId())).isPresent();
            assertThat(targetManagement.find(hiddenTarget.getId())).isEmpty();

            // verify targetManagement#get
            final List<Long> withHidden = List.of(permittedTarget.getId(), hiddenTarget.getId());
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> targetManagement.get(withHidden));

            // verify targetManagement#getControllerAttributes
            assertThat(targetManagement.getControllerAttributes(permittedTarget.getControllerId())).isEmpty();
            assertThatThrownBy(() -> targetManagement.getControllerAttributes(hiddenTargetControllerId))
                    .as("Target should not be found.")
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }

    @Test
    void verifyTagFilteringAndManagement() {
        final Target permittedTarget = targetManagement
                .create(Create.builder().controllerId("device01").updateStatus(TargetUpdateStatus.REGISTERED).build());

        final Target readOnlyTarget = targetManagement
                .create(Create.builder().controllerId("device02").updateStatus(TargetUpdateStatus.REGISTERED).build());
        final String readOnlyTargetControllerId = readOnlyTarget.getControllerId();

        final Target hiddenTarget = targetManagement
                .create(Create.builder().controllerId("device03").updateStatus(TargetUpdateStatus.REGISTERED).build());

        final Long myTagId = targetTagManagement.create(TargetTagManagement.Create.builder().name("myTag").build()).getId();

        // perform tag assignment before setting access rules
        targetManagement.assignTag(
                List.of(permittedTarget.getControllerId(), readOnlyTargetControllerId, hiddenTarget.getControllerId()), myTagId);

        runAs(withUser("user",
                        READ_TARGET + "/controllerId==" + permittedTarget.getControllerId() + " or controllerId==" + readOnlyTargetControllerId,
                        UPDATE_TARGET + "/controllerId==" + permittedTarget.getControllerId()),
                () -> {
                    // verify targetManagement#findByTag
                    assertThat(
                            targetManagement.findByTag(myTagId, Pageable.unpaged()).get().map(Identifiable::getId).toList())
                            .containsOnly(permittedTarget.getId(), readOnlyTarget.getId());

                    // verify targetManagement#findByRsqlAndTag
                    assertThat(targetManagement.findByRsqlAndTag("id==*", myTagId, Pageable.unpaged()).get()
                            .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId(), readOnlyTarget.getId());

                    // verify targetManagement#assignTag on permitted target
                    assertThat(targetManagement.assignTag(Collections.singletonList(permittedTarget.getControllerId()), myTagId)).hasSize(1);
                    // verify targetManagement#unassignTag on permitted target
                    assertThat(targetManagement.unassignTag(Collections.singletonList(permittedTarget.getControllerId()), myTagId)).hasSize(1);
                    // verify targetManagement#assignTag on permitted target
                    assertThat(targetManagement.assignTag(Collections.singletonList(permittedTarget.getControllerId()), myTagId))
                            .hasSize(1);
                    // verify targetManagement#unAssignTag on permitted target
                    assertThat(targetManagement.unassignTag(List.of(permittedTarget.getControllerId()), myTagId).get(0).getControllerId())
                            .isEqualTo(permittedTarget.getControllerId());

                    // assignment is denied for readOnlyTarget (read, but no update permissions)
                    final List<String> readTargetControllerIdList = Collections.singletonList(readOnlyTargetControllerId);
                    assertThatThrownBy(() -> targetManagement.assignTag(readTargetControllerIdList, myTagId))
                            .as("Missing update permissions for target to toggle tag assignment.")
                            .isInstanceOfAny(InsufficientPermissionException.class);

                    // assignment is denied for readOnlyTarget (read, but no update permissions)
                    final List<String> readOnlyTargetControllerIdList = List.of(readOnlyTargetControllerId);
                    assertThatThrownBy(() -> targetManagement.unassignTag(readOnlyTargetControllerIdList, myTagId))
                            .as("Missing update permissions for target to toggle tag assignment.")
                            .isInstanceOf(InsufficientPermissionException.class);

                    // assignment is denied for hiddenTarget since it's hidden
                    final List<String> hiddenTargetControllerIdList = Collections.singletonList(hiddenTarget.getControllerId());
                    assertThatThrownBy(() -> targetManagement.assignTag(hiddenTargetControllerIdList, myTagId))
                            .as("Missing update permissions for target to toggle tag assignment.")
                            .isInstanceOf(InsufficientPermissionException.class);

                    // assignment is denied for hiddenTarget since it's hidden
                    assertThatThrownBy(() -> targetManagement.assignTag(hiddenTargetControllerIdList, myTagId))
                            .as("Missing update permissions for target to toggle tag assignment.")
                            .isInstanceOf(InsufficientPermissionException.class);

                    // assignment is denied for hiddenTarget since it's hidden
                    assertThatThrownBy(() -> targetManagement.unassignTag(hiddenTargetControllerIdList, myTagId))
                            .as("Missing update permissions for target to toggle tag assignment.")
                            .isInstanceOf(InsufficientPermissionException.class);
                });
    }

    /**
     * Verifies rules for target assignment
     */
    @Test
    void verifyTargetAssignment() {
        final DistributionSet ds = testdataFactory.createDistributionSet("myDs");
        distributionSetManagement.lock(ds);

        final Target permittedTarget = targetManagement
                .create(Create.builder().controllerId("device01").updateStatus(TargetUpdateStatus.REGISTERED).build());
        final String hiddenTargetControllerId = targetManagement
                .create(Create.builder().controllerId("device02").updateStatus(TargetUpdateStatus.REGISTERED).build())
                .getControllerId();

        runAs(withUser("user",
                READ_DISTRIBUTION_SET,
                READ_TARGET + "/controllerId==" + permittedTarget.getControllerId(),
                UPDATE_TARGET + "/controllerId==" + permittedTarget.getControllerId()), () -> {
            final Long dsId = ds.getId();
            assertThat(assignDistributionSet(dsId, permittedTarget.getControllerId()).getAssigned()).isEqualTo(1);
            // assigning of not allowed target behaves as not found
            assertThatThrownBy(() -> assignDistributionSet(dsId, hiddenTargetControllerId)).isInstanceOf(AssertionError.class);
        });
    }

    /**
     * Verifies rules for target assignment
     */
    @Test
    void verifyTargetAssignmentOnNonUpdatableTarget() {
        final DistributionSet firstDs = testdataFactory.createDistributionSet("myDs");
        distributionSetManagement.lock(firstDs);
        final DistributionSet secondDs = testdataFactory.createDistributionSet("anotherDs");
        distributionSetManagement.lock(secondDs);

        final Target manageableTarget = targetManagement
                .create(Create.builder().controllerId("device01").updateStatus(TargetUpdateStatus.REGISTERED).build());
        final Target readOnlyTarget = targetManagement
                .create(Create.builder().controllerId("device02").updateStatus(TargetUpdateStatus.REGISTERED).build());

        runAs(withUser("user",
                READ_DISTRIBUTION_SET,
                READ_TARGET + "/controllerId==" + manageableTarget.getControllerId() + " or controllerId==" + readOnlyTarget.getControllerId(),
                UPDATE_TARGET + "/controllerId==" + manageableTarget.getControllerId()), () -> {
            final Long firstDsId = firstDs.getId();
            // assignment is permitted for manageableTarget
            assertThat(assignDistributionSet(firstDsId, manageableTarget.getControllerId()).getAssigned()).isEqualTo(1);

            // assignment is denied for readOnlyTarget (read, but no update permissions)
            final var readOnlyTargetControllerId = readOnlyTarget.getControllerId();
            assertThatThrownBy(() -> assignDistributionSet(firstDsId, readOnlyTargetControllerId)).isInstanceOf(AssertionError.class);

            // bunch assignment skips denied since at least one target without update permissions is present
            assertThat(assignDistributionSet(secondDs.getId(),
                    Arrays.asList(readOnlyTargetControllerId, manageableTarget.getControllerId()),
                    ActionType.FORCED).getAssigned()).isEqualTo(1);
        });
    }

    /**
     * Verifies only manageable targets are part of the rollout
     */
    @Test
    void verifyRolloutTargetScope() {
        final DistributionSet ds = testdataFactory.createDistributionSet("myDs");
        distributionSetManagement.lock(ds);

        final String[] updateTargetControllerIds = { "update1", "update2", "update3" };
        final List<Target> updateTargets = testdataFactory.createTargets(updateTargetControllerIds);
        final String[] readTargetControllerIds = { "read1", "read2", "read3", "read4" };
        final List<Target> readTargets = testdataFactory.createTargets(readTargetControllerIds);
        final List<Target> hiddenTargets = testdataFactory.createTargets("hidden1", "hidden2", "hidden3", "hidden4", "hidden5");

        runAs(withUser("user",
                READ_DISTRIBUTION_SET,
                READ_TARGET + "/controllerId=in=(" + String.join(", ", List.of(updateTargetControllerIds)) + ")" +
                        " or controllerId=in=(" + String.join(", ", List.of(readTargetControllerIds)) + ")",
                UPDATE_TARGET + "/controllerId=in=(" + String.join(", ", List.of(updateTargetControllerIds)) + ")",
                CREATE_ROLLOUT, READ_ROLLOUT), () -> {
            final Rollout rollout = testdataFactory.createRolloutByVariables(
                    "testRollout", "description", updateTargets.size(), "id==*", ds, "50", "5");
            assertThat(rollout.getTotalTargets()).isEqualTo(updateTargets.size());

            final List<RolloutGroup> content = rolloutGroupManagement.findByRollout(rollout.getId(), Pageable.unpaged()).getContent();
            assertThat(content).hasSize(updateTargets.size());

            final List<Target> rolloutTargets = content.stream().flatMap(
                            group -> rolloutGroupManagement.findTargetsOfRolloutGroup(group.getId(), Pageable.unpaged()).get())
                    .toList();

            assertThat(rolloutTargets).hasSize(updateTargets.size()).allMatch(
                            target -> updateTargets.stream().anyMatch(readTarget -> readTarget.getId().equals(target.getId())))
                    .noneMatch(target -> readTargets.stream()
                            .anyMatch(readTarget -> readTarget.getId().equals(target.getId())))
                    .noneMatch(target -> hiddenTargets.stream()
                            .anyMatch(readTarget -> readTarget.getId().equals(target.getId())));
        });
    }

    /**
     * Verifies only manageable targets are part of an auto assignment.
     */
    @Test
    void verifyAutoAssignmentTargetScope() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        distributionSetManagement.lock(distributionSet);

        final String[] updateTargetControllerIds = { "update1", "update2", "update3" };
        final List<Target> updateTargets = testdataFactory.createTargets(updateTargetControllerIds);
        final String[] readTargetControllerIds = { "read1", "read2", "read3", "read4" };
        final List<Target> readTargets = testdataFactory.createTargets(readTargetControllerIds);
        final List<Target> hiddenTargets = testdataFactory.createTargets("hidden1", "hidden2", "hidden3", "hidden4", "hidden5");

        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(TargetFilterQueryManagement.Create.builder().name("testName").query("id==*").build());

        runAs(withUser("user",
                READ_TARGET + "/controllerId=in=(" + String.join(", ", List.of(updateTargetControllerIds)) + ")" +
                        " or controllerId=in=(" + String.join(", ", List.of(readTargetControllerIds)) + ")",
                UPDATE_TARGET + "/controllerId=in=(" + String.join(", ", List.of(updateTargetControllerIds)) + ")",
                READ_DISTRIBUTION_SET + "/id==" + distributionSet.getId()), () -> {

            targetFilterQueryManagement.updateAutoAssignDS(
                    new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(distributionSet.getId()));

            autoAssignChecker.checkAllTargets();

            assertThat(targetManagement.findByAssignedDistributionSet(distributionSet.getId(), Pageable.unpaged())
                    .getContent())
                    .hasSize(updateTargets.size())
                    .allMatch(assignedTarget -> updateTargets.stream()
                            .anyMatch(updateTarget -> updateTarget.getId().equals(assignedTarget.getId())))
                    .noneMatch(assignedTarget -> readTargets.stream()
                            .anyMatch(updateTarget -> updateTarget.getId().equals(assignedTarget.getId())))
                    .noneMatch(assignedTarget -> hiddenTargets.stream()
                            .anyMatch(updateTarget -> updateTarget.getId().equals(assignedTarget.getId())));
        });
    }
}
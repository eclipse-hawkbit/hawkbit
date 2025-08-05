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
import static org.eclipse.hawkbit.im.authentication.SpPermission.CREATE_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_REPOSITORY;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.TargetTagManagement;
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
import org.springframework.test.context.ContextConfiguration;

/**
 * Feature: Component Tests - Access Control<br/>
 * Story: Test Target Access Controller
 */
@ContextConfiguration(classes = { DefaultAccessControllerConfiguration.class })
class TargetAccessControllerTest extends AbstractJpaIntegrationTest {

    @Autowired
    AutoAssignChecker autoAssignChecker;

    /**
     * Verifies read access rules for targets
     */
    @Test
    void verifyTargetReadOperations() {
        final Target permittedTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device01").status(TargetUpdateStatus.REGISTERED));

        final Target hiddenTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device02").status(TargetUpdateStatus.REGISTERED));

        runAs(withUser("user", READ_TARGET + "/controllerId==" + permittedTarget.getControllerId()), () -> {
            // verify targetManagement#findAll
            assertThat(targetManagement.findAll(Pageable.unpaged()).get().map(Identifiable::getId).toList())
                    .containsOnly(permittedTarget.getId());

            // verify targetManagement#findByRsql
            assertThat(targetManagement.findByRsql("id==*", Pageable.unpaged()).get().map(Identifiable::getId).toList())
                    .containsOnly(permittedTarget.getId());

            // verify targetManagement#findByUpdateStatus
            assertThat(targetManagement.findByUpdateStatus(TargetUpdateStatus.REGISTERED, Pageable.unpaged()).get()
                    .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());

            // verify targetManagement#getByControllerID
            assertThat(targetManagement.getByControllerID(permittedTarget.getControllerId())).isPresent();
            final String hiddenTargetControllerId = hiddenTarget.getControllerId();
            assertThatThrownBy(() -> targetManagement.getByControllerID(hiddenTargetControllerId))
                    .as("Missing read permissions for hidden target.")
                    .isInstanceOf(InsufficientPermissionException.class);

            // verify targetManagement#getByControllerID
            assertThat(targetManagement
                    .getByControllerID(Arrays.asList(permittedTarget.getControllerId(), hiddenTargetControllerId))
                    .stream().map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());

            // verify targetManagement#get
            assertThat(targetManagement.get(permittedTarget.getId())).isPresent();
            assertThat(targetManagement.get(hiddenTarget.getId())).isEmpty();

            // verify targetManagement#get
            assertThat(targetManagement.get(Arrays.asList(permittedTarget.getId(), hiddenTarget.getId())).stream()
                    .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());

            // verify targetManagement#getControllerAttributes
            assertThat(targetManagement.getControllerAttributes(permittedTarget.getControllerId())).isEmpty();
            assertThatThrownBy(() -> targetManagement.getControllerAttributes(hiddenTargetControllerId))
                    .as("Target should not be found.")
                    .isInstanceOf(InsufficientPermissionException.class);
        });

        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(TargetFilterQueryManagement.Create.builder().name("test").query("id==*").build());

        runAs(withUser("user", READ_TARGET + "/controllerId==" + permittedTarget.getControllerId()), () -> {
            // verify targetManagement#findByTargetFilterQuery
            assertThat(targetManagement.findByTargetFilterQuery(targetFilterQuery.getId(), Pageable.unpaged()).get()
                    .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());

            // verify targetManagement#findByTargetFilterQuery (used by UI)
            assertThat(targetManagement.findByFilters(new FilterParams(null, null, null, null), Pageable.unpaged()).get()
                    .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());
        });
    }

    @Test
    void verifyTagFilteringAndManagement() {
        final Target permittedTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device01").status(TargetUpdateStatus.REGISTERED));

        final Target readOnlyTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device02").status(TargetUpdateStatus.REGISTERED));
        final String readOnlyTargetControllerId = readOnlyTarget.getControllerId();

        final Target hiddenTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device03").status(TargetUpdateStatus.REGISTERED));

        final Long myTagId = targetTagManagement.create(TargetTagManagement.Create.builder().name("myTag").build()).getId();

        // perform tag assignment before setting access rules
        targetManagement.assignTag(
                List.of(permittedTarget.getControllerId(), readOnlyTargetControllerId, hiddenTarget.getControllerId()), myTagId);

        runAs(withUser("user",
                        READ_TARGET + "/controllerId==" + permittedTarget.getControllerId() + " or controllerId==" + readOnlyTargetControllerId,
                        UPDATE_TARGET + "/controllerId==" + permittedTarget.getControllerId(),
                        READ_REPOSITORY),
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
        final Long dsId = testdataFactory.createDistributionSet("myDs").getId();
        distributionSetManagement.lock(dsId);

        final Target permittedTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device01").status(TargetUpdateStatus.REGISTERED));
        final String hiddenTargetControllerId = targetManagement
                .create(entityFactory.target().create().controllerId("device02").status(TargetUpdateStatus.REGISTERED))
                .getControllerId();

        runAs(withUser("user", READ_TARGET + "/controllerId==" + permittedTarget.getControllerId()), () ->
                // verify targetManagement#findByUpdateStatus before assignment
                assertThat(targetManagement.findByUpdateStatus(TargetUpdateStatus.REGISTERED, Pageable.unpaged()).get()
                        .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId()));

        runAs(withUser("user",
                READ_TARGET + "/controllerId==" + permittedTarget.getControllerId(),
                UPDATE_TARGET + "/controllerId==" + permittedTarget.getControllerId(),
                READ_REPOSITORY), () -> {
            assertThat(assignDistributionSet(dsId, permittedTarget.getControllerId()).getAssigned()).isEqualTo(1);
            // assigning of not allowed target behaves as not found
            assertThatThrownBy(() -> assignDistributionSet(dsId, hiddenTargetControllerId)).isInstanceOf(AssertionError.class);

            // verify targetManagement#findByUpdateStatus(REGISTERED) after assignment
            assertThat(targetManagement.findByUpdateStatus(TargetUpdateStatus.REGISTERED, Pageable.unpaged())
                    .getTotalElements()).isZero();

            // verify targetManagement#findByUpdateStatus(PENDING) after assignment
            assertThat(targetManagement.findByUpdateStatus(TargetUpdateStatus.PENDING, Pageable.unpaged()).get()
                    .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());
        });
    }

    /**
     * Verifies rules for target assignment
     */
    @Test
    void verifyTargetAssignmentOnNonUpdatableTarget() {
        final Long firstDsId = testdataFactory.createDistributionSet("myDs").getId();
        distributionSetManagement.lock(firstDsId);
        final DistributionSet secondDs = testdataFactory.createDistributionSet("anotherDs");
        distributionSetManagement.lock(secondDs.getId());

        final Target manageableTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device01").status(TargetUpdateStatus.REGISTERED));
        final Target readOnlyTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device02").status(TargetUpdateStatus.REGISTERED));

        runAs(withUser("user",
                READ_TARGET + "/controllerId==" + manageableTarget.getControllerId() + " or controllerId==" + readOnlyTarget.getControllerId(),
                UPDATE_TARGET + "/controllerId==" + manageableTarget.getControllerId(),
                READ_REPOSITORY), () -> {
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
        distributionSetManagement.lock(ds.getId());

        final String[] updateTargetControllerIds = { "update1", "update2", "update3" };
        final List<Target> updateTargets = testdataFactory.createTargets(updateTargetControllerIds);
        final String[] readTargetControllerIds = { "read1", "read2", "read3", "read4" };
        final List<Target> readTargets = testdataFactory.createTargets(readTargetControllerIds);
        final List<Target> hiddenTargets = testdataFactory.createTargets("hidden1", "hidden2", "hidden3", "hidden4", "hidden5");

        runAs(withUser("user",
                READ_TARGET + "/controllerId=in=(" + String.join(", ", List.of(updateTargetControllerIds)) + ")" +
                        " or controllerId=in=(" + String.join(", ", List.of(readTargetControllerIds)) + ")",
                UPDATE_TARGET + "/controllerId=in=(" + String.join(", ", List.of(updateTargetControllerIds)) + ")",
                READ_REPOSITORY,
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
        distributionSetManagement.lock(distributionSet.getId());

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
                READ_REPOSITORY + "/id==" + distributionSet.getId()), () -> {

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
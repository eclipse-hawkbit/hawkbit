/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.autoassign.AutoAssignChecker;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Access Control")
@Story("Test Target Access Controlling")
class TargetAccessControllingTest extends AbstractAccessControllingTest {

    @Autowired
    AutoAssignChecker autoAssignChecker;

    @Test
    @Description("Verifies read access rules for targets")
    void verifyTargetReadOperations() {
        permitAllOperations(AccessController.Operation.CREATE);

        final Target permittedTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device01").status(TargetUpdateStatus.REGISTERED));

        final Target hiddenTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device02").status(TargetUpdateStatus.REGISTERED));

        // define access controlling rule
        testAccessControlManger.defineAccessRule(JpaTarget.class, AccessController.Operation.READ,
                TargetSpecifications.hasId(permittedTarget.getId()));

        // verify targetManagement#findAll
        assertThat(targetManagement.findAll(Pageable.unpaged()).get().map(Identifiable::getId).toList())
                .containsOnly(permittedTarget.getId());

        // verify targetManagement#findByRsql
        assertThat(targetManagement.findByRsql(Pageable.unpaged(), "id==*").get().map(Identifiable::getId).toList())
                .containsOnly(permittedTarget.getId());

        // verify targetManagement#findByUpdateStatus
        assertThat(targetManagement.findByUpdateStatus(Pageable.unpaged(), TargetUpdateStatus.REGISTERED).get()
                .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());

        // verify targetManagement#getByControllerID
        assertThat(targetManagement.getByControllerID(permittedTarget.getControllerId())).isPresent();
        assertThat(targetManagement.getByControllerID(hiddenTarget.getControllerId())).isEmpty();

        // verify targetManagement#getByControllerID
        assertThat(targetManagement
                .getByControllerID(Arrays.asList(permittedTarget.getControllerId(), hiddenTarget.getControllerId()))
                .stream().map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());

        // verify targetManagement#get
        assertThat(targetManagement.get(permittedTarget.getId())).isPresent();
        assertThat(targetManagement.get(hiddenTarget.getId())).isEmpty();

        // verify targetManagement#get
        assertThat(targetManagement.get(Arrays.asList(permittedTarget.getId(), hiddenTarget.getId())).stream()
                .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());

        // verify targetManagement#getControllerAttributes
        assertThat(targetManagement.getControllerAttributes(permittedTarget.getControllerId())).isEmpty();
        assertThatThrownBy(() -> {
            assertThat(targetManagement.getControllerAttributes(hiddenTarget.getControllerId())).isEmpty();
        }).as("Target should not be found.").isInstanceOf(EntityNotFoundException.class);

        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("test").query("id==*"));

        // verify targetManagement#findByTargetFilterQuery
        assertThat(targetManagement.findByTargetFilterQuery(Pageable.unpaged(), targetFilterQuery.getId()).get()
                .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());

        // verify targetManagement#findByTargetFilterQuery (used by UI)
        assertThat(targetManagement.findByFilters(Pageable.unpaged(), new FilterParams(null, null, null, null)).get()
                .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());
    }

    @Test
    void verifyTagFilteringAndManagement() {
        // permit all operations first to prepare test setup
        permitAllOperations(AccessController.Operation.CREATE);
        permitAllOperations(AccessController.Operation.UPDATE);

        final Target permittedTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device01").status(TargetUpdateStatus.REGISTERED));

        final Target readOnlyTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device02").status(TargetUpdateStatus.REGISTERED));

        final Target hiddenTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device03").status(TargetUpdateStatus.REGISTERED));

        final TargetTag myTag = targetTagManagement.create(entityFactory.tag().create().name("myTag"));

        // perform tag assignment before setting access rules
        targetManagement.assignTag(Arrays.asList(permittedTarget.getControllerId(), readOnlyTarget.getControllerId(),
                hiddenTarget.getControllerId()), myTag.getId());

        // define access controlling rule
        testAccessControlManger.deleteAllRules();
        testAccessControlManger.defineAccessRule(JpaTarget.class, AccessController.Operation.READ,
                TargetSpecifications.hasIdIn(Arrays.asList(permittedTarget.getId(), readOnlyTarget.getId())));
        // allow update operation
        testAccessControlManger.permitOperation(JpaTarget.class, AccessController.Operation.UPDATE,
                target -> target.getId().equals(permittedTarget.getId()));

        // verify targetManagement#findByTag
        assertThat(
                targetManagement.findByTag(Pageable.unpaged(), myTag.getId()).get().map(Identifiable::getId).toList())
                .containsOnly(permittedTarget.getId(), readOnlyTarget.getId());

        // verify targetManagement#findByRsqlAndTag
        assertThat(targetManagement.findByRsqlAndTag(Pageable.unpaged(), "id==*", myTag.getId()).get()
                .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId(), readOnlyTarget.getId());

        // verify targetManagement#toggleTagAssignment on permitted target
        assertThat(targetManagement
                .toggleTagAssignment(Collections.singletonList(permittedTarget.getControllerId()), myTag.getName())
                .getUnassigned()).isEqualTo(1);
        // verify targetManagement#assignTag on permitted target
        assertThat(
                targetManagement.assignTag(Collections.singletonList(permittedTarget.getControllerId()), myTag.getId()))
                .hasSize(1);
        // verify targetManagement#unAssignTag on permitted target
        assertThat(targetManagement.unAssignTag(permittedTarget.getControllerId(), myTag.getId()).getControllerId())
                .isEqualTo(permittedTarget.getControllerId());

        // assignment is denied for readOnlyTarget (read, but no update permissions)
        assertThatThrownBy(() -> {
            targetManagement
                    .toggleTagAssignment(Collections.singletonList(readOnlyTarget.getControllerId()), myTag.getName())
                    .getUnassigned();
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(InsufficientPermissionException.class);

        // assignment is denied for readOnlyTarget (read, but no update permissions)
        assertThatThrownBy(() -> {
            targetManagement.assignTag(Collections.singletonList(readOnlyTarget.getControllerId()), myTag.getId());
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(InsufficientPermissionException.class);

        // assignment is denied for readOnlyTarget (read, but no update permissions)
        assertThatThrownBy(() -> {
            targetManagement.unAssignTag(readOnlyTarget.getControllerId(), myTag.getId());
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(InsufficientPermissionException.class);

        // assignment is denied for hiddenTarget since it's hidden
        assertThatThrownBy(() -> {
            targetManagement
                    .toggleTagAssignment(Collections.singletonList(hiddenTarget.getControllerId()), myTag.getName())
                    .getUnassigned();
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(EntityNotFoundException.class);

        // assignment is denied for hiddenTarget since it's hidden
        assertThatThrownBy(() -> {
            targetManagement.assignTag(Collections.singletonList(hiddenTarget.getControllerId()), myTag.getId());
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(EntityNotFoundException.class);

        // assignment is denied for hiddenTarget since it's hidden
        assertThatThrownBy(() -> {
            targetManagement.unAssignTag(hiddenTarget.getControllerId(), myTag.getId());
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @Description("Verifies rules for target assignment")
    void verifyTargetAssignment() {
        permitAllOperations(AccessController.Operation.CREATE);

        final Target permittedTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device01").status(TargetUpdateStatus.REGISTERED));

        final Target hiddenTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device02").status(TargetUpdateStatus.REGISTERED));

        final DistributionSet ds = testdataFactory.createDistributionSet("myDs");

        // define access controlling rule
        testAccessControlManger.defineAccessRule(JpaTarget.class, AccessController.Operation.READ,
                TargetSpecifications.hasId(permittedTarget.getId()));

        // verify targetManagement#findByUpdateStatus before assignment
        assertThat(targetManagement.findByUpdateStatus(Pageable.unpaged(), TargetUpdateStatus.REGISTERED).get()
                .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());

        testAccessControlManger.permitOperation(JpaTarget.class, AccessController.Operation.UPDATE,
                target -> target.getId().equals(permittedTarget.getId()));

        assertThat(assignDistributionSet(ds.getId(), permittedTarget.getControllerId()).getAssigned()).isEqualTo(1);
        assertThat(assignDistributionSet(ds.getId(), hiddenTarget.getControllerId()).getAssigned()).isZero();

        // verify targetManagement#findByUpdateStatus(REGISTERED) after assignment
        assertThat(targetManagement.findByUpdateStatus(Pageable.unpaged(), TargetUpdateStatus.REGISTERED)
                .getTotalElements()).isZero();

        // verify targetManagement#findByUpdateStatus(PENDING) after assignment
        assertThat(targetManagement.findByUpdateStatus(Pageable.unpaged(), TargetUpdateStatus.PENDING).get()
                .map(Identifiable::getId).toList()).containsOnly(permittedTarget.getId());
    }

    @Test
    @Description("Verifies rules for target assignment")
    void verifyTargetAssignmentOnNonUpdatableTarget() {
        permitAllOperations(AccessController.Operation.CREATE);

        final Target manageableTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device01").status(TargetUpdateStatus.REGISTERED));

        final Target readOnlyTarget = targetManagement
                .create(entityFactory.target().create().controllerId("device02").status(TargetUpdateStatus.REGISTERED));

        final DistributionSet firstDs = testdataFactory.createDistributionSet("myDs");

        // define access controlling rule
        testAccessControlManger.defineAccessRule(JpaTarget.class, AccessController.Operation.READ,
                TargetSpecifications.hasIdIn(Arrays.asList(manageableTarget.getId(), readOnlyTarget.getId())));

        testAccessControlManger.permitOperation(JpaTarget.class, AccessController.Operation.UPDATE,
                target -> target.getId().equals(manageableTarget.getId()));

        // assignment is permitted for manageableTarget
        assertThat(assignDistributionSet(firstDs.getId(), manageableTarget.getControllerId()).getAssigned())
                .isEqualTo(1);

        // assignment is denied for readOnlyTarget (read, but no update permissions)
        assertThatThrownBy(() -> {
            assertThat(assignDistributionSet(firstDs.getId(), readOnlyTarget.getControllerId()).getAssigned()).isZero();
        }).as("Target type delete shouldn't be allowed since the target type is not visible.")
                .isInstanceOf(InsufficientPermissionException.class);

        final DistributionSet secondDs = testdataFactory.createDistributionSet("anotherDs");

        // bunch assignment is denied since at least one target without update
        // permissions is present
        assertThatThrownBy(() -> {
            assertThat(assignDistributionSet(secondDs.getId(),
                    Arrays.asList(readOnlyTarget.getControllerId(), manageableTarget.getControllerId()),
                    Action.ActionType.FORCED).getAssigned()).isZero();
        }).as("Target type delete shouldn't be allowed since the target type is not visible.")
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    @Description("Verifies only manageable targets are part of the rollout")
    void verifyRolloutTargetScope() {
        permitAllOperations(AccessController.Operation.CREATE);

        final List<Target> updateTargets = testdataFactory.createTargets("update1", "update2", "update3");
        final List<Target> readTargets = testdataFactory.createTargets("read1", "read2", "read3", "read4");
        final List<Target> hiddenTargets = testdataFactory.createTargets("hidden1", "hidden2", "hidden3", "hidden4",
                "hidden5");

        testAccessControlManger.defineAccessRule(JpaTarget.class, AccessController.Operation.UPDATE,
                TargetSpecifications.hasIdIn(updateTargets.stream().map(Identifiable::getId).toList()));
        testAccessControlManger.defineAccessRule(JpaTarget.class, AccessController.Operation.READ,
                TargetSpecifications.hasIdIn(readTargets.stream().map(Identifiable::getId).toList()));

        final Rollout rollout = testdataFactory.createRolloutByVariables("testRollout", "description",
                updateTargets.size(), "id==*", testdataFactory.createDistributionSet(), "50", "5");

        assertThat(rollout.getTotalTargets()).isEqualTo(updateTargets.size());

        final List<RolloutGroup> content = rolloutGroupManagement.findByRollout(Pageable.unpaged(), rollout.getId())
                .getContent();
        assertThat(content).hasSize(updateTargets.size());

        final List<Target> rolloutTargets = content.stream().flatMap(
                group -> rolloutGroupManagement.findTargetsOfRolloutGroup(Pageable.unpaged(), group.getId()).get())
                .toList();

        assertThat(rolloutTargets).hasSize(updateTargets.size()).allMatch(
                target -> updateTargets.stream().anyMatch(readTarget -> readTarget.getId().equals(target.getId())))
                .noneMatch(target -> readTargets.stream()
                        .anyMatch(readTarget -> readTarget.getId().equals(target.getId())))
                .noneMatch(target -> hiddenTargets.stream()
                        .anyMatch(readTarget -> readTarget.getId().equals(target.getId())));
    }

    @Test
    @Description("Verifies only manageable targets are part of an auto assignment.")
    void verifyAutoAssignmentTargetScope() {
        permitAllOperations(AccessController.Operation.CREATE);

        final List<Target> updateTargets = testdataFactory.createTargets("update1", "update2", "update3");
        final List<Target> readTargets = testdataFactory.createTargets("read1", "read2", "read3", "read4");
        final List<Target> hiddenTargets = testdataFactory.createTargets("hidden1", "hidden2", "hidden3", "hidden4",
                "hidden5");

        testAccessControlManger.permitOperation(JpaTarget.class, AccessController.Operation.UPDATE,
                target -> updateTargets.stream().map(Identifiable::getId).anyMatch(id -> target.getId().equals(id)));

        testAccessControlManger.defineAccessRule(JpaTarget.class, AccessController.Operation.UPDATE,
                TargetSpecifications.hasIdIn(updateTargets.stream().map(Identifiable::getId).toList()));
        testAccessControlManger.defineAccessRule(JpaTarget.class, AccessController.Operation.READ,
                TargetSpecifications.hasIdIn(updateTargets.stream().map(Identifiable::getId).toList()));
        testAccessControlManger.defineAccessRule(JpaTarget.class, AccessController.Operation.READ,
                TargetSpecifications.hasIdIn(readTargets.stream().map(Identifiable::getId).toList()));

        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("testName").query("id==*"));

        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                .updateAutoAssign(targetFilterQuery.getId()).ds(distributionSet.getId()));

        autoAssignChecker.checkAllTargets();

        assertThat(targetManagement.findByAssignedDistributionSet(Pageable.unpaged(), distributionSet.getId())
                .getContent())
                .hasSize(updateTargets.size())
                .allMatch(assignedTarget -> updateTargets.stream()
                        .anyMatch(updateTarget -> updateTarget.getId().equals(assignedTarget.getId())))
                .noneMatch(assignedTarget -> readTargets.stream()
                        .anyMatch(updateTarget -> updateTarget.getId().equals(assignedTarget.getId())))
                .noneMatch(assignedTarget -> hiddenTargets.stream()
                        .anyMatch(updateTarget -> updateTarget.getId().equals(assignedTarget.getId())));
    }

}

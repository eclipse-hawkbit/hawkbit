/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility.RsqlToSpecBuilder.LEGACY_G1;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpRole;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.TargetManagement.Create;
import org.eclipse.hawkbit.repository.TargetManagement.Update;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAddressException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetTypeAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Target Management
 */
class TargetManagementTest extends AbstractJpaIntegrationTest {

    private static final String WHITESPACE_ERROR = "target with whitespaces in controller id should not be created";

    /**
     * Verifies that management get access react as specified on calls for non existing entities by means 
     * of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1) })
    void nonExistingEntityAccessReturnsNotPresent() {
        final Target target = testdataFactory.createTarget();
        assertThat(targetManagement.getByControllerId(NOT_EXIST_ID)).isNotPresent();
        assertThat(targetManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(targetManagement.getMetadata(target.getControllerId()).get(NOT_EXIST_ID)).isNull();
    }

    /**
     * Verifies that management queries react as specified on calls for non existing entities 
     *  by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetTagCreatedEvent.class, count = 1) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final TargetTag tag = targetTagManagement.create(TargetTagManagement.Create.builder().name("A").build());
        final Target target = testdataFactory.createTarget();

        verifyThrownExceptionBy(
                () -> targetManagement.assignTag(Collections.singletonList(target.getControllerId()), NOT_EXIST_IDL), "TargetTag");
        verifyThrownExceptionBy(() -> targetManagement.assignTag(Collections.singletonList(NOT_EXIST_ID), tag.getId()), "Target");

        verifyThrownExceptionBy(() -> targetManagement.findByTag(NOT_EXIST_IDL, PAGE), "TargetTag");
        verifyThrownExceptionBy(() -> targetManagement.findByRsqlAndTag("name==*", NOT_EXIST_IDL, PAGE), "TargetTag");

        verifyThrownExceptionBy(() -> targetManagement.countByAssignedDistributionSet(NOT_EXIST_IDL), "DistributionSet");
        verifyThrownExceptionBy(() -> targetManagement.countByInstalledDistributionSet(NOT_EXIST_IDL), "DistributionSet");
        verifyThrownExceptionBy(() -> targetManagement.existsByInstalledOrAssignedDistributionSet(NOT_EXIST_IDL), "DistributionSet");

        verifyThrownExceptionBy(() -> targetManagement.countByTargetFilterQuery(NOT_EXIST_IDL), "TargetFilterQuery");
        verifyThrownExceptionBy(() -> targetManagement.countByRsqlAndNonDSAndCompatibleAndUpdatable(NOT_EXIST_IDL, "name==*"),
                "DistributionSet");

        verifyThrownExceptionBy(() -> targetManagement.deleteByControllerID(NOT_EXIST_ID), "Target");
        verifyThrownExceptionBy(() -> targetManagement.delete(Collections.singletonList(NOT_EXIST_IDL)), "Target");

        verifyThrownExceptionBy(
                () -> targetManagement.findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(NOT_EXIST_IDL, "name==*", PAGE),
                "DistributionSet");
        verifyThrownExceptionBy(() -> targetManagement.findByInRolloutGroupWithoutAction(NOT_EXIST_IDL, PAGE), "RolloutGroup");
        verifyThrownExceptionBy(() -> targetManagement.findByAssignedDistributionSet(NOT_EXIST_IDL, PAGE), "DistributionSet");
        verifyThrownExceptionBy(
                () -> targetManagement.findByAssignedDistributionSetAndRsql(NOT_EXIST_IDL, "name==*", PAGE), "DistributionSet");

        verifyThrownExceptionBy(() -> targetManagement.findByInstalledDistributionSet(NOT_EXIST_IDL, PAGE), "DistributionSet");
        verifyThrownExceptionBy(
                () -> targetManagement.findByInstalledDistributionSetAndRsql(NOT_EXIST_IDL, "name==*", PAGE), "DistributionSet");

        verifyThrownExceptionBy(() -> targetManagement
                .assignTag(Collections.singletonList(target.getControllerId()), Long.parseLong(NOT_EXIST_ID)), "TargetTag");
        verifyThrownExceptionBy(
                () -> targetManagement.assignTag(Collections.singletonList(NOT_EXIST_ID), tag.getId()), "Target");

        verifyThrownExceptionBy(() -> targetManagement.unassignTag(List.of(NOT_EXIST_ID), tag.getId()), "Target");
        verifyThrownExceptionBy(() -> targetManagement.unassignTag(List.of(target.getControllerId()), NOT_EXIST_IDL), "TargetTag");
        verifyThrownExceptionBy(() -> targetManagement.update(Update.builder().id(NOT_EXIST_IDL).build()), "Target");

        verifyThrownExceptionBy(() -> targetManagement.createMetadata(NOT_EXIST_ID, Map.of("123", "123")), "Target");
        verifyThrownExceptionBy(() -> targetManagement.deleteMetadata(NOT_EXIST_ID, "xxx"), "Target");
        verifyThrownExceptionBy(() -> targetManagement.deleteMetadata(target.getControllerId(), NOT_EXIST_ID), "Target");
        verifyThrownExceptionBy(() -> targetManagement.getMetadata(NOT_EXIST_ID).get("xxx"), "Target");
        verifyThrownExceptionBy(() -> targetManagement.createMetadata(NOT_EXIST_ID, "xxx", "xxx"), "Target");
    }

    /**
     * Ensures that retrieving the target security is only permitted with the necessary permissions.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1) })
    void getTargetSecurityTokenOnlyWithCorrectPermission() throws Exception {
        final Target createdTarget = targetManagement
                .create(Create.builder().controllerId("targetWithSecurityToken").securityToken("token").build());

        // retrieve security token only with READ_TARGET_SEC_TOKEN permission
        final String securityTokenWithReadPermission = SecurityContextSwitch.getAs(
                SecurityContextSwitch.withUser("OnlyTargetReadPermission", SpPermission.READ_TARGET_SEC_TOKEN),
                createdTarget::getSecurityToken);
        // retrieve security token only with ROLE_TARGET_ADMIN permission
        final String securityTokenWithTargetAdminPermission = SecurityContextSwitch.getAs(
                SecurityContextSwitch.withUser("OnlyTargetAdminPermission", SpRole.TARGET_ADMIN),
                createdTarget::getSecurityToken);
        // retrieve security token only with ROLE_TENANT_ADMIN permission
        final String securityTokenWithTenantAdminPermission = SecurityContextSwitch.getAs(
                SecurityContextSwitch.withUser("OnlyTenantAdminPermission", SpRole.TENANT_ADMIN),
                createdTarget::getSecurityToken);

        // retrieve security token as system code execution
        final String securityTokenAsSystemCode = systemSecurityContext.runAsSystem(createdTarget::getSecurityToken);

        // retrieve security token without any permissions
        final String securityTokenWithoutPermission = SecurityContextSwitch
                .getAs(SecurityContextSwitch.withUser("NoPermission"), createdTarget::getSecurityToken);

        assertThat(createdTarget.getSecurityToken()).isEqualTo("token");
        assertThat(securityTokenWithReadPermission).isNotNull();
        assertThat(securityTokenWithTargetAdminPermission).isNotNull();
        assertThat(securityTokenWithTenantAdminPermission).isNotNull();
        assertThat(securityTokenAsSystemCode).isNotNull();

        assertThat(securityTokenWithoutPermission).isNull();
    }

    /**
     * Verify that a target with same controller ID than another device cannot be created.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1) })
    void createTargetThatViolatesUniqueConstraintFails() {
        final Create targetCreate = Create.builder().controllerId("123").build();
        targetManagement.create(targetCreate);
        assertThatExceptionOfType(EntityAlreadyExistsException.class).isThrownBy(() -> targetManagement.create(targetCreate));
    }

    /**
     * Verify that a target with with invalid properties cannot be created or updated
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class) })
    void createAndUpdateTargetWithInvalidFields() {
        final Target target = testdataFactory.createTarget();

        createTargetWithInvalidControllerId();
        createAndUpdateTargetWithInvalidDescription(target);
        createAndUpdateTargetWithInvalidName(target);
        createAndUpdateTargetWithInvalidSecurityToken(target);
        createAndUpdateTargetWithInvalidAddress(target);
    }

    /**
     * Ensures that targets can assigned and unassigned to a target tag. Not exists target will be ignored for the assignment.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 4),
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 5) })
    void assignAndUnassignTargetsToTag() {
        final List<String> assignTarget = new ArrayList<>();
        assignTarget.add(targetManagement.create(Create.builder().controllerId("targetId123").build()).getControllerId());
        assignTarget.add(targetManagement.create(Create.builder().controllerId("targetId1234").build()).getControllerId());
        assignTarget.add(targetManagement.create(Create.builder().controllerId("targetId1235").build()).getControllerId());
        assignTarget.add(targetManagement.create(Create.builder().controllerId("targetId1236").build()).getControllerId());

        final TargetTag targetTag = targetTagManagement.create(TargetTagManagement.Create.builder().name("Tag1").build());

        final List<Target> assignedTargets = targetManagement.assignTag(assignTarget, targetTag.getId());
        assertThat(assignedTargets).as("Assigned targets are wrong").hasSize(4);
        assignedTargets.forEach(target -> assertThat(getTargetTags(target.getControllerId())).hasSize(1));

        final TargetTag findTargetTag = targetTagManagement.get(targetTag.getId()).orElseThrow(IllegalStateException::new);
        assertThat(assignedTargets).as("Assigned targets are wrong")
                .hasSize(targetManagement.findByTag(targetTag.getId(), PAGE).getNumberOfElements());

        final Target unAssignTarget = targetManagement.unassignTag(List.of("targetId123"), findTargetTag.getId()).get(0);
        assertThat(unAssignTarget.getControllerId()).as("Controller id is wrong").isEqualTo("targetId123");
        assertThat(getTargetTags(unAssignTarget.getControllerId())).as("Tag size is wrong").isEmpty();
        targetTagManagement.get(targetTag.getId()).orElseThrow(NoSuchElementException::new);
        assertThat(targetManagement.findByTag(targetTag.getId(), PAGE)).as("Assigned targets are wrong").hasSize(3);
        assertThat(targetManagement.findByRsqlAndTag("controllerId==targetId123", targetTag.getId(), PAGE))
                .as("Assigned targets are wrong").isEmpty();
        assertThat(targetManagement.findByRsqlAndTag("controllerId==targetId1234", targetTag.getId(), PAGE))
                .as("Assigned targets are wrong").hasSize(1);

    }

    /**
     * Ensures that targets can deleted e.g. test all cascades
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 12),
            @Expect(type = TargetDeletedEvent.class, count = 12),
            @Expect(type = TargetUpdatedEvent.class, count = 6) })
    void deleteAndCreateTargets() {
        Target target = targetManagement.create(Create.builder().controllerId("targetId123").build());
        assertThat(targetManagement.count()).as("target count is wrong").isEqualTo(1);
        targetManagement.delete(Collections.singletonList(target.getId()));
        assertThat(targetManagement.count()).as("target count is wrong").isZero();

        target = createTargetWithAttributes("4711");
        assertThat(targetManagement.count()).as("target count is wrong").isEqualTo(1);
        assertThat(targetManagement.existsByControllerId("4711")).isTrue();
        targetManagement.delete(Collections.singletonList(target.getId()));
        assertThat(targetManagement.count()).as("target count is wrong").isZero();
        assertThat(targetManagement.existsByControllerId("4711")).isFalse();

        final List<Long> targets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            target = targetManagement.create(Create.builder().controllerId("" + i).build());
            targets.add(target.getId());
            targets.add(createTargetWithAttributes("" + (i * i + 1000)).getId());
        }
        assertThat(targetManagement.count()).as("target count is wrong").isEqualTo(10);
        targetManagement.delete(targets);
        assertThat(targetManagement.count()).as("target count is wrong").isZero();
    }

    /**
     * Finds a target by given ID and checks if all data is in the response (including the data defined as lazy).
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 5),
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void findTargetByControllerIdWithDetails() {
        final DistributionSet testDs1 = testdataFactory.createDistributionSet("test");
        final DistributionSet testDs2 = testdataFactory.createDistributionSet("test2");

        assertThat(targetManagement.countByAssignedDistributionSet(testDs1.getId()))
                .as("For newly created distributions sets the assigned target count should be zero").isZero();
        assertThat(targetManagement.countByInstalledDistributionSet(testDs1.getId()))
                .as("For newly created distributions sets the installed target count should be zero").isZero();
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs1.getId()))
                .as("Exists assigned or installed query should return false for new distribution sets").isFalse();
        assertThat(targetManagement.countByAssignedDistributionSet(testDs2.getId()))
                .as("For newly created distributions sets the assigned target count should be zero").isZero();
        assertThat(targetManagement.countByInstalledDistributionSet(testDs2.getId()))
                .as("For newly created distributions sets the installed target count should be zero").isZero();
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs2.getId()))
                .as("For newly created distributions sets the assigned target count should be zero").isFalse();

        createTargetWithAttributes("4711");

        final long current = System.currentTimeMillis();
        controllerManagement.findOrRegisterTargetIfItDoesNotExist("4711", LOCALHOST);

        final DistributionSetAssignmentResult result = assignDistributionSet(testDs1.getId(), "4711");
        implicitLock(testDs1);

        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(getFirstAssignedActionId(result)).status(Status.FINISHED));
        assignDistributionSet(testDs2.getId(), "4711");
        implicitLock(testDs2);

        Target target = targetManagement.getByControllerId("4711").orElseThrow(IllegalStateException::new);
        // read data

        assertThat(targetManagement.countByAssignedDistributionSet(testDs1.getId())).as("Target count is wrong")
                .isZero();
        assertThat(targetManagement.countByInstalledDistributionSet(testDs1.getId())).as("Target count is wrong")
                .isEqualTo(1);
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs1.getId()))
                .as("Target count is wrong").isTrue();
        assertThat(targetManagement.countByAssignedDistributionSet(testDs2.getId())).as("Target count is wrong")
                .isEqualTo(1);
        assertThat(targetManagement.countByInstalledDistributionSet(testDs2.getId())).as("Target count is wrong")
                .isZero();
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs2.getId()))
                .as("Target count is wrong").isTrue();
        assertThat(target.getLastTargetQuery()).as("Target query is not work").isGreaterThanOrEqualTo(current);

        final DistributionSet assignedDs = deploymentManagement.getAssignedDistributionSet("4711")
                .orElseThrow(NoSuchElementException::new);
        assertThat(assignedDs).as("Assigned ds size is wrong").isEqualTo(testDs2);

        final DistributionSet installedDs = deploymentManagement.getInstalledDistributionSet("4711")
                .orElseThrow(NoSuchElementException::new);
        assertThat(installedDs).as("Installed ds is wrong").isEqualTo(testDs1);
    }

    /**
     * Checks if the EntityAlreadyExistsException is thrown if the targets with the same controller ID are created twice.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 5) })
    void createMultipleTargetsDuplicate() {
        testdataFactory.createTargets(5, "mySimpleTargs", "my simple targets");
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("Targets already exists")
                .isThrownBy(() -> testdataFactory.createTargets(5, "mySimpleTargs", "my simple targets"));
    }

    /**
     * Checks if the EntityAlreadyExistsException is thrown if a single target with the same controller ID are created twice.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1) })
    void createTargetDuplicate() {
        final Create targetCreate = Create.builder().controllerId("4711").build();
        targetManagement.create(targetCreate);
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("Target already exists")
                .isThrownBy(() -> targetManagement.create(targetCreate));
    }

    /**
     * Creates and updates a target and verifies the changes in the repository.
     */
    @Test
    @WithUser(allSpPermissions = true)
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    void singleTargetIsInsertedIntoRepo() throws Exception {

        final String myCtrlID = "myCtrlID";

        Target savedTarget = testdataFactory.createTarget(myCtrlID);
        assertThat(savedTarget).as("The target should not be null").isNotNull();
        final long createdAt = savedTarget.getCreatedAt();
        long modifiedAt = savedTarget.getLastModifiedAt();

        assertThat(createdAt).as("CreatedAt compared with modifiedAt").isEqualTo(modifiedAt);

        Awaitility.await().until(() -> System.currentTimeMillis() > createdAt + 1);

        savedTarget = targetManagement.update(Update.builder().id(savedTarget.getId()).description("changed description").build());
        assertThat(createdAt).as("CreatedAt compared with saved modifiedAt").isNotEqualTo(savedTarget.getLastModifiedAt());
        assertThat(modifiedAt).as("ModifiedAt compared with saved modifiedAt").isNotEqualTo(savedTarget.getLastModifiedAt());
        modifiedAt = savedTarget.getLastModifiedAt();

        final Target foundTarget = targetManagement.getByControllerId(savedTarget.getControllerId())
                .orElseThrow(IllegalStateException::new);
        assertThat(foundTarget).as("The target should not be null").isNotNull();
        assertThat(myCtrlID).as("ControllerId compared with saved controllerId").isEqualTo(foundTarget.getControllerId());
        assertThat(savedTarget).as("Target compared with saved target").isEqualTo(foundTarget);
        assertThat(createdAt).as("CreatedAt compared with saved createdAt").isEqualTo(foundTarget.getCreatedAt());
        assertThat(modifiedAt).as("LastModifiedAt compared with saved lastModifiedAt").isEqualTo(foundTarget.getLastModifiedAt());
    }

    /**
     * Create multiple targets as bulk operation and delete them in bulk.
     */
    @Test
    @WithUser(allSpPermissions = true)
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 101),
            @Expect(type = TargetUpdatedEvent.class, count = 100),
            @Expect(type = TargetDeletedEvent.class, count = 51) })
    void bulkTargetCreationAndDelete() {
        final String myCtrlID = "myCtrlID";
        List<? extends Target> firstList = testdataFactory.createTargets(100, myCtrlID, "first description");

        final Target extra = testdataFactory.createTarget("myCtrlID-00081XX");

        final Iterable<JpaTarget> allFound = targetRepository.findAll();

        assertThat(Long.valueOf(firstList.size())).as("List size of targets")
                .isEqualTo(firstList.spliterator().getExactSizeIfKnown());
        assertThat(Long.valueOf(firstList.size() + 1)).as("LastModifiedAt compared with saved lastModifiedAt")
                .isEqualTo(allFound.spliterator().getExactSizeIfKnown());

        // change the objects and save to again to trigger a change on
        // lastModifiedAt
        firstList = firstList.stream()
                .map(t -> targetManagement.update(
                        Update.builder().id(t.getId()).name(t.getName().concat("\tchanged")).build()))
                .toList();

        // verify that all entries are found
    _founds:
        for (final Target foundTarget : allFound) {
            for (final Target changedTarget : firstList) {
                if (changedTarget.getControllerId().equals(foundTarget.getControllerId())) {
                    assertThat(changedTarget.getDescription())
                            .as("Description of changed target compared with description saved target")
                            .isEqualTo(foundTarget.getDescription());
                    assertThat(changedTarget.getName()).as("Name of changed target starts with name of saved target")
                            .startsWith(foundTarget.getName());
                    assertThat(changedTarget.getName()).as("Name of changed target ends with 'changed'")
                            .endsWith("changed");
                    assertThat(changedTarget.getCreatedAt()).as("CreatedAt compared with saved createdAt")
                            .isEqualTo(foundTarget.getCreatedAt());
                    assertThat(changedTarget.getLastModifiedAt()).as("LastModifiedAt compared with saved createdAt")
                            .isNotEqualTo(changedTarget.getCreatedAt());
                    continue _founds;
                }
            }

            if (!foundTarget.getControllerId().equals(extra.getControllerId())) {
                fail("The controllerId of the found target is not equal to the controllerId of the saved target");
            }
        }

        targetManagement.deleteByControllerID(extra.getControllerId());

        final int numberToDelete = 50;
        final Collection<? extends Target> targetsToDelete = firstList.subList(0, numberToDelete);
        final Target[] deletedTargets = toArray(targetsToDelete, Target.class);
        final List<Long> targetsIdsToDelete = targetsToDelete.stream().map(Target::getId).toList();

        targetManagement.delete(targetsIdsToDelete);

        final List<? extends Target> targetsLeft = targetManagement.findAll(PageRequest.of(0, 200)).getContent();
        assertThat(firstList.spliterator().getExactSizeIfKnown() - numberToDelete).as("Size of split list")
                .isEqualTo(targetsLeft.spliterator().getExactSizeIfKnown());

       Assertions.<Target>assertThat(targetsLeft).as("Not all undeleted found").doesNotContain(deletedTargets);
    }

    /**
     * Tests the assignment of tags to the a single target.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetTagCreatedEvent.class, count = 7),
            @Expect(type = TargetUpdatedEvent.class, count = 7) })
    void targetTagAssignment() {
        final Target t1 = testdataFactory.createTarget("id-1");
        final int noT2Tags = 4;
        final int noT1Tags = 3;
        final List<? extends TargetTag> t1Tags = testdataFactory.createTargetTags(noT1Tags, "tag1");

        t1Tags.forEach(tag -> targetManagement.assignTag(Collections.singletonList(t1.getControllerId()), tag.getId()));

        final Target t2 = testdataFactory.createTarget("id-2");
        final List<? extends TargetTag> t2Tags = testdataFactory.createTargetTags(noT2Tags, "tag2");
        t2Tags.forEach(tag -> targetManagement.assignTag(Collections.singletonList(t2.getControllerId()), tag.getId()));

        final Target t11 = targetManagement.getByControllerId(t1.getControllerId())
                .orElseThrow(IllegalStateException::new);
        assertThat(getTargetTags(t11.getControllerId())).as("Tag size is wrong")
                .hasSize(noT1Tags).containsAll(t1Tags);
        assertThat(getTargetTags(t11.getControllerId())).as("Tag size is wrong")
                .hasSize(noT1Tags).doesNotContain(toArray(t2Tags, TargetTag.class));

        final Target t21 = targetManagement.getByControllerId(t2.getControllerId())
                .orElseThrow(IllegalStateException::new);
        assertThat(getTargetTags(t21.getControllerId())).as("Tag size is wrong")
                .hasSize(noT2Tags).containsAll(t2Tags);
        assertThat(getTargetTags(t21.getControllerId())).as("Tag size is wrong")
                .hasSize(noT2Tags).doesNotContain(toArray(t1Tags, TargetTag.class));
    }

    /**
     * Tests the assignment of tags to multiple targets.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 50),
            @Expect(type = TargetTagCreatedEvent.class, count = 4),
            @Expect(type = TargetUpdatedEvent.class, count = 80) })
    void targetTagBulkAssignments() {
        final List<Target> tagATargets = testdataFactory.createTargets(10, "tagATargets", "first description");
        final List<Target> tagBTargets = testdataFactory.createTargets(10, "tagBTargets", "first description");
        final List<Target> tagCTargets = testdataFactory.createTargets(10, "tagCTargets", "first description");

        final List<Target> tagABTargets = testdataFactory.createTargets(10, "tagABTargets", "first description");

        final List<Target> tagABCTargets = testdataFactory.createTargets(10, "tagABCTargets", "first description");

        final TargetTag tagA = targetTagManagement.create(TargetTagManagement.Create.builder().name("A").build());
        final TargetTag tagB = targetTagManagement.create(TargetTagManagement.Create.builder().name("B").build());
        final TargetTag tagC = targetTagManagement.create(TargetTagManagement.Create.builder().name("C").build());
        targetTagManagement.create(TargetTagManagement.Create.builder().name("X").build());

        // doing different assignments
        assignTag(tagATargets, tagA);
        assignTag(tagBTargets, tagB);
        assignTag(tagCTargets, tagC);

        assignTag(tagABTargets, tagA);
        assignTag(tagABTargets, tagB);

        assignTag(tagABCTargets, tagA);
        assignTag(tagABCTargets, tagB);
        assignTag(tagABCTargets, tagC);

        assertThat(targetManagement.countByFilters(new FilterParams(null, null, null, null, Boolean.FALSE, "X")))
                .as("Target count is wrong").isZero();

        // search for targets with tag tagA
        final List<Target> targetWithTagA = new ArrayList<>();
        final List<Target> targetWithTagB = new ArrayList<>();
        final List<Target> targetWithTagC = new ArrayList<>();

        // storing target lists to enable easy evaluation
        targetWithTagA.addAll(tagATargets);
        targetWithTagA.addAll(tagABTargets);
        targetWithTagA.addAll(tagABCTargets);

        targetWithTagB.addAll(tagBTargets);
        targetWithTagB.addAll(tagABTargets);
        targetWithTagB.addAll(tagABCTargets);

        targetWithTagC.addAll(tagCTargets);
        targetWithTagC.addAll(tagABCTargets);

        // check the target lists as returned by assignTag
        checkTargetHasTags(false, targetWithTagA, tagA);
        checkTargetHasTags(false, targetWithTagB, tagB);
        checkTargetHasTags(false, targetWithTagC, tagC);

        checkTargetHasNotTags(tagATargets, tagB, tagC);
        checkTargetHasNotTags(tagBTargets, tagA, tagC);
        checkTargetHasNotTags(tagCTargets, tagA, tagB);

        // check again target lists refreshed from DB
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, null, null, Boolean.FALSE, "A")))
                .as("Target count is wrong").isEqualTo(targetWithTagA.size());
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, null, null, Boolean.FALSE, "B")))
                .as("Target count is wrong").isEqualTo(targetWithTagB.size());
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, null, null, Boolean.FALSE, "C")))
                .as("Target count is wrong").isEqualTo(targetWithTagC.size());
    }

    /**
     * Tests the unassigment of tags to multiple targets.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 3),
            @Expect(type = TargetCreatedEvent.class, count = 109),
            @Expect(type = TargetUpdatedEvent.class, count = 227) })
    void targetTagBulkUnassignments() {
        final TargetTag targTagA = targetTagManagement.create(TargetTagManagement.Create.builder().name("Targ-A-Tag").build());
        final TargetTag targTagB = targetTagManagement.create(TargetTagManagement.Create.builder().name("Targ-B-Tag").build());
        final TargetTag targTagC = targetTagManagement.create(TargetTagManagement.Create.builder().name("Targ-C-Tag").build());

        final List<Target> targAs = testdataFactory.createTargets(25, "target-id-A", "first description");
        final List<Target> targBs = testdataFactory.createTargets(20, "target-id-B", "first description");
        final List<Target> targCs = testdataFactory.createTargets(15, "target-id-C", "first description");

        final List<Target> targABs = testdataFactory.createTargets(12, "target-id-AB", "first description");
        final List<Target> targACs = testdataFactory.createTargets(13, "target-id-AC", "first description");
        final List<Target> targBCs = testdataFactory.createTargets(7, "target-id-BC", "first description");
        final List<Target> targABCs = testdataFactory.createTargets(17, "target-id-ABC", "first description");

        assignTag(targAs, targTagA);
        assignTag(targABs, targTagA);
        assignTag(targACs, targTagA);
        assignTag(targABCs, targTagA);

        assignTag(targBs, targTagB);
        assignTag(targABs, targTagB);
        assignTag(targBCs, targTagB);
        assignTag(targABCs, targTagB);

        assignTag(targCs, targTagC);
        assignTag(targACs, targTagC);
        assignTag(targBCs, targTagC);
        assignTag(targABCs, targTagC);

        checkTargetHasTags(true, targAs, targTagA);
        checkTargetHasTags(true, targBs, targTagB);
        checkTargetHasTags(true, targABs, targTagA, targTagB);
        checkTargetHasTags(true, targACs, targTagA, targTagC);
        checkTargetHasTags(true, targBCs, targTagB, targTagC);
        checkTargetHasTags(true, targABCs, targTagA, targTagB, targTagC);

        unassignTag(targCs, targTagC);
        unassignTag(targACs, targTagC);
        unassignTag(targBCs, targTagC);
        unassignTag(targABCs, targTagC);

        checkTargetHasTags(true, targAs, targTagA); // 0
        checkTargetHasTags(true, targBs, targTagB);
        checkTargetHasTags(true, targABs, targTagA, targTagB);
        checkTargetHasTags(true, targBCs, targTagB);
        checkTargetHasTags(true, targACs, targTagA);

        checkTargetHasNotTags(targCs, targTagC);
        checkTargetHasNotTags(targACs, targTagC);
        checkTargetHasNotTags(targBCs, targTagC);
        checkTargetHasNotTags(targABCs, targTagC);
    }

    /**
     * Test that NO TAG functionality which gives all targets with no tag assigned.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 50),
            @Expect(type = TargetUpdatedEvent.class, count = 25) })
    void findTargetsWithNoTag() {
        final TargetTag targTagA = targetTagManagement.create(TargetTagManagement.Create.builder().name("Targ-A-Tag").build());
        final List<Target> targAs = testdataFactory.createTargets(25, "target-id-A", "first description");
        assignTag(targAs, targTagA);

        testdataFactory.createTargets(25, "target-id-B", "first description");

        final String[] tagNames = null;
        final List<Target> targetsListWithNoTag = targetManagement
                .findByFilters(new FilterParams(null, null, null, null, Boolean.TRUE, tagNames), PAGE).getContent();

        assertThat(targetManagement.count()).as("Total targets").isEqualTo(50L);
        assertThat(targetsListWithNoTag).as("Targets with no tag").hasSize(25);

    }

    /**
     * Tests the a target can be read with only the read target permission
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void targetCanBeReadWithOnlyReadTargetPermission() throws Exception {
        final String knownTargetControllerId = "readTarget";
        controllerManagement.findOrRegisterTargetIfItDoesNotExist(knownTargetControllerId, new URI("http://127.0.0.1"));

        SecurityContextSwitch.getAs(SecurityContextSwitch.withUser("bumlux", "READ_TARGET"), () -> {
            final Target findTargetByControllerID = targetManagement.getByControllerId(knownTargetControllerId)
                    .orElseThrow(IllegalStateException::new);
            assertThat(findTargetByControllerID).isNotNull();
            assertThat(findTargetByControllerID.getPollStatus()).isNotNull();
            return null;
        });

    }

    /**
     * Test that RSQL filter finds targets with tags or specific ids.
     */
    @Test
    void findTargetsWithTagOrId() {
        final String rsqlFilter = "tag==Targ-A-Tag,id==target-id-B-00001,id==target-id-B-00008";
        final TargetTag targTagA = targetTagManagement.create(TargetTagManagement.Create.builder().name("Targ-A-Tag").build());
        final List<String> targAs = testdataFactory.createTargets(25, "target-id-A", "first description").stream()
                .map(Target::getControllerId).toList();
        targetManagement.assignTag(targAs, targTagA.getId());

        testdataFactory.createTargets(25, "target-id-B", "first description");

        final Page<? extends Target> foundTargets = targetManagement.findByRsql(rsqlFilter, PAGE);
        final long foundTargetsCount = targetManagement.countByRsql(rsqlFilter);

        assertThat(targetManagement.count()).as("Total targets").isEqualTo(50L);
        assertThat(foundTargets.getNumberOfElements()).as("Targets in RSQL filter").isEqualTo(foundTargetsCount)
                .isEqualTo(27L);
    }

    /**
     * Verify that the find all targets by ids method contains the entities that we are looking for
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 12) })
    void verifyFindTargetAllById() {
        final List<Long> searchIds = Arrays.asList(testdataFactory.createTarget("target-4").getId(),
                testdataFactory.createTarget("target-5").getId(), testdataFactory.createTarget("target-6").getId());
        for (int i = 0; i < 9; i++) {
            testdataFactory.createTarget("test" + i);
        }

        final List<? extends Target> foundDs = targetManagement.get(searchIds);
        assertThat(foundDs).hasSize(3);

        final List<Long> collect = foundDs.stream().map(Target::getId).toList();
        assertThat(collect).containsAll(searchIds);
    }

    /**
     * Verify that the flag for requesting controller attributes is set correctly.
     */
    @Test
    void verifyRequestControllerAttributes() {
        final String knownControllerId = "KnownControllerId";
        final Target target = createTargetWithAttributes(knownControllerId);

        assertThat(targetManagement.findByControllerAttributesRequested(PAGE)).isEmpty();
        assertThat(targetManagement.isControllerAttributesRequested(knownControllerId)).isFalse();

        targetManagement.requestControllerAttributes(knownControllerId);
        final Target updated = targetManagement.getByControllerId(knownControllerId).get();

        assertThat(target.isRequestControllerAttributes()).isFalse();
        assertThat(targetManagement.findByControllerAttributesRequested(PAGE).getContent()).contains(updated);
        assertThat(targetManagement.isControllerAttributesRequested(knownControllerId)).isTrue();
    }

    /**
     * Verifies the enforcement of the metadata quota per target.
     */
    @Test
    void createMetadataUntilQuotaIsExceeded() {
        // add meta-data one by one
        final Target target1 = testdataFactory.createTarget("target1");
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerTarget();
        for (int i = 0; i < maxMetaData; ++i) {
            insertMetadata("k" + i, "v" + i, target1);
        }

        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> insertMetadata("k" + maxMetaData, "v" + maxMetaData, target1));

        // add multiple meta data entries at once
        final Target target2 = testdataFactory.createTarget("target2");
        final Map<String, String> metaData2 = new HashMap<>();
        for (int i = 0; i < maxMetaData + 1; i++) {
            metaData2.put("k" + i, "v" + i);
        }
        // verify quota is exceeded
        final String target2ControllerId = target2.getControllerId();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.createMetadata(target2ControllerId, metaData2));

        // add some meta data entries
        final Target target3 = testdataFactory.createTarget("target3");
        final int firstHalf = maxMetaData / 2;
        for (int i = 0; i < firstHalf; i++) {
            insertMetadata("k" + i, "v" + i, target3);
        }
        // add too many data entries
        final int secondHalf = maxMetaData - firstHalf;
        final Map<String, String> metaData3 = new HashMap<>();
        for (int i = 0; i < secondHalf + 1; i++) {
            metaData3.put("kk" + i, "vv" + i);
        }
        // verify quota is exceeded
        final String target3ControllerId = target3.getControllerId();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.createMetadata(target3ControllerId, metaData3));
    }

    /**
     * Queries and loads the metadata related to a given target.
     */
    @Test
    void getMetadata() {
        // create targets
        assertThat(targetManagement.getMetadata(testdataFactory.createTarget("target0").getControllerId())).isEmpty();
        assertThat(targetManagement.getMetadata(createTargetWithMetadata("target1", 10).getControllerId())).hasSize(10);
        assertThat(targetManagement.getMetadata(createTargetWithMetadata("target2", 8).getControllerId())).hasSize(8);
    }

    /**
     * Checks that metadata for a target can be updated.
     */
    @Test
    @WithUser(allSpPermissions = true)
    void createMetadata() {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a target
        final Target target = testdataFactory.createTarget("target1");
        // initial opt lock revision must be zero
        assertThat(target.getOptLockRevision()).isEqualTo(1);

        // create target meta data entry
        insertMetadata(knownKey, knownValue, target);

        final Target changedLockRevisionTarget = targetManagement.get(target.getId()).orElseThrow(NoSuchElementException::new);
        assertThat(changedLockRevisionTarget.getOptLockRevision()).isEqualTo(2);

        // update the target metadata
        targetManagement.createMetadata(target.getControllerId(), knownKey, knownUpdateValue);
        // we are updating the target meta-data so also modifying the target so opt lock revision must be three
        final Target changedLockRevisionTarget2 = targetManagement.get(target.getId()).orElseThrow(NoSuchElementException::new);
        assertThat(changedLockRevisionTarget2.getOptLockRevision()).isEqualTo(3);
        assertThat(changedLockRevisionTarget2.getLastModifiedAt()).isPositive();

        // verify updated meta data contains the updated value
        assertThat(targetManagement.getMetadata(target.getControllerId())).containsEntry(knownKey, knownUpdateValue);
    }

    /**
     * Queries and loads the metadata related to a given target.
     */
    @Test
    void deleteMetadata() {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";

        // create target
        final String controllerId = createTargetWithMetadata("target1", 10).getControllerId();
        targetManagement.createMetadata(controllerId, Map.of(knownKey, knownValue));

        targetManagement.deleteMetadata(controllerId, knownKey);
        // already removed
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> targetManagement.deleteMetadata(controllerId, knownKey));
    }

    /**
     * Checks that target type for a target can be created, updated and unassigned.
     */
    @Test
    @WithUser(allSpPermissions = true)
    void createAndUpdateTargetTypeInTarget() {
        // create a target type
        final List<? extends TargetType> targetTypes = testdataFactory.createTargetTypes("targettype", 2);
        assertThat(targetTypes).hasSize(2);
        // create a target
        final Target target = testdataFactory.createTarget("target1", "testtarget", targetTypes.get(0));
        // initial opt lock revision must be one
        final Optional<JpaTarget> targetFound = targetRepository.findById(target.getId());
        assertThat(targetFound).isPresent();
        assertThat(targetFound.get().getOptLockRevision()).isEqualTo(1);
        assertThat(targetFound.get().getTargetType().getId()).isEqualTo(targetTypes.get(0).getId());

        // update the target type
        final Update targetUpdate = Update.builder().id(target.getId()).targetType(targetTypes.get(1)).build();
        targetManagement.update(targetUpdate);

        // opt lock revision must be changed
        final Optional<JpaTarget> targetFound1 = targetRepository.findById(target.getId());
        assertThat(targetFound1).isPresent();
        assertThat(targetFound1.get().getOptLockRevision()).isEqualTo(2);
        assertThat(targetFound1.get().getTargetType().getId()).isEqualTo(targetTypes.get(1).getId());

        // unassign the target type
        targetManagement.unassignType(target.getControllerId());

        // opt lock revision must be changed
        final Optional<JpaTarget> targetFound2 = targetRepository.findById(target.getId());
        assertThat(targetFound2).isPresent();
        assertThat(targetFound2.get().getOptLockRevision()).isEqualTo(3);
        assertThat(targetFound2.get().getTargetType()).isNull();
    }

    /**
     * Checks that target type to a target can be assigned.
     */
    @Test
    @WithUser(allSpPermissions = true)
    void assignTargetTypeInTarget() {
        // create a target
        final Target target = testdataFactory.createTarget("target1", "testtarget");
        // initial opt lock revision must be one
        final Optional<JpaTarget> targetFound = targetRepository.findById(target.getId());
        assertThat(targetFound).isPresent();
        assertThat(targetFound.get().getOptLockRevision()).isEqualTo(1);
        assertThat(targetFound.get().getTargetType()).isNull();

        // create a target type
        final TargetType targetType = testdataFactory.findOrCreateTargetType("targettype");
        assertThat(targetType).isNotNull();

        // assign target type to target
        targetManagement.assignType(targetFound.get().getControllerId(), targetType.getId());

        // opt lock revision must be changed
        final Optional<JpaTarget> targetFound1 = targetRepository.findById(target.getId());
        assertThat(targetFound1).isPresent();
        assertThat(targetFound1.get().getOptLockRevision()).isEqualTo(2);
        assertThat(targetFound1.get().getTargetType().getId()).isEqualTo(targetType.getId());
    }

    /**
     * Tests the assignment of types to multiple targets.
     */
    @Test
    @WithUser(allSpPermissions = true)
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 20),
            @Expect(type = TargetTypeCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 29),
            @Expect(type = TargetDeletedEvent.class, count = 1) })
    void targetTypeBulkAssignments() {
        final List<Target> typeATargets = testdataFactory.createTargets(10, "typeATargets", "first description");
        final List<Target> typeBTargets = testdataFactory.createTargets(10, "typeBTargets", "first description");

        // create a target type
        final TargetType typeA = testdataFactory.createTargetType("A", Set.of(standardDsType));
        final TargetType typeB = testdataFactory.createTargetType("B", Set.of(standardDsType));

        // assign target type to target
        TargetTypeAssignmentResult resultA = initiateTypeAssignment(typeATargets, typeA);
        TargetTypeAssignmentResult resultB = initiateTypeAssignment(typeBTargets, typeB);
        assertThat(resultA.getAssigned()).isEqualTo(10);
        assertThat(resultB.getAssigned()).isEqualTo(10);
        checkTargetsHaveType(typeATargets, typeA);
        checkTargetsHaveType(typeBTargets, typeB);

        // double assignment does not unassign
        resultA = initiateTypeAssignment(typeATargets, typeA);
        resultB = initiateTypeAssignment(typeBTargets, typeB);
        assertThat(resultA.getAssigned()).isZero();
        assertThat(resultB.getAssigned()).isZero();
        assertThat(resultA.getAlreadyAssigned()).isEqualTo(10);
        assertThat(resultB.getAlreadyAssigned()).isEqualTo(10);
        checkTargetsHaveType(typeATargets, typeA);
        checkTargetsHaveType(typeBTargets, typeB);

        // verify that type assignment does not throw an error if target list
        // includes an unknown id
        targetManagement.deleteByControllerID(typeATargets.get(0).getControllerId());
        final TargetTypeAssignmentResult resultC = initiateTypeAssignment(typeATargets, typeB);
        assertThat(resultC.getAssigned()).isEqualTo(9);
        assertThat(resultC.getAlreadyAssigned()).isZero();
        checkTargetsHaveType(typeATargets, typeB);
    }

    /**
     * Checks that target type is not assigned to target if invalid.
     */
    @Test
    @WithUser(allSpPermissions = true)
    void assignInvalidTargetTypeToTarget() {
        // create a target
        final Target target = testdataFactory.createTarget("target1", "testtarget");
        // initial opt lock revision must be one
        final Optional<JpaTarget> targetFound = targetRepository.findById(target.getId());
        assertThat(targetFound).isPresent();
        assertThat(targetFound.get().getOptLockRevision()).isEqualTo(1);
        assertThat(targetFound.get().getTargetType()).isNull();

        // assign target type to target
        final String controllerId = targetFound.get().getControllerId();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target type with id=null cannot be assigned")
                .isThrownBy(() -> targetManagement.assignType(controllerId, null));

        assertThatExceptionOfType(EntityNotFoundException.class)
                .as("target type with id that does not exists cannot be assigned")
                .isThrownBy(() -> targetManagement.assignType(controllerId, 114L));

        // opt lock revision is not changed
        final Optional<JpaTarget> targetFound1 = targetRepository.findById(target.getId());
        assertThat(targetFound1).isPresent();
        assertThat(targetFound1.get().getOptLockRevision()).isEqualTo(1);
    }

    /**
     * Checks that target type can be unassigned from target.
     */
    @Test
    @WithUser(allSpPermissions = true)
    void unAssignTargetTypeFromTarget() {
        // create a target type
        final TargetType targetType = testdataFactory.findOrCreateTargetType("targettype");
        assertThat(targetType).isNotNull();
        // create a target
        final Target target = testdataFactory.createTarget("target1", "testtarget", targetType);
        // initial opt lock revision must be one
        final Optional<JpaTarget> targetFound = targetRepository.findById(target.getId());
        assertThat(targetFound).isPresent();
        assertThat(targetFound.get().getOptLockRevision()).isEqualTo(1);
        assertThat(targetFound.get().getTargetType().getName()).isEqualTo(targetType.getName());

        // un-assign target type from target
        targetManagement.unassignType(targetFound.get().getControllerId());

        // opt lock revision must be changed
        final Optional<JpaTarget> targetFound1 = targetRepository.findById(target.getId());
        assertThat(targetFound1).isPresent();
        assertThat(targetFound1.get().getOptLockRevision()).isEqualTo(2);
        assertThat(targetFound1.get().getTargetType()).isNull();
    }

    /**
     * Test that RSQL filter finds targets with metadata and/or controllerId.
     */
    @Test
    void findTargetsByRsqlWithMetadata() {
        final String controllerId1 = "target1";
        final String controllerId2 = "target2";
        createTargetWithMetadata(controllerId1, 2);
        createTargetWithMetadata(controllerId2, 2);

        final String rsqlAndControllerIdFilter = "id==target1 and metadata.key1==target1-value1";
        final String rsqlAndControllerIdWithWrongKeyFilter = "id==* and metadata.unknown==value1";
        final String rsqlAndControllerIdNotEqualFilter = "id==* and metadata.key2!=target1-value2";
        final String rsqlOrControllerIdFilter = "id==target1 or metadata.key1==*value1";
        final String rsqlOrControllerIdWithWrongKeyFilter = "id==target2 or metadata.unknown==value1";
        final String rsqlOrControllerIdNotEqualFilter = "id==target1 or metadata.key1!=target1-value1";

        assertThat(targetManagement.count()).as("Total targets").isEqualTo(2);
        validateFoundTargetsByRsql(rsqlAndControllerIdFilter, controllerId1);
        validateFoundTargetsByRsql(rsqlAndControllerIdWithWrongKeyFilter);
        validateFoundTargetsByRsql(rsqlAndControllerIdNotEqualFilter, controllerId2);
        validateFoundTargetsByRsql(rsqlOrControllerIdFilter, controllerId1, controllerId2);
        validateFoundTargetsByRsql(rsqlOrControllerIdWithWrongKeyFilter, controllerId2);
        validateFoundTargetsByRsql(rsqlOrControllerIdNotEqualFilter, controllerId1, controllerId2);
    }

    /**
     * Test that RSQL filter finds targets with tag and metadata.
     */
    @Test
    void findTargetsByRsqlWithTypeAndMetadata() {
        if (RsqlUtility.getInstance().getRsqlToSpecBuilder() == LEGACY_G1) {
            // legacy visitor fail with that
            return;
        }
        final String controllerId1 = "target1";
        final String controllerId2 = "target2";
        createTargetWithMetadata(controllerId1, 2);
        final TargetType type = testdataFactory.createTargetType("type1", Set.of());
        createTargetWithTargetTypeAndMetadata(controllerId2, type, 2);

        assertThat(targetManagement.count()).as("Total targets").isEqualTo(2);

        final String rsqlAndByBoth = "targettype.key==type1 or metadata.key1==target1-value1";
        validateFoundTargetsByRsql(rsqlAndByBoth, controllerId1, controllerId2);

        final String rsqlAndControllerIdFilter = "targettype.key==type1 and metadata.key1==target1-value1";
        validateFoundTargetsByRsql(rsqlAndControllerIdFilter);
    }

    /**
     * Target matches filter.
     */
    @Test
    void matchesFilter() {
        final Target target = createTargetWithMetadata("target1", 2);
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final String filter = "metadata.key1==target1-value1";

        assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target.getControllerId(),
                ds.getId(), filter)).isTrue();
    }

    /**
     * Target does not matches filter.
     */
    @Test
    void matchesFilterWrongFilter() {
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final String filter = "metadata.key==not_existing";

        assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target.getControllerId(),
                ds.getId(), filter)).isFalse();
    }

    /**
     * Target matches filter but DS already assigned.
     */
    @Test
    void matchesFilterDsAssigned() {
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds1 = testdataFactory.createDistributionSet();
        final DistributionSet ds2 = testdataFactory.createDistributionSet();
        assignDistributionSet(ds1, target);
        assignDistributionSet(ds2, target);
        final String filter = "name==*";

        assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target.getControllerId(),
                ds1.getId(), filter)).isFalse();
    }

    /**
     * Target matches filter for DS with wrong type.
     */
    @Test
    void matchesFilterWrongType() {
        final TargetType type = testdataFactory.createTargetType("type", Set.of());
        final Target target = testdataFactory.createTarget("target", "target", type);
        final DistributionSet ds = testdataFactory.createDistributionSet();

        assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target.getControllerId(),
                ds.getId(), "name==*")).isFalse();
    }

    /**
     * Target matches filter that is invalid.
     */
    @Test
    void matchesFilterInvalidFilter() {
        final String target = testdataFactory.createTarget().getControllerId();
        final Long ds = testdataFactory.createDistributionSet().getId();

        assertThatExceptionOfType(RSQLParameterSyntaxException.class).isThrownBy(() -> targetManagement
                .isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target, ds, "invalid_syntax"));
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class).isThrownBy(() -> targetManagement
                .isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target, ds, "invalid_field==1"));
    }

    /**
     * Target matches filter for not existing target.
     */
    @Test
    void matchesFilterTargetNotExists() {
        final DistributionSet ds = testdataFactory.createDistributionSet();

        assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
                "notExisting", ds.getId(),"name==*")).isFalse();
    }

    /**
     * Tests action based aspects of the dynamic group assignment filters.
     */
    /**
     * Target matches filter no active action with ge weight.
     */
    @Test
    void findByTargetFilterQueryAndNoOverridingActionsAndNotInRolloutAndCompatibleAndUpdatable() {
        final String targetPrefix = "dyn_action_filter_";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(targetPrefix, 10);
        final Rollout rolloutOlder = testdataFactory.createRollout();
        final Rollout rollout = testdataFactory.createRollout();
        final Rollout rolloutNewer = testdataFactory.createRollout();

        int target = 0;
        final List<Integer> expected = new ArrayList<>();
        // old ro with less weight - match
        expected.add(target);
        createAction(targets.get(target++), rolloutOlder, 0, Status.RUNNING, distributionSet);
        // old ro with equal weight - match
        expected.add(target);
        createAction(targets.get(target++), rolloutOlder, 10, Status.RUNNING, distributionSet);
        // old ro with bigger weight, scheduled - match
        expected.add(target);
        createAction(targets.get(target++), rolloutOlder, 11, Status.SCHEDULED, distributionSet);
        // old ro with bigger weight, running - match
        expected.add(target);
        createAction(targets.get(target++), rolloutOlder, 11, Status.RUNNING, distributionSet);
        // old ro with bigger weight, running - match
        expected.add(target);
        createAction(targets.get(target++), rolloutOlder, 11, Status.FINISHED, distributionSet);
        // same ro - doesn't match
        createAction(targets.get(target++), rollout, 10, Status.RUNNING, distributionSet);
        // new ro with less weight - doesn't match
        createAction(targets.get(target++), rolloutNewer, 0, Status.RUNNING, distributionSet);
        // new ro with less weight - doesn't match
        createAction(targets.get(target++), rolloutNewer, 5, Status.WARNING, distributionSet);
        // NEW ro with EQUAL weight - doesn't match
        createAction(targets.get(target++), rolloutNewer, 10, Status.RUNNING, distributionSet);
        // new ro with BIGGER weight - doesn't match
        createAction(targets.get(target), rolloutNewer, 20, Status.DOWNLOADED, distributionSet);

        final Slice<Target> matching =
                targetManagement.findByRsqlAndNoOverridingActionsAndNotInRolloutAndCompatibleAndUpdatable(
                        rollout.getId(), "controllerid==dyn_action_filter_*", distributionSet.getType(), PAGE);

        assertThat(matching.getNumberOfElements()).isEqualTo(expected.size());
        assertThat(matching.stream()
                .map(Target::getControllerId)
                .map(s -> s.substring(targetPrefix.length()))
                .map(Integer::parseInt)
                .sorted()
                .toList()).isEqualTo(expected);
    }

    /**
     * Target matches filter for not existing DS.
     */
    @Test
    void matchesFilterDsNotExists() {
        final String target = testdataFactory.createTarget().getControllerId();

        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(
                () -> targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target, 123, "name==*"));
    }

    /**
     * Test update status convert
     */
    @Test
    void testUpdateStatusConvert() {
        final long id = testdataFactory.createTarget().getId();
        for (final TargetUpdateStatus status : TargetUpdateStatus.values()) {
            final JpaTarget target = targetRepository.findById(id).orElseThrow(() -> new IllegalStateException("Target not found"));
            target.setUpdateStatus(status);
            targetRepository.save(target);
            assertThat(targetRepository.findById(target.getId()).orElseThrow(() -> new IllegalStateException("Target not found"))
                    .getUpdateStatus()).isEqualTo(status);
        }
    }

    private void createAndUpdateTargetWithInvalidDescription(final Target target) {
        final Create targetCreateTooLong = Create.builder().controllerId("a").description(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long description should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreateTooLong));

        final Create targetCreateInvalidHtml = Create.builder().controllerId("a").description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid description should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreateInvalidHtml));

        final Update targetUpdateTooLong = Update.builder().id(target.getId()).description(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long description should not be updated")
                .isThrownBy(() -> targetManagement.update(targetUpdateTooLong));

        final Update targetUpdateInvalidHtml = Update.builder().id(target.getId()).description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid description should not be updated")
                .isThrownBy(() -> targetManagement.update(targetUpdateInvalidHtml));
    }

    private void createAndUpdateTargetWithInvalidName(final Target target) {
        final Create targetCreateTooLong = Create.builder().controllerId("a").name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long name should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreateTooLong));

        final Create targetCreateInvalidHtml = Create.builder().controllerId("a").name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid name should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreateInvalidHtml));

        final Update targetUpdateTooLong = Update.builder().id(target.getId()).name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long name should not be updated")
                .isThrownBy(() -> targetManagement.update(targetUpdateTooLong));

        final Update targetUpdateInvalidHtml = Update.builder().id(target.getId()).name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid name should not be updated")
                .isThrownBy(() -> targetManagement.update(targetUpdateInvalidHtml));

        final Update targetUpdateEmpty = Update.builder().id(target.getId()).name("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too short name should not be updated")
                .isThrownBy(() -> targetManagement.update(targetUpdateEmpty));
    }

    private void createAndUpdateTargetWithInvalidSecurityToken(final Target target) {
        final Create targetCreateTooLong = Create.builder()
                .controllerId("a").securityToken(randomString(Target.SECURITY_TOKEN_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long token should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreateTooLong));

        final Create targetCreateInvalidTextHtml = Create.builder().controllerId("a").securityToken(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid token should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreateInvalidTextHtml));

        final Update targetUpdateTooLong = Update.builder().id(target.getId())
                .securityToken(randomString(Target.SECURITY_TOKEN_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long token should not be updated")
                .isThrownBy(() -> targetManagement.update(targetUpdateTooLong));

        final Update targetUpdateInvalidHtml = Update.builder().id(target.getId()).securityToken(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid token should not be updated")
                .isThrownBy(() -> targetManagement.update(targetUpdateInvalidHtml));

        final Update targetUpdateEmpty = Update.builder().id(target.getId()).securityToken("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too short token should not be updated")
                .isThrownBy(() -> targetManagement.update(targetUpdateEmpty));
    }

    private void createAndUpdateTargetWithInvalidAddress(final Target target) {
        final Create targetCreate = Create.builder().controllerId("a").address(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long address should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreate));

        final Update targetUpdate = Update.builder().id(target.getId()).address(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long address should not be updated")
                .isThrownBy(() -> targetManagement.update(targetUpdate));
    }
    private void createTargetWithInvalidControllerId() {
        final Create targetCreateEmpty = Create.builder().controllerId("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with empty controller id should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreateEmpty));

        final Create targetCreateNull = Create.builder().controllerId(null).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with null controller id should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreateNull));

        final Create targetCreateTooLongControllerId = Create.builder()
                .controllerId(randomString(Target.CONTROLLER_ID_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long controller id should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreateTooLongControllerId));

        final Create targetCreateInvaidTextHtml = Create.builder().controllerId(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid controller id should not be created")
                .isThrownBy(() -> targetManagement.create(targetCreateInvaidTextHtml));

        final Create targetCreateEmptyTrim = Create.builder().controllerId(" ").build();
        assertThatExceptionOfType(ConstraintViolationException.class).as(WHITESPACE_ERROR)
                .isThrownBy(() -> targetManagement.create(targetCreateEmptyTrim));

        final Create targetCreateContainingSpace = Create.builder().controllerId("a b").build();
        assertThatExceptionOfType(ConstraintViolationException.class).as(WHITESPACE_ERROR)
                .isThrownBy(() -> targetManagement.create(targetCreateContainingSpace));

        final Create targetCreateEmptyTrim2 = Create.builder().controllerId("     ").build();
        assertThatExceptionOfType(ConstraintViolationException.class).as(WHITESPACE_ERROR)
                .isThrownBy(() -> targetManagement.create(targetCreateEmptyTrim2));

        final Create targetCreateContainingSpaces = Create.builder().controllerId("aaa   bbb").build();
        assertThatExceptionOfType(ConstraintViolationException.class).as(WHITESPACE_ERROR)
                .isThrownBy(() -> targetManagement.create(targetCreateContainingSpaces));
    }

    private Target createTargetWithAttributes(final String controllerId) {
        final Map<String, String> testData = new HashMap<>();
        testData.put("test1", "testdata1");

        targetManagement.create(Create.builder().controllerId(controllerId).build());
        final Target target = controllerManagement.updateControllerAttributes(controllerId, testData, null);

        assertThat(targetManagement.getControllerAttributes(controllerId)).as("Controller Attributes are wrong").isEqualTo(testData);
        return target;
    }

    /**
     * verifies, that all {@link TargetTag} of parameter. NOTE: it's accepted
     * that the target have additional tags assigned to them which are not
     * contained within parameter tags.
     *
     * @param strict if true, the given targets MUST contain EXACTLY ALL given
     *         tags, AND NO OTHERS. If false, the given targets MUST contain
     *         ALL given tags, BUT MAY CONTAIN FURTHER ONE
     * @param targets targets to be verified
     * @param tags are contained within tags of all targets.
     */
    private void checkTargetHasTags(final boolean strict, final Iterable<Target> targets, final TargetTag... tags) {
    _target:
        for (final Target tl : targets) {
            for (final Tag tt : getTargetTags(tl.getControllerId())) {
                for (final Tag tag : tags) {
                    if (tag.getName().equals(tt.getName())) {
                        continue _target;
                    }
                }
                if (strict) {
                    fail("Target does not contain all tags");
                }
            }
            fail("Target does not contain any tags or the expected tag was not found");
        }
    }

    private void checkTargetHasNotTags(final Iterable<Target> targets, final TargetTag... tags) {
        for (final Target tl : targets) {
            targetManagement.getByControllerId(tl.getControllerId()).get();

            for (final Tag tag : tags) {
                for (final Tag tt : getTargetTags(tl.getControllerId())) {
                    if (tag.getName().equals(tt.getName())) {
                        fail("Target should have no tags");
                    }
                }
            }
        }
    }

    private void insertMetadata(final String knownKey, final String knownValue, final Target target) {
        targetManagement.createMetadata(target.getControllerId(), Map.of(knownKey, knownValue));
        assertThat(targetManagement.getMetadata(target.getControllerId()).get(knownKey)).isEqualTo(knownValue);
    }

    private void checkTargetsHaveType(final List<Target> targets, final TargetType type) {
        final List<JpaTarget> foundTargets = targetRepository
                .findAllById(targets.stream().map(Identifiable::getId).toList());
        for (final Target target : foundTargets) {
            if (!type.getName().equals(type.getName())) {
                fail(String.format("Target %s is not of type %s.", target, type));
            }
        }
    }

    private Target createTargetWithMetadata(final String controllerId, final int count) {
        final Target target = testdataFactory.createTarget(controllerId);

        for (int index = 1; index <= count; index++) {
            insertMetadata("key" + index, controllerId + "-value" + index, target);
        }

        return target;
    }

    private Target createTargetWithTargetTypeAndMetadata(final String controllerId, final TargetType targetType, final int count) {
        final Target target = testdataFactory.createTarget(controllerId, controllerId, targetType);

        for (int index = 1; index <= count; index++) {
            insertMetadata("key" + index, controllerId + "-value" + index, target);
        }

        return target;
    }

    private void createAction(final Target target, final Rollout rollout, final Integer weight, final Action.Status status,
            final DistributionSet distributionSet) {
        final JpaAction action = new JpaAction();
        action.setActionType(Action.ActionType.FORCED);
        action.setTarget(target);
        action.setInitiatedBy("test");
        if (rollout != null) {
            action.setRollout(rollout);
        }
        if (weight != null) {
            action.setWeight(weight);
        }
        action.setStatus(status);
        action.setActive(status != Status.FINISHED && status != Status.ERROR && status != Status.CANCELED);
        action.setDistributionSet(distributionSet);
        actionRepository.save(action);
    }

    private void validateFoundTargetsByRsql(final String rsqlFilter, final String... controllerIds) {
        final Page<? extends Target> foundTargetsByMetadataAndControllerId = targetManagement.findByRsql(rsqlFilter, PAGE);
        final long foundTargetsByMetadataAndControllerIdCount = targetManagement.countByRsql(rsqlFilter);

        assertThat(foundTargetsByMetadataAndControllerId.getNumberOfElements())
                .as("Targets count in RSQL filter is wrong").isEqualTo(foundTargetsByMetadataAndControllerIdCount)
                .isEqualTo(controllerIds.length);
        assertThat(foundTargetsByMetadataAndControllerId.getContent().stream().map(Target::getControllerId))
                .as("Targets found by RSQL filter have wrong controller ids").containsExactlyInAnyOrder(controllerIds);
    }
}

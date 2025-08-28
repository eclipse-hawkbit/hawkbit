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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;

import org.assertj.core.api.Condition;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement.Create;
import org.eclipse.hawkbit.repository.DistributionSetManagement.Update;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link DistributionSetManagement} tests.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: DistributionSet Management
 */
class DistributionSetManagementTest extends AbstractRepositoryManagementWithMetadataTest<DistributionSet, Create, Update, String, String> {

    private static final String TAG1_NAME = "Tag1";

    @Autowired
    private RepositoryProperties repositoryProperties;

    /**
     * Verifies that management get access react as specified on calls for non existing entities by means of Optional not present.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    void nonExistingEntityAccessReturnsNotPresent() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        assertThat(distributionSetManagement.find(NOT_EXIST_IDL)).isNotPresent();
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> distributionSetManagement.getWithDetails(NOT_EXIST_IDL));
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> distributionSetManagement.findByNameAndVersion(NOT_EXIST_ID, NOT_EXIST_ID));
        assertThat(distributionSetManagement.getMetadata(set.getId()).get(NOT_EXIST_ID)).isNull();
    }

    /**
     * Verifies that management queries react as specified on calls for non existing entities by means of
     * throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 4) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        final DistributionSetTag dsTag = testdataFactory.createDistributionSetTags(1).get(0);
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp();

        verifyThrownExceptionBy(
                () -> distributionSetManagement.assignSoftwareModules(NOT_EXIST_IDL, singletonList(module.getId())), "DistributionSet");
        verifyThrownExceptionBy(
                () -> distributionSetManagement.assignSoftwareModules(set.getId(), singletonList(NOT_EXIST_IDL)), "SoftwareModule");

        verifyThrownExceptionBy(() -> distributionSetManagement.unassignSoftwareModule(NOT_EXIST_IDL, module.getId()), "DistributionSet");
        verifyThrownExceptionBy(() -> distributionSetManagement.unassignSoftwareModule(set.getId(), NOT_EXIST_IDL), "SoftwareModule");

        verifyThrownExceptionBy(() -> distributionSetManagement.assignTag(singletonList(set.getId()), NOT_EXIST_IDL), "DistributionSetTag");
        verifyThrownExceptionBy(() -> distributionSetManagement.assignTag(singletonList(NOT_EXIST_IDL), dsTag.getId()), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.findByTag(NOT_EXIST_IDL, PAGE), "DistributionSetTag");
        verifyThrownExceptionBy(() -> distributionSetManagement.findByRsqlAndTag("name==*", NOT_EXIST_IDL, PAGE), "DistributionSetTag");

        verifyThrownExceptionBy(() -> distributionSetManagement.assignTag(singletonList(NOT_EXIST_IDL), dsTag.getId()), "DistributionSet");
        verifyThrownExceptionBy(() ->
                distributionSetManagement.assignTag(singletonList(set.getId()), Long.parseLong(NOT_EXIST_ID)), "DistributionSetTag");

        verifyThrownExceptionBy(() -> distributionSetManagement.unassignTag(singletonList(set.getId()), NOT_EXIST_IDL), "DistributionSetTag");

        verifyThrownExceptionBy(() -> distributionSetManagement.unassignTag(singletonList(NOT_EXIST_IDL), dsTag.getId()), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.createMetadata(NOT_EXIST_IDL, Map.of("123", "123")), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.delete(singletonList(NOT_EXIST_IDL)), "DistributionSet");
        verifyThrownExceptionBy(() -> distributionSetManagement.delete(NOT_EXIST_IDL), "DistributionSet");
        verifyThrownExceptionBy(() -> distributionSetManagement.deleteMetadata(NOT_EXIST_IDL, "xxx"), "DistributionSet");
        verifyThrownExceptionBy(() -> distributionSetManagement.deleteMetadata(set.getId(), NOT_EXIST_ID), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.getMetadata(NOT_EXIST_IDL).get("xxx"), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.getMetadata(NOT_EXIST_IDL), "DistributionSet");

        verifyThrownExceptionBy(
                () -> distributionSetManagement.update(Update.builder().id(NOT_EXIST_IDL).build()),
                "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.createMetadata(NOT_EXIST_IDL, "xxx", "xxx"), "DistributionSet");
        verifyThrownExceptionBy(() -> distributionSetManagement.get(NOT_EXIST_IDL), "DistributionSet");
        verifyThrownExceptionBy(() -> distributionSetManagement.getValidAndComplete(NOT_EXIST_IDL), "DistributionSet");
    }

    /**
     * Verify that a DistributionSet with invalid properties cannot be created or updated
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class) })
    void createAndUpdateDistributionSetWithInvalidFields() {
        final DistributionSet set = testdataFactory.createDistributionSet();

        createAndUpdateDistributionSetWithInvalidDescription(set);
        createAndUpdateDistributionSetWithInvalidName(set);
        createAndUpdateDistributionSetWithInvalidVersion(set);
    }

    /**
     * Ensures that it is not possible to create a DS that already exists (unique constraint is on name,version for DS).
     */
    @Test
    void createDuplicateDistributionSetsFailsWithException() {
        testdataFactory.createDistributionSet("a");

        assertThatThrownBy(() -> testdataFactory.createDistributionSet("a"))
                .isInstanceOf(EntityAlreadyExistsException.class);
    }

    /**
     * Verifies that a DS is of default type if not specified explicitly at creation time.
     */
    @Test
    void createDistributionSetWithImplicitType() {
        final DistributionSet set = distributionSetManagement
                .create(Create.builder().type(defaultDsType()).name("newtypesoft").version("1").build());

        assertThat(set.getType())
                .as("Type should be equal to default type of tenant")
                .isEqualTo(systemManagement.getTenantMetadata().getDefaultDsType());

    }

    /**
     * Verifies that a DS cannot be created if another DS with same name and version exists.
     */
    @Test
    void createDistributionSetWithDuplicateNameAndVersionFails() {
        final Create distributionSetCreate =
                Create.builder().type(defaultDsType()).name("newtypesoft").version("1").build();
        distributionSetManagement.create(distributionSetCreate);

        assertThatExceptionOfType(EntityAlreadyExistsException.class).isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));
    }

    /**
     * Verifies that multiple DS are of default type if not specified explicitly at creation time.
     */
    @Test
    void createMultipleDistributionSetsWithImplicitType() {
        final List<Create> creates = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            creates.add(Create.builder().type(defaultDsType()).name("newtypesoft" + i).version("1" + i).build());
        }

        assertThat(distributionSetManagement.create(creates))
                .as("Type should be equal to default type of tenant")
                .are(new Condition<DistributionSet>() {

                    @Override
                    public boolean matches(final DistributionSet value) {
                        return value.getType().equals(systemManagement.getTenantMetadata().getDefaultDsType());
                    }
                });
    }

    /**
     * Ensures that distribution sets can assigned and unassigned to a  distribution set tag.
     */
    @Test
    void assignAndUnassignDistributionSetToTag() {
        final List<Long> assignDS = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            assignDS.add(testdataFactory.createDistributionSet("DS" + i, "1.0", Collections.emptyList()).getId());
        }

        final DistributionSetTag tag = distributionSetTagManagement.create(
                DistributionSetTagManagement.Create.builder().name(TAG1_NAME).build());

        final List<? extends DistributionSet> assignedDS = distributionSetManagement.assignTag(assignDS, tag.getId());
        assertThat(assignedDS).as("assigned ds has wrong size").hasSize(4);
        assignedDS.stream().map(JpaDistributionSet.class::cast).forEach(ds -> assertThat(ds.getTags())
                .as("ds has wrong tag size")
                .hasSize(1));

        final DistributionSetTag findDistributionSetTag = distributionSetTagManagement.get(tag.getId());

        assertThat(assignedDS)
                .as("assigned ds has wrong size")
                .hasSize(distributionSetManagement.findByTag(tag.getId(), PAGE).getNumberOfElements());

        final JpaDistributionSet unAssignDS = (JpaDistributionSet) distributionSetManagement
                .unassignTag(List.of(assignDS.get(0)), findDistributionSetTag.getId()).get(0);
        assertThat(unAssignDS.getId()).as("unassigned ds is wrong").isEqualTo(assignDS.get(0));
        assertThat(unAssignDS.getTags()).as("unassigned ds has wrong tag size").isEmpty();
        assertThat(distributionSetTagManagement.find(tag.getId())).isPresent();
        assertThat(distributionSetManagement.findByTag(tag.getId(), PAGE).getNumberOfElements())
                .as("ds tag ds has wrong ds size").isEqualTo(3);

        assertThat(distributionSetManagement.findByRsqlAndTag("name==" + unAssignDS.getName(), tag.getId(), PAGE)
                .getNumberOfElements()).as("ds tag ds has wrong ds size").isZero();
        assertThat(distributionSetManagement.findByRsqlAndTag("name!=" + unAssignDS.getName(), tag.getId(), PAGE)
                .getNumberOfElements()).as("ds tag ds has wrong ds size").isEqualTo(3);
    }

    /**
     * Ensures that updates concerning the internal software structure of a DS are not possible if the DS is already assigned.
     */
    @Test
    void updateDistributionSetForbiddenWithIllegalUpdate() {
        // prepare data
        final Target target = testdataFactory.createTarget();

        DistributionSet ds = testdataFactory.createDistributionSet("ds-1");

        final SoftwareModule ah2 = testdataFactory.createSoftwareModuleApp();
        final Set<Long> os2Id = Set.of(testdataFactory.createSoftwareModuleOs().getId());

        // update is allowed as it is still not assigned to a target
        ds = distributionSetManagement.assignSoftwareModules(ds.getId(), Set.of(ah2.getId()));

        // assign target
        assignDistributionSet(ds.getId(), target.getControllerId());
        ds = distributionSetManagement.getWithDetails(ds.getId());

        final Long dsId = ds.getId();
        // not allowed as it is assigned now
        assertThatThrownBy(() -> distributionSetManagement.assignSoftwareModules(dsId, os2Id))
                .isInstanceOf(EntityReadOnlyException.class);

        // not allowed as it is assigned now
        final Long appId = findFirstModuleByType(ds, appType).map(Identifiable::getId).orElseThrow();
        assertThatThrownBy(() -> distributionSetManagement.unassignSoftwareModule(dsId, appId))
                .isInstanceOf(EntityReadOnlyException.class);
    }

    /**
     * Ensures that it is not possible to add a software module that is not defined of the DS's type.
     */
    @Test
    void updateDistributionSetUnsupportedModuleFails() {
        final Long setId = distributionSetManagement.create(
                Create.builder()
                        .type(distributionSetTypeManagement.create(
                                DistributionSetTypeManagement.Create.builder()
                                        .key("test")
                                        .name("test")
                                        .mandatoryModuleTypes(Set.of(osType))
                                        .build()))
                        .name("agent-hub2")
                        .version("1.0.5")
                        .build()).getId();

        final Set<Long> moduleId = Set.of(softwareModuleManagement.create(
                SoftwareModuleManagement.Create.builder()
                        .type(appType)
                        .name("agent-hub2").version("1.0.5")
                        .build()).getId());

        // update data
        assertThatThrownBy(() -> distributionSetManagement.assignSoftwareModules(setId, moduleId))
                .isInstanceOf(UnsupportedSoftwareModuleForThisDistributionSetException.class);
    }

    /**
     * Legal updates of a DS, e.g. name or description and module addition, removal while still unassigned.
     */
    @Test
    void assignAndUnassignSm() {
        // prepare data
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        // legal update of module - addition
        distributionSetManagement.assignSoftwareModules(ds.getId(), Set.of(os.getId()));
        assertThat(findFirstModuleByType(distributionSetManagement.get(ds.getId()), osType)).hasValue(os);

        // legal update of module - removal
        distributionSetManagement.unassignSoftwareModule(ds.getId(), findFirstModuleByType(ds, appType).map(Identifiable::getId).orElseThrow());
        assertThat(findFirstModuleByType(distributionSetManagement.get(ds.getId()), appType)).isNotPresent();
    }

    /**
     * Verifies that an exception is thrown when trying to update an invalid distribution set
     */
    @Test
    void failToUpdateInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        final Update update =
                Update.builder().id(distributionSet.getId()).name("new_name").build();
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception").isThrownBy(() -> distributionSetManagement.update(update));
    }

    @Test
    void failToModifyMetadataForInvalidDistributionSet() {
        final String key = forType(String.class);

        final Long instanceId = instance().getId();
        distributionSetManagement.createMetadata(instanceId, Map.of(key, forType(String.class)));

        distributionSetManagement.invalidate(distributionSetManagement.get(instanceId));

        // assert that no new metadata can be created
        final String key2 = forType(String.class);
        final String createValue = forType(String.class);
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Create metadata of an invalid entity should throw an exception")
                .isThrownBy(() -> distributionSetManagement.createMetadata(instanceId, key2, createValue));
        final Map<String, String> createMetadata = Map.of(key2, createValue);
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Create metadata of an invalid entity should throw an exception")
                .isThrownBy(() -> distributionSetManagement.createMetadata(instanceId, createMetadata));

        // assert that an existing metadata can not be updated
        final String updateValue = forType(String.class);
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Update metadata of an invalid entity should throw an exception")
                .isThrownBy(() -> distributionSetManagement.createMetadata(instanceId, key, updateValue));
        final Map<String, String> updateMetadata = Map.of(key, updateValue);
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Update metadata of an invalid entity should throw an exception")
                .isThrownBy(() -> distributionSetManagement.createMetadata(instanceId, updateMetadata));
    }

    /**
     * Verifies the enforcement of the software module quota per distribution set.
     */
    @Test
    void assignSoftwareModulesUntilQuotaIsExceeded() {

        // create some software modules
        final int maxModules = quotaManagement.getMaxSoftwareModulesPerDistributionSet();
        final List<Long> modules = new ArrayList<>();
        for (int i = 0; i < maxModules + 1; ++i) {
            modules.add(testdataFactory.createSoftwareModuleApp("sm" + i).getId());
        }

        // assign software modules one by one
        final DistributionSet ds1 = testdataFactory.createDistributionSetWithNoSoftwareModules("ds1", "1.0");
        final Long ds1Id = ds1.getId();
        for (int i = 0; i < maxModules; ++i) {
            distributionSetManagement.assignSoftwareModules(ds1Id, singletonList(modules.get(i)));
        }
        // add one more to cause the quota to be exceeded
        final List<Long> maxModulsList = singletonList(modules.get(maxModules));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetManagement.assignSoftwareModules(ds1Id, maxModulsList));

        // assign all software modules at once
        final DistributionSet ds2 = testdataFactory.createDistributionSetWithNoSoftwareModules("ds2", "1.0");
        // verify quota is exceeded
        final Long ds2Id = ds2.getId();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetManagement.assignSoftwareModules(ds2Id, modules));

        // assign some software modules
        final DistributionSet ds3 = testdataFactory.createDistributionSetWithNoSoftwareModules("ds3", "1.0");
        final int firstHalf = Math.round((maxModules) / 2.f);
        final Long ds3Id = ds3.getId();
        for (int i = 0; i < firstHalf; ++i) {
            distributionSetManagement.assignSoftwareModules(ds3Id, singletonList(modules.get(i)));
        }
        // assign the remaining modules to cause the quota to be exceeded
        final List<Long> firstHalfAndModules = modules.subList(firstHalf, modules.size());
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetManagement.assignSoftwareModules(ds3Id, firstHalfAndModules));

    }

    /**
     * Verifies that an exception is thrown when trying to assign software modules to an invalidated distribution set.
     */
    @Test
    void verifyAssignSoftwareModulesToInvalidDistributionSet() {
        final Long distributionSetId = testdataFactory.createAndInvalidateDistributionSet().getId();
        final List<Long> softwareModuleIds = List.of(testdataFactory.createSoftwareModuleOs().getId());

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetManagement.assignSoftwareModules(distributionSetId, softwareModuleIds));
    }

    /**
     * Verifies that an exception is thrown when trying to unassign a software module from an invalidated distribution set.
     */
    @Test
    void verifyUnassignSoftwareModulesToInvalidDistributionSet() {
        final Long distributionSetId = testdataFactory.createDistributionSet().getId();
        final Long softwareModuleId = testdataFactory.createSoftwareModuleOs().getId();
        distributionSetManagement.assignSoftwareModules(distributionSetId, singletonList(softwareModuleId));
        distributionSetInvalidationManagement.invalidateDistributionSet(
                new DistributionSetInvalidation(singletonList(distributionSetId), ActionCancellationType.NONE));

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception").isThrownBy(() -> distributionSetManagement
                        .unassignSoftwareModule(distributionSetId, softwareModuleId));
    }

    /**
     * Locks a DS.
     */
    @Test
    void lockDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        assertThat(distributionSetManagement.find(distributionSet.getId()).map(DistributionSet::isLocked).orElse(true)).isFalse();
        distributionSetManagement.lock(distributionSet);
        assertThat(distributionSetManagement.find(distributionSet.getId()).map(DistributionSet::isLocked).orElse(false)).isTrue();
        // assert software modules are locked
        assertThat(distributionSet.getModules().size()).isNotZero();
        distributionSetManagement.getWithDetails(distributionSet.getId()).getModules()
                .forEach(module -> assertThat(module.isLocked()).isTrue());
    }

    /**
     * Locked a DS could be hard deleted.
     */
    @Test
    void deleteUnassignedLockedDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        distributionSetManagement.lock(distributionSet);
        assertThat(distributionSetManagement.find(distributionSet.getId()).map(DistributionSet::isLocked).orElse(false)).isTrue();

        distributionSetManagement.delete(distributionSet.getId());
        assertThat(distributionSetManagement.find(distributionSet.getId())).isEmpty();
    }

    /**
     * Locked an assigned DS could be soft deleted.
     */
    @Test
    void deleteAssignedLockedDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        distributionSetManagement.lock(distributionSet);
        assertThat(distributionSetManagement.find(distributionSet.getId()).map(DistributionSet::isLocked).orElse(false)).isTrue();

        final Target target = testdataFactory.createTarget();
        assignDistributionSet(distributionSet.getId(), target.getControllerId());

        distributionSetManagement.delete(distributionSet.getId());
        assertThat(distributionSetManagement.get(distributionSet.getId()).isDeleted()).isTrue();
    }

    /**
     * Unlocks a DS.
     */
    @Test
    void unlockDistributionSet() {
        DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        distributionSet = distributionSetManagement.lock(distributionSet);
        assertThat(distributionSetManagement.find(distributionSet.getId()).map(DistributionSet::isLocked).orElse(false)).isTrue();
        distributionSet = distributionSetManagement.unlock(distributionSet);
        assertThat(distributionSetManagement.find(distributionSet.getId()).map(DistributionSet::isLocked).orElse(true)).isFalse();
        // assert software modules are not unlocked
        assertThat(distributionSet.getModules().size()).isNotZero();
        distributionSetManagement.getWithDetails(distributionSet.getId()).getModules()
                .forEach(module -> assertThat(module.isLocked()).isTrue());
    }

    /**
     * Software modules of a locked DS can't be modified. Expected behaviour is to throw an exception and to do not modify them.
     */
    @Test
    void lockDistributionSetApplied() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        final int softwareModuleCount = distributionSet.getModules().size();
        assertThat(softwareModuleCount).isNotZero();
        distributionSetManagement.lock(distributionSet);
        final Long distributionSetId = distributionSet.getId();
        assertThat(distributionSetManagement.find(distributionSetId).map(DistributionSet::isLocked).orElse(false)).isTrue();

        // try add
        final List<Long> moduleIds = List.of(testdataFactory.createSoftwareModule("sm-1").getId());
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked DS software modules should throw an exception")
                .isThrownBy(() -> distributionSetManagement.assignSoftwareModules(distributionSetId, moduleIds));
        assertThat(distributionSetManagement.getWithDetails(distributionSetId).getModules())
                .as("Software module shall not be added to a locked DS.")
                .hasSize(softwareModuleCount);

        // try remove
        final Long firstModuleId = distributionSet.getModules().stream().findFirst().orElseThrow().getId();
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked DS software modules should throw an exception")
                .isThrownBy(() -> distributionSetManagement.unassignSoftwareModule(distributionSetId, firstModuleId));
        assertThat(distributionSetManagement.getWithDetails(distributionSetId).getModules())
                .as("Software module shall not be removed from a locked DS.")
                .hasSize(softwareModuleCount);
    }

    /**
     * Test implicit locks for a DS and skip tags.
     */
    @Test
    void shouldLockImplicitlyForDistributionSet() {
        final JpaDistributionSetManagement distributionSetManagement = (JpaDistributionSetManagement) this.distributionSetManagement;
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-non-skip");
        // assert that implicit lock is applicable for non skip tags
        assertThat(distributionSetManagement.shouldLockImplicitly(distributionSet)).isTrue();

        assertThat(repositoryProperties.getSkipImplicitLockForTags().size()).isNotZero();
        final List<? extends DistributionSetTag> skipTags = distributionSetTagManagement.create(
                repositoryProperties.getSkipImplicitLockForTags().stream()
                        .map(String::toLowerCase)
                        // remove same in case-insensitive terms tags
                        // in of case-insensitive db's it will end up as same names and constraint violation (?)
                        .distinct()
                        .map(skipTag -> (DistributionSetTagManagement.Create) DistributionSetTagManagement.Create.builder().name(skipTag)
                                .build())
                        .toList());
        // assert that implicit lock locks for every skip tag
        skipTags.forEach(skipTag -> {
            DistributionSet distributionSetWithSkipTag = testdataFactory.createDistributionSet("ds-skip-" + skipTag.getName());
            distributionSetManagement.assignTag(List.of(distributionSetWithSkipTag.getId()), skipTag.getId());
            distributionSetWithSkipTag = distributionSetManagement.find(distributionSetWithSkipTag.getId()).orElseThrow();
            // assert that implicit lock isn't applicable for skip tags
            assertThat(distributionSetManagement.shouldLockImplicitly(distributionSetWithSkipTag)).isFalse();
        });
    }

    /**
     * Locks an incomplete DS. Expected behaviour is to throw an exception and to do not lock it.
     */
    @Test
    void lockIncompleteDistributionSetFails() {
        final DistributionSet incompleteDistributionSet = testdataFactory.createIncompleteDistributionSet();
        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Locking an incomplete distribution set should throw an exception")
                .isThrownBy(() -> distributionSetManagement.lock(incompleteDistributionSet));
        assertThat(
                distributionSetManagement.find(incompleteDistributionSet.getId()).map(DistributionSet::isLocked).orElse(true))
                .isFalse();
    }

    /**
     * Deletes an invalid distribution set
     */
    @Test
    void deleteInvalidDistributionSet() {
        final DistributionSet set = testdataFactory.createAndInvalidateDistributionSet();
        assertThat(distributionSetRepository.findById(set.getId())).isNotEmpty();
        distributionSetManagement.delete(set.getId());
        assertThat(distributionSetRepository.findById(set.getId())).isEmpty();
    }

    /**
     * Deletes an incomplete distribution set
     */
    @Test
    void deleteIncompleteDistributionSet() {
        final DistributionSet set = testdataFactory.createIncompleteDistributionSet();
        assertThat(distributionSetRepository.findById(set.getId())).isNotEmpty();
        distributionSetManagement.delete(set.getId());
        assertThat(distributionSetRepository.findById(set.getId())).isEmpty();
    }

    /**
     * Deletes a DS that is in use by either target assignment or rollout. Expected behaviour is a soft delete on the database, i.e. only marked as
     * deleted, kept as reference but unavailable for future use..
     */
    @Test
    void deleteAssignedDistributionSet() {
        testdataFactory.createDistributionSet("ds-1");
        testdataFactory.createDistributionSet("ds-2");
        final DistributionSet dsToTargetAssigned = testdataFactory.createDistributionSet("ds-3");
        final DistributionSet dsToRolloutAssigned = testdataFactory.createDistributionSet("ds-4");

        // create assigned DS
        final Target savedTarget = testdataFactory.createTarget();
        assignDistributionSet(dsToTargetAssigned.getId(), savedTarget.getControllerId());

        // create assigned rollout
        testdataFactory.createRolloutByVariables("test", "test", 5, "name==*", dsToRolloutAssigned, "50", "5");

        // delete assigned ds
        assertThat(distributionSetRepository.findAll()).hasSize(4);
        distributionSetManagement.delete(Arrays.asList(dsToTargetAssigned.getId(), dsToRolloutAssigned.getId()));

        // not assigned so not marked as deleted
        assertThat(distributionSetRepository.findAll()).hasSize(4);
        assertThat(distributionSetManagement.findAll(PAGE)).hasSize(2);
        assertThat(distributionSetManagement.findByRsql("name==*", PAGE)).hasSize(2);
        assertThat(distributionSetManagement.count()).isEqualTo(2);
    }

    /**
     * Verify that the find all by ids contains the entities which are looking for
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 12),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 36) })
    void verifyFindDistributionSetAllById() {
        final List<Long> searchIds = new ArrayList<>();
        searchIds.add(testdataFactory.createDistributionSet("ds-4").getId());
        searchIds.add(testdataFactory.createDistributionSet("ds-5").getId());
        searchIds.add(testdataFactory.createDistributionSet("ds-6").getId());
        for (int i = 0; i < 9; i++) {
            testdataFactory.createDistributionSet("test" + i);
        }

        final List<? extends DistributionSet> foundDs = distributionSetManagement.get(searchIds);

        assertThat(foundDs).hasSize(3);

        final List<Long> collect = foundDs.stream().map(DistributionSet::getId).toList();
        assertThat(collect).containsAll(searchIds);
    }

    /**
     * Verify that an exception is thrown when trying to get an invalid distribution set
     */
    @Test
    void verifyGetValid() {
        final Long distributionSetId = testdataFactory.createAndInvalidateDistributionSet().getId();
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetManagement.getValidAndComplete(distributionSetId));
    }

    /**
     * Verify that an exception is thrown when trying to get an incomplete distribution set
     */
    @Test
    void verifyGetValidAndComplete() {
        final Long distributionSetId = testdataFactory.createIncompleteDistributionSet().getId();
        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetManagement.getValidAndComplete(distributionSetId));
    }

    /**
     * Get the Rollouts count by status statistics for a specific Distribution Set
     */
    @Test
    void getRolloutsCountStatisticsForDistributionSet() {
        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");
        DistributionSet ds3 = testdataFactory.createDistributionSet("DS3");
        testdataFactory.createTargets("targets", 4);
        Rollout rollout1 = testdataFactory.createRolloutByVariables("rollout1", "description",
                1, "name==targets*", ds1, "50", "5", false);
        Rollout rollout2 = testdataFactory.createRolloutByVariables("rollout2", "description",
                1, "name==targets*", ds2, "50", "5", false);

        rolloutManagement.start(rollout2.getId());

        assertThat(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds1.getId())).hasSize(1);
        assertThat(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds2.getId())).hasSize(1);
        assertThat(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds3.getId())).isEmpty();

        Rollout rollout = rolloutManagement.get(rollout1.getId());
        assertThat(Rollout.RolloutStatus.valueOf(
                String.valueOf(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds1.getId()).get(0).getName()))).isEqualTo(
                rollout.getStatus());

        rollout = rolloutManagement.get(rollout2.getId());
        assertThat(Rollout.RolloutStatus.valueOf(
                String.valueOf(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds2.getId()).get(0).getName()))).isEqualTo(
                rollout.getStatus());
    }

    /**
     * Get the Rollouts count by status statistics for a specific Distribution Set
     */
    @Test
    void getActionsCountStatisticsForDistributionSet() {
        final DistributionSet ds = testdataFactory.createDistributionSet("DS");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");
        testdataFactory.createTargets("targets", 4);
        final Rollout rollout = testdataFactory.createRolloutByVariables("rollout", "description", 1, "name==targets*", ds, "50", "5", false);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<Statistic> statistics = distributionSetManagement.countActionsByStatusForDistributionSet(ds.getId());

        assertThat(statistics).hasSize(1);
        assertThat(distributionSetManagement.countActionsByStatusForDistributionSet(ds2.getId())).isEmpty();

        statistics.forEach(statistic -> assertThat(Status.valueOf(String.valueOf(statistic.getName()))).isEqualTo(Status.RUNNING));
    }

    /**
     * Get the Rollouts count by status statistics for a specific Distribution Set
     */
    @Test
    void getAutoAssignmentsCountStatisticsForDistributionSet() {
        DistributionSet ds = testdataFactory.createDistributionSet("DS");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");
        testdataFactory.createTargets("targets", 4);
        targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder()
                        .name("test filter 1").autoAssignDistributionSet(ds).query("name==targets*")
                        .build());

        targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder()
                        .name("test filter 2").autoAssignDistributionSet(ds).query("name==targets*")
                        .build());

        assertThat(distributionSetManagement.countAutoAssignmentsForDistributionSet(ds.getId())).isEqualTo(2);
        assertThat(distributionSetManagement.countAutoAssignmentsForDistributionSet(ds2.getId())).isNull();
    }

    private void createAndUpdateDistributionSetWithInvalidDescription(final DistributionSet set) {
        final Create distributionSetCreate =
                Create.builder().name("a").version("a").description(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));

        final Create distributionSetCreate2 =
                Create.builder().name("a").version("a").description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate2));

        final Update distributionSetUpdate =
                Update.builder().id(set.getId()).description(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long description should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate));

        final Update distributionSetUpdate2 =
                Update.builder().id(set.getId()).description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate2));
    }

    private void createAndUpdateDistributionSetWithInvalidName(final DistributionSet set) {
        final Create distributionSetCreate = Create.builder()
                .version("a").name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));

        final Create distributionSetCreate2 = Create.builder().version("a").name("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate2));

        final Create distributionSetCreate3 = Create.builder().version("a").name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters in name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate3));

        final Update distributionSetUpdate = Update.builder().id(set.getId())
                .name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate));

        final Update distributionSetUpdate2 = Update.builder().id(set.getId()).name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate2));

        final Update distributionSetUpdate3 = Update.builder().id(set.getId()).name("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate3));
    }

    private void createAndUpdateDistributionSetWithInvalidVersion(final DistributionSet set) {
        final Create distributionSetCreate = Create.builder()
                .name("a").version(randomString(NamedVersionedEntity.VERSION_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));

        final Create distributionSetCreate2 = Create.builder().name("a").version("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate2));

        final Update distributionSetUpdate = Update.builder().id(set.getId())
                .version(randomString(NamedVersionedEntity.VERSION_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate));

        final Update distributionSetUpdate2 = Update.builder().id(set.getId()).version("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate2));
    }
}
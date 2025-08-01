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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolationException;

import org.assertj.core.api.Condition;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
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
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link DistributionSetManagement} tests.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: DistributionSet Management
 */
class DistributionSetManagementTest extends AbstractJpaIntegrationTest {

    private static final String TAG1_NAME = "Tag1";

    @Autowired
    RepositoryProperties repositoryProperties;

    /**
     * Verifies that management get access react as specified on calls for non existing entities by means of Optional not present.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    void nonExistingEntityAccessReturnsNotPresent() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        assertThat(distributionSetManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(distributionSetManagement.getWithDetails(NOT_EXIST_IDL)).isNotPresent();
        assertThat(distributionSetManagement.findByNameAndVersion(NOT_EXIST_ID, NOT_EXIST_ID)).isNotPresent();
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

        verifyThrownExceptionBy(() -> distributionSetManagement.countByTypeId(NOT_EXIST_IDL), "DistributionSet");

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

        verifyThrownExceptionBy(() -> distributionSetManagement.findByAction(NOT_EXIST_IDL), "Action");

        verifyThrownExceptionBy(() -> distributionSetManagement.getMetadata(NOT_EXIST_IDL).get("xxx"), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.getMetadata(NOT_EXIST_IDL).get(PAGE), "DistributionSet");

        assertThatThrownBy(() -> distributionSetManagement.isInUse(NOT_EXIST_IDL))
                .isInstanceOf(EntityNotFoundException.class).hasMessageContaining(NOT_EXIST_ID)
                .hasMessageContaining("DistributionSet");

        verifyThrownExceptionBy(
                () -> distributionSetManagement.update(DistributionSetManagement.Update.builder().id(NOT_EXIST_IDL).build()),
                "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.createMetadata(NOT_EXIST_IDL, "xxx", "xxx"), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.getOrElseThrowException(NOT_EXIST_IDL), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.getValidAndComplete(NOT_EXIST_IDL), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.getValid(NOT_EXIST_IDL), "DistributionSet");
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
                .create(DistributionSetManagement.Create.builder().type(defaultDsType()).name("newtypesoft").version("1").build());

        assertThat(set.getType())
                .as("Type should be equal to default type of tenant")
                .isEqualTo(systemManagement.getTenantMetadata().getDefaultDsType());

    }

    /**
     * Verifies that a DS cannot be created if another DS with same name and version exists.
     */
    @Test
    void createDistributionSetWithDuplicateNameAndVersionFails() {
        final DistributionSetManagement.Create distributionSetCreate =
                DistributionSetManagement.Create.builder().type(defaultDsType()).name("newtypesoft").version("1").build();
        distributionSetManagement.create(distributionSetCreate);

        assertThatExceptionOfType(EntityAlreadyExistsException.class).isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));
    }

    /**
     * Verifies that multiple DS are of default type if not specified explicitly at creation time.
     */
    @Test
    void createMultipleDistributionSetsWithImplicitType() {
        final List<DistributionSetManagement.Create> creates = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            creates.add(DistributionSetManagement.Create.builder().type(defaultDsType()).name("newtypesoft" + i).version("1" + i).build());
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
     * Verifies the enforcement of the metadata quota per distribution set.
     */
    @Test
    void createMetadataUntilQuotaIsExceeded() {

        // add meta data one by one
        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerDistributionSet();
        for (int i = 0; i < maxMetaData; ++i) {
            insertMetadata("k" + i, "v" + i, ds1);
        }

        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> insertMetadata("k" + maxMetaData, "v" + maxMetaData, ds1));

        // add multiple meta data entries at once
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");
        final Map<String, String> metaData2 = new HashMap<>();
        for (int i = 0; i < maxMetaData + 1; ++i) {
            metaData2.put("k" + i, "v" + i);
        }
        // verify quota is exceeded
        final Long ds2Id = ds2.getId();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetManagement.createMetadata(ds2Id, metaData2));

        // add some meta data entries
        final DistributionSet ds3 = testdataFactory.createDistributionSet("ds3");
        final int firstHalf = Math.round((maxMetaData) / 2.f);
        final Long ds3Id = ds3.getId();
        for (int i = 0; i < firstHalf; ++i) {
            insertMetadata("k" + i, "v" + i, ds3);
        }
        // add too many data entries
        final int secondHalf = maxMetaData - firstHalf;
        final Map<String, String> metaData3 = new HashMap<>();
        for (int i = 0; i < secondHalf + 1; ++i) {
            metaData3.put("kk" + i, "vv" + i);
        }
        // verify quota is exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetManagement.createMetadata(ds3Id, metaData3));
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

        final DistributionSetTag findDistributionSetTag = getOrThrow(distributionSetTagManagement.findByName(TAG1_NAME));

        assertThat(assignedDS)
                .as("assigned ds has wrong size")
                .hasSize(distributionSetManagement.findByTag(tag.getId(), PAGE).getNumberOfElements());

        final JpaDistributionSet unAssignDS = (JpaDistributionSet) distributionSetManagement
                .unassignTag(List.of(assignDS.get(0)), findDistributionSetTag.getId()).get(0);
        assertThat(unAssignDS.getId()).as("unassigned ds is wrong").isEqualTo(assignDS.get(0));
        assertThat(unAssignDS.getTags()).as("unassigned ds has wrong tag size").isEmpty();
        assertThat(distributionSetTagManagement.findByName(TAG1_NAME)).isPresent();
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
        ds = getOrThrow(distributionSetManagement.getWithDetails(ds.getId()));

        final Long dsId = ds.getId();
        // not allowed as it is assigned now
        assertThatThrownBy(() -> distributionSetManagement.assignSoftwareModules(dsId, os2Id))
                .isInstanceOf(EntityReadOnlyException.class);

        // not allowed as it is assigned now
        final Long appId = getOrThrow(findFirstModuleByType(ds, appType)).getId();
        assertThatThrownBy(() -> distributionSetManagement.unassignSoftwareModule(dsId, appId))
                .isInstanceOf(EntityReadOnlyException.class);
    }

    /**
     * Ensures that it is not possible to add a software module that is not defined of the DS's type.
     */
    @Test
    void updateDistributionSetUnsupportedModuleFails() {
        final Long setId = distributionSetManagement.create(
                DistributionSetManagement.Create.builder()
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
    void updateDistributionSet() {
        // prepare data
        DistributionSet ds = testdataFactory.createDistributionSet("");
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        // update data
        // legal update of module addition
        distributionSetManagement.assignSoftwareModules(ds.getId(), Set.of(os.getId()));
        ds = getOrThrow(distributionSetManagement.getWithDetails(ds.getId()));
        assertThat(getOrThrow(findFirstModuleByType(ds, osType))).isEqualTo(os);

        // legal update of module removal
        distributionSetManagement.unassignSoftwareModule(ds.getId(),
                getOrThrow(findFirstModuleByType(ds, appType)).getId());
        ds = getOrThrow(distributionSetManagement.getWithDetails(ds.getId()));
        assertThat(findFirstModuleByType(ds, appType)).isNotPresent();

        // Update description
        distributionSetManagement.update(DistributionSetManagement.Update.builder().id(ds.getId()).name("a new name")
                .description("a new description").version("a new version").requiredMigrationStep(true).build());
        ds = getOrThrow(distributionSetManagement.getWithDetails(ds.getId()));
        assertThat(ds.getDescription()).isEqualTo("a new description");
        assertThat(ds.getName()).isEqualTo("a new name");
        assertThat(ds.getVersion()).isEqualTo("a new version");
        assertThat(ds.isRequiredMigrationStep()).isTrue();
    }

    /**
     * Verifies that an exception is thrown when trying to update an invalid distribution set
     */
    @Test
    void updateInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        final DistributionSetManagement.Update update =
                DistributionSetManagement.Update.builder().id(distributionSet.getId()).name("new_name").build();
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception").isThrownBy(() -> distributionSetManagement.update(update));
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
                new DistributionSetInvalidation(singletonList(distributionSetId), CancelationType.NONE, false));

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception").isThrownBy(() -> distributionSetManagement
                        .unassignSoftwareModule(distributionSetId, softwareModuleId));
    }

    /**
     * Checks that metadata for a distribution set can be updated.
     */
    @Test
    @WithUser(allSpPermissions = true)
    void createMetadata() {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a DS
        final DistributionSet ds = testdataFactory.createDistributionSet("testDs");
        // initial opt lock revision must be zero
        assertThat(ds.getOptLockRevision()).isEqualTo(1);

        waitNextMillis();
        // create an DS meta data entry
        insertMetadata(knownKey, knownValue, ds);

        final DistributionSet changedLockRevisionDS = getOrThrow(distributionSetManagement.get(ds.getId()));
        assertThat(changedLockRevisionDS.getOptLockRevision()).isEqualTo(2);

        waitNextMillis();
        // update the DS metadata
        distributionSetManagement.createMetadata(ds.getId(), knownKey, knownUpdateValue);
        // we are updating the sw metadata so also modifying the base software
        // module so opt lock revision must be three
        final DistributionSet reloadedDS = getOrThrow(distributionSetManagement.get(ds.getId()));
        assertThat(reloadedDS.getOptLockRevision()).isEqualTo(3);
        assertThat(reloadedDS.getLastModifiedAt()).isPositive();

        // verify updated meta data is the updated value
        assertThat(distributionSetManagement.getMetadata(ds.getId()).get(knownKey)).isEqualTo(knownUpdateValue);
    }

    /**
     * searches for distribution sets based on the various filter options, e.g. name, version, desc., tags.
     */
    @Test
    void searchDistributionSetsOnFilters() {
        DistributionSetTag dsTagA = distributionSetTagManagement
                .create(DistributionSetTagManagement.Create.builder().name("DistributionSetTag-A").build());
        final DistributionSetTag dsTagB = distributionSetTagManagement
                .create(DistributionSetTagManagement.Create.builder().name("DistributionSetTag-B").build());
        final DistributionSetTag dsTagC = distributionSetTagManagement
                .create(DistributionSetTagManagement.Create.builder().name("DistributionSetTag-C").build());
        distributionSetTagManagement.create(DistributionSetTagManagement.Create.builder().name("DistributionSetTag-D").build());

        List<? extends DistributionSet> dsGroup1 = testdataFactory.createDistributionSets("", 5);
        final String dsGroup2Prefix = "test";
        List<? extends DistributionSet> dsGroup2 = testdataFactory.createDistributionSets(dsGroup2Prefix, 5);
        DistributionSet dsDeleted = testdataFactory.createDistributionSet("testDeleted");
        final DistributionSet dsInComplete = distributionSetManagement.create(
                DistributionSetManagement.Create.builder()
                        .name("notcomplete").version("1").type(standardDsType).build());

        DistributionSetType newType = distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("foo").name("bar").description("test").build());

        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(newType.getId(),
                singletonList(osType.getId()));
        newType = distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(newType.getId(),
                Arrays.asList(appType.getId(), runtimeType.getId()));

        final DistributionSet dsNewType = distributionSetManagement.create(
                DistributionSetManagement.Create.builder()
                        .type(newType)
                        .name("newtype").version("1")
                        .modules(new HashSet<>(dsDeleted.getModules()))
                        .build());

        assignDistributionSet(dsDeleted, testdataFactory.createTargets(5));
        distributionSetManagement.delete(dsDeleted.getId());
        dsDeleted = getOrThrow(distributionSetManagement.get(dsDeleted.getId()));

        dsGroup1 = assignTag(dsGroup1, dsTagA);
        dsTagA = getOrThrow(distributionSetTagRepository.findByNameEquals(dsTagA.getName()));
        dsGroup1 = assignTag(dsGroup1, dsTagB);
        dsTagA = getOrThrow(distributionSetTagRepository.findByNameEquals(dsTagA.getName()));
        dsGroup2 = assignTag(dsGroup2, dsTagA);
        dsTagA = getOrThrow(distributionSetTagRepository.findByNameEquals(dsTagA.getName()));

        final List<? extends DistributionSet> allDistributionSets = Stream
                .of(dsGroup1, dsGroup2, Arrays.asList(dsDeleted, dsInComplete, dsNewType)).flatMap(Collection::stream)
                .toList();
        final List<? extends DistributionSet> dsGroup1WithGroup2 = Stream.of(dsGroup1, dsGroup2).flatMap(Collection::stream)
                .toList();
        final int sizeOfAllDistributionSets = allDistributionSets.size();

        // check setup
        assertThat(distributionSetRepository.findAll()).hasSize(sizeOfAllDistributionSets);

        validateFindAll(allDistributionSets);
        validateDeleted(dsDeleted, sizeOfAllDistributionSets - 1);
        validateCompleted(dsInComplete, sizeOfAllDistributionSets - 1);
        validateType(newType, dsNewType, sizeOfAllDistributionSets - 1);
        validateSearchText(allDistributionSets, dsGroup2Prefix);
        validateTags(dsTagA, dsTagB, dsTagC, dsGroup1WithGroup2, dsGroup1);
        validateDeletedAndCompleted(dsGroup1WithGroup2, dsNewType, dsDeleted);
        validateDeletedAndCompletedAndType(dsGroup1WithGroup2, dsDeleted, newType, dsNewType);
        validateDeletedAndCompletedAndTypeAndSearchText(dsGroup2, newType, dsGroup2Prefix);
        validateDeletedAndCompletedAndTypeAndSearchText(dsGroup1WithGroup2, dsDeleted, dsInComplete, dsNewType, newType, ":1");
        validateDeletedAndCompletedAndTypeAndSearchTextAndTag(dsGroup2, dsTagA, dsGroup2Prefix);
    }

    /**
     * Simple DS load without the related data that should be loaded lazy.
     */
    @Test
    void findDistributionSetsWithoutLazy() {
        testdataFactory.createDistributionSets(20);

        assertThat(distributionSetManagement.findByCompleted(true, PAGE)).hasSize(20);
    }

    /**
     * Locks a DS.
     */
    @Test
    void lockDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        assertThat(
                distributionSetManagement.get(distributionSet.getId()).map(DistributionSet::isLocked).orElse(true))
                .isFalse();
        distributionSetManagement.lock(distributionSet.getId());
        assertThat(
                distributionSetManagement.get(distributionSet.getId()).map(DistributionSet::isLocked).orElse(false))
                .isTrue();
        // assert software modules are locked
        assertThat(distributionSet.getModules().size()).isNotZero();
        distributionSetManagement.getWithDetails(distributionSet.getId()).map(DistributionSet::getModules)
                .orElseThrow().forEach(module -> assertThat(module.isLocked()).isTrue());
    }

    /**
     * Locked a DS could be hard deleted.
     */
    @Test
    void deleteUnassignedLockedDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        distributionSetManagement.lock(distributionSet.getId());
        assertThat(
                distributionSetManagement.get(distributionSet.getId()).map(DistributionSet::isLocked)
                        .orElse(false))
                .isTrue();

        distributionSetManagement.delete(distributionSet.getId());
        assertThat(distributionSetManagement.get(distributionSet.getId())).isEmpty();
    }

    /**
     * Locked an assigned DS could be soft deleted.
     */
    @Test
    void deleteAssignedLockedDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        distributionSetManagement.lock(distributionSet.getId());
        assertThat(
                distributionSetManagement.get(distributionSet.getId()).map(DistributionSet::isLocked)
                        .orElse(false))
                .isTrue();

        final Target target = testdataFactory.createTarget();
        assignDistributionSet(distributionSet.getId(), target.getControllerId());

        distributionSetManagement.delete(distributionSet.getId());
        assertThat(distributionSetManagement.getOrElseThrowException(distributionSet.getId()).isDeleted()).isTrue();
    }

    /**
     * Unlocks a DS.
     */
    @Test
    void unlockDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        distributionSetManagement.lock(distributionSet.getId());
        assertThat(
                distributionSetManagement.get(distributionSet.getId()).map(DistributionSet::isLocked)
                        .orElse(false))
                .isTrue();
        distributionSetManagement.unlock(distributionSet.getId());
        assertThat(
                distributionSetManagement.get(distributionSet.getId()).map(DistributionSet::isLocked)
                        .orElse(true))
                .isFalse();
        // assert software modules are not unlocked
        assertThat(distributionSet.getModules().size()).isNotZero();
        distributionSetManagement.getWithDetails(distributionSet.getId()).map(DistributionSet::getModules)
                .orElseThrow().forEach(module -> assertThat(module.isLocked()).isTrue());
    }

    /**
     * Software modules of a locked DS can't be modified. Expected behaviour is to throw an exception and to do not modify them.
     */
    @Test
    void lockDistributionSetApplied() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        final int softwareModuleCount = distributionSet.getModules().size();
        assertThat(softwareModuleCount).isNotZero();
        final Long distributionSetId = distributionSet.getId();
        distributionSetManagement.lock(distributionSetId);
        assertThat(distributionSetManagement.get(distributionSetId).map(DistributionSet::isLocked).orElse(false)).isTrue();

        // try add
        final List<Long> moduleIds = List.of(testdataFactory.createSoftwareModule("sm-1").getId());
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked DS software modules should throw an exception")
                .isThrownBy(() -> distributionSetManagement.assignSoftwareModules(distributionSetId, moduleIds));
        assertThat(distributionSetManagement.getWithDetails(distributionSetId).get().getModules())
                .as("Software module shall not be added to a locked DS.")
                .hasSize(softwareModuleCount);

        // try remove
        final Long fisrtModuleId = distributionSet.getModules().stream().findFirst().get().getId();
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked DS software modules should throw an exception")
                .isThrownBy(() -> distributionSetManagement.unassignSoftwareModule(distributionSetId, fisrtModuleId));
        assertThat(distributionSetManagement.getWithDetails(distributionSetId).get().getModules())
                .as("Software module shall not be removed from a locked DS.")
                .hasSize(softwareModuleCount);
    }

    /**
     * Test implicit locks for a DS and skip tags.
     */
    @SuppressWarnings("rawtypes")
    @Test
    void isImplicitLockApplicableForDistributionSet() {
        final JpaDistributionSetManagement distributionSetManagement = (JpaDistributionSetManagement) (DistributionSetManagement) this.distributionSetManagement;
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-non-skip");
        // assert that implicit lock is applicable for non skip tags
        assertThat(distributionSetManagement.isImplicitLockApplicable(distributionSet)).isTrue();

        assertThat(repositoryProperties.getSkipImplicitLockForTags().size()).isNotZero();
        final List<? extends DistributionSetTag> skipTags = distributionSetTagManagement.create(
                repositoryProperties.getSkipImplicitLockForTags().stream()
                        .map(String::toLowerCase)
                        // remove same in case-insensitive terms tags
                        // in of case-insensitive db's it will end up as same names and constraint violation (?)
                        .distinct()
                        .map(skipTag -> (DistributionSetTagManagement.Create)DistributionSetTagManagement.Create.builder().name(skipTag).build())
                        .toList());
        // assert that implicit lock locks for every skip tag
        skipTags.forEach(skipTag -> {
            DistributionSet distributionSetWithSkipTag = testdataFactory.createDistributionSet("ds-skip-" + skipTag.getName());
            distributionSetManagement.assignTag(List.of(distributionSetWithSkipTag.getId()), skipTag.getId());
            distributionSetWithSkipTag = distributionSetManagement.get(distributionSetWithSkipTag.getId()).orElseThrow();
            // assert that implicit lock isn't applicable for skip tags
            assertThat(distributionSetManagement.isImplicitLockApplicable(distributionSetWithSkipTag)).isFalse();
        });
    }

    /**
     * Locks an incomplete DS. Expected behaviour is to throw an exception and to do not lock it.
     */
    @Test
    void lockIncompleteDistributionSetFails() {
        final long incompleteDistributionSetId = testdataFactory.createIncompleteDistributionSet().getId();
        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Locking an incomplete distribution set should throw an exception")
                .isThrownBy(() -> distributionSetManagement.lock(incompleteDistributionSetId));
        assertThat(
                distributionSetManagement.get(incompleteDistributionSetId).map(DistributionSet::isLocked).orElse(true))
                .isFalse();
    }

    /**
     * Deletes a DS that is no in use. Expected behaviour is a hard delete on the database.
     */
    @Test
    void deleteUnassignedDistributionSet() {
        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds-1");
        testdataFactory.createDistributionSet("ds-2");

        // delete a ds
        assertThat(distributionSetRepository.findAll()).hasSize(2);
        distributionSetManagement.delete(ds1.getId());
        // not assigned so not marked as deleted but fully deleted
        assertThat(distributionSetRepository.findAll()).hasSize(1);
        assertThat(distributionSetManagement.findByCompleted(true, PAGE)).hasSize(1);
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
     * Queries and loads the metadata related to a given distribution set.
     */
    @Test
    void getMetadata() {
        // create a DS
        final DistributionSet ds1 = testdataFactory.createDistributionSet("testDs1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("testDs2");

        for (int index = 0; index < quotaManagement.getMaxMetaDataEntriesPerDistributionSet(); index++) {
            insertMetadata("key" + index, "value" + index, ds1);
        }

        for (int index = 0; index <= quotaManagement.getMaxMetaDataEntriesPerDistributionSet() - 2; index++) {
            insertMetadata("key" + index, "value" + index, ds2);
        }

        assertThat(distributionSetManagement.getMetadata(ds1.getId())).hasSize(quotaManagement.getMaxMetaDataEntriesPerDistributionSet());
        assertThat(distributionSetManagement.getMetadata(ds2.getId())).hasSize(quotaManagement.getMaxMetaDataEntriesPerDistributionSet() - 1);
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
        assertThat(distributionSetManagement.findByCompleted(true, PAGE)).hasSize(2);
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
                .isThrownBy(() -> distributionSetManagement.getValid(distributionSetId));
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
     * Verify that an exception is thrown when trying to create or update metadata for an invalid distribution set.
     */
    @Test
    void createMetadataForInvalidDistributionSet() {
        final String knownKey1 = "myKnownKey1";
        final String knownKey2 = "myKnownKey2";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "knownUpdateValue";

        final Long dsId = testdataFactory.createDistributionSet().getId();
        distributionSetManagement.createMetadata(dsId, Map.of(knownKey1, knownValue));

        distributionSetInvalidationManagement.invalidateDistributionSet(
                new DistributionSetInvalidation(singletonList(dsId), CancelationType.NONE, false));

        // assert that no new metadata can be created
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetManagement.createMetadata(dsId, Map.of(knownKey2, knownValue)));

        // assert that an existing metadata can not be updated
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetManagement.createMetadata(dsId, knownKey1, knownUpdateValue));
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

        Optional<Rollout> rollout = rolloutManagement.get(rollout1.getId());
        rollout.ifPresent(value -> assertThat(Rollout.RolloutStatus.valueOf(
                String.valueOf(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds1.getId()).get(0).getName()))).isEqualTo(
                value.getStatus()));

        rollout = rolloutManagement.get(rollout2.getId());
        rollout.ifPresent(value -> assertThat(Rollout.RolloutStatus.valueOf(
                String.valueOf(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds2.getId()).get(0).getName()))).isEqualTo(
                value.getStatus()));
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
                entityFactory.targetFilterQuery().create().name("test filter 1").autoAssignDistributionSet(ds.getId()).query("name==targets*"));

        targetFilterQueryManagement.create(
                entityFactory.targetFilterQuery().create().name("test filter 2").autoAssignDistributionSet(ds.getId()).query("name==targets*"));

        assertThat(distributionSetManagement.countAutoAssignmentsForDistributionSet(ds.getId())).isEqualTo(2);
        assertThat(distributionSetManagement.countAutoAssignmentsForDistributionSet(ds2.getId())).isNull();
    }

    private void createAndUpdateDistributionSetWithInvalidDescription(final DistributionSet set) {
        final DistributionSetManagement.Create distributionSetCreate =
                DistributionSetManagement.Create.builder().name("a").version("a").description(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));

        final DistributionSetManagement.Create distributionSetCreate2 =
                DistributionSetManagement.Create.builder().name("a").version("a").description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate2));

        final DistributionSetManagement.Update distributionSetUpdate =
                DistributionSetManagement.Update.builder().id(set.getId()).description(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long description should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate));

        final DistributionSetManagement.Update distributionSetUpdate2 =
                DistributionSetManagement.Update.builder().id(set.getId()).description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate2));
    }

    private void createAndUpdateDistributionSetWithInvalidName(final DistributionSet set) {
        final DistributionSetManagement.Create distributionSetCreate = DistributionSetManagement.Create.builder()
                .version("a").name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));

        final DistributionSetManagement.Create distributionSetCreate2 = DistributionSetManagement.Create.builder().version("a").name("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate2));

        final DistributionSetManagement.Create distributionSetCreate3 = DistributionSetManagement.Create.builder().version("a").name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters in name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate3));

        final DistributionSetManagement.Update distributionSetUpdate = DistributionSetManagement.Update.builder().id(set.getId())
                .name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate));

        final DistributionSetManagement.Update distributionSetUpdate2 = DistributionSetManagement.Update.builder().id(set.getId()).name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate2));

        final DistributionSetManagement.Update distributionSetUpdate3 = DistributionSetManagement.Update.builder().id(set.getId()).name("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate3));
    }

    private void createAndUpdateDistributionSetWithInvalidVersion(final DistributionSet set) {
        final DistributionSetManagement.Create distributionSetCreate = DistributionSetManagement.Create.builder()
                .name("a").version(randomString(NamedVersionedEntity.VERSION_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));

        final DistributionSetManagement.Create distributionSetCreate2 = DistributionSetManagement.Create.builder().name("a").version("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate2));

        final DistributionSetManagement.Update distributionSetUpdate = DistributionSetManagement.Update.builder().id(set.getId())
                .version(randomString(NamedVersionedEntity.VERSION_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate));

        final DistributionSetManagement.Update distributionSetUpdate2 = DistributionSetManagement.Update.builder().id(set.getId()).version("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate2));
    }

    private void validateFindAll(final List<? extends DistributionSet> expectedDistributionSets) {

        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder(), expectedDistributionSets);
    }

    private void validateDeleted(final DistributionSet deletedDistributionSet, final int notDeletedSize) {

        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().isDeleted(Boolean.TRUE),
                singletonList(deletedDistributionSet));

        assertThatFilterHasSizeAndDoesNotContainDistributionSet(
                DistributionSetFilter.builder().isDeleted(Boolean.FALSE), notDeletedSize, deletedDistributionSet);
    }

    private void validateCompleted(final DistributionSet dsIncomplete, final int completedSize) {

        assertThatFilterHasSizeAndDoesNotContainDistributionSet(
                DistributionSetFilter.builder().isComplete(Boolean.TRUE), completedSize, dsIncomplete);

        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().isComplete(Boolean.FALSE), singletonList(dsIncomplete));
    }

    private void validateType(final DistributionSetType newType, final DistributionSet dsNewType,
            final int standardDsTypeSize) {
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().typeId(newType.getId()),
                singletonList(dsNewType));
        assertThatFilterHasSizeAndDoesNotContainDistributionSet(
                DistributionSetFilter.builder().typeId(standardDsType.getId()), standardDsTypeSize, dsNewType);
    }

    private void validateSearchText(final List<? extends DistributionSet> allDistributionSets, final String dsNamePrefix) {
        final List<? extends DistributionSet> withTestNamePrefix = allDistributionSets.stream()
                .filter(ds -> ds.getName().startsWith(dsNamePrefix)).toList();
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().searchText(dsNamePrefix),
                withTestNamePrefix);

        final List<? extends DistributionSet> withTestNameExact = withTestNamePrefix.stream()
                .filter(ds -> ds.getName().equals(dsNamePrefix)).toList();
        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().searchText(dsNamePrefix + ":"), withTestNameExact);

        final List<? extends DistributionSet> withTestNameExactAndVersionPrefix = withTestNameExact.stream()
                .filter(ds -> ds.getVersion().startsWith("1")).toList();
        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().searchText(dsNamePrefix + ":1"),
                withTestNameExactAndVersionPrefix);

        final List<? extends DistributionSet> dsWithExactNameAndVersion = withTestNameExactAndVersionPrefix.stream()
                .filter(ds -> ds.getVersion().equals("1.0.0")).toList();
        assertThat(dsWithExactNameAndVersion).hasSize(1);
        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().searchText(dsNamePrefix + ":1.0.0"), dsWithExactNameAndVersion);

        final List<? extends DistributionSet> withVersionPrefix = allDistributionSets.stream()
                .filter(ds -> ds.getVersion().startsWith("1.0.")).toList();
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().searchText(":1.0."),
                withVersionPrefix);

        final List<? extends DistributionSet> withVersionExact = withVersionPrefix.stream()
                .filter(ds -> ds.getVersion().equals("1.0.0")).toList();
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().searchText(":1.0.0"), withVersionExact);
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().searchText(":"), allDistributionSets);
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().searchText(" : "), allDistributionSets);
    }

    private void validateTags(final DistributionSetTag dsTagA, final DistributionSetTag dsTagB,
            final DistributionSetTag dsTagC, final List<? extends DistributionSet> dsWithTagA,
            final List<? extends DistributionSet> dsWithTagB) {
        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().tagNames(singletonList(dsTagA.getName())), dsWithTagA);

        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().tagNames(singletonList(dsTagB.getName())), dsWithTagB);

        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().tagNames(Arrays.asList(dsTagA.getName(), dsTagB.getName())),
                dsWithTagA);

        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().tagNames(Arrays.asList(dsTagC.getName(), dsTagB.getName())),
                dsWithTagB);

        assertThatFilterDoesNotContainAnyDistributionSet(
                DistributionSetFilter.builder().tagNames(singletonList(dsTagC.getName())));
    }

    private void validateDeletedAndCompleted(final List<? extends DistributionSet> completedStandardType,
            final DistributionSet dsNewType, final DistributionSet dsDeleted) {

        final List<DistributionSet> completedNotDeleted = new ArrayList<>(completedStandardType);
        completedNotDeleted.add(dsNewType);
        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().isComplete(Boolean.TRUE).isDeleted(Boolean.FALSE),
                completedNotDeleted);

        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().isComplete(Boolean.TRUE).isDeleted(Boolean.TRUE),
                singletonList(dsDeleted));

        assertThatFilterDoesNotContainAnyDistributionSet(
                DistributionSetFilter.builder().isComplete(Boolean.FALSE).isDeleted(Boolean.TRUE));
    }

    private void validateDeletedAndCompletedAndType(final List<? extends DistributionSet> deletedAndCompletedAndStandardType,
            final DistributionSet dsDeleted, final DistributionSetType newType, final DistributionSet dsNewType) {
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().isDeleted(Boolean.FALSE)
                .isComplete(Boolean.TRUE).typeId(standardDsType.getId()), deletedAndCompletedAndStandardType);
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().isComplete(Boolean.TRUE)
                .typeId(standardDsType.getId()).isDeleted(Boolean.TRUE), singletonList(dsDeleted));
        assertThatFilterDoesNotContainAnyDistributionSet(DistributionSetFilter.builder().isDeleted(Boolean.TRUE)
                .isComplete(Boolean.FALSE).typeId(standardDsType.getId()));
        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().isComplete(Boolean.TRUE).typeId(newType.getId()),
                singletonList(dsNewType));
    }

    private void validateDeletedAndCompletedAndTypeAndSearchText(
            final List<? extends DistributionSet> completedAndStandardTypeAndSearchText, final DistributionSetType newType, final String text) {
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().isDeleted(Boolean.FALSE)
                        .isComplete(Boolean.TRUE).typeId(standardDsType.getId()).searchText(text),
                completedAndStandardTypeAndSearchText);

        assertThatFilterDoesNotContainAnyDistributionSet(DistributionSetFilter.builder().isComplete(Boolean.TRUE)
                .isDeleted(Boolean.TRUE).typeId(standardDsType.getId()).searchText(text + ":"));

        assertThatFilterDoesNotContainAnyDistributionSet(
                DistributionSetFilter.builder().typeId(standardDsType.getId()).searchText(text)
                        .isComplete(Boolean.FALSE).isDeleted(Boolean.FALSE));

        assertThatFilterDoesNotContainAnyDistributionSet(DistributionSetFilter.builder().typeId(newType.getId())
                .searchText(text).isComplete(Boolean.TRUE).isDeleted(Boolean.FALSE));
    }

    private void validateDeletedAndCompletedAndTypeAndSearchText(
            final List<? extends DistributionSet> completedAndNotDeletedStandardTypeAndFilterString,
            final DistributionSet dsDeleted, final DistributionSet dsInComplete, final DistributionSet dsNewType,
            final DistributionSetType newType, final String filterString) {

        final List<DistributionSet> completedAndStandardTypeAndFilterString = new ArrayList<>(
                completedAndNotDeletedStandardTypeAndFilterString);
        completedAndStandardTypeAndFilterString.add(dsDeleted);
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().isComplete(Boolean.TRUE)
                        .typeId(standardDsType.getId()).searchText(filterString),
                completedAndStandardTypeAndFilterString);

        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().isComplete(Boolean.TRUE).isDeleted(Boolean.FALSE)
                        .typeId(standardDsType.getId()).searchText(filterString),
                completedAndNotDeletedStandardTypeAndFilterString);

        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().isComplete(Boolean.TRUE)
                        .isDeleted(Boolean.TRUE).typeId(standardDsType.getId()).searchText(filterString),
                singletonList(dsDeleted));

        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().typeId(standardDsType.getId()).searchText(filterString)
                        .isComplete(Boolean.FALSE).isDeleted(Boolean.FALSE),
                singletonList(dsInComplete));

        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().typeId(newType.getId())
                        .searchText(filterString).isComplete(Boolean.TRUE).isDeleted(Boolean.FALSE),
                singletonList(dsNewType));
    }

    private void validateDeletedAndCompletedAndTypeAndSearchTextAndTag(
            final List<? extends DistributionSet> completedAndStandartTypeAndSearchTextAndTagA, final DistributionSetTag dsTagA, final String text) {
        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().isComplete(Boolean.TRUE).typeId(standardDsType.getId())
                        .searchText(text).tagNames(singletonList(dsTagA.getName())),
                completedAndStandartTypeAndSearchTextAndTagA);

        assertThatFilterDoesNotContainAnyDistributionSet(DistributionSetFilter.builder()
                .typeId(standardDsType.getId()).searchText(text).tagNames(singletonList(dsTagA.getName()))
                .isComplete(Boolean.FALSE).isDeleted(Boolean.FALSE));
    }

    private void insertMetadata(final String knownKey, final String knownValue, final DistributionSet distributionSet) {
        distributionSetManagement.createMetadata(distributionSet.getId(), Map.of(knownKey, knownValue));
        assertThat(distributionSetManagement.getMetadata(distributionSet.getId()).get(knownKey)).isEqualTo(knownValue);
    }

    private void assertThatFilterContainsOnlyGivenDistributionSets(final DistributionSetFilterBuilder filterBuilder,
            final List<? extends DistributionSet> distributionSets) {
        final int expectedDsSize = distributionSets.size();
        assertThat(((DistributionSetManagement)distributionSetManagement).findByDistributionSetFilter(filterBuilder.build(), PAGE).getContent())
                .hasSize(expectedDsSize).containsOnly(distributionSets.toArray(new DistributionSet[expectedDsSize]));
    }

    private void assertThatFilterDoesNotContainAnyDistributionSet(final DistributionSetFilterBuilder filterBuilder) {
        assertThat(distributionSetManagement.findByDistributionSetFilter(filterBuilder.build(), PAGE).getContent())
                .isEmpty();
    }

    private void assertThatFilterHasSizeAndDoesNotContainDistributionSet(
            final DistributionSetFilterBuilder filterBuilder, final int size, final DistributionSet ds) {
        assertThat(((DistributionSetManagement)distributionSetManagement).findByDistributionSetFilter(filterBuilder.build(), PAGE).getContent())
                .hasSize(size).doesNotContain(ds);
    }

    // can be removed with java-11
    private <T> T getOrThrow(final Optional<T> opt) {
        return opt.orElseThrow(NoSuchElementException::new);
    }
}

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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolationException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Condition;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
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
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * {@link DistributionSetManagement} tests.
 */
@Feature("Component Tests - Repository")
@Story("DistributionSet Management")
class DistributionSetManagementTest extends AbstractJpaIntegrationTest {

    private static final String TAG1_NAME = "Tag1";

    @Autowired
    RepositoryProperties repositoryProperties;

    @Test
    @Description("Verifies that management get access react as specified on calls for non existing entities by means of Optional not present.")
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3) })
    void nonExistingEntityAccessReturnsNotPresent() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        assertThat(distributionSetManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(distributionSetManagement.getWithDetails(NOT_EXIST_IDL)).isNotPresent();
        assertThat(distributionSetManagement.findByNameAndVersion(NOT_EXIST_ID, NOT_EXIST_ID)).isNotPresent();
        assertThat(distributionSetManagement.findMetaDataByDistributionSetId(set.getId(), NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specified on calls for non existing entities by means of " +
            "throwing EntityNotFoundException.")
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

        verifyThrownExceptionBy(() -> distributionSetManagement.create(
                entityFactory.distributionSet().create().name("xxx").type(NOT_EXIST_ID)), "DistributionSetType");

        verifyThrownExceptionBy(() -> distributionSetManagement.putMetaData(
                NOT_EXIST_IDL, singletonList(entityFactory.generateDsMetadata("123", "123"))), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.delete(singletonList(NOT_EXIST_IDL)), "DistributionSet");
        verifyThrownExceptionBy(() -> distributionSetManagement.delete(NOT_EXIST_IDL), "DistributionSet");
        verifyThrownExceptionBy(() -> distributionSetManagement.deleteMetaData(NOT_EXIST_IDL, "xxx"),
                "DistributionSet");
        verifyThrownExceptionBy(() -> distributionSetManagement.deleteMetaData(set.getId(), NOT_EXIST_ID),
                "DistributionSetMetadata");

        verifyThrownExceptionBy(() -> distributionSetManagement.findByAction(NOT_EXIST_IDL), "Action");

        verifyThrownExceptionBy(() -> distributionSetManagement.findMetaDataByDistributionSetId(NOT_EXIST_IDL, "xxx"),
                "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.findMetaDataByDistributionSetId(NOT_EXIST_IDL, PAGE),
                "DistributionSet");

        verifyThrownExceptionBy(
                () -> distributionSetManagement.findMetaDataByDistributionSetIdAndRsql(NOT_EXIST_IDL, "name==*", PAGE),
                "DistributionSet");

        assertThatThrownBy(() -> distributionSetManagement.isInUse(NOT_EXIST_IDL))
                .isInstanceOf(EntityNotFoundException.class).hasMessageContaining(NOT_EXIST_ID)
                .hasMessageContaining("DistributionSet");

        verifyThrownExceptionBy(
                () -> distributionSetManagement.update(entityFactory.distributionSet().update(NOT_EXIST_IDL)),
                "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.updateMetaData(NOT_EXIST_IDL,
                entityFactory.generateDsMetadata("xxx", "xxx")), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.updateMetaData(set.getId(),
                entityFactory.generateDsMetadata(NOT_EXIST_ID, "xxx")), "DistributionSetMetadata");

        verifyThrownExceptionBy(() -> distributionSetManagement.getOrElseThrowException(NOT_EXIST_IDL),
                "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.getValidAndComplete(NOT_EXIST_IDL), "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetManagement.getValid(NOT_EXIST_IDL), "DistributionSet");
    }

    @Test
    @Description("Verify that a DistributionSet with invalid properties cannot be created or updated")
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

    @Test
    @Description("Ensures that it is not possible to create a DS that already exists (unique constraint is on name,version for DS).")
    void createDuplicateDistributionSetsFailsWithException() {
        testdataFactory.createDistributionSet("a");

        assertThatThrownBy(() -> testdataFactory.createDistributionSet("a"))
                .isInstanceOf(EntityAlreadyExistsException.class);
    }

    @Test
    @Description("Verifies that a DS is of default type if not specified explicitly at creation time.")
    void createDistributionSetWithImplicitType() {
        final DistributionSet set = distributionSetManagement
                .create(entityFactory.distributionSet().create().name("newtypesoft").version("1"));

        assertThat(set.getType())
                .as("Type should be equal to default type of tenant")
                .isEqualTo(systemManagement.getTenantMetadata().getDefaultDsType());

    }

    @Test
    @Description("Verifies that a DS cannot be created if another DS with same name and version exists.")
    void createDistributionSetWithDuplicateNameAndVersionFails() {
        distributionSetManagement.create(entityFactory.distributionSet().create().name("newtypesoft").version("1"));

        assertThatExceptionOfType(EntityAlreadyExistsException.class).isThrownBy(() -> distributionSetManagement
                .create(entityFactory.distributionSet().create().name("newtypesoft").version("1")));

    }

    @Test
    @Description("Verifies that multiple DS are of default type if not specified explicitly at creation time.")
    void createMultipleDistributionSetsWithImplicitType() {
        final List<DistributionSetCreate> creates = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            creates.add(entityFactory.distributionSet().create().name("newtypesoft" + i).version("1" + i));
        }

        assertThat(distributionSetManagement.create(creates))
                .as("Type should be equal to default type of tenant")
                .are(new Condition<>() {

                    @Override
                    public boolean matches(final DistributionSet value) {
                        return value.getType().equals(systemManagement.getTenantMetadata().getDefaultDsType());
                    }
                });
    }

    @Test
    @Description("Checks that metadata for a distribution set can be created.")
    void createDistributionSetMetadata() {
        final String knownKey = "dsMetaKnownKey";
        final String knownValue = "dsMetaKnownValue";

        final DistributionSet ds = testdataFactory.createDistributionSet("testDs");

        final DistributionSetMetadata metadata = new JpaDistributionSetMetadata(knownKey, ds, knownValue);
        final JpaDistributionSetMetadata createdMetadata = (JpaDistributionSetMetadata) createDistributionSetMetadata(
                ds.getId(), metadata);

        assertThat(createdMetadata).isNotNull();
        assertThat(createdMetadata.getId().getKey()).isEqualTo(knownKey);
        assertThat(createdMetadata.getDistributionSet().getId()).isEqualTo(ds.getId());
        assertThat(createdMetadata.getValue()).isEqualTo(knownValue);
    }

    @Test
    @Description("Verifies the enforcement of the metadata quota per distribution set.")
    void createDistributionSetMetadataUntilQuotaIsExceeded() {

        // add meta data one by one
        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerDistributionSet();
        for (int i = 0; i < maxMetaData; ++i) {
            assertThat((JpaDistributionSetMetadata) createDistributionSetMetadata(ds1.getId(),
                    new JpaDistributionSetMetadata("k" + i, ds1, "v" + i))).isNotNull();
        }

        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> createDistributionSetMetadata(ds1.getId(), createDistributionSetMetadata(ds1.getId(),
                        new JpaDistributionSetMetadata("k" + maxMetaData, ds1, "v" + maxMetaData))));

        // add multiple meta data entries at once
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");
        final List<MetaData> metaData2 = new ArrayList<>();
        for (int i = 0; i < maxMetaData + 1; ++i) {
            metaData2.add(new JpaDistributionSetMetadata("k" + i, ds2, "v" + i));
        }
        // verify quota is exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> createDistributionSetMetadata(ds2.getId(), metaData2));

        // add some meta data entries
        final DistributionSet ds3 = testdataFactory.createDistributionSet("ds3");
        final int firstHalf = Math.round((maxMetaData) / 2.f);
        for (int i = 0; i < firstHalf; ++i) {
            createDistributionSetMetadata(ds3.getId(), new JpaDistributionSetMetadata("k" + i, ds3, "v" + i));
        }
        // add too many data entries
        final int secondHalf = maxMetaData - firstHalf;
        final List<MetaData> metaData3 = new ArrayList<>();
        for (int i = 0; i < secondHalf + 1; ++i) {
            metaData3.add(new JpaDistributionSetMetadata("kk" + i, ds3, "vv" + i));
        }
        // verify quota is exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> createDistributionSetMetadata(ds3.getId(), metaData3));

    }

    @Test
    @Description("Ensures that distribution sets can assigned and unassigned to a  distribution set tag.")
    void assignAndUnassignDistributionSetToTag() {
        final List<Long> assignDS = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            assignDS.add(testdataFactory.createDistributionSet("DS" + i, "1.0", Collections.emptyList()).getId());
        }

        final DistributionSetTag tag = distributionSetTagManagement.create(entityFactory.tag().create().name(TAG1_NAME));

        final List<DistributionSet> assignedDS = distributionSetManagement.assignTag(assignDS, tag.getId());
        assertThat(assignedDS.size()).as("assigned ds has wrong size").isEqualTo(4);
        assignedDS.stream().map(JpaDistributionSet.class::cast).forEach(ds -> assertThat(ds.getTags().size())
                .as("ds has wrong tag size")
                .isEqualTo(1));

        final DistributionSetTag findDistributionSetTag = getOrThrow(distributionSetTagManagement.findByName(TAG1_NAME));

        assertThat(assignedDS.size()).as("assigned ds has wrong size")
                .isEqualTo(distributionSetManagement.findByTag(tag.getId(), PAGE).getNumberOfElements());

        final JpaDistributionSet unAssignDS = (JpaDistributionSet) distributionSetManagement
                .unassignTag(List.of(assignDS.get(0)), findDistributionSetTag.getId()).get(0);
        assertThat(unAssignDS.getId()).as("unassigned ds is wrong").isEqualTo(assignDS.get(0));
        assertThat(unAssignDS.getTags().size()).as("unassigned ds has wrong tag size").isZero();
        assertThat(distributionSetTagManagement.findByName(TAG1_NAME)).isPresent();
        assertThat(distributionSetManagement.findByTag(tag.getId(), PAGE).getNumberOfElements())
                .as("ds tag ds has wrong ds size").isEqualTo(3);

        assertThat(distributionSetManagement.findByRsqlAndTag("name==" + unAssignDS.getName(), tag.getId(), PAGE)
                .getNumberOfElements()).as("ds tag ds has wrong ds size").isZero();
        assertThat(distributionSetManagement.findByRsqlAndTag("name!=" + unAssignDS.getName(), tag.getId(), PAGE)
                .getNumberOfElements()).as("ds tag ds has wrong ds size").isEqualTo(3);
    }

    @Test
    @Description("Ensures that updates concerning the internal software structure of a DS are not possible if the DS is already assigned.")
    void updateDistributionSetForbiddenWithIllegalUpdate() {
        // prepare data
        final Target target = testdataFactory.createTarget();

        DistributionSet ds = testdataFactory.createDistributionSet("ds-1");

        final SoftwareModule ah2 = testdataFactory.createSoftwareModuleApp();
        final SoftwareModule os2 = testdataFactory.createSoftwareModuleOs();

        // update is allowed as it is still not assigned to a target
        ds = distributionSetManagement.assignSoftwareModules(ds.getId(), Set.of(ah2.getId()));

        // assign target
        assignDistributionSet(ds.getId(), target.getControllerId());
        ds = getOrThrow(distributionSetManagement.getWithDetails(ds.getId()));

        final Long dsId = ds.getId();
        // not allowed as it is assigned now
        assertThatThrownBy(() -> distributionSetManagement.assignSoftwareModules(dsId, Set.of(os2.getId())))
                .isInstanceOf(EntityReadOnlyException.class);

        // not allowed as it is assigned now
        final Long appId = getOrThrow(ds.findFirstModuleByType(appType)).getId();
        assertThatThrownBy(() -> distributionSetManagement.unassignSoftwareModule(dsId, appId))
                .isInstanceOf(EntityReadOnlyException.class);
    }

    @Test
    @Description("Ensures that it is not possible to add a software module that is not defined of the DS's type.")
    void updateDistributionSetUnsupportedModuleFails() {
        final DistributionSet set = distributionSetManagement
                .create(entityFactory
                        .distributionSet().create().name("agent-hub2").version(
                                "1.0.5")
                        .type(distributionSetTypeManagement.create(entityFactory.distributionSetType().create()
                                .key("test").name("test").mandatory(singletonList(osType.getId()))).getKey()));

        final SoftwareModule module = softwareModuleManagement.create(
                entityFactory.softwareModule().create().name("agent-hub2").version("1.0.5").type(appType.getKey()));

        // update data
        assertThatThrownBy(
                () -> distributionSetManagement.assignSoftwareModules(set.getId(), Set.of(module.getId())))
                .isInstanceOf(UnsupportedSoftwareModuleForThisDistributionSetException.class);
    }

    @Test
    @Description("Legal updates of a DS, e.g. name or description and module addition, removal while still unassigned.")
    void updateDistributionSet() {
        // prepare data
        DistributionSet ds = testdataFactory.createDistributionSet("");
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        // update data
        // legal update of module addition
        distributionSetManagement.assignSoftwareModules(ds.getId(), Set.of(os.getId()));
        ds = getOrThrow(distributionSetManagement.getWithDetails(ds.getId()));
        assertThat(getOrThrow(ds.findFirstModuleByType(osType))).isEqualTo(os);

        // legal update of module removal
        distributionSetManagement.unassignSoftwareModule(ds.getId(),
                getOrThrow(ds.findFirstModuleByType(appType)).getId());
        ds = getOrThrow(distributionSetManagement.getWithDetails(ds.getId()));
        assertThat(ds.findFirstModuleByType(appType)).isNotPresent();

        // Update description
        distributionSetManagement.update(entityFactory.distributionSet().update(ds.getId()).name("a new name")
                .description("a new description").version("a new version").requiredMigrationStep(true));
        ds = getOrThrow(distributionSetManagement.getWithDetails(ds.getId()));
        assertThat(ds.getDescription()).isEqualTo("a new description");
        assertThat(ds.getName()).isEqualTo("a new name");
        assertThat(ds.getVersion()).isEqualTo("a new version");
        assertThat(ds.isRequiredMigrationStep()).isTrue();
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to update an invalid distribution set")
    void updateInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(distributionSet.getId()).name("new_name")));
    }

    @Test
    @Description("Verifies the enforcement of the software module quota per distribution set.")
    void assignSoftwareModulesUntilQuotaIsExceeded() {

        // create some software modules
        final int maxModules = quotaManagement.getMaxSoftwareModulesPerDistributionSet();
        final List<Long> modules = new ArrayList<>();
        for (int i = 0; i < maxModules + 1; ++i) {
            modules.add(testdataFactory.createSoftwareModuleApp("sm" + i).getId());
        }

        // assign software modules one by one
        final DistributionSet ds1 = testdataFactory.createDistributionSetWithNoSoftwareModules("ds1", "1.0");
        for (int i = 0; i < maxModules; ++i) {
            distributionSetManagement.assignSoftwareModules(ds1.getId(), singletonList(modules.get(i)));
        }
        // add one more to cause the quota to be exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> distributionSetManagement
                .assignSoftwareModules(ds1.getId(), singletonList(modules.get(maxModules))));

        // assign all software modules at once
        final DistributionSet ds2 = testdataFactory.createDistributionSetWithNoSoftwareModules("ds2", "1.0");
        // verify quota is exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetManagement.assignSoftwareModules(ds2.getId(), modules));

        // assign some software modules
        final DistributionSet ds3 = testdataFactory.createDistributionSetWithNoSoftwareModules("ds3", "1.0");
        final int firstHalf = Math.round((maxModules) / 2.f);
        for (int i = 0; i < firstHalf; ++i) {
            distributionSetManagement.assignSoftwareModules(ds3.getId(), singletonList(modules.get(i)));
        }
        // assign the remaining modules to cause the quota to be exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> distributionSetManagement
                .assignSoftwareModules(ds3.getId(), modules.subList(firstHalf, modules.size())));

    }

    @Test
    @Description("Verifies that an exception is thrown when trying to assign software modules to an invalidated distribution set.")
    void verifyAssignSoftwareModulesToInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModuleOs();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception").isThrownBy(() -> distributionSetManagement
                        .assignSoftwareModules(distributionSet.getId(), singletonList(softwareModule.getId())));
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to unassign a software module from an invalidated distribution set.")
    void verifyUnassignSoftwareModulesToInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModuleOs();
        distributionSetManagement.assignSoftwareModules(distributionSet.getId(), singletonList(softwareModule.getId()));
        distributionSetInvalidationManagement.invalidateDistributionSet(
                new DistributionSetInvalidation(singletonList(distributionSet.getId()), CancelationType.NONE, false));

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception").isThrownBy(() -> distributionSetManagement
                        .unassignSoftwareModule(distributionSet.getId(), softwareModule.getId()));
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that metadata for a distribution set can be updated.")
    void updateDistributionSetMetadata() {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a DS
        final DistributionSet ds = testdataFactory.createDistributionSet("testDs");
        // initial opt lock revision must be zero
        assertThat(ds.getOptLockRevision()).isEqualTo(1);

        waitNextMillis();
        // create an DS meta data entry
        createDistributionSetMetadata(ds.getId(), new JpaDistributionSetMetadata(knownKey, ds, knownValue));

        final DistributionSet changedLockRevisionDS = getOrThrow(distributionSetManagement.get(ds.getId()));
        assertThat(changedLockRevisionDS.getOptLockRevision()).isEqualTo(2);

        waitNextMillis();
        // update the DS metadata
        final JpaDistributionSetMetadata updated = (JpaDistributionSetMetadata) distributionSetManagement
                .updateMetaData(ds.getId(), entityFactory.generateDsMetadata(knownKey, knownUpdateValue));
        // we are updating the sw metadata so also modifying the base software
        // module so opt lock revision must be three
        final DistributionSet reloadedDS = getOrThrow(distributionSetManagement.get(ds.getId()));
        assertThat(reloadedDS.getOptLockRevision()).isEqualTo(3);
        assertThat(reloadedDS.getLastModifiedAt()).isPositive();

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(updated.getId().getKey()).isEqualTo(knownKey);
        assertThat(updated.getDistributionSet().getId()).isEqualTo(ds.getId());
    }

    @Test
    @Description("searches for distribution sets based on the various filter options, e.g. name, version, desc., tags.")
    void searchDistributionSetsOnFilters() {
        DistributionSetTag dsTagA = distributionSetTagManagement
                .create(entityFactory.tag().create().name("DistributionSetTag-A"));
        final DistributionSetTag dsTagB = distributionSetTagManagement
                .create(entityFactory.tag().create().name("DistributionSetTag-B"));
        final DistributionSetTag dsTagC = distributionSetTagManagement
                .create(entityFactory.tag().create().name("DistributionSetTag-C"));
        distributionSetTagManagement.create(entityFactory.tag().create().name("DistributionSetTag-D"));

        List<DistributionSet> dsGroup1 = testdataFactory.createDistributionSets("", 5);
        final String dsGroup2Prefix = "test";
        List<DistributionSet> dsGroup2 = testdataFactory.createDistributionSets(dsGroup2Prefix, 5);
        DistributionSet dsDeleted = testdataFactory.createDistributionSet("testDeleted");
        final DistributionSet dsInComplete = distributionSetManagement.create(entityFactory.distributionSet().create()
                .name("notcomplete").version("1").type(standardDsType.getKey()));

        DistributionSetType newType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("foo").name("bar").description("test"));

        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(newType.getId(),
                singletonList(osType.getId()));
        newType = distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(newType.getId(),
                Arrays.asList(appType.getId(), runtimeType.getId()));

        final DistributionSet dsNewType = distributionSetManagement.create(
                entityFactory.distributionSet().create().name("newtype").version("1").type(newType.getKey()).modules(
                        dsDeleted.getModules().stream().map(SoftwareModule::getId).collect(Collectors.toList())));

        assignDistributionSet(dsDeleted, testdataFactory.createTargets(5));
        distributionSetManagement.delete(dsDeleted.getId());
        dsDeleted = getOrThrow(distributionSetManagement.get(dsDeleted.getId()));

        dsGroup1 = assignTag(dsGroup1, dsTagA);
        dsTagA = getOrThrow(distributionSetTagRepository.findByNameEquals(dsTagA.getName()));
        dsGroup1 = assignTag(dsGroup1, dsTagB);
        dsTagA = getOrThrow(distributionSetTagRepository.findByNameEquals(dsTagA.getName()));
        dsGroup2 = assignTag(dsGroup2, dsTagA);
        dsTagA = getOrThrow(distributionSetTagRepository.findByNameEquals(dsTagA.getName()));

        final List<DistributionSet> allDistributionSets = Stream
                .of(dsGroup1, dsGroup2, Arrays.asList(dsDeleted, dsInComplete, dsNewType)).flatMap(Collection::stream)
                .collect(Collectors.toList());
        final List<DistributionSet> dsGroup1WithGroup2 = Stream.of(dsGroup1, dsGroup2).flatMap(Collection::stream)
                .collect(Collectors.toList());
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
        validateDeletedAndCompletedAndTypeAndSearchText(dsGroup1WithGroup2, dsDeleted, dsInComplete, dsNewType, newType,
                ":1");
        validateDeletedAndCompletedAndTypeAndSearchTextAndTag(dsGroup2, dsTagA, dsGroup2Prefix);
    }

    @Test
    @Description("Simple DS load without the related data that should be loaded lazy.")
    void findDistributionSetsWithoutLazy() {
        testdataFactory.createDistributionSets(20);

        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(20);
    }

    @Test
    @Description("Locks a DS.")
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
        assertThat(distributionSet.getModules().size()).isNotEqualTo(0);
        distributionSetManagement.getWithDetails(distributionSet.getId()).map(DistributionSet::getModules)
                .orElseThrow().forEach(module -> assertThat(module.isLocked()).isTrue());
    }

    @Test
    @Description("Locked a DS could be hard deleted.")
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

    @Test
    @Description("Locked an assigned DS could be soft deleted.")
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

    @Test
    @Description("Unlocks a DS.")
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
        assertThat(distributionSet.getModules().size()).isNotEqualTo(0);
        distributionSetManagement.getWithDetails(distributionSet.getId()).map(DistributionSet::getModules)
                .orElseThrow().forEach(module -> assertThat(module.isLocked()).isTrue());
    }

    @Test
    @Description("Software modules of a locked DS can't be modified. Expected behaviour is to throw an exception and to do not modify them.")
    void lockDistributionSetApplied() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-1");
        final int softwareModuleCount = distributionSet.getModules().size();
        assertThat(softwareModuleCount).isNotEqualTo(0);
        distributionSetManagement.lock(distributionSet.getId());
        assertThat(
                distributionSetManagement.get(distributionSet.getId()).map(DistributionSet::isLocked).orElse(false))
                .isTrue();

        // try add
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked DS software modules should throw an exception")
                .isThrownBy(() -> distributionSetManagement.assignSoftwareModules(
                        distributionSet.getId(), List.of(testdataFactory.createSoftwareModule("sm-1").getId())));
        assertThat(distributionSetManagement.getWithDetails(distributionSet.getId()).get().getModules().size())
                .as("Software module shall not be added to a locked DS.")
                .isEqualTo(softwareModuleCount);

        // try remove
        assertThatExceptionOfType(LockedException.class)
                .as("Attempt to modify a locked DS software modules should throw an exception")
                .isThrownBy(() -> distributionSetManagement.unassignSoftwareModule(
                        distributionSet.getId(), distributionSet.getModules().stream().findFirst().get().getId()));
        assertThat(distributionSetManagement.getWithDetails(distributionSet.getId()).get().getModules().size())
                .as("Software module shall not be removed from a locked DS.")
                .isEqualTo(softwareModuleCount);
    }

    @Test
    @Description("Test implicit locks for a DS and skip tags.")
    void isImplicitLockApplicableForDistributionSet() {
        final JpaDistributionSetManagement distributionSetManagement = (JpaDistributionSetManagement) this.distributionSetManagement;
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds-non-skip");
        // assert that implicit lock is applicable for non skip tags
        assertThat(distributionSetManagement.isImplicitLockApplicable(distributionSet)).isTrue();

        assertThat(repositoryProperties.getSkipImplicitLockForTags().size()).isNotZero();
        final List<DistributionSetTag> skipTags = distributionSetTagManagement.create(
                repositoryProperties.getSkipImplicitLockForTags().stream()
                        .map(String::toLowerCase)
                        // remove same in case-insensitive terms tags
                        // in of case-insensitive db's it will end up as same names and constraint violation (?)
                        .distinct()
                        .map(skipTag -> entityFactory.tag().create().name(skipTag))
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

    @Test
    @Description("Locks an incomplete DS. Expected behaviour is to throw an exception and to do not lock it.")
    void lockIncompleteDistributionSetFails() {
        final long incompleteDistributionSetId = testdataFactory.createIncompleteDistributionSet().getId();
        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Locking an incomplete distribution set should throw an exception")
                .isThrownBy(() -> distributionSetManagement.lock(incompleteDistributionSetId));
        assertThat(
                distributionSetManagement.get(incompleteDistributionSetId).map(DistributionSet::isLocked).orElse(true))
                .isFalse();
    }

    @Test
    @Description("Deletes a DS that is no in use. Expected behaviour is a hard delete on the database.")
    void deleteUnassignedDistributionSet() {
        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds-1");
        testdataFactory.createDistributionSet("ds-2");

        // delete a ds
        assertThat(distributionSetRepository.findAll()).hasSize(2);
        distributionSetManagement.delete(ds1.getId());
        // not assigned so not marked as deleted but fully deleted
        assertThat(distributionSetRepository.findAll()).hasSize(1);
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(1);
    }

    @Test
    @Description("Deletes an invalid distribution set")
    void deleteInvalidDistributionSet() {
        final DistributionSet set = testdataFactory.createAndInvalidateDistributionSet();
        assertThat(distributionSetRepository.findById(set.getId())).isNotEmpty();
        distributionSetManagement.delete(set.getId());
        assertThat(distributionSetRepository.findById(set.getId())).isEmpty();
    }

    @Test
    @Description("Deletes an incomplete distribution set")
    void deleteIncompleteDistributionSet() {
        final DistributionSet set = testdataFactory.createIncompleteDistributionSet();
        assertThat(distributionSetRepository.findById(set.getId())).isNotEmpty();
        distributionSetManagement.delete(set.getId());
        assertThat(distributionSetRepository.findById(set.getId())).isEmpty();
    }

    @Test
    @Description("Queries and loads the metadata related to a given software module.")
    void findAllDistributionSetMetadataByDsId() {
        // create a DS
        final DistributionSet ds1 = testdataFactory.createDistributionSet("testDs1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("testDs2");

        for (int index = 0; index < quotaManagement.getMaxMetaDataEntriesPerDistributionSet(); index++) {
            createDistributionSetMetadata(ds1.getId(),
                    new JpaDistributionSetMetadata("key" + index, ds1, "value" + index));
        }

        for (int index = 0; index <= quotaManagement.getMaxMetaDataEntriesPerDistributionSet() - 2; index++) {
            createDistributionSetMetadata(ds2.getId(),
                    new JpaDistributionSetMetadata("key" + index, ds2, "value" + index));
        }

        final Page<DistributionSetMetadata> metadataOfDs1 = distributionSetManagement
                .findMetaDataByDistributionSetId(ds1.getId(), PageRequest.of(0, 100));

        final Page<DistributionSetMetadata> metadataOfDs2 = distributionSetManagement
                .findMetaDataByDistributionSetId(ds2.getId(), PageRequest.of(0, 100));

        assertThat(metadataOfDs1.getNumberOfElements())
                .isEqualTo(quotaManagement.getMaxMetaDataEntriesPerDistributionSet());
        assertThat(metadataOfDs1.getTotalElements())
                .isEqualTo(quotaManagement.getMaxMetaDataEntriesPerDistributionSet());

        assertThat(metadataOfDs2.getNumberOfElements())
                .isEqualTo(quotaManagement.getMaxMetaDataEntriesPerDistributionSet() - 1);
        assertThat(metadataOfDs2.getTotalElements())
                .isEqualTo(quotaManagement.getMaxMetaDataEntriesPerDistributionSet() - 1);
    }

    @Test
    @Description("Deletes a DS that is in use by either target assignment or rollout. Expected behaviour is a soft delete on the database, i.e. only marked as "
            + "deleted, kept as reference but unavailable for future use..")
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
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(2);
        assertThat(distributionSetManagement.findAll(PAGE)).hasSize(2);
        assertThat(distributionSetManagement.findByRsql("name==*", PAGE)).hasSize(2);
        assertThat(distributionSetManagement.count()).isEqualTo(2);
    }

    @Test
    @Description("Verify that the find all by ids contains the entities which are looking for")
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

        final List<DistributionSet> foundDs = distributionSetManagement.get(searchIds);

        assertThat(foundDs).hasSize(3);

        final List<Long> collect = foundDs.stream().map(DistributionSet::getId).collect(Collectors.toList());
        assertThat(collect).containsAll(searchIds);
    }

    @Test
    @Description("Verify that an exception is thrown when trying to get an invalid distribution set")
    void verifyGetValid() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetManagement.getValid(distributionSet.getId()));
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetManagement.getValidAndComplete(distributionSet.getId()));
    }

    @Test
    @Description("Verify that an exception is thrown when trying to get an incomplete distribution set")
    void verifyGetValidAndComplete() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetManagement.getValidAndComplete(distributionSet.getId()));
    }

    @Test
    @Description("Verify that an exception is thrown when trying to create or update metadata for an invalid distribution set.")
    void createMetadataForInvalidDistributionSet() {
        final String knownKey1 = "myKnownKey1";
        final String knownKey2 = "myKnownKey2";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "knownUpdateValue";

        final DistributionSet ds = testdataFactory.createDistributionSet();
        distributionSetManagement.putMetaData(ds.getId(),
                singletonList(entityFactory.generateDsMetadata(knownKey1, knownValue)));

        distributionSetInvalidationManagement.invalidateDistributionSet(
                new DistributionSetInvalidation(singletonList(ds.getId()), CancelationType.NONE, false));

        // assert that no new metadata can be created
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetManagement.putMetaData(ds.getId(),
                        singletonList(entityFactory.generateDsMetadata(knownKey2, knownValue))));

        // assert that an existing metadata can not be updated
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception").isThrownBy(() -> distributionSetManagement
                        .updateMetaData(ds.getId(), entityFactory.generateDsMetadata(knownKey1, knownUpdateValue)));
    }

    @Test
    @Description("Get the Rollouts count by status statistics for a specific Distribution Set")
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

    @Test
    @Description("Get the Rollouts count by status statistics for a specific Distribution Set")
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

    @Test
    @Description("Get the Rollouts count by status statistics for a specific Distribution Set")
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

    @Step
    private void createAndUpdateDistributionSetWithInvalidDescription(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version("a").description(RandomStringUtils.randomAlphanumeric(513))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version("a").description(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long description should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .description(RandomStringUtils.randomAlphanumeric(513))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).description(INVALID_TEXT_HTML)));
    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidName(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().version("a")
                        .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short name should not be created").isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().version("a").name("")));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters in name should not be created")
                .isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().version("a").name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short name should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).name("")));

    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidVersion(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version(RandomStringUtils.randomAlphanumeric(NamedVersionedEntity.VERSION_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short version should not be created").isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().name("a").version("")));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .version(RandomStringUtils.randomAlphanumeric(NamedVersionedEntity.VERSION_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short version should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).version("")));

    }

    @Step
    private void validateFindAll(final List<DistributionSet> expectedDistributionsets) {

        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder(), expectedDistributionsets);
    }

    @Step
    private void validateDeleted(final DistributionSet deletedDistributionSet, final int notDeletedSize) {

        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().isDeleted(Boolean.TRUE),
                singletonList(deletedDistributionSet));

        assertThatFilterHasSizeAndDoesNotContainDistributionSet(
                DistributionSetFilter.builder().isDeleted(Boolean.FALSE), notDeletedSize, deletedDistributionSet);
    }

    @Step
    private void validateCompleted(final DistributionSet dsIncomplete, final int completedSize) {

        assertThatFilterHasSizeAndDoesNotContainDistributionSet(
                DistributionSetFilter.builder().isComplete(Boolean.TRUE), completedSize, dsIncomplete);

        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().isComplete(Boolean.FALSE), singletonList(dsIncomplete));
    }

    @Step
    private void validateType(final DistributionSetType newType, final DistributionSet dsNewType,
            final int standardDsTypeSize) {
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().typeId(newType.getId()),
                singletonList(dsNewType));
        assertThatFilterHasSizeAndDoesNotContainDistributionSet(
                DistributionSetFilter.builder().typeId(standardDsType.getId()), standardDsTypeSize, dsNewType);
    }

    @Step
    private void validateSearchText(final List<DistributionSet> allDistributionSets, final String dsNamePrefix) {

        final List<DistributionSet> withTestNamePrefix = allDistributionSets.stream()
                .filter(ds -> ds.getName().startsWith(dsNamePrefix)).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().searchText(dsNamePrefix),
                withTestNamePrefix);

        final List<DistributionSet> withTestNameExact = withTestNamePrefix.stream()
                .filter(ds -> ds.getName().equals(dsNamePrefix)).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().searchText(dsNamePrefix + ":"), withTestNameExact);

        final List<DistributionSet> withTestNameExactAndVersionPrefix = withTestNameExact.stream()
                .filter(ds -> ds.getVersion().startsWith("1")).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().searchText(dsNamePrefix + ":1"),
                withTestNameExactAndVersionPrefix);

        final List<DistributionSet> dsWithExactNameAndVersion = withTestNameExactAndVersionPrefix.stream()
                .filter(ds -> ds.getVersion().equals("1.0.0")).collect(Collectors.toList());
        assertThat(dsWithExactNameAndVersion).hasSize(1);
        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().searchText(dsNamePrefix + ":1.0.0"), dsWithExactNameAndVersion);

        final List<DistributionSet> withVersionPrefix = allDistributionSets.stream()
                .filter(ds -> ds.getVersion().startsWith("1.0.")).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().searchText(":1.0."),
                withVersionPrefix);

        final List<DistributionSet> withVersionExact = withVersionPrefix.stream()
                .filter(ds -> ds.getVersion().equals("1.0.0")).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().searchText(":1.0.0"),
                withVersionExact);

        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().searchText(":"),
                allDistributionSets);

        assertThatFilterContainsOnlyGivenDistributionSets(DistributionSetFilter.builder().searchText(" : "),
                allDistributionSets);
    }

    @Step
    private void validateTags(final DistributionSetTag dsTagA, final DistributionSetTag dsTagB,
            final DistributionSetTag dsTagC, final List<DistributionSet> dsWithTagA,
            final List<DistributionSet> dsWithTagB) {

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

    @Step
    private void validateDeletedAndCompleted(final List<DistributionSet> completedStandardType,
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

    @Step
    private void validateDeletedAndCompletedAndType(final List<DistributionSet> deletedAndCompletedAndStandardType,
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

    @Step
    private void validateDeletedAndCompletedAndTypeAndSearchText(
            final List<DistributionSet> completedAndStandardTypeAndSearchText, final DistributionSetType newType,
            final String text) {

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

    @Step
    private void validateDeletedAndCompletedAndTypeAndSearchText(
            final List<DistributionSet> completedAndNotDeletedStandardTypeAndFilterString,
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

    @Step
    private void validateDeletedAndCompletedAndTypeAndSearchTextAndTag(
            final List<DistributionSet> completedAndStandartTypeAndSearchTextAndTagA, final DistributionSetTag dsTagA,
            final String text) {

        assertThatFilterContainsOnlyGivenDistributionSets(
                DistributionSetFilter.builder().isComplete(Boolean.TRUE).typeId(standardDsType.getId())
                        .searchText(text).tagNames(singletonList(dsTagA.getName())),
                completedAndStandartTypeAndSearchTextAndTagA);

        assertThatFilterDoesNotContainAnyDistributionSet(DistributionSetFilter.builder()
                .typeId(standardDsType.getId()).searchText(text).tagNames(singletonList(dsTagA.getName()))
                .isComplete(Boolean.FALSE).isDeleted(Boolean.FALSE));
    }

    private void assertThatFilterContainsOnlyGivenDistributionSets(final DistributionSetFilterBuilder filterBuilder,
            final List<DistributionSet> distributionSets) {
        final int expectedDsSize = distributionSets.size();
        assertThat(distributionSetManagement.findByDistributionSetFilter(filterBuilder.build(), PAGE).getContent())
                .hasSize(expectedDsSize).containsOnly(distributionSets.toArray(new DistributionSet[expectedDsSize]));
    }

    private void assertThatFilterDoesNotContainAnyDistributionSet(final DistributionSetFilterBuilder filterBuilder) {
        assertThat(distributionSetManagement.findByDistributionSetFilter(filterBuilder.build(), PAGE).getContent())
                .isEmpty();
    }

    private void assertThatFilterHasSizeAndDoesNotContainDistributionSet(
            final DistributionSetFilterBuilder filterBuilder, final int size, final DistributionSet ds) {
        assertThat(distributionSetManagement.findByDistributionSetFilter(filterBuilder.build(), PAGE).getContent())
                .hasSize(size).doesNotContain(ds);
    }

    // can be removed with java-11
    private <T> T getOrThrow(final Optional<T> opt) {
        return opt.orElseThrow(NoSuchElementException::new);
    }
}

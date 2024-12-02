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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;

/**
 * {@link DistributionSetManagement} tests.
 */
@Feature("Component Tests - Repository")
@Story("DistributionSet Management")
public class DistributionSetTypeManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access react as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 0) })
    public void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(distributionSetTypeManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(distributionSetTypeManagement.getByKey(NOT_EXIST_ID)).isNotPresent();
        assertThat(distributionSetTypeManagement.getByName(NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 0),
            @Expect(type = DistributionSetTypeCreatedEvent.class, count = 1) })
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {

        final List<Long> softwareModuleTypes = Collections.singletonList(osType.getId());

        verifyThrownExceptionBy(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(NOT_EXIST_IDL,
                softwareModuleTypes), "DistributionSetType");
        final List<Long> notExistingSwModuleTypeIds = Collections.singletonList(NOT_EXIST_IDL);
        verifyThrownExceptionBy(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(
                        testdataFactory.findOrCreateDistributionSetType("xxx", "xxx").getId(), notExistingSwModuleTypeIds),
                "SoftwareModuleType");

        verifyThrownExceptionBy(() -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(NOT_EXIST_IDL,
                softwareModuleTypes), "DistributionSetType");
        verifyThrownExceptionBy(() -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(
                        testdataFactory.findOrCreateDistributionSetType("xxx", "xxx").getId(), notExistingSwModuleTypeIds),
                "SoftwareModuleType");

        verifyThrownExceptionBy(() -> distributionSetTypeManagement.delete(NOT_EXIST_IDL), "DistributionSetType");

        verifyThrownExceptionBy(
                () -> distributionSetTypeManagement.update(entityFactory.distributionSetType().update(NOT_EXIST_IDL)),
                "DistributionSet");
    }

    @Test
    @Description("Verify that a DistributionSet with invalid properties cannot be created or updated")
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 0) })
    public void createAndUpdateDistributionSetWithInvalidFields() {
        final DistributionSet set = testdataFactory.createDistributionSet();

        createAndUpdateDistributionSetWithInvalidDescription(set);
        createAndUpdateDistributionSetWithInvalidName(set);
        createAndUpdateDistributionSetWithInvalidVersion(set);
    }

    @Test
    @Description("Tests the successful module update of unused distribution set type which is in fact allowed.")
    public void updateUnassignedDistributionSetTypeModules() {
        final DistributionSetType updatableType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("updatableType").name("to be deleted"));
        assertThat(distributionSetTypeManagement.getByKey("updatableType").get().getMandatoryModuleTypes()).isEmpty();

        // add OS
        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(updatableType.getId(),
                Set.of(osType.getId()));
        assertThat(distributionSetTypeManagement.getByKey("updatableType").get().getMandatoryModuleTypes())
                .containsOnly(osType);

        // add JVM
        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(updatableType.getId(),
                Set.of(runtimeType.getId()));
        assertThat(distributionSetTypeManagement.getByKey("updatableType").get().getMandatoryModuleTypes())
                .containsOnly(osType, runtimeType);

        // remove OS
        distributionSetTypeManagement.unassignSoftwareModuleType(updatableType.getId(), osType.getId());
        assertThat(distributionSetTypeManagement.getByKey("updatableType").get().getMandatoryModuleTypes())
                .containsOnly(runtimeType);
    }

    @Test
    @Description("Verifies that the quota for software module types per distribution set type is enforced as expected.")
    public void quotaMaxSoftwareModuleTypes() {

        final int quota = quotaManagement.getMaxSoftwareModuleTypesPerDistributionSetType();
        // create software module types
        final List<Long> moduleTypeIds = new ArrayList<>();
        for (int i = 0; i < quota + 1; ++i) {
            final SoftwareModuleTypeCreate smCreate = entityFactory.softwareModuleType().create().name("smType_" + i)
                    .description("smType_" + i).maxAssignments(1).colour("blue").key("smType_" + i);
            moduleTypeIds.add(softwareModuleTypeManagement.create(smCreate).getId());
        }

        // assign all types at once
        final DistributionSetType dsType1 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("dst1").name("dst1"));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(
                () -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType1.getId(), moduleTypeIds));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(
                () -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(dsType1.getId(), moduleTypeIds));

        // assign as many mandatory modules as possible
        final DistributionSetType dsType2 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("dst2").name("dst2"));
        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType2.getId(),
                moduleTypeIds.subList(0, quota));
        assertThat(distributionSetTypeManagement.get(dsType2.getId())).isNotEmpty();
        assertThat(distributionSetTypeManagement.get(dsType2.getId()).get().getMandatoryModuleTypes().size())
                .isEqualTo(quota);
        // assign one more to trigger the quota exceeded error
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType2.getId(),
                        Collections.singletonList(moduleTypeIds.get(quota))));

        // assign as many optional modules as possible
        final DistributionSetType dsType3 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("dst3").name("dst3"));
        distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(dsType3.getId(),
                moduleTypeIds.subList(0, quota));
        assertThat(distributionSetTypeManagement.get(dsType3.getId())).isNotEmpty();
        assertThat(distributionSetTypeManagement.get(dsType3.getId()).get().getOptionalModuleTypes().size())
                .isEqualTo(quota);
        // assign one more to trigger the quota exceeded error
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(dsType3.getId(),
                        Collections.singletonList(moduleTypeIds.get(quota))));

    }

    @Test
    @Description("Tests the successfull update of used distribution set type meta data which is in fact allowed.")
    public void updateAssignedDistributionSetTypeMetaData() {
        final DistributionSetType nonUpdatableType = createDistributionSetTypeUsedByDs();

        distributionSetTypeManagement.update(
                entityFactory.distributionSetType().update(nonUpdatableType.getId()).description("a new description"));

        assertThat(distributionSetTypeManagement.getByKey("updatableType").get().getDescription())
                .isEqualTo("a new description");
        assertThat(distributionSetTypeManagement.getByKey("updatableType").get().getColour()).isEqualTo("test123");
    }

    @Test
    @Description("Tests the unsuccessfull update of used distribution set type (module addition).")
    public void addModuleToAssignedDistributionSetTypeFails() {
        final DistributionSetType nonUpdatableType = createDistributionSetTypeUsedByDs();

        assertThatThrownBy(() -> distributionSetTypeManagement
                .assignMandatorySoftwareModuleTypes(nonUpdatableType.getId(), Set.of(osType.getId())))
                .isInstanceOf(EntityReadOnlyException.class);
    }

    @Test
    @Description("Tests the unsuccessfull update of used distribution set type (module removal).")
    public void removeModuleToAssignedDistributionSetTypeFails() {
        DistributionSetType nonUpdatableType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("updatableType").name("to be deleted"));
        assertThat(distributionSetTypeManagement.getByKey("updatableType").get().getMandatoryModuleTypes()).isEmpty();

        nonUpdatableType = distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(nonUpdatableType.getId(),
                Set.of(osType.getId()));
        distributionSetManagement.create(entityFactory.distributionSet().create().name("newtypesoft").version("1")
                .type(nonUpdatableType.getKey()));

        final Long typeId = nonUpdatableType.getId();
        assertThatThrownBy(() -> distributionSetTypeManagement.unassignSoftwareModuleType(typeId, osType.getId()))
                .isInstanceOf(EntityReadOnlyException.class);
    }

    @Test
    @Description("Tests the successfull deletion of unused (hard delete) distribution set types.")
    public void deleteUnassignedDistributionSetType() {
        final JpaDistributionSetType hardDelete = (JpaDistributionSetType) distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("delete").name("to be deleted"));

        assertThat(distributionSetTypeRepository.findAll()).contains(hardDelete);
        distributionSetTypeManagement.delete(hardDelete.getId());

        assertThat(distributionSetTypeRepository.findAll()).doesNotContain(hardDelete);
    }

    @Test
    @Description("Tests the successfull deletion of used (soft delete) distribution set types.")
    public void deleteAssignedDistributionSetType() {
        final int existing = (int) distributionSetTypeManagement.count();
        final JpaDistributionSetType toBeDeleted = (JpaDistributionSetType) distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("softdeleted").name("to be deleted"));

        assertThat(distributionSetTypeRepository.findAll()).contains(toBeDeleted);
        distributionSetManagement.create(
                entityFactory.distributionSet().create().name("softdeleted").version("1").type(toBeDeleted.getKey()));

        distributionSetTypeManagement.delete(toBeDeleted.getId());
        final Optional<DistributionSetType> softdeleted = distributionSetTypeManagement.getByKey("softdeleted");
        assertThat(softdeleted).isPresent();
        assertThat(softdeleted.get().isDeleted()).isTrue();
        assertThat(distributionSetTypeManagement.findAll(PAGE)).hasSize(existing);
        assertThat(distributionSetTypeManagement.findByRsql(PAGE, "name==*")).hasSize(existing);
        assertThat(distributionSetTypeManagement.count()).isEqualTo(existing);
    }

    @Test
    @Description("Verifies that when no SoftwareModules are assigned to a Distribution then the DistributionSet is not complete.")
    public void shouldFailWhenDistributionSetHasNoSoftwareModulesAssigned() {

        final JpaDistributionSetType jpaDistributionSetType = (JpaDistributionSetType) distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("newType").name("new Type"));

        final List<SoftwareModule> softwareModules = new ArrayList<>();

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("DistributionOne", "3.1.2",
                jpaDistributionSetType, softwareModules);

        assertThat(jpaDistributionSetType.checkComplete(distributionSet)).isFalse();
    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidDescription(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version("a").description(RandomStringUtils.randomAlphanumeric(513))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set invalid description text should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version("a").description(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long description should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .description(RandomStringUtils.randomAlphanumeric(513))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid description should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).description(INVALID_TEXT_HTML)));

    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidName(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with too long name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().version("a")
                        .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with invalid name should not be created")
                .isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().version("a").name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short name should not be created").isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().version("a").name("")));

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with null name should not be created")
                .isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().version("a").name(null)));

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with too long name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with invalid name should not be updated")
                .isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short name should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).name("")));
    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidVersion(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version(RandomStringUtils.randomAlphanumeric(NamedVersionedEntity.VERSION_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid version should not be created").isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().name("a").version(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short version should not be created").isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().name("a").version("")));

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with null version should not be created")
                .isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().name("a").version(null)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .version(RandomStringUtils.randomAlphanumeric(NamedVersionedEntity.VERSION_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid version should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).version(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short version should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).version("")));
    }

    private DistributionSetType createDistributionSetTypeUsedByDs() {
        final DistributionSetType nonUpdatableType = distributionSetTypeManagement.create(entityFactory
                .distributionSetType().create().key("updatableType").name("to be deleted").colour("test123"));
        assertThat(distributionSetTypeManagement.getByKey("updatableType").get().getMandatoryModuleTypes()).isEmpty();
        distributionSetManagement.create(entityFactory.distributionSet().create().name("newtypesoft").version("1")
                .type(nonUpdatableType.getKey()));
        return nonUpdatableType;
    }
}

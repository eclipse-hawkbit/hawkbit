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

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
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
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: DistributionSet Management
 */
class DistributionSetTypeManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Verifies that management get access react as specfied on calls for non existing entities by means 
     * of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = DistributionSetCreatedEvent.class, count = 0) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(distributionSetTypeManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(distributionSetTypeManagement.findByKey(NOT_EXIST_ID)).isNotPresent();
        assertThat(distributionSetTypeManagement.findByName(NOT_EXIST_ID)).isNotPresent();
    }

    /**
     * Verifies that management queries react as specfied on calls for non existing entities 
     *  by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 0),
            @Expect(type = DistributionSetTypeCreatedEvent.class, count = 1) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {

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
                () -> distributionSetTypeManagement.update(DistributionSetTypeManagement.Update.builder().id(NOT_EXIST_IDL).build()),
                        "DistributionSetType");
    }

    /**
     * Verify that a DistributionSet with invalid properties cannot be created or updated
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 0) })
    void createAndUpdateDistributionSetWithInvalidFields() {
        final DistributionSet set = testdataFactory.createDistributionSet();

        createAndUpdateDistributionSetWithInvalidDescription(set);
        createAndUpdateDistributionSetWithInvalidName(set);
        createAndUpdateDistributionSetWithInvalidVersion(set);
    }

    /**
     * Tests the successful module update of unused distribution set type which is in fact allowed.
     */
    @Test
    void updateUnassignedDistributionSetTypeModules() {
        final DistributionSetType updatableType = distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("updatableType").name("to be deleted").build());
        assertThat(distributionSetTypeManagement.findByKey("updatableType").get().getMandatoryModuleTypes()).isEmpty();

        // add OS
        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(updatableType.getId(),
                Set.of(osType.getId()));
        assertThat(distributionSetTypeManagement.findByKey("updatableType").get().getMandatoryModuleTypes())
                .containsOnly(osType);

        // add JVM
        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(updatableType.getId(),
                Set.of(runtimeType.getId()));
        assertThat(distributionSetTypeManagement.findByKey("updatableType").get().getMandatoryModuleTypes())
                .containsOnly(osType, runtimeType);

        // remove OS
        distributionSetTypeManagement.unassignSoftwareModuleType(updatableType.getId(), osType.getId());
        assertThat(distributionSetTypeManagement.findByKey("updatableType").get().getMandatoryModuleTypes())
                .containsOnly(runtimeType);
    }

    /**
     * Verifies that the quota for software module types per distribution set type is enforced as expected.
     */
    @Test
    void quotaMaxSoftwareModuleTypes() {
        final int quota = quotaManagement.getMaxSoftwareModuleTypesPerDistributionSetType();
        // create software module types
        final List<Long> moduleTypeIds = new ArrayList<>();
        for (int i = 0; i < quota + 1; ++i) {
            final SoftwareModuleTypeManagement.Create smCreate = SoftwareModuleTypeManagement.Create.builder().name("smType_" + i)
                    .description("smType_" + i).maxAssignments(1).colour("blue").key("smType_" + i).build();
            moduleTypeIds.add(softwareModuleTypeManagement.create(smCreate).getId());
        }

        // assign all types at once
        final DistributionSetType dsType1 = distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("dst1").name("dst1").build());
        final Long dsType1Id = dsType1.getId();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType1Id, moduleTypeIds));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(
                () -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(dsType1Id, moduleTypeIds));

        // assign as many mandatory modules as possible
        final DistributionSetType dsType2 = distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("dst2").name("dst2").build());
        final Long dsType2Id = dsType2.getId();
        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType2Id,
                moduleTypeIds.subList(0, quota));
        assertThat(distributionSetTypeManagement.get(dsType2Id)).isNotEmpty();
        assertThat(distributionSetTypeManagement.get(dsType2Id).get().getMandatoryModuleTypes()).hasSize(quota);
        // assign one more to trigger the quota exceeded error
        final List<Long> softwareModuleTypeIds = Collections.singletonList(moduleTypeIds.get(quota));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType2Id, softwareModuleTypeIds));

        // assign as many optional modules as possible
        final DistributionSetType dsType3 = distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("dst3").name("dst3").build());
        final Long dsType3Id = dsType3.getId();
        distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(dsType3Id, moduleTypeIds.subList(0, quota));
        assertThat(distributionSetTypeManagement.get(dsType3Id)).isNotEmpty();
        assertThat(distributionSetTypeManagement.get(dsType3Id).get().getOptionalModuleTypes()).hasSize(quota);
        // assign one more to trigger the quota exceeded error
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(dsType3Id, softwareModuleTypeIds));

    }

    /**
     * Tests the successfull update of used distribution set type meta data which is in fact allowed.
     */
    @Test
    void updateAssignedDistributionSetTypeMetaData() {
        final DistributionSetType nonUpdatableType = createDistributionSetTypeUsedByDs();

        distributionSetTypeManagement.update(
                DistributionSetTypeManagement.Update.builder().id(nonUpdatableType.getId()).description("a new description").build());

        assertThat(distributionSetTypeManagement.findByKey("updatableType").orElseThrow().getDescription()).isEqualTo("a new description");
        assertThat(distributionSetTypeManagement.findByKey("updatableType").orElseThrow().getColour()).isEqualTo("test123");
    }

    /**
     * Tests the unsuccessful update of used distribution set type (module addition).
     */
    @Test
    void addModuleToAssignedDistributionSetTypeFails() {
        final Long nonUpdatableTypeId = createDistributionSetTypeUsedByDs().getId();
        final Set<Long> osTypeId = Set.of(osType.getId());
        assertThatThrownBy(() -> distributionSetTypeManagement
                .assignMandatorySoftwareModuleTypes(nonUpdatableTypeId, osTypeId))
                .isInstanceOf(EntityReadOnlyException.class);
    }

    /**
     * Tests the unsuccessful update of used distribution set type (module removal).
     */
    @Test
    void removeModuleToAssignedDistributionSetTypeFails() {
        DistributionSetType nonUpdatableType = distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("updatableType").name("to be deleted").build());
        assertThat(distributionSetTypeManagement.findByKey("updatableType").get().getMandatoryModuleTypes()).isEmpty();

        final Long osTypeId = osType.getId();
        nonUpdatableType = distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(nonUpdatableType.getId(), Set.of(osTypeId));
        distributionSetManagement.create(
                DistributionSetManagement.Create.builder().type(nonUpdatableType).name("newtypesoft").version("1").build());

        final Long typeId = nonUpdatableType.getId();
        assertThatThrownBy(() -> distributionSetTypeManagement.unassignSoftwareModuleType(typeId, osTypeId))
                .isInstanceOf(EntityReadOnlyException.class);
    }

    /**
     * Tests the successfull deletion of unused (hard delete) distribution set types.
     */
    @Test
    void deleteUnassignedDistributionSetType() {
        final JpaDistributionSetType hardDelete = (JpaDistributionSetType) distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("delete").name("to be deleted").build());

        assertThat(distributionSetTypeRepository.findAll()).contains(hardDelete);
        distributionSetTypeManagement.delete(hardDelete.getId());

        assertThat(distributionSetTypeRepository.findAll()).doesNotContain(hardDelete);
    }

    /**
     * Tests the successfull deletion of used (soft delete) distribution set types.
     */
    @Test
    void deleteAssignedDistributionSetType() {
        final int existing = (int) distributionSetTypeManagement.count();
        final JpaDistributionSetType toBeDeleted = (JpaDistributionSetType) distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("softdeleted").name("to be deleted").build());

        assertThat(distributionSetTypeRepository.findAll()).contains(toBeDeleted);
        distributionSetManagement.create(
                DistributionSetManagement.Create.builder().type(toBeDeleted).name("softdeleted").version("1").build());

        distributionSetTypeManagement.delete(toBeDeleted.getId());
        final Optional<? extends DistributionSetType> softDeleted = distributionSetTypeManagement.findByKey("softdeleted");
        assertThat(softDeleted).isPresent();
        assertThat(softDeleted.get().isDeleted()).isTrue();
        assertThat(distributionSetTypeManagement.findAll(PAGE)).hasSize(existing);
        assertThat(distributionSetTypeManagement.findByRsql("name==*", PAGE)).hasSize(existing);
        assertThat(distributionSetTypeManagement.count()).isEqualTo(existing);
    }

    /**
     * Verifies that when no SoftwareModules are assigned to a Distribution then the DistributionSet is not complete.
     */
    @Test
    void shouldFailWhenDistributionSetHasNoSoftwareModulesAssigned() {
        final JpaDistributionSetType jpaDistributionSetType = (JpaDistributionSetType) distributionSetTypeManagement
                .create(DistributionSetTypeManagement.Create.builder().key("newType").name("new Type").build());

        final List<SoftwareModule> softwareModules = new ArrayList<>();

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("DistributionOne", "3.1.2",
                jpaDistributionSetType, softwareModules);

        assertThat(jpaDistributionSetType.checkComplete(distributionSet)).isFalse();
    }

    private void createAndUpdateDistributionSetWithInvalidDescription(final DistributionSet set) {
        final DistributionSetManagement.Create distributionSetCreate = DistributionSetManagement.Create.builder()
                .name("a").version("a").description(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));

        final DistributionSetManagement.Create distributionSetCreate2 = DistributionSetManagement.Create.builder()
                .name("a").version("a").description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set invalid description text should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate2));

        final DistributionSetManagement.Update distributionSetUpdate = DistributionSetManagement.Update.builder().id(set.getId())
                .description(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long description should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate));

        final DistributionSetManagement.Update distributionSetUpdate2 = DistributionSetManagement.Update.builder().id(set.getId()).description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid description should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate2));
    }

    private void createAndUpdateDistributionSetWithInvalidName(final DistributionSet set) {
        final DistributionSetManagement.Create distributionSetCreate = DistributionSetManagement.Create.builder()
                .version("a").name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));

        final DistributionSetManagement.Create distributionSetCreate2 =
                DistributionSetManagement.Create.builder().version("a").name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate2));

        final DistributionSetManagement.Create distributionSetCreate3 =
                DistributionSetManagement.Create.builder().version("a").name("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate3));

        final DistributionSetManagement.Create distributionSetCreate4 =
                DistributionSetManagement.Create.builder().version("a").name(null).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with null name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate4));

        final DistributionSetManagement.Update distributionSetUpdate = DistributionSetManagement.Update.builder().id(set.getId())
                .name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate));

        final DistributionSetManagement.Update distributionSetUpdate2 =
                DistributionSetManagement.Update.builder().id(set.getId()).name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate2));

        final DistributionSetManagement.Update distributionSetUpdate3 =
                DistributionSetManagement.Update.builder().id(set.getId()).name("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate3));
    }

    private void createAndUpdateDistributionSetWithInvalidVersion(final DistributionSet set) {
        final DistributionSetManagement.Create distributionSetCreate = DistributionSetManagement.Create.builder()
                .name("a").version(randomString(NamedVersionedEntity.VERSION_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate));

        final DistributionSetManagement.Create distributionSetCreate2 =
                DistributionSetManagement.Create.builder().name("a").version(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate2));

        final DistributionSetManagement.Create distributionSetCreate3 =
                DistributionSetManagement.Create.builder().name("a").version("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate3));

        final DistributionSetManagement.Create distributionSetCreate4 =
                DistributionSetManagement.Create.builder().name("a").version(null).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with null version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(distributionSetCreate4));

        final DistributionSetManagement.Update distributionSetUpdate = DistributionSetManagement.Update.builder().id(set.getId())
                .version(randomString(NamedVersionedEntity.VERSION_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate));

        final DistributionSetManagement.Update distributionSetUpdate2 =
                DistributionSetManagement.Update.builder().id(set.getId()).version(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate2));

        final DistributionSetManagement.Update distributionSetUpdate3 =
                DistributionSetManagement.Update.builder().id(set.getId()).version("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(distributionSetUpdate3));
    }

    private DistributionSetType createDistributionSetTypeUsedByDs() {
        final DistributionSetType nonUpdatableType = distributionSetTypeManagement.create(
                DistributionSetTypeManagement.Create.builder().key("updatableType").name("to be deleted").colour("test123").build());
        assertThat(distributionSetTypeManagement.findByKey("updatableType").orElseThrow().getMandatoryModuleTypes()).isEmpty();
        distributionSetManagement.create(DistributionSetManagement.Create.builder()
                .type(nonUpdatableType)
                .name("newtypesoft").version("1")
                .build());
        return nonUpdatableType;
    }
}

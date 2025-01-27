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

import java.util.Arrays;
import java.util.List;

import jakarta.validation.ConstraintViolationException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;

@Feature("Component Tests - Repository")
@Story("Software Module Management")
class SoftwareModuleTypeManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 0) })
    void nonExistingEntityAccessReturnsNotPresent() {

        assertThat(softwareModuleTypeManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(softwareModuleTypeManagement.findByKey(NOT_EXIST_ID)).isNotPresent();
        assertThat(softwareModuleTypeManagement.findByName(NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 0) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        verifyThrownExceptionBy(() -> softwareModuleTypeManagement.delete(NOT_EXIST_IDL), "SoftwareModuleType");

        verifyThrownExceptionBy(
                () -> softwareModuleTypeManagement.update(entityFactory.softwareModuleType().update(NOT_EXIST_IDL)),
                "SoftwareModuleType");
    }

    @Test
    @Description("Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.")
    void updateNothingResultsInUnchangedRepositoryForType() {
        final SoftwareModuleType created = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("test-key").name("test-name"));

        final SoftwareModuleType updated = softwareModuleTypeManagement
                .update(entityFactory.softwareModuleType().update(created.getId()));

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entitity to be equal to created version")
                .isEqualTo(created.getOptLockRevision());
    }

    @Test
    @Description("Calling update for changed fields results in change in the repository.")
    void updateSoftwareModuleTypeFieldsToNewValue() {
        final SoftwareModuleType created = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("test-key").name("test-name"));

        final SoftwareModuleType updated = softwareModuleTypeManagement.update(
                entityFactory.softwareModuleType().update(created.getId()).description("changed").colour("changed"));

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entities is")
                .isEqualTo(created.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo("changed");
        assertThat(updated.getColour()).as("Updated vendor is").isEqualTo("changed");
    }

    @Test
    @Description("Create Software Module Types call fails when called for existing entities.")
    void createModuleTypesCallFailsForExistingTypes() {
        final List<SoftwareModuleTypeCreate> created = Arrays.asList(
                entityFactory.softwareModuleType().create().key("test-key").name("test-name"),
                entityFactory.softwareModuleType().create().key("test-key2").name("test-name2"));
        softwareModuleTypeManagement.create(created);
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("should not have worked as module type already exists")
                .isThrownBy(() -> softwareModuleTypeManagement.create(created));
    }

    @Test
    @Description("Tests the successfull deletion of software module types. Both unused (hard delete) and used ones (soft delete).")
    void deleteAssignedAndUnassignedSoftwareModuleTypes() {
        assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(3).contains(osType, runtimeType, appType);

        SoftwareModuleType type = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("bundle").name("OSGi Bundle"));

        assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(4).contains(osType, runtimeType, appType, type);

        // delete unassigned
        softwareModuleTypeManagement.delete(type.getId());
        assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(3).contains(osType, runtimeType, appType);
        assertThat(softwareModuleTypeRepository.findAll()).hasSize(3).contains((JpaSoftwareModuleType) osType,
                (JpaSoftwareModuleType) runtimeType, (JpaSoftwareModuleType) appType);

        type = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("bundle2").name("OSGi Bundle2"));

        assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(4).contains(osType, runtimeType, appType, type);

        softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(type).name("Test SM").version("1.0"));

        // delete assigned
        softwareModuleTypeManagement.delete(type.getId());
        assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(3).contains(osType, runtimeType, appType);
        assertThat(softwareModuleTypeManagement.findByRsql("name==*", PAGE)).hasSize(3).contains(osType, runtimeType,
                appType);
        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);

        assertThat(softwareModuleTypeRepository.findAll()).hasSize(4).contains((JpaSoftwareModuleType) osType,
                (JpaSoftwareModuleType) runtimeType, (JpaSoftwareModuleType) appType,
                softwareModuleTypeRepository.findById(type.getId()).get());
    }

    @Test
    @Description("Checks that software module typeis found based on given name.")
    void findSoftwareModuleTypeByName() {
        testdataFactory.createSoftwareModuleOs();
        final SoftwareModuleType found = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("thetype").name("thename"));
        softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("thetype2").name("anothername"));

        assertThat(softwareModuleTypeManagement.findByName("thename")).as("Type with given name").contains(found);
    }

    @Test
    @Description("Verifies that it is not possible to create a type that alrady exists.")
    void createSoftwareModuleTypeFailsWithExistingEntity() {
        final SoftwareModuleTypeCreate create = entityFactory.softwareModuleType().create().key("thetype").name("thename");
        softwareModuleTypeManagement.create(create);
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("should not have worked as module type already exists")
                .isThrownBy(() -> softwareModuleTypeManagement
                        .create(create));
    }

    @Test
    @Description("Verifies that it is not possible to create a list of types where one already exists.")
    void createSoftwareModuleTypesFailsWithExistingEntity() {
        softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create().key("thetype").name("thename"));
        final List<SoftwareModuleTypeCreate> creates = List.of(
                entityFactory.softwareModuleType().create().key("thetype").name("thename"),
                entityFactory.softwareModuleType().create().key("anothertype").name("anothername"));
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("should not have worked as module type already exists")
                .isThrownBy(() -> softwareModuleTypeManagement.create(creates));
    }

    @Test
    @Description("Verifies that the creation of a softwareModuleType is failing because of invalid max assignment")
    void createSoftwareModuleTypesFailsWithInvalidMaxAssignment() {
        final SoftwareModuleTypeCreate create = entityFactory.softwareModuleType().create().key("type").name("name").maxAssignments(0);
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("should not have worked as max assignment is invalid. Should be greater than 0")
                .isThrownBy(() -> softwareModuleTypeManagement.create(create));
    }

    @Test
    @Description("Verifies that multiple types are created as requested.")
    void createMultipleSoftwareModuleTypes() {
        final List<SoftwareModuleType> created = softwareModuleTypeManagement
                .create(Arrays.asList(entityFactory.softwareModuleType().create().key("thetype").name("thename"),
                        entityFactory.softwareModuleType().create().key("thetype2").name("thename2")));

        assertThat(created).as("Number of created types").hasSize(2);
        assertThat(softwareModuleTypeManagement.count()).as("Number of types in repository").isEqualTo(5);
    }

}

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

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement.Create;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Software Module Management
 */
class SoftwareModuleTypeManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Verifies that management get access reacts as specfied on calls for non existing entities by means
     * of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class) })
    void nonExistingEntityAccessReturnsNotPresent() {

        assertThat(softwareModuleTypeManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(softwareModuleTypeManagement.findByKey(NOT_EXIST_ID)).isNotPresent();
        assertThat(softwareModuleTypeManagement.findByName(NOT_EXIST_ID)).isNotPresent();
    }

    /**
     * Verifies that management queries react as specfied on calls for non existing entities
     * by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        verifyThrownExceptionBy(() -> softwareModuleTypeManagement.delete(NOT_EXIST_IDL), "SoftwareModuleType");

        verifyThrownExceptionBy(
                () -> softwareModuleTypeManagement.update(SoftwareModuleTypeManagement.Update.builder().id(NOT_EXIST_IDL).build()),
                "SoftwareModuleType");
    }

    /**
     * Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.
     */
    @Test
    void updateNothingResultsInUnchangedRepositoryForType() {
        final SoftwareModuleType created = softwareModuleTypeManagement.create(Create.builder().key("test-key").name("test-name").build());

        final SoftwareModuleType updated = softwareModuleTypeManagement.update(SoftwareModuleTypeManagement.Update.builder().id(created.getId())
                .build());

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entitity to be equal to created version")
                .isEqualTo(created.getOptLockRevision());
    }

    /**
     * Calling update for changed fields results in change in the repository.
     */
    @Test
    void updateSoftwareModuleTypeFieldsToNewValue() {
        final SoftwareModuleType created = softwareModuleTypeManagement
                .create(Create.builder().key("test-key").name("test-name").build());

        final SoftwareModuleType updated = softwareModuleTypeManagement.update(
                SoftwareModuleTypeManagement.Update.builder().id(created.getId()).description("changed").colour("changed").build());

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entities is")
                .isEqualTo(created.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo("changed");
        assertThat(updated.getColour()).as("Updated vendor is").isEqualTo("changed");
    }

    /**
     * Create Software Module Types call fails when called for existing entities.
     */
    @Test
    void createModuleTypesCallFailsForExistingTypes() {
        final List<Create> created = Arrays.asList(
                Create.builder().key("test-key").name("test-name").build(),
                Create.builder().key("test-key2").name("test-name2").build());
        softwareModuleTypeManagement.create(created);
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("should not have worked as module type already exists")
                .isThrownBy(() -> softwareModuleTypeManagement.create(created));
    }

    /**
     * Tests the successfull deletion of software module types. Both unused (hard delete) and used ones (soft delete).
     */
    @Test
    void deleteAssignedAndUnassignedSoftwareModuleTypes() {
        Assertions.<SoftwareModuleType>assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(3).contains(osType, runtimeType, appType);

        SoftwareModuleType type = softwareModuleTypeManagement
                .create(Create.builder().key("bundle").name("OSGi Bundle").build());

        Assertions.<SoftwareModuleType>assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(4).contains(osType, runtimeType, appType, type);

        // delete unassigned
        softwareModuleTypeManagement.delete(type.getId());
        Assertions.<SoftwareModuleType>assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(3).contains(osType, runtimeType, appType);
        Assertions.<SoftwareModuleType>assertThat(softwareModuleTypeRepository.findAll()).hasSize(3).contains(osType, runtimeType, appType);

        type = softwareModuleTypeManagement
                .create(Create.builder().key("bundle2").name("OSGi Bundle2").build());

        Assertions.<SoftwareModuleType>assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(4).contains(osType, runtimeType, appType, type);

        softwareModuleManagement
                .create(SoftwareModuleManagement.Create.builder().type(type).name("Test SM").version("1.0").build());

        // delete assigned
        softwareModuleTypeManagement.delete(type.getId());
        Assertions.<SoftwareModuleType>assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(3).contains(osType, runtimeType, appType);
        Assertions.<SoftwareModuleType>assertThat(softwareModuleTypeManagement.findByRsql("name==*", PAGE)).hasSize(3).contains(osType, runtimeType, appType);
        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);

        assertThat(softwareModuleTypeRepository.findAll()).hasSize(4).contains((JpaSoftwareModuleType) osType,
                (JpaSoftwareModuleType) runtimeType, (JpaSoftwareModuleType) appType,
                softwareModuleTypeRepository.findById(type.getId()).orElseThrow());
    }

    /**
     * Checks that software module typeis found based on given name.
     */
    @Test
    void findSoftwareModuleTypeByName() {
        testdataFactory.createSoftwareModuleOs();
        final SoftwareModuleType found = softwareModuleTypeManagement
                .create(Create.builder().key("thetype").name("thename").build());
        softwareModuleTypeManagement
                .create(Create.builder().key("thetype2").name("anothername").build());

        Assertions.<SoftwareModuleType>assertThat(((SoftwareModuleTypeManagement<SoftwareModuleType>) softwareModuleTypeManagement).findByName("thename"))
                .as("Type with given name").contains(found);
    }

    /**
     * Verifies that it is not possible to create a type that alrady exists.
     */
    @Test
    void createSoftwareModuleTypeFailsWithExistingEntity() {
        final Create create = Create.builder().key("thetype").name("thename").build();
        softwareModuleTypeManagement.create(create);
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("should not have worked as module type already exists")
                .isThrownBy(() -> softwareModuleTypeManagement.create(create));
    }

    /**
     * Verifies that it is not possible to create a list of types where one already exists.
     */
    @Test
    void createSoftwareModuleTypesFailsWithExistingEntity() {
        softwareModuleTypeManagement.create(Create.builder().key("thetype").name("thename").build());
        final List<Create> creates = List.of(
                Create.builder().key("thetype").name("thename").build(),
                Create.builder().key("anothertype").name("anothername").build());
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("should not have worked as module type already exists")
                .isThrownBy(() -> softwareModuleTypeManagement.create(creates));
    }

    /**
     * Verifies that the creation of a softwareModuleType is failing because of invalid max assignment
     */
    @Test
    void createSoftwareModuleTypesFailsWithInvalidMaxAssignment() {
        final Create create =
                Create.builder().key("type").name("name").maxAssignments(0).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("should not have worked as max assignment is invalid. Should be greater than 0")
                .isThrownBy(() -> softwareModuleTypeManagement.create(create));
    }

    /**
     * Verifies that multiple types are created as requested.
     */
    @Test
    void createMultipleSoftwareModuleTypes() {
        final List<? extends SoftwareModuleType> created = softwareModuleTypeManagement
                .create(List.of(
                        Create.builder().key("thetype").name("thename").build(),
                        Create.builder().key("thetype2").name("thename2").build()));

        assertThat(created).as("Number of created types").hasSize(2);
        assertThat(softwareModuleTypeManagement.count()).as("Number of types in repository").isEqualTo(5);
    }
}
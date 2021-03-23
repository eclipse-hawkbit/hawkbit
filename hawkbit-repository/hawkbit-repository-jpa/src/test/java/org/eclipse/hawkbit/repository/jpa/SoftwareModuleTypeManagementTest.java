/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("Software Module Management")
public class SoftwareModuleTypeManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 0) })
    public void nonExistingEntityAccessReturnsNotPresent() {

        assertThat(softwareModuleTypeManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(softwareModuleTypeManagement.getByKey(NOT_EXIST_ID)).isNotPresent();
        assertThat(softwareModuleTypeManagement.getByName(NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 0) })
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        verifyThrownExceptionBy(() -> softwareModuleTypeManagement.delete(NOT_EXIST_IDL), "SoftwareModuleType");

        verifyThrownExceptionBy(
                () -> softwareModuleTypeManagement.update(entityFactory.softwareModuleType().update(NOT_EXIST_IDL)),
                "SoftwareModuleType");
    }

    @Test
    @Description("Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.")
    public void updateNothingResultsInUnchangedRepositoryForType() {
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
    public void updateSoftareModuleTypeFieldsToNewValue() {
        final SoftwareModuleType created = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("test-key").name("test-name"));

        final SoftwareModuleType updated = softwareModuleTypeManagement.update(
                entityFactory.softwareModuleType().update(created.getId()).description("changed").colour("changed"));

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entitity is")
                .isEqualTo(created.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo("changed");
        assertThat(updated.getColour()).as("Updated vendor is").isEqualTo("changed");
    }

    @Test
    @Description("Create Software Module Types call fails when called for existing entities.")
    public void createModuleTypesCallFailsForExistingTypes() {
        final List<SoftwareModuleTypeCreate> created = Arrays.asList(
                entityFactory.softwareModuleType().create().key("test-key").name("test-name"),
                entityFactory.softwareModuleType().create().key("test-key2").name("test-name2"));

        softwareModuleTypeManagement.create(created);
        try {
            softwareModuleTypeManagement.create(created);
            fail("Should not have worked as module already exists.");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Tests the successfull deletion of software module types. Both unused (hard delete) and used ones (soft delete).")
    public void deleteAssignedAndUnassignedSoftwareModuleTypes() {
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

        assertThat(softwareModuleTypeRepository.findAll()).hasSize(4).contains((JpaSoftwareModuleType) osType,
                (JpaSoftwareModuleType) runtimeType, (JpaSoftwareModuleType) appType,
                softwareModuleTypeRepository.findById(type.getId()).get());
    }

    @Test
    @Description("Checks that software module typeis found based on given name.")
    public void findSoftwareModuleTypeByName() {
        testdataFactory.createSoftwareModuleOs();
        final SoftwareModuleType found = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("thetype").name("thename"));
        softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key("thetype2").name("anothername"));

        assertThat(softwareModuleTypeManagement.getByName("thename").get()).as("Type with given name").isEqualTo(found);
    }

    @Test
    @Description("Verfies that it is not possible to create a type that alrady exists.")
    public void createSoftwareModuleTypeFailsWithExistingEntity() {
        softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create().key("thetype").name("thename"));
        try {
            softwareModuleTypeManagement
                    .create(entityFactory.softwareModuleType().create().key("thetype").name("thename"));
            fail("should not have worked as module type already exists");
        } catch (final EntityAlreadyExistsException e) {

        }

    }

    @Test
    @Description("Verfies that it is not possible to create a list of types where one already exists.")
    public void createSoftwareModuleTypesFailsWithExistingEntity() {
        softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create().key("thetype").name("thename"));
        try {
            softwareModuleTypeManagement
                    .create(Arrays.asList(entityFactory.softwareModuleType().create().key("thetype").name("thename"),
                            entityFactory.softwareModuleType().create().key("anothertype").name("anothername")));
            fail("should not have worked as module type already exists");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Verifies that the creation of a softwareModuleType is failing because of invalid max assignment")
    public void createSoftwareModuleTypesFailsWithInvalidMaxAssignment() {
        try {
            softwareModuleTypeManagement
                    .create(entityFactory.softwareModuleType().create().key("type").name("name").maxAssignments(0));
            fail("should not have worked as max assignment is invalid. Should be greater than 0.");
        } catch (final ConstraintViolationException e) {

        }
    }

    @Test
    @Description("Verfies that multiple types are created as requested.")
    public void createMultipleSoftwareModuleTypes() {
        final List<SoftwareModuleType> created = softwareModuleTypeManagement
                .create(Arrays.asList(entityFactory.softwareModuleType().create().key("thetype").name("thename"),
                        entityFactory.softwareModuleType().create().key("thetype2").name("thename2")));

        assertThat(created.size()).as("Number of created types").isEqualTo(2);
        assertThat(softwareModuleTypeManagement.count()).as("Number of types in repository").isEqualTo(5);
    }

}

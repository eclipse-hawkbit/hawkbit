/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Optional;

import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("Target Type Management")
class TargetTypeManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access react as specified on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetTypeCreatedEvent.class) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(targetTypeManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(targetTypeManagement.getByName(NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specified on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = TargetTypeUpdatedEvent.class) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        verifyThrownExceptionBy(() -> targetTypeManagement.delete(NOT_EXIST_IDL), "TargetType");
        verifyThrownExceptionBy(() -> targetTypeManagement.update(entityFactory.targetType().update(NOT_EXIST_IDL)),
                "TargetType");
    }

    @Test
    @Description("Verify that a target type with invalid properties cannot be created or updated")
    void createAndUpdateTargetTypeWithInvalidFields() {
        final TargetType targetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("targettype1").description("targettypedes1"));

        createAndUpdateTargetTypeWithInvalidDescription(targetType);
        createAndUpdateTargetTypeWithInvalidColour(targetType);
        createAndUpdateTargetTypeWithInvalidName(targetType);
    }

    @Step
    void createAndUpdateTargetTypeWithInvalidDescription(final TargetType targetType) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long description should not be created")
                .isThrownBy(() -> targetTypeManagement.create(
                        entityFactory.targetType().create().name("a").description(
                                RandomStringUtils.randomAlphanumeric(TargetType.DESCRIPTION_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid description should not be created").isThrownBy(() -> targetTypeManagement
                        .create(entityFactory.targetType().create().name("a").description(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long description should not be updated")
                .isThrownBy(() -> targetTypeManagement.update(
                        entityFactory.targetType().update(targetType.getId()).description(
                                RandomStringUtils.randomAlphanumeric(TargetType.DESCRIPTION_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid description should not be updated")
                .isThrownBy(() -> targetTypeManagement
                        .update(entityFactory.targetType().update(targetType.getId()).description(INVALID_TEXT_HTML)));
    }

    @Step
    private void createAndUpdateTargetTypeWithInvalidColour(final TargetType targetType) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long colour should not be created")
                .isThrownBy(() -> targetTypeManagement.create(
                        entityFactory.targetType().create().name("a")
                                .colour(RandomStringUtils.randomAlphanumeric(TargetType.COLOUR_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid colour should not be created").isThrownBy(() -> targetTypeManagement
                        .create(entityFactory.targetType().create().name("a").colour(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long colour should not be updated")
                .isThrownBy(() -> targetTypeManagement.update(
                        entityFactory.targetType().update(targetType.getId())
                                .colour(RandomStringUtils.randomAlphanumeric(TargetType.COLOUR_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid colour should not be updated").isThrownBy(() -> targetTypeManagement
                        .update(entityFactory.targetType().update(targetType.getId()).colour(INVALID_TEXT_HTML)));
    }

    @Step
    private void createAndUpdateTargetTypeWithInvalidName(final TargetType targetType) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long name should not be created")
                .isThrownBy(() -> targetTypeManagement
                        .create(entityFactory.targetType().create().name(RandomStringUtils.randomAlphanumeric(
                                NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid name should not be created").isThrownBy(
                        () -> targetTypeManagement.create(entityFactory.targetType().create().name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long name should not be updated")
                .isThrownBy(() -> targetTypeManagement
                        .update(entityFactory.targetType().update(targetType.getId()).name(RandomStringUtils.randomAlphanumeric(
                                NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid name should not be updated").isThrownBy(() -> targetTypeManagement
                        .update(entityFactory.targetType().update(targetType.getId()).name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too short name should not be updated").isThrownBy(() -> targetTypeManagement
                        .update(entityFactory.targetType().update(targetType.getId()).name("")));

    }

    @Test
    @Description("Tests the successful assignment of compatible distribution set types to a target type")
    void assignCompatibleDistributionSetTypesToTargetType() {
        final TargetType targetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("targettype1").description("targettypedes1"));
        DistributionSetType distributionSetType = testdataFactory.findOrCreateDistributionSetType("testDst", "dst1");
        targetTypeManagement.assignCompatibleDistributionSetTypes(targetType.getId(), Collections.singletonList(distributionSetType.getId()));

        Optional<JpaTargetType> targetTypeWithDsTypes = targetTypeRepository.findById(targetType.getId());
        assertThat(targetTypeWithDsTypes).isPresent();
        assertThat(targetTypeWithDsTypes.get().getCompatibleDistributionSetTypes()).extracting("key").contains("testDst");
    }

    @Test
    @Description("Tests the successful removal of compatible distribution set types to a target type")
    void unassignCompatibleDistributionSetTypesToTargetType() {
        final TargetType targetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("targettype11").description("targettypedes11"));
        DistributionSetType distributionSetType = testdataFactory.findOrCreateDistributionSetType("testDst1", "dst11");
        targetTypeManagement.assignCompatibleDistributionSetTypes(targetType.getId(), Collections.singletonList(distributionSetType.getId()));
        Optional<JpaTargetType> targetTypeWithDsTypes = targetTypeRepository.findById(targetType.getId());
        assertThat(targetTypeWithDsTypes).isPresent();
        assertThat(targetTypeWithDsTypes.get().getCompatibleDistributionSetTypes()).extracting("key").contains("testDst1");
        targetTypeManagement.unassignDistributionSetType(targetType.getId(),distributionSetType.getId());
        Optional<JpaTargetType> targetTypeWithDsTypes1 = targetTypeRepository.findById(targetType.getId());
        assertThat(targetTypeWithDsTypes1).isPresent();
        assertThat(targetTypeWithDsTypes1.get().getCompatibleDistributionSetTypes()).isEmpty();
    }

    @Test
    @Description("Ensures that all types are retrieved through repository.")
    void findAllTargetTypes() {
        testdataFactory.createTargetTypes("targettype", 10);
        assertThat(targetTypeRepository.findAll()).as("Target type size").hasSize(10);
    }

    @Test
    @Description("Ensures that a created target type is persisted in the repository as defined.")
    void createTargetType() {
        final TargetType targetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("targettype1").description("targettypedes1").colour("colour1"));

        assertThat(targetTypeRepository.findByName("targettype1").get().getDescription()).as("type found")
                .isEqualTo("targettypedes1");
        assertThat(targetTypeManagement.getByName("targettype1").get().getColour()).as("type found").isEqualTo("colour1");
        assertThat(targetTypeManagement.get(targetType.getId()).get().getColour()).as("type found").isEqualTo("colour1");
    }

    @Test
    @Description("Ensures that a deleted target type is removed from the repository as defined.")
    void deleteTargetType() {
        // create test data
        final TargetType targetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("targettype11").description("targettypedes11"));
        assertThat(targetTypeRepository.findByName("targettype11").get().getDescription()).as("type found")
                .isEqualTo("targettypedes11");
        targetTypeManagement.delete(targetType.getId());
        assertThat(targetTypeRepository.findById(targetType.getId())).as("No target type should be found").isNotPresent();

    }

    @Test
    @Description("Tests the name update of a target type.")
    void updateTargetType() {
        final TargetType targetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("targettype111").description("targettypedes111"));
        assertThat(targetTypeRepository.findByName("targettype111").get().getDescription()).as("type found")
                .isEqualTo("targettypedes111");
        targetTypeManagement.update(entityFactory.targetType().update(targetType.getId()).name("updatedtargettype111"));
        assertThat(targetTypeRepository.findByName("updatedtargettype111")).as("Updated target type should be found").isPresent();
    }

    @Test
    @Description("Ensures that a target type cannot be created if one exists already with that name (expects EntityAlreadyExistsException).")
    void failedDuplicateTargetTypeNameException() {
        targetTypeManagement.create(entityFactory.targetType().create().name("targettype123"));
        assertThrows(EntityAlreadyExistsException.class, () -> targetTypeManagement.create(entityFactory.targetType().create().name("targettype123")));
    }

    @Test
    @Description("Ensures that a target type cannot be updated to a name that already exists (expects EntityAlreadyExistsException).")
    void failedDuplicateTargetTypeNameExceptionAfterUpdate() {
        targetTypeManagement.create(entityFactory.targetType().create().name("targettype1234"));
        TargetType targetType = targetTypeManagement.create(entityFactory.targetType().create().name("targettype12345"));
        assertThrows(EntityAlreadyExistsException.class, () -> targetTypeManagement.update(entityFactory.targetType().update(targetType.getId()).name("targettype1234")));
    }

}

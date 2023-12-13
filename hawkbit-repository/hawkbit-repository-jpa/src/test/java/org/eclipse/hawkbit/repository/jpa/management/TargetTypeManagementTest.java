/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Optional;

import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.Type;
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
                .create(entityFactory.targetType().create()
                        .name("targettype1").description("targettypedes1")
                        .key("targettype1.key"));

        createAndUpdateTargetTypeWithInvalidDescription(targetType);
        createAndUpdateTargetTypeWithInvalidColour(targetType);
        createTargetTypeWithInvalidKey();
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
    private void createTargetTypeWithInvalidKey() {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long key should not be created")
                .isThrownBy(() -> targetTypeManagement
                        .create(entityFactory.targetType().create().name(RandomStringUtils.randomAlphanumeric(
                                Type.KEY_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid key should not be created").isThrownBy(
                        () -> targetTypeManagement.create(entityFactory.targetType().create().name(INVALID_TEXT_HTML)));
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
                .create(entityFactory.targetType().create()
                        .name("targettype1").description("targettypedes1")
                        .key("targettyp1.key"));
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
                .create(entityFactory.targetType().create()
                        .name("targettype1").description("targettypedes1")
                        .key("targettyp1.key"));
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
        final String name = "targettype1";
        final String key = "targettype1.key";
        targetTypeManagement
                .create(entityFactory.targetType().create()
                        .name(name)
                        .description("targettypedes1")
                        .key(key)
                        .colour("colour1"));

        assertThat(findByName(name).map(JpaTargetType::getName).orElse(null)).as("type found (name)")
                .isEqualTo(name);
        assertThat(findByName(name).map(JpaTargetType::getDescription).orElse(null))
                .as("type found (des)").isEqualTo("targettypedes1");
        assertThat(findByName(name).map(JpaTargetType::getKey).orElse(null)).as("type found (key)")
                .isEqualTo(key);
        assertThat(findByName(name).map(JpaTargetType::getColour).orElse(null))
                .as("type found (colour)").isEqualTo("colour1");
        assertThat(findByKey(key).map(JpaTargetType::getName).orElse(null)).as("type found (name)")
                .isEqualTo(name);
        assertThat(findByKey(key).map(JpaTargetType::getDescription).orElse(null))
                .as("type found (des)").isEqualTo("targettypedes1");
        assertThat(findByKey(key).map(JpaTargetType::getKey).orElse(null)).as("type found (key)")
                .isEqualTo(key);
        assertThat(findByKey(key).map(JpaTargetType::getColour).orElse(null)).as("type found (colour)")
                .isEqualTo("colour1");
    }

    @Test
    @Description("Ensures that a deleted target type is removed from the repository as defined.")
    void deleteTargetType() {
        // create test data
        final TargetType targetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("targettype11").description("targettypedes11"));
        assertThat(findByName("targettype11").get().getDescription()).as("type found")
                .isEqualTo("targettypedes11");
        targetTypeManagement.delete(targetType.getId());
        assertThat(targetTypeRepository.findById(targetType.getId())).as("No target type should be found").isNotPresent();

    }

    @Test
    @Description("Tests the name update of a target type.")
    void updateTargetType() {
        final TargetType targetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("targettype111").description("targettypedes111"));
        assertThat(findByName("targettype111").get().getDescription()).as("type found")
                .isEqualTo("targettypedes111");
        targetTypeManagement.update(entityFactory.targetType().update(targetType.getId()).name("updatedtargettype111"));
        assertThat(findByName("updatedtargettype111")).as("Updated target type should be found").isPresent();
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

    private Optional<JpaTargetType> findByName(final String name) {
        return targetTypeManagement.getByName(name).map(JpaTargetType.class::cast);
    }

    private Optional<JpaTargetType> findByKey(final String key) {
        return targetTypeManagement.getByKey(key).map(JpaTargetType.class::cast);
    }
}

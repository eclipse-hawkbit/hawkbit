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

import jakarta.validation.ConstraintViolationException;

import org.eclipse.hawkbit.repository.TargetTypeManagement.Create;
import org.eclipse.hawkbit.repository.TargetTypeManagement.Update;
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

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Target Type Management
 */
class TargetTypeManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Verifies that management get access react as specified on calls for non existing entities by means 
     * of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetTypeCreatedEvent.class) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(targetTypeManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(targetTypeManagement.getByName(NOT_EXIST_ID)).isNotPresent();
    }

    /**
     * Verifies that management queries react as specified on calls for non existing entities 
     *  by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetTypeUpdatedEvent.class) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        verifyThrownExceptionBy(() -> targetTypeManagement.delete(NOT_EXIST_IDL), "TargetType");
        verifyThrownExceptionBy(() -> targetTypeManagement.update(Update.builder().id(NOT_EXIST_IDL).build()),"TargetType");
    }

    /**
     * Verify that a target type with invalid properties cannot be created or updated
     */
    @Test
    void createAndUpdateTargetTypeWithInvalidFields() {
        final TargetType targetType = targetTypeManagement
                .create(Create.builder().name("targettype1").description("targettypedes1").key("targettype1.key").build());

        createAndUpdateTargetTypeWithInvalidDescription(targetType);
        createAndUpdateTargetTypeWithInvalidColour(targetType);
        createTargetTypeWithInvalidKey();
        createAndUpdateTargetTypeWithInvalidName(targetType);
    }

    void createAndUpdateTargetTypeWithInvalidDescription(final TargetType targetType) {
        final Create targetTypeCreateTooLong = Create.builder()
                .name("a").description(randomString(TargetType.DESCRIPTION_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long description should not be created")
                .isThrownBy(() -> targetTypeManagement.create(targetTypeCreateTooLong));

        final Create targetTypeCreateInvalidHtml = Create.builder().name("a").description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid description should not be created")
                .isThrownBy(() -> targetTypeManagement.create(targetTypeCreateInvalidHtml));

        final Update targetTypeUpdateTooLong = Update.builder().id(targetType.getId())
                .description(randomString(TargetType.DESCRIPTION_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long description should not be updated")
                .isThrownBy(() -> targetTypeManagement.update(targetTypeUpdateTooLong));

        final Update targetTypeUpdateInvalidHtml = Update.builder().id(targetType.getId()).description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid description should not be updated")
                .isThrownBy(() -> targetTypeManagement.update(targetTypeUpdateInvalidHtml));
    }

    /**
     * Tests the successful assignment of compatible distribution set types to a target type
     */
    @Test
    void assignCompatibleDistributionSetTypesToTargetType() {
        final TargetType targetType = targetTypeManagement
                .create(Create.builder()
                        .name("targettype1").description("targettypedes1").key("targettyp1.key").build());
        DistributionSetType distributionSetType = testdataFactory.findOrCreateDistributionSetType("testDst", "dst1");
        targetTypeManagement.assignCompatibleDistributionSetTypes(targetType.getId(), Collections.singletonList(distributionSetType.getId()));

        Optional<JpaTargetType> targetTypeWithDsTypes = targetTypeRepository.findById(targetType.getId());
        assertThat(targetTypeWithDsTypes).isPresent();
        assertThat(targetTypeWithDsTypes.get().getDistributionSetTypes()).extracting("key").contains("testDst");
    }

    /**
     * Tests the successful removal of compatible distribution set types to a target type
     */
    @Test
    void unassignCompatibleDistributionSetTypesToTargetType() {
        final TargetType targetType = targetTypeManagement
                .create(Create.builder().name("targettype1").description("targettypedes1").key("targettyp1.key").build());
        DistributionSetType distributionSetType = testdataFactory.findOrCreateDistributionSetType("testDst1", "dst11");
        targetTypeManagement.assignCompatibleDistributionSetTypes(targetType.getId(), Collections.singletonList(distributionSetType.getId()));
        Optional<JpaTargetType> targetTypeWithDsTypes = targetTypeRepository.findById(targetType.getId());
        assertThat(targetTypeWithDsTypes).isPresent();
        assertThat(targetTypeWithDsTypes.get().getDistributionSetTypes()).extracting("key").contains("testDst1");
        targetTypeManagement.unassignDistributionSetType(targetType.getId(), distributionSetType.getId());
        Optional<JpaTargetType> targetTypeWithDsTypes1 = targetTypeRepository.findById(targetType.getId());
        assertThat(targetTypeWithDsTypes1).isPresent();
        assertThat(targetTypeWithDsTypes1.get().getDistributionSetTypes()).isEmpty();
    }

    /**
     * Ensures that all types are retrieved through repository.
     */
    @Test
    void findAllTargetTypes() {
        testdataFactory.createTargetTypes("targettype", 10);
        assertThat(targetTypeRepository.findAll()).as("Target type size").hasSize(10);
    }

    /**
     * Ensures that a created target type is persisted in the repository as defined.
     */
    @Test
    void createTargetType() {
        final String name = "targettype1";
        final String key = "targettype1.key";
        targetTypeManagement.create(Create.builder().name(name).description("targettypedes1").key(key).colour("colour1").build());

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

    /**
     * Ensures that a deleted target type is removed from the repository as defined.
     */
    @Test
    void deleteTargetType() {
        // create test data
        final TargetType targetType = targetTypeManagement.create(Create.builder().name("targettype11").description("targettypedes11").build());
        assertThat(findByName("targettype11").get().getDescription()).as("type found").isEqualTo("targettypedes11");
        targetTypeManagement.delete(targetType.getId());
        assertThat(targetTypeRepository.findById(targetType.getId())).as("No target type should be found").isNotPresent();

    }

    /**
     * Tests the name update of a target type.
     */
    @Test
    void updateTargetType() {
        final TargetType targetType =
                targetTypeManagement.create(Create.builder().name("targettype111").description("targettypedes111").build());
        assertThat(findByName("targettype111").get().getDescription()).as("type found").isEqualTo("targettypedes111");
        targetTypeManagement.update(Update.builder().id(targetType.getId()).name("updatedtargettype111").build());
        assertThat(findByName("updatedtargettype111")).as("Updated target type should be found").isPresent();
    }

    /**
     * Ensures that a target type cannot be created if one exists already with that name (expects EntityAlreadyExistsException).
     */
    @Test
    void failedDuplicateTargetTypeNameException() {
        final Create targetTypeCreate = Create.builder().name("targettype123").build();
        targetTypeManagement.create(targetTypeCreate);
        assertThrows(EntityAlreadyExistsException.class, () -> targetTypeManagement.create(targetTypeCreate));
    }

    /**
     * Ensures that a target type cannot be updated to a name that already exists (expects EntityAlreadyExistsException).
     */
    @Test
    void failedDuplicateTargetTypeNameExceptionAfterUpdate() {
        targetTypeManagement.create(Create.builder().name("targettype1234").build());
        TargetType targetType = targetTypeManagement.create(Create.builder().name("targettype12345").build());
        assertThrows(EntityAlreadyExistsException.class,
                () -> targetTypeManagement.update(Update.builder().id(targetType.getId()).name("targettype1234").build()));
    }

    private void createAndUpdateTargetTypeWithInvalidColour(final TargetType targetType) {
        final Create targetTypeCreateTooLong = Create.builder().name("a").colour(randomString(Type.COLOUR_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long colour should not be created")
                .isThrownBy(() -> targetTypeManagement.create(targetTypeCreateTooLong));

        final Create targetTypeCreateInvalidHtml = Create.builder().name("a").colour(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid colour should not be created")
                .isThrownBy(() -> targetTypeManagement.create(targetTypeCreateInvalidHtml));

        final Update targetTypeUpdateTooLong = Update.builder()
                .id(targetType.getId()).colour(randomString(Type.COLOUR_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long colour should not be updated")
                .isThrownBy(() -> targetTypeManagement.update(targetTypeUpdateTooLong));

        final Update targetTypeUpdateInvalidHtml = Update.builder().id(targetType.getId()).colour(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid colour should not be updated")
                .isThrownBy(() -> targetTypeManagement.update(targetTypeUpdateInvalidHtml));
    }

    private void createTargetTypeWithInvalidKey() {
        final Create targetTypeCreateTooLong = Create.builder().name(randomString(Type.KEY_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long key should not be created")
                .isThrownBy(() -> targetTypeManagement.create(targetTypeCreateTooLong));

        final Create targetTypeCreateInvalidHtmle = Create.builder().name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid key should not be created")
                .isThrownBy(() -> targetTypeManagement.create(targetTypeCreateInvalidHtmle));
    }

    private void createAndUpdateTargetTypeWithInvalidName(final TargetType targetType) {
        final Create targetTypeCreateTooLong = Create.builder().name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long name should not be created")
                .isThrownBy(() -> targetTypeManagement
                        .create(targetTypeCreateTooLong));

        final Create targetTypeCreateInvalidHtml = Create.builder().name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid name should not be created")
                .isThrownBy(() -> targetTypeManagement.create(targetTypeCreateInvalidHtml));

        final Update targetTypeUpdateTooLong = Update.builder()
                .id(targetType.getId()).name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too long name should not be updated")
                .isThrownBy(() -> targetTypeManagement.update(targetTypeUpdateTooLong));

        final Update targetTypeUpdateInvalidHtml = Update.builder().id(targetType.getId()).name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with invalid name should not be updated")
                .isThrownBy(() -> targetTypeManagement.update(targetTypeUpdateInvalidHtml));

        final Update targetTypeUpdateEmpty = Update.builder().id(targetType.getId()).name("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("targetType with too short name should not be updated")
                .isThrownBy(() -> targetTypeManagement.update(targetTypeUpdateEmpty));

    }

    private Optional<JpaTargetType> findByName(final String name) {
        return targetTypeManagement.getByName(name).map(JpaTargetType.class::cast);
    }

    private Optional<JpaTargetType> findByKey(final String key) {
        return targetTypeManagement.getByKey(key).map(JpaTargetType.class::cast);
    }
}

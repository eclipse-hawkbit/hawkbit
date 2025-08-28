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
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
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
class TargetTypeManagementTest extends AbstractRepositoryManagementTest<TargetType, Create, Update> {

    /**
     * Tests the successful assignment of compatible distribution set types to a target type
     */
    @Test
    void assignCompatibleDistributionSetTypesToTargetType() {
        final TargetType targetType = targetTypeManagement.create(
                Create.builder().name("targettype1").description("targettypedes1").key("targettyp1.key").build());
        final DistributionSetType distributionSetType = testdataFactory.findOrCreateDistributionSetType("testDst", "dst1");
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

    @Test
    @ExpectEvents({ @Expect(type = TargetTypeUpdatedEvent.class) })
    void failIfReferNotExistingEntity() {
        verifyThrownExceptionBy(() -> targetTypeManagement.delete(NOT_EXIST_IDL), "TargetType");
        verifyThrownExceptionBy(() -> targetTypeManagement.update(Update.builder().id(NOT_EXIST_IDL).build()),"TargetType");
    }

    /**
     * Verify that a target type with invalid properties cannot be created or updated
     */
    @Test
    void failToCreateAndUpdateTargetTypeWithInvalidFields() {
        final TargetType targetType = targetTypeManagement
                .create(Create.builder().name("targettype1").description("targettypedes1").key("targettype1.key").build());

        createAndUpdateTargetTypeWithInvalidDescription(targetType);
        createAndUpdateTargetTypeWithInvalidColour(targetType);
        createTargetTypeWithInvalidKey();
        createAndUpdateTargetTypeWithInvalidName(targetType);
    }

    /**
     * Ensures that a target type cannot be updated to a name that already exists (expects EntityAlreadyExistsException).
     */
    @Test
    void failToDuplicateTargetTypeNameExceptionOnUpdate() {
        targetTypeManagement.create(Create.builder().name("targettype1234").build());
        final TargetType targetType = targetTypeManagement.create(Create.builder().name("targettype12345").build());
        final Update update = Update.builder().id(targetType.getId()).name("targettype1234").build();
        assertThrows(EntityAlreadyExistsException.class, () -> targetTypeManagement.update(update));
    }

    private void createAndUpdateTargetTypeWithInvalidDescription(final TargetType targetType) {
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
}

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

import java.util.List;

import jakarta.validation.ConstraintViolationException;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement.Create;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement.Update;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.junit.jupiter.api.Test;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Software Module Management
 */
class SoftwareModuleTypeManagementTest extends AbstractRepositoryManagementTest<SoftwareModuleType, Create, Update> {

    @Test
    void failIfReferNotExistingEntity() {
        assertThat(softwareModuleTypeManagement.findByKey(NOT_EXIST_ID)).isNotPresent();
        verifyThrownExceptionBy(() -> softwareModuleTypeManagement.delete(NOT_EXIST_IDL), "SoftwareModuleType");
        verifyThrownExceptionBy(() -> softwareModuleTypeManagement.update(Update.builder().id(NOT_EXIST_IDL).build()), "SoftwareModuleType");
    }

    /**
     * Tests the successful deletion of software module types. Both unused (hard delete) and used ones (soft delete).
     */
    @Test
    void deleteAssignedAndUnassignedSoftwareModuleTypes() {
        Assertions.<SoftwareModuleType> assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(3)
                .contains(osType, runtimeType, appType);

        SoftwareModuleType type = softwareModuleTypeManagement.create(Create.builder().key("bundle").name("OSGi Bundle").build());

        Assertions.<SoftwareModuleType> assertThat(softwareModuleTypeManagement.findAll(PAGE))
                .hasSize(4)
                .contains(osType, runtimeType, appType, type);

        // delete unassigned
        softwareModuleTypeManagement.delete(type.getId());
        Assertions.<SoftwareModuleType> assertThat(softwareModuleTypeManagement.findAll(PAGE))
                .hasSize(3)
                .contains(osType, runtimeType, appType);
        Assertions.<SoftwareModuleType> assertThat(softwareModuleTypeRepository.findAll()).hasSize(3).contains(osType, runtimeType, appType);

        type = softwareModuleTypeManagement.create(Create.builder().key("bundle2").name("OSGi Bundle2").build());

        Assertions.<SoftwareModuleType> assertThat(softwareModuleTypeManagement.findAll(PAGE))
                .hasSize(4)
                .contains(osType, runtimeType, appType, type);

        softwareModuleManagement.create(SoftwareModuleManagement.Create.builder().type(type).name("Test SM").version("1.0").build());

        // delete assigned
        softwareModuleTypeManagement.delete(type.getId());
        Assertions.<SoftwareModuleType> assertThat(softwareModuleTypeManagement.findAll(PAGE))
                .hasSize(3)
                .contains(osType, runtimeType, appType);
        Assertions.<SoftwareModuleType> assertThat(softwareModuleTypeManagement.findByRsql("name==*", PAGE))
                .hasSize(3)
                .contains(osType, runtimeType, appType);
        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);

        assertThat(softwareModuleTypeRepository.findAll())
                .hasSize(4)
                .contains(
                        (JpaSoftwareModuleType) osType,
                        (JpaSoftwareModuleType) runtimeType,
                        (JpaSoftwareModuleType) appType,
                        softwareModuleTypeRepository.findById(type.getId()).orElseThrow());
    }

    /**
     * Verifies that the creation of a softwareModuleType is failing because of invalid max assignment
     */
    @Test
    void createSoftwareModuleTypesFailsWithInvalidMaxAssignment() {
        final Create create = Create.builder().key("type").name("name").maxAssignments(0).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("should not have worked as max assignment is invalid. Should be greater than 0")
                .isThrownBy(() -> softwareModuleTypeManagement.create(create));
    }
}
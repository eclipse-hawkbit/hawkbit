/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.hawkbit.auth.SpPermission.CREATE_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.DELETE_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.READ_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.SOFTWARE_MODULE_TYPE;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_PREFIX;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement.Update;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.junit.jupiter.api.Test;

class SoftwareModuleTypeManagementTest extends AbstractAccessControllerManagementTest {

    @Test
    void verifyCreate() {
        // permissions to read all and create new type with name 'permitted'
        runAs(withAuthorities(READ_PREFIX + SOFTWARE_MODULE_TYPE, CREATE_PREFIX + SOFTWARE_MODULE_TYPE + "/key==permitted"), () -> {
            assertThat(testdataFactory.findOrCreateSoftwareModuleType("permitted")).isNotNull();
            assertThatThrownBy(() -> testdataFactory.findOrCreateSoftwareModuleType("not_permitted_2"))
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }

    @Test
    void verifyRead() {
        // permissions to read only type1 sm types
        runAs(withAuthorities(READ_PREFIX + SOFTWARE_MODULE_TYPE + "/id==" + smType1.getId()), () -> {
            Assertions.<SoftwareModuleType> assertThat(softwareModuleTypeManagement.findAll(UNPAGED)).containsExactly(smType1);
            assertThat(softwareModuleTypeManagement.count()).isEqualTo(1);

            Assertions.<SoftwareModuleType> assertThat(softwareModuleTypeManagement.findByRsql("name==*", UNPAGED)).containsExactly(smType1);
            assertThat(softwareModuleTypeManagement.countByRsql("name==*")).isEqualTo(1);

            assertThat(softwareModuleTypeManagement.exists(smType1.getId())).isTrue();
            assertThat(softwareModuleTypeManagement.exists(smType2.getId())).isFalse();

            assertThat(softwareModuleTypeManagement.find(smType1.getId()).map(SoftwareModuleType.class::cast)).hasValue(smType1);
            assertThat(softwareModuleTypeManagement.find(smType2.getId())).isEmpty();

            Assertions.<SoftwareModuleType> assertThat(softwareModuleTypeManagement.get(List.of(smType1.getId()))).containsExactly(smType1);

            final List<Long> noPermissionIdList = List.of(smType2.getId());
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> softwareModuleTypeManagement.get(noPermissionIdList));
            final List<Long> allPermissionsIdList = List.of(smType1.getId(), smType2.getId());
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> softwareModuleTypeManagement.get(allPermissionsIdList));

            assertThat(softwareModuleTypeManagement.findByKey(smType1.getKey()).map(SoftwareModuleType.class::cast)).hasValue(smType1);
            final String noPermissionsTypeKey = smType2.getKey();
            assertThatThrownBy(() -> softwareModuleTypeManagement.findByKey(noPermissionsTypeKey))
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }

    @Test
    void verifyUpdate() {
        // permissions to read only type1 sm types
        runAs(withAuthorities(READ_PREFIX + SOFTWARE_MODULE_TYPE, UPDATE_PREFIX + SOFTWARE_MODULE_TYPE + "/id==" + smType1.getId()), () -> {
            final String newDescription = randomString(16);
            assertThat(softwareModuleTypeManagement.update(Update.builder().id(smType1.getId()).description(newDescription).build()))
                    .extracting(NamedEntity::getDescription)
                    .isEqualTo(newDescription);

            final Update descriptionUpdate = Update.builder().id(smType2.getId()).description(newDescription).build();
            assertThatThrownBy(() -> softwareModuleTypeManagement.update(descriptionUpdate))
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }

    @Test
    void verifyDelete() {
        // permissions to read all and update only type1 sm types
        runAs(withAuthorities(READ_PREFIX + SOFTWARE_MODULE_TYPE, DELETE_PREFIX + SOFTWARE_MODULE_TYPE + "/id==" + smType1.getId()), () -> {
            assertThat(softwareModuleTypeManagement.find(smType1.getId())).isPresent();
            softwareModuleTypeManagement.delete(smType1.getId());
            // soft delete since smType1 is assigned to sm1Type1
            assertThat(softwareModuleTypeManagement.find(smType1.getId())).hasValueSatisfying(SoftwareModuleType::isDeleted);

            final Long smType2Id = smType2.getId();
            assertThatThrownBy(() -> distributionSetTypeManagement.delete(smType2Id)).isInstanceOf(InsufficientPermissionException.class);
        });
    }
}
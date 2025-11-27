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
import static org.eclipse.hawkbit.auth.SpPermission.READ_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.SOFTWARE_MODULE;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_PREFIX;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValueCreate;
import org.junit.jupiter.api.Test;

class SoftwareModuleManagementTest extends AbstractAccessControllerManagementTest {

    @Test
    void verifyRead() {
        runAs(withAuthorities(READ_PREFIX + SOFTWARE_MODULE + "/type.id==" + smType1.getId()), () -> {
            Assertions.<SoftwareModule> assertThat(softwareModuleManagement.findAll(UNPAGED)).containsExactly(sm1Type1);
            assertThat(softwareModuleManagement.count()).isEqualTo(1);

            Assertions.<SoftwareModule> assertThat(softwareModuleManagement.findByRsql("name==*", UNPAGED)).containsExactly(sm1Type1);
            Assertions.assertThat(softwareModuleManagement.countByRsql("name==*")).isEqualTo(1);

            Assertions.<SoftwareModule> assertThat(softwareModuleManagement.findByAssignedTo(ds2Type2.getId(), UNPAGED).toList())
                    .containsExactly(sm1Type1); // no sm2Type2

            assertThat(softwareModuleManagement.getMetadata(sm1Type1.getId())).isEmpty();
            final Long sm2Type2Id = sm2Type2.getId();
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> softwareModuleManagement.getMetadata(sm2Type2Id));
        });
    }

    @Test
    void verifyUpdate() {
        runAs(withAuthorities(READ_PREFIX + SOFTWARE_MODULE, UPDATE_PREFIX + SOFTWARE_MODULE + "/type.id==" + smType1.getId()), () -> {
            final String metadataKey = "key.dot";
            final MetadataValueCreate metaDataCreate = new MetadataValueCreate(randomString(16), true);

            final Long sm1Type1Id = sm1Type1.getId();
            softwareModuleManagement.createMetadata(sm1Type1Id, metadataKey, metaDataCreate);
            assertThat(softwareModuleManagement.getMetadata(sm1Type1Id, metadataKey))
                    .satisfies(createdMetadata -> assertTrue(createdMetadata.isTargetVisible()));
            assertThat(softwareModuleManagement.getMetadata(sm1Type1Id)).hasSize(1);
            final Long sm2Type2Id = sm2Type2.getId();
            assertThatThrownBy(() -> softwareModuleManagement.createMetadata(sm2Type2Id, metadataKey, metaDataCreate))
                    .isInstanceOf(InsufficientPermissionException.class);
            final Map<String, MetadataValueCreate> metaDataCreateMap = Map.of(metadataKey, metaDataCreate);
            assertThatThrownBy(() -> softwareModuleManagement.createMetadata(sm2Type2Id, metaDataCreateMap))
                    .isInstanceOf(InsufficientPermissionException.class);

            final MetadataValueCreate metadataUpdate = new MetadataValueCreate(randomString(16));
            softwareModuleManagement.createMetadata(sm1Type1Id, metadataKey, metadataUpdate);
            assertThat(softwareModuleManagement.getMetadata(sm1Type1Id, metadataKey))
                    .satisfies(updatedMetadata -> assertFalse(updatedMetadata.isTargetVisible()));
            softwareModuleManagement.deleteMetadata(sm1Type1Id, metadataKey);
            assertThatThrownBy(() -> softwareModuleManagement.createMetadata(sm2Type2Id, metadataKey, metadataUpdate))
                    .isInstanceOf(InsufficientPermissionException.class);
            assertThatThrownBy(() -> softwareModuleManagement.deleteMetadata(sm2Type2Id, metadataKey))
                    .isInstanceOf(EntityNotFoundException.class);
        });
    }
}
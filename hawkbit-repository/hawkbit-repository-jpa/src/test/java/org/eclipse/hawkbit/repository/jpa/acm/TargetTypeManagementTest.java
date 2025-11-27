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
import static org.eclipse.hawkbit.auth.SpPermission.DELETE_TARGET_TYPE;
import static org.eclipse.hawkbit.auth.SpPermission.DISTRIBUTION_SET_TYPE;
import static org.eclipse.hawkbit.auth.SpPermission.READ_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET_TYPE;
import static org.eclipse.hawkbit.auth.SpPermission.TARGET_TYPE;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_TARGET_TYPE;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.TargetTypeManagement.Update;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.junit.jupiter.api.Test;

class TargetTypeManagementTest extends AbstractAccessControllerManagementTest {

    @Test
    void verifyCreate() {
        // permissions to read all and create new type with name 'permitted'
        runAs(withAuthorities(READ_TARGET_TYPE, CREATE_PREFIX + TARGET_TYPE + "/name==permitted"), () -> {
            assertThat(testdataFactory.findOrCreateTargetType("permitted")).isNotNull();
            assertThatThrownBy(() -> testdataFactory.findOrCreateTargetType("not_permitted_2"))
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }

    @Test
    void verifyRead() {
        // permissions to read only type1 target types
        runAs(withAuthorities(READ_TARGET_TYPE + "/id==" + targetType1.getId()), () -> {
            Assertions.<TargetType> assertThat(targetTypeManagement.findAll(UNPAGED)).containsExactly(targetType1);
            assertThat(targetTypeManagement.count()).isEqualTo(1);

            Assertions.<TargetType> assertThat(targetTypeManagement.findByRsql("name==*", UNPAGED))
                    .contains(targetType1).doesNotContain(targetType2);

            assertThat((Optional) targetTypeManagement.find(targetType1.getId())).hasValue(targetType1);
            assertThat(targetTypeManagement.find(targetType2.getId())).isEmpty();

            Assertions.<TargetType> assertThat(targetTypeManagement.get(Collections.singletonList(targetType1.getId())))
                    .contains(targetType1).doesNotContain(targetType2);
            final List<Long> noPermissionsEntitiesIds = Collections.singletonList(targetType2.getId());
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> targetTypeManagement.get(noPermissionsEntitiesIds));
            final List<Long> allEntitiesIds = List.of(targetType1.getId(), targetType2.getId());
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> targetTypeManagement.get(allEntitiesIds));
        });
    }

    @Test
    void verifyUpdate() {
        runAs(withAuthorities(READ_TARGET_TYPE, UPDATE_TARGET_TYPE + "/id==" + targetType1.getId(), READ_PREFIX + DISTRIBUTION_SET_TYPE),
                () -> {
                    assertThat(targetTypeManagement.update(Update.builder().id(targetType1.getId()).description("anotherDesc").build()))
                            .extracting(NamedEntity::getDescription).isEqualTo("anotherDesc");
                    final Update targetTypeUpdate = Update.builder().id(targetType2.getId()).description("anotherDesc").build();
                    assertThatThrownBy(() -> targetTypeManagement.update(targetTypeUpdate)).isInstanceOf(InsufficientPermissionException.class);

                    final Long dsType2Id = dsType2.getId();
                    final List<Long> dsType2IdList = List.of(dsType2Id);
                    targetTypeManagement.assignCompatibleDistributionSetTypes(targetType1.getId(), dsType2IdList);
                    final Long targetType2Id = targetType2.getId();
                    assertThatThrownBy(
                            () -> targetTypeManagement.assignCompatibleDistributionSetTypes(targetType2Id, dsType2IdList))
                            .isInstanceOf(InsufficientPermissionException.class);

                    targetTypeManagement.unassignDistributionSetType(targetType1.getId(), dsType2Id);
                    assertThatThrownBy(() -> targetTypeManagement.unassignDistributionSetType(targetType2Id, dsType2Id))
                            .isInstanceOf(InsufficientPermissionException.class);
                });
    }

    @Test
    void verifyDelete() {
        // assigned type can't be deleted, so create dedicated ones
        final TargetType targetType4 = testdataFactory.findOrCreateTargetType("toBeDeleted");
        final Long targetType5Id = testdataFactory.findOrCreateTargetType("toBeDeleted2").getId();
        runAs(withAuthorities(READ_TARGET_TYPE, DELETE_TARGET_TYPE + "/id==" + targetType4.getId()), () -> {
            targetTypeManagement.delete(targetType4.getId());
            assertThatThrownBy(() -> targetTypeManagement.delete(targetType5Id)).isInstanceOf(InsufficientPermissionException.class);
        });
    }
}
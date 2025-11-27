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
import static org.eclipse.hawkbit.auth.SpPermission.DISTRIBUTION_SET_TYPE;
import static org.eclipse.hawkbit.auth.SpPermission.READ_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_PREFIX;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.junit.jupiter.api.Test;

class DistributionSetTypeManagementTest extends AbstractAccessControllerManagementTest {

    @Test
    void verifyCreate() {
        // permissions to read all and create new type with name 'permitted'
        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE, CREATE_PREFIX + DISTRIBUTION_SET_TYPE + "/name==permitted"), () -> {
            assertThat(testdataFactory.findOrCreateDistributionSetType("newType", "permitted")).isNotNull();
            assertThatThrownBy(() -> testdataFactory.findOrCreateDistributionSetType("newType_2", "not_permitted_2"))
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }

    @Test
    void verifyRead() {
        // permissions to read only type1 ds types
        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE + "/id==" + dsType1.getId()), () -> {
            // perform distributionSetTypeManagement#findAll and verify
            Assertions.<DistributionSetType> assertThat(distributionSetTypeManagement.findAll(UNPAGED))
                    .containsExactly(dsType1);
            assertThat(distributionSetTypeManagement.count()).isEqualTo(1);

            Assertions.<DistributionSetType> assertThat(distributionSetTypeManagement.findByRsql("name==*", UNPAGED))
                    .containsExactly(dsType1);
            assertThat(distributionSetTypeManagement.countByRsql("name==*")).isEqualTo(1);

            assertThat(distributionSetTypeManagement.exists(dsType1.getId())).isTrue();
            assertThat(distributionSetTypeManagement.exists(dsType2.getId())).isFalse();

            assertThat(distributionSetTypeManagement.find(dsType1.getId()).map(DistributionSetType.class::cast)).hasValue(dsType1);
            assertThat(distributionSetTypeManagement.find(dsType2.getId())).isEmpty();

            Assertions.<DistributionSetType> assertThat(distributionSetTypeManagement.get(List.of(dsType1.getId())))
                    .containsExactly(dsType1);
            final List<Long> noPermissionIdList = List.of(dsType2.getId());
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> distributionSetTypeManagement.get(noPermissionIdList));
            final List<Long> allPermissionsIdList = List.of(dsType1.getId(), dsType2.getId());
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> distributionSetTypeManagement.get(allPermissionsIdList));

            assertThat(distributionSetTypeManagement.findByKey(dsType1.getKey()).map(DistributionSetType.class::cast)).hasValue(dsType1);
            assertThat(distributionSetTypeManagement.findByKey(dsType2.getKey())).isEmpty();
        });
    }

    @Test
    void verifyUpdate() {
        // permissions to read all and update only type1 ds types
        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE, UPDATE_PREFIX + DISTRIBUTION_SET_TYPE + "/id==" + dsType1.getId()), () -> {
            final String newDescription = randomString(16);

            final Long dsType2Id = dsType2.getId();
            assertThat(distributionSetTypeManagement.update(
                    DistributionSetTypeManagement.Update.builder().id(dsType1.getId()).description(newDescription).build()))
                    .extracting(NamedEntity::getDescription).isEqualTo(newDescription);

            final DistributionSetTypeManagement.Update descriptionUpdate = DistributionSetTypeManagement.Update.builder()
                    .id(dsType2Id).description(newDescription).build();
            assertThatThrownBy(() -> distributionSetTypeManagement.update(descriptionUpdate))
                    .isInstanceOf(InsufficientPermissionException.class);
        });

        // override types with unassigned in order to have modifiable types
        final DistributionSetType dsType1 = testdataFactory.findOrCreateDistributionSetType(
                "DsType1_override", "DistributionSetType-1-override", List.of(smType1), List.of());
        final DistributionSetType dsType2 = testdataFactory.findOrCreateDistributionSetType(
                "DsType2_override", "DistributionSetType-2-override", List.of(smType2), List.of(smType1));
        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE, UPDATE_PREFIX + DISTRIBUTION_SET_TYPE + "/id==" + dsType1.getId()), () -> {
            final Long osTypeId = osType.getId();
            final List<Long> osAndAppTypeIds = List.of(osTypeId, appType.getId());
            // verify distributionSetTypeManagement#assignCompatibleDistributionSetTypes
            DistributionSetType dsType1Up = distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType1.getId(), osAndAppTypeIds);
            assertThat(dsType1Up).satisfies(
                    updatedType -> assertThat(Stream.of(osType, appType).allMatch(updatedType::containsModuleType)).isTrue());
            assertThat(dsType1Up.containsModuleType(osType)).isTrue();
            assertThat(dsType1Up.containsModuleType(appType)).isTrue();
            final Long dsType2Id = dsType2.getId();
            assertThatThrownBy(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType2Id, osAndAppTypeIds))
                    .isInstanceOf(InsufficientPermissionException.class);

            assertThat(distributionSetTypeManagement
                    .unassignSoftwareModuleType(dsType1.getId(), osTypeId))
                    .satisfies(updatedType -> assertThat(updatedType.containsModuleType(osType)).isFalse());
            assertThatThrownBy(
                    () -> distributionSetTypeManagement.unassignSoftwareModuleType(dsType2Id, osTypeId))
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }

    @Test
    void verifyDelete() {
        // permissions to read all and update only type1 ds types
        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE, DELETE_PREFIX + DISTRIBUTION_SET_TYPE + "/id==" + dsType1.getId()), () -> {
            assertThat(distributionSetTypeManagement.find(dsType1.getId())).isPresent();
            distributionSetTypeManagement.delete(dsType1.getId());
            // soft delete since dsType1 is assigned to ds1Type1
            assertThat(distributionSetTypeManagement.find(dsType1.getId())).hasValueSatisfying(DistributionSetType::isDeleted);

            final Long dsType2Id = dsType2.getId();
            assertThatThrownBy(() -> distributionSetTypeManagement.delete(dsType2Id)).isInstanceOf(InsufficientPermissionException.class);
        });
    }
}
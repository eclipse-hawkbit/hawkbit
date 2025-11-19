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
import static org.eclipse.hawkbit.auth.SpPermission.DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.DistributionSetManagement.Create;
import org.eclipse.hawkbit.repository.DistributionSetManagement.Update;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.junit.jupiter.api.Test;

class DistributionSetManagementTest extends AbstractAccessControllerManagementTest {

    @Test
    void verifyCreate() {
        // permissions to read all and create only type1 ds
        runAs(withAuthorities(READ_DISTRIBUTION_SET, CREATE_PREFIX + DISTRIBUTION_SET + "/type.id==" + dsType1.getId()), () -> {
            final Create dsType1Create = Create.builder().type(dsType1).name(randomString(16)).version("1.0").build();
            final Create dsType2Create = Create.builder().type(dsType2).name(randomString(16)).version("1.0").build();

            assertThat(distributionSetManagement.create(dsType1Create)).isNotNull();
            assertThatThrownBy(() -> distributionSetManagement.create(dsType2Create)).isInstanceOf(InsufficientPermissionException.class);

            final List<Create> dsType1CreateList = List.of(
                    Create.builder().type(dsType1).name(randomString(16)).version("1.0").build(),
                    Create.builder().type(dsType1).name(randomString(16)).version("1.0").build());
            final List<Create> dsType2CreateList = List.of(
                    Create.builder().type(dsType2).name(randomString(16)).version("1.0").build(),
                    Create.builder().type(dsType2).name(randomString(16)).version("1.0").build());
            assertThat(distributionSetManagement.create(dsType1CreateList)).hasSize(2);
            assertThatThrownBy(() -> distributionSetManagement.create(dsType2CreateList)).isInstanceOf(InsufficientPermissionException.class);
        });
    }

    @Test
    void verifyRead() {
        // permissions to read only type1 ds
        runAs(withAuthorities(READ_DISTRIBUTION_SET + "/type.id==" + dsType1.getId()), () -> {
            Assertions.<DistributionSet> assertThat(distributionSetManagement.findAll(UNPAGED)).containsExactlyInAnyOrder(ds1Type1);
            assertThat(distributionSetManagement.count()).isEqualTo(1);

            Assertions.<DistributionSet> assertThat(distributionSetManagement.findByRsql("name==*", UNPAGED)).containsExactly(ds1Type1);
            assertThat(distributionSetManagement.countByRsql("name==*")).isEqualTo(1);

            Assertions.<DistributionSet> assertThat(distributionSetManagement.findByTag(dsTag1.getId(), UNPAGED)).containsExactly(ds1Type1);
            assertThat(distributionSetManagement.findByTag(dsTag2.getId(), UNPAGED)).isEmpty();

            Assertions.<DistributionSet> assertThat(distributionSetManagement.findByRsqlAndTag("name==*", dsTag1.getId(), UNPAGED))
                    .containsExactly(ds1Type1);
            assertThat(distributionSetManagement.findByRsqlAndTag("name==*", dsTag2.getId(), UNPAGED)).isEmpty();

            // perform distributionSetManagement#get and verify
            final var ds1Type1Id = ds1Type1.getId();
            final Long ds2Type2Id = ds2Type2.getId();
            assertThat(distributionSetManagement.get(ds1Type1Id)).isEqualTo(ds1Type1);
            assertThat(distributionSetManagement.getWithDetails(ds1Type1Id)).isEqualTo(ds1Type1);
            assertThat(distributionSetManagement.find(ds1Type1Id).map(DistributionSet.class::cast)).hasValue(ds1Type1);
            assertThatThrownBy(() -> distributionSetManagement.get(ds2Type2Id)).isInstanceOf(EntityNotFoundException.class);
            assertThatThrownBy(() -> distributionSetManagement.getWithDetails(ds2Type2Id)).isInstanceOf(EntityNotFoundException.class);
            assertThat(distributionSetManagement.find(ds2Type2Id)).isEmpty();

            Assertions.<DistributionSet> assertThat(distributionSetManagement.get(List.of(ds1Type1Id))).contains(ds1Type1);
            final List<Long> noPermissionsTestDataDsIdList = List.of(ds2Type2Id);
            assertThatThrownBy(() -> distributionSetManagement.get(noPermissionsTestDataDsIdList)).isInstanceOf(EntityNotFoundException.class);
            final List<Long> containingNoPermissionTestDataDsIsList = List.of(ds1Type1Id, ds2Type2Id);
            assertThatThrownBy(() -> distributionSetManagement.get(containingNoPermissionTestDataDsIsList))
                    .isInstanceOf(EntityNotFoundException.class);

            // perform distributionSetManagement#getByNameAndVersion and verify
            assertThat(distributionSetManagement.findByNameAndVersion(ds1Type1.getName(), ds1Type1.getVersion())).isEqualTo(ds1Type1);
            final String ds2Type2Name = ds2Type2.getName();
            final String ds2Type2Version = ds2Type2.getVersion();
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(
                    () -> distributionSetManagement.findByNameAndVersion(ds2Type2Name, ds2Type2Version));

            assertThat(distributionSetManagement.getValidAndComplete(ds1Type1Id)).isEqualTo(ds1Type1);
            assertThatThrownBy(() -> distributionSetManagement.getValidAndComplete(ds2Type2Id)).isInstanceOf(EntityNotFoundException.class);

            assertThat(distributionSetManagement.getMetadata(ds1Type1Id)).isEmpty();
            assertThatThrownBy(() -> distributionSetManagement.getMetadata(ds2Type2Id)).isInstanceOf(EntityNotFoundException.class);
        });
    }

    @Test
    void verifyUpdate() {
        final String ds2MdKey = "ds2MdKey";
        distributionSetManagement.createMetadata(ds2Type2.getId(), ds2MdKey, "ds2MdValue");
        // override with updated
        final DistributionSet ds2Type2 = distributionSetManagement.get(this.ds2Type2.getId());
        // permissions to read all and update only type1 ds
        runAs(withAuthorities(READ_DISTRIBUTION_SET, UPDATE_DISTRIBUTION_SET + "/type.id==" + dsType1.getId()), () -> {
            assertThat(distributionSetManagement.assignTag(List.of(ds1Type1.getId()), dsTag2.getId())).hasSize(1);
            final List<Long> noPermissionsTestDataDsId = List.of(ds3Type2.getId());
            final Long tagToAssignId = dsTag1.getId();
            assertThatThrownBy(() -> distributionSetManagement.assignTag(noPermissionsTestDataDsId, tagToAssignId))
                    .isInstanceOf(InsufficientPermissionException.class);

            assertThat(distributionSetManagement.unassignTag(List.of(ds1Type1.getId()), dsTag2.getId())).hasSize(1);
            final Long tagToRemoveId = dsTag2.getId();
            assertThatThrownBy(() -> distributionSetManagement.unassignTag(noPermissionsTestDataDsId, tagToRemoveId))
                    .isInstanceOf(InsufficientPermissionException.class);

            final String metadataKey = "key.dot";
            final Map<String, String> metaData = Map.of(metadataKey, "value.dot");

            final Long ds2Type2Id = ds2Type2.getId();
            distributionSetManagement.createMetadata(ds1Type1.getId(), metaData);
            assertThatThrownBy(() -> distributionSetManagement.createMetadata(ds2Type2Id, metaData))
                    .isInstanceOf(InsufficientPermissionException.class);

            final String metadataNewValue = "newValue.dot";
            distributionSetManagement.createMetadata(ds1Type1.getId(), metadataKey, metadataNewValue);
            assertThatThrownBy(() -> distributionSetManagement.createMetadata(ds2Type2Id, metadataKey, metadataNewValue))
                    .isInstanceOf(InsufficientPermissionException.class);

            distributionSetManagement.deleteMetadata(ds1Type1.getId(), metadataKey);
            assertThatThrownBy(() -> distributionSetManagement.deleteMetadata(ds2Type2Id, metadataKey))
                    .isInstanceOf(EntityNotFoundException.class);
            assertThatThrownBy(() -> distributionSetManagement.deleteMetadata(ds2Type2Id, ds2MdKey))
                    .isInstanceOf(InsufficientPermissionException.class);

            final Update distributionSet1Update = Update.builder().id(ds1Type1.getId()).description(randomString(16)).build();
            final Update distributionSet2Update = Update.builder().id(ds2Type2Id).description(randomString(16)).build();
            DistributionSet ds1Type1 = distributionSetManagement.update(distributionSet1Update);
            assertThat(ds1Type1).extracting(NamedEntity::getDescription).isEqualTo(distributionSet1Update.getDescription());
            assertThatThrownBy(() -> distributionSetManagement.update(distributionSet2Update))
                    .isInstanceOf(InsufficientPermissionException.class);

            distributionSetManagement.unlock(ds1Type1);
            ds1Type1 = distributionSetManagement.unassignSoftwareModule(ds1Type1.getId(), sm1Type1.getId());
            assertThat(ds1Type1).matches(
                    updated -> updated.getModules().isEmpty());
            ds1Type1 = distributionSetManagement.assignSoftwareModules(ds1Type1.getId(), List.of(sm1Type1.getId()));
            assertThat(ds1Type1).matches(updated -> updated.getModules().stream()
                    .map(Identifiable::getId).anyMatch(sm1Type1.getId()::equals));
            try {
                SecurityContextSwitch.callAsPrivileged(() -> distributionSetManagement.unlock(ds2Type2));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            final Long softwareModuleAppId = sm1Type1.getId();
            final List<Long> softwareModuleAppIdList = List.of(softwareModuleAppId);
            assertThatThrownBy(() -> distributionSetManagement.assignSoftwareModules(ds2Type2Id, softwareModuleAppIdList))
                    .isInstanceOf(InsufficientPermissionException.class);
            assertThatThrownBy(() -> distributionSetManagement.unassignSoftwareModule(ds2Type2Id, softwareModuleAppId))
                    .isInstanceOf(InsufficientPermissionException.class);

            distributionSetManagement.invalidate(ds1Type1);
            assertThatThrownBy(() -> distributionSetManagement.invalidate(ds2Type2)).isInstanceOf(InsufficientPermissionException.class);
        });
    }
}
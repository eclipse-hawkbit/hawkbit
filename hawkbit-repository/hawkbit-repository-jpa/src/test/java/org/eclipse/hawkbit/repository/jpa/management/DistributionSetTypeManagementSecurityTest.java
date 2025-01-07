/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.List;
import java.util.Random;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - DistributionSetTypeManagement")
@Story("SecurityTests DistributionSetTypeManagement")
public class DistributionSetTypeManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<DistributionSetType, DistributionSetTypeCreate, DistributionSetTypeUpdate> {

    @Override
    protected RepositoryManagement<DistributionSetType, DistributionSetTypeCreate, DistributionSetTypeUpdate> getRepositoryManagement() {
        return distributionSetTypeManagement;
    }

    @Override
    protected DistributionSetTypeCreate getCreateObject() {
        return entityFactory.distributionSetType().create().key(String.format("key-%d", new Random().nextInt())).name(String.format("name-%d", new Random().nextInt()));
    }

    @Override
    protected DistributionSetTypeUpdate getUpdateObject() {
        return entityFactory.distributionSetType().update(1L).description("description");
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByKeyPermissionsCheck() {
        assertPermissions(() -> distributionSetTypeManagement.getByKey("key"), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByNamePermissionsCheck() {
        assertPermissions(() -> distributionSetTypeManagement.getByName("name"), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void assignOptionalSoftwareModuleTypesPermissionsCheck() {
        assertPermissions(() -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(1L, List.of(1L)),
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void assignMandatorySoftwareModuleTypesPermissionsCheck() {
        assertPermissions(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(1L, List.of(1L)),
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void unassignSoftwareModuleTypePermissionsCheck() {
        assertPermissions(() -> distributionSetTypeManagement.unassignSoftwareModuleType(1L, 1L), List.of(SpPermission.UPDATE_REPOSITORY));
    }
}

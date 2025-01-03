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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
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
        return entityFactory.distributionSetType().create().key("key").name("name");
    }

    @Override
    protected DistributionSetTypeUpdate getUpdateObject() {
        return entityFactory.distributionSetType().update(1L).description("description");
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getByKeyWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetTypeManagement.getByKey("key"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getByKeyWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetTypeManagement.getByKey("key"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getByNameWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetTypeManagement.getByName("name"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetTypeManagement.getByName("name"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void assignOptionalSoftwareModuleTypesWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(1L, List.of(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void assignOptionalSoftwareModuleTypesWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(1L, List.of(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void assignMandatorySoftwareModuleTypesWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(1L, List.of(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void assignMandatorySoftwareModuleTypesWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(1L, List.of(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void unassignSoftwareModuleTypeWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetTypeManagement.unassignSoftwareModuleType(1L, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void unassignSoftwareModuleTypeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetTypeManagement.unassignSoftwareModuleType(1L, 1L));
    }
}

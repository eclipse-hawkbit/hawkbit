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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - SoftwareModuleTypeManagement")
@Story("SecurityTests SoftwareModuleTypeManagement")
public class SoftwareModuleTypeManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<SoftwareModuleType, SoftwareModuleTypeCreate, SoftwareModuleTypeUpdate> {

    @Override
    protected RepositoryManagement<SoftwareModuleType, SoftwareModuleTypeCreate, SoftwareModuleTypeUpdate> getRepositoryManagement() {
        return softwareModuleTypeManagement;
    }

    @Override
    protected SoftwareModuleTypeCreate getCreateObject() {
        return entityFactory.softwareModuleType().create().key("key").name("name");
    }

    @Override
    protected SoftwareModuleTypeUpdate getUpdateObject() {
        return entityFactory.softwareModuleType().update(1L).description("description");
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getByKeyWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleTypeManagement.getByKey("key"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_ROLLOUT, SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY })
    void getByKeyWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleTypeManagement.getByKey("key"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getByNameWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleTypeManagement.getByName("name"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_ROLLOUT, SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY })
    void getByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleTypeManagement.getByName("name"));
    }
}

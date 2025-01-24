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
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - SoftwareModuleTypeManagement")
@Story("SecurityTests SoftwareModuleTypeManagement")
class SoftwareModuleTypeManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<SoftwareModuleType, SoftwareModuleTypeCreate, SoftwareModuleTypeUpdate> {

    @Override
    protected RepositoryManagement<SoftwareModuleType, SoftwareModuleTypeCreate, SoftwareModuleTypeUpdate> getRepositoryManagement() {
        return softwareModuleTypeManagement;
    }

    @Override
    protected SoftwareModuleTypeCreate getCreateObject() {
        return entityFactory.softwareModuleType().create().key(String.format("key-%d", new Random().nextInt())).name(String.format("name-%d", new Random().nextInt()));
    }

    @Override
    protected SoftwareModuleTypeUpdate getUpdateObject() {
        return entityFactory.softwareModuleType().update(1L).description("description");
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByKeyPermissionsCheck() {
        assertPermissions(() -> softwareModuleTypeManagement.findByKey("key"), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByNamePermissionsCheck() {
        assertPermissions(() -> softwareModuleTypeManagement.findByName("name"), List.of(SpPermission.READ_REPOSITORY));
    }

}

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

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - SoftwareModuleTypeManagement<br/>
 * Story: SecurityTests SoftwareModuleTypeManagement
 */
class SoftwareModuleTypeManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<SoftwareModuleType, SoftwareModuleTypeCreate<SoftwareModuleType>, SoftwareModuleTypeUpdate> {

    @Override
    protected RepositoryManagement<SoftwareModuleType, SoftwareModuleTypeCreate<SoftwareModuleType>, SoftwareModuleTypeUpdate> getRepositoryManagement() {
        return softwareModuleTypeManagement;
    }

    @Override
    protected SoftwareModuleTypeCreate<SoftwareModuleType> getCreateObject() {
        return entityFactory.softwareModuleType().create().key(String.format("key-%d", new Random().nextInt())).name(String.format("name-%d", new Random().nextInt()));
    }

    @Override
    protected SoftwareModuleTypeUpdate getUpdateObject() {
        return entityFactory.softwareModuleType().update(1L).description("description");
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByKeyPermissionsCheck() {
        assertPermissions(() -> softwareModuleTypeManagement.findByKey("key"), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByNamePermissionsCheck() {
        assertPermissions(() -> softwareModuleTypeManagement.findByName("name"), List.of(SpPermission.READ_REPOSITORY));
    }

}

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
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * Feature: SecurityTests - DistributionSetTagManagement<br/>
 * Story: SecurityTests DistributionSetTagManagement
 */
class DistributionSetTagManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<DistributionSetTag, DistributionSetTagManagement.Create, DistributionSetTagManagement.Update> {

    @Override
    protected RepositoryManagement getRepositoryManagement() {
        return distributionSetTagManagement;
    }

    @Override
    protected DistributionSetTagManagement.Create getCreateObject() {
        return DistributionSetTagManagement.Create.builder().name(String.format("tag-%d", new Random().nextInt())).build();
    }

    @Override
    protected DistributionSetTagManagement.Update getUpdateObject() {
        return DistributionSetTagManagement.Update.builder().id(1L).name("tag").build();
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByNameWitPermissionWorks() {
        assertPermissions(() -> distributionSetTagManagement.findByName("tagName"), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByDistributionSetPermissionsCheck() {
        assertPermissions(() -> distributionSetTagManagement.findByDistributionSet(1L, Pageable.unpaged()),
                List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deleteDistributionSetTagPermissionsCheck() {
        assertPermissions(() -> {
            distributionSetTagManagement.delete("tagName");
            return null;
        }, List.of(SpPermission.DELETE_REPOSITORY));
    }
}

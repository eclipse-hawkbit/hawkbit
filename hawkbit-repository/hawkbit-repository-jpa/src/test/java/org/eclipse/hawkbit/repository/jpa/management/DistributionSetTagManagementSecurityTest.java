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
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * Feature: SecurityTests - DistributionSetTagManagement<br/>
 * Story: SecurityTests DistributionSetTagManagement
 */
class DistributionSetTagManagementSecurityTest extends AbstractRepositoryManagementSecurityTest<DistributionSetTag, TagCreate, TagUpdate> {

    @Override
    protected RepositoryManagement<DistributionSetTag, TagCreate, TagUpdate> getRepositoryManagement() {
        return distributionSetTagManagement;
    }

    @Override
    protected TagCreate getCreateObject() {
        return entityFactory.tag().create().name(String.format("tag-%d", new Random().nextInt()));
    }

    @Override
    protected TagUpdate getUpdateObject() {
        return entityFactory.tag().update(1L).name("tag");
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

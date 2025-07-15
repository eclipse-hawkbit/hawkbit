/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

public abstract class AbstractRepositoryManagementSecurityTest<T extends BaseEntity, C, U> extends AbstractJpaIntegrationTest {

    /**
     * @return the repository management to test with
     */
    protected abstract RepositoryManagement<T, C, U> getRepositoryManagement();

    /**
     * @return the object to create
     */
    protected abstract C getCreateObject();

    /**
     * @return the object to update
     */
    protected abstract U getUpdateObject();

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createCollectionPermissionCheck() {
        assertPermissions(() -> getRepositoryManagement().create(List.of(getCreateObject())), List.of(SpPermission.CREATE_REPOSITORY, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createPermissionCheck() {
        assertPermissions(() -> getRepositoryManagement().create(getCreateObject()), List.of(SpPermission.CREATE_REPOSITORY, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void updatePermissionCheck() {
        assertPermissions(() -> getRepositoryManagement().update(getUpdateObject()), List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deletePermissionCheck() {
        assertPermissions(() -> {
            getRepositoryManagement().delete(1L);
            return null;
            }, List.of(SpPermission.DELETE_REPOSITORY));
    }

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
     void countPermissionCheck() {
        assertPermissions(() -> getRepositoryManagement().count(), List.of(SpPermission.READ_REPOSITORY));
    }
    

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
     void deleteCollectionRepositoryManagement() {
        assertPermissions(() -> {
            getRepositoryManagement().delete(List.of(1L));
            return null;
        }, List.of(SpPermission.DELETE_REPOSITORY));
    }

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
     void getPermissionCheck() {
        assertPermissions(() -> getRepositoryManagement().get(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
     void getCollectionPermissionCheck() {
        assertPermissions(() -> getRepositoryManagement().get(List.of(1L)), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
     void existsCollectionPermissionCheck() {
        assertPermissions(() -> getRepositoryManagement().exists(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
     void findAllPermissionCheck() {
        assertPermissions(() -> getRepositoryManagement().findAll(Pageable.ofSize(1)), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests RepositoryManagement PreAuthorized method with correct and insufficient permissions.
     */
    @Test
     void findByRsqlPermissionCheck() {
        assertPermissions(() -> getRepositoryManagement().findByRsql("(name==*)", Pageable.ofSize(1)), List.of(SpPermission.READ_REPOSITORY));
    }

}

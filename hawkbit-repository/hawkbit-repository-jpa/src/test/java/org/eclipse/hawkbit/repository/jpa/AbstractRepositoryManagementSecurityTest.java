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
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

public abstract class AbstractRepositoryManagementSecurityTest<T, C, U> extends AbstractJpaIntegrationTest {

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

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void createCollectionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> getRepositoryManagement().create(List.of(getCreateObject())));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void createWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> getRepositoryManagement().create(getCreateObject()));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void updateWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> getRepositoryManagement().update(getUpdateObject()));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void deleteWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            getRepositoryManagement().delete(1L);
            return null;
        });
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    public void countWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> getRepositoryManagement().count());
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    public void deleteRepositoryManagement() {
        assertInsufficientPermission(() -> {
            getRepositoryManagement().delete(1L);
            return null;
        });
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    public void deleteCollectionRepositoryManagement() {
        assertInsufficientPermission(() -> {
            getRepositoryManagement().delete(List.of(1L));
            return null;
        });
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    public void getWithoutPermissionAccessDenied() {
        assertInsufficientPermission(() -> {
            getRepositoryManagement().get(1L);
            return null;
        });
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    public void getCollectionWithoutPermissionAccessDenied() {
        assertInsufficientPermission(() -> {
            getRepositoryManagement().get(List.of(1L));
            return null;
        });
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    public void existsCollectionWithoutPermissionAccessDenied() {
        assertInsufficientPermission(() -> {
            getRepositoryManagement().exists(1L);
            return null;
        });
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    public void findAllWithoutPermissionAccessDenied() {
        assertInsufficientPermission(() -> {
            getRepositoryManagement().findAll(Pageable.ofSize(1));
            return null;
        });
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    public void findByRsqlWithoutPermissionAccessDenied() {
        assertInsufficientPermission(() -> {
            getRepositoryManagement().findByRsql(Pageable.ofSize(1), "rsql");
            return null;
        });
    }

}

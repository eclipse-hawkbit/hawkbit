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

import java.io.ByteArrayInputStream;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - ArtifactManagement")
@Story("SecurityTests ArtifactManagement")
class ArtifactManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the count method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countWithPermissionWorks() {
        assertPermissionWorks(() -> artifactManagement.count());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void countWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> artifactManagement.count());
    }

    @Test
    @Description("Tests that the count method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY })
    void createWithPermissionWorks() {
        ArtifactUpload artifactUpload = new ArtifactUpload(new ByteArrayInputStream("RandomString".getBytes()), 1L, "filename", false, 1024);
        assertPermissionWorks(() -> artifactManagement.create(artifactUpload));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void createWithoutPermissionThrowsAccessDenied() {
        ArtifactUpload artifactUpload = new ArtifactUpload(new ByteArrayInputStream("RandomString".getBytes()), 1L, "filename", false, 1024);
        assertInsufficientPermission(() -> artifactManagement.create(artifactUpload));
    }

    @Test
    @Description("Tests that the count method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY })
    void deleteWithPermissionWorks() {
        assertPermissionWorks(() -> {
            artifactManagement.delete(1);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void deleteWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            artifactManagement.delete(1);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getWithPermissionWorks() {
        assertPermissionWorks(() -> artifactManagement.get(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> artifactManagement.get(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getByFilenameAndSoftwareModuleWithPermissionWorks() {
        assertInsufficientPermission(() -> artifactManagement.getByFilenameAndSoftwareModule("filename", 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getByFilenameAndSoftwareModuleWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> artifactManagement.getByFilenameAndSoftwareModule("filename", 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findFirstBySHA1WithPermissionWorks() {
        assertPermissionWorks(() -> artifactManagement.findFirstBySHA1("sha1"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void findFirstBySHA1WithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> artifactManagement.findFirstBySHA1("sha1"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getByFilenameWithPermissionWorks() {
        assertPermissionWorks(() -> artifactManagement.getByFilename("filename"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getByFilenameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> artifactManagement.getByFilename("filename"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findBySoftwareModuleWithPermissionWorks() {
        assertPermissionWorks(() -> artifactManagement.findBySoftwareModule(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void findBySoftwareModuleWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> artifactManagement.findBySoftwareModule(null, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countBySoftwareModuleWithPermissionWorks() {
        assertPermissionWorks(() -> artifactManagement.countBySoftwareModule(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void countBySoftwareModuleWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> artifactManagement.countBySoftwareModule(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT })
    void loadArtifactBinaryWithPermissionWorks() {
        assertPermissionWorks(() -> artifactManagement.loadArtifactBinary("sha1", 1L, false));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void loadArtifactBinaryWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> artifactManagement.loadArtifactBinary("sha1", 1L, false));
    }
}
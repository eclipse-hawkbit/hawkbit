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
import java.util.List;

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
    @Description("Tests ArtifactManagement#count() method")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countPermissionCheck() {
        assertPermissions(() -> artifactManagement.count(), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ArtifactManagement#create() method")
    void createPermissionCheck() {
        ArtifactUpload artifactUpload = new ArtifactUpload(new ByteArrayInputStream("RandomString".getBytes()), 1L, "filename", false, 1024);
        assertPermissions(() -> artifactManagement.create(artifactUpload), List.of(SpPermission.CREATE_REPOSITORY));
    }

    @Test
    @Description("Tests ArtifactManagement#delete() method")
    void deletePermissionCheck() {
        assertPermissions(() -> {
            artifactManagement.delete(1);
            return null;
        }, List.of(SpPermission.DELETE_REPOSITORY));
    }

    @Test
    @Description("Tests ArtifactManagement#get() method")
    void getPermissionCheck() {
        assertPermissions(() -> artifactManagement.get(1L), List.of(SpPermission.READ_REPOSITORY));
        assertPermissions(() -> artifactManagement.get(1L), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

    @Test
    @Description("Tests ArtifactManagement#getByFilenameAndSoftwareModule() method")
    void getByFilenameAndSoftwareModulePermissionCheck() {
        assertPermissions(() -> artifactManagement.getByFilenameAndSoftwareModule("filename", 1L),
                List.of(SpPermission.READ_REPOSITORY), List.of(SpPermission.CREATE_REPOSITORY));
        assertPermissions(() -> artifactManagement.getByFilenameAndSoftwareModule("filename", 1L),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

    @Test
    @Description("Tests ArtifactManagement#findFirstBySHA1() method")
    void findFirstBySHA1PermissionCheck() {
        assertPermissions(() -> artifactManagement.findFirstBySHA1("sha1"), List.of(SpPermission.READ_REPOSITORY));
        assertPermissions(() -> artifactManagement.findFirstBySHA1("sha1"), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

    @Test
    @Description("Tests ArtifactManagement#getByFilename() method")
    void getByFilenamePermissionCheck() {
        assertPermissions(() -> artifactManagement.getByFilename("filename"), List.of(SpPermission.READ_REPOSITORY));
        assertPermissions(() -> artifactManagement.getByFilename("filename"), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

    @Test
    @Description("Tests ArtifactManagement#findBySoftwareModule() method")
    void findBySoftwareModulePermissionCheck() {
        assertPermissions(() -> artifactManagement.findBySoftwareModule(PAGE, 1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ArtifactManagement#countBySoftwareModule() method")
    void countBySoftwareModulePermissionCheck() {
        assertPermissions(() -> artifactManagement.countBySoftwareModule(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ArtifactManagement#loadArtifactBinary() method")
    void loadArtifactBinaryPermissionCheck() {
        assertPermissions(() -> artifactManagement.loadArtifactBinary("sha1", 1L, false), List.of(SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT), List.of(SpPermission.CREATE_REPOSITORY));
        assertPermissions(() -> artifactManagement.loadArtifactBinary("sha1", 1L, false), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

}
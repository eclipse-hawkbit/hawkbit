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

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - ArtifactManagement<br/>
 * Story: SecurityTests ArtifactManagement
 */
class ArtifactManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ArtifactManagement#count() method
     */
    @Test    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countPermissionCheck() {
        assertPermissions(() -> artifactManagement.count(), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ArtifactManagement#create() method
     */
    @Test    void createPermissionCheck() {
        ArtifactUpload artifactUpload = new ArtifactUpload(new ByteArrayInputStream("RandomString".getBytes()), 1L, "filename", false, 1024);
        assertPermissions(() -> artifactManagement.create(artifactUpload), List.of(SpPermission.CREATE_REPOSITORY));
    }

    /**
     * Tests ArtifactManagement#delete() method
     */
    @Test    void deletePermissionCheck() {
        assertPermissions(() -> {
            artifactManagement.delete(1);
            return null;
        }, List.of(SpPermission.DELETE_REPOSITORY));
    }

    /**
     * Tests ArtifactManagement#get() method
     */
    @Test    void getPermissionCheck() {
        assertPermissions(() -> artifactManagement.get(1L), List.of(SpPermission.READ_REPOSITORY));
        assertPermissions(() -> artifactManagement.get(1L), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

    /**
     * Tests ArtifactManagement#getByFilenameAndSoftwareModule() method
     */
    @Test    void getByFilenameAndSoftwareModulePermissionCheck() {
        assertPermissions(() -> artifactManagement.getByFilenameAndSoftwareModule("filename", 1L),
                List.of(SpPermission.READ_REPOSITORY), List.of(SpPermission.CREATE_REPOSITORY));
        assertPermissions(() -> artifactManagement.getByFilenameAndSoftwareModule("filename", 1L),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

    /**
     * Tests ArtifactManagement#findFirstBySHA1() method
     */
    @Test    void findFirstBySHA1PermissionCheck() {
        assertPermissions(() -> artifactManagement.findFirstBySHA1("sha1"), List.of(SpPermission.READ_REPOSITORY));
        assertPermissions(() -> artifactManagement.findFirstBySHA1("sha1"), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

    /**
     * Tests ArtifactManagement#getByFilename() method
     */
    @Test    void getByFilenamePermissionCheck() {
        assertPermissions(() -> artifactManagement.getByFilename("filename"), List.of(SpPermission.READ_REPOSITORY));
        assertPermissions(() -> artifactManagement.getByFilename("filename"), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

    /**
     * Tests ArtifactManagement#findBySoftwareModule() method
     */
    @Test    void findBySoftwareModulePermissionCheck() {
        assertPermissions(() -> artifactManagement.findBySoftwareModule(1L, PAGE), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ArtifactManagement#countBySoftwareModule() method
     */
    @Test    void countBySoftwareModulePermissionCheck() {
        assertPermissions(() -> artifactManagement.countBySoftwareModule(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ArtifactManagement#loadArtifactBinary() method
     */
    @Test    void loadArtifactBinaryPermissionCheck() {
        assertPermissions(() -> artifactManagement.loadArtifactBinary("sha1", 1L, false), List.of(SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT), List.of(SpPermission.CREATE_REPOSITORY));
        assertPermissions(() -> artifactManagement.loadArtifactBinary("sha1", 1L, false), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

}
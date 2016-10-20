/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.test.util.HashGeneratorUtils;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test class for {@link ArtifactManagement} with running MongoDB instance..
 *
 *
 *
 *
 */
@Features("Component Tests - Repository")
@Stories("Artifact Management")
public class ArtifactManagementTest extends AbstractJpaIntegrationTestWithMongoDB {
    public ArtifactManagementTest() {
        LOG = LoggerFactory.getLogger(ArtifactManagementTest.class);
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.ArtifactManagement#createArtifact(java.io.InputStream)}
     * .
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @Description("Test if a local artifact can be created by API including metadata.")
    public void createArtifact() throws NoSuchAlgorithmException, IOException {
        // checkbaseline
        assertThat(softwareModuleRepository.findAll()).hasSize(0);
        assertThat(artifactRepository.findAll()).hasSize(0);

        JpaSoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        JpaSoftwareModule sm2 = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 2",
                "version 2", null, null);
        sm2 = softwareModuleRepository.save(sm2);

        JpaSoftwareModule sm3 = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 3",
                "version 3", null, null);
        sm3 = softwareModuleRepository.save(sm3);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final Artifact result = artifactManagement.createArtifact(new ByteArrayInputStream(random), sm.getId(), "file1",
                false);
        final Artifact result11 = artifactManagement.createArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file11", false);
        final Artifact result12 = artifactManagement.createArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file12", false);
        final Artifact result2 = artifactManagement.createArtifact(new ByteArrayInputStream(random), sm2.getId(),
                "file2", false);

        assertThat(result).isInstanceOf(Artifact.class);
        assertThat(result.getSoftwareModule().getId()).isEqualTo(sm.getId());
        assertThat(result2.getSoftwareModule().getId()).isEqualTo(sm2.getId());
        assertThat(((JpaArtifact) result).getFilename()).isEqualTo("file1");
        assertThat(((JpaArtifact) result).getGridFsFileName()).isNotNull();
        assertThat(result).isNotEqualTo(result2);
        assertThat(((JpaArtifact) result).getGridFsFileName()).isEqualTo(((JpaArtifact) result2).getGridFsFileName());

        assertThat(artifactManagement.findArtifactByFilename("file1").get(0).getSha1Hash())
                .isEqualTo(HashGeneratorUtils.generateSHA1(random));
        assertThat(artifactManagement.findArtifactByFilename("file1").get(0).getMd5Hash())
                .isEqualTo(HashGeneratorUtils.generateMD5(random));

        assertThat(artifactRepository.findAll()).hasSize(4);
        assertThat(softwareModuleRepository.findAll()).hasSize(3);

        assertThat(softwareManagement.findSoftwareModuleWithDetails(sm.getId()).getArtifacts()).hasSize(3);
    }

    @Test
    @Description("Tests hard delete directly on repository.")
    public void hardDeleteSoftwareModule() throws NoSuchAlgorithmException, IOException {
        JpaSoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        artifactManagement.createArtifact(new ByteArrayInputStream(random), sm.getId(), "file1", false);
        assertThat(artifactRepository.findAll()).hasSize(1);

        softwareModuleRepository.deleteAll();
        assertThat(artifactRepository.findAll()).hasSize(0);
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.ArtifactManagement#deleteArtifact(java.lang.Long)}
     * .
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @Description("Tests the deletion of a local artifact including metadata.")
    public void deleteArtifact() throws NoSuchAlgorithmException, IOException {
        JpaSoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        JpaSoftwareModule sm2 = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 2",
                "version 2", null, null);
        sm2 = softwareModuleRepository.save(sm2);

        assertThat(artifactRepository.findAll()).isEmpty();

        final Artifact result = artifactManagement.createArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(),
                "file1", false);
        final Artifact result2 = artifactManagement.createArtifact(new RandomGeneratedInputStream(5 * 1024),
                sm2.getId(), "file2", false);

        assertThat(artifactRepository.findAll()).hasSize(2);

        assertThat(result.getId()).isNotNull();
        assertThat(result2.getId()).isNotNull();
        assertThat(((JpaArtifact) result).getGridFsFileName())
                .isNotEqualTo(((JpaArtifact) result2).getGridFsFileName());
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((JpaArtifact) result).getGridFsFileName()))))
                        .isNotNull();
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((JpaArtifact) result2).getGridFsFileName()))))
                        .isNotNull();

        artifactManagement.deleteArtifact(result.getId());
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((JpaArtifact) result).getGridFsFileName()))))
                        .isNull();
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((JpaArtifact) result2).getGridFsFileName()))))
                        .isNotNull();

        artifactManagement.deleteArtifact(result2.getId());
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((JpaArtifact) result2).getGridFsFileName()))))
                        .isNull();

        assertThat(artifactRepository.findAll()).hasSize(0);
    }

    @Test
    @Description("Test the deletion of an artifact metadata where the binary is still linked to another "
            + "metadata element. The expected result is that the metadata is deleted but the binary kept.")
    public void deleteDuplicateArtifacts() throws NoSuchAlgorithmException, IOException {
        JpaSoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        JpaSoftwareModule sm2 = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 2",
                "version 2", null, null);
        sm2 = softwareModuleRepository.save(sm2);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final Artifact result = artifactManagement.createArtifact(new ByteArrayInputStream(random), sm.getId(), "file1",
                false);
        final Artifact result2 = artifactManagement.createArtifact(new ByteArrayInputStream(random), sm2.getId(),
                "file2", false);

        assertThat(artifactRepository.findAll()).hasSize(2);
        assertThat(result.getId()).isNotNull();
        assertThat(result2.getId()).isNotNull();
        assertThat(((JpaArtifact) result).getGridFsFileName()).isEqualTo(((JpaArtifact) result2).getGridFsFileName());

        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((JpaArtifact) result).getGridFsFileName()))))
                        .isNotNull();
        artifactManagement.deleteArtifact(result.getId());
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((JpaArtifact) result).getGridFsFileName()))))
                        .isNotNull();

        artifactManagement.deleteArtifact(result2.getId());
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((JpaArtifact) result).getGridFsFileName()))))
                        .isNull();
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.ArtifactManagement#findArtifact(java.lang.Long)}
     * .
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @Description("Loads an local artifact based on given ID.")
    public void findArtifact() throws NoSuchAlgorithmException, IOException {
        SoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final Artifact result = artifactManagement.createArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(),
                "file1", false);

        assertThat(artifactManagement.findArtifact(result.getId())).isEqualTo(result);
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.ArtifactManagement#loadArtifactBinary(java.lang.Long)}
     * .
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @Description("Loads an artifact binary based on given ID.")
    public void loadStreamOfArtifact() throws NoSuchAlgorithmException, IOException {
        SoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final Artifact result = artifactManagement.createArtifact(new ByteArrayInputStream(random), sm.getId(), "file1",
                false);

        assertTrue("The stored binary matches the given binary", IOUtils.contentEquals(new ByteArrayInputStream(random),
                artifactManagement.loadArtifactBinary(result).getFileInputStream()));
    }

    @Test
    @WithUser(allSpPermissions = true, removeFromAllPermission = { SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT })
    @Description("Trys and fails to load an artifact without required permission. Checks if expected InsufficientPermissionException is thrown.")
    public void loadArtifactBinaryWithoutDownloadArtifactThrowsPermissionDenied() {
        try {
            artifactManagement.loadArtifactBinary(new JpaArtifact());
            fail("Should not have worked with missing permission.");
        } catch (final InsufficientPermissionException e) {

        }
    }

    @Test
    @Description("Searches an artifact through the relations of a software module.")
    public void findArtifactBySoftwareModule() {
        SoftwareModule sm = new JpaSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        SoftwareModule sm2 = new JpaSoftwareModule(osType, "name 2", "version 2", null, null);
        sm2 = softwareManagement.createSoftwareModule(sm2);

        assertThat(artifactManagement.findArtifactBySoftwareModule(pageReq, sm.getId())).isEmpty();

        final Artifact result = artifactManagement.createArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(),
                "file1", false);

        assertThat(artifactManagement.findArtifactBySoftwareModule(pageReq, sm.getId())).hasSize(1);
    }

    @Test
    @Description("Searches an artifact through the relations of a software module and the filename.")
    public void findByFilenameAndSoftwareModule() {
        SoftwareModule sm = new JpaSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        assertThat(artifactManagement.findByFilenameAndSoftwareModule("file1", sm.getId())).isEmpty();

        artifactManagement.createArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(), "file1", false);
        artifactManagement.createArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(), "file2", false);

        assertThat(artifactManagement.findByFilenameAndSoftwareModule("file1", sm.getId())).hasSize(1);

    }
}

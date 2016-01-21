/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.AbstractIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.HashGeneratorUtils;
import org.eclipse.hawkbit.RandomGeneratedInputStream;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.exception.ArtifactDeleteFailedException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ExternalArtifact;
import org.eclipse.hawkbit.repository.model.ExternalArtifactProvider;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
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
public class ArtifactManagementTest extends AbstractIntegrationTestWithMongoDB {
    public ArtifactManagementTest() {
        LOG = LoggerFactory.getLogger(ArtifactManagementTest.class);
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.ArtifactManagement#createLocalArtifact(java.io.InputStream)}
     * .
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @Description("Test if a local artifact can be created by API including metadata.")
    public void createLocalArtifact() throws NoSuchAlgorithmException, IOException {
        // checkbaseline
        assertThat(softwareModuleRepository.findAll()).hasSize(0);
        assertThat(artifactRepository.findAll()).hasSize(0);

        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        SoftwareModule sm2 = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 2",
                "version 2", null, null);
        sm2 = softwareModuleRepository.save(sm2);

        SoftwareModule sm3 = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 3",
                "version 3", null, null);
        sm3 = softwareModuleRepository.save(sm3);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final Artifact result = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file1", false);
        final Artifact result11 = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file11", false);
        final Artifact result12 = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file12", false);
        final Artifact result2 = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm2.getId(),
                "file2", false);

        assertThat(result).isInstanceOf(LocalArtifact.class);
        assertThat(result.getSoftwareModule().getId()).isEqualTo(sm.getId());
        assertThat(result2.getSoftwareModule().getId()).isEqualTo(sm2.getId());
        assertThat(((LocalArtifact) result).getFilename()).isEqualTo("file1");
        assertThat(((LocalArtifact) result).getGridFsFileName()).isNotNull();
        assertThat(result).isNotEqualTo(result2);
        assertThat(((LocalArtifact) result).getGridFsFileName())
                .isEqualTo(((LocalArtifact) result2).getGridFsFileName());

        assertThat(artifactManagement.findLocalArtifactByFilename("file1").get(0).getSha1Hash())
                .isEqualTo(HashGeneratorUtils.generateSHA1(random));
        assertThat(artifactManagement.findLocalArtifactByFilename("file1").get(0).getMd5Hash())
                .isEqualTo(HashGeneratorUtils.generateMD5(random));

        assertThat(artifactRepository.findAll()).hasSize(4);
        assertThat(softwareModuleRepository.findAll()).hasSize(3);

        assertThat(softwareManagement.findSoftwareModuleWithDetails(sm.getId()).getArtifacts()).hasSize(3);
    }

    @Test
    @Description("Tests hard delete directly on repository.")
    public void hardDeleteSoftwareModule() throws NoSuchAlgorithmException, IOException {
        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(), "file1", false);
        assertThat(artifactRepository.findAll()).hasSize(1);

        softwareModuleRepository.deleteAll();
        assertThat(artifactRepository.findAll()).hasSize(0);
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.ArtifactManagement#createExternalArtifact(org.eclipse.hawkbit.repository.model.ExternalArtifactProvider, java.lang.String)}
     * .
     */
    @Test
    @Description("Tests the creation of an external artifact metadata element.")
    public void createExternalArtifact() {
        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        SoftwareModule sm2 = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 2",
                "version 2", null, null);
        sm2 = softwareModuleRepository.save(sm2);

        final ExternalArtifactProvider provider = artifactManagement.createExternalArtifactProvider("provider X", null,
                "https://fhghdfjgh", "/{version}/");

        ExternalArtifact result = artifactManagement.createExternalArtifact(provider, null, sm.getId());

        assertNotNull(result);
        assertThat(externalArtifactRepository.findAll()).contains(result).hasSize(1);
        assertThat(result.getSoftwareModule().getId()).isEqualTo(sm.getId());
        assertThat(result.getUrl()).isEqualTo("https://fhghdfjgh/{version}/");
        assertThat(result.getExternalArtifactProvider()).isEqualTo(provider);

        result = artifactManagement.createExternalArtifact(provider, "/test", sm2.getId());
        assertNotNull(result);
        assertThat(externalArtifactRepository.findAll()).contains(result).hasSize(2);
        assertThat(result.getUrl()).isEqualTo("https://fhghdfjgh/test");
        assertThat(result.getExternalArtifactProvider()).isEqualTo(provider);
    }

    @Test
    @Description("Tests deletio of an external artifact metadata element.")
    public void deleteExternalArtifact() {
        assertThat(artifactRepository.findAll()).isEmpty();

        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        final ExternalArtifactProvider provider = artifactManagement.createExternalArtifactProvider("provider X", null,
                "https://fhghdfjgh", "/{version}/");

        final ExternalArtifact result = artifactManagement.createExternalArtifact(provider, null, sm.getId());
        assertNotNull(result);
        assertThat(externalArtifactRepository.findAll()).contains(result).hasSize(1);

        artifactManagement.deleteExternalArtifact(result.getId());
        assertThat(externalArtifactRepository.findAll()).isEmpty();
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.ArtifactManagement#deleteLocalArtifact(java.lang.Long)}
     * .
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @Description("Tests the deletion of a local artifact including metadata.")
    public void deleteLocalArtifact() throws NoSuchAlgorithmException, IOException {
        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        SoftwareModule sm2 = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 2",
                "version 2", null, null);
        sm2 = softwareModuleRepository.save(sm2);

        assertThat(artifactRepository.findAll()).isEmpty();

        final Artifact result = artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024),
                sm.getId(), "file1", false);
        final Artifact result2 = artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024),
                sm2.getId(), "file2", false);

        assertThat(artifactRepository.findAll()).hasSize(2);

        assertThat(result.getId()).isNotNull();
        assertThat(result2.getId()).isNotNull();
        assertThat(((LocalArtifact) result).getGridFsFileName())
                .isNotEqualTo(((LocalArtifact) result2).getGridFsFileName());
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((LocalArtifact) result).getGridFsFileName()))))
                        .isNotNull();
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((LocalArtifact) result2).getGridFsFileName()))))
                        .isNotNull();

        artifactManagement.deleteLocalArtifact(result.getId());
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((LocalArtifact) result).getGridFsFileName()))))
                        .isNull();
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((LocalArtifact) result2).getGridFsFileName()))))
                        .isNotNull();

        artifactManagement.deleteLocalArtifact(result2.getId());
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((LocalArtifact) result2).getGridFsFileName()))))
                        .isNull();

        assertThat(artifactRepository.findAll()).hasSize(0);
    }

    @Test
    @Description("Trys and fails to delete local artifact with a down mongodb and checks if expected ArtifactDeleteFailedException is thrown.")
    public void deleteArtifactsWithNoMongoDb() throws UnknownHostException, IOException {
        // ensure baseline
        assertThat(artifactRepository.findAll()).isEmpty();

        // prepare test
        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        final Artifact result = artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024),
                sm.getId(), "file1", false);

        assertThat(artifactRepository.findAll()).hasSize(1);

        internalShutDownMongo();
        try {
            artifactManagement.deleteLocalArtifact(result.getId());
            fail("deletion should have failed");
        } catch (final ArtifactDeleteFailedException e) {

        }
        setupMongo();

        assertThat(artifactRepository.findAll()).hasSize(1);
        assertThat(artifactManagement.findArtifact(result.getId())).isEqualTo(result);

    }

    @Test
    @Description("Test the deletion of an artifact metadata where the binary is still linked to another "
            + "metadata element. The expected result is that the metadata is deleted but the binary kept.")
    public void deleteDuplicateArtifacts() throws NoSuchAlgorithmException, IOException {
        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        SoftwareModule sm2 = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 2",
                "version 2", null, null);
        sm2 = softwareModuleRepository.save(sm2);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final Artifact result = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file1", false);
        final Artifact result2 = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm2.getId(),
                "file2", false);

        assertThat(artifactRepository.findAll()).hasSize(2);
        assertThat(result.getId()).isNotNull();
        assertThat(result2.getId()).isNotNull();
        assertThat(((LocalArtifact) result).getGridFsFileName())
                .isEqualTo(((LocalArtifact) result2).getGridFsFileName());

        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((LocalArtifact) result).getGridFsFileName()))))
                        .isNotNull();
        artifactManagement.deleteLocalArtifact(result.getId());
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((LocalArtifact) result).getGridFsFileName()))))
                        .isNotNull();

        artifactManagement.deleteLocalArtifact(result2.getId());
        assertThat(operations.findOne(
                new Query().addCriteria(Criteria.where("filename").is(((LocalArtifact) result).getGridFsFileName()))))
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
    @Description("Loads an artifact based on given ID.")
    public void findArtifact() throws NoSuchAlgorithmException, IOException {
        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final Artifact result = artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024),
                sm.getId(), "file1", false);

        assertThat(artifactManagement.findArtifact(result.getId())).isEqualTo(result);
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.ArtifactManagement#loadLocalArtifactBinary(java.lang.Long)}
     * .
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @Description("Loads an artifact binary based on given ID.")
    public void loadStreamOfLocalArtifact() throws NoSuchAlgorithmException, IOException {
        SoftwareModule sm = new SoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final LocalArtifact result = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                sm.getId(), "file1", false);

        assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(random),
                artifactManagement.loadLocalArtifactBinary(result).getFileInputStream()));
    }

    @Test(expected = InsufficientPermissionException.class)
    @WithUser(allSpPermissions = true, removeFromAllPermission = { SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT })
    @Description("Trys and fails to load an artifact without required permission. Checks if expected InsufficientPermissionException is thrown.")
    public void loadLocalArtifactBinaryWithoutDownloadArtifactThrowsPermissionDenied() {
        artifactManagement.loadLocalArtifactBinary(new LocalArtifact());
    }

    @Test
    @Description("Searches an artifact through the relations of a software module.")
    public void findLocalArtifactBySoftwareModule() {
        SoftwareModule sm = new SoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        SoftwareModule sm2 = new SoftwareModule(osType, "name 2", "version 2", null, null);
        sm2 = softwareManagement.createSoftwareModule(sm2);

        assertThat(artifactManagement.findLocalArtifactBySoftwareModule(pageReq, sm.getId())).isEmpty();

        final Artifact result = artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024),
                sm.getId(), "file1", false);

        assertThat(artifactManagement.findLocalArtifactBySoftwareModule(pageReq, sm.getId())).hasSize(1);
    }

    @Test
    @Description("Searches an artifact through the relations of a software module and the filename.")
    public void findByFilenameAndSoftwareModule() {
        SoftwareModule sm = new SoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        assertThat(artifactManagement.findByFilenameAndSoftwareModule("file1", sm.getId())).isEmpty();

        artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(), "file1", false);
        artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(), "file2", false);

        assertThat(artifactManagement.findByFilenameAndSoftwareModule("file1", sm.getId())).hasSize(1);

    }
}

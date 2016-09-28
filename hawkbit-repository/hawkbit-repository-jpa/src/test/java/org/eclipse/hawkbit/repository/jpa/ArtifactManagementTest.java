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
import static org.junit.Assert.assertNotNull;
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
import org.eclipse.hawkbit.repository.jpa.model.JpaExternalArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaExternalArtifactProvider;
import org.eclipse.hawkbit.repository.jpa.model.JpaLocalArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ExternalArtifactProvider;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
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
        assertThat(((JpaLocalArtifact) result).getFilename()).isEqualTo("file1");
        assertThat(((JpaLocalArtifact) result).getGridFsFileName()).isNotNull();
        assertThat(result).isNotEqualTo(result2);
        assertThat(((JpaLocalArtifact) result).getGridFsFileName())
                .isEqualTo(((JpaLocalArtifact) result2).getGridFsFileName());

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
        JpaSoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
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
        JpaSoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        JpaSoftwareModule sm2 = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 2",
                "version 2", null, null);
        sm2 = softwareModuleRepository.save(sm2);

        final ExternalArtifactProvider provider = artifactManagement.createExternalArtifactProvider("provider X", null,
                "https://fhghdfjgh", "/{version}/");

        JpaExternalArtifact result = (JpaExternalArtifact) artifactManagement.createExternalArtifact(provider, null,
                sm.getId());

        assertNotNull("The result of an external artifact should not be null", result);
        assertThat(externalArtifactRepository.findAll()).contains(result).hasSize(1);
        assertThat(result.getSoftwareModule().getId()).isEqualTo(sm.getId());
        assertThat(result.getUrl()).isEqualTo("https://fhghdfjgh/{version}/");
        assertThat(result.getExternalArtifactProvider()).isEqualTo(provider);

        result = (JpaExternalArtifact) artifactManagement.createExternalArtifact(provider, "/test", sm2.getId());
        assertNotNull("The newly created external artifact should not be null", result);
        assertThat(externalArtifactRepository.findAll()).contains(result).hasSize(2);
        assertThat(result.getUrl()).isEqualTo("https://fhghdfjgh/test");
        assertThat(result.getExternalArtifactProvider()).isEqualTo(provider);
    }

    @Test
    @Description("Tests deletio of an external artifact metadata element.")
    public void deleteExternalArtifact() {
        assertThat(artifactRepository.findAll()).isEmpty();

        JpaSoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        final JpaExternalArtifactProvider provider = (JpaExternalArtifactProvider) artifactManagement
                .createExternalArtifactProvider("provider X", null, "https://fhghdfjgh", "/{version}/");

        final JpaExternalArtifact result = (JpaExternalArtifact) artifactManagement.createExternalArtifact(provider,
                null, sm.getId());
        assertNotNull("The newly created external artifact should not be null", result);
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
        JpaSoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareModuleRepository.save(sm);

        JpaSoftwareModule sm2 = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 2",
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
        assertThat(((JpaLocalArtifact) result).getGridFsFileName())
                .isNotEqualTo(((JpaLocalArtifact) result2).getGridFsFileName());
        assertThat(operations.findOne(new Query()
                .addCriteria(Criteria.where("filename").is(((JpaLocalArtifact) result).getGridFsFileName()))))
                        .isNotNull();
        assertThat(operations.findOne(new Query()
                .addCriteria(Criteria.where("filename").is(((JpaLocalArtifact) result2).getGridFsFileName()))))
                        .isNotNull();

        artifactManagement.deleteLocalArtifact(result.getId());
        assertThat(operations.findOne(new Query()
                .addCriteria(Criteria.where("filename").is(((JpaLocalArtifact) result).getGridFsFileName())))).isNull();
        assertThat(operations.findOne(new Query()
                .addCriteria(Criteria.where("filename").is(((JpaLocalArtifact) result2).getGridFsFileName()))))
                        .isNotNull();

        artifactManagement.deleteLocalArtifact(result2.getId());
        assertThat(operations.findOne(new Query()
                .addCriteria(Criteria.where("filename").is(((JpaLocalArtifact) result2).getGridFsFileName()))))
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

        final Artifact result = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm.getId(),
                "file1", false);
        final Artifact result2 = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random), sm2.getId(),
                "file2", false);

        assertThat(artifactRepository.findAll()).hasSize(2);
        assertThat(result.getId()).isNotNull();
        assertThat(result2.getId()).isNotNull();
        assertThat(((JpaLocalArtifact) result).getGridFsFileName())
                .isEqualTo(((JpaLocalArtifact) result2).getGridFsFileName());

        assertThat(operations.findOne(new Query()
                .addCriteria(Criteria.where("filename").is(((JpaLocalArtifact) result).getGridFsFileName()))))
                        .isNotNull();
        artifactManagement.deleteLocalArtifact(result.getId());
        assertThat(operations.findOne(new Query()
                .addCriteria(Criteria.where("filename").is(((JpaLocalArtifact) result).getGridFsFileName()))))
                        .isNotNull();

        artifactManagement.deleteLocalArtifact(result2.getId());
        assertThat(operations.findOne(new Query()
                .addCriteria(Criteria.where("filename").is(((JpaLocalArtifact) result).getGridFsFileName())))).isNull();
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.ArtifactManagement#findLocalArtifact(java.lang.Long)}
     * .
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @Description("Loads an local artifact based on given ID.")
    public void findLocalArtifact() throws NoSuchAlgorithmException, IOException {
        SoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final LocalArtifact result = artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024),
                sm.getId(), "file1", false);

        assertThat(artifactManagement.findLocalArtifact(result.getId())).isEqualTo(result);
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
        SoftwareModule sm = new JpaSoftwareModule(softwareManagement.findSoftwareModuleTypeByKey("os"), "name 1",
                "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        final byte random[] = RandomStringUtils.random(5 * 1024).getBytes();

        final LocalArtifact result = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                sm.getId(), "file1", false);

        assertTrue("The stored binary matches the given binary", IOUtils.contentEquals(new ByteArrayInputStream(random),
                artifactManagement.loadLocalArtifactBinary(result).getFileInputStream()));
    }

    @Test
    @WithUser(allSpPermissions = true, removeFromAllPermission = { SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT })
    @Description("Trys and fails to load an artifact without required permission. Checks if expected InsufficientPermissionException is thrown.")
    public void loadLocalArtifactBinaryWithoutDownloadArtifactThrowsPermissionDenied() {
        try {
            artifactManagement.loadLocalArtifactBinary(new JpaLocalArtifact());
            fail("Should not have worked with missing permission.");
        } catch (final InsufficientPermissionException e) {

        }
    }

    @Test
    @Description("Searches an artifact through the relations of a software module.")
    public void findLocalArtifactBySoftwareModule() {
        SoftwareModule sm = new JpaSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        SoftwareModule sm2 = new JpaSoftwareModule(osType, "name 2", "version 2", null, null);
        sm2 = softwareManagement.createSoftwareModule(sm2);

        assertThat(artifactManagement.findLocalArtifactBySoftwareModule(pageReq, sm.getId())).isEmpty();

        final Artifact result = artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024),
                sm.getId(), "file1", false);

        assertThat(artifactManagement.findLocalArtifactBySoftwareModule(pageReq, sm.getId())).hasSize(1);
    }

    @Test
    @Description("Searches an artifact through the relations of a software module and the filename.")
    public void findByFilenameAndSoftwareModule() {
        SoftwareModule sm = new JpaSoftwareModule(osType, "name 1", "version 1", null, null);
        sm = softwareManagement.createSoftwareModule(sm);

        assertThat(artifactManagement.findByFilenameAndSoftwareModule("file1", sm.getId())).isEmpty();

        artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(), "file1", false);
        artifactManagement.createLocalArtifact(new RandomGeneratedInputStream(5 * 1024), sm.getId(), "file2", false);

        assertThat(artifactManagement.findByFilenameAndSoftwareModule("file1", sm.getId())).hasSize(1);

    }
}

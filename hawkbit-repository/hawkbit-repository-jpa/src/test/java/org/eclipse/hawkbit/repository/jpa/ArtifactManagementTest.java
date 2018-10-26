/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.QuotaExceededException;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.HashGeneratorUtils;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.Test;

import com.google.common.collect.Lists;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test class for {@link ArtifactManagement}.
 */
@Features("Component Tests - Repository")
@Stories("Artifact Management")
public class ArtifactManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access react as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    public void nonExistingEntityAccessReturnsNotPresent() {
        final SoftwareModule module = testdataFactory.createSoftwareModuleOs();

        assertThat(artifactManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(artifactManagement.getByFilenameAndSoftwareModule(NOT_EXIST_ID, module.getId()).isPresent())
                .isFalse();

        assertThat(artifactManagement.findFirstBySHA1(NOT_EXIST_ID)).isNotPresent();
        assertThat(artifactManagement.loadArtifactBinary(NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = SoftwareModuleDeletedEvent.class, count = 0) })
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() throws URISyntaxException {

        final String artifactData = "test";
        final int artifactSize = artifactData.length();
        verifyThrownExceptionBy(() -> artifactManagement.create(IOUtils.toInputStream(artifactData, "UTF-8"),
                NOT_EXIST_IDL, "xxx", null, null, false, null, artifactSize), "SoftwareModule");

        verifyThrownExceptionBy(() -> artifactManagement.create(IOUtils.toInputStream(artifactData, "UTF-8"), 1234L,
                "xxx", false, artifactSize), "SoftwareModule");

        verifyThrownExceptionBy(() -> artifactManagement.delete(NOT_EXIST_IDL), "Artifact");

        verifyThrownExceptionBy(() -> artifactManagement.findBySoftwareModule(PAGE, NOT_EXIST_IDL), "SoftwareModule");
        assertThat(artifactManagement.getByFilename(NOT_EXIST_ID).isPresent()).isFalse();

        verifyThrownExceptionBy(() -> artifactManagement.getByFilenameAndSoftwareModule("xxx", NOT_EXIST_IDL),
                "SoftwareModule");
    }

    @Test
    @Description("Test if a local artifact can be created by API including metadata.")
    public void createArtifact() throws NoSuchAlgorithmException, IOException {

        // check baseline
        assertThat(softwareModuleRepository.findAll()).hasSize(0);
        assertThat(artifactRepository.findAll()).hasSize(0);

        final JpaSoftwareModule sm = softwareModuleRepository
                .save(new JpaSoftwareModule(osType, "name 1", "version 1", null, null));
        final JpaSoftwareModule sm2 = softwareModuleRepository
                .save(new JpaSoftwareModule(osType, "name 2", "version 2", null, null));
        softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 3", "version 3", null, null));

        final int artifactSize = 5 * 1024;
        final byte[] randomBytes = randomBytes(artifactSize);

        try (final InputStream inputStream1 = new ByteArrayInputStream(randomBytes);
                final InputStream inputStream2 = new ByteArrayInputStream(randomBytes);
                final InputStream inputStream3 = new ByteArrayInputStream(randomBytes);
                final InputStream inputStream4 = new ByteArrayInputStream(randomBytes);) {

            final Artifact artifact1 = createArtifactForSoftwareModule("file1", sm.getId(), artifactSize, inputStream1);
            createArtifactForSoftwareModule("file11", sm.getId(), artifactSize, inputStream2);
            createArtifactForSoftwareModule("file12", sm.getId(), artifactSize, inputStream3);
            final Artifact artifact2 = createArtifactForSoftwareModule("file2", sm2.getId(), artifactSize, inputStream4);

            assertThat(artifact1).isInstanceOf(Artifact.class);
            assertThat(artifact1.getSoftwareModule().getId()).isEqualTo(sm.getId());
            assertThat(artifact2.getSoftwareModule().getId()).isEqualTo(sm2.getId());
            assertThat(((JpaArtifact) artifact1).getFilename()).isEqualTo("file1");
            assertThat(((JpaArtifact) artifact1).getSha1Hash()).isNotNull();
            assertThat(artifact1).isNotEqualTo(artifact2);
            assertThat(((JpaArtifact) artifact1).getSha1Hash()).isEqualTo(((JpaArtifact) artifact2).getSha1Hash());

            assertThat(artifactManagement.getByFilename("file1").get().getSha1Hash())
                    .isEqualTo(HashGeneratorUtils.generateSHA1(randomBytes));
            assertThat(artifactManagement.getByFilename("file1").get().getMd5Hash())
                    .isEqualTo(HashGeneratorUtils.generateMD5(randomBytes));

            assertThat(artifactRepository.findAll()).hasSize(4);
            assertThat(softwareModuleRepository.findAll()).hasSize(3);

            assertThat(softwareModuleManagement.get(sm.getId()).get().getArtifacts()).hasSize(3);
        }

    }

    @Test
    @Description("Verifies that the quota specifying the maximum number of artifacts per software module is enforced.")
    public void createArtifactsUntilQuotaIsExceeded() throws NoSuchAlgorithmException, IOException {

        // create a software module
        final long smId = softwareModuleRepository.save(new JpaSoftwareModule(osType, "sm1", "1.0", null, null))
                .getId();

        // now create artifacts for this module until the quota is exceeded
        final long maxArtifacts = quotaManagement.getMaxArtifactsPerSoftwareModule();
        final List<Long> artifactIds = Lists.newArrayList();
        final int artifactSize = 5 * 1024;
        for (int i = 0; i < maxArtifacts; ++i) {
            artifactIds.add(createArtifactForSoftwareModule("file" + i, smId, artifactSize).getId());
        }
        assertThat(artifactRepository.findBySoftwareModuleId(PAGE, smId).getTotalElements()).isEqualTo(maxArtifacts);

        // create one mode to trigger the quota exceeded error
        assertThatExceptionOfType(QuotaExceededException.class)
                .isThrownBy(() -> createArtifactForSoftwareModule("file" + maxArtifacts, smId, artifactSize));

        // delete one of the artifacts
        artifactManagement.delete(artifactIds.get(0));
        assertThat(artifactRepository.findBySoftwareModuleId(PAGE, smId).getTotalElements())
                .isEqualTo(maxArtifacts - 1);

        // now we should be able to create an artifact again
        createArtifactForSoftwareModule("fileXYZ", smId, artifactSize);
        assertThat(artifactRepository.findBySoftwareModuleId(PAGE, smId).getTotalElements()).isEqualTo(maxArtifacts);
    }

    @Test
    @Description("Verifies that the quota specifying the maximum artifact storage is enforced (across software modules).")
    public void createArtifactsUntilStorageQuotaIsExceeded() throws NoSuchAlgorithmException, IOException {

        // create as many small artifacts as possible w/o violating the storage
        // quota
        final long maxBytes = quotaManagement.getMaxArtifactStorage();
        final List<Long> artifactIds = Lists.newArrayList();

        // choose an artifact size which does not violate the max file size
        final int artifactSize = Math.toIntExact(quotaManagement.getMaxArtifactSize() / 10);
        final int numArtifacts = Math.toIntExact(maxBytes / artifactSize);
        for (int i = 0; i < numArtifacts; ++i) {
            final JpaSoftwareModule sm = softwareModuleRepository
                    .save(new JpaSoftwareModule(osType, "smd" + i, "1.0", null, null));
            artifactIds.add(createArtifactForSoftwareModule("file" + i, sm.getId(), artifactSize).getId());
        }

        // upload one more artifact to trigger the quota exceeded error
        final JpaSoftwareModule sm = softwareModuleRepository
                .save(new JpaSoftwareModule(osType, "smd" + numArtifacts, "1.0", null, null));
        assertThatExceptionOfType(QuotaExceededException.class)
                .isThrownBy(() -> createArtifactForSoftwareModule("file" + numArtifacts, sm.getId(), artifactSize));

        // delete one of the artifacts
        artifactManagement.delete(artifactIds.get(0));

        // now we should be able to create an artifact again
        createArtifactForSoftwareModule("fileXYZ", sm.getId(), artifactSize);
    }

    @Test
    @Description("Verifies that the quota specifying the maximum artifact storage is enforced (across software modules).")
    public void createArtifactWhichExceedsMaxStorage() throws NoSuchAlgorithmException, IOException {

        // create one artifact which exceeds the storage quota at once
        final long maxBytes = quotaManagement.getMaxArtifactStorage();
        final JpaSoftwareModule sm = softwareModuleRepository
                .save(new JpaSoftwareModule(osType, "smd345", "1.0", null, null));
        assertThatExceptionOfType(QuotaExceededException.class).isThrownBy(
                () -> createArtifactForSoftwareModule("file345", sm.getId(), Math.toIntExact(maxBytes) + 128));
    }

    @Test
    @Description("Verifies that you cannot create artifacts which exceed the configured maximum size.")
    public void createArtifactFailsIfTooLarge() throws NoSuchAlgorithmException, IOException {

        // create a software module
        final JpaSoftwareModule sm1 = softwareModuleRepository
                .save(new JpaSoftwareModule(osType, "sm1", "1.0", null, null));

        // create an artifact that exceeds the configured quota
        final long maxSize = quotaManagement.getMaxArtifactSize();
        assertThatExceptionOfType(QuotaExceededException.class)
                .isThrownBy(() -> createArtifactForSoftwareModule("file", sm1.getId(), Math.toIntExact(maxSize) + 8));
    }

    @Test
    @Description("Tests hard delete directly on repository.")
    public void hardDeleteSoftwareModule() throws NoSuchAlgorithmException, IOException {

        final JpaSoftwareModule sm = softwareModuleRepository
                .save(new JpaSoftwareModule(osType, "name 1", "version 1", null, null));

        createArtifactForSoftwareModule("file1", sm.getId(), 5 * 1024);
        assertThat(artifactRepository.findAll()).hasSize(1);

        softwareModuleRepository.deleteAll();
        assertThat(artifactRepository.findAll()).hasSize(0);
    }

    /**
     * Test method for
     * {@link org.eclipse.hawkbit.repository.ArtifactManagement#delete(java.lang.Long)}
     * .
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @Description("Tests the deletion of a local artifact including metadata.")
    public void deleteArtifact() throws NoSuchAlgorithmException, IOException {

        final JpaSoftwareModule sm = softwareModuleRepository
                .save(new JpaSoftwareModule(osType, "name 1", "version 1", null, null));
        final JpaSoftwareModule sm2 = softwareModuleRepository
                .save(new JpaSoftwareModule(osType, "name 2", "version 2", null, null));

        assertThat(artifactRepository.findAll()).isEmpty();

        final int artifactSize = 5 * 1024;
        try (final InputStream inputStream1 = new RandomGeneratedInputStream(artifactSize);
                final InputStream inputStream2 = new RandomGeneratedInputStream(artifactSize)) {

            final Artifact artifact1 = createArtifactForSoftwareModule("file1", sm.getId(), artifactSize, inputStream1);
            final Artifact artifact2 = createArtifactForSoftwareModule("file2", sm2.getId(), artifactSize, inputStream2);

            assertThat(artifactRepository.findAll()).hasSize(2);

            assertThat(artifact1.getId()).isNotNull();
            assertThat(artifact2.getId()).isNotNull();
            assertThat(((JpaArtifact) artifact1).getSha1Hash()).isNotEqualTo(((JpaArtifact) artifact2).getSha1Hash());

            assertThat(binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), artifact1.getSha1Hash()))
                    .isNotNull();
            assertThat(
                    binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), artifact2.getSha1Hash()))
                            .isNotNull();

            artifactManagement.delete(artifact1.getId());

            assertThat(binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), artifact1.getSha1Hash()))
                    .isNull();
            assertThat(
                    binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), artifact2.getSha1Hash()))
                            .isNotNull();

            artifactManagement.delete(artifact2.getId());
            assertThat(
                    binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), artifact2.getSha1Hash()))
                            .isNull();

            assertThat(artifactRepository.findAll()).hasSize(0);
        }
    }

    @Test
    @Description("Test the deletion of an artifact metadata where the binary is still linked to another "
            + "metadata element. The expected result is that the metadata is deleted but the binary kept.")
    public void deleteDuplicateArtifacts() throws NoSuchAlgorithmException, IOException {

        final JpaSoftwareModule sm = softwareModuleRepository
                .save(new JpaSoftwareModule(osType, "name 1", "version 1", null, null));
        final JpaSoftwareModule sm2 = softwareModuleRepository
                .save(new JpaSoftwareModule(osType, "name 2", "version 2", null, null));

        final int artifactSize = 5 * 1024;
        final byte[] randomBytes = randomBytes(artifactSize);

        try (final InputStream inputStream1 = new ByteArrayInputStream(randomBytes);
                final InputStream inputStream2 = new ByteArrayInputStream(randomBytes)) {
            final Artifact artifact1 = createArtifactForSoftwareModule("file1", sm.getId(), artifactSize, inputStream1);
            final Artifact artifact2 = createArtifactForSoftwareModule("file2", sm2.getId(), artifactSize, inputStream2);

            assertThat(artifactRepository.findAll()).hasSize(2);
            assertThat(artifact1.getId()).isNotNull();
            assertThat(artifact2.getId()).isNotNull();
            assertThat(((JpaArtifact) artifact1).getSha1Hash()).isEqualTo(((JpaArtifact) artifact2).getSha1Hash());

            assertThat(binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), artifact1.getSha1Hash()))
                    .isNotNull();
            artifactManagement.delete(artifact1.getId());
            assertThat(binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), artifact1.getSha1Hash()))
                    .isNotNull();

            artifactManagement.delete(artifact2.getId());
            assertThat(binaryArtifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), artifact1.getSha1Hash()))
                    .isNull();
        }
    }

    @Test
    @Description("Loads an local artifact based on given ID.")
    public void findArtifact() throws NoSuchAlgorithmException, IOException {
        final int artifactSize = 5 * 1024;
        try (final InputStream inputStream = new RandomGeneratedInputStream(artifactSize)) {
            final Artifact artifact = createArtifactForSoftwareModule("file1",
                    testdataFactory.createSoftwareModuleOs().getId(), artifactSize, inputStream);
            assertThat(artifactManagement.get(artifact.getId()).get()).isEqualTo(artifact);
        }
    }

    @Test
    @Description("Loads an artifact binary based on given ID.")
    public void loadStreamOfArtifact() throws NoSuchAlgorithmException, IOException {
        final int artifactSize = 5 * 1024;
        final byte[] randomBytes = randomBytes(artifactSize);
        try (final InputStream input = new ByteArrayInputStream(randomBytes)) {
            final Artifact artifact = createArtifactForSoftwareModule("file1",
                    testdataFactory.createSoftwareModuleOs().getId(), artifactSize, input);
            try (final InputStream inputStream = artifactManagement.loadArtifactBinary(artifact.getSha1Hash()).get()
                    .getFileInputStream()) {
                assertTrue("The stored binary matches the given binary",
                        IOUtils.contentEquals(new ByteArrayInputStream(randomBytes), inputStream));
            }
        }
    }

    @Test
    @WithUser(allSpPermissions = true, removeFromAllPermission = { SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT })
    @Description("Trys and fails to load an artifact without required permission. Checks if expected InsufficientPermissionException is thrown.")
    public void loadArtifactBinaryWithoutDownloadArtifactThrowsPermissionDenied() {
        try {
            artifactManagement.loadArtifactBinary("123");
            fail("Should not have worked with missing permission.");
        } catch (final InsufficientPermissionException e) {

        }
    }

    @Test
    @Description("Searches an artifact through the relations of a software module.")
    public void findArtifactBySoftwareModule() throws IOException {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        assertThat(artifactManagement.findBySoftwareModule(PAGE, sm.getId())).isEmpty();

        final int artifactSize = 5 * 1024;
        try (final InputStream input = new RandomGeneratedInputStream(artifactSize)) {
            createArtifactForSoftwareModule("file1", sm.getId(), artifactSize, input);
            assertThat(artifactManagement.findBySoftwareModule(PAGE, sm.getId())).hasSize(1);
        }
    }

    @Test
    @Description("Searches an artifact through the relations of a software module and the filename.")
    public void findByFilenameAndSoftwareModule() throws IOException {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        assertThat(artifactManagement.getByFilenameAndSoftwareModule("file1", sm.getId())).isNotPresent();

        final int artifactSize = 5 * 1024;
        try (final InputStream inputStream1 = new RandomGeneratedInputStream(artifactSize);
                final InputStream inputStream2 = new RandomGeneratedInputStream(artifactSize)) {
            createArtifactForSoftwareModule("file1", sm.getId(), artifactSize, inputStream1);
            createArtifactForSoftwareModule("file2", sm.getId(), artifactSize, inputStream2);
            assertThat(artifactManagement.getByFilenameAndSoftwareModule("file1", sm.getId())).isPresent();
        }

    }

    private Artifact createArtifactForSoftwareModule(final String filename, final long moduleId, final int artifactSize)
            throws IOException {
        final byte[] randomBytes = randomBytes(artifactSize);
        try (final InputStream inputStream = new ByteArrayInputStream(randomBytes)) {
            return createArtifactForSoftwareModule(filename, moduleId, artifactSize, inputStream);
        }
    }

    private Artifact createArtifactForSoftwareModule(final String filename, final long moduleId, final int artifactSize,
            final InputStream inputStream) throws IOException {
        return artifactManagement.create(inputStream, moduleId, filename, false, artifactSize);
    }

    private static byte[] randomBytes(final int len) {
        return RandomStringUtils.randomAlphanumeric(len).getBytes();
    }
}

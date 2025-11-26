/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Callable;

import jakarta.validation.ConstraintViolationException;

import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.artifact.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.artifact.exception.FileSizeQuotaExceededException;
import org.eclipse.hawkbit.artifact.exception.StorageQuotaExceededException;
import org.eclipse.hawkbit.artifact.model.ArtifactHashes;
import org.eclipse.hawkbit.artifact.model.ArtifactStream;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.InvalidMd5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSha1HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSha256HashException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.RandomGeneratedInputStream;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link ArtifactManagement}.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Artifact Management
 */
class ArtifactManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Verifies that management get access react as specfied on calls for non existing entities by means of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = SoftwareModuleCreatedEvent.class, count = 1) })
    void nonExistingEntityAccessReturnsNotPresent() {
        final SoftwareModule module = testdataFactory.createSoftwareModuleOs();

        final long moduleId = module.getId();
        final boolean encrypted = module.isEncrypted();
        assertThatExceptionOfType(ArtifactBinaryNotFoundException.class)
                .isThrownBy(() -> artifactManagement.getArtifactStream(NOT_EXIST_ID, moduleId, encrypted));
    }

    @Test
    @ExpectEvents()
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final String artifactData = "test";
        final int artifactSize = artifactData.length();
        verifyThrownExceptionBy(
                () -> artifactManagement.create(new ArtifactUpload(
                        IOUtils.toInputStream(artifactData, "UTF-8"),
                        null, artifactSize, null, NOT_EXIST_IDL, "xxx", false)), "SoftwareModule");

        verifyThrownExceptionBy(
                () -> artifactManagement.create(new ArtifactUpload(
                        IOUtils.toInputStream(artifactData, "UTF-8"),
                        null, artifactSize, null, NOT_EXIST_IDL, "xxx", false)), "SoftwareModule");

        verifyThrownExceptionBy(() -> artifactManagement.delete(NOT_EXIST_IDL), "Artifact");
    }

    /**
     * Test if a local artifact can be created by API including metadata.
     */
    @Test
    void createArtifact() throws IOException {
        // check baseline
        assertThat(softwareModuleRepository.findAll()).isEmpty();
        assertThat(artifactRepository.findAll()).isEmpty();

        final JpaSoftwareModule sm = softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 1", "version 1"));
        final JpaSoftwareModule sm2 = softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 2", "version 2"));
        softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 3", "version 3"));

        final int artifactSize = 5 * 1024;
        final byte[] randomBytes = randomBytes(artifactSize);

        try (final InputStream inputStream1 = new ByteArrayInputStream(randomBytes);
                final InputStream inputStream2 = new ByteArrayInputStream(randomBytes);
                final InputStream inputStream3 = new ByteArrayInputStream(randomBytes);
                final InputStream inputStream4 = new ByteArrayInputStream(randomBytes)) {
            final Artifact artifact1 = createArtifactForSoftwareModule("file1", sm.getId(), artifactSize, inputStream1);
            createArtifactForSoftwareModule("file11", sm.getId(), artifactSize, inputStream2);
            createArtifactForSoftwareModule("file12", sm.getId(), artifactSize, inputStream3);
            final Artifact artifact2 = createArtifactForSoftwareModule("file2", sm2.getId(), artifactSize, inputStream4);

            assertThat(artifact1).isInstanceOf(Artifact.class);
            assertThat(artifact1.getSoftwareModule().getId()).isEqualTo(sm.getId());
            assertThat(artifact2.getSoftwareModule().getId()).isEqualTo(sm2.getId());
            assertThat(artifact1.getFilename()).isEqualTo("file1");
            assertThat(artifact1.getSha1Hash()).isNotNull();
            assertThat(artifact1).isNotEqualTo(artifact2);
            assertThat(artifact1.getSha1Hash()).isEqualTo(artifact2.getSha1Hash());

            assertThat(artifactRepository.findAll()).hasSize(4);
            assertThat(softwareModuleRepository.findAll()).hasSize(3);

            assertThat(softwareModuleManagement.get(sm.getId()).getArtifacts()).hasSize(3);
        }
    }

    /**
     * Verifies that artifact management does not create artifacts with illegal filename.
     */
    @Test
    void entityQueryWithIllegalFilenameThrowsException() {
        final String illegalFilename = "<img src=ernw onerror=alert(1)>.xml";
        final String artifactData = "test";
        final int artifactSize = artifactData.length();

        final long smID = softwareModuleRepository.save(new JpaSoftwareModule(osType, "smIllegalFilenameTest", "1.0")).getId();
        final ArtifactUpload artifactUpload = new ArtifactUpload(
                IOUtils.toInputStream(artifactData, "UTF-8"), null, artifactSize, null, smID, illegalFilename, false);
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> artifactManagement.create(artifactUpload));
        assertThat(softwareModuleManagement.get(smID).getArtifacts()).isEmpty();
    }

    /**
     * Verifies that the quota specifying the maximum number of artifacts per software module is enforced.
     */
    @Test
    void failIfMaxArtifactsPerSoftwareModuleQuotaIsExceeded() throws IOException {
        // create a software module
        final long smId = softwareModuleRepository.save(new JpaSoftwareModule(osType, "sm1", "1.0")).getId();

        // now create artifacts for this module until the quota is exceeded
        final long maxArtifacts = quotaManagement.getMaxArtifactsPerSoftwareModule();
        final List<Long> artifactIds = new ArrayList<>();
        final int artifactSize = 5 * 1024;
        for (int i = 0; i < maxArtifacts; ++i) {
            artifactIds.add(createArtifactForSoftwareModule("file" + i, smId, artifactSize).getId());
        }

        // create one mode to trigger the quota exceeded error
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> createArtifactForSoftwareModule("file" + maxArtifacts, smId, artifactSize));

        // delete one of the artifacts
        artifactManagement.delete(artifactIds.get(0));

        // now we should be able to create an artifact again
        createArtifactForSoftwareModule("fileXYZ", smId, artifactSize);
    }

    @Test
    void failIfMaxArtifactStorageQuotaIsExceeded() throws IOException {
        // create as many small artifacts as possible w/o violating the storage quota
        final long maxBytes = quotaManagement.getMaxArtifactStorage();
        final List<Long> artifactIds = new ArrayList<>();

        // choose an artifact size which does not violate the max file size
        final int artifactSize = Math.toIntExact(quotaManagement.getMaxArtifactSize() / 10);
        final int numArtifacts = Math.toIntExact(maxBytes / artifactSize);
        for (int i = 0; i < numArtifacts; ++i) {
            final JpaSoftwareModule sm = softwareModuleRepository.save(new JpaSoftwareModule(osType, "smd" + i, "1.0"));
            artifactIds.add(createArtifactForSoftwareModule("file" + i, sm.getId(), artifactSize).getId());
        }

        // upload one more artifact to trigger the quota exceeded error
        final long smId = softwareModuleRepository.save(new JpaSoftwareModule(osType, "smd" + numArtifacts, "1.0")).getId();
        assertThatExceptionOfType(StorageQuotaExceededException.class)
                .isThrownBy(() -> createArtifactForSoftwareModule("file" + numArtifacts, smId, artifactSize));

        // delete one of the artifacts
        artifactManagement.delete(artifactIds.get(0));

        // now we should be able to create an artifact again
        createArtifactForSoftwareModule("fileXYZ", smId, artifactSize);
    }

    /**
     * Verifies that you cannot create artifacts which exceed the configured maximum size.
     */
    @Test
    void createArtifactFailsIfTooLarge() {
        // create a software module
        final long smId = softwareModuleRepository.save(new JpaSoftwareModule(osType, "sm1", "1.0")).getId();

        // create an artifact that exceeds the configured quota
        final int artifactSize = Math.toIntExact(quotaManagement.getMaxArtifactSize()) + 8;
        assertThatExceptionOfType(FileSizeQuotaExceededException.class)
                .isThrownBy(() -> createArtifactForSoftwareModule("file", smId, artifactSize));
    }

    /**
     * Tests hard delete directly on repository.
     */
    @Test
    void hardDeleteSoftwareModule() throws IOException {
        final JpaSoftwareModule sm = softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 1", "version 1"));

        createArtifactForSoftwareModule("file1", sm.getId(), 5 * 1024);
        assertThat(artifactRepository.findAll()).hasSize(1);

        softwareModuleRepository.deleteAll();
        assertThat(artifactRepository.findAll()).isEmpty();
    }

    /**
     * Test method for {@link org.eclipse.hawkbit.repository.ArtifactManagement#delete(long)}.
     * Tests the deletion of a local artifact including metadata.
     */
    @Test
    void deleteArtifact() throws IOException {
        final JpaSoftwareModule sm = softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 1", "version 1"));
        final JpaSoftwareModule sm2 = softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 2", "version 2"));

        assertThat(artifactRepository.findAll()).isEmpty();

        final int artifactSize = 5 * 1024;
        try (final InputStream inputStream1 = new RandomGeneratedInputStream(artifactSize);
                final InputStream inputStream2 = new RandomGeneratedInputStream(artifactSize)) {
            final Artifact artifact1 = createArtifactForSoftwareModule("file1", sm.getId(), artifactSize, inputStream1);
            final Artifact artifact2 = createArtifactForSoftwareModule("file2", sm2.getId(), artifactSize, inputStream2);

            assertThat(artifactRepository.findAll()).hasSize(2);

            assertThat(artifact1.getId()).isNotNull();
            assertThat(artifact2.getId()).isNotNull();
            assertThat(artifact1.getSha1Hash()).isNotEqualTo(artifact2.getSha1Hash());

            final String currentTenant = AccessContext.tenant();
            assertThat(artifactStorage.getBySha1(currentTenant, artifact1.getSha1Hash())).isNotNull();
            assertThat(artifactStorage.getBySha1(currentTenant, artifact2.getSha1Hash())).isNotNull();

            artifactManagement.delete(artifact1.getId());

            assertThat(artifactRepository.findAll()).hasSize(1);

            final String sha1Hash = artifact1.getSha1Hash();
            assertThatExceptionOfType(ArtifactBinaryNotFoundException.class)
                    .isThrownBy(() -> artifactStorage.getBySha1(currentTenant, sha1Hash));

            assertThat(artifactStorage.getBySha1(currentTenant, artifact2.getSha1Hash())).isNotNull();

            artifactManagement.delete(artifact2.getId());
            final String sha1Hash2 = artifact2.getSha1Hash();
            assertThatExceptionOfType(ArtifactBinaryNotFoundException.class)
                    .isThrownBy(() -> artifactStorage.getBySha1(currentTenant, sha1Hash2));

            assertThat(artifactRepository.findAll()).isEmpty();
        }
    }

    /**
     * Test the deletion of an artifact metadata where the binary is still linked to another metadata element.
     * The expected result is that the metadata is deleted but the binary kept.
     */
    @Test
    void deleteDuplicateArtifacts() throws IOException {
        final JpaSoftwareModule sm = softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 1", "version 1"));
        final JpaSoftwareModule sm2 = softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 2", "version 2"));

        final int artifactSize = 5 * 1024;
        final byte[] randomBytes = randomBytes(artifactSize);

        try (final InputStream inputStream1 = new ByteArrayInputStream(randomBytes);
                final InputStream inputStream2 = new ByteArrayInputStream(randomBytes)) {
            final Artifact artifact1 = createArtifactForSoftwareModule("file1", sm.getId(), artifactSize, inputStream1);
            final Artifact artifact2 = createArtifactForSoftwareModule("file2", sm2.getId(), artifactSize, inputStream2);

            assertThat(artifact1.getId()).isNotNull();
            assertThat(artifact2.getId()).isNotNull();
            assertThat((artifact1).getSha1Hash()).isEqualTo(artifact2.getSha1Hash());
            assertThat(artifactRepository.findAll()).hasSize(2);
            final String currentTenant = AccessContext.tenant();
            assertThat(artifactStorage.getBySha1(currentTenant, artifact1.getSha1Hash())).isNotNull();

            artifactManagement.delete(artifact1.getId());
            assertThat(artifactRepository.existsById(artifact1.getId())).isFalse();
            assertThat(artifactRepository.findAll()).hasSize(1);
            assertThat(artifactStorage.getBySha1(currentTenant, artifact1.getSha1Hash())).isNotNull();

            artifactManagement.delete(artifact2.getId());
            final String sha1Hash = artifact1.getSha1Hash();
            assertThatExceptionOfType(ArtifactBinaryNotFoundException.class)
                    .isThrownBy(() -> artifactStorage.getBySha1(currentTenant, sha1Hash));
            assertThat(artifactRepository.findAll()).isEmpty();
        }
    }

    /**
     * Verifies that you cannot delete an artifact which exists with the same hash, in the same tenant and the SoftwareModule is not deleted .
     */
    @Test
    void deleteArtifactWithSameHashAndSoftwareModuleIsNotDeletedInSameTenants() throws IOException {
        final JpaSoftwareModule sm = softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 1", "version 1"));
        final JpaSoftwareModule sm2 = softwareModuleRepository.save(new JpaSoftwareModule(osType, "name 2", "version 2"));

        final int artifactSize = 5 * 1024;
        final byte[] randomBytes = randomBytes(artifactSize);

        try (final InputStream inputStream1 = new ByteArrayInputStream(randomBytes);
                final InputStream inputStream2 = new ByteArrayInputStream(randomBytes)) {
            final Artifact artifact1 = createArtifactForSoftwareModule("file1", sm.getId(), artifactSize, inputStream1);
            final Artifact artifact2 = createArtifactForSoftwareModule("file2", sm2.getId(), artifactSize, inputStream2);

            assertEqualFileContents(
                    artifactManagement.getArtifactStream(artifact2.getSha1Hash(), sm2.getId(), sm2.isEncrypted()), randomBytes);

            assertThat(artifactRepository.findAll()).hasSize(2);

            assertThat(artifact1.getSha1Hash()).isEqualTo(artifact2.getSha1Hash());

            assertThat(artifactRepository.countBySha1HashAndTenantAndSoftwareModuleDeletedIsFalse(
                    artifact1.getSha1Hash(), artifact1.getTenant()))
                    .isGreaterThan(1);

            artifactRepository.deleteById(artifact1.getId());
            assertThat(artifactRepository.findAll()).hasSize(1);

            assertThat(artifactRepository.countBySha1HashAndTenantAndSoftwareModuleDeletedIsFalse(
                    artifact2.getSha1Hash(), artifact2.getTenant()))
                    .isLessThanOrEqualTo(1);

            artifactRepository.deleteById(artifact2.getId());
            assertThat(artifactRepository.findAll()).isEmpty();
        }
    }

    /**
     * Verifies that you can not delete artifacts from another tenant which exists in another tenant with the same hash and the SoftwareModule is not deleted
     */
    @Test
    void deleteArtifactWithSameHashAndSoftwareModuleIsNotDeletedInDifferentTenants() throws Exception {
        final String tenant1 = "mytenant";
        final String tenant2 = "tenant2";

        final SoftwareModule module = createSoftwareModuleForTenant(tenant1);
        final SoftwareModule module2 = createSoftwareModuleForTenant(tenant2);

        createArtifactForTenant(tenant1, "myInput", module.getId(), "myFirstFile");
        createArtifactForTenant(tenant2, "myInput", module2.getId(), "mySecondFile");
        final Artifact artifactTenant2 = createArtifactForTenant(tenant2, "myInput", module2.getId(), "myThirdFile");

        verifyTenantArtifactCountIs(tenant1, 1);
        verifyTenantArtifactCountIs(tenant2, 2);

        assertThat(runAsTenant(tenant1, () -> artifactRepository
                .countBySha1HashAndTenantAndSoftwareModuleDeletedIsFalse(artifactTenant2.getSha1Hash(), tenant2)))
                .isLessThanOrEqualTo(1);
        runAsTenant(tenant2, () -> {
            artifactRepository.deleteById(artifactTenant2.getId());
            return null;
        });
        verifyTenantArtifactCountIs(tenant1, 1);
        verifyTenantArtifactCountIs(tenant2, 1);
    }

    /**
     * Loads an artifact binary based on given ID.
     */
    @Test
    void loadStreamOfArtifact() throws IOException {
        final int artifactSize = 5 * 1024;
        final byte[] randomBytes = randomBytes(artifactSize);
        try (final InputStream input = new ByteArrayInputStream(randomBytes)) {
            final SoftwareModule smOs = testdataFactory.createSoftwareModuleOs();
            final Artifact artifact = createArtifactForSoftwareModule("file1", smOs.getId(), artifactSize, input);
            assertEqualFileContents(
                    artifactManagement.getArtifactStream(artifact.getSha1Hash(), smOs.getId(), smOs.isEncrypted()), randomBytes);
        }
    }

    /**
     * Trys and fails to load an artifact without required permission. Checks if expected InsufficientPermissionException is thrown.
     */
    @Test
    @WithUser(allSpPermissions = true, removeFromAllPermission = {
            SpPermission.READ_SOFTWARE_MODULE_ARTIFACT,
            SpRole.CONTROLLER_ROLE, SpRole.CONTROLLER_ROLE_ANONYMOUS })
    void getArtifactBinaryWithoutDownloadArtifactThrowsPermissionDenied() {
        assertThatExceptionOfType(InsufficientPermissionException.class)
                .as("Should not have worked with missing permission.")
                .isThrownBy(() -> artifactManagement.getArtifactStream("123", 1, false));
    }

    /**
     * Verifies that creation of an artifact with none matching hashes fails.
     */
    @Test
    void createArtifactWithNoneMatchingHashes() throws IOException, NoSuchAlgorithmException {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte[] testData = randomBytes(100);
        final ArtifactHashes artifactHashes = calcHashes(testData);

        try (final InputStream inputStream = new ByteArrayInputStream(testData)) {
            final ArtifactUpload artifactUploadWithInvalidSha1 = new ArtifactUpload(
                    inputStream, null, testData.length, new ArtifactHashes("sha1", artifactHashes.md5(), artifactHashes.sha256()),
                    sm.getId(), "test-file", false);
            assertThatExceptionOfType(InvalidSha1HashException.class)
                    .isThrownBy(() -> artifactManagement.create(artifactUploadWithInvalidSha1));
        }

        try (final InputStream inputStream = new ByteArrayInputStream(testData)) {
            final ArtifactUpload artifactUploadWithInvalidMd5 = new ArtifactUpload(
                    inputStream, null, testData.length, new ArtifactHashes(artifactHashes.sha1(), "md5", artifactHashes.sha256()),
                    sm.getId(), "test-file", false);
            assertThatExceptionOfType(InvalidMd5HashException.class)
                    .isThrownBy(() -> artifactManagement.create(artifactUploadWithInvalidMd5));
        }

        try (final InputStream inputStream = new ByteArrayInputStream(testData)) {
            final ArtifactUpload artifactUploadWithInvalidSha256 = new ArtifactUpload(
                    inputStream, null, testData.length, new ArtifactHashes(artifactHashes.sha1(), artifactHashes.md5(), "sha256"),
                    sm.getId(), "test-file", false);
            assertThatExceptionOfType(InvalidSha256HashException.class)
                    .isThrownBy(() -> artifactManagement.create(artifactUploadWithInvalidSha256));
        }
    }

    /**
     * Verifies that creation of an artifact with matching hashes works.
     */
    @Test
    void createArtifactWithMatchingHashes() throws IOException, NoSuchAlgorithmException {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final byte[] testData = randomBytes(100);
        final ArtifactHashes artifactHashes = calcHashes(testData);

        try (final InputStream inputStream = new ByteArrayInputStream(testData)) {
            final ArtifactUpload artifactUpload = new ArtifactUpload(
                    inputStream, null, testData.length,
                    new ArtifactHashes(artifactHashes.sha1(), artifactHashes.md5(), artifactHashes.sha256()),
                    sm.getId(), "test-file", false);
            final Artifact createdArtifact = artifactManagement.create(artifactUpload);
            assertThat(createdArtifact.getSha1Hash()).isEqualTo(artifactHashes.sha1());
            assertThat(createdArtifact.getMd5Hash()).isEqualTo(artifactHashes.md5());
            assertThat(createdArtifact.getSha256Hash()).isEqualTo(artifactHashes.sha256());
        }

        assertThat(artifactManagement.getArtifactStream(artifactHashes.sha1(), sm.getId(), sm.isEncrypted())).isNotNull();
    }

    /**
     * Verifies that creation of an existing artifact returns a full hash list.
     */
    @Test
    void createExistingArtifactReturnsFullHashList() throws IOException, NoSuchAlgorithmException {
        final SoftwareModule smOs = testdataFactory.createSoftwareModuleOs();
        final SoftwareModule smApp = testdataFactory.createSoftwareModuleApp();

        final byte[] testData = randomBytes(100);
        final ArtifactHashes artifactHashes = calcHashes(testData);

        try (final InputStream inputStream = new ByteArrayInputStream(testData)) {
            final ArtifactUpload artifactUpload = new ArtifactUpload(
                    inputStream, null, testData.length,
                    new ArtifactHashes(artifactHashes.sha1(), artifactHashes.md5(), artifactHashes.sha256()),
                    smOs.getId(), "test-file", false);
            final Artifact artifact = artifactManagement.create(artifactUpload);
            assertThat(artifact).isNotNull();
        }

        assertThat(artifactManagement.getArtifactStream(artifactHashes.sha1(), smOs.getId(), smOs.isEncrypted())).isNotNull();

        try (final InputStream inputStream = new ByteArrayInputStream(testData)) {
            final ArtifactUpload existingArtifactUpload = new ArtifactUpload(
                    inputStream, null, testData.length, null, smApp.getId(), "test-file", false);
            final Artifact createdArtifact = artifactManagement.create(existingArtifactUpload);
            assertThat(createdArtifact.getSha1Hash()).isEqualTo(artifactHashes.sha1());
            assertThat(createdArtifact.getMd5Hash()).isEqualTo(artifactHashes.md5());
            assertThat(createdArtifact.getSha256Hash()).isEqualTo(artifactHashes.sha256());
        }
    }

    private ArtifactHashes calcHashes(final byte[] input) throws NoSuchAlgorithmException {
        final String sha1Hash = toBase16Hash("SHA1", input);
        final String md5Hash = toBase16Hash("MD5", input);
        final String sha256Hash = toBase16Hash("SHA-256", input);

        return new ArtifactHashes(sha1Hash, md5Hash, sha256Hash);
    }

    private String toBase16Hash(final String algorithm, final byte[] input) throws NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        messageDigest.update(input);
        return HexFormat.of().withLowerCase().formatHex(messageDigest.digest());
    }

    private Artifact createArtifactForSoftwareModule(final String filename, final long moduleId, final int artifactSize) throws IOException {
        final byte[] randomBytes = randomBytes(artifactSize);
        try (final InputStream inputStream = new ByteArrayInputStream(randomBytes)) {
            return createArtifactForSoftwareModule(filename, moduleId, artifactSize, inputStream);
        }
    }

    private Artifact createArtifactForSoftwareModule(
            final String filename, final long moduleId, final int artifactSize, final InputStream inputStream) {
        return artifactManagement.create(new ArtifactUpload(inputStream, null, artifactSize, null, moduleId, filename, false));
    }

    private <T> T runAsTenant(final String tenant, final Callable<T> callable) throws Exception {
        return SecurityContextSwitch.callAs(SecurityContextSwitch.withUserAndTenantAllSpPermissions("user", tenant), callable);
    }

    private SoftwareModule createSoftwareModuleForTenant(final String tenant) throws Exception {
        return runAsTenant(tenant, () -> testdataFactory.createSoftwareModuleApp());
    }

    private Artifact createArtifactForTenant(final String tenant, final String artifactData, final long moduleId, final String filename)
            throws Exception {
        return runAsTenant(tenant, () -> testdataFactory.createArtifact(artifactData, moduleId, filename));
    }

    private void verifyTenantArtifactCountIs(final String tenant, final int count) throws Exception {
        assertThat(runAsTenant(tenant, () -> artifactRepository.findAll())).hasSize(count);
    }

    private void assertEqualFileContents(final ArtifactStream artifact, final byte[] randomBytes) throws IOException {
        try (final InputStream inputStream = artifact) {
            assertTrue(
                    IOUtils.contentEquals(new ByteArrayInputStream(randomBytes), inputStream),
                    "The stored binary matches the given binary");
        }
    }
}
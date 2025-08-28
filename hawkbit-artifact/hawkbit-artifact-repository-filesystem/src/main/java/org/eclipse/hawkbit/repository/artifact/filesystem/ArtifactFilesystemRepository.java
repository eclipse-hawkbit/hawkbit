/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.artifact.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.eclipse.hawkbit.repository.artifact.AbstractArtifactRepository;
import org.eclipse.hawkbit.repository.artifact.ArtifactRepository;
import org.eclipse.hawkbit.repository.artifact.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.repository.artifact.exception.ArtifactStoreException;
import org.eclipse.hawkbit.repository.artifact.model.AbstractDbArtifact;
import org.eclipse.hawkbit.repository.artifact.model.DbArtifactHash;
import org.springframework.validation.annotation.Validated;

/**
 * Implementation of the {@link ArtifactRepository} to store artifacts on the file-system. The files are stored by their SHA1 hash of the
 * artifact binary. Duplicate files with the same SHA1 hash will only be stored once.
 * <p/>
 * All files are stored flat in one base directory configured in the
 * {@link ArtifactFilesystemProperties#getPath()}.
 * <p/>
 * Due to the limit of many file-systems of files within one directory, the files are stored in different subdirectories based on the last four
 * digits of the SHA1-hash {@code (/basepath/[two digit sha1]/[two digit sha1])}.
 */
@Validated
public class ArtifactFilesystemRepository extends AbstractArtifactRepository {

    private final ArtifactFilesystemProperties artifactResourceProperties;

    public ArtifactFilesystemRepository(final ArtifactFilesystemProperties artifactResourceProperties) {
        this.artifactResourceProperties = artifactResourceProperties;
    }

    @Override
    public void deleteBySha1(final String tenant, final String sha1Hash) {
        FileUtils.deleteQuietly(getFile(tenant, sha1Hash));
    }

    @Override
    public AbstractDbArtifact getBySha1(final String tenant, final String sha1) {
        final File file = getFile(tenant, sha1);
        if (!file.exists()) {
            throw new ArtifactBinaryNotFoundException(sha1);
        }

        return new ArtifactFilesystem(file, sha1, new DbArtifactHash(sha1, null, null), file.length(), null);
    }

    @Override
    public void deleteByTenant(final String tenant) {
        FileUtils.deleteQuietly(Paths.get(artifactResourceProperties.getPath(), sanitizeTenant(tenant)).toFile());
    }

    @Override
    public boolean existsBySha1(final String tenant, final String sha1) {
        return getFile(tenant, sha1).exists();
    }

    @Override
    protected AbstractDbArtifact store(final String tenant, final DbArtifactHash base16Hashes, final String contentType, final String tempFile)
            throws IOException {
        final File file = new File(tempFile);
        final File fileSHA1Naming = getFile(tenant, base16Hashes.sha1());
        if (fileSHA1Naming.exists()) {
            FileUtils.deleteQuietly(file);
        } else {
            Files.move(file.toPath(), fileSHA1Naming.toPath());
        }

        return new ArtifactFilesystem(fileSHA1Naming, base16Hashes.sha1(), base16Hashes, fileSHA1Naming.length(), contentType);
    }

    private File getFile(final String tenant, final String sha1) {
        // ensure that the sha1 is not a path traversal attack
        if (sha1.indexOf('/') >= 0 || sha1.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Invalid SHA-1 hash: " + sha1);
        }

        final File artifactDirectory = getSha1DirectoryPath(tenant, sha1).toFile();
        if (!artifactDirectory.isDirectory()) {
            if (artifactDirectory.isFile()) {
                throw new ArtifactStoreException(artifactDirectory + " is a file, but a directory is required.");
            } else if (!artifactDirectory.mkdirs()) {
                throw new ArtifactStoreException("Fail to create directories: " + artifactDirectory);
            }
        }
        return new File(artifactDirectory, sha1);
    }

    private Path getSha1DirectoryPath(final String tenant, final String sha1) {
        final int length = sha1.length();
        final String folder1 = sha1.substring(length - 4, length - 2);
        final String folder2 = sha1.substring(length - 2, length);
        return Paths.get(artifactResourceProperties.getPath(), sanitizeTenant(tenant), folder1, folder2);
    }
}
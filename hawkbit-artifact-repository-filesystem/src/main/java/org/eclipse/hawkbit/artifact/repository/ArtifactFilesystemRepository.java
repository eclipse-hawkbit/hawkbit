/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Splitter;
import com.google.common.io.Files;

/**
 * Implementation of the {@link ArtifactRepository} to store artifacts on the
 * file-system. The files are stored by their SHA1 hash of the artifact binary.
 * Duplicate files with the same SHA1 hash will only stored once.
 * 
 * All files are stored flat in one base directory configured in the
 * {@link ArtifactFilesystemProperties#getPath()}.
 * 
 * Due the limit of many file-systems of files within one directory, the files
 * are stored in different sub-directories based on the last four digits of the
 * SHA1-hash {@code (/basepath/[two digit sha1]/[two digit sha1])}.
 */
@Validated
public class ArtifactFilesystemRepository extends AbstractArtifactRepository {

    private final ArtifactFilesystemProperties artifactResourceProperties;

    /**
     * Constructor.
     * 
     * @param artifactResourceProperties
     *            the properties which holds the necessary configuration for the
     *            file-system repository
     */
    public ArtifactFilesystemRepository(final ArtifactFilesystemProperties artifactResourceProperties) {
        this.artifactResourceProperties = artifactResourceProperties;
    }

    @Override
    public void deleteBySha1(final String tenant, final String sha1Hash) {
        FileUtils.deleteQuietly(getFile(tenant, sha1Hash));
    }

    @Override
    public ArtifactFilesystem getArtifactBySha1(final String tenant, final String sha1) {
        final File file = getFile(tenant, sha1);
        if (!file.exists()) {
            return null;
        }

        return new ArtifactFilesystem(file, sha1, new DbArtifactHash(sha1, null, null), file.length(), null);
    }

    @Override
    protected AbstractDbArtifact store(final String tenant, final DbArtifactHash base16Hashes, final String contentType, final String tempFile) throws IOException {

        final File file = new File(tempFile);
        return renameFileToSHA1Naming(tenant, file, new ArtifactFilesystem(file, base16Hashes.getSha1(), base16Hashes, file.length(), contentType));
    }

    private ArtifactFilesystem renameFileToSHA1Naming(final String tenant, final File file,
            final AbstractDbArtifact artifact) throws IOException {
        final File fileSHA1Naming = getFile(tenant, artifact.getHashes().getSha1());
        if (fileSHA1Naming.exists()) {
            FileUtils.deleteQuietly(file);
        } else {
            Files.move(file, fileSHA1Naming);
        }

        return new ArtifactFilesystem(fileSHA1Naming, artifact.getArtifactId(), artifact.getHashes(),
                artifact.getSize(), artifact.getContentType());
    }

    private File getFile(final String tenant, final String sha1) {
        final File aritfactDirectory = getSha1DirectoryPath(tenant, sha1).toFile();
        aritfactDirectory.mkdirs();
        return new File(aritfactDirectory, sha1);
    }

    private Path getSha1DirectoryPath(final String tenant, final String sha1) {
        final int length = sha1.length();
        final List<String> folders = Splitter.fixedLength(2).splitToList(sha1.substring(length - 4, length));
        final String folder1 = folders.get(0);
        final String folder2 = folders.get(1);
        return Paths.get(artifactResourceProperties.getPath(), sanitizeTenant(tenant), folder1, folder2);
    }

    @Override
    public void deleteByTenant(final String tenant) {
        FileUtils.deleteQuietly(Paths.get(artifactResourceProperties.getPath(), sanitizeTenant(tenant)).toFile());
    }

    @Override
    public boolean existsByTenantAndSha1(final String tenant, final String sha1) {
        return getFile(tenant, sha1).exists();
    }
}

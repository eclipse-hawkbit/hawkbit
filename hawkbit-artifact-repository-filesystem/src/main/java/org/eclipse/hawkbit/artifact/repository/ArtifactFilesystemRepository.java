/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
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
public class ArtifactFilesystemRepository implements ArtifactRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactFilesystemRepository.class);

    private static final String TEMP_FILE_PREFIX = "tmp";
    private static final String TEMP_FILE_SUFFIX = "artifactrepo";
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
    public ArtifactFilesystem store(final String tenant, final InputStream content, final String filename,
            final String contentType) {
        return store(tenant, content, filename, contentType, null);
    }

    @Override
    // suppress warning, of not strong enough hashing algorithm, SHA-1 and MD5
    // is not used security related
    @SuppressWarnings("squid:S2070")
    public ArtifactFilesystem store(final String tenant, final InputStream content, final String filename,
            final String contentType, final DbArtifactHash hash) {

        final MessageDigest mdSHA1;
        final MessageDigest mdMD5;
        try {
            mdSHA1 = MessageDigest.getInstance("SHA1");
            mdMD5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }

        final File file = createTempFile();
        final AbstractDbArtifact artifact = store(content, contentType, hash, mdSHA1, mdMD5, file);
        return renameFileToSHA1Naming(tenant, file, artifact);
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

        return new ArtifactFilesystem(file, sha1, new DbArtifactHash(sha1, null), file.length(), null);
    }

    private AbstractDbArtifact store(final InputStream content, final String contentType, final DbArtifactHash hash,
            final MessageDigest mdSHA1, final MessageDigest mdMD5, final File file) {
        AbstractDbArtifact artifact;
        try (final DigestOutputStream outputstream = openFileOutputStream(file, mdSHA1, mdMD5)) {
            final long artifactSize = ByteStreams.copy(content, outputstream);
            outputstream.flush();
            final String sha1Hash = BaseEncoding.base16().lowerCase().encode(mdSHA1.digest());
            final String md5Hash = BaseEncoding.base16().lowerCase().encode(mdMD5.digest());

            artifact = new ArtifactFilesystem(file, sha1Hash, new DbArtifactHash(sha1Hash, md5Hash), artifactSize,
                    contentType);

            checkHashes(artifact, hash);
        } catch (final IOException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        } catch (final HashNotMatchException e) {
            if (!file.delete()) {
                LOG.error("Could not delete temp file {}", file);
            }
            throw e;
        }
        return artifact;
    }

    private ArtifactFilesystem renameFileToSHA1Naming(final String tenant, final File file,
            final AbstractDbArtifact artifact) {
        final File fileSHA1Naming = getFile(tenant, artifact.getHashes().getSha1());
        final ArtifactFilesystem fileSystemArtifact = new ArtifactFilesystem(fileSHA1Naming, artifact.getArtifactId(),
                artifact.getHashes(), artifact.getSize(), artifact.getContentType());
        if (fileSHA1Naming.exists()) {
            FileUtils.deleteQuietly(file);
        } else {
            try {
                Files.move(file, fileSHA1Naming);
            } catch (final IOException e) {
                throw new ArtifactStoreException("Could not store the file " + fileSHA1Naming, e);
            }
        }

        if (!file.delete()) {
            LOG.debug("Could not delete temp file {}", file);
        }

        return fileSystemArtifact;
    }

    private AbstractDbArtifact checkHashes(final AbstractDbArtifact artifact, final DbArtifactHash hash) {
        if (hash == null) {
            return artifact;
        }
        if (hash.getSha1() != null && !artifact.getHashes().getSha1().equals(hash.getSha1())) {
            throw new HashNotMatchException("The given sha1 hash " + hash.getSha1()
                    + " does not match with the calcualted sha1 hash " + artifact.getHashes().getSha1(),
                    HashNotMatchException.SHA1);
        }
        if (hash.getMd5() != null && !artifact.getHashes().getMd5().equals(hash.getMd5())) {
            throw new HashNotMatchException("The given md5 hash " + hash.getMd5()
                    + " does not match with the calcualted md5 hash " + artifact.getHashes().getMd5(),
                    HashNotMatchException.MD5);
        }
        return artifact;
    }

    private File createTempFile() {
        try {
            return File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
        } catch (final IOException e) {
            throw new ArtifactStoreException("Cannot create tempfile", e);
        }
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

    private DigestOutputStream openFileOutputStream(final File file, final MessageDigest mdSHA1,
            final MessageDigest mdMD5) throws FileNotFoundException {
        return new DigestOutputStream(
                new DigestOutputStream(new BufferedOutputStream(new FileOutputStream(file)), mdMD5), mdSHA1);
    }

    @Override
    public void deleteByTenant(final String tenant) {
        FileUtils.deleteQuietly(Paths.get(artifactResourceProperties.getPath(), sanitizeTenant(tenant)).toFile());
    }

    private static String sanitizeTenant(final String tenant) {
        return tenant.trim().toUpperCase();
    }
}

/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;

/**
 * Abstract utility class for ArtifactRepository implementations with common
 * functionality, e.g. computation of hashes.
 */
public abstract class AbstractArtifactRepository implements ArtifactRepository {
    private static final String TEMP_FILE_PREFIX = "tmp";
    private static final String TEMP_FILE_SUFFIX = "artifactrepo";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractArtifactRepository.class);

    @Override
    // suppress warning, of not strong enough hashing algorithm, SHA-1 and MD5
    // is not used security related
    @SuppressWarnings("squid:S2070")
    public AbstractDbArtifact store(final String tenant, final InputStream content, final String filename,
            final String contentType, final DbArtifactHash providedHashes) {
        final MessageDigest mdSHA1;
        final MessageDigest mdMD5;
        final MessageDigest mdSHA256;
        try {
            mdSHA1 = MessageDigest.getInstance("SHA1");
            mdMD5 = MessageDigest.getInstance("MD5");
            mdSHA256 = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }

        String tempFile = null;
        try (final DigestInputStream inputStream = wrapInDigestInputStream(content, mdSHA1, mdMD5, mdSHA256)) {

            tempFile = storeTempFile(inputStream);

            final String sha1Hash16 = BaseEncoding.base16().lowerCase().encode(mdSHA1.digest());
            final String md5Hash16 = BaseEncoding.base16().lowerCase().encode(mdMD5.digest());
            final String sha256Hash16 = BaseEncoding.base16().lowerCase().encode(mdSHA256.digest());

            checkHashes(sha1Hash16, md5Hash16, sha256Hash16, providedHashes);

            // Check if file with same sha1 hash exists and if so return it
            if (existsByTenantAndSha1(tenant, sha1Hash16)) {
                return addMissingHashes(getArtifactBySha1(tenant, sha1Hash16), sha1Hash16, md5Hash16, sha256Hash16);
            }

            return store(sanitizeTenant(tenant), new DbArtifactHash(sha1Hash16, md5Hash16, sha256Hash16), contentType,
                    tempFile);
        } catch (final IOException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        } finally {
            if (!StringUtils.isEmpty(tempFile)) {
                deleteTempFile(tempFile);
            }
        }
    }

    private AbstractDbArtifact addMissingHashes(final AbstractDbArtifact existing, final String calculatedSha1,
            final String calculatedMd5, final String calculatedSha256) {

        final String sha1 = checkEmpty(existing.getHashes().getSha1(), calculatedSha1);
        final String md5 = checkEmpty(existing.getHashes().getMd5(), calculatedMd5);
        final String sha256 = checkEmpty(existing.getHashes().getSha256(), calculatedSha256);

        existing.setHashes(new DbArtifactHash(sha1, md5, sha256));
        return existing;
    }

    private String checkEmpty(final String value, final String fallback) {
        return StringUtils.isEmpty(value) ? fallback : value;
    }

    protected void deleteTempFile(final String tempFile) {
        final File file = new File(tempFile);
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            LOG.error("Could not delete temp file {} ({})", file, e.getMessage());
        }
    }

    protected String storeTempFile(final InputStream content) throws IOException {
        final File file = createTempFile();
        try (final OutputStream outputstream = new BufferedOutputStream(new FileOutputStream(file))) {
            ByteStreams.copy(content, outputstream);
            outputstream.flush();
        }
        return file.getPath();
    }

    private static File createTempFile() {
        try {
            return File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
        } catch (final IOException e) {
            throw new ArtifactStoreException("Cannot create tempfile", e);
        }
    }

    private static void checkHashes(final String sha1Hash16, final String md5Hash16, final String sha256Hash16,
            final DbArtifactHash providedHashes) {
        if (providedHashes == null) {
            return;
        }
        if (areHashesNotMatching(providedHashes.getSha1(), sha1Hash16)) {
            throw new HashNotMatchException("The given sha1 hash " + providedHashes.getSha1()
                    + " does not match the calculated sha1 hash " + sha1Hash16, HashNotMatchException.SHA1);
        }
        if (areHashesNotMatching(providedHashes.getMd5(), md5Hash16)) {
            throw new HashNotMatchException("The given md5 hash " + providedHashes.getMd5()
                    + " does not match the calculated md5 hash " + md5Hash16, HashNotMatchException.MD5);
        }
        if (areHashesNotMatching(providedHashes.getSha256(), sha256Hash16)) {
            throw new HashNotMatchException(
                    "The given sha256 hash " + providedHashes.getSha256()
                            + " does not match the calculated sha256 hash " + sha256Hash16,
                    HashNotMatchException.SHA256);
        }
    }

    private static boolean areHashesNotMatching(final String providedHashValue, final String hashValue) {
        return providedHashValue != null && !hashValue.equals(providedHashValue);
    }

    protected abstract AbstractDbArtifact store(final String tenant, final DbArtifactHash base16Hashes,
            final String contentType, final String tempFile) throws IOException;

    private static DigestInputStream wrapInDigestInputStream(final InputStream input, final MessageDigest mdSHA1,
            final MessageDigest mdMD5, final MessageDigest mdSHA256) {
        return new DigestInputStream(new DigestInputStream(new DigestInputStream(input, mdSHA256), mdMD5), mdSHA1);
    }

    protected static String sanitizeTenant(final String tenant) {
        return tenant.trim().toUpperCase();
    }
}

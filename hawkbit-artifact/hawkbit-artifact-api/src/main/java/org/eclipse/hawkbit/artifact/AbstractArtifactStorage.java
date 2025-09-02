/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact;

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
import java.util.HexFormat;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.exception.ArtifactStoreException;
import org.eclipse.hawkbit.artifact.exception.HashNotMatchException;
import org.eclipse.hawkbit.artifact.model.ArtifactHashes;
import org.eclipse.hawkbit.artifact.model.StoredArtifactInfo;
import org.springframework.util.ObjectUtils;

/**
 * Abstract utility class for ArtifactRepository implementations with common functionality, e.g. computation of hashes.
 */
@Slf4j
public abstract class AbstractArtifactStorage implements ArtifactStorage {

    private static final String TEMP_FILE_PREFIX = "tmp";
    private static final String TEMP_FILE_SUFFIX = "artifactrepo";

    // suppress warning, of not strong enough hashing algorithm, SHA-1 and MD5 is not used security related
    @SuppressWarnings("squid:S2070")
    @Override
    public StoredArtifactInfo store(
            final String tenant, final InputStream content, final String filename, final String contentType,
            final ArtifactHashes providedHashes) {
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

            final HexFormat hexFormat = HexFormat.of().withLowerCase();

            final String sha1Hash = hexFormat.formatHex(mdSHA1.digest());
            final String md5Hash = hexFormat.formatHex(mdMD5.digest());
            final String sha256Hash = hexFormat.formatHex(mdSHA256.digest());

            checkHashes(providedHashes, sha1Hash, md5Hash, sha256Hash);

            // Check if file with same sha1 hash exists and if so return it
            if (existsBySha1(tenant, sha1Hash)) {
                // TODO - shall check if the file is really the same as bytes or just sha1 hash is the same
                return new StoredArtifactInfo(contentType, tempFile.length(), new ArtifactHashes(sha1Hash, md5Hash, sha256Hash));
            }

            return store(sanitizeTenant(tenant), new ArtifactHashes(sha1Hash, md5Hash, sha256Hash), contentType, tempFile);
        } catch (final IOException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        } finally {
            if (!ObjectUtils.isEmpty(tempFile)) {
                deleteTempFile(tempFile);
            }
        }
    }

    protected static String sanitizeTenant(final String tenant) {
        return tenant.trim().toUpperCase();
    }

    protected void deleteTempFile(final String tempFile) {
        final File file = new File(tempFile);
        try {
            Files.deleteIfExists(file.toPath());
        } catch (final IOException e) {
            log.error("Could not delete temp file {} ({})", file, e.getMessage());
        }
    }

    protected String storeTempFile(final InputStream content) throws IOException {
        final File file = createTempFile(false);
        try (final OutputStream outputstream = new BufferedOutputStream(new FileOutputStream(file))) {
            content.transferTo(outputstream);
            outputstream.flush();
        }
        return file.getPath();
    }

    protected abstract StoredArtifactInfo store(
            final String tenant, final ArtifactHashes base16Hashes, final String contentType, final String tempFile) throws IOException;

    // java:S1066 - more readable with separate "if" statements
    // java:S4042 - delete reason is not needed
    @SuppressWarnings({ "java:S1066", "java:S4042" })
    public static File createTempFile(final boolean directory) {
        try {
            final File file = (directory
                    ? Files.createTempDirectory(TEMP_FILE_PREFIX)
                    : Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)).toFile();
            file.deleteOnExit();
            if (!file.setReadable(true, true) || !file.setWritable(true, true)) {
                if (file.delete()) { // try to delete immediately, if failed - on exit
                    throw new IOException("Can't set proper permissions!");
                } else {
                    throw new IOException("Can't set proper permissions (failed to delete the file immediately(!");
                }
            }
            // try, if not supported - ok
            if (!file.setExecutable(false)) {
                log.debug("Can't remove executable permissions for temp file {}", file);
            }
            if (directory) {
                if (!file.setExecutable(true, true)) {
                    log.debug("Can't set executable permissions for temp directory {} for the owner", file);
                }
            }
            return file;
        } catch (final IOException e) {
            throw new ArtifactStoreException("Cannot create temp file", e);
        }
    }

    private static void checkHashes(
            final ArtifactHashes providedHashes, final String sha1Hash16, final String md5Hash16, final String sha256Hash16) {
        if (providedHashes == null) {
            return;
        }

        if (areHashesNotMatching(providedHashes.sha1(), sha1Hash16)) {
            throw new HashNotMatchException("The given sha1 hash " + providedHashes.sha1() +
                    " does not match the calculated sha1 hash " + sha1Hash16, HashNotMatchException.SHA1);
        }
        if (areHashesNotMatching(providedHashes.md5(), md5Hash16)) {
            throw new HashNotMatchException("The given md5 hash " + providedHashes.md5() +
                    " does not match the calculated md5 hash " + md5Hash16, HashNotMatchException.MD5);
        }
        if (areHashesNotMatching(providedHashes.sha256(), sha256Hash16)) {
            throw new HashNotMatchException("The given sha256 hash " + providedHashes.sha256() +
                    " does not match the calculated sha256 hash " + sha256Hash16, HashNotMatchException.SHA256);
        }
    }

    private static boolean areHashesNotMatching(final String providedHashValue, final String hashValue) {
        return providedHashValue != null && !hashValue.equals(providedHashValue);
    }

    private static DigestInputStream wrapInDigestInputStream(final InputStream input,
            final MessageDigest mdSHA1, final MessageDigest mdMD5, final MessageDigest mdSHA256) {
        return new DigestInputStream(new DigestInputStream(new DigestInputStream(input, mdSHA256), mdMD5), mdSHA1);
    }

    private String checkEmpty(final String value, final String fallback) {
        return ObjectUtils.isEmpty(value) ? fallback : value;
    }
}
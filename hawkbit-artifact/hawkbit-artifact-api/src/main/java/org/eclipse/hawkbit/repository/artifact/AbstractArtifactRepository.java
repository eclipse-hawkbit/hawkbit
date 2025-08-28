/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact;

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
import org.eclipse.hawkbit.repository.artifact.exception.ArtifactStoreException;
import org.eclipse.hawkbit.repository.artifact.exception.HashNotMatchException;
import org.eclipse.hawkbit.repository.artifact.model.AbstractDbArtifact;
import org.eclipse.hawkbit.repository.artifact.model.DbArtifactHash;
import org.springframework.util.ObjectUtils;

/**
 * Abstract utility class for ArtifactRepository implementations with common functionality, e.g. computation of hashes.
 */
@Slf4j
public abstract class AbstractArtifactRepository implements ArtifactRepository {

    private static final String TEMP_FILE_PREFIX = "tmp";
    private static final String TEMP_FILE_SUFFIX = "artifactrepo";

    // suppress warning, of not strong enough hashing algorithm, SHA-1 and MD5 is not used security related
    @SuppressWarnings("squid:S2070")
    @Override
    public AbstractDbArtifact store(
            final String tenant, final InputStream content, final String filename, final String contentType,
            final DbArtifactHash providedHashes) {
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

            final String sha1Hash16 = hexFormat.formatHex(mdSHA1.digest());
            final String md5Hash16 = hexFormat.formatHex(mdMD5.digest());
            final String sha256Hash16 = hexFormat.formatHex(mdSHA256.digest());

            checkHashes(providedHashes, sha1Hash16, md5Hash16, sha256Hash16);

            // Check if file with same sha1 hash exists and if so return it
            if (existsBySha1(tenant, sha1Hash16)) {
                // TODO - shall check if the file is really the same as bytes or just sha1 hash is the same
                return addMissingHashes(getBySha1(tenant, sha1Hash16), sha1Hash16, md5Hash16, sha256Hash16);
            }

            return store(sanitizeTenant(tenant), new DbArtifactHash(sha1Hash16, md5Hash16, sha256Hash16), contentType, tempFile);
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

    protected abstract AbstractDbArtifact store(final String tenant, final DbArtifactHash base16Hashes,
            final String contentType, final String tempFile) throws IOException;

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

    private static void checkHashes(final DbArtifactHash providedHashes,
            final String sha1Hash16, final String md5Hash16, final String sha256Hash16) {
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

    private AbstractDbArtifact addMissingHashes(final AbstractDbArtifact existing,
            final String calculatedSha1, final String calculatedMd5, final String calculatedSha256) {
        final String sha1 = checkEmpty(existing.getHashes().sha1(), calculatedSha1);
        final String md5 = checkEmpty(existing.getHashes().md5(), calculatedMd5);
        final String sha256 = checkEmpty(existing.getHashes().sha256(), calculatedSha256);

        existing.setHashes(new DbArtifactHash(sha1, md5, sha256));
        return existing;
    }

    private String checkEmpty(final String value, final String fallback) {
        return ObjectUtils.isEmpty(value) ? fallback : value;
    }
}
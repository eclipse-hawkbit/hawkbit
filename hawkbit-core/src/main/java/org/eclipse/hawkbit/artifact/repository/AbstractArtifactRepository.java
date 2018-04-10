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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;

/**
 * Abstract utility class for ArtifactRepository implementations with common
 * functionality, e.g. computation of hashes.
 *
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
            final String contentType, final DbArtifactHash hash, final long maxBytes) {
        final MessageDigest mdSHA1;
        final MessageDigest mdMD5;
        try {
            mdSHA1 = MessageDigest.getInstance("SHA1");
            mdMD5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }

        LOG.debug("Creating temporary file and store the inputstream to it");

        final File file = createTempFile();

        try (final DigestOutputStream outputstream = openFileOutputStream(file, mdSHA1, mdMD5)) {
            ByteStreams.copy(content, outputstream);
            outputstream.flush();

            checkFile(filename, file, maxBytes);

            final String sha1Hash16 = BaseEncoding.base16().lowerCase().encode(mdSHA1.digest());
            final String md5Hash16 = BaseEncoding.base16().lowerCase().encode(mdMD5.digest());

            LOG.debug("Temporary file {} stored. Calculated sha1: {} and md5: {} hashes", file, sha1Hash16, md5Hash16);

            checkHashes(sha1Hash16, md5Hash16, hash);

            return store(sanitizeTenant(tenant), sha1Hash16, md5Hash16, contentType, file);
        } catch (final IOException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        } finally {
            if (file != null && file.exists() && !file.delete()) {
                LOG.error("Could not delete temp file {}", file);
            }
        }
    }

    /**
     * Checks if the given file can be stored.
     * 
     * @param filename
     *            The name of the file to be stored.
     * @param file
     *            The file to be stored.
     * @param maxBytes
     *            The maximum file size that is allowed.
     */
    private void checkFile(final String filename, final File file, final long maxBytes) {
        if (file == null || file.isDirectory()) {
            return;
        }
        // make sure the artifact is not too big
        if (maxBytes > 0) {
            final long bytes = file.length();
            if (bytes > maxBytes) {
                LOG.warn(
                        "The uploaded artifact '{}' exceeds the maximum artifact size that is allowed. "
                                + "The artifact has a size of {} bytes. The configured maximum size is {} bytes.",
                        filename, bytes, maxBytes);
                throw new ArtifactExceedsMaxSizeException(filename, bytes, maxBytes);
            }
        }
    }

    private static void checkHashes(final String sha1Hash16, final String md5Hash16, final DbArtifactHash hash) {
        if (hash == null) {
            return;
        }
        if (hash.getSha1() != null && !sha1Hash16.equals(hash.getSha1())) {
            throw new HashNotMatchException("The given sha1 hash " + hash.getSha1()
                    + " does not match with the calcualted sha1 hash " + sha1Hash16, HashNotMatchException.SHA1);
        }
        if (hash.getMd5() != null && !md5Hash16.equals(hash.getMd5())) {
            throw new HashNotMatchException(
                    "The given md5 hash " + hash.getMd5() + " does not match with the calcualted md5 hash " + md5Hash16,
                    HashNotMatchException.MD5);
        }
    }

    protected abstract AbstractDbArtifact store(final String tenant, final String sha1Hash16, final String mdMD5Hash16,
            final String contentType, final File file) throws IOException;

    private static File createTempFile() {
        try {
            return File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
        } catch (final IOException e) {
            throw new ArtifactStoreException("Cannot create tempfile", e);
        }
    }

    private static DigestOutputStream openFileOutputStream(final File file, final MessageDigest mdSHA1,
            final MessageDigest mdMD5) throws FileNotFoundException {
        return new DigestOutputStream(
                new DigestOutputStream(new BufferedOutputStream(new FileOutputStream(file)), mdMD5), mdSHA1);
    }

    protected static String sanitizeTenant(final String tenant) {
        return tenant.trim().toUpperCase();
    }

}

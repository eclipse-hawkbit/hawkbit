/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.springframework.util.StringUtils;

import com.google.common.io.BaseEncoding;

/**
 * Abstract utility class for ArtifactRepository implementations with common
 * functionality, e.g. computation of hashes.
 *
 */
public abstract class AbstractArtifactRepository implements ArtifactRepository {

    @Override
    // suppress warning, of not strong enough hashing algorithm, SHA-1 and MD5
    // is not used security related
    @SuppressWarnings("squid:S2070")
    public AbstractDbArtifact store(final String tenant, final InputStream content, final String filename,
            final String contentType, final DbArtifactHash hash) {
        final MessageDigest mdSHA1;
        final MessageDigest mdMD5;
        try {
            mdSHA1 = MessageDigest.getInstance("SHA1");
            mdMD5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        }

        String tempFile = null;
        try (final DigestInputStream inputstream = wrapInDigestInputStream(content, mdSHA1, mdMD5)) {

            tempFile = storeTempFile(inputstream);

            final String sha1Hash16 = BaseEncoding.base16().lowerCase().encode(mdSHA1.digest());
            final String md5Hash16 = BaseEncoding.base16().lowerCase().encode(mdMD5.digest());

            checkHashes(sha1Hash16, md5Hash16, hash);

            return store(sanitizeTenant(tenant), sha1Hash16, md5Hash16, contentType, tempFile);
        } catch (final IOException e) {
            throw new ArtifactStoreException(e.getMessage(), e);
        } finally {
            if (!StringUtils.isEmpty(tempFile)) {
                deleteTempFile(tempFile);
            }
        }
    }

    protected abstract String storeTempFile(final InputStream content) throws IOException;

    protected abstract void deleteTempFile(String tempFile);

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
            final String contentType, final String tempFile) throws IOException;

    private static DigestInputStream wrapInDigestInputStream(final InputStream input, final MessageDigest mdSHA1,
            final MessageDigest mdMD5) {
        return new DigestInputStream(new DigestInputStream(input, mdMD5), mdSHA1);
    }

    protected static String sanitizeTenant(final String tenant) {
        return tenant.trim().toUpperCase();
    }

}

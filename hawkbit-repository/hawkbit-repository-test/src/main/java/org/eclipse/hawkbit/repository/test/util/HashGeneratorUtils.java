/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;

/**
 * Hash digest utility.
 */
public final class HashGeneratorUtils {

    private static final Logger LOG = LoggerFactory.getLogger(HashGeneratorUtils.class);

    private HashGeneratorUtils() {

    }

    /**
     * Generates a MD5 cryptographic string.
     * 
     * @param message
     *            the plain message
     * @return the cryptographic string
     */
    public static String generateMD5(final byte[] message) {
        return hashString(message, "MD5");
    }

    /**
     * Generates a SHA-1 cryptographic string.
     * 
     * @param message
     *            the plain message
     * @return the cryptographic string
     */
    public static String generateSHA1(final byte[] message) {
        return hashString(message, "SHA-1");
    }

    /**
     * Generates a SHA-256 cryptographic string.
     * 
     * @param message
     *            the plain message
     * @return the cryptographic string
     */
    public static String generateSHA256(final byte[] message) {
        return hashString(message, "SHA-256");
    }

    private static String hashString(final byte[] message, final String algorithm) {

        try {
            final MessageDigest digest = MessageDigest.getInstance(algorithm);
            final byte[] hashedBytes = digest.digest(message);
            return BaseEncoding.base16().lowerCase().encode(hashedBytes);
        } catch (final NoSuchAlgorithmException e) {
            LOG.error("Algorithm could not be found", e);
        }
        return null;
    }

}

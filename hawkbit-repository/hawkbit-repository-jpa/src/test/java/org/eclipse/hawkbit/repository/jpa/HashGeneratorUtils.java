/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hash utility calls copied from
 * http://www.codejava.net/coding/how-to-calculate-md5-and-sha-hash-values-in-
 * java.
 *
 *
 *
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
            return convertByteArrayToHexString(hashedBytes);
        } catch (final NoSuchAlgorithmException e) {
            LOG.error("Algorithm could not be find", e);
        }
        return null;
    }

    private static String convertByteArrayToHexString(final byte[] arrayBytes) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arrayBytes.length; i++) {
            builder.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return builder.toString();
    }
}

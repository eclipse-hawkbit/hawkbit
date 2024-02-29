/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hash digest utility.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HashGeneratorUtils {

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
            return HexFormat.of().withLowerCase().formatHex(hashedBytes);
        } catch (final NoSuchAlgorithmException e) {
            log.error("Algorithm could not be found", e);
        }
        return null;
    }
}
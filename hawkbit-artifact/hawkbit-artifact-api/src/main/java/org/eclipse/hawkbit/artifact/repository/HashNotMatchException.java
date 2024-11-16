/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.Serial;

import lombok.Getter;

/**
 * {@link HashNotMatchException} is thrown when provided hashes and hashes calculated during storing are not matching.
 */
@Getter
public class HashNotMatchException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String SHA1 = "SHA-1";
    public static final String MD5 = "MD5";
    public static final String SHA256 = "SHA-256";

    private final String hashFunction;

    /**
     * Constructs a HashNotMatchException with message.
     *
     * @param message the message of the exception
     * @param hashFunction the hash function which caused this exception
     */
    public HashNotMatchException(final String message, final String hashFunction) {
        super(message);
        this.hashFunction = hashFunction;
    }
}
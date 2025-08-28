/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.exception;

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

    public HashNotMatchException(final String message, final String hashFunction) {
        super(message);
        this.hashFunction = hashFunction;
    }
}
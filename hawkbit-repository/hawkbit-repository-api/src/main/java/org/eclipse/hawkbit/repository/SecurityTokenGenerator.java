/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;

/**
 * A singleton bean which holds the {@link SecurityTokenGenerator} and make it
 * accessible to beans which are not managed by spring, e.g. JPA entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public final class SecurityTokenGenerator {

    private static final int TOKEN_LENGTH = 16;
    private static final BytesKeyGenerator SECURE_RANDOM = KeyGenerators.secureRandom(TOKEN_LENGTH);

    /**
     * Generates a random secure token of {@link #TOKEN_LENGTH} bytes length as hexadecimal string.
     *
     * @return a new generated random alphanumeric string.
     */
    public static String generateToken() {
        return new String(Hex.encode(SECURE_RANDOM.generateKey()));
    }
}
/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;

/**
 * A security token generator service which can be used to generate security
 * tokens for e.g. target or gateway tokens which are valid for authenticates
 * against SP.
 */
public class SecurityTokenGenerator {

    private static final int TOKEN_LENGTH = 16;
    private static final BytesKeyGenerator SECURE_RANDOM = KeyGenerators.secureRandom(TOKEN_LENGTH);

    /**
     * Generates a random secure token of {@link #TOKEN_LENGTH} bytes length as
     * hexadecimal string.
     * 
     * @return a new generated random alphanumeric string.
     */
    public String generateToken() {
        return new String(Hex.encode(SECURE_RANDOM.generateKey()));
    }
}

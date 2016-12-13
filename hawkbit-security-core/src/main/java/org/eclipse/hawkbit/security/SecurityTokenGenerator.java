/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.security.SecureRandom;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * A security token generator service which can be used to generate security
 * tokens for e.g. target or gateway tokens which are valid for authenticates
 * against SP.
 */
public class SecurityTokenGenerator {

    private static final boolean LETTERS_GENERATION = true;
    private static final boolean NUMBER_GENERATION = true;
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a random secure token of length {@link #TOKEN_LENGTH}
     * characters with alphanumeric characters {@code A-Z_a-z_0-9}.
     * 
     * @return a new generated random alphanumeric string.
     */
    public String generateToken() {
        return RandomStringUtils.random(TOKEN_LENGTH, 0, 0, LETTERS_GENERATION, NUMBER_GENERATION, null, SECURE_RANDOM);
    }
}

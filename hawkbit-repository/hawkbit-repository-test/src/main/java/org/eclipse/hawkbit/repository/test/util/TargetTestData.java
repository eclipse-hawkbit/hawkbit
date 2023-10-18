/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import java.security.SecureRandom;
import java.util.Random;

import org.eclipse.hawkbit.repository.model.Target;

public class TargetTestData {
    public static final String ATTRIBUTE_KEY_TOO_LONG;
    public static final String ATTRIBUTE_KEY_VALID;
    public static final String ATTRIBUTE_VALUE_TOO_LONG;
    public static final String ATTRIBUTE_VALUE_VALID;

    static {
        final Random rand = new SecureRandom();
        ATTRIBUTE_KEY_TOO_LONG = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_KEY_SIZE + 1, rand);
        ATTRIBUTE_KEY_VALID = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_KEY_SIZE, rand);
        ATTRIBUTE_VALUE_TOO_LONG = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_VALUE_SIZE + 1, rand);
        ATTRIBUTE_VALUE_VALID = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_VALUE_SIZE, rand);
    }

    private static String generateRandomStringWithLength(final int length, final Random rand) {
        final StringBuilder randomStringBuilder = new StringBuilder(length);
        final int lowercaseACode = 97;
        final int lowercaseZCode = 122;

        for (int i = 0; i < length; i++) {
            final char randomCharacter = (char) (rand.nextInt(lowercaseZCode - lowercaseACode + 1) + lowercaseACode);
            randomStringBuilder.append(randomCharacter);
        }
        return randomStringBuilder.toString();
    }

    private TargetTestData() {
        // nothing to do here
    }
}

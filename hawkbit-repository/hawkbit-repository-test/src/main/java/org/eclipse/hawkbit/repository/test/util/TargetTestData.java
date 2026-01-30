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

import static org.eclipse.hawkbit.repository.test.util.TestdataFactory.SECURE_RND;

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.Target;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class TargetTestData {

    public static final String ATTRIBUTE_KEY_TOO_LONG;
    public static final String ATTRIBUTE_KEY_VALID;
    public static final String ATTRIBUTE_VALUE_TOO_LONG;
    public static final String ATTRIBUTE_VALUE_VALID;

    static {
        ATTRIBUTE_KEY_TOO_LONG = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_MAX_KEY_SIZE + 1);
        ATTRIBUTE_KEY_VALID = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_MAX_KEY_SIZE);
        ATTRIBUTE_VALUE_TOO_LONG = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_MAX_VALUE_SIZE + 1);
        ATTRIBUTE_VALUE_VALID = generateRandomStringWithLength(Target.CONTROLLER_ATTRIBUTE_MAX_VALUE_SIZE);
    }

    private static String generateRandomStringWithLength(final int length) {
        final StringBuilder randomStringBuilder = new StringBuilder(length);
        final int lowercaseACode = 97;
        final int lowercaseZCode = 122;

        for (int i = 0; i < length; i++) {
            final char randomCharacter = (char) (SECURE_RND.nextInt(lowercaseZCode - lowercaseACode + 1) + lowercaseACode);
            randomStringBuilder.append(randomCharacter);
        }
        return randomStringBuilder.toString();
    }
}
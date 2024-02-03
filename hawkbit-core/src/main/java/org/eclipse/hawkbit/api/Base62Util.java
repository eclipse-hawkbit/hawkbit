/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.api;

import lombok.NoArgsConstructor;

/**
 * Utility class for Base10 to Base62 conversion and vice versa. Base62 has the
 * benefit of being shorter in ASCII representation than Base10.
 */
@NoArgsConstructor
public final class Base62Util {

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE62_BASE = BASE62_ALPHABET.length();

    /**
     * @param base10
     *            number
     * @return converted number into Base62 ASCII string
     */
    public static String fromBase10(final long base10) {
        if (base10 == 0) {
            return "0";
        }

        long temp = base10;
        final StringBuilder sb = new StringBuilder();

        while (temp > 0) {
            temp = fromBase10(temp, sb);
        }
        return sb.reverse().toString();
    }

    /**
     * @param base62
     *            number
     * @return converted number into Base10
     */
    public static Long toBase10(final String base62) {
        return toBase10(new StringBuilder(base62).reverse().toString().toCharArray());
    }

    private static Long fromBase10(final long base10, final StringBuilder sb) {
        final int rem = (int) (base10 % BASE62_BASE);
        sb.append(BASE62_ALPHABET.charAt(rem));
        return base10 / BASE62_BASE;
    }

    private static Long toBase10(final char[] chars) {
        long base10 = 0L;
        for (int i = chars.length - 1; i >= 0; i--) {
            base10 += toBase10(BASE62_ALPHABET.indexOf(chars[i]), i);
        }
        return base10;
    }

    private static int toBase10(final int n, final int pow) {
        return n * (int) Math.pow(BASE62_BASE, pow);
    }
}
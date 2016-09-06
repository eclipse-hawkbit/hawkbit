/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

public final class Base62Util {
    private static final String BASE62_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE62_BASE = BASE62_ALPHABET.length();

    public static String fromBase10(final Long from) {
        Long temp = from;

        final StringBuilder sb = new StringBuilder("");
        if (temp == 0) {
            return "a";
        }
        while (temp > 0) {
            temp = fromBase10(temp, sb);
        }
        return sb.reverse().toString();
    }

    public static int toBase10(final String base62) {
        return toBase10(new StringBuilder(base62).reverse().toString().toCharArray());
    }

    private static Long fromBase10(final Long from, final StringBuilder sb) {
        final int rem = (int) (from % BASE62_BASE);
        sb.append(BASE62_ALPHABET.charAt(rem));
        return from / BASE62_BASE;
    }

    private static int toBase10(final char[] chars) {
        int n = 0;
        for (int i = chars.length - 1; i >= 0; i--) {
            n += toBase10(BASE62_ALPHABET.indexOf(chars[i]), i);
        }
        return n;
    }

    private static int toBase10(final int n, final int pow) {
        return n * (int) Math.pow(BASE62_BASE, pow);
    }
}

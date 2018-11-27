/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Helper to evaluate regular expressions defined by {@link RegexChar}.
 */
public class RegexHelper {

    public static boolean stringContainsIllegalCharacters(final String stringToCheck,
            final Set<RegexChar> illegalChars) {
        final StringBuilder charBuilder = new StringBuilder();
        illegalChars.forEach(character -> charBuilder.append(character.regExp));
        final String regularExpressions = String.format("[%s]", charBuilder.toString());
        return !Pattern.matches(regularExpressions, stringToCheck);
    }

}

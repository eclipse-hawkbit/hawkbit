/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Objects;

/**
 * Encodes a string attribute of an entity (e.g. name) to make it embeddable
 * into HTML as element identifiers.
 */
public class StringHtmlEncoder {

    /**
     * Base64 encoder which suppresses trailing padding characters.
     */
    private static Encoder BASE64 = Base64.getEncoder().withoutPadding();

    private StringHtmlEncoder() {
        // class should not be instantiated
    }

    /**
     * Encodes the given string attribute so that it can be used as part of DOM
     * element IDs.
     *
     * @param attribute
     *            The attribute of an entity to be encoded. Must not be
     *            <code>null</code>.
     *
     * @return The encoded string attribute to be used as element identifier in
     *         DOM tree.
     */
    public static String encode(final String attribute) {
        Objects.requireNonNull(attribute);
        return BASE64.encodeToString(attribute.getBytes());
    }

}

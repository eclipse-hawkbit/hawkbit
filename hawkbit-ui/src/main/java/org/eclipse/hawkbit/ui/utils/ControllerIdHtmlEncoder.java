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
 * Encodes controller IDs to make them embeddable into HTML as element
 * identifiers.
 */
public class ControllerIdHtmlEncoder {

    /**
     * Base64 encoder which suppresses trailing padding characters.
     */
    private static Encoder BASE64 = Base64.getEncoder().withoutPadding();

    private ControllerIdHtmlEncoder() {
        // class should not be instantiated
    }

    /**
     * Encodes the given controller ID so that it can be used as part of DOM
     * element IDs.
     * 
     * @param controllerId
     *            The controller ID to be encoded. Must not be
     *            <code>null</code>.
     * 
     * @return The encoded controller ID.
     */
    public static String encode(final String controllerId) {
        Objects.requireNonNull(controllerId);
        return BASE64.encodeToString(controllerId.getBytes());
    }

}

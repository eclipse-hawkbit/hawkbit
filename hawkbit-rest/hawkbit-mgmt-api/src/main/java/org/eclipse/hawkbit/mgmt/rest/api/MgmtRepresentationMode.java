/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration of the supported representation modes.
 */
public enum MgmtRepresentationMode {

    FULL("full"),

    COMPACT("compact");

    private final String mode;

    private MgmtRepresentationMode(final String mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return mode;
    }

    public static Optional<MgmtRepresentationMode> fromValue(final String value) {
        return Arrays.stream(MgmtRepresentationMode.values()).filter(v -> v.mode.equalsIgnoreCase(value)).findFirst();
    }

}

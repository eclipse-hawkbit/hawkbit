/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

    MgmtRepresentationMode(final String mode) {
        this.mode = mode;
    }

    public static Optional<MgmtRepresentationMode> fromValue(final String value) {
        return Arrays.stream(MgmtRepresentationMode.values()).filter(v -> v.mode.equalsIgnoreCase(value)).findFirst();
    }

    @Override
    public String toString() {
        return mode;
    }
}
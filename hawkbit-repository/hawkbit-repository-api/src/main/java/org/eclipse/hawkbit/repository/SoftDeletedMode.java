/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Arrays;
import java.util.Optional;

public enum SoftDeletedMode {
    ONLY_SOFT_DELETED("only_soft_deleted"),
    EXCLUDE_SOFT_DELETED("exclude_soft_deleted"),
    INCLUDE_SOFT_DELETED("include_soft_deleted");

    private final String mode;

    SoftDeletedMode(String mode) {
        this.mode = mode;
    }

    public static Optional<SoftDeletedMode> fromValue(final String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(SoftDeletedMode.values()).filter(v -> v.mode.equalsIgnoreCase(value)).findFirst();
    }

    @Override
    public String toString() {
        return mode;
    }
}

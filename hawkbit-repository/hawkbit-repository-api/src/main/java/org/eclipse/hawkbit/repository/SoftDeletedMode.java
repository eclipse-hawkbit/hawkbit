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

public enum SoftDeletedMode {
    ONLY_SOFT_DELETED("only_soft_deleted"),
    EXCLUDE_SOFT_DELETED("exclude_soft_deleted"),
    INCLUDE_SOFT_DELETED("include_soft_deleted");

    private final String mode;

    SoftDeletedMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return mode;
    }
}

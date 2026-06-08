/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.Optional;

public enum MgmtSoftDeletedMode {

    ONLY_SOFT_DELETED,
    EXCLUDE_SOFT_DELETED,
    INCLUDE_SOFT_DELETED;

    public static Optional<MgmtSoftDeletedMode> fromValue(final String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(MgmtSoftDeletedMode.valueOf(value.toUpperCase()));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

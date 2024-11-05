/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Definition of the action cancel type for the distribution set invalidation
 * via REST management API.
 */
public enum MgmtCancelationType {

    /**
     * Actions will be soft canceled.
     */
    SOFT("soft"),
    /**
     * Actions will be force quit.
     */
    FORCE("force"),
    /**
     * No actions will be canceled.
     */
    NONE("none");

    private final String name;

    MgmtCancelationType(final String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
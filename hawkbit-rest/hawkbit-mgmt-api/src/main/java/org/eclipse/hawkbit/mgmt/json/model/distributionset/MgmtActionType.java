/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
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
 * Definition of the Action type for the REST management API.
 */
public enum MgmtActionType {

    /**
     * The soft action type.
     */
    SOFT("soft"),
    /**
     * The forced action type.
     */
    FORCED("forced"),
    /**
     * The time forced action type.
     */
    TIMEFORCED("timeforced"),
    /**
     * The Download-Only action type.
     */
    DOWNLOAD_ONLY("downloadonly");

    private final String name;

    MgmtActionType(final String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
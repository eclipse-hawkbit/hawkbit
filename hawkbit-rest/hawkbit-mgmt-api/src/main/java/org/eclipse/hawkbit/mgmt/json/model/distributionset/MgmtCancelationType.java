/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Definition of the action cancel type for the distribution set invalidation
 * via REST management API.
 *
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

    private MgmtCancelationType(final String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}

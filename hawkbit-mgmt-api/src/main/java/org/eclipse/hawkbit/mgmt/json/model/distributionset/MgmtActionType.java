/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Definition of the Action type for the REST management API.
 *
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
    TIMEFORCED("timeforced");

    private final String name;

    private MgmtActionType(final String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

}

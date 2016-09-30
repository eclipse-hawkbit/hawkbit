/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.targetfilter;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for target PUT/POST commands.
 *
 */
public class MgmtTargetFilterQueryRequestBody {
    @JsonProperty(required = true)
    private String name;

    @JsonProperty(required = true)
    private String query;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the filter query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query
     *            the filter query
     */
    public void setQuery(String query) {
        this.query = query;
    }

}

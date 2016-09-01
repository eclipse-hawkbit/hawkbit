/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
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
    public MgmtTargetFilterQueryRequestBody setName(final String name) {
        this.name = name;
        return this;
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

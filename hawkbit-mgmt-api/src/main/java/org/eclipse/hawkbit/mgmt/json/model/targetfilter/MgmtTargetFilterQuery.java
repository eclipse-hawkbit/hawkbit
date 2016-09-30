/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.targetfilter;

import org.eclipse.hawkbit.mgmt.json.model.MgmtBaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Target Filter Queries to RESTful API
 * representation.
 *
 */
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetFilterQuery extends MgmtBaseEntity {

    @JsonProperty(value = "id", required = true)
    private Long filterId;

    @JsonProperty
    private String name;

    @JsonProperty
    private String query;

    @JsonProperty
    private Long autoAssignDistributionSet;

    public Long getFilterId() {
        return filterId;
    }

    public void setFilterId(final Long filterId) {
        this.filterId = filterId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public Long getAutoAssignDistributionSet() {
        return autoAssignDistributionSet;
    }

    public void setAutoAssignDistributionSet(final Long autoAssignDistributionSet) {
        this.autoAssignDistributionSet = autoAssignDistributionSet;
    }

}

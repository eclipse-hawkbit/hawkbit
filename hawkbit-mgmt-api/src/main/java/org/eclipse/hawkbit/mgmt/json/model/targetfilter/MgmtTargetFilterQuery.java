/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.targetfilter;

import org.eclipse.hawkbit.mgmt.json.model.MgmtBaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Target to RESTful API representation.
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

    /**
     * @return the filterId
     */
    public Long getFilterId() {
        return filterId;
    }

    /**
     * @param filterId
     *            the filterId to set
     */
    public void setFilterId(final Long filterId) {
        this.filterId = filterId;
    }

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
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query
     *            the query to set
     */
    @JsonIgnore
    public void setQuery(final String query) {
        this.query = query;
    }

    public Long getAutoAssignDistributionSet() {
        return autoAssignDistributionSet;
    }

    @JsonIgnore
    public void setAutoAssignDistributionSet(final Long autoAssignDistributionSet) {
        this.autoAssignDistributionSet = autoAssignDistributionSet;
    }

}

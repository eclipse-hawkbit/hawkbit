/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for target PUT/POST commands.
 *
 */
public class MgmtTargetRequestBody {
    @JsonProperty(required = true)
    private String name;

    private String description;

    @JsonProperty(required = true)
    private String controllerId;

    @JsonProperty
    private String address;

    @JsonProperty
    private String securityToken;

    public String getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(final String securityToken) {
        this.securityToken = securityToken;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the controllerId
     */
    public String getControllerId() {
        return controllerId;
    }

    /**
     * @param name
     *            the name to set
     */
    public MgmtTargetRequestBody setName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param description
     *            the description to set
     */
    public MgmtTargetRequestBody setDescription(final String description) {
        this.description = description;
        return this;
    }

    /**
     * @param controllerId
     *            the controllerId to set
     */
    public MgmtTargetRequestBody setControllerId(final String controllerId) {
        this.controllerId = controllerId;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

}

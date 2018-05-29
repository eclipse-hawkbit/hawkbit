/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import java.net.URI;

import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.MgmtPollStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Target to RESTful API representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTarget extends MgmtNamedEntity {

    @JsonProperty(required = true)
    private String controllerId;

    @JsonProperty
    private String updateStatus;

    @JsonProperty
    private Long lastControllerRequestAt;

    @JsonProperty
    private Long installedAt;

    @JsonProperty
    private String ipAddress;

    @JsonProperty
    private String address;

    @JsonProperty
    private MgmtPollStatus pollStatus;

    @JsonProperty
    private String securityToken;

    /**
     * @return the controllerId
     */
    public String getControllerId() {
        return controllerId;
    }

    /**
     * @param controllerId
     *            the controllerId to set
     */
    public void setControllerId(final String controllerId) {
        this.controllerId = controllerId;
    }

    /**
     * @return the updateStatus
     */
    public String getUpdateStatus() {
        return updateStatus;
    }

    /**
     * @param updateStatus
     *            the updateStatus to set
     */
    public void setUpdateStatus(final String updateStatus) {
        this.updateStatus = updateStatus;
    }

    /**
     * @return the lastControllerRequestAt
     */
    public Long getLastControllerRequestAt() {
        return lastControllerRequestAt;
    }

    /**
     * @param lastControllerRequestAt
     *            the lastControllerRequestAt to set
     */
    @JsonIgnore
    public void setLastControllerRequestAt(final Long lastControllerRequestAt) {
        this.lastControllerRequestAt = lastControllerRequestAt;
    }

    /**
     * @return the installedAt
     */
    public Long getInstalledAt() {
        return installedAt;
    }

    /**
     * @param installedAt
     *            the installedAt to set
     */
    @JsonIgnore
    public void setInstalledAt(final Long installedAt) {
        this.installedAt = installedAt;
    }

    /**
     * @return the pollStatus
     */
    public MgmtPollStatus getPollStatus() {
        return pollStatus;
    }

    /**
     * @param pollStatus
     *            the pollStatus to set
     */
    @JsonIgnore
    public void setPollStatus(final MgmtPollStatus pollStatus) {
        this.pollStatus = pollStatus;
    }

    /**
     * @return the ipAddress
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @param ipAddress
     *            the ipAddress to set
     */
    @JsonIgnore
    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * @return the securityToken
     */
    public String getSecurityToken() {
        return securityToken;
    }

    public String getAddress() {
        return address;
    }

    @JsonIgnore
    public void setAddress(final String address) {
        if (address != null) {
            URI.create(address);
        }
        this.address = address;
    }

    /**
     * @param securityToken
     *            the securityToken to set
     */
    @JsonIgnore
    public void setSecurityToken(final String securityToken) {
        this.securityToken = securityToken;
    }
}

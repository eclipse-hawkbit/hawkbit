/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.rest.resource.model.target;

import java.net.URI;

import org.eclipse.hawkbit.rest.resource.model.NamedEntityRest;
import org.eclipse.hawkbit.rest.resource.model.PollStatusRest;
import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A json annotated rest model for Target to RESTful API representation.
 * 
 *
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("Provisioning Target")
public class TargetRest extends NamedEntityRest {

    @ApiModelProperty(value = ApiModelProperties.ITEM_ID, required = true)
    @JsonProperty(required = true)
    private String controllerId;

    @ApiModelProperty(value = ApiModelProperties.UPDATE_STATUS, allowableValues = "error, in_sync, pending, registered, unknown")
    @JsonProperty
    private String updateStatus;

    @ApiModelProperty(value = ApiModelProperties.LAST_REQUEST_AT)
    @JsonProperty
    private Long lastControllerRequestAt;

    @ApiModelProperty(value = ApiModelProperties.INSTALLED_AT)
    @JsonProperty
    private Long installedAt;

    @ApiModelProperty(value = ApiModelProperties.IP_ADDRESS)
    @JsonProperty
    private String ipAddress;

    @ApiModelProperty(value = ApiModelProperties.ADDRESS)
    @JsonProperty
    private String address;

    @ApiModelProperty(value = ApiModelProperties.POLL_STATUS)
    @JsonProperty
    private PollStatusRest pollStatus;

    @ApiModelProperty(value = ApiModelProperties.SECURITY_TOKEN)
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
    public PollStatusRest getPollStatus() {
        return pollStatus;
    }

    /**
     * @param pollStatus
     *            the pollStatus to set
     */
    @JsonIgnore
    public void setPollStatus(final PollStatusRest pollStatus) {
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

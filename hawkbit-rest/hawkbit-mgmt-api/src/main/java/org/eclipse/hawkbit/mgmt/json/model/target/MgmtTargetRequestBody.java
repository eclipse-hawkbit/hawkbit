/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for target PUT/POST commands.
 *
 */
public class MgmtTargetRequestBody {
    @JsonProperty(required = true)
    @Schema(example = "controllerName")
    private String name;

    @Schema(example = "Example description of a target")
    private String description;

    @JsonProperty(required = true)
    @Schema(example = "12345")
    private String controllerId;

    @JsonProperty
    @Schema(example = "https://192.168.0.1")
    private String address;

    @JsonProperty
    @Schema(example = "2345678DGGDGFTDzztgf")
    private String securityToken;

    @JsonProperty
    @Schema(example = "false")
    private Boolean requestAttributes;

    @JsonProperty
    @Schema(example = "10")
    private Long targetType;

    /**
     * @return Target type ID
     */
    public Long getTargetType() {
        return targetType;
    }

    /**
     * @param targetType
     *          Target type ID
     */
    public void setTargetType(Long targetType) {
        this.targetType = targetType;
    }

    /**
     * @return token
     */
    public String getSecurityToken() {
        return securityToken;
    }

    /**
     * @param securityToken Token
     */
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

    /**
     * @return address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address
     *          Address
     */
    public void setAddress(final String address) {
        this.address = address;
    }

    /**
     * @return boolean true or false
     */
    public Boolean isRequestAttributes() {
        return requestAttributes;
    }

    /**
     * @param requestAttributes
     *          Attributes
     */
    public void setRequestAttributes(final Boolean requestAttributes) {
        this.requestAttributes = requestAttributes;
    }
}

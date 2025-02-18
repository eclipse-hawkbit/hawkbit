/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security.controller;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * JSON representation to authenticate a tenant.
 */
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ControllerSecurityToken {

    public static final String AUTHORIZATION_HEADER = "Authorization";

    @JsonProperty
    private final Long tenantId;
    @JsonProperty
    private final Long targetId;
    @JsonProperty
    private final String controllerId;
    @JsonProperty
    private String tenant;
    @JsonProperty
    private Map<String, String> headers;

    /**
     * Constructor.
     *
     * @param tenant the tenant for the security token
     * @param tenantId alternative tenant identification by technical ID
     * @param controllerId the ID of the controller for the security token
     * @param targetId alternative target identification by technical ID
     */
    @JsonCreator
    public ControllerSecurityToken(
            @JsonProperty("tenant") final String tenant,
            @JsonProperty("tenantId") final Long tenantId, @JsonProperty("controllerId") final String controllerId,
            @JsonProperty("targetId") final Long targetId) {
        this.tenant = tenant;
        this.tenantId = tenantId;
        this.controllerId = controllerId;
        this.targetId = targetId;
    }

    /**
     * Constructor.
     *
     * @param tenant the tenant for the security token
     * @param controllerId the ID of the controller for the security token
     */
    public ControllerSecurityToken(final String tenant, final String controllerId) {
        this(tenant, null, controllerId, null);
    }

    /**
     * Gets a header value.
     *
     * @param name of header
     * @return the value
     */
    public String getHeader(final String name) {
        if (headers == null) {
            return null;
        }

        return headers.get(name);
    }

    /**
     * Associates the specified header value with the specified name.
     *
     * @param name of the header
     * @param value of the header
     */
    public void putHeader(final String name, final String value) {
        if (headers == null) {
            headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }

        headers.put(name, value);
    }
}
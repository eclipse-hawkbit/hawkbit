/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

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
public class DmfTenantSecurityToken {

    public static final String AUTHORIZATION_HEADER = "Authorization";

    @JsonProperty
    private String tenant;
    @JsonProperty
    private final Long tenantId;
    @JsonProperty
    private final String controllerId;
    @JsonProperty
    private final Long targetId;

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
    public DmfTenantSecurityToken(@JsonProperty("tenant") final String tenant,
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
    public DmfTenantSecurityToken(final String tenant, final String controllerId) {
        this(tenant, null, controllerId, null);
    }

    /**
     * Constructor.
     * 
     * @param tenantId the tenant for the security token
     * @param targetId target identification by technical ID
     */
    public DmfTenantSecurityToken(final Long tenantId, final Long targetId) {
        this(null, tenantId, null, targetId);
    }

    /**
     * Gets a header value.
     * 
     * @param name
     *            of header
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
     * 
     * @return the previous value associated with the <tt>name</tt>, or <tt>null</tt> if there was no mapping for <tt>name</tt>.
     */
    public String putHeader(final String name, final String value) {
        if (headers == null) {
            headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }
        return headers.put(name, value);
    }
}
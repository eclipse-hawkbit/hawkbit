/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.json.model;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON representation to authenticate a tenant.
 * 
 *
 *
 */

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantSecruityToken {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String COAP_AUTHORIZATION_HEADER = "Coap-Authorization";
    public static final String COAP_TOKEN_VALUE = "CoapToken";

    @JsonProperty
    private final String tenant;
    @JsonProperty
    private final String controllerId;
    @JsonProperty(required = false)
    private Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    @JsonProperty(required = false)
    private final String sha1;

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant for the security token
     * @param controllerId
     *            the ID of the controller for the security token
     * @param sha1
     *            the sha1 of authentication
     */
    @JsonCreator
    public TenantSecruityToken(@JsonProperty("tenant") final String tenant,
            @JsonProperty("controllerId") final String controllerId, @JsonProperty("sha1") final String sha1) {
        this.tenant = tenant;
        this.controllerId = controllerId;
        this.sha1 = sha1;
    }

    public String getTenant() {
        return tenant;
    }

    public String getControllerId() {
        return controllerId;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getSha1() {
        return sha1;
    }

    /**
     * Gets a header value.
     * 
     * @param name
     *            of header
     * @return the value
     */
    public String getHeader(final String name) {
        return headers.get(name);
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.headers.putAll(headers);
    }

}

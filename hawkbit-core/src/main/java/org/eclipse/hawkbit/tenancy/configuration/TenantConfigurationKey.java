/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy.configuration;

/**
 * An enum which defines the tenant specific configurations which can be
 * configured for each tenant seperately.
 *
 *
 *
 *
 */
public enum TenantConfigurationKey {

    /**
     * boolean value {@code true} {@code false}.
     */
    AUTHENTICATION_MODE_HEADER_ENABLED("authentication.header.enabled",
            "hawkbit.server.ddi.security.authentication.header.enabled", Boolean.FALSE.toString()),
    /**
    *
    */
    AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME("authentication.header.authority",
            "hawkbit.server.ddi.security.authentication.header.authority", Boolean.FALSE.toString()),
    /**
     * boolean value {@code true} {@code false}.
     */
    AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED("authentication.targettoken.enabled",
            "hawkbit.server.ddi.security.authentication.targettoken.enabled", Boolean.FALSE.toString()),

    /**
     * boolean value {@code true} {@code false}.
     */
    AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED("authentication.gatewaytoken.enabled",
            "hawkbit.server.ddi.security.authentication.gatewaytoken.enabled", Boolean.FALSE.toString()),
    /**
     * string value which holds the name of the security token key.
     */
    AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME("authentication.gatewaytoken.name",
            "hawkbit.server.ddi.security.authentication.gatewaytoken.name", null),

    /**
     * string value which holds the actual security-key of the gateway security
     * token.
     */
    AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY("authentication.gatewaytoken.key",
            "hawkbit.server.ddi.security.authentication.gatewaytoken.key", null);

    private final String keyName;
    private final String defaultKeyName;
    private final String defaultValue;

    /**
     * @param key
     *            the property key name
     * @param allowedValues
     *            the allowed values for this specific key
     */
    private TenantConfigurationKey(final String key, final String defaultKeyName, final String defaultValue) {
        this.keyName = key;
        this.defaultKeyName = defaultKeyName;
        this.defaultValue = defaultValue;
    }

    /**
     * @return the keyName
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * @return the defaultKeyName
     */
    public String getDefaultKeyName() {
        return defaultKeyName;
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }
}

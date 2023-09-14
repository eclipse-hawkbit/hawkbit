/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

/**
 * Proxy for key-value details.
 */
public class ProxyKeyValueDetails {

    private final String id;
    private final String key;
    private final String value;

    /**
     * Constructor for ProxyKeyValueDetails
     *
     * @param id
     *         Id of entity
     * @param key
     *          Key of entity
     * @param value
     *          value for related key
     */
    public ProxyKeyValueDetails(final String id, final String key, final String value) {
        this.id = id;
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the id
     *
     * @return Id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the key
     *
     * @return key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the value
     *
     * @return value
     */
    public String getValue() {
        return value;
    }
}

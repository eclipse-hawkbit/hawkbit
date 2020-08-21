/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

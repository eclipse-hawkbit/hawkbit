/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.systemmanagement;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Model representation of an Cache entry as json.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSystemCache {

    private final String name;
    private final Collection<String> keys;

    /**
     * @param name
     *            the name of the cache
     * @param cacheKeys
     *            the keys which contains in the cache
     */
    public MgmtSystemCache(final String name, final Collection<String> cacheKeys) {
        this.name = name;
        this.keys = cacheKeys;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the keys
     */
    public Collection<String> getKeys() {
        return keys;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MgmtSystemCache [name=" + name + ", keys=" + keys + "]";
    }
}

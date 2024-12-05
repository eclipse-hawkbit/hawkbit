/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.systemmanagement;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.ToString;

/**
 * Model representation of an Cache entry as json.
 */
@Data
@ToString
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSystemCache {

    private final String name;
    private final Collection<String> keys;

    /**
     * @param name the name of the cache
     * @param cacheKeys the keys which contains in the cache
     */
    public MgmtSystemCache(final String name, final Collection<String> cacheKeys) {
        this.name = name;
        this.keys = cacheKeys;
    }
}
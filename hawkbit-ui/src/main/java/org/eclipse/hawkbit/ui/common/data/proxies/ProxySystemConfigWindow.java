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

import java.io.Serializable;

/**
 * Proxy for system config window.
 */
public class ProxySystemConfigWindow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;


    /**
     * Gets the id
     *
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id
     *
     * @param id
     *            System config window id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets the name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     *
     * @param name
     *            System config window name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the description
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description
     *
     * @param description
     *            System config window description
     */
    public void setDescription(final String description) {
        this.description = description;
    }

}

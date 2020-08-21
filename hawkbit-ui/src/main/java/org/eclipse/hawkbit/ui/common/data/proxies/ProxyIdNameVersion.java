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
 * Represent a data transfer object for the ui with id, name and version.
 */
public class ProxyIdNameVersion extends ProxyIdentifiableEntity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String version;

    /**
     * Constructor.
     * 
     * @param id
     *            the id
     * @param name
     *            the name
     * @param version
     *            the version
     *
     */
    public ProxyIdNameVersion(final Long id, final String name, final String version) {
        super(id);

        this.name = name;
        this.version = version;
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
     *          Name of entity
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the version
     *
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version
     *
     * @param version
     *          Version of entity
     */
    public void setVersion(final String version) {
        this.version = version;
    }
}

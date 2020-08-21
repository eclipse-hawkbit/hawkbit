/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.util.Objects;

/**
 * Holds information about a type
 */
public class ProxyTypeInfo extends ProxyIdentifiableEntity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String key;

    /**
     * Constructor
     */
    public ProxyTypeInfo() {
        super();
    }

    /**
     * Constructor
     * 
     * @param id
     *            type name
     * @param name
     *            type ID
     * @param key
     *            type key
     */
    public ProxyTypeInfo(final Long id, final String name, final String key) {
        super(id);
        this.name = name;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getKey());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProxyTypeInfo other = (ProxyTypeInfo) obj;
        return Objects.equals(this.getId(), other.getId()) && Objects.equals(this.getName(), other.getName())
                && Objects.equals(this.getKey(), other.getKey());
    }
}

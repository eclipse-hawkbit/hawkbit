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

import java.util.Objects;

/**
 * Holds information about a target filter query
 */
public class ProxyTargetFilterQueryInfo extends ProxyIdentifiableEntity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String query;

    /**
     * Constructor
     */
    public ProxyTargetFilterQueryInfo() {
        super();
    }

    /**
     * Constructor
     * 
     * @param id
     *            target filter ID
     * @param name
     *            target filter name
     * @param query
     *            target filter query
     */
    public ProxyTargetFilterQueryInfo(final Long id, final String name, final String query) {
        super(id);
        this.name = name;
        this.query = query;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getQuery());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProxyTargetFilterQueryInfo other = (ProxyTargetFilterQueryInfo) obj;
        return Objects.equals(this.getId(), other.getId()) && Objects.equals(this.getName(), other.getName())
                && Objects.equals(this.getQuery(), other.getQuery());
    }
}

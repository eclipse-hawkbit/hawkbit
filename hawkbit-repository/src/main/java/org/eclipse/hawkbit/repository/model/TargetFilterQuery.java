/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 *
 *
 */
@Entity
@Table(name = "sp_target_filter_query", indexes = {
        @Index(name = "sp_idx_target_filter_query_01", columnList = "tenant,name") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "name", "tenant" }, name = "uk_tenant_custom_filter_name") )
public class TargetFilterQuery extends BaseEntity {
    /**
     * 
     * 
     */
    private static final long serialVersionUID = 7493966984413479089L;

    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "query", length = 1024)
    private String query;

    public TargetFilterQuery() {
        name = null;
        query = null;
    }

    public TargetFilterQuery(final String name, final String query) {
        this.name = name;
        this.query = query;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query
     *            the query to set
     */
    public void setQuery(final String query) {
        this.query = query;
    }

    @Override
    public boolean equals(final Object obj) {// NOSONAR - as this is generated
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TargetFilterQuery other = (TargetFilterQuery) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() { // NOSONAR - as this is generated
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }
}

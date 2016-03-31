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
 * Stored target filter.
 *
 */
@Entity
@Table(name = "sp_target_filter_query", indexes = {
        @Index(name = "sp_idx_target_filter_query_01", columnList = "tenant,name") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "name", "tenant" }, name = "uk_tenant_custom_filter_name"))
public class TargetFilterQuery extends TenantAwareBaseEntity {
    private static final long serialVersionUID = 7493966984413479089L;

    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "query", length = 1024)
    private String query;

    public TargetFilterQuery() {
        // Default constructor for JPA.
    }

    public TargetFilterQuery(final String name, final String query) {
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
}

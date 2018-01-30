/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.TenantMetaData;

/**
 * Tenant entity with meta data that is configured globally for the entire
 * tenant. This entity is not tenant aware to allow the system to access it
 * through the {@link EntityManager} even before the actual tenant exists.
 *
 * Entities owned by the tenant are based on {@link TenantAwareBaseEntity}.
 *
 */
@Table(name = "sp_tenant", indexes = {
        @Index(name = "sp_idx_tenant_prim", columnList = "tenant,id") }, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "tenant" }, name = "uk_tenantmd_tenant") })
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaTenantMetaData extends AbstractJpaBaseEntity implements TenantMetaData {
    private static final long serialVersionUID = 1L;

    @Column(name = "tenant", nullable = false, updatable = false, length = 40)
    @Size(min = 1, max = 40)
    @NotNull
    private String tenant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_ds_type", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_tenant_md_default_ds_type"))
    private JpaDistributionSetType defaultDsType;

    /**
     * Default constructor needed for JPA entities.
     */
    public JpaTenantMetaData() {
        // Default constructor needed for JPA entities.
    }

    /**
     * Standard constructor.
     *
     * @param defaultDsType
     *            of this tenant
     * @param tenant
     */
    public JpaTenantMetaData(final DistributionSetType defaultDsType, final String tenant) {
        super();
        this.defaultDsType = (JpaDistributionSetType) defaultDsType;
        this.tenant = tenant;
    }

    @Override
    public DistributionSetType getDefaultDsType() {
        return defaultDsType;
    }

    public void setDefaultDsType(final DistributionSetType defaultDsType) {
        this.defaultDsType = (JpaDistributionSetType) defaultDsType;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }
}

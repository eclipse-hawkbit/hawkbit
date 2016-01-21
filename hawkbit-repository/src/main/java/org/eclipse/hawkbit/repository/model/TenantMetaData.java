/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Tenant metadata that is configured globally for the entire tenant. This
 * entity is
 *
 *
 *
 *
 */
@Access(AccessType.FIELD)
@Table(name = "sp_tenant", indexes = {
        @Index(name = "sp_idx_tenant_prim", columnList = "tenant,id") }, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "tenant" }, name = "uk_tenantmd_tenant") })
@Entity
public class TenantMetaData implements Serializable {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant", nullable = false, length = 40)
    private String tenant;

    private String createdBy;
    private String lastModifiedBy;
    private Long createdAt;
    private Long lastModifiedAt;

    @Version
    @Column(name = "optlock_revision")
    private long optLockRevision;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_ds_type", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_tenant_md_default_ds_type") )
    // use deprecated annotation until HHH-8862 is fixed
    @SuppressWarnings("deprecation")
    // @org.hibernate.annotations.ForeignKey( name =
    // "fk_tenant_md_default_ds_type" )
    private DistributionSetType defaultDsType;

    public TenantMetaData() {
    }

    /**
     * Standard constructor.
     *
     * @param defaultDsType
     *            of this tenant
     * @param tenant
     */
    public TenantMetaData(final DistributionSetType defaultDsType, final String tenant) {
        super();
        this.defaultDsType = defaultDsType;
        this.tenant = tenant;
    }

    /**
     * @return the defaultDsType
     */
    public DistributionSetType getDefaultDsType() {
        return defaultDsType;
    }

    /**
     * Set the DistributionSet for a tenant.
     *
     * @param defaultDsType
     *            the new default DistributionSet
     */
    public void setDefaultDsType(final DistributionSetType defaultDsType) {
        this.defaultDsType = defaultDsType;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(final Long id) {
        this.id = id;
    }

    @Access(AccessType.PROPERTY)
    @Column(name = "created_at", insertable = true, updatable = false)
    public Long getCreatedAt() {
        return createdAt;
    }

    @Access(AccessType.PROPERTY)
    @Column(name = "created_by", insertable = true, updatable = false, length = 40)
    public String getCreatedBy() {
        return createdBy;
    }

    @Access(AccessType.PROPERTY)
    @Column(name = "last_modified_at", insertable = false, updatable = true)
    public Long getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Access(AccessType.PROPERTY)
    @Column(name = "last_modified_by", insertable = false, updatable = true, length = 40)
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param createdBy
     *            the createdBy to set
     */
    @CreatedBy
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @param lastModifiedBy
     *            the lastModifiedBy to set
     */
    @LastModifiedBy
    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * @param createdAt
     *            the createdAt to set
     */
    @CreatedDate
    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * @param lastModifiedAt
     *            the lastModifiedAt to set
     */
    @LastModifiedDate
    public void setLastModifiedAt(final Long lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    /**
     * @return the optLockRevision
     */
    public long getOptLockRevision() {
        return optLockRevision;
    }

    /**
     * @param optLockRevision
     *            the optLockRevision to set
     */
    public void setOptLockRevision(final long optLockRevision) {
        this.optLockRevision = optLockRevision;
    }

    /**
     * @return the tenant
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * @param tenant
     *            the tenant to set
     */
    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) { // NOSONAR - as this is generated
                                              // code
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TenantMetaData other = (TenantMetaData) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

}

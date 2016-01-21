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
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.Version;

import org.eclipse.hawkbit.eventbus.CacheFieldEntityListener;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.eclipse.hawkbit.repository.model.helper.SystemManagementHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantAwareHolder;
import org.eclipse.persistence.annotations.Multitenant;
import org.eclipse.persistence.annotations.MultitenantType;
import org.eclipse.persistence.annotations.TenantDiscriminatorColumn;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.hateoas.Identifiable;

/**
 * Holder of all base attributes common to all entities.
 *
 *
 *
 *
 *
 */
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners({ AuditingEntityListener.class, CacheFieldEntityListener.class })
@TenantDiscriminatorColumn(name = "tenant", length = 40)
@Multitenant(MultitenantType.SINGLE_TABLE)
public abstract class BaseEntity implements Serializable, Identifiable<Long> {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String createdBy;
    private String lastModifiedBy;
    private Long createdAt;
    private Long lastModifiedAt;

    @Version
    @Column(name = "optlock_revision")
    private long optLockRevision;

    @Column(name = "tenant", nullable = false, insertable = false, updatable = false, length = 40)
    private String tenant;

    /**
    *
    */
    public BaseEntity() {

    }

    /**
     * @param entity
     *            the entity to copy
     */
    public BaseEntity(final BaseEntity entity) {
        id = entity.id;
        createdAt = entity.createdAt;
        createdBy = entity.createdBy;
        lastModifiedAt = entity.lastModifiedAt;
        lastModifiedBy = entity.lastModifiedBy;
        optLockRevision = entity.optLockRevision;
    }

    /**
     * PrePersist listener method for all {@link BaseEntity} entities.
     */
    @PrePersist
    public void prePersist() {
        // before persisting the entity check the current ID of the tenant by
        // using the TenantAware
        // service
        final String currentTenant = SystemManagementHolder.getInstance().currentTenant();
        if (currentTenant == null) {
            throw new TenantNotExistException(
                    "Tenant " + TenantAwareHolder.getInstance().getTenantAware().getCurrentTenant()
                            + " does not exists, cannot create entity " + this.getClass() + " with id " + id);
        }
        setTenant(currentTenant.toUpperCase());
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

    public long getOptLockRevision() {
        return optLockRevision;
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

    /**
     * @return the id
     */
    @Override
    public Long getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BaseEntity [id=" + id + "]";
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() { // NOSONAR - as this is generated code
        final int prime = 31;
        int result = 1;
        result = prime * result + (id == null ? 0 : id.hashCode());
        result = prime * result + (int) (optLockRevision ^ optLockRevision >>> 32);
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
        final BaseEntity other = (BaseEntity) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (optLockRevision != other.optLockRevision) {
            return false;
        }
        return true;
    }

}

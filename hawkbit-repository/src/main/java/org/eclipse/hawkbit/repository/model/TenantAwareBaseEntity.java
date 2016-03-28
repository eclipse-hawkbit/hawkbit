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
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;

import org.eclipse.hawkbit.eventbus.CacheFieldEntityListener;
import org.eclipse.hawkbit.eventbus.EntityPropertyChangeListener;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.eclipse.hawkbit.repository.model.helper.SystemManagementHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantAwareHolder;
import org.eclipse.persistence.annotations.Multitenant;
import org.eclipse.persistence.annotations.MultitenantType;
import org.eclipse.persistence.annotations.TenantDiscriminatorColumn;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Holder of the base attributes common to all tenant aware entities.
 *
 */
@MappedSuperclass
@EntityListeners({ AuditingEntityListener.class, CacheFieldEntityListener.class, EntityPropertyChangeListener.class })
@TenantDiscriminatorColumn(name = "tenant", length = 40)
@Multitenant(MultitenantType.SINGLE_TABLE)
public abstract class TenantAwareBaseEntity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "tenant", nullable = false, insertable = false, updatable = false, length = 40)
    private String tenant;

    /**
     * Default constructor needed for JPA entities.
     */
    public TenantAwareBaseEntity() {
        // Default constructor needed for JPA entities.
    }

    /**
     * PrePersist listener method for all {@link TenantAwareBaseEntity}
     * entities.
     */
    @PrePersist
    public void prePersist() {
        // before persisting the entity check the current ID of the tenant by
        // using the TenantAware
        // service
        final String currentTenant = SystemManagementHolder.getInstance().currentTenant();
        if (currentTenant == null) {
            throw new TenantNotExistException("Tenant "
                    + TenantAwareHolder.getInstance().getTenantAware().getCurrentTenant()
                    + " does not exists, cannot create entity " + this.getClass() + " with id " + super.getId());
        }
        setTenant(currentTenant.toUpperCase());
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    @Override
    public String toString() {
        return "BaseEntity [id=" + super.getId() + "]";
    }

    /**
     * Tenant aware entities extend the equals/hashcode strategy with the tenant
     * name. That would allow for instance in a multi-schema based data
     * separation setup to have the same primary key for different entities of
     * different tenants.
     * 
     * @see org.eclipse.hawkbit.repository.model.BaseEntity#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (tenant == null ? 0 : tenant.hashCode());
        return result;
    }

    /**
     * Tenant aware entities extend the equals/hashcode strategy with the tenant
     * name. That would allow for instance in a multi-schema based data
     * separation setup to have the same primary key for different entities of
     * different tenants.
     * 
     * @see org.eclipse.hawkbit.repository.model.BaseEntity#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof TenantAwareBaseEntity)) {
            return false;
        }
        final TenantAwareBaseEntity other = (TenantAwareBaseEntity) obj;
        if (tenant == null) {
            if (other.tenant != null) {
                return false;
            }
        } else if (!tenant.equals(other.tenant)) {
            return false;
        }
        return true;
    }

}

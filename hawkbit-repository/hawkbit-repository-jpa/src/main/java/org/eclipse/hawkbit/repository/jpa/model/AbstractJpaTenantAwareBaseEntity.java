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
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.eclipse.hawkbit.repository.jpa.model.helper.TenantAwareHolder;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.helper.SystemManagementHolder;
import org.eclipse.persistence.annotations.Multitenant;
import org.eclipse.persistence.annotations.MultitenantType;
import org.eclipse.persistence.annotations.TenantDiscriminatorColumn;

/**
 * Holder of the base attributes common to all tenant aware entities.
 *
 */
@MappedSuperclass
@TenantDiscriminatorColumn(name = "tenant", length = 40)
@Multitenant(MultitenantType.SINGLE_TABLE)
public abstract class AbstractJpaTenantAwareBaseEntity extends AbstractJpaBaseEntity implements TenantAwareBaseEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "tenant", nullable = false, insertable = false, updatable = false, length = 40)
    @Size(min = 1, max = 40)
    @NotNull
    private String tenant;

    /**
     * Default constructor needed for JPA entities.
     */
    public AbstractJpaTenantAwareBaseEntity() {
        // Default constructor needed for JPA entities.
    }

    /**
     * PrePersist listener method for all {@link TenantAwareBaseEntity}
     * entities.
     */
    @PrePersist
    void prePersist() {
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

    @Override
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
    // exception squid:S2259 - obj is checked for null in super
    @SuppressWarnings("squid:S2259")
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final AbstractJpaTenantAwareBaseEntity other = (AbstractJpaTenantAwareBaseEntity) obj;
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

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.persistence.annotations.Multitenant;
import org.eclipse.persistence.annotations.MultitenantType;
import org.eclipse.persistence.annotations.TenantDiscriminatorColumn;

/**
 * Holder of the base attributes common to all tenant aware entities.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Default constructor needed for JPA entities.
@Setter
@Getter
@MappedSuperclass
// Eclipse link MultiTenant support
@TenantDiscriminatorColumn(name = "tenant", length = 40)
@Multitenant(MultitenantType.SINGLE_TABLE)
public abstract class AbstractJpaTenantAwareBaseEntity extends AbstractJpaBaseEntity implements TenantAwareBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "tenant", nullable = false, insertable = false, updatable = false, length = 40)
    @Size(min = 1, max = 40)
    @NotNull
    private String tenant;

    /**
     * Tenant aware entities extend the equals/hashcode strategy with the tenant name. That would allow for instance in a
     * multi-schema based data separation setup to have the same primary key for different entities of different tenants.
     */
    @Override
    public int hashCode() {
        return 31 * super.hashCode() + (tenant == null ? 0 : tenant.hashCode());
    }

    /**
     * Tenant aware entities extend the equals/hashcode strategy with the tenant name. That would allow for instance in a
     * multi-schema based data separation setup to have the same primary key for different entities of
     * different tenants.
     */
    @Override
    // exception squid:S2259 - obj is checked for null in super
    @SuppressWarnings("squid:S2259")
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        return Objects.equals(getTenant(), ((AbstractJpaTenantAwareBaseEntity) obj).getTenant());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [tenant=" + getTenant() + ", id=" + getId() + " / optLockRevision=" + getOptLockRevision() + "]";
    }

    /**
     * PrePersist listener method for all {@link TenantAwareBaseEntity} entities.
     *
     * // TODO - check if the tenant support should set tenant from context
     * // TODO - should we check if tenant exists in the system? Note: seems it's not good to work with db in the listener
     */
    @PrePersist
    void prePersist() {
        // before persisting the entity check the current ID of the tenant by using the TenantAware
        final String currentTenant = TenantAware.getCurrentTenant();
        if (currentTenant == null) {
            throw new TenantNotExistException(
                    String.format(
                            "Tenant %s does not exists, cannot create entity %s with id %d",
                            TenantAware.getCurrentTenant(), getClass(), getId()));
        }
        setTenant(currentTenant.toUpperCase());
    }
}
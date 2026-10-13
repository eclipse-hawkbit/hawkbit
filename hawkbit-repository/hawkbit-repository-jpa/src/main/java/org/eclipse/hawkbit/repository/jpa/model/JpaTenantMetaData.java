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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.jpa.management.JpaSystemManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.TenantMetaData;

/**
 * AccessContext entity with meta data that is configured globally for the entire
 * tenant. This entity is not tenant aware to allow the system to access it
 * through the {@link EntityManager} even before the actual tenant exists.
 *
 * Entities owned by the tenant are based on {@link TenantAwareBaseEntity}.
 */
@NoArgsConstructor // Default constructor for JPA
@Setter
@Getter
@Table(name = "sp_tenant")
@NamedEntityGraph(name = "TenantMetaData.withDetails", attributeNodes = { @NamedAttributeNode("defaultDsType") })
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaTenantMetaData extends AbstractJpaBaseEntity implements TenantMetaData {

    @Setter(AccessLevel.NONE)
    @Column(name = "tenant", nullable = false, updatable = false, length = 40)
    @Size(min = 1, max = 40)
    @NotNull
    private String tenant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_ds_type", nullable = false)
    private JpaDistributionSetType defaultDsType;

    // soft delete time (epoch millis), may be in the future (scheduled soft delete) - the tenant is soft deleted iff
    // deletedAt != null && deletedAt <= now
    @Column(name = "deleted_at")
    private Long deletedAt;

    @Column(name = "deleted_by", length = USERNAME_FIELD_LENGTH)
    private String deletedBy;

    // time (epoch millis) after which a soft deleted tenant is hard deleted
    @Column(name = "hard_delete_at")
    private Long hardDeleteAt;

    public JpaTenantMetaData(final String tenant, final DistributionSetType defaultDsType) {
        this.defaultDsType = (JpaDistributionSetType) defaultDsType;
        this.tenant = JpaSystemManagement.normalizeTenant(tenant);
    }

    @Override
    public String getTenant() {
        return JpaSystemManagement.normalizeTenant(tenant);
    }
}

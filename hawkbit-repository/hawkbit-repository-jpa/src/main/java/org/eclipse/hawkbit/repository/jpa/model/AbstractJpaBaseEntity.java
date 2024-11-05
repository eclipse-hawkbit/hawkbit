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

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Holder of the base attributes common to all entities.
 */
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners({ AuditingEntityListener.class, EntityPropertyChangeListener.class, EntityInterceptorListener.class })
public abstract class AbstractJpaBaseEntity implements BaseEntity {

    protected static final int USERNAME_FIELD_LENGTH = 64;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String createdBy;
    private String lastModifiedBy;
    private long createdAt;
    private long lastModifiedAt;

    @Version
    @Column(name = "optlock_revision")
    private int optLockRevision;

    /**
     * Default constructor needed for JPA entities.
     */
    protected AbstractJpaBaseEntity() {
        // Default constructor needed for JPA entities.
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "created_at", updatable = false, nullable = false)
    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "created_by", updatable = false, nullable = false, length = USERNAME_FIELD_LENGTH)
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "last_modified_at", nullable = false)
    public long getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "last_modified_by", nullable = false, length = USERNAME_FIELD_LENGTH)
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @LastModifiedBy
    public void setLastModifiedBy(final String lastModifiedBy) {
        if (isController()) {
            return;
        }

        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public int getOptLockRevision() {
        return optLockRevision;
    }

    public void setOptLockRevision(final int optLockRevision) {
        this.optLockRevision = optLockRevision;
    }

    @LastModifiedDate
    public void setLastModifiedAt(final long lastModifiedAt) {

        if (isController()) {
            return;
        }

        this.lastModifiedAt = lastModifiedAt;
    }

    @CreatedBy
    public void setCreatedBy(final String createdBy) {
        if (isController()) {
            this.createdBy = "CONTROLLER_PLUG_AND_PLAY";

            // In general modification audit entry is not changed by the
            // controller. However, we want to stay consistent with
            // EnableJpaAuditing#modifyOnCreate=true.
            this.lastModifiedBy = this.createdBy;
            return;
        }

        this.createdBy = createdBy;
    }

    @CreatedDate
    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;

        // In general modification audit entry is not changed by the controller.
        // However, we want to stay consistent with
        // EnableJpaAuditing#modifyOnCreate=true.
        if (isController()) {
            this.lastModifiedAt = createdAt;
        }
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Defined equals/hashcode strategy for the repository in general is that an
     * entity is equal if it has the same {@link #getId()} and
     * {@link #getOptLockRevision()} and class.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    // Exception squid:S864 - generated code
    @SuppressWarnings({ "squid:S864" })
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (id == null ? 0 : id.hashCode());
        result = prime * result + optLockRevision;
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    /**
     * Defined equals/hashcode strategy for the repository in general is that an
     * entity is equal if it has the same {@link #getId()} and
     * {@link #getOptLockRevision()} and class.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(this.getClass().isInstance(obj))) {
            return false;
        }
        final AbstractJpaBaseEntity other = (AbstractJpaBaseEntity) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return optLockRevision == other.optLockRevision;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + id + "]";
    }

    private boolean isController() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication()
                .getDetails() instanceof TenantAwareAuthenticationDetails
                && ((TenantAwareAuthenticationDetails) SecurityContextHolder.getContext().getAuthentication()
                .getDetails()).isController();
    }

}

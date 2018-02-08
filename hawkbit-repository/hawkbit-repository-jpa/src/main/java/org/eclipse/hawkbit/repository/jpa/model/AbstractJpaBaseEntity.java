/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

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
 *
 */
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners({ AuditingEntityListener.class, EntityPropertyChangeListener.class, EntityInterceptorListener.class })
public abstract class AbstractJpaBaseEntity implements BaseEntity {
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
    public AbstractJpaBaseEntity() {
        // Default constructor needed for JPA entities.
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "created_at", insertable = true, updatable = false)
    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "created_by", insertable = true, updatable = false, length = 40)
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "last_modified_at", insertable = true, updatable = true)
    public long getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "last_modified_by", insertable = true, updatable = true, length = 40)
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @CreatedBy
    public void setCreatedBy(final String createdBy) {
        if (isController()) {
            this.createdBy = "CONTROLLER_PLUG_AND_PLAY";
            return;
        }

        this.createdBy = createdBy;
    }

    @CreatedDate
    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;
    }

    @LastModifiedDate
    public void setLastModifiedAt(final long lastModifiedAt) {

        if (isController()) {
            return;
        }

        this.lastModifiedAt = lastModifiedAt;
    }

    @LastModifiedBy
    public void setLastModifiedBy(final String lastModifiedBy) {
        if (isController()) {
            return;
        }

        this.lastModifiedBy = lastModifiedBy;
    }

    private boolean isController() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication()
                        .getDetails() instanceof TenantAwareAuthenticationDetails
                && ((TenantAwareAuthenticationDetails) SecurityContextHolder.getContext().getAuthentication()
                        .getDetails()).isController();
    }

    @Override
    public int getOptLockRevision() {
        return optLockRevision;
    }

    public void setOptLockRevision(final int optLockRevision) {
        this.optLockRevision = optLockRevision;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + id + "]";
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

}

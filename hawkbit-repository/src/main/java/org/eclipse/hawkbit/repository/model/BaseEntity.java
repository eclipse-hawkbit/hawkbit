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
import javax.persistence.Version;

import org.eclipse.hawkbit.eventbus.CacheFieldEntityListener;
import org.eclipse.hawkbit.eventbus.EntityPropertyChangeListener;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.hateoas.Identifiable;

/**
 * Holder of the base attributes common to all entities.
 *
 */
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners({ AuditingEntityListener.class, CacheFieldEntityListener.class, EntityPropertyChangeListener.class })
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

    /**
     * Default constructor needed for JPA entities.
     */
    public BaseEntity() {
        // Default constructor needed for JPA entities.
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

    @CreatedBy
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    @LastModifiedBy
    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @CreatedDate
    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    @LastModifiedDate
    public void setLastModifiedAt(final Long lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public long getOptLockRevision() {
        return optLockRevision;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "BaseEntity [id=" + id + "]";
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
    public int hashCode() { // NOSONAR - as this is generated code
        final int prime = 31;
        int result = 1;
        result = prime * result + (id == null ? 0 : id.hashCode());
        result = prime * result + (int) (optLockRevision ^ optLockRevision >>> 32);
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

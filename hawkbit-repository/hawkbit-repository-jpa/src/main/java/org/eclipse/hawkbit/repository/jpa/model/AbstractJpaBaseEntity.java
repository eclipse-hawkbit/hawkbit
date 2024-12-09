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

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Base hawkBit entity class containing the common attributes.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Default constructor needed for JPA entities.
@Setter
@Getter
@MappedSuperclass
@EntityListeners({ AuditingEntityListener.class, EntityInterceptorListener.class })
public abstract class AbstractJpaBaseEntity implements BaseEntity {

    protected static final int USERNAME_FIELD_LENGTH = 64;

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Version
    @Column(name = "optlock_revision")
    private int optLockRevision;

    // Audit fields. use property access to ensure that setters will be called and checked for modification
    // (touch implementation depends on setLastModifiedAt(0).
    @Column(name = "created_by", updatable = false, nullable = false, length = USERNAME_FIELD_LENGTH)
    private String createdBy;
    @Column(name = "created_at", updatable = false, nullable = false)
    private long createdAt;
    @Column(name = "last_modified_by", nullable = false, length = USERNAME_FIELD_LENGTH)
    private String lastModifiedBy;
    @Column(name = "last_modified_at", nullable = false)
    private long lastModifiedAt;

    @CreatedBy
    public void setCreatedBy(final String createdBy) {
        if (isController()) {
            this.createdBy = "CONTROLLER_PLUG_AND_PLAY";

            // In general modification audit entry is not changed by the controller.
            // However, we want to stay consistent with EnableJpaAuditing#modifyOnCreate=true.
            this.lastModifiedBy = this.createdBy;
            return;
        }

        this.createdBy = createdBy;
    }

    // maybe needed to have correct createdBy value in the database
    @Access(AccessType.PROPERTY)
    public String getCreatedBy() {
        return createdBy;
    }

    @CreatedDate
    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;

        // In general modification audit entry is not changed by the controller.
        // However, we want to stay consistent with EnableJpaAuditing#modifyOnCreate=true.
        if (isController()) {
            this.lastModifiedAt = createdAt;
        }
    }

    @Column(name = "created_at", updatable = false, nullable = false)
    @Access(AccessType.PROPERTY)
    public long getCreatedAt() {
        return createdAt;
    }

    @LastModifiedBy
    public void setLastModifiedBy(final String lastModifiedBy) {
        if (isController()) {
            return;
        }

        this.lastModifiedBy = lastModifiedBy;
    }

    @Column(name = "last_modified_by", nullable = false, length = USERNAME_FIELD_LENGTH)
    @Access(AccessType.PROPERTY)
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @LastModifiedDate
    public void setLastModifiedAt(final long lastModifiedAt) {
        if (isController()) {
            return;
        }

        this.lastModifiedAt = lastModifiedAt;
    }

    // property access to make entity manager to detect touch
    @Access(AccessType.PROPERTY)
    public long getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Defined equals/hashcode strategy for the repository in general is that an entity is equal if it has the same {@link #getId()} and
     * {@link #getOptLockRevision()} and class.
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
     * Defined equals/hashcode strategy for the repository in general is that an entity is equal if it has the same {@link #getId()} and
     * {@link #getOptLockRevision()} and class.
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
        final Long id = getId();
        final Long otherId = other.getId();
        if (id == null) {
            if (otherId != null) {
                return false;
            }
        } else if (!id.equals(otherId)) {
            return false;
        }
        return getOptLockRevision() == other.getOptLockRevision();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + id + "]";
    }

    @PostPersist
    public void postInsert() {
        if (this instanceof EventAwareEntity eventAwareEntity) {
            doNotify(eventAwareEntity::fireCreateEvent);
        }
    }

    @PostUpdate
    public void postUpdate() {
        if (this instanceof EventAwareEntity eventAwareEntity) {
            doNotify(eventAwareEntity::fireUpdateEvent);
        }
    }

    @PostRemove
    public void postDelete() {
        if (this instanceof EventAwareEntity eventAwareEntity) {
            doNotify(eventAwareEntity::fireDeleteEvent);
        }
    }

    protected static void doNotify(final Runnable runnable) {
        // fire events onl AFTER transaction commit
        AfterTransactionCommitExecutorHolder.getInstance().getAfterCommit().afterCommit(runnable);
    }

    private boolean isController() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication()
                .getDetails() instanceof TenantAwareAuthenticationDetails tenantAwareDetails
                && tenantAwareDetails.isController();
    }
}
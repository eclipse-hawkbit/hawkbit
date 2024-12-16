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

import java.io.Serializable;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Core information of all entities.
 */
@MappedSuperclass
@EntityListeners({ AuditingEntityListener.class, EntityInterceptorListener.class })
public abstract class AbstractBaseEntity implements BaseEntity, Serializable {

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
        result = prime * result + (getId() == null ? 0 : getId().hashCode());
        result = prime * result + getOptLockRevision();
        result = prime * result + getClass().getName().hashCode();
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
        if (!getClass().isInstance(obj)) {
            return false;
        }
        final BaseEntity other = (BaseEntity) obj;
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
        return getClass().getSimpleName() + " [id=" + getId() + "]";
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

    protected boolean isController() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication()
                .getDetails() instanceof TenantAwareAuthenticationDetails tenantAwareDetails
                && tenantAwareDetails.isController();
    }
}
/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.eclipse.hawkbit.repository.jpa.model.helper.EntityInterceptorHolder;

/**
 * Entity listener which calls all entity interceptor for the lifecyles
 * callbacks.
 */
public class EntityInterceptorListener {

    @PrePersist
    protected void prePersist(final Object entity) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors()
                .forEach(interceptor -> interceptor.prePersist(entity));
    }

    @PostPersist
    protected void postPersist(final Object entity) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors()
                .forEach(interceptor -> interceptor.postPersist(entity));
    }

    @PostRemove
    protected void postRemove(final Object entity) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors()
                .forEach(interceptor -> interceptor.postRemove(entity));
    }

    @PreRemove
    protected void preRemove(final Object entity) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors()
                .forEach(interceptor -> interceptor.preRemove(entity));
    }

    @PostLoad
    protected void postLoad(final Object entity) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors()
                .forEach(interceptor -> interceptor.postLoad(entity));
    }

    @PreUpdate
    protected void preUpdate(final Object entity) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors()
                .forEach(interceptor -> interceptor.preUpdate(entity));
    }

    @PostUpdate
    protected void postUpdate(final Object entity) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors()
                .forEach(interceptor -> interceptor.postUpdate(entity));
    }

}

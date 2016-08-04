/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.function.Consumer;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.eclipse.hawkbit.repository.jpa.model.helper.EntityInterceptorHolder;
import org.eclipse.hawkbit.repository.model.EntityInterceptor;

/**
 * Entity listener which calls the callback's of all registered entity
 * interceptors.
 */
public class EntityInterceptorListener {

    @PrePersist
    protected void prePersist(final Object entity) {
        notifyAll(interceptor -> interceptor.prePersist(entity));
    }

    @PostPersist
    protected void postPersist(final Object entity) {
        notifyAll(interceptor -> interceptor.postPersist(entity));
    }

    @PostRemove
    protected void postRemove(final Object entity) {
        notifyAll(interceptor -> interceptor.postRemove(entity));
    }

    @PreRemove
    protected void preRemove(final Object entity) {
        notifyAll(interceptor -> interceptor.preRemove(entity));
    }

    @PostLoad
    protected void postLoad(final Object entity) {
        notifyAll(interceptor -> interceptor.postLoad(entity));
    }

    @PreUpdate
    protected void preUpdate(final Object entity) {
        notifyAll(interceptor -> interceptor.preUpdate(entity));
    }

    @PostUpdate
    protected void postUpdate(final Object entity) {
        notifyAll(interceptor -> interceptor.postUpdate(entity));
    }

    private void notifyAll(final Consumer<? super EntityInterceptor> action) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors().forEach(action);
    }

}

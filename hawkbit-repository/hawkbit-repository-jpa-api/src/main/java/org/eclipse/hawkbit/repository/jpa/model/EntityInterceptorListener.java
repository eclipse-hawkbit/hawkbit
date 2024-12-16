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

import java.util.function.Consumer;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import org.eclipse.hawkbit.repository.jpa.EntityInterceptor;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityInterceptorHolder;

/**
 * Entity listener which calls the callback's of all registered entity interceptors.
 */
public class EntityInterceptorListener {

    /**
     * Callback for lifecyle event <i>pre persist</i>.
     *
     * @param entity the JPA entity which this listener is associated with
     */
    @PrePersist
    public void prePersist(final Object entity) {
        notifyAll(interceptor -> interceptor.prePersist(entity));
    }

    /**
     * Callback for lifecyle event <i>post persist</i>.
     *
     * @param entity the JPA entity which this listener is associated with
     */
    @PostPersist
    public void postPersist(final Object entity) {
        notifyAll(interceptor -> interceptor.postPersist(entity));
    }

    /**
     * Callback for lifecyle event <i>post remove</i>.
     *
     * @param entity the JPA entity which this listener is associated with
     */
    @PostRemove
    public void postRemove(final Object entity) {
        notifyAll(interceptor -> interceptor.postRemove(entity));
    }

    /**
     * Callback for lifecyle event <i>pre remove</i>.
     *
     * @param entity the JPA entity which this listener is associated with
     */
    @PreRemove
    public void preRemove(final Object entity) {
        notifyAll(interceptor -> interceptor.preRemove(entity));
    }

    /**
     * Callback for lifecyle event <i>post load</i>.
     *
     * @param entity the JPA entity which this listener is associated with
     */
    @PostLoad
    public void postLoad(final Object entity) {
        notifyAll(interceptor -> interceptor.postLoad(entity));
    }

    /**
     * Callback for lifecyle event <i>pre update</i>.
     *
     * @param entity the JPA entity which this listener is associated with
     */
    @PreUpdate
    public void preUpdate(final Object entity) {
        notifyAll(interceptor -> interceptor.preUpdate(entity));
    }

    /**
     * Callback for lifecyle event <i>post update</i>.
     *
     * @param entity the JPA entity which this listener is associated with
     */
    @PostUpdate
    public void postUpdate(final Object entity) {
        notifyAll(interceptor -> interceptor.postUpdate(entity));
    }

    private static void notifyAll(final Consumer<? super EntityInterceptor> action) {
        EntityInterceptorHolder.getInstance().getEntityInterceptors().forEach(action);
    }
}

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

import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.descriptors.DescriptorEventAdapter;
import org.eclipse.persistence.queries.UpdateObjectQuery;

/**
 * Listens to change in property values of an entity and calls the corresponding
 * {@link EventAwareEntity}.
 */
public class EntityPropertyChangeListener extends DescriptorEventAdapter {

    @Override
    public void postDelete(final DescriptorEvent event) {
        final Object object = event.getObject();
        if (isEventAwareEntity(object)) {
            doNotifiy(() -> ((EventAwareEntity) object).fireDeleteEvent(event));
        }
    }

    @Override
    public void postInsert(final DescriptorEvent event) {
        final Object object = event.getObject();
        if (isEventAwareEntity(object)) {
            doNotifiy(() -> ((EventAwareEntity) object).fireCreateEvent(event));
        }
    }

    @Override
    public void postUpdate(final DescriptorEvent event) {

        final Object object = event.getObject();
        if (isEventAwareEntity(object)
                && isFireUpdate((EventAwareEntity) object, (UpdateObjectQuery) event.getQuery())) {
            doNotifiy(() -> ((EventAwareEntity) object).fireUpdateEvent(event));
        }

    }

    private static boolean isEventAwareEntity(final Object object) {
        return object instanceof EventAwareEntity;
    }

    private static void doNotifiy(final Runnable runnable) {
        AfterTransactionCommitExecutorHolder.getInstance().getAfterCommit().afterCommit(runnable);
    }

    private static boolean isFireUpdate(final EventAwareEntity entity, final UpdateObjectQuery query) {
        return entity.getUpdateIgnoreFields().isEmpty() || query.getObjectChangeSet().getChangedAttributeNames()
                .stream().anyMatch(field -> !entity.getUpdateIgnoreFields().contains(field));
    }

}

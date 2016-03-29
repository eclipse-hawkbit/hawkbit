/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.eventbus.event.AbstractPropertyChangeEvent;
import org.eclipse.hawkbit.eventbus.event.ActionCreatedEvent;
import org.eclipse.hawkbit.eventbus.event.ActionPropertyChangeEvent;
import org.eclipse.hawkbit.eventbus.event.RolloutGroupPropertyChangeEvent;
import org.eclipse.hawkbit.eventbus.event.RolloutPropertyChangeEvent;
import org.eclipse.hawkbit.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.model.helper.EventBusHolder;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.descriptors.DescriptorEventAdapter;
import org.eclipse.persistence.internal.sessions.ObjectChangeSet;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;

import com.google.common.eventbus.EventBus;

/**
 * Listens to change in property values of an entity.
 *
 */
public class EntityPropertyChangeListener extends DescriptorEventAdapter {
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.persistence.descriptors.DescriptorEventAdapter#postInsert
     * (org.eclipse.persistence.descriptors.DescriptorEvent)
     */
    @Override
    public void postInsert(final DescriptorEvent event) {
        if (event.getObject().getClass().equals(Action.class)) {
            final Action action = (Action) event.getObject();
            if (action.getRollout() != null) {
                final EventBus eventBus = getEventBus();
                final AfterTransactionCommitExecutor afterCommit = getAfterTransactionCommmitExecutor();
                afterCommit.afterCommit(() -> eventBus.post(new ActionCreatedEvent(action)));
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.persistence.descriptors.DescriptorEventAdapter#postUpdate
     * (org.eclipse.persistence.descriptors.DescriptorEvent)
     */
    @Override
    public void postUpdate(final DescriptorEvent event) {
        if (event.getObject().getClass().equals(Action.class)) {
            getAfterTransactionCommmitExecutor().afterCommit(
                    () -> getEventBus()
                            .post(new ActionPropertyChangeEvent((Action) event.getObject(), getChangeSet(Action.class,
                                    event))));
        } else if (event.getObject().getClass().equals(Rollout.class)) {
            getAfterTransactionCommmitExecutor().afterCommit(
                    () -> getEventBus().post(
                            new RolloutPropertyChangeEvent((Rollout) event.getObject(), getChangeSet(Rollout.class,
                                    event))));
        } else if (event.getObject().getClass().equals(RolloutGroup.class)) {
            getAfterTransactionCommmitExecutor().afterCommit(
                    () -> getEventBus().post(
                            new RolloutGroupPropertyChangeEvent((RolloutGroup) event.getObject(), getChangeSet(
                                    RolloutGroup.class, event))));
        }
    }

    private <T extends TenantAwareBaseEntity> Map<String, AbstractPropertyChangeEvent<T>.Values> getChangeSet(
            final Class<T> clazz, final DescriptorEvent event) {
        final T rolloutGroup = clazz.cast(event.getObject());
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        return changeSet
                .getChanges()
                .stream()
                .filter(record -> record instanceof DirectToFieldChangeRecord)
                .map(record -> (DirectToFieldChangeRecord) record)
                .collect(
                        Collectors.toMap(record -> record.getAttribute(), record -> new AbstractPropertyChangeEvent<T>(
                                rolloutGroup, null).new Values(record.getOldValue(), record.getNewValue())));
    }

    private AfterTransactionCommitExecutor getAfterTransactionCommmitExecutor() {
        return AfterTransactionCommitExecutorHolder.getInstance().getAfterCommit();
    }

    private EventBus getEventBus() {
        return EventBusHolder.getInstance().getEventBus();
    }
}

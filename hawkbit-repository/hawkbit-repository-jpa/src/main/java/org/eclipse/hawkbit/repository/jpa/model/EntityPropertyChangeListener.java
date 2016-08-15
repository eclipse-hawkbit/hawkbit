/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.eventbus.event.AbstractPropertyChangeEvent.PropertyChange;
import org.eclipse.hawkbit.repository.eventbus.event.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.ActionPropertyChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutGroupPropertyChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutPropertyChangeEvent;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.EventBusHolder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.descriptors.DescriptorEventAdapter;
import org.eclipse.persistence.internal.sessions.ObjectChangeSet;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;

import com.google.common.eventbus.EventBus;

/**
 * Listens to change in property values of an entity.
 */
public class EntityPropertyChangeListener extends DescriptorEventAdapter {

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

    @Override
    public void postUpdate(final DescriptorEvent event) {
        if (event.getObject().getClass().equals(JpaAction.class)) {
            getAfterTransactionCommmitExecutor().afterCommit(() -> getEventBus()
                    .post(new ActionPropertyChangeEvent((Action) event.getObject(), getChangeSet(event))));
        } else if (event.getObject().getClass().equals(JpaRollout.class)) {
            getAfterTransactionCommmitExecutor().afterCommit(() -> getEventBus()
                    .post(new RolloutPropertyChangeEvent((Rollout) event.getObject(), getChangeSet(event))));
        } else if (event.getObject().getClass().equals(JpaRolloutGroup.class)) {
            getAfterTransactionCommmitExecutor().afterCommit(() -> getEventBus()
                    .post(new RolloutGroupPropertyChangeEvent((RolloutGroup) event.getObject(), getChangeSet(event))));
        }
    }

    private Map<String, PropertyChange> getChangeSet(final DescriptorEvent event) {
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        return changeSet.getChanges().stream().filter(record -> record instanceof DirectToFieldChangeRecord)
                .map(record -> (DirectToFieldChangeRecord) record)
                .collect(Collectors.toMap(record -> record.getAttribute(),
                        record -> new PropertyChange(record.getOldValue(), record.getNewValue())));
    }

    private AfterTransactionCommitExecutor getAfterTransactionCommmitExecutor() {
        return AfterTransactionCommitExecutorHolder.getInstance().getAfterCommit();
    }

    private EventBus getEventBus() {
        return EventBusHolder.getInstance().getEventBus();
    }
}

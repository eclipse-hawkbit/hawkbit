/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.eventbus;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.eventbus.event.AbstractPropertyChangeEvent;
import org.eclipse.hawkbit.eventbus.event.ActionCreatedEvent;
import org.eclipse.hawkbit.eventbus.event.ActionPropertyChangeEvent;
import org.eclipse.hawkbit.eventbus.event.RolloutPropertyChangeEvent;
import org.eclipse.hawkbit.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.model.helper.EventBusHolder;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.descriptors.DescriptorEventAdapter;
import org.eclipse.persistence.internal.sessions.ObjectChangeSet;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;

import com.google.common.eventbus.EventBus;

/**
 * @author AMU7KOR
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
                final EventBus eventBus = EventBusHolder.getInstance().getEventBus();
                final AfterTransactionCommitExecutor afterCommit = AfterTransactionCommitExecutorHolder.getInstance()
                        .getAfterCommit();
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
            onActionPropertyChange(event);
        } else if (event.getObject().getClass().equals(Rollout.class)) {
            onRolloutPropertyChange(event);
        }
    }

    private void onRolloutPropertyChange(final DescriptorEvent event) {
        final Rollout rollout = (Rollout) event.getObject();
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        final Map<String, AbstractPropertyChangeEvent<Rollout>.Values> changeSetValues = changeSet
                .getChanges()
                .stream()
                .filter(record -> record instanceof DirectToFieldChangeRecord
                        && ("status").equals(record.getAttribute()))
                .map(record -> (DirectToFieldChangeRecord) record)
                .collect(
                        Collectors.toMap(
                                record -> record.getAttribute(),
                                record -> new AbstractPropertyChangeEvent<Rollout>(rollout, null).new Values(record
                                        .getOldValue(), record.getNewValue())));
        final EventBus eventBus = EventBusHolder.getInstance().getEventBus();
        final AfterTransactionCommitExecutor afterCommit = AfterTransactionCommitExecutorHolder.getInstance()
                .getAfterCommit();
        afterCommit.afterCommit(() -> eventBus.post(new RolloutPropertyChangeEvent(rollout, changeSetValues)));

    }

    private void onActionPropertyChange(final DescriptorEvent event) {
        final Action action = (Action) event.getObject();
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        final Map<String, AbstractPropertyChangeEvent<Action>.Values> changeSetValues = changeSet
                .getChanges()
                .stream()
                .filter(record -> record instanceof DirectToFieldChangeRecord
                        && ("status").equals(record.getAttribute()))
                .map(record -> (DirectToFieldChangeRecord) record)
                .collect(
                        Collectors.toMap(
                                record -> record.getAttribute(),
                                record -> new AbstractPropertyChangeEvent<Action>(action, null).new Values(record
                                        .getOldValue(), record.getNewValue())));

        final EventBus eventBus = EventBusHolder.getInstance().getEventBus();
        final AfterTransactionCommitExecutor afterCommit = AfterTransactionCommitExecutorHolder.getInstance()
                .getAfterCommit();
        afterCommit.afterCommit(() -> eventBus.post(new ActionPropertyChangeEvent(action, changeSetValues)));

    }
}

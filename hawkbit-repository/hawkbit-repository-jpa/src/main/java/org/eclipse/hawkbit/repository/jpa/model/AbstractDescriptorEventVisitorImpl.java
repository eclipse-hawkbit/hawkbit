/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.eventbus.event.AbstractPropertyChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.ActionPropertyChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetUpdateEvent;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutGroupPropertyChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutPropertyChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.DescriptorEventDetails.ActionType;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.EventBusHolder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.internal.sessions.ObjectChangeSet;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

/**
 * Implementation of @link{AbstractDescriptorEventVisitor} .Publishes the
 * appropriate event after any action on entity like create/update.
 *
 */
public class AbstractDescriptorEventVisitorImpl implements
		AbstractDescriptorEventVisitor {

	private static final String COMPLETE = "complete";
	private static final Logger LOG = LoggerFactory
			.getLogger(AbstractDescriptorEventVisitorImpl.class);

	@Override
	public void publishEventPostAction(final DescriptorEventDetails event) {
		Method method = null;
		DescriptorEvent descriptorEvent = event.getDescriptorEvent();
		ActionType actiontype = event.getActiontype();
		try {
			method = getMethod(descriptorEvent, actiontype);
			if (method != null) {
				method.invoke(this, descriptorEvent.getObject(),
						descriptorEvent);
			}
		} catch (NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			LOG.info(
					"Exception when invoking approriate method to publis event {}",
					e);
		}
	}

	private Method getMethod(DescriptorEvent descriptorEvent,
			ActionType actiontype) throws NoSuchMethodException {
		if (actiontype == ActionType.UPDATE) {
			return this.getClass().getMethod("publishEventAfterUpdate",
					descriptorEvent.getObject().getClass(),
					DescriptorEvent.class);
		} else if (actiontype == ActionType.CREATE) {
			return this.getClass().getMethod("publishEventAfterCreate",
					descriptorEvent.getObject().getClass(),
					DescriptorEvent.class);
		}
		return null;
	}

	public void publishEventAfterCreate(JpaAction action, DescriptorEvent event) {
		if (action.getRollout() != null) {
			getAfterTransactionCommmitExecutor().afterCommit(
					() -> getEventBus().post(new ActionCreatedEvent(action)));
		}
	}

	public void publishEventAfterCreate(JpaTarget target, DescriptorEvent event) {
		getAfterTransactionCommmitExecutor().afterCommit(
				() -> getEventBus().post(new TargetCreatedEvent(target)));
	}

	public void publishEventAfterCreate(JpaDistributionSet ds,
			DescriptorEvent event) {
		getAfterTransactionCommmitExecutor().afterCommit(
				() -> getEventBus().post(new DistributionCreatedEvent(ds)));

	}

	public void publishEventAfterUpdate(JpaAction action, DescriptorEvent event) {
		getAfterTransactionCommmitExecutor().afterCommit(
				() -> getEventBus().post(
						new ActionPropertyChangeEvent(action, getChangeSet(
								Action.class, event))));
	}

	public void publishEventAfterUpdate(JpaTarget target, DescriptorEvent event) {
		getAfterTransactionCommmitExecutor().afterCommit(
				() -> getEventBus().post(new TargetUpdatedEvent(target)));
	}

	public void publishEventAfterUpdate(JpaTargetInfo targetInfo,
			DescriptorEvent event) {
		getAfterTransactionCommmitExecutor()
				.afterCommit(
						() -> getEventBus().post(
								new TargetInfoUpdateEvent(targetInfo)));
	}

	public void publishEventAfterUpdate(JpaRollout entity, DescriptorEvent event) {
		getAfterTransactionCommmitExecutor().afterCommit(
				() -> getEventBus().post(
						new RolloutPropertyChangeEvent(entity, getChangeSet(
								Rollout.class, event))));
	}

	public void publishEventAfterUpdate(JpaRolloutGroup entity,
			DescriptorEvent event) {
		getAfterTransactionCommmitExecutor().afterCommit(
				() -> getEventBus().post(
						new RolloutGroupPropertyChangeEvent(entity,
								getChangeSet(RolloutGroup.class, event))));

	}

	public void publishEventAfterUpdate(JpaDistributionSet entity,
			DescriptorEvent event) {
		Map<String, AbstractPropertyChangeEvent<JpaDistributionSet>.Values> changeSet = getChangeSet(
				JpaDistributionSet.class, event);
		if (changeSet.containsKey(COMPLETE)
				&& changeSet.get(COMPLETE).getOldValue().equals(false)
				&& changeSet.get(COMPLETE).getNewValue().equals(true)) {
			getAfterTransactionCommmitExecutor().afterCommit(
					() -> getEventBus().post(
							new DistributionCreatedEvent(entity)));
		}

		getAfterTransactionCommmitExecutor().afterCommit(
				() -> getEventBus()
						.post(new DistributionSetUpdateEvent(entity)));

	}

	private <T extends TenantAwareBaseEntity> Map<String, AbstractPropertyChangeEvent<T>.Values> getChangeSet(
			final Class<T> clazz, final DescriptorEvent event) {
		final T rolloutGroup = clazz.cast(event.getObject());
		final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery())
				.getObjectChangeSet();
		return changeSet
				.getChanges()
				.stream()
				.filter(record -> record instanceof DirectToFieldChangeRecord)
				.map(record -> (DirectToFieldChangeRecord) record)
				.collect(
						Collectors.toMap(
								record -> record.getAttribute(),
								record -> new AbstractPropertyChangeEvent<T>(
										rolloutGroup, null).new Values(record
										.getOldValue(), record.getNewValue())));
	}

	private AfterTransactionCommitExecutor getAfterTransactionCommmitExecutor() {
		return AfterTransactionCommitExecutorHolder.getInstance()
				.getAfterCommit();
	}

	private EventBus getEventBus() {
		return EventBusHolder.getInstance().getEventBus();
	}

}

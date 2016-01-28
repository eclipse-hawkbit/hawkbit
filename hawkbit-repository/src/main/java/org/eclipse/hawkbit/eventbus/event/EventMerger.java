/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.hawkbit.eventbus.EventSubscriber;
import org.eclipse.hawkbit.eventbus.event.Event;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * 
 * @author Michael Hirsch
 *
 */
@EventSubscriber
public class EventMerger {

    private static final Set<RolloutEventKey> rolloutEvents = ConcurrentHashMap.newKeySet();
    private static final Set<RolloutEventKey> rolloutGroupEvents = ConcurrentHashMap.newKeySet();

    @Autowired
    private EventBus eventBus;

    @Scheduled(initialDelay = 10000, fixedDelay = 2000)
    public void rolloutEventScheduler() {
        final Iterator<RolloutEventKey> rolloutIterator = rolloutEvents.iterator();
        while (rolloutIterator.hasNext()) {
            final RolloutEventKey eventKey = rolloutIterator.next();
            eventBus.post(new RolloutChangeEvent(1, eventKey.tenant, eventKey.rolloutId));
            rolloutIterator.remove();
        }

        final Iterator<RolloutEventKey> rolloutGroupIterator = rolloutEvents.iterator();
        while (rolloutGroupIterator.hasNext()) {
            final RolloutEventKey eventKey = rolloutGroupIterator.next();
            eventBus.post(new RolloutGroupChangeEvent(1, eventKey.tenant, eventKey.rolloutId));
            rolloutGroupIterator.remove();
        }
    }

    @Subscribe
    public void onEvent(final Event event) {
        Long rolloutId = null;
        Long rolloutGroupId = null;

        if (event instanceof ActionCreatedEvent) {
            final Rollout rollout = ((ActionCreatedEvent) event).getEntity().getRollout();
            if (rollout != null) {
                rolloutId = rollout.getId();
            }
        } else if (event instanceof ActionPropertyChangeEvent) {
            final Rollout rollout = ((ActionPropertyChangeEvent) event).getEntity().getRollout();
            if (rollout != null) {
                rolloutId = rollout.getId();
            }
        } else if (event instanceof RolloutPropertyChangeEvent) {
            rolloutId = ((RolloutPropertyChangeEvent) event).getEntity().getId();
        } else if (event instanceof RolloutPropertyChangeEvent) {
            rolloutId = ((RolloutPropertyChangeEvent) event).getEntity().getId();
        } else if (event instanceof RolloutGroupCreatedEvent) {
            rolloutId = ((RolloutGroupCreatedEvent) event).getRolloutId();
            rolloutGroupId = ((RolloutGroupCreatedEvent) event).getRolloutGroupId();
        } else if (event instanceof RolloutGroupPropertyChangeEvent) {
            final RolloutGroup rolloutGroup = ((RolloutGroupPropertyChangeEvent) event).getEntity();
            rolloutId = rolloutGroup.getRollout().getId();
            rolloutGroupId = rolloutGroup.getId();
        }

        if (rolloutId != null && rolloutGroupId != null) {
            rolloutGroupEvents.add(new RolloutEventKey(rolloutId, rolloutGroupId, event.getTenant()));
        } else if (rolloutId != null) {
            rolloutEvents.add(new RolloutEventKey(rolloutId, event.getTenant()));
        }
    }

    private static final class RolloutEventKey {
        private final Long rolloutId;
        private final String tenant;
        private final Long rolloutGroupId;

        private RolloutEventKey(final Long rolloutId, final Long rolloutGroupId, final String tenant) {
            this.rolloutGroupId = rolloutGroupId;
            this.rolloutId = rolloutId;
            this.tenant = tenant;
        }

        private RolloutEventKey(final Long rolloutId, final String tenant) {
            this(rolloutId, null, tenant);
        }

        @Override
        public int hashCode() {// NOSONAR - as this is generated
            final int prime = 31;
            int result = 1;
            result = prime * result + ((rolloutGroupId == null) ? 0 : rolloutGroupId.hashCode());
            result = prime * result + ((rolloutId == null) ? 0 : rolloutId.hashCode());
            result = prime * result + ((tenant == null) ? 0 : tenant.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {// NOSONAR - as this is
                                                 // generated
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RolloutEventKey other = (RolloutEventKey) obj;
            if (rolloutGroupId == null) {
                if (other.rolloutGroupId != null) {
                    return false;
                }
            } else if (!rolloutGroupId.equals(other.rolloutGroupId)) {
                return false;
            }
            if (rolloutId == null) {
                if (other.rolloutId != null) {
                    return false;
                }
            } else if (!rolloutId.equals(other.rolloutId)) {
                return false;
            }
            if (tenant == null) {
                if (other.tenant != null) {
                    return false;
                }
            } else if (!tenant.equals(other.tenant)) {
                return false;
            }
            return true;
        }

    }
}

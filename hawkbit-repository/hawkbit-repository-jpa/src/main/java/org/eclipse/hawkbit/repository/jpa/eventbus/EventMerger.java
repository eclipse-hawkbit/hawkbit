/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.eventbus;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.hawkbit.eventbus.event.Event;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.entity.ActionPropertyChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.entity.RolloutGroupPropertyChangeEvent;
import org.eclipse.hawkbit.repository.eventbus.event.entity.RolloutPropertyChangeEvent;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Collects and merges fine grained events to more generic events and push them
 * in a fixed delay on the events bus. This helps for code which are not
 * interested in all fine grained events, e.g. UI code. The UI code is not
 * interested in handling the flood of events, so collecting the events and
 * merge them to one event together and post them in a fixed interval is easier
 * to consume e.g. for push notifications on UI.
 * 
 * 
 * TODO:
 */
@Service
public class EventMerger {

    private static final Set<RolloutEventKey> rolloutEvents = ConcurrentHashMap.newKeySet();
    private static final Set<RolloutEventKey> rolloutGroupEvents = ConcurrentHashMap.newKeySet();

    /**
     * Checks if there are events to publish in the fixed interval.
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 2000)
    public void rolloutEventScheduler() {
        final Iterator<RolloutEventKey> rolloutIterator = rolloutEvents.iterator();
        while (rolloutIterator.hasNext()) {
            final RolloutEventKey eventKey = rolloutIterator.next();
            // eventBus.post(new RolloutChangeEvent(1, eventKey.tenant,
            // eventKey.rolloutId));
            rolloutIterator.remove();
        }

        final Iterator<RolloutEventKey> rolloutGroupIterator = rolloutGroupEvents.iterator();
        while (rolloutGroupIterator.hasNext()) {
            final RolloutEventKey eventKey = rolloutGroupIterator.next();
            // eventBus.post(new RolloutGroupChangeEvent(1, eventKey.tenant,
            // eventKey.rolloutId, eventKey.rolloutGroupId));
            rolloutGroupIterator.remove();
        }
    }

    /**
     * Called by the event bus to retrieve all necessary events to collect and
     * merge.
     * 
     * @param event
     *            the event on the event bus
     */
    @EventListener(classes = Event.class)
    public void onEvent(final Event event) {
        Long rolloutId = null;
        Long rolloutGroupId = null;
        if (event instanceof ActionCreatedEvent) {
            rolloutId = getRolloutId(((ActionCreatedEvent) event).getEntity().getRollout());
            rolloutGroupId = getRolloutGroupId(((ActionCreatedEvent) event).getEntity().getRolloutGroup());
        } else if (event instanceof ActionPropertyChangeEvent) {
            rolloutId = getRolloutId(((ActionPropertyChangeEvent) event).getEntity().getRollout());
            rolloutGroupId = getRolloutGroupId(((ActionPropertyChangeEvent) event).getEntity().getRolloutGroup());
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

        if (rolloutId != null) {
            rolloutEvents.add(new RolloutEventKey(rolloutId, event.getTenant()));
            if (rolloutGroupId != null) {
                rolloutGroupEvents.add(new RolloutEventKey(rolloutId, rolloutGroupId, event.getTenant()));
            }
        }
    }

    private Long getRolloutGroupId(final RolloutGroup rolloutGroup) {
        if (rolloutGroup != null) {
            return rolloutGroup.getId();
        }
        return null;
    }

    private Long getRolloutId(final Rollout rollout) {
        if (rollout != null) {
            return rollout.getId();
        }
        return null;
    }

    /**
     * The rollout key in the concurrent set to be hold.
     * 
     * @author Michael Hirsch
     *
     */
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

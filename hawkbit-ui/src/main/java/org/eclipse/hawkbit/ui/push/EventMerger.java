/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push;

import org.springframework.stereotype.Service;

/**
 * Collects and merges fine grained events to more generic events and push them
 * in a fixed delay on the events bus. This helps for code which are not
 * interested in all fine grained events, e.g. UI code. The UI code is not
 * interested in handling the flood of events, so collecting the events and
 * merge them to one event together and post them in a fixed interval is easier
 * to consume e.g. for push notifications on UI.
 * 
 */
@Service
public class EventMerger {

    // @Autowired
    // private ApplicationEventPublisher applicationEventPublisher;

    // /**
    // * Checks if there are events to publish in the fixed interval.
    // */
    // @Scheduled(initialDelay = 10000, fixedDelay = 2000)
    // public void rolloutEventScheduler() {
    // final Iterator<RolloutEventKey> rolloutIterator =
    // rolloutEvents.iterator();
    // while (rolloutIterator.hasNext()) {
    // final RolloutEventKey eventKey = rolloutIterator.next();
    // applicationEventPublisher.publishEvent(new
    // RolloutChangeEvent(eventKey.tenant, eventKey.rolloutId));
    // rolloutIterator.remove();
    // }
    //
    // final Iterator<RolloutEventKey> rolloutGroupIterator =
    // rolloutGroupEvents.iterator();
    // while (rolloutGroupIterator.hasNext()) {
    // final RolloutEventKey eventKey = rolloutGroupIterator.next();
    // applicationEventPublisher.publishEvent(
    // new RolloutGroupChangeEvent(eventKey.tenant, eventKey.rolloutId,
    // eventKey.rolloutGroupId));
    // rolloutGroupIterator.remove();
    // }
    // }
}

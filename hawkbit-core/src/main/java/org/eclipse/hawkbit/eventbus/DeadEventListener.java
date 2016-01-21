/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;

/**
 * Catches all dead events by means of events with no fitting subscriber on the
 * bus.
 *
 *
 *
 */
@EventSubscriber
public class DeadEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(DeadEventListener.class);

    /**
     * Listens for dead vents and prints them into LOG.
     *
     * @param event
     *            to print
     */
    @Subscribe
    public void listen(final DeadEvent event) {
        LOG.info("DeadEvent on bus! {}", event.getEvent());
    }
}

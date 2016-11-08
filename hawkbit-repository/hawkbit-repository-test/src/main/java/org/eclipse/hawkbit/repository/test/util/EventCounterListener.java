/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Event counter listener to count events and save the type and count in a map.
 */
public class EventCounterListener implements ApplicationListener<RemoteApplicationEvent> {

    private final ConcurrentMap<Class<?>, AtomicInteger> map;

    public EventCounterListener() {
        map = new ConcurrentHashMap<Class<?>, AtomicInteger>();
    }

    public ConcurrentMap<Class<?>, AtomicInteger> getEventCounterMap() {
        return map;
    }

    @Override
    public void onApplicationEvent(final RemoteApplicationEvent event) {

        System.err.println("Event received " + event);

        map.putIfAbsent(event.getClass(), new AtomicInteger());
        map.get(event.getClass()).incrementAndGet();
    }

}

package org.eclipse.hawkbit.repository.jpa.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationListener;

public class EventCounterListener implements ApplicationListener<RemoteApplicationEvent> {

    final ConcurrentMap<Class<?>, AtomicInteger> map;

    public EventCounterListener() {
        map = new ConcurrentHashMap<Class<?>, AtomicInteger>();
    }

    public ConcurrentMap<Class<?>, AtomicInteger> getEventCounterMap() {
        return map;
    }

    @Override
    public void onApplicationEvent(final RemoteApplicationEvent event) {
        map.putIfAbsent(event.getClass(), new AtomicInteger(0));
        map.get(event.getClass()).incrementAndGet();
    }

}

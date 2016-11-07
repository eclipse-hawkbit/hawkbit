package org.eclipse.hawkbit.repository.jpa.utils;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.hawkbit.repository.test.util.TestApplicationContextProvider;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;

public class EventVerifier implements TestRule {

    EventCounterListener eventCounterListener;

    @Override
    public Statement apply(final Statement rootStatement, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                beforeTest(description);
                rootStatement.evaluate();
                afterTest(description);
            }
        };
    }

    private void beforeTest(final Description description) throws InstantiationException, IllegalAccessException {

        if (hasEventCountAnnotation(description)) {
            final ExpectEvent[] expectEvent = getExpectEvent(description);
            if (expectEvent.length > 0) {
                final ConfigurableApplicationContext context = TestApplicationContextProvider.getContext();
                eventCounterListener = new EventCounterListener();
                context.addApplicationListener(eventCounterListener);
            }
        }
    }

    private void afterTest(final Description description) throws InterruptedException {

        if (hasEventCountAnnotation(description)) {
            final ExpectEvent[] expectEvent = getExpectEvent(description);
            if (expectEvent.length > 0) {
                final ConcurrentMap<Class<?>, AtomicInteger> eventCounterMap = eventCounterListener
                        .getEventCounterMap();
                for (final ExpectEvent e : expectEvent) {
                    eventCounterMap.putIfAbsent(e.type(), new AtomicInteger(0));
                    final AtomicInteger atomicInteger = eventCounterMap.get(e.type());
                    assertThat(atomicInteger.get()).as("Did not receive the expected amount of events form " + e.type())
                            .isEqualTo(e.count());
                }
            }
        }
        removeEventCounterListener();
    }

    private boolean hasEventCountAnnotation(final Description description) {
        return description.getAnnotation(EventCounter.class) != null;
    }

    private void removeEventCounterListener() {
        if (eventCounterListener == null) {
            return;
        }
        final ConfigurableApplicationContext context = TestApplicationContextProvider.getContext();
        final ApplicationEventMulticaster applicationEventMulticaster = context
                .getBean(ApplicationEventMulticaster.class);
        applicationEventMulticaster.removeApplicationListener(eventCounterListener);
    }

    private ExpectEvent[] getExpectEvent(final Description description) {
        final EventCounter annotation = description.getAnnotation(EventCounter.class);
        return annotation.expectEvent();
    }

}

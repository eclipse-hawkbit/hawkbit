/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.repository.test.matcher;

import static org.hamcrest.Matchers.equalTo;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.test.util.TestContextProvider;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionTimeoutException;

/**
 * Test rule to setup and verify the event count for a method.
 */
public class EventVerifier implements TestRule {

    private EventCaptor eventCaptor;

    @Override
    public Statement apply(final Statement test, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                final Optional<Expect[]> expectedEvents = getExpectationsFrom(description);
                expectedEvents.ifPresent(events -> beforeTest());
                try {
                    test.evaluate();
                    expectedEvents.ifPresent(events -> afterTest(events));
                } finally {
                    expectedEvents.ifPresent(listener -> removeEventListener());
                }
            }
        };
    }

    private Optional<Expect[]> getExpectationsFrom(final Description description) {
        return Optional.ofNullable(description.getAnnotation(ExpectEvents.class)).map(ExpectEvents::value);
    }

    private void beforeTest() {
        final ConfigurableApplicationContext context = TestContextProvider.getContext();
        eventCaptor = new EventCaptor();
        context.addApplicationListener(eventCaptor);
    }

    private void afterTest(final Expect[] expectedEvents) {
        verifyRightCountOfEvents(expectedEvents);
        verifyAllEventsCounted(expectedEvents);
    }

    private void verifyRightCountOfEvents(final Expect[] expectedEvents) {

        for (final Expect expectedEvent : expectedEvents) {
            try {
                Awaitility.await().atMost(5, TimeUnit.SECONDS)
                        .until(() -> eventCaptor.getCountFor(expectedEvent.type()), equalTo(expectedEvent.count()));

            } catch (final ConditionTimeoutException ex) {
                Assert.fail("Did not receive the expected amount of events form " + expectedEvent.type() + " Expected: "
                        + expectedEvent.count() + " but was: " + eventCaptor.getCountFor(expectedEvent.type()));
            }
        }
    }

    private void verifyAllEventsCounted(final Expect[] expectedEvents) {

        final Set<Class<?>> diffSet = eventCaptor.diff(expectedEvents);
        if (diffSet.size() > 0) {
            final StringBuilder failMessage = new StringBuilder("Missing event verification for ");
            final Iterator<Class<?>> itr = diffSet.iterator();
            while (itr.hasNext()) {
                final Class<?> element = itr.next();
                final int count = eventCaptor.getCountFor(element);
                failMessage.append(element + " with count: " + count + " ");
            }
            Assert.fail(failMessage.toString());
        }

    }

    private void removeEventListener() {
        final ApplicationEventMulticaster multicaster = TestContextProvider.getContext()
                .getBean(ApplicationEventMulticaster.class);
        multicaster.removeApplicationListener(eventCaptor);
    }

    private static class EventCaptor implements ApplicationListener<RemoteApplicationEvent> {

        private final Multiset<Class<?>> capturedEvents = HashMultiset.create();

        @Override
        public synchronized void onApplicationEvent(final RemoteApplicationEvent event) {
            capturedEvents.add(event.getClass());
        }

        public synchronized int getCountFor(final Class<?> expectedEvent) {
            return capturedEvents.count(expectedEvent);
        }

        public synchronized Set<Class<?>> diff(final Expect[] allEvents) {
            return Sets.difference(capturedEvents.elementSet(),
                    java.util.stream.Stream.of(allEvents).map((e) -> e.type()).collect(Collectors.toSet()));
        }

    }

}

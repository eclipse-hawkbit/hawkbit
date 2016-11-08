/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.matcher;

import static java.util.Optional.ofNullable;
import static org.fest.assertions.api.Assertions.assertThat;

import java.util.Optional;

import org.eclipse.hawkbit.repository.test.util.TestContextProvider;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Test rule to setup and to verify the event count for a method.
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
                test.evaluate();
                expectedEvents.ifPresent(events -> afterTest(events));
            }
        };
    }

    private Optional<Expect[]> getExpectationsFrom(final Description description) {
        return ofNullable(description.getAnnotation(Count.class)).map(Count::events);
    }

    private void beforeTest() {
        final ConfigurableApplicationContext context = TestContextProvider.getContext();
        eventCaptor = new EventCaptor();
        context.addApplicationListener(eventCaptor);
    }

    private void afterTest(final Expect[] expectedEvents) {
        removeEventListener();
        verifyAmountOfEvents(expectedEvents);
    }

    private void verifyAmountOfEvents(final Expect[] expectedEvents) {

        for (final Expect expectedEvent : expectedEvents) {
            final int count = eventCaptor.getCountFor(expectedEvent);
            assertThat(count).as("Did not receive the expected amount of events form " + expectedEvent.type())
                    .isEqualTo(expectedEvent.count());
        }
    }

    private void removeEventListener() {
        final ApplicationEventMulticaster multicaster = TestContextProvider.getContext()
                .getBean(ApplicationEventMulticaster.class);
        multicaster.removeApplicationListener(eventCaptor);
    }

    private static class EventCaptor implements ApplicationListener<RemoteApplicationEvent> {

        public final Multiset<Class<?>> capturedEvents = HashMultiset.create();

        @Override
        public void onApplicationEvent(final RemoteApplicationEvent event) {
            capturedEvents.add(event.getClass());
        }

        public int getCountFor(final Expect expectedEvent) {
            return capturedEvents.count(expectedEvent.type());
        }
    }
}

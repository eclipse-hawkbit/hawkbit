/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.repository.test.matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serial;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEvent;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.RemoteIdEvent;
import org.eclipse.hawkbit.repository.event.remote.RemoteTenantAwareEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.service.ActionCreatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.ActionUpdatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.CancelTargetAssignmentServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetAssignDistributionSetServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetAttributesRequestedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetCreatedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetDeletedServiceEvent;
import org.eclipse.hawkbit.repository.event.remote.service.TargetUpdatedServiceEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Test rule to set up and verify the event count for a method.
 */
@Slf4j
public class EventVerifier extends AbstractTestExecutionListener {

    private EventCaptor eventCaptor;

    /**
     * Publishes a reset counter marker event on the context to reset the current counted events. This allows test to prepare a setup such in
     * {@code @Before} annotations which are actually counted to the executed test-method and maybe fire events which are not
     * covered / recognized by the test-method itself and reset the counter again.
     * <p/>
     * Note that this approach is only working when using a single-thread executor in the ApplicationEventMultiCaster, so the order of the
     * events keep the same.
     *
     * @param publisher the {@link ApplicationEventPublisher} to publish the marker event to
     */
    public static void publishResetMarkerEvent(final ApplicationEventPublisher publisher) {
        publisher.publishEvent(new ResetCounterMarkerEvent());
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        Optional.ofNullable(testContext.getTestMethod().getAnnotation(ExpectEvents.class))
                .map(ExpectEvents::value)
                .ifPresent(events -> {
                    eventCaptor = new EventCaptor();
                    ((ConfigurableApplicationContext) testContext.getApplicationContext()).addApplicationListener(eventCaptor);
                });
    }

    @Override
    public void afterTestMethod(final TestContext testContext) {
        if (testContext.getTestException() != null) {
            // test has failed anyway
            // not expected event set could be result of failed test / incomplete steps - no need to check and mess up with real exception
            return;
        }

        getExpectationsFrom(testContext.getTestMethod()).ifPresent(expectedEvents -> {
            try {
                verifyRightCountOfEvents(expectedEvents);
                verifyAllEventsCounted(expectedEvents);
                log.info("Expected events received:\n\t{}", Arrays.stream(expectedEvents).map(Object::toString).collect(Collectors.joining("\n\t")));
            } finally {
                testContext.getApplicationContext().getBean(ApplicationEventMulticaster.class).removeApplicationListener(eventCaptor);
                DYNAMIC_EXPECTATIONS.remove();
            }
        });
    }

    private void verifyRightCountOfEvents(final Expect[] expectedEvents) {
        for (final Expect expectedEvent : expectedEvents) {
            try {
                Awaitility.await()
                        .atMost(5, TimeUnit.SECONDS)
                        .until(() -> eventCaptor.getCountFor(expectedEvent.type()), equalTo(expectedEvent.count()));
            } catch (final ConditionTimeoutException ex) {
                fail(String.format(
                        "Did not receive the expected amount of events form %s Expected: %d but was: %d",
                        expectedEvent.type(), expectedEvent.count(), eventCaptor.getCountFor(expectedEvent.type())));
            }
        }
    }

    private void verifyAllEventsCounted(final Expect[] expectedEvents) {
        final Set<Class<?>> diffSet = eventCaptor.diff(expectedEvents);
        if (!diffSet.isEmpty()) {
            final StringBuilder failMessage = new StringBuilder("Missing event expectation for ");
            for (final Class<?> element : diffSet) {
                final int count = eventCaptor.getCountFor(element);
                failMessage.append(element).append(" with count: ").append(count).append(" ");
            }
            fail(failMessage.toString());
        }
    }

    public static final ThreadLocal<Supplier<Expect[]>> DYNAMIC_EXPECTATIONS = new ThreadLocal<>();

    private Optional<Expect[]> getExpectationsFrom(final Method testMethod) {
        return Optional.ofNullable(testMethod.getAnnotation(ExpectEvents.class))
                .map(ExpectEvents::value)
                .map(expectedEvents -> {
                    final Supplier<Expect[]> supplier = DYNAMIC_EXPECTATIONS.get();
                    if (expectedEvents.length == 0) {
                        if (supplier != null) {
                            return supplier.get();
                        }
                    } else {
                        if (supplier != null && supplier.get().length > 0) {
                            fail("Expectations defined - both static and dynamic - only one definition is allowed");
                        }
                    }
                    return expectedEvents;
                })
                .map(expectedEvents -> {
                    final List<Expect> modifiedEvents = new ArrayList<>(Arrays.asList(expectedEvents));
                    for (final Expect event : expectedEvents) {
                        addServiceEventIfNeeded(event, modifiedEvents);
                    }
                    return modifiedEvents.toArray(new Expect[0]);
                });
    }

    private static void addServiceEventIfNeeded(final Expect event, final List<Expect> modifiedEvents) {
        final Class<?> type = event.type();
        if (type.isAssignableFrom(TargetCreatedEvent.class)) {
            modifiedEvents.add(new DynamicExpect(TargetCreatedServiceEvent.class, event.count()));
        } else if (type.isAssignableFrom(TargetUpdatedEvent.class)) {
            modifiedEvents.add(new DynamicExpect(TargetUpdatedServiceEvent.class, event.count()));
        } else if (type.isAssignableFrom(TargetDeletedEvent.class)) {
            modifiedEvents.add(new DynamicExpect(TargetDeletedServiceEvent.class, event.count()));
        } else if (type.isAssignableFrom(TargetAssignDistributionSetEvent.class)) {
            modifiedEvents.add(new DynamicExpect(TargetAssignDistributionSetServiceEvent.class, event.count()));
        } else if (type.isAssignableFrom(TargetAttributesRequestedEvent.class)) {
            modifiedEvents.add(new DynamicExpect(TargetAttributesRequestedServiceEvent.class, event.count()));
        } else if (type.isAssignableFrom(CancelTargetAssignmentEvent.class)) {
            modifiedEvents.add(new DynamicExpect(CancelTargetAssignmentServiceEvent.class, event.count()));
        } else if (type.isAssignableFrom(ActionCreatedEvent.class)) {
            modifiedEvents.add(new DynamicExpect(ActionCreatedServiceEvent.class, event.count()));
        } else if (type.isAssignableFrom(ActionUpdatedEvent.class)) {
            modifiedEvents.add(new DynamicExpect(ActionUpdatedServiceEvent.class, event.count()));
        }
    }

    public record DynamicExpect(Class<?> type, int count) implements Expect {

        @Override
        public Class<? extends Annotation> annotationType() {
            return Expect.class;
        }
    }

    private static final class EventCaptor implements ApplicationListener<AbstractRemoteEvent> {

        private final ConcurrentHashMap<Class<?>, Integer> capturedEvents = new ConcurrentHashMap<>();

        @Override
        public void onApplicationEvent(final AbstractRemoteEvent event) {
            log.debug("Received event {}", event.getClass().getSimpleName());

            if (ResetCounterMarkerEvent.class.isAssignableFrom(event.getClass())) {
                log.debug("Retrieving reset counter marker event - resetting counters");
                capturedEvents.clear();
                return;
            }

            if (event instanceof RemoteTenantAwareEvent remoteTenantAwareEvent) {
                assertThat(remoteTenantAwareEvent.getTenant()).isNotEmpty();
            }

            if (event instanceof RemoteIdEvent remoteIdEvent) {
                assertThat(remoteIdEvent.getEntityId()).isNotNull();
            }

            if (event instanceof TargetAssignDistributionSetEvent targetAssignDistributionSetEvent) {
                assertThat(targetAssignDistributionSetEvent.getActions()).isNotEmpty();
            }

            capturedEvents.compute(event.getClass(), (k, v) -> v == null ? 1 : v + 1);
        }

        public int getCountFor(final Class<?> expectedEvent) {
            return Optional.ofNullable(capturedEvents.get(expectedEvent)).orElse(0);
        }

        public Set<Class<?>> diff(final Expect[] allEvents) {
            final Set<Class<?>> keys = new HashSet<>(capturedEvents.keySet());
            keys.removeAll(Stream.of(allEvents).map(Expect::type).collect(Collectors.toSet()));
            return keys;
        }
    }

    private static final class ResetCounterMarkerEvent extends AbstractRemoteEvent {

        @Serial
        private static final long serialVersionUID = 1L;

        private ResetCounterMarkerEvent() {
            super("event-verifier");
        }
    }
}
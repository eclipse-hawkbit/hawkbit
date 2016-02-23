/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Cluster Event Bus")
@Stories("EventBus Subscriber Processor Test")
@RunWith(MockitoJUnitRunner.class)
// TODO: create description annotations
public class EventBusSubscriberProcessorTest {

    @Mock
    private EventBus eventBusMock;

    private final EventBusSubscriberProcessor postProcessorUnderTest = new EventBusSubscriberProcessor();

    @Before
    public void before() {
        reset(eventBusMock);
        postProcessorUnderTest.setEventBus(eventBusMock);
    }

    @Test
    public void correctAnnotatedClassAndMethodIsRegistered() {
        final TestEventSubscriberClass testEventSubscriberClass = new TestEventSubscriberClass();
        postProcessorUnderTest.postProcessAfterInitialization(testEventSubscriberClass, "correctEventSubscriber");
        verify(eventBusMock, times(1)).register(testEventSubscriberClass);
    }

    @Test
    public void eventSubscriberWithoutMethodAnnotationIsNotRegistered() {
        final TestWrongEventSubscriberClass testEventSubscriberClass = new TestWrongEventSubscriberClass();
        postProcessorUnderTest.postProcessAfterInitialization(testEventSubscriberClass, "correctEventSubscriber");
        verify(eventBusMock, times(0)).register(testEventSubscriberClass);
    }

    @EventSubscriber
    private class TestEventSubscriberClass {
        @Subscribe
        public void subscribe(final String s) {

        }
    }

    @EventSubscriber
    private class TestWrongEventSubscriberClass {
        public void methodWithoutAnnotation(final String s) {

        }
    }
}

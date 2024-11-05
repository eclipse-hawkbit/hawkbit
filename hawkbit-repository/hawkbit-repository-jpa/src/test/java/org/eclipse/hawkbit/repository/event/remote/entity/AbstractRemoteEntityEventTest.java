/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEventTest;

/**
 * Test the remote entity events.
 */
@Feature("Component Tests - Repository")
@Story("Entity Events")
public abstract class AbstractRemoteEntityEventTest<E> extends AbstractRemoteEventTest {

    protected RemoteEntityEvent<?> assertAndCreateRemoteEvent(final Class<? extends RemoteEntityEvent<?>> eventType) {
        final E baseEntity = createEntity();
        final RemoteEntityEvent<?> event = createRemoteEvent(baseEntity, eventType);
        assertEntity(baseEntity, event);
        return event;
    }

    protected RemoteEntityEvent<?> createRemoteEvent(final E baseEntity,
            final Class<? extends RemoteEntityEvent<?>> eventType) {

        final int constructorParamCount = getConstructorParamCount();
        final Constructor<?> eventConstructor = findEventConstructorByParamCount(eventType, constructorParamCount);
        final Object[] eventConstructorParams = getConstructorParams(baseEntity);
        try {
            return (RemoteEntityEvent<?>) eventConstructor.newInstance(eventConstructorParams);
        } catch (final ReflectiveOperationException e) {
            fail("Exception should not happen " + e.getMessage());
        }
        return null;
    }

    protected int getConstructorParamCount() {
        return 2;
    }

    protected Constructor<?> findEventConstructorByParamCount(final Class<? extends RemoteEntityEvent<?>> eventType,
            final int paramCount) {
        return Arrays.stream(eventType.getDeclaredConstructors())
                .filter(constructor -> constructor.getParameterCount() == paramCount).findAny()
                .orElseThrow(() -> new IllegalArgumentException("No suitable constructor founded"));
    }

    protected Object[] getConstructorParams(final E baseEntity) {
        return new Object[] { baseEntity, "Node" };
    }

    protected RemoteEntityEvent<?> assertEntity(final E baseEntity, final RemoteEntityEvent<?> event) {
        assertThat(event.getEntity()).isPresent().get().isSameAs(baseEntity);

        RemoteEntityEvent<?> underTestCreatedEvent = createProtoStuffEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).isPresent().get().isEqualTo(baseEntity);

        underTestCreatedEvent = createJacksonEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).isPresent().get().isEqualTo(baseEntity);
        return underTestCreatedEvent;
    }

    protected abstract E createEntity();
}

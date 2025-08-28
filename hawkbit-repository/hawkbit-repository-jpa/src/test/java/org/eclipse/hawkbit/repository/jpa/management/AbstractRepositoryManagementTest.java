/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.test.matcher.EventVerifier;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.utils.ObjectCopyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.util.ObjectUtils;

@Slf4j
public abstract class AbstractRepositoryManagementTest<T extends BaseEntity, C, U extends Identifiable<Long>>
        extends AbstractJpaIntegrationTest {

    protected RepositoryManagement<T, C, U> repositoryManagement;

    @Getter(AccessLevel.PROTECTED)
    private Class<T> entityType; // T
    private Class<C> createType; // C
    private Class<U> updateType; // U
    private Function<Class<?>, ?> entityFactory;

    @BeforeEach
    void setup() {
        synchronized (AbstractRepositoryManagementTest.class) {
            if (repositoryManagement == null) {
                final RepoMan<T, C, U> repoMan = resolveRepositoryManagement();
                repositoryManagement = repoMan.repo;
                entityType = repoMan.entityType();
                createType = repoMan.createType();
                updateType = repoMan.updateType();
                entityFactory = repoMan.entityFactory;
            }
        }

        expectedEvents.clear();
        EventVerifier.DYNAMIC_EXPECTATIONS.set(() -> expectedEvents.entrySet().stream()
                .map(e -> new EventVerifier.DynamicExpect(e.getKey(), e.getValue().get()))
                .toList()
                .toArray(new Expect[0]));
    }

    // also test get/find(Long)
    @ExpectEvents
    @Test
    void create() {
        final T instance = instance();
        assertEquals(repositoryManagement.get(instance.getId()), instance);
        assertThat(repositoryManagement.find(instance.getId()))
                .isPresent()
                .hasValueSatisfying(get -> assertEquals(get, instance));
    }

    // also test get/find(Collection<Long>)
    @ExpectEvents
    @Test
    void creates() {
        final List<T> instances = instances();
        final List<Long> instanceIds = instances.stream().map(Identifiable::getId).toList();
        assertEquals(repositoryManagement.get(instanceIds), instances);
        assertThat(repositoryManagement.find(instanceIds)).hasSize(instances.size());
    }

    @Test
    void findCountAll() {
        // some entities as distribution set types has pre-initialized entities
        final int alreadyExisting = repositoryManagement.findAll(UNPAGED).getContent().size();
        final int count = 5;
        final List<T> instances = instances(count); // create 'count' instances
        assertThat(repositoryManagement.findAll(UNPAGED).getContent()).as("Wrong size of all entities").hasSize(count + alreadyExisting);
        assertThat(repositoryManagement.count()).as("Wrong size of all entities").isEqualTo(count + alreadyExisting);

        repositoryManagement.delete(instances.get(0).getId());
        assertThat(repositoryManagement.findAll(UNPAGED).getContent()).as("Wrong size of all entities").hasSize(count + alreadyExisting - 1);
        assertThat(repositoryManagement.count()).as("Wrong size of all entities").isEqualTo(count + alreadyExisting - 1);
    }

    @ExpectEvents
    @Test
    void update() {
        final T instance = instance();

        final U update = forBuildableTypeRe(updateType, instance.getId());
        incrementEvents(entityType, EventType.UPDATED);
        final T instanceUpdated = repositoryManagement.update(update);
        assertNotNull(instanceUpdated);
        assertEquals(instanceUpdated, update);

        final T get = repositoryManagement.get(instance.getId());
        assertEquals(get, update);
        assertEquals(get, ObjectCopyUtil.copy(update, instance, false, UnaryOperator.identity()));
    }

    /**
     * Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.
     */
    @SneakyThrows
    @Test
    void updateNothingDontChangeRepository() {
        final T instance = instance();

        final U emptyUpdate = forBuildableType(updateType, instance.getId(), (name, type) -> {
            try {
                try {
                    final Method getter = entityType.getMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
                    if (getter.getReturnType() == type) {
                        return getter.invoke(instance);
                    }
                } catch (final NoSuchMethodException e) {
                    if (type == boolean.class || type == Boolean.class) {
                        final Method getter = entityType.getMethod("is" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
                        if (getter.getReturnType() == boolean.class || getter.getReturnType() == Boolean.class) {
                            return getter.invoke(instance);
                        }
                    }
                }
            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e2) {
                log.debug("Could not invoke getter for {} in {}", name, instance, e2);
            }
            return null;
        });

        final T updated = repositoryManagement.update(emptyUpdate);
        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entity to be equal to created version")
                .isEqualTo(instance.getOptLockRevision());
    }

    @ExpectEvents
    @Test
    void delete() {
        final Long instanceId = instance().getId();

        incrementEvents(entityType, EventType.DELETED);
        repositoryManagement.delete(instanceId);
        assertThat(repositoryManagement.find(instanceId)).isEmpty();
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> repositoryManagement.get(instanceId));
    }

    @ExpectEvents
    @Test
    void deletes() {
        final List<Long> instanceIds = instances().stream().map(Identifiable::getId).toList();
        final int count = instanceIds.size();
        assertThat(repositoryManagement.get(instanceIds)).hasSize(count);
        assertThat(repositoryManagement.find(instanceIds)).hasSize(count);

        incrementEvents(entityType, EventType.DELETED, count);
        repositoryManagement.delete(instanceIds);
        for (final Long instanceId : instanceIds) {
            assertThat(repositoryManagement.find(instanceId)).isEmpty();
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> repositoryManagement.get(instanceId));
        }
    }

    @Test
    void failToReadUpdateDeleteNotExisting() {
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> repositoryManagement.get(NOT_EXIST_IDL));
        assertThat(repositoryManagement.find(-1L)).isEmpty();
        final U notExistUpdate = forBuildableTypeRe(updateType, NOT_EXIST_IDL);
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> repositoryManagement.update(notExistUpdate));
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> repositoryManagement.delete(NOT_EXIST_IDL));
    }

    @Test
    void failToDuplicate() {
        final C create = forType(createType);
        incrementEvents(entityType, EventType.CREATED);
        repositoryManagement.create(create);
        assertThatExceptionOfType(EntityAlreadyExistsException.class).isThrownBy(() -> repositoryManagement.create(create));
    }

    protected T instance() {
        final C create = forType(createType);
        incrementEvents(entityType, EventType.CREATED);
        final T instance = repositoryManagement.create(create);

        assertNotNull(instance);
        assertEquals(instance, create);
        final Long instanceId = instance.getId();
        assertNotNull(repositoryManagement.get(instanceId));
        assertThat(repositoryManagement.find(instanceId)).isPresent();

        return instance;
    }

    protected List<T> instances() {
        return instances(1 + RND.nextInt(5));
    }

    protected List<T> instances(final int count) {
        final List<C> creates = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            creates.add(forType(createType));
        }
        incrementEvents(entityType, EventType.CREATED, count);
        final List<T> instances = repositoryManagement.create(creates);

        assertNotNull(instances);
        assertThat(instances).hasSize(count);
        assertEquals(instances, creates);

        return instances;
    }

    // asserts that all expected's fields (getters) are equal to the actual's fields
    // does a deep compare for fields which are not primitive or String
    protected void assertEquals(final Object actual, final Object expected) {
        final Deque<String> stack = new ArrayDeque<>();
        try {
            assertEquals(actual, expected, stack);
        } catch (final AssertionError e) {
            throw new AssertionError(
                    String.format(
                            "Comparison failed at path: %s%n\tActual: %s,%n\tExpected: %s",
                            String.join("->", stack), actual, expected),
                    e);
        }
    }

    private final Map<Class<?>, AtomicInteger> expectedEvents = new ConcurrentHashMap<>();

    protected void incrementEvents(final Class<?> entityType, final EventType eventType) {
        incrementEvents(entityType, eventType, 1);
    }

    protected void incrementEvents(final Class<?> entityType, final EventType eventType, final int count) {
        expectedEvents.computeIfAbsent(eventType.getEventType(entityType), k -> new AtomicInteger(0)).addAndGet(count);
    }

    protected static Type[] genericTypes(final Class<?> clazz, final Class<?> targetSuperClass) {
        return findGenericSuperType(ResolvableType.forType(clazz), targetSuperClass)
                .map(ResolvableType::resolveGenerics)
                .orElseThrow(() ->
                        new IllegalStateException("No generic type found for " + targetSuperClass.getName() + " in " + clazz.getName()));
    }

    protected final AtomicLong counter = new AtomicLong();

    @SuppressWarnings("unchecked")
    protected <O> O forType(final Class<O> type) {
        if (type == boolean.class || type == Boolean.class) {
            return (O) (counter.incrementAndGet() % 2 == 0 ? Boolean.TRUE : Boolean.FALSE);
        } else if (type == Integer.class || type == int.class) {
            return (O) Integer.valueOf((int) counter.incrementAndGet());
        } else if (type == Long.class || type == long.class) {
            return (O) Long.valueOf(counter.incrementAndGet());
        } else if (type == Float.class || type == float.class) {
            return (O) Float.valueOf(counter.incrementAndGet());
        } else if (type == Double.class || type == double.class) {
            return (O) Double.valueOf(counter.incrementAndGet());
        } else if (type == String.class) {
            return (O) ("test-" + counter.incrementAndGet());
        } else if (type == Set.class) {
            final Set<?> set = new HashSet<>();
//            set.add(forType(createType));
            return (O) set;
        } else if (type.isEnum()) {
            final O[] constants = type.getEnumConstants();
            return constants[(int) (counter.incrementAndGet() % constants.length)];
        } else {
            // entity classes for created events are root level classes
            if ("org.eclipse.hawkbit.repository.model".equals(type.getPackageName()) && BaseEntity.class.isAssignableFrom(type)) {
                try {
                    incrementEvents(type, EventType.CREATED);
                    return (O) entityFactory.apply(type);
                } catch (final Exception e) {
                    log.error("Could not create instance of {} using entity factory", type.getName(), e);
                }
            }
            try { // try with constructor
                final Constructor<?>[] constructors = type.getConstructors();
                if (ObjectUtils.isEmpty(constructors)) {
                    throw new NoSuchMethodException("No public constructor found for " + type);
                }
                // prefer empty constructor
                for (final Constructor<?> constructor : constructors) {
                    if (constructor.getParameterCount() == 0) {
                        return (O) constructor.newInstance();
                    }
                }
                constructors[0].setAccessible(true);
                return (O) constructors[0].newInstance(Stream.of(constructors[0].getParameterTypes())
                        .map(this::forType)
                        .toArray());
            } catch (final ReflectiveOperationException e) { // try with builder
                // try builder pattern
                try {
                    return forBuildableType(type, null);
                } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e1) {
                    log.debug("{} is not a builder. Throws could not instantiate", type.getName());
                }
                log.error("Could not instantiate {}", type.getName(), e);
                throw new IllegalStateException("Could not create instance of " + createType.getName(), e);
            }
        }
    }

    protected Object builderParameterValue(final Method builderSetter) {
        return forType(builderSetter.getParameterTypes()[0]);
    }

    private <O> O forBuildableTypeRe(final Class<O> type, final Long id) {
        try {
            return forBuildableType(type, id);
        } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.debug("{} is not a buildable type", type.getName());
            throw new IllegalStateException("Could not create instance of " + createType.getName(), e);
        }
    }

    private <O> O forBuildableType(final Class<O> type, final Long id)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return forBuildableType(type, id, null);
    }

    @SuppressWarnings("unchecked")
    private <O> O forBuildableType(final Class<O> type, final Long id, final BiFunction<String, Class<?>, Object> propertyValueSupplier)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final Object builder = type.getMethod("builder").invoke(null);
        Arrays.stream(builder.getClass().getMethods())
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> method.getDeclaringClass() != Object.class)
                .forEach(method -> {
                    try {
                        if (id != null && "id".equals(method.getName()) && method.getParameterTypes()[0] == Long.class) {
                            method.invoke(builder, id);
                        } else {
                            if (propertyValueSupplier == null) {
                                method.invoke(builder, builderParameterValue(method));
                            } else {
                                final Object propertyValue = propertyValueSupplier.apply(method.getName(), method.getParameterTypes()[0]);
                                if (propertyValue != null) {
                                    method.invoke(builder, propertyValue);
                                }
                            }
                        }
                    } catch (final IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    } catch (final InvocationTargetException ex) {
                        throw new RuntimeException(ex.getTargetException() == null ? ex : ex.getTargetException());
                    }
                });
        final Method build = builder.getClass().getDeclaredMethod("build");
        build.setAccessible(true);
        return (O) build.invoke(builder);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private RepoMan<T, C, U> resolveRepositoryManagement() {
        final Type[] abstractTestTypeArgs = genericTypes(getClass(), AbstractRepositoryManagementTest.class);
        final List<Field> fields = fields(getClass(), new ArrayList<>()).stream()
                .peek(field -> field.setAccessible(true))
                .filter(field -> RepositoryManagement.class.isAssignableFrom(field.getType()))
                .filter(field -> field.getDeclaringClass() != AbstractRepositoryManagementTest.class ||
                        !"repositoryManagement".equals(field.getName()))
                .toList();
        final Map<Class<?>, Supplier<?>> entityCreators = new HashMap<>();
        fields.forEach(field -> {
            try {
                field.setAccessible(true);
                final RepositoryManagement entityManager = (RepositoryManagement) field.get(this);
                final Type[] fieldGenericTypes = genericTypes(field.getType(), RepositoryManagement.class);
                entityCreators.put((Class<?>) fieldGenericTypes[0], () -> entityManager.create(forType((Class<?>) fieldGenericTypes[1])));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not access field " + field.getName(), e);
            }
        });
        return new RepoMan<T, C, U>(
                fields.stream()
                        .peek(field -> System.out.println("Found RepositoryManagement field: " + field.getName() + "-> " + Arrays.toString(
                                genericTypes(field.getType(), RepositoryManagement.class))))
                        .filter(field -> Arrays.equals(genericTypes(field.getType(), RepositoryManagement.class), abstractTestTypeArgs))
                        .map(field -> {
                            field.setAccessible(true);
                            try {
                                return field.get(this);
                            } catch (final IllegalAccessException e) {
                                throw new IllegalStateException("Could not access field " + field.getName(), e);
                            }
                        })
                        .map(RepositoryManagement.class::cast)
                        .findAny()
                        .orElseThrow(() -> new IllegalStateException(
                                "No field matching the generic types found: " + Arrays.toString(abstractTestTypeArgs))),
                (Class<T>) abstractTestTypeArgs[0],
                (Class<C>) abstractTestTypeArgs[1],
                (Class<U>) abstractTestTypeArgs[2],
                entityClass -> {
                    final Supplier<?> entitySupplier = entityCreators.get(entityClass);
                    if (entitySupplier == null) {
                        throw new IllegalStateException("No entity creator found for " + entityClass.getName());
                    }
                    return entitySupplier.get();
                });
    }

    private List<Field> fields(final Class<?> clazz, final List<Field> fields) {
        Collections.addAll(fields, clazz.getDeclaredFields());
        final Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            fields(superClass, fields);
        }
        return fields;
    }

    private static Optional<ResolvableType> findGenericSuperType(final ResolvableType resolvableType, final Class<?> targetSuperClass) {
        if (resolvableType.getRawClass() == targetSuperClass) {
            return Optional.of(resolvableType);
        }
        final Optional<ResolvableType> inInterfaces = Arrays.stream(resolvableType.getInterfaces())
                .filter(superInterface -> superInterface.getRawClass() == targetSuperClass)
                .findAny();
        if (inInterfaces.isPresent()) {
            return inInterfaces;
        } else if (resolvableType.getSuperType() != ResolvableType.NONE) {
            return findGenericSuperType(resolvableType.getSuperType(), targetSuperClass);
        } else {
            return Optional.empty();
        }
    }

    private void assertEquals(final Object actual, final Object expected, final Deque<String> path) {
        if (actual == expected) {
            return; // same reference
        }
        if (expected == null || actual == null) {
            throw new AssertionError("One of the expected and actual is null the other is not");
        }
        if (expected.equals(actual)) {
            return; // equal
        }
        final Class<?> expectedClass = expected.getClass();
        final Class<?> actualClass = actual.getClass();
        if (actualClass == expectedClass) {
            if (actual.equals(expected)) {
                if (actualClass == Boolean.class ||
                        actualClass == Integer.class || actualClass == Long.class ||
                        actualClass == Float.class || actualClass == Double.class ||
                        actualClass == String.class) {
                    return;
                }
            } else if (!(Collection.class.isAssignableFrom(actualClass))) {
                throw new AssertionError("Expected:\n\t" + expected + "but got:\n\t" + actual);
            }
        }
        if (expected instanceof Collection<?> expectedCollection) {
            if (actual instanceof Collection<?> actualCollection) {
                if (actualCollection.size() != expectedCollection.size()) {
                    throw new AssertionError("Expected collection size " + expectedCollection.size() + " but got " + actualCollection.size());
                }
                final AtomicInteger index = new AtomicInteger(0);
                expectedCollection.iterator().forEachRemaining(expectedElement -> {
                    index.incrementAndGet();
                    actualCollection.stream()
                            .filter(actualElement -> {
                                try {
                                    assertEquals(actualElement, expectedElement, path);
                                    return true;
                                } catch (final AssertionError e) {
                                    return false;
                                }
                            })
                            .findFirst()
                            .orElseThrow(
                                    () -> {
                                        path.addLast("[" + index.get() + ", not found]");
                                        return new AssertionError("Expected collection to contain " + expectedElement + " but it does not");
                                    });
                });
                return;
            } else {
                throw new AssertionError("Expected a collection but got " + actual.getClass().getName());
            }
        } else if (expectedClass.isArray()) {
            if (actual.getClass().isArray()) {
                final int expectedLength = Array.getLength(expected);
                final int actualLength = Array.getLength(actual);
                if (expectedLength != actualLength) {
                    throw new AssertionError("Expected array length " + expectedLength + " but got " + actualLength);
                }
                for (int i = 0; i < expectedLength; i++) {
                    final Object expectedElement = Array.get(expected, i);
                    path.addLast("[" + i + ", not found]");
                    assertEquals(Array.get(actual, i), expectedElement, path);
                    path.removeLast();
                }
                return;
            } else {
                throw new AssertionError("Expected an array but got " + actual.getClass().getName());
            }
        } else if (expected instanceof Optional<?> expectedOptional) {
            if (actual instanceof Optional<?> actualOptional) {
                if (expectedOptional.isPresent() && actualOptional.isPresent()) {
                    path.addLast(".get()");
                    assertEquals(actualOptional.get(), expectedOptional.get(), path);

                } else if (expectedOptional.isEmpty() && actualOptional.isEmpty()) {
                    return; // both are empty
                } else {
                    throw new AssertionError(
                            "Expected optional to be " + (expectedOptional.isPresent() ? "present" : "empty") +
                                    " but got " + (actualOptional.isPresent() ? "present" : "empty"));
                }
                return;
            } else {
                throw new AssertionError("Expected an Optional but got " + actual.getClass().getName());
            }
        } else if (expected instanceof Map<?, ?> expectedMap) {
            if (actual instanceof Map<?, ?> actualMap) {
                if (actualMap.size() != expectedMap.size()) {
                    throw new AssertionError("Expected map size " + expectedMap.size() + " but got " + actualMap.size());
                }
                expectedMap.forEach((key, expectedValue) -> {
                    path.add("[" + key + "]");
                    assertEquals(actualMap.get(key), expectedValue, path);
                    path.removeLast();
                });
                return;
            } else {
                throw new AssertionError("Expected a map but got " + actual.getClass().getName());
            }
        }
        // compare fields
        for (final Method expectedGetter : expectedClass.getMethods()) {
            if (expectedGetter.getDeclaringClass() != Object.class &&
                    expectedGetter.getParameterCount() == 0 &&
                    (expectedGetter.getName().startsWith("get") || expectedGetter.getName().startsWith("is"))) {
                final Object expectedValue;
                try {
                    expectedValue = expectedGetter.invoke(expected);
                } catch (final IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Could not invoke expectedGetter " + expectedGetter.getName(), e);
                }
                var actualGetter = getActualGetter(expectedGetter, expectedClass, actualClass);
                final Object actualValue;
                try {
                    actualValue = actualGetter.invoke(actual);
                } catch (final IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Could not invoke expectedGetter " + expectedGetter.getName(), e);
                }
                log.debug("Comparing actual={} vs expected={}: expectedGetter {}", actualValue, expectedValue, expectedGetter.getName());
                path.addLast("." + expectedGetter.getName() + "()");
                assertEquals(actualValue, expectedValue, path);
                path.removeLast();
            }
        }
    }

    private static Method getActualGetter(final Method expectedGetter, final Class<?> expectedClass, final Class<?> actualClass) {
        Method actualGetter;
        if (expectedClass == actualClass) {
            actualGetter = expectedGetter;
        } else {
            try {
                actualGetter = actualClass.getMethod(expectedGetter.getName());
            } catch (final NoSuchMethodException e) {
                try {
                    actualGetter = actualClass.getMethod(expectedGetter.getName().startsWith("get")
                            ? expectedGetter.getName().replaceFirst("get", "is")
                            : expectedGetter.getName().replaceFirst("is", "get"));
                } catch (final NoSuchMethodException nop) {
                    throw new RuntimeException(
                            "Could not find expectedGetter " + expectedGetter.getName() + " in " + actualClass.getName(), e);
                }
            }
        }
        return actualGetter;
    }

    protected enum EventType {

        CREATED("CreatedEvent"),
        UPDATED("UpdatedEvent"),
        DELETED("DeletedEvent");

        private static final Map<EventTypeKey, Class<?>> EVENT_TYPE_MAP = new ConcurrentHashMap<>();

        private final String suffix;

        EventType(String suffix) {
            this.suffix = suffix;
        }

        private Class<?> getEventType(final Class<?> entityType) {
            return EVENT_TYPE_MAP.computeIfAbsent(
                    new EventTypeKey(entityType, this),
                    key -> {
                        try {
                            return Class.forName(
                                    "org.eclipse.hawkbit.repository.event.remote." + (this == DELETED ? "" : "entity.") +
                                            entityType.getSimpleName() +
                                            suffix);
                        } catch (final ClassNotFoundException e) {
                            throw new IllegalStateException(
                                    "Could not find event class for " + entityType.getName() + " and event type " + this, e);
                        }
                    });

        }

        private record EventTypeKey(Class<?> entityClass, EventType eventType) {}
    }

    private record RepoMan<T extends BaseEntity, C, U extends Identifiable<Long>>(
            RepositoryManagement<T, C, U> repo,
            Class<T> entityType,
            Class<C> createType,
            Class<U> updateType,
            Function<Class<?>, ?> entityFactory) {}
}
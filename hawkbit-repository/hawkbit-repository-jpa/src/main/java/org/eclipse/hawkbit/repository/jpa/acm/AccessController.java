/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

/**
 * Interface of an extended access control. Used by management layer to verify
 * the permission for CRUD operations based on some access criteria.
 * <p/>
 * After the basic service based access control is applied some additional restrictions
 * (e.g. entity based) could be applied.
 * 
 * @param <T> the domain type the repository manages
 * @param <ID> – the type of the id of the entity the repository manages
 */
public interface AccessController<T, ID> {

    /**
     * Introduce a new specification to limit the access to a specific entity.
     *
     * @return a new specification limiting the access, if empty no access restrictions
     *   are to be applied
     */
    Optional<Specification<T>> getAccessRules(Operation operation);

    /**
     * Append the resource limitation on an already existing specification.
     * 
     * @param specification
     *            is the root specification which needs to be appended by the
     *            resource limitation
     * @return a new appended specification
     */
    @Nullable
    default Specification<T> appendAccessRules(final Operation operation, @Nullable final Specification<T> specification) {
        return getAccessRules(operation)
                .map(accessRules -> specification ==  null ? accessRules : specification.and(accessRules))
                .orElse(specification);
    }

    /**
     * Verify if the given {@link Operation} is permitted for ALL entities.
     *
     * @throws InsufficientPermissionException
     *             if operation is not permitted for given entities
     */
    void assertOperationAllowed(final Operation operation) throws InsufficientPermissionException;

    /**
     * Verify if the given {@link Operation} is permitted for the entities provided by the entity supplier.
     *
     * @throws InsufficientPermissionException
     *             if operation is not permitted for given entities
     */
    void assertOperationAllowed(final Operation operation, final Supplier<T> entitySupplier) throws InsufficientPermissionException;

    /**
     * Verify if the given {@link Operation} is permitted for the entities with the given entity ids.
     *
     * @throws InsufficientPermissionException
     *             if operation is not permitted for given entities
     */
    default void assertOperationAllowed(final Operation operation, final Iterable<ID> entityIds, final Function<ID, T> entityRetriever) throws InsufficientPermissionException {
        if (entityIds != null) {
            for (final ID entityId : entityIds) {
                assertOperationAllowed(operation, () -> entityRetriever.apply(entityId));
            }
        }
    }

    /**
     * Verify if the given {@link Operation} is permitted for the provided entity.
     *
     * @throws InsufficientPermissionException
     *             if operation is not permitted for given entities
     */
    default void assertOperationAllowed(final Operation operation, final T entity) throws InsufficientPermissionException {
        assertOperationAllowed(operation, () -> entity);
    }

    /**
     * Verify if the given {@link Operation} is permitted for all provided entities.
     * <p/>
     * Note: default implementation shall be used only if the type <code>I</code> implements properly
     * {@link Object#hashCode()} and {@link Object#equals(Object)} methods
     *
     * @throws InsufficientPermissionException
     *             if operation is not permitted for given entities
     */
    void assertOperationAllowed(final Operation operation, final Iterable<? extends T> entities) throws InsufficientPermissionException;

    /**
     * Returns if the given {@link Operation} is permitted for ALL entities.
     */
    default boolean isOperationAllowed(final Operation operation) {
        try {
            assertOperationAllowed(operation);
            return true;
        } catch (final InsufficientPermissionException e) {
            return false;
        }
    }

    /**
     * Returns if the given {@link Operation} is permitted for the entity supplied by the entity supplier.
     */
    default boolean isOperationAllowed(final Operation operation, final Supplier<T> entitySupplier) {
        try {
            assertOperationAllowed(operation, entitySupplier);
            return true;
        } catch (final InsufficientPermissionException e) {
            return false;
        }
    }

    /**
     * Returns if the given {@link Operation} is permitted for the entities with the given entity ids.
     */
    default boolean isOperationAllowed(final Operation operation, final Iterable<ID> entityIds, final Function<ID, T> entityRetriever) {
        Objects.requireNonNull(entityIds);
        for (final ID entityId : entityIds) {
            if (!isOperationAllowed(operation, () -> entityRetriever.apply(entityId))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns if the given {@link Operation} is permitted for the entities with the given entity.
     */
    default boolean isOperationAllowed(final Operation operation, final T entity) {
        return isOperationAllowed(operation, () -> entity);
    }

    /**
     * Returns if the given {@link Operation} is permitted for the entities with the given entities.
     */
    default boolean isOperationAllowed(final Operation operation, final Iterable<? extends T> entities) {
        try {
            assertOperationAllowed(operation, entities);
            return true;
        } catch (final InsufficientPermissionException e) {
            return false;
        }
    }

    /**
     * Enum to define the perform operation to verify
     */
    enum Operation {

        /**
         * Entity creation
         */
        CREATE,

        /**
         * Read entities
         */
        READ,

        /**
         * Entity modification (e.g. name/description change, tag/type assignment, etc.)
         */
        UPDATE,

        /**
         * Entity deletion
         */
        DELETE
    }

    /**
     * Access controller that doesn't do any additional restrictions.
     */
    class Nop<T, ID> implements AccessController<T, ID> {

        @Override
        public Optional<Specification<T>> getAccessRules(final Operation operation) {
            return Optional.empty();
        }

        @Override
        public Specification<T> appendAccessRules(final Operation operation, final Specification<T> specification) {
            return specification;
        }

        @Override
        public void assertOperationAllowed(final Operation operation) {
            // permit all
        }

        @Override
        public void assertOperationAllowed(final Operation operation, final Supplier<T> entitySupplier) throws InsufficientPermissionException {
            // permit all
        }

        @Override
        public void assertOperationAllowed(final Operation operation, final Iterable<ID> entityIds, final Function<ID, T> entityRetriever) throws InsufficientPermissionException {
            // permit all
        }

        @Override
        public void assertOperationAllowed(final Operation operation, final T entity) throws InsufficientPermissionException {
            // permit all
        }

        @Override
        public void assertOperationAllowed(final Operation operation, final Iterable<? extends T> entities) throws InsufficientPermissionException {
            // permit all
        }

        @Override
        public boolean isOperationAllowed(final Operation operation) {
            return true;
        }

        @Override
        public boolean isOperationAllowed(final Operation operation, final Supplier<T> entitySupplier) {
            return true;
        }

        @Override
        public boolean isOperationAllowed(final Operation operation, final Iterable<ID> entityIds, final Function<ID, T> entityRetriever) {
            return true;
        }

        @Override
        public boolean isOperationAllowed(final Operation operation, final T entity) {
            return true;
        }

        @Override
        public boolean isOperationAllowed(final Operation operation, final Iterable<? extends T> entities) {
            return true;
        }
    }

    /**
     * Utility that could be used with {@link #assertOperationAllowed(Operation, Iterable, Function)} and
     * {@link #isOperationAllowed(Operation, Iterable, Function)} to pass a function that call, if used, all
     * target entities and then reuse the result further for any subsequent calls.
     * <p/>
     * Note: {@link EntityRetriever} shall be used only if the type <code>I</code> implements properly
     * {@link Object#hashCode()} and {@link Object#equals(Object)} methods
     */
    class EntityRetriever<T, ID> implements Function<ID, T> {

        private final Supplier<Iterable<T>> entityRetriever;
        private final Function<T, ID> keyFn;

        private Map<ID, T> entityIdToEntity;

        public EntityRetriever(final Supplier<Iterable<T>> entityRetriever, final Function<T, ID> keyFn) {
            this.entityRetriever = entityRetriever;
            this.keyFn = keyFn;
        }

        @Override
        public T apply(final ID entityId) {
            if (entityIdToEntity == null) {
                entityIdToEntity = index(entityRetriever.get(), keyFn);
            }
            return Optional.ofNullable(entityIdToEntity.get(entityId)).orElseThrow(() -> new InsufficientPermissionException("Entity not found!"));
        }

        private static <K, V> Map<K, V> index(final Iterable<V> iterable, final Function<V, K> keyFn) {
            final Map<K, V> index = new HashMap<>();
            for (final V element : iterable) {
                index.put(keyFn.apply(element), element);
            }
            return index;
        }
    }
}

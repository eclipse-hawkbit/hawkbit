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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.springframework.data.jpa.domain.Specification;

/**
 * Interface of an extended access control. Used by management layer to verify
 * the permission for CRUD operations based on some access criteria.
 *
 * After the basic service based access control is applied some additional restrictions
 * (e.g. entity based) could be applied.
 * 
 * @param <T> the repository type
 */
public interface AccessController<T extends Identifiable<Long>> {

    /**
     * Introduce a new specification to limit the access to a specific entity.
     *
     * @return a new specification limiting the access
     */
    Specification<T> getAccessRules(Operation operation);

    /**
     * Append the resource limitation on an already existing specification.
     * 
     * @param specification
     *            is the root specification which needs to be appended by the
     *            resource limitation
     * @return a new appended specification
     */
    default Specification<T> appendAccessRules(final Operation operation, final Specification<T> specification) {
        return specification.and(getAccessRules(operation));
    }

    /**
     * Verify if the given {@link Operation} is permitted for the provided by the entity supplier.
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
    default void assertOperationAllowed(final Operation operation, final Iterable<Long> entityIds, final Function<Long, T> entityRetriever) throws InsufficientPermissionException {
        if (entityIds != null) {
            for (final Long entityId : entityIds) {
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
     *
     * @throws InsufficientPermissionException
     *             if operation is not permitted for given entities
     */
    default void assertOperationAllowed(final Operation operation, final Iterable<? extends T> entities) throws InsufficientPermissionException {
        final List<Long> entityIds = new ArrayList<>();
        final Map<Long, T> idToEntity = new HashMap<>();
        for (final T entity : entities) {
            entityIds.add(entity.getId());
            idToEntity.put(entity.getId(), entity);
        }
        assertOperationAllowed(operation, entityIds, idToEntity::get);
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
    default boolean isOperationAllowed(final Operation operation, final Iterable<Long> entityIds, final Function<Long, T> entityRetriever) {
        Objects.requireNonNull(entityIds);
        for (final Long entityId : entityIds) {
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
     *
     * @param <T> the repository type
     */
    class Nop<T extends Identifiable<Long>> implements AccessController<T> {

        @Override
        public Specification<T> getAccessRules(final Operation operation) {
            // TODO: Verify if it's okay to specify this kind of specification when
            // initializing a list of specs which is then combined in
            // 'org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder.combineWithAnd'
            return Specification.where(null);
        }

        @Override
        public Specification<T> appendAccessRules(final Operation operation, final Specification<T> specification) {
            return specification;
        }

        @Override
        public void assertOperationAllowed(final Operation operation, final Supplier<T> entitySupplier) throws InsufficientPermissionException {
            // permit all
        }

        @Override
        public void assertOperationAllowed(final Operation operation, final Iterable<Long> entityIds, final Function<Long, T> entityRetriever) throws InsufficientPermissionException {
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
        public boolean isOperationAllowed(final Operation operation, final Supplier<T> entitySupplier) {
            return true;
        }

        @Override
        public boolean isOperationAllowed(final Operation operation, final Iterable<Long> entityIds, final Function<Long, T> entityRetriever) {
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
     *
     * @param <T>
     */
    class EntityRetrieverBase<K, T> implements Function<K, T> {

        private final Supplier<Iterable<T>> entityRetriever;
        private final Function<T, K> keyFn;

        private Map<K, T> entityIdToEntity;

        public EntityRetrieverBase(final Supplier<Iterable<T>> entityRetriever, final Function<T, K> keyFn) {
            this.entityRetriever = entityRetriever;
            this.keyFn = keyFn;
        }

        @Override
        public T apply(final K entityId) {
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
        };
    }

    class EntityRetriever<T extends Identifiable<Long>> extends EntityRetrieverBase<Long, T> {

        public EntityRetriever(final Supplier<Iterable<T>> entityRetriever) {
            super(entityRetriever, Identifiable::getId);
        }
    }
}

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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.ql.QueryField;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.MetadataSupport;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.jpa.model.WithMetadata;
import org.eclipse.hawkbit.repository.jpa.repository.BaseEntityRepository;
import org.eclipse.hawkbit.utils.ObjectCopyUtil;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("java:S119") // java:S119 - better self explainable
abstract class AbstractJpaRepositoryWithMetadataManagement<T extends AbstractJpaBaseEntity & WithMetadata<MV, MVI>, C, U extends Identifiable<Long>, R extends BaseEntityRepository<T>, A extends Enum<A> & QueryField, MV, MVI extends MV>
        extends AbstractJpaRepositoryManagement<T, C, U, R, A> implements MetadataSupport<MV> {

    private final Supplier<MVI> metadataValueCreator;
    private final boolean useCopy;

    // java:S3011 - intentionally to provide option to forbid constructors
    // java:S1141 - better visible this way
    @SuppressWarnings({ "unchecked", "java:S3011", "java:S1141" })
    protected AbstractJpaRepositoryWithMetadataManagement(final R repository, final EntityManager entityManager) {
        super(repository, entityManager);
        try {
            final Class<MVI> metadataValueType = (Class<MVI>) ((ParameterizedType) jpaRepository.getDomainClass().getMethod("getMetadata")
                    .getGenericReturnType())
                    .getActualTypeArguments()[1];
            final Constructor<MVI> constructor;
            try {
                constructor = metadataValueType.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Metadata class " + jpaRepository.getDomainClass() + " shall have no-args constructor", e);
            }
            metadataValueCreator = () -> {
                try {
                    return constructor.newInstance();
                } catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Must NEVER happen!", e);
                }
            };
            useCopy = !String.class.equals(metadataValueType) && !metadataValueType.isPrimitive();
        } catch (final NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public void createMetadata(final Long id, final String key, final MV value) {
        final T jpaEntity = getValid(id);
        final Map<String, MVI> metadataValueMap = jpaEntity.getMetadata();
        final MVI existingValue = metadataValueMap.get(key);
        if (existingValue == null) {
            assertMetadataQuota(metadataValueMap.size() + 1L);
        }
        if (setMetadataValue(key, value, existingValue, metadataValueMap)) {
            jpaRepository.save(jpaEntity);
        }
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public void createMetadata(final Long id, final Map<String, ? extends MV> metadata) {
        final T jpaEntity = getValid(id);
        final Map<String, MVI> metadataValueMap = jpaEntity.getMetadata();
        assertMetadataQuota(metadata.keySet().stream().filter(key -> !metadataValueMap.containsKey(key)).count() + metadataValueMap.size());
        final AtomicBoolean changed = new AtomicBoolean(false);
        metadata.forEach((key, value) -> {
            if (setMetadataValue(key, value, metadataValueMap.get(key), metadataValueMap)) {
                changed.set(true);
            }
        });
        if (changed.get()) {
            jpaRepository.save(jpaEntity);
        }
    }

    @Override
    public MV getMetadata(final Long id, final String key) {
        final T jpaEntity = getValid(id);
        final MV metadataValue = jpaEntity.getMetadata().get(key);
        if (metadataValue == null) {
            throw new EntityNotFoundException("Metadata", jpaRepository.getManagementClass().getSimpleName() + ":" + id + ":" + key);
        } else {
            return metadataValue;
        }
    }

    @Override
    public Map<String, MV> getMetadata(final Long id) {
        final T entity = jpaRepository.getById(id);
        return entity.getMetadata().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public void deleteMetadata(final Long id, final String key) {
        final T jpaEntity = getValid(id);
        final Map<String, MVI> metadataValueMap = jpaEntity.getMetadata();
        if (!metadataValueMap.containsKey(key)) {
            throw new EntityNotFoundException("Metadata", jpaRepository.getManagementClass().getSimpleName() + ":" + id + ":" + key);
        }
        metadataValueMap.remove(key);
        jpaRepository.save(jpaEntity);
    }

    protected abstract void assertMetadataQuota(final long requested);

    @SuppressWarnings("unchecked")
    private boolean setMetadataValue(final String key, final MV newValue, final MVI existingValue, final Map<String, MVI> metadataValueMap) {
        if (useCopy) {
            final MVI jpaMetadataValue = existingValue == null ? metadataValueCreator.get() : existingValue;
            if (ObjectCopyUtil.copy(newValue, jpaMetadataValue, true, UnaryOperator.identity())) {
                metadataValueMap.put(key, jpaMetadataValue);
                return true;
            } else {
                return false;
            }
        } else {
            if (Objects.equals(newValue, existingValue)) {
                return false;
            } else {
                metadataValueMap.put(key, (MVI) newValue);
                return true;
            }
        }
    }
}
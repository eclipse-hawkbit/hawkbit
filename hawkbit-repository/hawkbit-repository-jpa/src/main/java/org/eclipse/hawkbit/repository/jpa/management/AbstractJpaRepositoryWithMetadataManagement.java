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

import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_DELAY;
import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_MAX;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.MetadataSupport;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.jpa.model.WithMetadata;
import org.eclipse.hawkbit.repository.jpa.repository.BaseEntityRepository;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.utils.ObjectCopyUtil;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("java:S119") // java:S119 - better self explainable
abstract class AbstractJpaRepositoryWithMetadataManagement <T extends AbstractJpaBaseEntity & WithMetadata<MV, MVI>, C, U extends Identifiable<Long>, R extends BaseEntityRepository<T>, A extends Enum<A> & RsqlQueryField, MV, MVI extends MV>
        extends AbstractJpaRepositoryManagement<T, C, U, R, A> implements MetadataSupport<MV> {

    protected AbstractJpaRepositoryWithMetadataManagement(final R repository, final EntityManager entityManager) {
        super(repository, entityManager);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void createMetadata(final Long id, final String key, final MV value) {
        final T softwareModule = jpaRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id));
        final Map<String, MVI> metadataValueMap = softwareModule.getMetadata();
        final MVI existingValue = metadataValueMap.get(key);
        if (existingValue == null) {
            assertMetadataQuota(metadataValueMap.size() + 1L);
        }
        final MVI jpaMetadataValue = existingValue == null ? createMetadataValue() : existingValue;
        if (ObjectCopyUtil.copy(value, jpaMetadataValue, true, UnaryOperator.identity())) {
            metadataValueMap.put(key, jpaMetadataValue);
            jpaRepository.save(softwareModule);
        }
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void createMetadata(final Long id, final Map<String, ? extends MV> metadata) {
        final T softwareModule = jpaRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id));
        final Map<String, MVI> metadataValueMap = softwareModule.getMetadata();
        assertMetadataQuota(metadata.keySet().stream().filter(key -> !metadataValueMap.containsKey(key)).count() + metadataValueMap.size());
        final AtomicBoolean changed = new AtomicBoolean(false);
        metadata.forEach((key, value) -> {
            final MVI jpaMetadataValue = metadataValueMap.getOrDefault(key, createMetadataValue());
            if (ObjectCopyUtil.copy(value, jpaMetadataValue, true, UnaryOperator.identity())) {
                metadataValueMap.put(key, jpaMetadataValue);
                changed.set(true);
            }
        });
        if (changed.get()) {
            jpaRepository.save(softwareModule);
        }
    }

    @Override
    public MV getMetadata(final Long id, final String key) {
        final T softwareModule = jpaRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id));
        final MV metadataValue = softwareModule.getMetadata().get(key);
        if (metadataValue == null) {
            throw new EntityNotFoundException("Metadata", jpaRepository.getManagementClass().getSimpleName() + ":" + id + ":" + key);
        } else {
            return metadataValue;
        }
    }

    @Override
    public Map<String, MV> getMetadata(final Long id) {
        return jpaRepository
                .findById(id)
                .map(T::getMetadata)
                .map(metadata -> metadata.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> (MV)e.getValue())))
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void deleteMetadata(final Long id, final String key) {
        final T softwareModule = jpaRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id));
        final Map<String, MVI> metadataValueMap = softwareModule.getMetadata();
        if (!metadataValueMap.containsKey(key)) {
            throw new EntityNotFoundException("Metadata", jpaRepository.getManagementClass().getSimpleName() + ":" + id + ":" + key);
        }
        metadataValueMap.remove(key);
        jpaRepository.save(softwareModule);
    }

    protected abstract MVI createMetadataValue();

    protected abstract void assertMetadataQuota(final long requested);
}
/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
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

import java.util.Collections;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.DistributionSetTagFields;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.builder.GenericTagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetTagCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetTagSpecifications;
import org.eclipse.hawkbit.repository.jpa.specifications.TagSpecification;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "distribution-set-tag-management" }, matchIfMissing = true)
public class JpaDistributionSetTagManagement
        extends AbstractJpaRepositoryManagement<JpaDistributionSetTag, JpaDistributionSetTagCreate, GenericTagUpdate, DistributionSetTagRepository, DistributionSetTagFields>
        implements DistributionSetTagManagement<JpaDistributionSetTag, JpaDistributionSetTagCreate, GenericTagUpdate> {

    private final DistributionSetRepository distributionSetRepository;

    public JpaDistributionSetTagManagement(
            final DistributionSetTagRepository distributionSetTagRepository,
            final EntityManager entityManager,
            final DistributionSetRepository distributionSetRepository) {
        super(distributionSetTagRepository, entityManager);
        this.distributionSetRepository = distributionSetRepository;
    }

    @Override
    public Optional<JpaDistributionSetTag> findByName(final String name) {
        return jpaRepository.findByNameEquals(name);
    }

    @Override
    public Page<JpaDistributionSetTag> findByDistributionSet(final long distributionSetId, final Pageable pageable) {
        if (!distributionSetRepository.existsById(distributionSetId)) {
            throw new EntityNotFoundException(DistributionSet.class, distributionSetId);
        }

        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository,
                Collections.singletonList(TagSpecification.ofDistributionSet(distributionSetId)), pageable
        );
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void delete(final String tagName) {
        final JpaDistributionSetTag dsTag = jpaRepository
                .findOne(DistributionSetTagSpecifications.byName(tagName))
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, tagName));
        jpaRepository.delete(dsTag);
    }
}
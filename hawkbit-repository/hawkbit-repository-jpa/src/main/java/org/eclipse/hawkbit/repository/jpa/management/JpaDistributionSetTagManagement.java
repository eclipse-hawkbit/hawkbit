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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetTagFields;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.builder.GenericTagUpdate;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTagCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetTagSpecifications;
import org.eclipse.hawkbit.repository.jpa.specifications.TagSpecification;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link TargetTagManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaDistributionSetTagManagement implements DistributionSetTagManagement {

    private final DistributionSetTagRepository distributionSetTagRepository;
    private final DistributionSetRepository distributionSetRepository;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final Database database;

    public JpaDistributionSetTagManagement(
            final DistributionSetTagRepository distributionSetTagRepository,
            final DistributionSetRepository distributionSetRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final Database database) {
        this.distributionSetTagRepository = distributionSetTagRepository;
        this.distributionSetRepository = distributionSetRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSetTag> create(final Collection<TagCreate> dst) {
        final List<JpaDistributionSetTag> toCreate = dst.stream().map(JpaTagCreate.class::cast)
                .map(JpaTagCreate::buildDistributionSetTag).toList();
        return Collections
                .unmodifiableList(distributionSetTagRepository.saveAll(AccessController.Operation.CREATE, toCreate));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetTag create(final TagCreate c) {
        final JpaTagCreate create = (JpaTagCreate) c;
        return distributionSetTagRepository.save(AccessController.Operation.CREATE, create.buildDistributionSetTag());
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetTag update(final TagUpdate u) {
        final GenericTagUpdate update = (GenericTagUpdate) u;

        final JpaDistributionSetTag tag = distributionSetTagRepository.findById(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, update.getId()));

        update.getName().ifPresent(tag::setName);
        update.getDescription().ifPresent(tag::setDescription);
        update.getColour().ifPresent(tag::setColour);

        return distributionSetTagRepository.save(tag);
    }

    @Override
    public long count() {
        return distributionSetTagRepository.count();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long id) {
        distributionSetTagRepository.deleteById(id);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        final List<JpaDistributionSetTag> setsFound = distributionSetTagRepository.findAllById(ids);

        if (setsFound.size() < ids.size()) {
            throw new EntityNotFoundException(DistributionSetTag.class, ids,
                    setsFound.stream().map(DistributionSetTag::getId).collect(Collectors.toList()));
        }

        distributionSetTagRepository.deleteAll(setsFound);
    }

    @Override
    public List<DistributionSetTag> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(distributionSetTagRepository.findAllById(ids));
    }

    @Override
    public boolean exists(final long id) {
        return distributionSetTagRepository.existsById(id);
    }

    @Override
    public Optional<DistributionSetTag> get(final long id) {
        return distributionSetTagRepository.findById(id).map(DistributionSetTag.class::cast);
    }

    @Override
    public Slice<DistributionSetTag> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetTagRepository, pageable, null);
    }

    @Override
    public Page<DistributionSetTag> findByRsql(final Pageable pageable, final String rsqlParam) {
        final Specification<JpaDistributionSetTag> spec = RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetTagFields.class,
                virtualPropertyReplacer, database);

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetTagRepository, pageable,
                Collections.singletonList(spec));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final String tagName) {
        final JpaDistributionSetTag dsTag = distributionSetTagRepository
                .findOne(DistributionSetTagSpecifications.byName(tagName))
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, tagName));

        distributionSetTagRepository.delete(dsTag);
    }

    @Override
    public Optional<DistributionSetTag> getByName(final String name) {
        return distributionSetTagRepository.findByNameEquals(name);
    }

    @Override
    public Page<DistributionSetTag> findByDistributionSet(final Pageable pageable, final long distributionSetId) {
        if (!distributionSetRepository.existsById(distributionSetId)) {
            throw new EntityNotFoundException(DistributionSet.class, distributionSetId);
        }

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetTagRepository, pageable,
                Collections.singletonList(TagSpecification.ofDistributionSet(distributionSetId)));
    }
}

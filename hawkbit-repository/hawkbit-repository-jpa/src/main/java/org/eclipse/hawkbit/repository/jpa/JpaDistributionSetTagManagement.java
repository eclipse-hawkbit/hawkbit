/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.builder.GenericTagUpdate;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTagCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TagSpecification;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link TargetTagManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaDistributionSetTagManagement implements DistributionSetTagManagement {

    private final DistributionSetTagRepository distributionSetTagRepository;

    private final DistributionSetRepository distributionSetRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final NoCountPagingRepository criteriaNoCountDao;

    JpaDistributionSetTagManagement(final DistributionSetTagRepository distributionSetTagRepository,
            final DistributionSetRepository distributionSetRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final NoCountPagingRepository criteriaNoCountDao) {
        this.distributionSetTagRepository = distributionSetTagRepository;
        this.distributionSetRepository = distributionSetRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.criteriaNoCountDao = criteriaNoCountDao;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
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
    public Optional<DistributionSetTag> getByName(final String name) {
        return distributionSetTagRepository.findByNameEquals(name);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetTag create(final TagCreate c) {
        final JpaTagCreate create = (JpaTagCreate) c;
        return distributionSetTagRepository.save(create.buildDistributionSetTag());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSetTag> create(final Collection<TagCreate> dst) {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final Collection<JpaTagCreate> creates = (Collection) dst;

        return Collections.unmodifiableList(
                creates.stream().map(create -> distributionSetTagRepository.save(create.buildDistributionSetTag()))
                        .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final String tagName) {
        if (!distributionSetTagRepository.existsByName(tagName)) {
            throw new EntityNotFoundException(DistributionSetTag.class, tagName);
        }

        distributionSetTagRepository.deleteByName(tagName);
    }

    @Override
    public Slice<DistributionSetTag> findAll(final Pageable pageable) {
        return convertDsPage(criteriaNoCountDao.findAll(pageable, JpaDistributionSetTag.class), pageable);
    }

    @Override
    public Page<DistributionSetTag> findByRsql(final Pageable pageable, final String rsqlParam) {
        final Specification<JpaDistributionSetTag> spec = RSQLUtility.parse(rsqlParam, TagFields.class,
                virtualPropertyReplacer);

        return convertDsPage(distributionSetTagRepository.findAll(spec, pageable), pageable);
    }

    @Override
    public Page<DistributionSetTag> findByDistributionSet(final Pageable pageable, final long setId) {
        if (!distributionSetRepository.exists(setId)) {
            throw new EntityNotFoundException(DistributionSet.class, setId);
        }

        return convertDsPage(distributionSetTagRepository.findAll(TagSpecification.ofDistributionSet(setId), pageable),
                pageable);
    }

    private static Page<DistributionSetTag> convertDsPage(final Page<JpaDistributionSetTag> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    private static Slice<DistributionSetTag> convertDsPage(final Slice<JpaDistributionSetTag> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, 0);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        final List<JpaDistributionSetTag> setsFound = distributionSetTagRepository.findAll(ids);

        if (setsFound.size() < ids.size()) {
            throw new EntityNotFoundException(DistributionSetTag.class, ids,
                    setsFound.stream().map(DistributionSetTag::getId).collect(Collectors.toList()));
        }

        distributionSetTagRepository.delete(setsFound);
    }

    @Override
    public List<DistributionSetTag> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(distributionSetTagRepository.findAll(ids));
    }

    @Override
    public Optional<DistributionSetTag> get(final long id) {
        return Optional.ofNullable(distributionSetTagRepository.findOne(id));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long id) {
        distributionSetTagRepository.delete(id);
    }

    @Override
    public boolean exists(final long id) {
        return distributionSetTagRepository.exists(id);
    }

    @Override
    public long count() {
        return distributionSetTagRepository.count();
    }

}

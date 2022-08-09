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

import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.builder.GenericTagUpdate;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTagCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TagSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.jpa.vendor.Database;
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
public class JpaTargetTagManagement implements TargetTagManagement {

    private final TargetTagRepository targetTagRepository;

    private final TargetRepository targetRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final Database database;

    public JpaTargetTagManagement(final TargetTagRepository targetTagRepository,
            final TargetRepository targetRepository, final VirtualPropertyReplacer virtualPropertyReplacer,
            final Database database) {
        this.targetTagRepository = targetTagRepository;
        this.targetRepository = targetRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
    }

    @Override
    public Optional<TargetTag> getByName(final String name) {
        return targetTagRepository.findByNameEquals(name);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTag create(final TagCreate c) {
        final JpaTagCreate create = (JpaTagCreate) c;

        return targetTagRepository.save(create.buildTargetTag());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<TargetTag> create(final Collection<TagCreate> tt) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<JpaTagCreate> targetTags = (Collection) tt;

        return Collections.unmodifiableList(targetTags.stream()
                .map(ttc -> targetTagRepository.save(ttc.buildTargetTag())).collect(Collectors.toList()));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final String targetTagName) {
        if (!targetTagRepository.existsByName(targetTagName)) {
            throw new EntityNotFoundException(TargetTag.class, targetTagName);
        }

        // finally delete the tag itself
        targetTagRepository.deleteByName(targetTagName);
    }

    @Override
    public Page<TargetTag> findByRsql(final Pageable pageable, final String rsqlParam) {
        return JpaManagementHelper.findAllWithCountBySpec(targetTagRepository, pageable, Collections.singletonList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, TagFields.class, virtualPropertyReplacer, database)));
    }

    @Override
    public long count() {
        return targetTagRepository.count();
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTag update(final TagUpdate u) {
        final GenericTagUpdate update = (GenericTagUpdate) u;

        final JpaTargetTag tag = targetTagRepository.findById(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, update.getId()));

        update.getName().ifPresent(tag::setName);
        update.getDescription().ifPresent(tag::setDescription);
        update.getColour().ifPresent(tag::setColour);

        return targetTagRepository.save(tag);
    }

    @Override
    public Optional<TargetTag> get(final long id) {
        return targetTagRepository.findById(id).map(TargetTag.class::cast);
    }

    @Override
    public List<TargetTag> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(targetTagRepository.findAllById(ids));
    }

    @Override
    public Page<TargetTag> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(targetTagRepository, pageable, null);
    }

    @Override
    public Page<TargetTag> findByTarget(final Pageable pageable, final String controllerId) {
        if (!targetRepository.exists(TargetSpecifications.hasControllerId(controllerId))) {
            throw new EntityNotFoundException(Target.class, controllerId);
        }

        return JpaManagementHelper.findAllWithCountBySpec(targetTagRepository, pageable,
                Collections.singletonList(TagSpecification.ofTarget(controllerId)));
    }
}

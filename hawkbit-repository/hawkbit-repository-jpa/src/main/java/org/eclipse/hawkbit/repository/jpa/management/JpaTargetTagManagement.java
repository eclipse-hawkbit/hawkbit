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

import org.eclipse.hawkbit.repository.TargetTagFields;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.builder.GenericTagUpdate;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTagCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag_;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
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
 */
@Transactional(readOnly = true)
@Validated
public class JpaTargetTagManagement implements TargetTagManagement {

    private final TargetTagRepository targetTagRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final Database database;

    public JpaTargetTagManagement(
            final TargetTagRepository targetTagRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final Database database) {
        this.targetTagRepository = targetTagRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
    }

    @Override
    public long count() {
        return targetTagRepository.count();
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTag create(final TagCreate c) {
        final JpaTagCreate create = (JpaTagCreate) c;

        return targetTagRepository.save(AccessController.Operation.CREATE, create.buildTargetTag());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<TargetTag> create(final Collection<TagCreate> tt) {
        final List<JpaTargetTag> targetTagList = tt.stream().map(JpaTagCreate.class::cast)
                .map(JpaTagCreate::buildTargetTag).toList();
        return Collections.unmodifiableList(
                targetTagRepository.saveAll(AccessController.Operation.CREATE, targetTagList));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final String targetTagName) {
        targetTagRepository.delete(
                targetTagRepository
                        .findOne(((root, query, cb) -> cb.equal(root.get(JpaTargetTag_.name), targetTagName)))
                        .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, targetTagName)));
    }

    @Override
    public Page<TargetTag> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(targetTagRepository, pageable, null);
    }

    @Override
    public Page<TargetTag> findByRsql(final Pageable pageable, final String rsqlParam) {
        return JpaManagementHelper.findAllWithCountBySpec(targetTagRepository, pageable, Collections.singletonList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, TargetTagFields.class, virtualPropertyReplacer, database)));
    }

    @Override
    public Optional<TargetTag> getByName(final String name) {
        return targetTagRepository.findByNameEquals(name);
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
}
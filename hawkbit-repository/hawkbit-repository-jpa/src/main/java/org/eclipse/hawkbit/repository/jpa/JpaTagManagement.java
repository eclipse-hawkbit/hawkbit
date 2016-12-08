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
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.builder.GenericTagUpdate;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTagCreate;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JP>A implementation of {@link TagManagement}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
public class JpaTagManagement implements TagManagement {

    @Autowired
    private TargetTagRepository targetTagRepository;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private DistributionSetTagRepository distributionSetTagRepository;

    @Autowired
    private DistributionSetRepository distributionSetRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Override
    public TargetTag findTargetTag(final String name) {
        return targetTagRepository.findByNameEquals(name);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetTag createTargetTag(final TagCreate c) {
        final JpaTagCreate create = (JpaTagCreate) c;

        final JpaTargetTag targetTag = create.buildTargetTag();

        if (findTargetTag(targetTag.getName()) != null) {
            throw new EntityAlreadyExistsException();
        }

        final TargetTag save = targetTagRepository.save(targetTag);

        afterCommit.afterCommit(
                () -> eventPublisher.publishEvent(new TargetTagCreatedEvent(save, applicationContext.getId())));

        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<TargetTag> createTargetTags(final Collection<TagCreate> tt) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<JpaTagCreate> targetTags = (Collection) tt;

        final List<TargetTag> save = Collections.unmodifiableList(targetTags.stream()
                .map(ttc -> targetTagRepository.save(ttc.buildTargetTag())).collect(Collectors.toList()));
        afterCommit.afterCommit(() -> save.forEach(
                tag -> eventPublisher.publishEvent(new TargetTagCreatedEvent(tag, applicationContext.getId()))));
        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteTargetTag(final String targetTagName) {
        final JpaTargetTag tag = targetTagRepository.findByNameEquals(targetTagName);

        targetRepository.findByTag(tag.getId()).forEach(set -> {
            set.removeTag(tag);
            targetRepository.save(set);
        });

        // finally delete the tag itself
        targetTagRepository.deleteByName(targetTagName);

        afterCommit.afterCommit(() -> eventPublisher.publishEvent(
                new TargetTagDeletedEvent(tenantAware.getCurrentTenant(), tag.getId(), applicationContext.getId())));

    }

    @Override
    public List<TargetTag> findAllTargetTags() {
        return Collections.unmodifiableList(targetTagRepository.findAll());
    }

    @Override
    public Page<TargetTag> findAllTargetTags(final String rsqlParam, final Pageable pageable) {

        final Specification<JpaTargetTag> spec = RSQLUtility.parse(rsqlParam, TagFields.class, virtualPropertyReplacer);
        return convertTPage(targetTagRepository.findAll(spec, pageable), pageable);
    }

    private static Page<TargetTag> convertTPage(final Page<JpaTargetTag> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    private static Page<DistributionSetTag> convertDsPage(final Page<JpaDistributionSetTag> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public long countTargetTags() {
        return targetTagRepository.count();
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetTag updateTargetTag(final TagUpdate u) {
        final GenericTagUpdate update = (GenericTagUpdate) u;

        final JpaTargetTag tag = Optional.ofNullable(targetTagRepository.findOne(update.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Target tag with ID " + update.getId() + " not found"));

        update.getName().ifPresent(tag::setName);
        update.getDescription().ifPresent(tag::setDescription);
        update.getColour().ifPresent(tag::setColour);

        final TargetTag save = targetTagRepository.save(tag);
        afterCommit.afterCommit(() -> eventPublisher
                .publishEvent(new TargetTagUpdateEvent(save, EventPublisherHolder.getInstance().getApplicationId())));
        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetTag updateDistributionSetTag(final TagUpdate u) {
        final GenericTagUpdate update = (GenericTagUpdate) u;

        final JpaDistributionSetTag tag = Optional.ofNullable(distributionSetTagRepository.findOne(update.getId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Distribution set tag with ID " + update.getId() + " not found"));

        update.getName().ifPresent(tag::setName);
        update.getDescription().ifPresent(tag::setDescription);
        update.getColour().ifPresent(tag::setColour);

        final DistributionSetTag save = distributionSetTagRepository.save(tag);
        afterCommit.afterCommit(() -> eventPublisher.publishEvent(
                new DistributionSetTagUpdateEvent(save, EventPublisherHolder.getInstance().getApplicationId())));
        return save;
    }

    @Override
    public DistributionSetTag findDistributionSetTag(final String name) {
        return distributionSetTagRepository.findByNameEquals(name);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetTag createDistributionSetTag(final TagCreate c) {
        final JpaTagCreate create = (JpaTagCreate) c;

        final JpaDistributionSetTag distributionSetTag = create.buildDistributionSetTag();

        if (distributionSetTagRepository.findByNameEquals(distributionSetTag.getName()) != null) {
            throw new EntityAlreadyExistsException();
        }

        final DistributionSetTag save = distributionSetTagRepository.save(distributionSetTag);

        afterCommit.afterCommit(() -> eventPublisher
                .publishEvent(new DistributionSetTagCreatedEvent(save, applicationContext.getId())));
        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<DistributionSetTag> createDistributionSetTags(final Collection<TagCreate> dst) {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final Collection<JpaTagCreate> creates = (Collection) dst;

        final List<DistributionSetTag> save = Collections.unmodifiableList(
                creates.stream().map(create -> distributionSetTagRepository.save(create.buildDistributionSetTag()))
                        .collect(Collectors.toList()));
        afterCommit.afterCommit(() -> save.forEach(tag -> eventPublisher
                .publishEvent(new DistributionSetTagCreatedEvent(tag, applicationContext.getId()))));
        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteDistributionSetTag(final String tagName) {
        final JpaDistributionSetTag tag = distributionSetTagRepository.findByNameEquals(tagName);

        distributionSetRepository.findByTag(tag).forEach(set -> {
            set.removeTag(tag);
            distributionSetRepository.save(set);
        });

        distributionSetTagRepository.deleteByName(tagName);

        afterCommit.afterCommit(
                () -> eventPublisher.publishEvent(new DistributionSetTagDeletedEvent(tenantAware.getCurrentTenant(),
                        tag.getId(), applicationContext.getId())));
    }

    @Override
    public List<DistributionSetTag> findAllDistributionSetTags() {
        return Collections.unmodifiableList(distributionSetTagRepository.findAll());
    }

    @Override
    public TargetTag findTargetTagById(final Long id) {
        return targetTagRepository.findOne(id);
    }

    @Override
    public DistributionSetTag findDistributionSetTagById(final Long id) {
        return distributionSetTagRepository.findOne(id);
    }

    @Override
    public Page<TargetTag> findAllTargetTags(final Pageable pageable) {
        return convertTPage(targetTagRepository.findAll(pageable), pageable);
    }

    @Override
    public Page<DistributionSetTag> findAllDistributionSetTags(final Pageable pageable) {
        return convertDsPage(distributionSetTagRepository.findAll(pageable), pageable);
    }

    @Override
    public Page<DistributionSetTag> findAllDistributionSetTags(final String rsqlParam, final Pageable pageable) {
        final Specification<JpaDistributionSetTag> spec = RSQLUtility.parse(rsqlParam, TagFields.class,
                virtualPropertyReplacer);

        return convertDsPage(distributionSetTagRepository.findAll(spec, pageable), pageable);
    }

}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetTagCreatedBulkEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.rsql.VirtualPropertyLookup;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.eventbus.EventBus;

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
    private EventBus eventBus;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    @Autowired
    private VirtualPropertyLookup virtualPropertyLookup;

    @Override
    public TargetTag findTargetTag(final String name) {
        return targetTagRepository.findByNameEquals(name);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetTag createTargetTag(final TargetTag targetTag) {
        if (null != targetTag.getId()) {
            throw new EntityAlreadyExistsException();
        }

        if (findTargetTag(targetTag.getName()) != null) {
            throw new EntityAlreadyExistsException();
        }

        final TargetTag save = targetTagRepository.save((JpaTargetTag) targetTag);

        afterCommit
                .afterCommit(() -> eventBus.post(new TargetTagCreatedBulkEvent(tenantAware.getCurrentTenant(), save)));

        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<TargetTag> createTargetTags(final Collection<TargetTag> tt) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<JpaTargetTag> targetTags = (Collection) tt;

        targetTags.forEach(tag -> {
            if (tag.getId() != null) {
                throw new EntityAlreadyExistsException();
            }
        });

        final List<TargetTag> save = Collections.unmodifiableList(targetTagRepository.save(targetTags));
        afterCommit
                .afterCommit(() -> eventBus.post(new TargetTagCreatedBulkEvent(tenantAware.getCurrentTenant(), save)));
        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteTargetTag(final String targetTagName) {
        final JpaTargetTag tag = targetTagRepository.findByNameEquals(targetTagName);

        final List<JpaTarget> changed = new LinkedList<>();
        for (final JpaTarget target : targetRepository.findByTag(tag)) {
            target.getTags().remove(tag);
            changed.add(target);
        }

        // save association delete
        targetRepository.save(changed);

        // finally delete the tag itself
        targetTagRepository.deleteByName(targetTagName);

        afterCommit.afterCommit(() -> eventBus.post(new TargetTagDeletedEvent(tag)));

    }

    @Override
    public List<TargetTag> findAllTargetTags() {
        return Collections.unmodifiableList(targetTagRepository.findAll());
    }

    @Override
    public Page<TargetTag> findAllTargetTags(final String rsqlParam, final Pageable pageable) {

        final Specification<JpaTargetTag> spec = RSQLUtility.parse(rsqlParam, TagFields.class, virtualPropertyLookup);
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
    public TargetTag updateTargetTag(final TargetTag targetTag) {
        checkNotNull(targetTag.getName());
        checkNotNull(targetTag.getId());
        final TargetTag save = targetTagRepository.save((JpaTargetTag) targetTag);
        afterCommit.afterCommit(() -> eventBus.post(new TargetTagUpdateEvent(save)));
        return save;
    }

    @Override
    public DistributionSetTag findDistributionSetTag(final String name) {
        return distributionSetTagRepository.findByNameEquals(name);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetTag createDistributionSetTag(final DistributionSetTag distributionSetTag) {
        if (null != distributionSetTag.getId()) {
            throw new EntityAlreadyExistsException();
        }

        if (distributionSetTagRepository.findByNameEquals(distributionSetTag.getName()) != null) {
            throw new EntityAlreadyExistsException();
        }

        final DistributionSetTag save = distributionSetTagRepository.save((JpaDistributionSetTag) distributionSetTag);

        afterCommit.afterCommit(
                () -> eventBus.post(new DistributionSetTagCreatedBulkEvent(tenantAware.getCurrentTenant(), save)));
        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<DistributionSetTag> createDistributionSetTags(final Collection<DistributionSetTag> dst) {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final Collection<JpaDistributionSetTag> distributionSetTags = (Collection) dst;

        for (final DistributionSetTag dsTag : distributionSetTags) {
            if (dsTag.getId() != null) {
                throw new EntityAlreadyExistsException();
            }
        }
        final List<DistributionSetTag> save = Collections
                .unmodifiableList(distributionSetTagRepository.save(distributionSetTags));
        afterCommit.afterCommit(
                () -> eventBus.post(new DistributionSetTagCreatedBulkEvent(tenantAware.getCurrentTenant(), save)));

        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteDistributionSetTag(final String tagName) {
        final JpaDistributionSetTag tag = distributionSetTagRepository.findByNameEquals(tagName);

        final List<JpaDistributionSet> changed = new LinkedList<>();
        for (final JpaDistributionSet set : distributionSetRepository.findByTag(tag)) {
            set.removeTag(tag);
            changed.add(set);
        }

        // save association delete
        distributionSetRepository.save(changed);

        distributionSetTagRepository.deleteByName(tagName);

        afterCommit.afterCommit(() -> eventBus.post(new DistributionSetTagDeletedEvent(tag)));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetTag updateDistributionSetTag(final DistributionSetTag distributionSetTag) {
        checkNotNull(distributionSetTag.getName());
        checkNotNull(distributionSetTag.getId());
        final DistributionSetTag save = distributionSetTagRepository.save((JpaDistributionSetTag) distributionSetTag);
        afterCommit.afterCommit(() -> eventBus.post(new DistributionSetTagUpdateEvent(save)));

        return save;
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
                virtualPropertyLookup);

        return convertDsPage(distributionSetTagRepository.findAll(spec, pageable), pageable);
    }

}

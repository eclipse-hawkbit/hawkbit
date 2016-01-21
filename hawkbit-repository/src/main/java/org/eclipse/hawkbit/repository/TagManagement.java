/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.eventbus.event.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagUpdateEvent;
import org.eclipse.hawkbit.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.eventbus.EventBus;

/**
 *
 * Mangement service class for {@link Tag}s.
 *
 *
 *
 *
 *
 */
@Transactional(readOnly = true)
@Validated
@Service
public class TagManagement {

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

    /**
     * Find {@link TargetTag} based on given Name.
     *
     * @param name
     *            to look for.
     * @return {@link TargetTag} or <code>null</code> if it does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public TargetTag findTargetTag(@NotEmpty final String name) {
        return targetTagRepository.findByNameEquals(name);
    }

    /**
     * Creates a new {@link TargetTag}.
     * 
     * @param targetTag
     *            to be created
     *
     * @return the new created {@link TargetTag}
     *
     * @throws EntityAlreadyExistsException
     *             if given object already exists
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    public TargetTag createTargetTag(@NotNull final TargetTag targetTag) {
        if (null != targetTag.getId()) {
            throw new EntityAlreadyExistsException();
        }

        if (findTargetTag(targetTag.getName()) != null) {
            throw new EntityAlreadyExistsException();
        }

        final TargetTag save = targetTagRepository.save(targetTag);

        afterCommit
                .afterCommit(() -> eventBus.post(new TargetTagCreatedBulkEvent(tenantAware.getCurrentTenant(), save)));

        return save;
    }

    /**
     * created multiple {@link TargetTag}s.
     * 
     * @param targetTags
     *            to be created
     * @return the new created {@link TargetTag}s
     *
     * @throws EntityAlreadyExistsException
     *             if given object has already an ID.
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    public List<TargetTag> createTargetTags(@NotNull final Iterable<TargetTag> targetTags) {
        targetTags.forEach(tag -> {
            if (tag.getId() != null) {
                throw new EntityAlreadyExistsException();
            }
        });
        final List<TargetTag> save = targetTagRepository.save(targetTags);
        afterCommit
                .afterCommit(() -> eventBus.post(new TargetTagCreatedBulkEvent(tenantAware.getCurrentTenant(), save)));
        return save;
    }

    /**
     * Deletes {@link TargetTag} with given name.
     * 
     * @param targetTagName
     *            tag name of the {@link TargetTag} to be deleted
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    public void deleteTargetTag(@NotEmpty final String targetTagName) {
        final TargetTag tag = targetTagRepository.findByNameEquals(targetTagName);

        final List<Target> changed = new LinkedList<Target>();
        for (final Target target : targetRepository.findByTag(tag)) {
            target.getTags().remove(tag);
            changed.add(target);
        }

        // save association delete
        targetRepository.save(changed);

        // finally delete the tag itself
        targetTagRepository.deleteByName(targetTagName);

        afterCommit.afterCommit(() -> eventBus.post(new TargetTagDeletedEvent(tag)));

    }

    /**
     * returns all {@link TargetTag}s.
     * 
     * @return all {@link TargetTag}s
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<TargetTag> findAllTargetTags() {
        return targetTagRepository.findAll();
    }

    /**
     * Retrieves all target tags based on the given specification.
     *
     * @param spec
     *            the specification for the query
     * @param pageable
     *            pagination parameter
     * @return the found {@link Target}s, never {@code null}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Page<TargetTag> findAllTargetTags(@NotNull final Specification<TargetTag> spec,
            @NotNull final Pageable pageable) {
        return targetTagRepository.findAll(spec, pageable);
    }

    /**
     * count {@link TargetTag}s.
     * 
     * @return size of {@link TargetTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public long countTargetTags() {
        return targetTagRepository.count();
    }

    /**
     * updates the {@link TargetTag}.
     *
     * @param targetTag
     *            the {@link TargetTag}
     * @return the new {@link TargetTag}
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public TargetTag updateTargetTag(@NotNull final TargetTag targetTag) {
        checkNotNull(targetTag.getName());
        checkNotNull(targetTag.getId());
        final TargetTag save = targetTagRepository.save(targetTag);
        afterCommit.afterCommit(() -> eventBus.post(new TargetTagUpdateEvent(save)));
        return save;
    }

    /**
     * Find {@link DistributionSet} based on given name.
     *
     * @param name
     *            to look for.
     * @return {@link DistributionSet} or <code>null</code> if it does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public DistributionSetTag findDistributionSetTag(@NotEmpty final String name) {
        return distributionSetTagRepository.findByNameEquals(name);
    }

    /**
     * Creates a {@link DistributionSet}.
     *
     * @param distributionSetTag
     *            to be created.
     * @return the new {@link DistributionSet}
     * @throws EntityAlreadyExistsException
     *             if distributionSetTag already exists
     *
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public DistributionSetTag createDistributionSetTag(@NotNull final DistributionSetTag distributionSetTag) {
        if (null != distributionSetTag.getId()) {
            throw new EntityAlreadyExistsException();
        }

        if (distributionSetTagRepository.findByNameEquals(distributionSetTag.getName()) != null) {
            throw new EntityAlreadyExistsException();
        }

        final DistributionSetTag save = distributionSetTagRepository.save(distributionSetTag);

        afterCommit.afterCommit(() -> eventBus.post(new DistributionSetTagCreatedBulkEvent(tenantAware
                .getCurrentTenant(), save)));
        return save;
    }

    /**
     * Creates multiple {@link DistributionSetTag}s.
     *
     * @param distributionSetTags
     *            to be created
     * @return the new {@link DistributionSetTag}
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public List<DistributionSetTag> createDistributionSetTags(
            @NotNull final Iterable<DistributionSetTag> distributionSetTags) {
        for (final DistributionSetTag dsTag : distributionSetTags) {
            if (dsTag.getId() != null) {
                throw new EntityAlreadyExistsException();
            }
        }
        final List<DistributionSetTag> save = distributionSetTagRepository.save(distributionSetTags);
        afterCommit.afterCommit(() -> eventBus.post(new DistributionSetTagCreatedBulkEvent(tenantAware
                .getCurrentTenant(), save)));

        return save;
    }

    /**
     * Deletes {@link DistributionSetTag} by given
     * {@link DistributionSetTag#getName()}.
     *
     * @param tagNames
     *            to be deleted
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void deleteDistributionSetTag(@NotEmpty final String tagName) {
        final DistributionSetTag tag = distributionSetTagRepository.findByNameEquals(tagName);

        final List<DistributionSet> changed = new LinkedList<DistributionSet>();
        for (final DistributionSet set : distributionSetRepository.findByTag(tag)) {
            set.getTags().remove(tag);
            changed.add(set);
        }

        // save association delete
        distributionSetRepository.save(changed);

        distributionSetTagRepository.deleteByName(tagName);

        afterCommit.afterCommit(() -> eventBus.post(new DistributionSetTagDeletedEvent(tag)));
    }

    /**
     * Updates an existing {@link DistributionSetTag}.
     *
     * @param distributionSetTag
     *            to be updated
     * @return the updated {@link DistributionSet}
     * @throws NullPointerException
     *             of {@link DistributionSetTag#getName()} is <code>null</code>
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public DistributionSetTag updateDistributionSetTag(@NotNull final DistributionSetTag distributionSetTag) {
        checkNotNull(distributionSetTag.getName());
        checkNotNull(distributionSetTag.getId());
        final DistributionSetTag save = distributionSetTagRepository.save(distributionSetTag);
        afterCommit.afterCommit(() -> eventBus.post(new DistributionSetTagUpdateEvent(save)));

        return save;
    }

    /**
     * returns all {@link DistributionTag}s.
     * 
     * @return all {@link DistributionTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public List<DistributionSetTag> findAllDistributionSetTags() {
        return distributionSetTagRepository.findAll();
    }

    /**
     * Finds {@link TargetTag} by given id.
     *
     * @param id
     *            to search for
     * @return the found {@link TargetTag}s or <code>null</code> if not found.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public TargetTag findTargetTagById(@NotNull final Long id) {
        return targetTagRepository.findOne(id);
    }

    /**
     * Finds {@link DistributionSetTag} by given id.
     *
     * @param id
     *            to search for
     * @return the found {@link DistributionSetTag}s or <code>null</code> if not
     *         found.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public DistributionSetTag findDistributionSetTagById(@NotNull final Long id) {
        return distributionSetTagRepository.findOne(id);
    }

    /**
     * returns all {@link TargetTag}s.
     * 
     * @param pageReq
     *            page parameter
     *
     * @return all {@link TargetTag}s
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Page<TargetTag> findAllTargetTags(@NotNull final Pageable pageReq) {
        return targetTagRepository.findAll(pageReq);
    }

    /**
     * returns all {@link DistributionSetTag}s.
     *
     * @param pageReq
     *            page parameter
     * @return all {@link DistributionSetTag}s
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<DistributionSetTag> findAllDistributionSetTags(@NotNull final Pageable pageReq) {
        return distributionSetTagRepository.findAll(pageReq);
    }

    /**
     * Retrieves all DistributionSet tags based on the given specification.
     *
     * @param spec
     *            the specification for the query
     * @param pageable
     *            pagination parameter
     * @return the found {@link DistributionSetTag}s, never {@code null}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Page<DistributionSetTag> findAllDistributionSetTags(@NotNull final Specification<DistributionSetTag> spec,
            @NotNull final Pageable pageable) {
        return distributionSetTagRepository.findAll(spec, pageable);
    }

}

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
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTagCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TagSpecification;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JP>A implementation of {@link TagManagement}.
 *
 */
@Transactional(readOnly = true)
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
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Override
    public Optional<TargetTag> findTargetTag(final String name) {
        return targetTagRepository.findByNameEquals(name);
    }

    @Override
    @Transactional
    public TargetTag createTargetTag(final TagCreate c) {
        final JpaTagCreate create = (JpaTagCreate) c;

        return targetTagRepository.save(create.buildTargetTag());
    }

    @Override
    @Transactional
    public List<TargetTag> createTargetTags(final Collection<TagCreate> tt) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<JpaTagCreate> targetTags = (Collection) tt;

        return Collections.unmodifiableList(targetTags.stream()
                .map(ttc -> targetTagRepository.save(ttc.buildTargetTag())).collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public void deleteTargetTag(final String targetTagName) {
        if (!targetTagRepository.existsByName(targetTagName)) {
            throw new EntityNotFoundException(TargetTag.class, targetTagName);
        }

        // finally delete the tag itself
        targetTagRepository.deleteByName(targetTagName);
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
    @Transactional
    public TargetTag updateTargetTag(final TagUpdate u) {
        final GenericTagUpdate update = (GenericTagUpdate) u;

        final JpaTargetTag tag = targetTagRepository.findById(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, update.getId()));

        update.getName().ifPresent(tag::setName);
        update.getDescription().ifPresent(tag::setDescription);
        update.getColour().ifPresent(tag::setColour);

        return targetTagRepository.save(tag);
    }

    @Override

    @Transactional
    public DistributionSetTag updateDistributionSetTag(final TagUpdate u) {
        final GenericTagUpdate update = (GenericTagUpdate) u;

        final JpaDistributionSetTag tag = distributionSetTagRepository.findById(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, update.getId()));

        update.getName().ifPresent(tag::setName);
        update.getDescription().ifPresent(tag::setDescription);
        update.getColour().ifPresent(tag::setColour);

        return distributionSetTagRepository.save(tag);
    }

    @Override
    public Optional<DistributionSetTag> findDistributionSetTag(final String name) {
        return distributionSetTagRepository.findByNameEquals(name);
    }

    @Override
    @Transactional
    public DistributionSetTag createDistributionSetTag(final TagCreate c) {
        final JpaTagCreate create = (JpaTagCreate) c;
        return distributionSetTagRepository.save(create.buildDistributionSetTag());
    }

    @Override
    @Transactional
    public List<DistributionSetTag> createDistributionSetTags(final Collection<TagCreate> dst) {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        final Collection<JpaTagCreate> creates = (Collection) dst;

        return Collections.unmodifiableList(
                creates.stream().map(create -> distributionSetTagRepository.save(create.buildDistributionSetTag()))
                        .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public void deleteDistributionSetTag(final String tagName) {
        if (!distributionSetTagRepository.existsByName(tagName)) {
            throw new EntityNotFoundException(DistributionSetTag.class, tagName);
        }

        distributionSetTagRepository.deleteByName(tagName);
    }

    @Override
    public Optional<TargetTag> findTargetTagById(final Long id) {
        return Optional.ofNullable(targetTagRepository.findOne(id));
    }

    @Override
    public Optional<DistributionSetTag> findDistributionSetTagById(final Long id) {
        return Optional.ofNullable(distributionSetTagRepository.findOne(id));
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

    @Override
    public Page<TargetTag> findAllTargetTags(final Pageable pageable, final String controllerId) {
        if (!targetRepository.existsByControllerId(controllerId)) {
            throw new EntityNotFoundException(Target.class, controllerId);
        }

        return convertTPage(targetTagRepository.findAll(TagSpecification.ofTarget(controllerId), pageable), pageable);
    }

    @Override
    public Page<DistributionSetTag> findDistributionSetTagsByDistributionSet(final Pageable pageable,
            final Long setId) {
        if (!distributionSetRepository.exists(setId)) {
            throw new EntityNotFoundException(DistributionSet.class, setId);
        }

        return convertDsPage(distributionSetTagRepository.findAll(TagSpecification.ofDistributionSet(setId), pageable),
                pageable);
    }
}

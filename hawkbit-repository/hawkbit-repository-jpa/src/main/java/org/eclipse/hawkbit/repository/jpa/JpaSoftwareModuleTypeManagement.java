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

import org.eclipse.hawkbit.repository.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link SoftwareModuleTypeManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaSoftwareModuleTypeManagement implements SoftwareModuleTypeManagement {

    private final DistributionSetTypeRepository distributionSetTypeRepository;

    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final SoftwareModuleRepository softwareModuleRepository;

    JpaSoftwareModuleTypeManagement(final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final SoftwareModuleRepository softwareModuleRepository) {
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.softwareModuleRepository = softwareModuleRepository;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleType update(final SoftwareModuleTypeUpdate u) {
        final GenericSoftwareModuleTypeUpdate update = (GenericSoftwareModuleTypeUpdate) u;

        final JpaSoftwareModuleType type = (JpaSoftwareModuleType) get(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, update.getId()));

        update.getDescription().ifPresent(type::setDescription);
        update.getColour().ifPresent(type::setColour);

        return softwareModuleTypeRepository.save(type);
    }

    @Override
    public Page<SoftwareModuleType> findByRsql(final Pageable pageable, final String rsqlParam) {

        final Specification<JpaSoftwareModuleType> spec = RSQLUtility.parse(rsqlParam, SoftwareModuleTypeFields.class,
                virtualPropertyReplacer);

        return convertSmTPage(softwareModuleTypeRepository.findAll(spec, pageable), pageable);
    }

    @Override
    public Page<SoftwareModuleType> findAll(final Pageable pageable) {
        return softwareModuleTypeRepository.findByDeleted(pageable, false);
    }

    @Override
    public long count() {
        return softwareModuleTypeRepository.countByDeleted(false);
    }

    @Override
    public Optional<SoftwareModuleType> getByKey(final String key) {
        return softwareModuleTypeRepository.findByKey(key);
    }

    @Override
    public Optional<SoftwareModuleType> getByName(final String name) {
        return softwareModuleTypeRepository.findByName(name);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleType create(final SoftwareModuleTypeCreate c) {
        final JpaSoftwareModuleTypeCreate create = (JpaSoftwareModuleTypeCreate) c;

        return softwareModuleTypeRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Long typeId) {
        final JpaSoftwareModuleType toDelete = softwareModuleTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, typeId));

        if (softwareModuleRepository.countByType(toDelete) > 0
                || distributionSetTypeRepository.countByElementsSmType(toDelete) > 0) {
            toDelete.setDeleted(true);
            softwareModuleTypeRepository.save(toDelete);
        } else {
            softwareModuleTypeRepository.delete(toDelete);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<SoftwareModuleType> create(final Collection<SoftwareModuleTypeCreate> creates) {
        return creates.stream().map(this::create).collect(Collectors.toList());
    }

    private static Page<SoftwareModuleType> convertSmTPage(final Page<JpaSoftwareModuleType> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        final List<JpaSoftwareModuleType> setsFound = softwareModuleTypeRepository.findAll(ids);

        if (setsFound.size() < ids.size()) {
            throw new EntityNotFoundException(SoftwareModuleType.class, ids,
                    setsFound.stream().map(SoftwareModuleType::getId).collect(Collectors.toList()));
        }

        softwareModuleTypeRepository.delete(setsFound);
    }

    @Override
    public List<SoftwareModuleType> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(softwareModuleTypeRepository.findAll(ids));
    }

    @Override
    public Optional<SoftwareModuleType> get(final Long id) {
        return Optional.ofNullable(softwareModuleTypeRepository.findOne(id));
    }

    @Override
    public boolean exists(final Long id) {
        return softwareModuleTypeRepository.exists(id);
    }

}

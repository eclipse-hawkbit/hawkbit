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

import org.eclipse.hawkbit.repository.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleTypeSpecification;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link SoftwareModuleTypeManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaSoftwareModuleTypeManagement implements SoftwareModuleTypeManagement {

    private final DistributionSetTypeRepository distributionSetTypeRepository;
    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final SoftwareModuleRepository softwareModuleRepository;
    private final Database database;

    public JpaSoftwareModuleTypeManagement(final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final SoftwareModuleRepository softwareModuleRepository,
            final Database database) {
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.softwareModuleRepository = softwareModuleRepository;
        this.database = database;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleType create(final SoftwareModuleTypeCreate c) {
        final JpaSoftwareModuleTypeCreate create = (JpaSoftwareModuleTypeCreate) c;

        return softwareModuleTypeRepository.save(AccessController.Operation.CREATE, create.build());
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleType update(final SoftwareModuleTypeUpdate u) {
        final GenericSoftwareModuleTypeUpdate update = (GenericSoftwareModuleTypeUpdate) u;

        final JpaSoftwareModuleType type = (JpaSoftwareModuleType) get(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, update.getId()));

        update.getDescription().ifPresent(type::setDescription);
        update.getColour().ifPresent(type::setColour);

        return softwareModuleTypeRepository.save(type);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long id) {
        final JpaSoftwareModuleType toDelete = softwareModuleTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, id));

        delete(toDelete);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        softwareModuleTypeRepository
                .findAll(AccessController.Operation.DELETE, softwareModuleTypeRepository.byIdsSpec(ids))
                .forEach(this::delete);
    }

    @Override
    public Optional<SoftwareModuleType> get(final long id) {
        return softwareModuleTypeRepository.findById(id).map(SoftwareModuleType.class::cast);
    }

    @Override
    public List<SoftwareModuleType> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(softwareModuleTypeRepository.findAllById(ids));
    }

    @Override
    public boolean exists(final long id) {
        return softwareModuleTypeRepository.existsById(id);
    }

    @Override
    public long count() {
        return softwareModuleTypeRepository.count(SoftwareModuleTypeSpecification.isNotDeleted());
    }

    @Override
    public Slice<SoftwareModuleType> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(softwareModuleTypeRepository,
                List.of(SoftwareModuleTypeSpecification.isNotDeleted()), pageable
        );
    }

    @Override
    public Page<SoftwareModuleType> findByRsql(final String rsql, final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(softwareModuleTypeRepository, List.of(
                RSQLUtility.buildRsqlSpecification(rsql, SoftwareModuleTypeFields.class,
                        virtualPropertyReplacer, database),
                SoftwareModuleTypeSpecification.isNotDeleted()), pageable
        );
    }

    @Override
    public Optional<SoftwareModuleType> findByKey(final String key) {
        return softwareModuleTypeRepository.findByKey(key);
    }

    @Override
    public Optional<SoftwareModuleType> findByName(final String name) {
        return softwareModuleTypeRepository.findByName(name);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<SoftwareModuleType> create(final Collection<SoftwareModuleTypeCreate> c) {
        final List<JpaSoftwareModuleType> creates = c.stream().map(JpaSoftwareModuleTypeCreate.class::cast)
                .map(JpaSoftwareModuleTypeCreate::build).toList();
        return Collections.unmodifiableList(
                softwareModuleTypeRepository.saveAll(AccessController.Operation.CREATE, creates));
    }

    private void delete(JpaSoftwareModuleType toDelete) {
        if (softwareModuleRepository.countByType(toDelete) > 0
                || distributionSetTypeRepository.countByElementsSmType(toDelete) > 0) {
            toDelete.setDeleted(true);
            softwareModuleTypeRepository.save(AccessController.Operation.DELETE, toDelete);
        } else {
            softwareModuleTypeRepository.delete(toDelete);
        }
    }
}

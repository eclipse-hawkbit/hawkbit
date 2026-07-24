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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.SoftDeletedMode;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.exception.DeletedException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleTypeSpecification;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.qfields.SoftwareModuleTypeFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "software-module-type=management" }, matchIfMissing = true)
public class JpaSoftwareModuleTypeManagement
        extends AbstractJpaRepositoryManagement<JpaSoftwareModuleType, SoftwareModuleTypeManagement.Create, SoftwareModuleTypeManagement.Update, SoftwareModuleTypeRepository, SoftwareModuleTypeFields>
        implements SoftwareModuleTypeManagement<JpaSoftwareModuleType> {

    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;
    private final DistributionSetTypeRepository distributionSetTypeRepository;
    private final SoftwareModuleRepository softwareModuleRepository;

    protected JpaSoftwareModuleTypeManagement(
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final EntityManager entityManager,
            final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleRepository softwareModuleRepository) {
        super(softwareModuleTypeRepository, entityManager);
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.softwareModuleRepository = softwareModuleRepository;
    }

    @Override
    public Optional<JpaSoftwareModuleType> findByKey(final String key) {
        return jpaRepository.findByKey(key);
    }

    @Override
    public Page<JpaSoftwareModuleType> findAll(SoftDeletedMode softDeletedMode, Pageable pageable) {
        if (softDeletedMode != SoftDeletedMode.INCLUDE_SOFT_DELETED) {
            final Specification<JpaSoftwareModuleType> deletedSpec =
                    SoftwareModuleTypeSpecification.isDeleted(softDeletedMode == SoftDeletedMode.ONLY_SOFT_DELETED);

            return softwareModuleTypeRepository.findAll(deletedSpec, pageable);
        }
        return softwareModuleTypeRepository.findAll(pageable);
    }

    @Override
    public Page<JpaSoftwareModuleType> findByRsql(String rsql, SoftDeletedMode softDeletedMode, Pageable pageable) {
        final Specification<JpaSoftwareModuleType> rsqlSpec = QLSupport.getInstance().buildSpec(rsql, SoftwareModuleTypeFields.class);
        if (softDeletedMode != SoftDeletedMode.INCLUDE_SOFT_DELETED) {
            final Specification<JpaSoftwareModuleType> deletedSpec =
                    SoftwareModuleTypeSpecification.isDeleted(softDeletedMode == SoftDeletedMode.ONLY_SOFT_DELETED);

            return softwareModuleTypeRepository.findAll(JpaManagementHelper.combineWithAnd(List.of(rsqlSpec, deletedSpec)), pageable);
        }
        return softwareModuleTypeRepository.findAll(rsqlSpec, pageable);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public JpaSoftwareModuleType update(final SoftwareModuleTypeManagement.Update update) {
        final JpaSoftwareModuleType softwareModuleType = softwareModuleTypeRepository.getById(update.getId());
        assertSoftwareModuleTypeIsNotDeleted(softwareModuleType);
        return super.update(update);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public Map<Long, JpaSoftwareModuleType> update(final Collection<SoftwareModuleTypeManagement.Update> updates) {
        final List<Long> ids = updates.stream().map(Identifiable::getId).toList();
        softwareModuleTypeRepository.findAllById(ids).forEach(this::assertSoftwareModuleTypeIsNotDeleted);
        return super.update(updates);
    }

    @Override
    protected Collection<JpaSoftwareModuleType> softDelete(final Collection<JpaSoftwareModuleType> toDelete) {
        return toDelete.stream().filter(smt ->
                softwareModuleRepository.countByType(smt) > 0 || distributionSetTypeRepository.countByElementsSmType(smt) > 0).toList();
    }

    private void assertSoftwareModuleTypeIsNotDeleted(final JpaSoftwareModuleType softwareModuleType) {
        if (softwareModuleType.isDeleted()) {
            throw new DeletedException(SoftwareModuleType.class, softwareModuleType.getId());
        }
    }
}
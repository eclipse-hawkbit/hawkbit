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
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.qfields.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "software-module-type=management" }, matchIfMissing = true)
public class JpaSoftwareModuleTypeManagement
        extends AbstractJpaRepositoryManagement<JpaSoftwareModuleType, SoftwareModuleTypeManagement.Create, SoftwareModuleTypeManagement.Update, SoftwareModuleTypeRepository, SoftwareModuleTypeFields>
        implements SoftwareModuleTypeManagement<JpaSoftwareModuleType> {

    private final DistributionSetTypeRepository distributionSetTypeRepository;
    private final SoftwareModuleRepository softwareModuleRepository;

    protected JpaSoftwareModuleTypeManagement(
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final EntityManager entityManager,
            final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleRepository softwareModuleRepository) {
        super(softwareModuleTypeRepository, entityManager);
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.softwareModuleRepository = softwareModuleRepository;
    }

    @Override
    protected Optional<Cache> getCache() {
        return Optional.of(TenantAwareCacheManager.getInstance().getCache(JpaSoftwareModuleType.class.getSimpleName()));
    }

    @Override
    public Optional<JpaSoftwareModuleType> findByKey(final String key) {
        return jpaRepository.findByKey(key);
    }

    @Override
    protected Collection<JpaSoftwareModuleType> softDelete(final Collection<JpaSoftwareModuleType> toDelete) {
        return toDelete.stream().filter(smt ->
                softwareModuleRepository.countByType(smt) > 0 || distributionSetTypeRepository.countByElementsSmType(smt) > 0).toList();
    }
}
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

import org.eclipse.hawkbit.repository.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.utils.ExceptionMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "software-module-type=management" }, matchIfMissing = true)
public class JpaSoftwareModuleTypeManagement
        extends AbstractJpaRepositoryManagement<JpaSoftwareModuleType, SoftwareModuleTypeManagement.Create, SoftwareModuleTypeManagement.Update, SoftwareModuleTypeRepository, SoftwareModuleTypeFields>
        implements SoftwareModuleTypeManagement<JpaSoftwareModuleType> {

    private final DistributionSetTypeRepository distributionSetTypeRepository;
    private final SoftwareModuleRepository softwareModuleRepository;

    JpaSoftwareModuleTypeManagement(
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final EntityManager entityManager,
            final DistributionSetTypeRepository distributionSetTypeRepository,
            final SoftwareModuleRepository softwareModuleRepository) {
        super(softwareModuleTypeRepository, entityManager);
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.softwareModuleRepository = softwareModuleRepository;
    }

    // TODO - do not override just to do a mapping
    public JpaSoftwareModuleType create(final Create create) {
        try {
            return super.create(create);
        } catch (final Exception e) {
            throw ExceptionMapper.mapRe(e);
        }
    }

    @Override
    public Optional<JpaSoftwareModuleType> findByKey(final String key) {
        return jpaRepository.findByKey(key);
    }

    @Override
    public Optional<JpaSoftwareModuleType> findByName(final String name) {
        return jpaRepository.findByName(name);
    }

    @Override
    protected Collection<JpaSoftwareModuleType> softDelete(final Collection<JpaSoftwareModuleType> toDelete) {
        return toDelete.stream().filter(smt ->
                softwareModuleRepository.countByType(smt) > 0 || distributionSetTypeRepository.countByElementsSmType(smt) > 0).toList();
    }
}
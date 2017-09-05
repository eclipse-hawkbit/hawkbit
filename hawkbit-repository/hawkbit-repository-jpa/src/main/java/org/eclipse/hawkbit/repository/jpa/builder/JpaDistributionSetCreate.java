/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.builder.AbstractDistributionSetUpdateCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.util.CollectionUtils;

/**
 * Create/build implementation.
 *
 */
public class JpaDistributionSetCreate extends AbstractDistributionSetUpdateCreate<DistributionSetCreate>
        implements DistributionSetCreate {

    private final DistributionSetTypeManagement distributionSetTypeManagement;
    private final SoftwareModuleManagement softwareModuleManagement;

    JpaDistributionSetCreate(final DistributionSetTypeManagement distributionSetTypeManagement,
            final SoftwareModuleManagement softwareManagement) {
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.softwareModuleManagement = softwareManagement;
    }

    @Override
    public JpaDistributionSet build() {
        return new JpaDistributionSet(name, version, description,
                Optional.ofNullable(type).map(this::findDistributionSetTypeWithExceptionIfNotFound).orElse(null),
                findSoftwareModuleWithExceptionIfNotFound(modules),
                Optional.ofNullable(requiredMigrationStep).orElse(Boolean.FALSE));
    }

    private DistributionSetType findDistributionSetTypeWithExceptionIfNotFound(final String distributionSetTypekey) {
        return distributionSetTypeManagement.getByKey(distributionSetTypekey)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, distributionSetTypekey));
    }

    private Collection<SoftwareModule> findSoftwareModuleWithExceptionIfNotFound(
            final Collection<Long> softwareModuleId) {
        if (CollectionUtils.isEmpty(softwareModuleId)) {
            return Collections.emptyList();
        }

        final Collection<SoftwareModule> module = softwareModuleManagement.get(softwareModuleId);
        if (module.size() < softwareModuleId.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId);
        }

        return module;
    }

}

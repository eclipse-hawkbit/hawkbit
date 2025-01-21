/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import lombok.Getter;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.ValidString;
import org.eclipse.hawkbit.repository.builder.AbstractDistributionSetUpdateCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.util.CollectionUtils;

/**
 * Create/build implementation.
 */
public class JpaDistributionSetCreate extends AbstractDistributionSetUpdateCreate<DistributionSetCreate> implements DistributionSetCreate {

    private final DistributionSetTypeManagement distributionSetTypeManagement;
    private final SoftwareModuleManagement softwareModuleManagement;

    @Getter
    @ValidString
    private String type;

    JpaDistributionSetCreate(
            final DistributionSetTypeManagement distributionSetTypeManagement, final SoftwareModuleManagement softwareManagement) {
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.softwareModuleManagement = softwareManagement;
    }

    @Override
    public DistributionSetCreate type(final String type) {
        this.type = type == null ? null : type.strip();
        return this;
    }

    @Override
    public JpaDistributionSet build() {
        return new JpaDistributionSet(
                name, version, description,
                Optional.ofNullable(type).map(this::findDistributionSetTypeWithExceptionIfNotFound).orElse(null),
                findSoftwareModuleWithExceptionIfNotFound(modules),
                Optional.ofNullable(requiredMigrationStep).orElse(Boolean.FALSE));
    }

    private DistributionSetType findDistributionSetTypeWithExceptionIfNotFound(final String distributionSetTypekey) {
        return distributionSetTypeManagement.findByKey(distributionSetTypekey)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, distributionSetTypekey));
    }

    private Collection<SoftwareModule> findSoftwareModuleWithExceptionIfNotFound(final Collection<Long> softwareModuleIds) {
        if (CollectionUtils.isEmpty(softwareModuleIds)) {
            return Collections.emptyList();
        }

        final Collection<SoftwareModule> modules = softwareModuleManagement.get(softwareModuleIds);
        if (modules.size() < softwareModuleIds.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, softwareModuleIds);
        }

        return modules;
    }
}
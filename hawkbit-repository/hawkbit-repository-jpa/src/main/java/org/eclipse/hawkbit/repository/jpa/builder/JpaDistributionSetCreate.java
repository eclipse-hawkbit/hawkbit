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
import org.eclipse.hawkbit.repository.ValidString;
import org.eclipse.hawkbit.repository.builder.AbstractDistributionSetUpdateCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.management.JpaDistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaSoftwareModuleManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.util.CollectionUtils;

/**
 * Create/build implementation.
 */
public class JpaDistributionSetCreate
        extends AbstractDistributionSetUpdateCreate<DistributionSetCreate<JpaDistributionSet>>
        implements DistributionSetCreate<JpaDistributionSet> {

    private final JpaDistributionSetTypeManagement distributionSetTypeManagement;
    private final JpaSoftwareModuleManagement softwareModuleManagement;

    @Getter
    @ValidString
    private String type;

    JpaDistributionSetCreate(
            final JpaDistributionSetTypeManagement distributionSetTypeManagement, final JpaSoftwareModuleManagement softwareManagement) {
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.softwareModuleManagement = softwareManagement;
    }

    @Override
    public DistributionSetCreate<JpaDistributionSet> type(final String type) {
        this.type = type.strip();
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

    private JpaDistributionSetType findDistributionSetTypeWithExceptionIfNotFound(final String distributionSetTypekey) {
        return distributionSetTypeManagement.findByKey(distributionSetTypekey)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, distributionSetTypekey));
    }

    private Collection<JpaSoftwareModule> findSoftwareModuleWithExceptionIfNotFound(final Collection<Long> softwareModuleIds) {
        if (CollectionUtils.isEmpty(softwareModuleIds)) {
            return Collections.emptyList();
        }

        final Collection<JpaSoftwareModule> modules = softwareModuleManagement.get(softwareModuleIds);
        if (modules.size() < softwareModuleIds.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, softwareModuleIds);
        }

        return modules;
    }
}
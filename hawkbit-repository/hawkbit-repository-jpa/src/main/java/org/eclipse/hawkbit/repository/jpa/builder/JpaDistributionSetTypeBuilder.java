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

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Builder implementation for {@link DistributionSetType}.
 */
public class JpaDistributionSetTypeBuilder implements DistributionSetTypeBuilder {

    private final SoftwareModuleTypeManagement<? extends SoftwareModuleType, ?, ?> softwareModuleTypeManagement;

    public JpaDistributionSetTypeBuilder(final SoftwareModuleTypeManagement<? extends SoftwareModuleType, ?, ?> softwareModuleTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    public DistributionSetTypeUpdate update(final long id) {
        return new GenericDistributionSetTypeUpdate(id);
    }

    @Override
    public DistributionSetTypeCreate create() {
        return new JpaDistributionSetTypeCreate(softwareModuleTypeManagement);
    }
}
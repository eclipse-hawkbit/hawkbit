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

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetUpdate;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Builder implementation for {@link DistributionSet}.
 */
public class JpaDistributionSetBuilder implements DistributionSetBuilder {

    private final DistributionSetTypeManagement distributionSetTypeManagement;
    private final SoftwareModuleManagement softwareModuleManagement;

    public JpaDistributionSetBuilder(final DistributionSetTypeManagement distributionSetTypeManagement,
            final SoftwareModuleManagement softwareManagement) {
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.softwareModuleManagement = softwareManagement;
    }

    @Override
    public DistributionSetUpdate update(final long id) {
        return new GenericDistributionSetUpdate(id);
    }

    @Override
    public DistributionSetCreate create() {
        return new JpaDistributionSetCreate(distributionSetTypeManagement, softwareModuleManagement);
    }
}
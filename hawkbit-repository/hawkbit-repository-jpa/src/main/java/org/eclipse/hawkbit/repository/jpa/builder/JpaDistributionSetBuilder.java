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

import org.eclipse.hawkbit.repository.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetUpdate;
import org.eclipse.hawkbit.repository.jpa.management.JpaDistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.jpa.management.JpaSoftwareModuleManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Builder implementation for {@link DistributionSet}.
 */
public class JpaDistributionSetBuilder implements DistributionSetBuilder<JpaDistributionSet> {

    private final JpaDistributionSetTypeManagement distributionSetTypeManagement;
    private final JpaSoftwareModuleManagement softwareModuleManagement;

    public JpaDistributionSetBuilder(
            final JpaDistributionSetTypeManagement distributionSetTypeManagement,
            final JpaSoftwareModuleManagement softwareManagement) {
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.softwareModuleManagement = softwareManagement;
    }

    @Override
    public DistributionSetUpdate update(final long id) {
        return new GenericDistributionSetUpdate(id);
    }

    @Override
    public DistributionSetCreate<JpaDistributionSet> create() {
        return new JpaDistributionSetCreate(distributionSetTypeManagement, softwareModuleManagement);
    }
}
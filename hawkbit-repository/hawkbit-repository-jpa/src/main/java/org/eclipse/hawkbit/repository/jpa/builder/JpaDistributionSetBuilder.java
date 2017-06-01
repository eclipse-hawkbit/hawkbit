/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 *
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

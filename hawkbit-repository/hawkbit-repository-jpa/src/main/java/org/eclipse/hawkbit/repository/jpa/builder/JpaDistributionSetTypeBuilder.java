/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.model.DistributionSetType;

/**
 * Builder implementation for {@link DistributionSetType}.
 *
 */
public class JpaDistributionSetTypeBuilder implements DistributionSetTypeBuilder {

    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;

    public JpaDistributionSetTypeBuilder(final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
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

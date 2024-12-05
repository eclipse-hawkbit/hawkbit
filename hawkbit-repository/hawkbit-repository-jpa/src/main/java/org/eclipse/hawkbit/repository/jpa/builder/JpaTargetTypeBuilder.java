/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.builder.GenericTargetTypeUpdate;
import org.eclipse.hawkbit.repository.builder.TargetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.builder.TargetTypeUpdate;
import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * Builder implementation for {@link TargetType}.
 */
public class JpaTargetTypeBuilder implements TargetTypeBuilder {

    private final DistributionSetTypeManagement distributionSetTypeManagement;

    /**
     * Constructor
     *
     * @param distributionSetTypeManagement Distribution set type management
     */
    public JpaTargetTypeBuilder(DistributionSetTypeManagement distributionSetTypeManagement) {
        this.distributionSetTypeManagement = distributionSetTypeManagement;
    }

    @Override
    public TargetTypeUpdate update(long id) {
        return new GenericTargetTypeUpdate(id);
    }

    @Override
    public TargetTypeCreate create() {
        return new JpaTargetTypeCreate(distributionSetTypeManagement);
    }
}
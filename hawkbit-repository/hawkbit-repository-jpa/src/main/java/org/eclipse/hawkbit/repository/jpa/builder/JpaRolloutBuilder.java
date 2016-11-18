/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.builder.GenericRolloutUpdate;
import org.eclipse.hawkbit.repository.builder.RolloutBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * Builder implementation for {@link Rollout}.
 *
 */
public class JpaRolloutBuilder implements RolloutBuilder {
    private final DistributionSetManagement distributionSetManagement;

    public JpaRolloutBuilder(final DistributionSetManagement distributionSetManagement) {
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    public RolloutUpdate update(final long id) {
        return new GenericRolloutUpdate(id);
    }

    @Override
    public RolloutCreate create() {
        return new JpaRolloutCreate(distributionSetManagement);
    }

}

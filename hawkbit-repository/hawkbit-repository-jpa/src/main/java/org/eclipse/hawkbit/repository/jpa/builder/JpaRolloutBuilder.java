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

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.builder.GenericRolloutUpdate;
import org.eclipse.hawkbit.repository.builder.RolloutBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * Builder implementation for {@link Rollout}.
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

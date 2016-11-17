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
import org.eclipse.hawkbit.repository.builder.GenericTargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * Builder implementation for {@link TargetFilterQuery}.
 *
 */
public class JpaTargetFilterQueryBuilder implements TargetFilterQueryBuilder {
    private final DistributionSetManagement distributionSetManagement;

    public JpaTargetFilterQueryBuilder(final DistributionSetManagement distributionSetManagement) {
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    public TargetFilterQueryUpdate update(final long id) {
        return new GenericTargetFilterQueryUpdate(id);
    }

    @Override
    public TargetFilterQueryCreate create() {
        return new JpaTargetFilterQueryCreate(distributionSetManagement);
    }

}

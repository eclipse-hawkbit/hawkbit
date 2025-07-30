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
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericTargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * Builder implementation for {@link TargetFilterQuery}.
 */
public class JpaTargetFilterQueryBuilder implements TargetFilterQueryBuilder {

    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;

    public JpaTargetFilterQueryBuilder(final DistributionSetManagement<? extends DistributionSet> distributionSetManagement) {
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    public TargetFilterQueryUpdate update(final long id) {
        return new GenericTargetFilterQueryUpdate(id);
    }

    @Override
    public AutoAssignDistributionSetUpdate updateAutoAssign(final long id) {
        return new AutoAssignDistributionSetUpdate(id);
    }

    @Override
    public TargetFilterQueryCreate create() {
        return new JpaTargetFilterQueryCreate(distributionSetManagement);
    }
}
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
import org.eclipse.hawkbit.repository.builder.AbstractTargetFilterQueryUpdateCreate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Create/build implementation.
 *
 */
public class JpaTargetFilterQueryCreate extends AbstractTargetFilterQueryUpdateCreate<TargetFilterQueryCreate>
        implements TargetFilterQueryCreate {

    private final DistributionSetManagement distributionSetManagement;

    JpaTargetFilterQueryCreate(final DistributionSetManagement distributionSetManagement) {
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    public JpaTargetFilterQuery build() {

        return new JpaTargetFilterQuery(name, query,
                getSet().map(this::findDistributionSetAndThrowExceptionIfNotFound).orElse(null));
    }

    private DistributionSet findDistributionSetAndThrowExceptionIfNotFound(final Long setId) {
        return distributionSetManagement.get(setId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, setId));
    }

}

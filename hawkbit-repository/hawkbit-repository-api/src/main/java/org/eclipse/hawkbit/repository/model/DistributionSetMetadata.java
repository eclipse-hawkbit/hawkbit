/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * {@link MetaData} of a {@link DistributionSet}.
 *
 */
public interface DistributionSetMetadata extends MetaData {

    /**
     * @return {@link DistributionSet} of this {@link MetaData} entry.
     */
    DistributionSet getDistributionSet();

    @Override
    default Long getEntityId() {
        return getDistributionSet().getId();
    }

}

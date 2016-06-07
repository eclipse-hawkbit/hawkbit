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
 * MetaData of a tenant account.
 *
 */
public interface TenantMetaData extends BaseEntity {

    /**
     * @return default {@link DistributionSetType}.
     */
    DistributionSetType getDefaultDsType();

    /**
     * @param defaultDsType
     *            that is used of none is selected for a new
     *            {@link DistributionSet}.
     */
    void setDefaultDsType(DistributionSetType defaultDsType);

    /**
     * @return tenant name
     */
    String getTenant();

}

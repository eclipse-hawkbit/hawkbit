/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.filters;

import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetDistributionsStateDataProvider;

/**
 * Filter params for {@link DistributionSetDistributionsStateDataProvider}.
 */
public class DsDistributionsFilterParams extends DsFilterParams {
    private static final long serialVersionUID = 1L;

    private Long dsTypeId;

    /**
     * Constructor for DsDistributionsFilterParams
     */
    public DsDistributionsFilterParams() {
        this(null, null);
    }

    /**
     * Constructor.
     *
     * @param searchText
     *            String
     * @param dsTypeId
     *            Long
     */
    public DsDistributionsFilterParams(final String searchText, final Long dsTypeId) {
        super(searchText);
        this.dsTypeId = dsTypeId;
    }

    /**
     * @return DsTypeId
     */
    public Long getDsTypeId() {
        return dsTypeId;
    }

    /**
     * Setter for DsTypeId
     *
     * @param dsTypeId
     *            Long
     */
    public void setDsTypeId(final Long dsTypeId) {
        this.dsTypeId = dsTypeId;
    }
}

/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.filters;

import java.util.Objects;

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
     * Copy Constructor.
     *
     * @param filter
     *            A filter to be copied
     */
    public DsDistributionsFilterParams(final DsDistributionsFilterParams filter) {
        super(filter);
        this.dsTypeId = filter.getDsTypeId();
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

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DsDistributionsFilterParams other = (DsDistributionsFilterParams) obj;
        return Objects.equals(this.getSearchText(), other.getSearchText())
                && Objects.equals(this.getDsTypeId(), other.getDsTypeId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSearchText(), getDsTypeId());
    }
}

/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.filters;

import java.io.Serializable;

import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetDistributionsStateDataProvider;
import org.springframework.util.StringUtils;

/**
 * Filter params for {@link DistributionSetDistributionsStateDataProvider}.
 */
public class DsDistributionsFilterParams implements Serializable {
    private static final long serialVersionUID = 1L;

    private String searchText;
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
     *          String
     * @param dsTypeId
     *          Long
     */
    public DsDistributionsFilterParams(final String searchText, final Long dsTypeId) {
        this.searchText = searchText;
        this.dsTypeId = dsTypeId;
    }

    /**
     * Gets the searchText
     *
     * @return SearchText
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Sets the SearchText
     *
     * @param searchText
     *          String
     */
    public void setSearchText(final String searchText) {
        this.searchText = !StringUtils.isEmpty(searchText) ? String.format("%%%s%%", searchText) : null;
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
     *          Long
     */
    public void setDsTypeId(final Long dsTypeId) {
        this.dsTypeId = dsTypeId;
    }
}

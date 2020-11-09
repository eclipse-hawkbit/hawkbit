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

import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetManagementStateDataProvider;
import org.springframework.util.StringUtils;

/**
 * Filter params for {@link DistributionSetManagementStateDataProvider}.
 */
public class DsFilterParams implements Serializable {
    private static final long serialVersionUID = 1L;

    private String searchText;

    /**
     * Constructor for DsManagementFilterParams
     */
    public DsFilterParams() {
        this("");
    }

    /**
     * Constructor.
     *
     * @param searchText
     *            String as search text
     */
    public DsFilterParams(final String searchText) {
        this.searchText = searchText;
    }

    /**
     * @return SearchText
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Setter for searchText
     *
     * @param searchText
     *            String
     */
    public void setSearchText(final String searchText) {
        this.searchText = !StringUtils.isEmpty(searchText) ? String.format("%%%s%%", searchText) : null;
    }

}

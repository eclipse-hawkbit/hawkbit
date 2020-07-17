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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetManagementStateDataProvider;
import org.springframework.util.StringUtils;

/**
 * Filter params for {@link DistributionSetManagementStateDataProvider}.
 */
public class DsManagementFilterParams implements Serializable {
    private static final long serialVersionUID = 1L;

    private String searchText;
    private boolean noTagClicked;
    private Collection<String> distributionSetTags;
    private String pinnedTargetControllerId;

    /**
     * Constructor.
     * 
     * @param searchText
     *          String as search text
     * @param noTagClicked
     *          boolean
     * @param distributionSetTags
     *          List of String as distribution tags
     * @param pinnedTargetControllerId
     *          String as pinned target controller Id
     */
    public DsManagementFilterParams(final String searchText, final boolean noTagClicked,
            final List<String> distributionSetTags, final String pinnedTargetControllerId) {
        this.searchText = searchText;
        this.noTagClicked = noTagClicked;
        this.distributionSetTags = distributionSetTags;
        this.pinnedTargetControllerId = pinnedTargetControllerId;
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
     *          String
     */
    public void setSearchText(final String searchText) {
        this.searchText = !StringUtils.isEmpty(searchText) ? String.format("%%%s%%", searchText) : null;
    }

    /**
     * @return DistributionSetTags
     */
    public Collection<String> getDistributionSetTags() {
        return distributionSetTags;
    }

    /**
     * Setter for distributionSetTags
     *
     * @param distributionSetTags
     *          collection of String
     */
    public void setDistributionSetTags(final Collection<String> distributionSetTags) {
        this.distributionSetTags = distributionSetTags;
    }

    /**
     * @return PinnedTargetControllerId
     */
    public String getPinnedTargetControllerId() {
        return pinnedTargetControllerId;
    }

    /**
     * Setter for pinnedTargetControllerId
     *
     * @param pinnedTargetControllerId
     *          String
     */
    public void setPinnedTargetControllerId(final String pinnedTargetControllerId) {
        this.pinnedTargetControllerId = pinnedTargetControllerId;
    }

    /**
     * Constructor for DsManagementFilterParams
     */
    public DsManagementFilterParams() {
        this("", false, new ArrayList<>(), "");
    }

    /**
     * @return noTagClicked
     */
    public boolean isNoTagClicked() {
        return noTagClicked;
    }

    /**
     * Setter for noTagClicked
     *
     * @param noTagClicked
     *          boolean
     */
    public void setNoTagClicked(final boolean noTagClicked) {
        this.noTagClicked = noTagClicked;
    }
}

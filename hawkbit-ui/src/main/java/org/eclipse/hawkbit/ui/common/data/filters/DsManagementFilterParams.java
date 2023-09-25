/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetManagementStateDataProvider;

import com.google.common.base.MoreObjects;

/**
 * Filter params for {@link DistributionSetManagementStateDataProvider}.
 */
public class DsManagementFilterParams extends DsFilterParams {
    private static final long serialVersionUID = 1L;

    private boolean noTagClicked;
    private Collection<String> distributionSetTags;
    private String pinnedTargetControllerId;

    /**
     * Constructor for DsManagementFilterParams
     */
    public DsManagementFilterParams() {
        this("", false, new ArrayList<>(), "");
    }

    /**
     * Constructor.
     *
     * @param searchText
     *            String as search text
     * @param noTagClicked
     *            boolean
     * @param distributionSetTags
     *            List of String as distribution tags
     * @param pinnedTargetControllerId
     *            String as pinned target controller Id
     */
    public DsManagementFilterParams(final String searchText, final boolean noTagClicked,
            final Collection<String> distributionSetTags, final String pinnedTargetControllerId) {
        super(searchText);
        this.noTagClicked = noTagClicked;
        this.distributionSetTags = distributionSetTags;
        this.pinnedTargetControllerId = pinnedTargetControllerId;
    }

    /**
     * Copy Constructor.
     *
     * @param filter
     *            A filter to be copied
     */
    public DsManagementFilterParams(final DsManagementFilterParams filter) {
        super(filter);
        this.noTagClicked = filter.isNoTagClicked();
        this.distributionSetTags = filter.getDistributionSetTags() != null
                ? new ArrayList<>(filter.getDistributionSetTags())
                : null;
        this.pinnedTargetControllerId = filter.getPinnedTargetControllerId();
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
     *            collection of String
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
     *            String
     */
    public void setPinnedTargetControllerId(final String pinnedTargetControllerId) {
        this.pinnedTargetControllerId = pinnedTargetControllerId;
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
     *            boolean
     */
    public void setNoTagClicked(final boolean noTagClicked) {
        this.noTagClicked = noTagClicked;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DsManagementFilterParams other = (DsManagementFilterParams) obj;
        return Objects.equals(this.getSearchText(), other.getSearchText())
                && Objects.equals(this.isNoTagClicked(), other.isNoTagClicked())
                && Objects.equals(this.getDistributionSetTags(), other.getDistributionSetTags())
                && Objects.equals(this.getPinnedTargetControllerId(), other.getPinnedTargetControllerId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSearchText(), isNoTagClicked(), getDistributionSetTags(), getPinnedTargetControllerId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("searchText", getSearchText()).add("noTagClicked", isNoTagClicked())
                .add("distributionSetTags", getDistributionSetTags())
                .add("pinnedTargetControllerId", getPinnedTargetControllerId()).toString();
    }
}

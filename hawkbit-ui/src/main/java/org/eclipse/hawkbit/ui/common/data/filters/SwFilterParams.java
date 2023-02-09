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
import java.util.Objects;

import org.eclipse.hawkbit.ui.common.data.providers.SoftwareModuleDataProvider;

import com.google.common.base.MoreObjects;
import org.springframework.util.StringUtils;

/**
 * Filter params for {@link SoftwareModuleDataProvider}.
 */
public class SwFilterParams implements Serializable {
    private static final long serialVersionUID = 1L;

    private String searchText;
    private Long softwareModuleTypeId;
    private Long lastSelectedDistributionId;

    /**
     * Constructor for SwFilterParams to initialize
     */
    public SwFilterParams() {
        this(null, null, null);
    }

    /**
     * Constructor.
     * 
     * @param searchText
     *            String as search text
     * @param softwareModuleTypeId
     *            Long as software module Id
     * @param lastSelectedDistributionId
     *            Long as Last selected Distribution Id
     */
    public SwFilterParams(final String searchText, final Long softwareModuleTypeId,
            final Long lastSelectedDistributionId) {
        this.searchText = !StringUtils.isEmpty(searchText) ? String.format("%%%s%%", searchText) : null;
        this.softwareModuleTypeId = softwareModuleTypeId;
        this.lastSelectedDistributionId = lastSelectedDistributionId;
    }

    /**
     * Copy Constructor.
     *
     * @param filter
     *            A filter to be copied
     */
    public SwFilterParams(final SwFilterParams filter) {
        this.searchText = filter.getSearchText();
        this.softwareModuleTypeId = filter.getSoftwareModuleTypeId();
        this.lastSelectedDistributionId = filter.getLastSelectedDistributionId();
    }

    /**
     * Get SearchText
     *
     * @return searchText
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

    /**
     * Get softwareModuleTypeId
     *
     * @return softwareModuleTypeId
     */
    public Long getSoftwareModuleTypeId() {
        return softwareModuleTypeId;
    }

    /**
     * Setter for softwareModuleTypeId
     *
     * @param softwareModuleTypeId
     *            Long
     */
    public void setSoftwareModuleTypeId(final Long softwareModuleTypeId) {
        this.softwareModuleTypeId = softwareModuleTypeId;
    }

    /**
     * Get lastSelectedDistributionId
     *
     * @return lastSelectedDistributionId
     */
    public Long getLastSelectedDistributionId() {
        return lastSelectedDistributionId;
    }

    /**
     * Setter for lastSelectedDistributionId
     *
     * @param lastSelectedDistributionId
     *            Long
     */
    public void setLastSelectedDistributionId(final Long lastSelectedDistributionId) {
        this.lastSelectedDistributionId = lastSelectedDistributionId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SwFilterParams other = (SwFilterParams) obj;
        return Objects.equals(this.getSearchText(), other.getSearchText())
                && Objects.equals(this.getSoftwareModuleTypeId(), other.getSoftwareModuleTypeId())
                && Objects.equals(this.getLastSelectedDistributionId(), other.getLastSelectedDistributionId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSearchText(), getSoftwareModuleTypeId(), getLastSelectedDistributionId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("searchText", getSearchText())
                .add("softwareModuleTypeId", getSoftwareModuleTypeId())
                .add("lastSelectedDistributionId", getLastSelectedDistributionId()).toString();
    }
}

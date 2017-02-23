/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement.state;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 *
 *
 */
@VaadinSessionScope
@SpringComponent
public class FilterManagementUIState implements Serializable {

    private static final long serialVersionUID = 2477103280605559284L;

    private String targetFilterSearchText;

    private boolean createFilterViewDisplayed;

    private boolean editViewDisplayed;

    private TargetFilterQuery targetFilterQuery;

    private Long targetsTruncated;

    private final AtomicLong targetsCountAll = new AtomicLong();

    private String filterQueryValue;

    private Boolean isFilterByInvalidFilterQuery = Boolean.FALSE;

    /**
     * @return the isFilterByInvalidFilterQuery
     */
    public Boolean getIsFilterByInvalidFilterQuery() {
        return isFilterByInvalidFilterQuery;
    }

    /**
     * @param isFilterByInvalidFilterQuery
     *            the isFilterByInvalidFilterQuery to set
     */
    public void setIsFilterByInvalidFilterQuery(final Boolean isFilterByInvalidFilterQuery) {
        this.isFilterByInvalidFilterQuery = isFilterByInvalidFilterQuery;
    }

    /**
     * @return the isEditViewDisplayed
     */
    public boolean isEditViewDisplayed() {
        return editViewDisplayed;
    }

    /**
     * @param isEditViewDisplayed
     *            the isEditViewDisplayed to set
     */
    public void setEditViewDisplayed(final boolean isEditViewDisplayed) {
        this.editViewDisplayed = isEditViewDisplayed;
    }

    /**
     * @return the rsqlSearch
     */
    public String getFilterQueryValue() {
        return filterQueryValue;
    }

    /**
     * @param rsqlSearch
     *            the rsqlSearch to set
     */
    public void setFilterQueryValue(final String filterQueryValue) {
        this.filterQueryValue = filterQueryValue;
    }

    /**
     * @return the targetsCountAll
     */
    public AtomicLong getTargetsCountAll() {
        return targetsCountAll;
    }

    /**
     * @param targetsCountAll
     *            the targetsCountAll to set
     */
    public void setTargetsCountAll(final long targetsCountAll) {
        this.targetsCountAll.set(targetsCountAll);
    }

    /**
     * @return the targetsTruncated
     */
    public Long getTargetsTruncated() {
        return targetsTruncated;
    }

    /**
     * @param targetsTruncated
     *            the targetsTruncated to set
     */
    public void setTargetsTruncated(final Long targetsTruncated) {
        this.targetsTruncated = targetsTruncated;
    }

    /**
     * @return the tfQuery
     */
    public Optional<TargetFilterQuery> getTfQuery() {
        return Optional.ofNullable(targetFilterQuery);
    }

    /**
     * @param tfQuery
     *            the tfQuery to set
     */
    public void setTfQuery(final TargetFilterQuery tfQuery) {
        this.targetFilterQuery = tfQuery;
    }

    public boolean isCreateFilterViewDisplayed() {
        return createFilterViewDisplayed;
    }

    public void setCreateFilterBtnClicked(final boolean isCreateFilterBtnClicked) {
        this.createFilterViewDisplayed = isCreateFilterBtnClicked;
    }

    public Optional<String> getCustomFilterSearchText() {
        return Optional.ofNullable(targetFilterSearchText);
    }

    public void setCustomFilterSearchText(final String updateCustomFilterSearchText) {
        this.targetFilterSearchText = updateCustomFilterSearchText;
    }

}

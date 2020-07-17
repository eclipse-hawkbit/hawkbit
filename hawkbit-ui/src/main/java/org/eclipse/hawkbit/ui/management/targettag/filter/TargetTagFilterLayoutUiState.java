/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.filter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.common.state.TagFilterLayoutUiState;

/**
 * Target tag filter layout ui state
 */
public class TargetTagFilterLayoutUiState extends TagFilterLayoutUiState {
    private static final long serialVersionUID = 1L;

    private Long clickedTargetFilterQueryId;
    private final List<TargetUpdateStatus> clickedTargetUpdateStatusFilters = new ArrayList<>();
    private boolean isOverdueFilterClicked;
    private boolean isCustomFilterTabSelected;

    /**
     * @return Id of clicked target filter query
     */
    public Long getClickedTargetFilterQueryId() {
        return clickedTargetFilterQueryId;
    }

    /**
     * Sets the id of clicked target filter query
     *
     * @param clickedTargetFilterQueryId
     *          Query id
     */
    public void setClickedTargetFilterQueryId(final Long clickedTargetFilterQueryId) {
        this.clickedTargetFilterQueryId = clickedTargetFilterQueryId;
    }

    /**
     * @return Clicked target update status filters
     */
    public List<TargetUpdateStatus> getClickedTargetUpdateStatusFilters() {
        return clickedTargetUpdateStatusFilters;
    }

    /**
     * Sets the clicked target update status filters
     *
     * @param clickedTargetUpdateStatusFilters
     *          List of target update status filters
     */
    public void setClickedTargetUpdateStatusFilters(final List<TargetUpdateStatus> clickedTargetUpdateStatusFilters) {
        this.clickedTargetUpdateStatusFilters.clear();
        this.clickedTargetUpdateStatusFilters.addAll(clickedTargetUpdateStatusFilters);
    }

    /**
     * @return True if overdue filter is clicked else false
     */
    public boolean isOverdueFilterClicked() {
        return isOverdueFilterClicked;
    }

    /**
     * Sets the status of overdue filter clicked
     *
     * @param isOverdueFilterClicked
     *          boolean
     */
    public void setOverdueFilterClicked(final boolean isOverdueFilterClicked) {
        this.isOverdueFilterClicked = isOverdueFilterClicked;
    }

    /**
     * @return True if custom filter tab is selected else false
     */
    public boolean isCustomFilterTabSelected() {
        return isCustomFilterTabSelected;
    }

    /**
     * Sets the status of custom filter tab selected
     *
     * @param isCustomFilterTabSelected
     *          boolean
     */
    public void setCustomFilterTabSelected(final boolean isCustomFilterTabSelected) {
        this.isCustomFilterTabSelected = isCustomFilterTabSelected;
    }
}

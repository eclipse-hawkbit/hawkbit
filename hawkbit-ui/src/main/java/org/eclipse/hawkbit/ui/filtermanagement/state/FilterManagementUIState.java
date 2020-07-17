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

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * Filter management ui state
 */
@VaadinSessionScope
@SpringComponent
public class FilterManagementUIState implements Serializable {

    private static final long serialVersionUID = 2477103280605559284L;

    /**
     * Constants for filter view
     */
    public enum FilterView {
        FILTERS, DETAILS
    }

    private final TargetFilterGridLayoutUiState gridLayoutUiState;

    private final TargetFilterDetailsLayoutUiState detailsLayoutUiState;

    private FilterView currentView;

    FilterManagementUIState() {
        this.gridLayoutUiState = new TargetFilterGridLayoutUiState();
        this.detailsLayoutUiState = new TargetFilterDetailsLayoutUiState();
    }

    /**
     * @return Current filter view
     */
    public FilterView getCurrentView() {
        return currentView;
    }

    /**
     * Sets the current filter view
     *
     * @param currentView
     *          FilterView
     */
    public void setCurrentView(final FilterView currentView) {
        this.currentView = currentView;
    }

    /**
     * @return Target filter grid layout ui state
     */
    public TargetFilterGridLayoutUiState getGridLayoutUiState() {
        return gridLayoutUiState;
    }

    /**
     * @return Target filter details layout ui state
     */
    public TargetFilterDetailsLayoutUiState getDetailsLayoutUiState() {
        return detailsLayoutUiState;
    }

}

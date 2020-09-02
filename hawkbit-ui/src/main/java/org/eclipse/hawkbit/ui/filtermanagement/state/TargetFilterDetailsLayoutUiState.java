/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement.state;

import java.io.Serializable;

/**
 * Target filter details layout ui state
 */
public class TargetFilterDetailsLayoutUiState implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constants for filter current mode
     */
    public enum Mode {
        CREATE, EDIT
    }

    private Mode currentMode;
    private Long selectedFilterId;
    private String selectedFilterName;
    private String nameInput;
    private String filterQueryValueInput;

    /**
     * @return Current filter mode
     */
    public Mode getCurrentMode() {
        return currentMode;
    }

    /**
     * Sets the current filter mode
     *
     * @param currentMode
     *          Mode
     */
    public void setCurrentMode(final Mode currentMode) {
        this.currentMode = currentMode;
    }

    /**
     * @return Id of selected filter
     */
    public Long getSelectedFilterId() {
        return selectedFilterId;
    }

    /**
     * Sets the selected filter id
     *
     * @param selectedFilterId
     *          Filter id
     */
    public void setSelectedFilterId(final Long selectedFilterId) {
        this.selectedFilterId = selectedFilterId;
    }

    /**
     * @return Selected filter name
     */
    public String getSelectedFilterName() {
        return selectedFilterName;
    }

    /**
     * Sets the name of selected filter
     *
     * @param selectedFilterName
     *          Filter name
     */
    public void setSelectedFilterName(final String selectedFilterName) {
        this.selectedFilterName = selectedFilterName;
    }

    /**
     * @return Filter name of input
     */
    public String getNameInput() {
        return nameInput == null ? "" : nameInput;
    }

    /**
     * Sets the filter name in input
     *
     * @param nameInput
     *          input value
     */
    public void setNameInput(final String nameInput) {
        this.nameInput = nameInput;
    }

    /**
     * @return Filter query value of input
     */
    public String getFilterQueryValueInput() {
        return filterQueryValueInput == null ? "" : filterQueryValueInput;
    }

    /**
     * Sets the filter query value in input
     *
     * @param filterQueryValueInput
     *          Query value
     */
    public void setFilterQueryValueInput(final String filterQueryValueInput) {
        this.filterQueryValueInput = filterQueryValueInput;
    }
}

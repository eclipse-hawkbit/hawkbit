/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.filtermanagement.state;

import java.io.Serializable;

/**
 * Target filter grid layout ui state
 */
public class TargetFilterGridLayoutUiState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String searchFilterInput;

    /**
     * @return Search filter input value
     */
    public String getSearchFilterInput() {
        return searchFilterInput;
    }

    /**
     * Sets the search filter input value
     *
     * @param searchFilterInput
     *          Filter value
     */
    public void setSearchFilterInput(final String searchFilterInput) {
        this.searchFilterInput = searchFilterInput;
    }
}

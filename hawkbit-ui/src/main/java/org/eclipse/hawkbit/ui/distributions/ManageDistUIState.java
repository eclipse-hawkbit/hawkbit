/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.distributions;

import java.io.Serializable;

import org.eclipse.hawkbit.ui.common.state.GridLayoutUiState;
import org.eclipse.hawkbit.ui.common.state.TypeFilterLayoutUiState;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * Manage Distributions user state.
 */
@SpringComponent
@VaadinSessionScope
public class ManageDistUIState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final TypeFilterLayoutUiState dsTypeFilterLayoutUiState;
    private final GridLayoutUiState distributionSetGridLayoutUiState;
    private final GridLayoutUiState swModuleGridLayoutUiState;
    private final TypeFilterLayoutUiState smTypeFilterLayoutUiState;

    ManageDistUIState() {
        this.dsTypeFilterLayoutUiState = new TypeFilterLayoutUiState();
        this.distributionSetGridLayoutUiState = new GridLayoutUiState();
        this.swModuleGridLayoutUiState = new GridLayoutUiState();
        this.smTypeFilterLayoutUiState = new TypeFilterLayoutUiState();
    }

    /**
     * @return Distribution set type filter layout ui state
     */
    public TypeFilterLayoutUiState getDsTypeFilterLayoutUiState() {
        return dsTypeFilterLayoutUiState;
    }

    /**
     * @return Distribution set grid layout ui state
     */
    public GridLayoutUiState getDistributionSetGridLayoutUiState() {
        return distributionSetGridLayoutUiState;
    }

    /**
     * @return Software module grid layout ui state
     */
    public GridLayoutUiState getSwModuleGridLayoutUiState() {
        return swModuleGridLayoutUiState;
    }

    /**
     * @return Software module type filter layout ui state
     */
    public TypeFilterLayoutUiState getSmTypeFilterLayoutUiState() {
        return smTypeFilterLayoutUiState;
    }
}

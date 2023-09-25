/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.state;

/**
 * Type filter layout ui state
 */
public class TypeFilterLayoutUiState extends HidableLayoutUiState {
    private static final long serialVersionUID = 1L;

    private Long clickedTypeId;

    /**
     * @return Id of clicked type filter
     */
    public Long getClickedTypeId() {
        return clickedTypeId;
    }

    /**
     * Sets the id of clicked type filter
     *
     * @param clickedTypeId
     *          Id
     */
    public void setClickedTypeId(final Long clickedTypeId) {
        this.clickedTypeId = clickedTypeId;
    }
}

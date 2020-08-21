/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

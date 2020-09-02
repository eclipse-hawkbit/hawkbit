/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import org.eclipse.hawkbit.ui.common.state.GridLayoutUiState;

/**
 * Distribution grid layout ui state
 */
public class DistributionGridLayoutUiState extends GridLayoutUiState {
    private static final long serialVersionUID = 1L;

    private Long pinnedDsId;

    /**
     * @return pinned distribution id
     */
    public Long getPinnedDsId() {
        return pinnedDsId;
    }

    /**
     * Sets the pinned distribution id
     *
     * @param pinnedDsId
     *          id
     */
    public void setPinnedDsId(final Long pinnedDsId) {
        this.pinnedDsId = pinnedDsId;
    }
}

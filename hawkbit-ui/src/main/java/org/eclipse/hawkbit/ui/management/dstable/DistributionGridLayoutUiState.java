/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdNameVersion;
import org.eclipse.hawkbit.ui.common.state.GridLayoutUiState;

/**
 * Target grid layout ui state
 */
public class TargetGridLayoutUiState extends GridLayoutUiState {
    private static final long serialVersionUID = 1L;

    private Long pinnedTargetId;
    private String pinnedControllerId;
    private ProxyIdNameVersion filterDsIdNameVersion;

    /**
     * @return Pinned controller id
     */
    public String getPinnedControllerId() {
        return pinnedControllerId;
    }

    /**
     * Sets the pinned controller id
     *
     * @param pinnedControllerId
     *          Id
     */
    public void setPinnedControllerId(final String pinnedControllerId) {
        this.pinnedControllerId = pinnedControllerId;
    }

    /**
     * @return Pinned target id
     */
    public Long getPinnedTargetId() {
        return pinnedTargetId;
    }

    /**
     * Sets the pinned target id
     *
     * @param pinnedTargetId
     *          Id
     */
    public void setPinnedTargetId(final Long pinnedTargetId) {
        this.pinnedTargetId = pinnedTargetId;
    }

    /**
     * @return filter distribution set id name and version
     */
    public ProxyIdNameVersion getFilterDsIdNameVersion() {
        return filterDsIdNameVersion;
    }

    /**
     * Sets the filter distribution set id name and version
     *
     * @param filterDsIdNameVersion
     *          ProxyIdNameVersion
     */
    public void setFilterDsIdNameVersion(final ProxyIdNameVersion filterDsIdNameVersion) {
        this.filterDsIdNameVersion = filterDsIdNameVersion;
    }
}

/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.grid.selection.client;

import com.vaadin.shared.communication.ServerRpc;

/**
 * Interface defining RPC calls to communicate the request of a user to select
 * items in a grid.
 *
 */
public interface RangeSelectionServerRpc extends ServerRpc {
    /**
     * Select all items that lay between two items
     * 
     * @param startIndex
     *            index of first item
     * @param endIndex
     *            index of last item
     */
    void selectRange(int startIndex, int endIndex);

    /**
     * Select all items
     */
    void selectAll();

    /**
     * Show the warning notification in case the selection limit is reached
     */
    void showMaxSelectionLimitWarning();
}

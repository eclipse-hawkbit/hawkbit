/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

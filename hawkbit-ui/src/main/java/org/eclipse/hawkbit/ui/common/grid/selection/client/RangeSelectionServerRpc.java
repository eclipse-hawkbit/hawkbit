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
     * @param overwrite
     *            if <code>false</code> the items are selected additionally to
     *            the ones that were selected before. Else the previous
     *            selection is overwritten
     */
    void selectRange(int startIndex, int endIndex, boolean overwrite);

    /**
     * select all items
     */
    void selectAll();
}

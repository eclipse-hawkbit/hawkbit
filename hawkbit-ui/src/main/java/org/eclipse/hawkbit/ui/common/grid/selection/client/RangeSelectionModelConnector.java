/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.selection.client;

import org.eclipse.hawkbit.ui.common.grid.selection.RangeSelectionModel;

import com.vaadin.client.connectors.grid.MultiSelectionModelConnector;
import com.vaadin.client.widgets.Grid;
import com.vaadin.shared.ui.Connect;

import elemental.json.JsonObject;

/**
 * 
 * Client side Connector to that connects to {@link RangeSelectionModel} and
 * extends {@link MultiSelectionModelConnector} to also allow selection of a
 * range of items by pressing CTRL
 *
 */
@Connect(RangeSelectionModel.class)
public class RangeSelectionModelConnector extends MultiSelectionModelConnector {
    private static final long serialVersionUID = 1L;

    @Override
    protected void initSelectionModel() {
        super.initSelectionModel();

        final RangeSelectionHandler selectionHandler = new RangeSelectionHandler(
                getRpcProxy(RangeSelectionServerRpc.class), () -> getState().getSelectionCount());
        final Grid<JsonObject> grid = getGrid();

        grid.addBodyClickHandler(selectionHandler);
        grid.addBodyKeyDownHandler(selectionHandler);
        grid.addBodyKeyUpHandler(selectionHandler);
        grid.getSelectionColumn().ifPresent(selectionCol -> {
            selectionCol.setHidable(false);
            selectionCol.setHidden(true);
        });
    }

    @Override
    public RangeSelectionState getState() {
        return (RangeSelectionState) super.getState();
    }
}

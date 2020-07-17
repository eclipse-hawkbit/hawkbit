/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.selection.client;

import com.vaadin.client.widget.grid.CellReference;
import com.vaadin.client.widget.grid.events.BodyClickHandler;
import com.vaadin.client.widget.grid.events.BodyKeyDownHandler;
import com.vaadin.client.widget.grid.events.BodyKeyUpHandler;
import com.vaadin.client.widget.grid.events.GridClickEvent;
import com.vaadin.client.widget.grid.events.GridKeyDownEvent;
import com.vaadin.client.widget.grid.events.GridKeyUpEvent;
import com.vaadin.client.widget.grid.selection.SelectionModel;
import com.vaadin.event.ShortcutAction.KeyCode;

import elemental.json.JsonObject;

/**
 * Client side handler that detects selection requests of the user on grid items
 * and forwards them to the server side.
 *
 */
public class RangeSelectionHandler implements BodyClickHandler, BodyKeyDownHandler, BodyKeyUpHandler {
    private final RangeSelectionServerRpc rangeSelectionServerRpc;

    private int previousRowIndex;

    /**
     * Constructor
     * 
     * @param rangeSelectionServerRpc
     *            RPC server to forward selection requests to
     */
    public RangeSelectionHandler(final RangeSelectionServerRpc rangeSelectionServerRpc) {
        this.rangeSelectionServerRpc = rangeSelectionServerRpc;
    }

    @Override
    public void onClick(final GridClickEvent event) {
        final CellReference<JsonObject> eventCell = (CellReference<JsonObject>) event.getTargetCell();
        final SelectionModel<JsonObject> selectionModel = eventCell.getGrid().getSelectionModel();
        final int currentRowIndex = eventCell.getRowIndex();
        final JsonObject item = eventCell.getRow();

        if (event.isShiftKeyDown()) {
            rangeSelectionServerRpc.selectRange(previousRowIndex, currentRowIndex, !event.isControlKeyDown());
            return;
        }

        if (event.isControlKeyDown() || event.isMetaKeyDown()) {
            if (selectionModel.isSelected(item)) {
                selectionModel.deselect(item);
            } else {
                selectionModel.select(item);
            }
        } else {
            selectionModel.deselectAll();
            selectionModel.select(item);
        }

        previousRowIndex = currentRowIndex;
    }

    @Override
    public void onKeyDown(final GridKeyDownEvent event) {
        if ((event.isControlKeyDown() || event.isMetaKeyDown()) && (event.getNativeKeyCode() == KeyCode.A)) {
            rangeSelectionServerRpc.selectAll();
        }
    }

    @Override
    public void onKeyUp(final GridKeyUpEvent event) {
        if (event.isDownArrow() || event.isUpArrow()) {
            final CellReference<JsonObject> eventCell = (CellReference<JsonObject>) event.getFocusedCell();
            final SelectionModel<JsonObject> selectionModel = eventCell.getGrid().getSelectionModel();
            final int currentRowIndex = eventCell.getRowIndex();
            final JsonObject item = eventCell.getRow();

            selectionModel.deselectAll();
            selectionModel.select(item);

            previousRowIndex = currentRowIndex;
        }
    }

}

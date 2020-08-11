/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.selection.client;

import java.util.function.IntSupplier;

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
    private final IntSupplier selectionCount;

    private int previousRowIndex;

    /**
     * Constructor
     * 
     * @param rangeSelectionServerRpc
     *            RPC server to forward selection requests to
     * @param selectionCount
     *            selection count callback to get the number of currently
     *            selected items from the shared state
     */
    public RangeSelectionHandler(final RangeSelectionServerRpc rangeSelectionServerRpc,
            final IntSupplier selectionCount) {
        this.rangeSelectionServerRpc = rangeSelectionServerRpc;
        this.selectionCount = selectionCount;
    }

    @Override
    public void onClick(final GridClickEvent event) {
        final CellReference<JsonObject> eventCell = (CellReference<JsonObject>) event.getTargetCell();
        final SelectionModel<JsonObject> selectionModel = eventCell.getGrid().getSelectionModel();
        final int currentRowIndex = eventCell.getRowIndex();
        final JsonObject item = eventCell.getRow();
        final boolean preserveSelection = shouldPreserveSelection(event);

        if (event.isShiftKeyDown() && !preserveSelection) {
            rangeSelectionServerRpc.selectRange(previousRowIndex, currentRowIndex);
            return;
        }

        previousRowIndex = currentRowIndex;

        if (preserveSelection) {
            adaptSelectionByItem(selectionModel, item);
            return;
        }

        selectSingleItemOnly(selectionModel, item);
    }

    private boolean shouldPreserveSelection(final GridClickEvent event) {
        return event.isControlKeyDown() || event.isMetaKeyDown();
    }

    private void adaptSelectionByItem(final SelectionModel<JsonObject> selectionModel, final JsonObject item) {
        if (selectionModel.isSelected(item)) {
            selectionModel.deselect(item);
            return;
        }

        if (isMaxSelectionLimitReached()) {
            rangeSelectionServerRpc.showMaxSelectionLimitWarning();
            return;
        }

        selectionModel.select(item);
    }

    private boolean isMaxSelectionLimitReached() {
        return selectionCount.getAsInt() + 1 > RangeSelectionState.MAX_SELECTION_LIMIT;
    }

    private static void selectSingleItemOnly(final SelectionModel<JsonObject> selectionModel, final JsonObject item) {
        selectionModel.deselectAll();
        selectionModel.select(item);
    }

    @Override
    public void onKeyDown(final GridKeyDownEvent event) {
        if ((event.isControlKeyDown() || event.isMetaKeyDown()) && (event.getNativeKeyCode() == KeyCode.A)) {
            event.preventDefault();
            event.stopPropagation();

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

            if (event.isShiftKeyDown()) {
                rangeSelectionServerRpc.selectRange(previousRowIndex, currentRowIndex);
                return;
            }

            previousRowIndex = currentRowIndex;

            selectSingleItemOnly(selectionModel, item);
        }
    }
}

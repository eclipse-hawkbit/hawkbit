/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.renderers.ProgressBarRenderer;

import elemental.json.JsonValue;

/**
 * Shows upload status during upload.
 *
 *
 *
 */

@ViewScope
@SpringComponent
public class UploadStatusInfoWindow extends Window implements Window.CloseListener, Window.WindowModeChangeListener {

    private static final String PROGRESS = "Progress";

    private static final String FILE_NAME = "File name";

    private static final String STATUS = "Status";

    private static final String REASON = "Reason";

    private static final long serialVersionUID = 1L;

    private final Grid grid;

    private final IndexedContainer uploads;

    private volatile boolean errorOccured = false;

    /**
     * Default Constructor.
     */
    UploadStatusInfoWindow() {
        super("Upload Status");

        addStyleName(SPUIStyleDefinitions.UPLOAD_INFO);
        center();
        setImmediate(true);
        setResizable(true);
        setDraggable(true);
        setClosable(true);
        uploads = new IndexedContainer();
        uploads.addContainerProperty(STATUS, String.class, "Active");
        uploads.addContainerProperty(FILE_NAME, String.class, null);
        uploads.addContainerProperty(PROGRESS, Double.class, 0D);
        uploads.addContainerProperty(REASON, String.class, "");

        grid = new Grid(uploads);
        grid.addStyleName(SPUIStyleDefinitions.UPLOAD_STATUS_GRID);
        grid.setSelectionMode(SelectionMode.NONE);
        grid.getColumn(STATUS).setRenderer(new StatusRenderer());
        grid.getColumn(PROGRESS).setRenderer(new ProgressBarRenderer());
        setColumnWidth();
        grid.setFrozenColumnCount(4);
        grid.setColumnOrder(STATUS, PROGRESS, FILE_NAME, REASON);
        grid.setHeaderVisible(true);
        grid.setImmediate(true);
        setPopupSizeInMinMode();

        setContent(grid);
        addCloseListener(this);
        addWindowModeChangeListener(this);
    }

    private void setColumnWidth() {
        grid.getColumn(STATUS).setWidth(70);
        grid.getColumn(PROGRESS).setWidth(150);
        grid.getColumn(FILE_NAME).setWidth(280);
        grid.getColumn(REASON).setWidth(300);
    }

    private void resetColumnWidth() {
        grid.getColumn(STATUS).setWidthUndefined();
        grid.getColumn(PROGRESS).setWidthUndefined();
        grid.getColumn(FILE_NAME).setWidthUndefined();
        grid.getColumn(REASON).setWidthUndefined();

    }

    private static class StatusRenderer extends HtmlRenderer {

        @Override
        public JsonValue encode(final String value) {

            String result = "";
            switch (value) {
            case "Finished":
                result = "<div class=\"statusIconGreen\">" + FontAwesome.CHECK_CIRCLE.getHtml() + "</div>";
                break;
            case "Failed":
                result = "<div class=\"statusIconRed\">" + FontAwesome.EXCLAMATION_CIRCLE.getHtml() + "</div>";
                break;
            default:
                result = "<div class=\"statusIconActive\"></div>";
            }

            return super.encode(result);
        }
    }

    /**
     * Automatically close if not error has occured.
     */
    void uploadSessionFinished() {
        if (!errorOccured) {
            close();
        }

    }

    void uploadSessionStarted() {
        close();
        UI.getCurrent().addWindow(this);
        center();
    }

    void uploadStarted(final String filename) {
        final Item item = uploads.addItem(filename);
        item.getItemProperty(FILE_NAME).setValue(filename);
        grid.scrollToEnd();

    }

    void updateProgress(final String filename, final long readBytes, final long contentLength) {
        final Item item = uploads.getItem(filename);
        if (item != null) {
            item.getItemProperty(PROGRESS).setValue((double) readBytes / (double) contentLength);

        }
    }

    /**
     * Called when each file upload is success.
     * 
     * @param filename
     *            of the uploaded file.
     */
    public void uploadSucceeded(final String filename) {
        final Item item = uploads.getItem(filename);
        if (item != null) {
            item.getItemProperty(STATUS).setValue("Finished");

        }
    }

    void uploadFailed(final String filename, final String errorReason) {
        final Item item = uploads.getItem(filename);
        if (item != null) {
            if (!errorOccured) {
                errorOccured = true;
            }
            item.getItemProperty(REASON).setValue(errorReason);
            item.getItemProperty(STATUS).setValue("Failed");

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Window.CloseListener#windowClose(com.vaadin.ui.Window.
     * CloseEvent)
     */
    @Override
    public void windowClose(final CloseEvent e) {
        clearWindow();
    }

    private void clearWindow() {
        errorOccured = false;
        uploads.removeAllItems();
        setWindowMode(WindowMode.NORMAL);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Window.WindowModeChangeListener#windowModeChanged(com.
     * vaadin.ui.Window. WindowModeChangeEvent)
     */
    @Override
    public void windowModeChanged(final WindowModeChangeEvent event) {
        if (event.getWindow().getWindowMode() == WindowMode.MAXIMIZED) {
            resetColumnWidth();
            grid.getColumn(STATUS).setExpandRatio(0);
            grid.getColumn(PROGRESS).setExpandRatio(1);
            grid.getColumn(FILE_NAME).setExpandRatio(2);
            grid.getColumn(REASON).setExpandRatio(3);
            grid.setSizeFull();
        } else {
            setColumnWidth();
            setPopupSizeInMinMode();
        }
    }

    private void setPopupSizeInMinMode() {
        grid.setWidth(800, Unit.PIXELS);
        grid.setHeight(510, Unit.PIXELS);
    }
}

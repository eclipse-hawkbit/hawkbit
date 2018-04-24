/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.renderers.ProgressBarRenderer;
import com.vaadin.ui.themes.ValoTheme;

import elemental.json.JsonValue;

/**
 * Shows the progress of all uploads.
 */
public class UploadProgressInfoWindow extends Window implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String COLUMN_PROGRESS = "Progress";

    private static final String COLUMN_FILE_NAME = "File name";

    private static final String COLUMN_STATUS = "Status";

    private static final String COLUMN_REASON = "Reason";

    private static final String STATUS_INPROGRESS = "InProgress";

    private static final String STATUS_FINISHED = "Finished";

    private static final String STATUS_FAILED = "Failed";

    private final ArtifactUploadState artifactUploadState;

    private final VaadinMessageSource i18n;

    private final Grid grid;

    private final IndexedContainer uploads;

    private final VerticalLayout mainLayout;

    private final UI ui;

    private Label windowCaption;

    private Button minimizeButton;

    private Button closeButton;

    private Button resizeButton;

    UploadProgressInfoWindow(final UIEventBus eventBus, final ArtifactUploadState artifactUploadState,
            final VaadinMessageSource i18n) {
        this.artifactUploadState = artifactUploadState;
        this.i18n = i18n;

        setPopupProperties();
        createStatusPopupHeaderComponents();

        mainLayout = new VerticalLayout();
        mainLayout.setSpacing(Boolean.TRUE);
        mainLayout.setSizeUndefined();
        setPopupSizeInMinMode();

        uploads = getGridContainer();
        grid = createGrid();
        setGridColumnProperties();

        mainLayout.addComponents(getCaptionLayout(), grid);
        mainLayout.setExpandRatio(grid, 1.0F);
        setContent(mainLayout);
        eventBus.subscribe(this);
        ui = UI.getCurrent();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadStatusEvent event) {
        switch (event.getUploadStatusEventType()) {
        case UPLOAD_STARTED:
            ui.access(() -> onUploadStarted(event));
            break;
        case UPLOAD_IN_PROGRESS:
        case UPLOAD_FAILED:
        case UPLOAD_SUCCESSFUL:
            ui.access(() -> updateUploadProgressinforRowObject(event));
            break;
        case UPLOAD_FINISHED:
            ui.access(this::onUploadFinished);
            break;
        default:
            break;
        }
    }

    private void onUploadStarted(final UploadStatusEvent event) {
        final FileUploadProgress fileUploadProgress = event.getFileUploadProgress();
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();

        updateUploadProgressInfoRowObject(fileUploadId);

        if (isFirstFileUpload()) {
            maximizeWindow();
        }

        grid.scrollTo(fileUploadId);
    }

    private void restoreState() {
        final Indexed container = grid.getContainerDataSource();
        container.removeAllItems();
        for (final FileUploadId fileUploadId : artifactUploadState.getAllFileUploadIdsFromOverallUploadProcessList()) {
            updateUploadProgressInfoRowObject(fileUploadId);
        }
    }

    private void setPopupProperties() {
        setId(UIComponentIdProvider.UPLOAD_STATUS_POPUP_ID);
        addStyleName(SPUIStyleDefinitions.UPLOAD_INFO);
        setImmediate(true);
        setResizable(false);
        setDraggable(true);
        setClosable(false);
        setModal(true);
    }

    private void setGridColumnProperties() {
        grid.getColumn(COLUMN_STATUS).setRenderer(new StatusRenderer());
        grid.getColumn(COLUMN_PROGRESS).setRenderer(new ProgressBarRenderer());
        grid.setColumnOrder(COLUMN_STATUS, COLUMN_PROGRESS, COLUMN_FILE_NAME, SPUILabelDefinitions.NAME_VERSION,
                COLUMN_REASON);
        setColumnWidth();
        grid.getColumn(SPUILabelDefinitions.NAME_VERSION)
                .setHeaderCaption(i18n.getMessage("upload.swModuleTable.header"));
        grid.setFrozenColumnCount(5);
    }

    private Grid createGrid() {
        final Grid statusGrid = new Grid(uploads);
        statusGrid.addStyleName(SPUIStyleDefinitions.UPLOAD_STATUS_GRID);
        statusGrid.setSelectionMode(SelectionMode.NONE);
        statusGrid.setHeaderVisible(true);
        statusGrid.setImmediate(true);
        statusGrid.setSizeFull();
        return statusGrid;
    }

    private IndexedContainer getGridContainer() {
        final IndexedContainer uploadContainer = new IndexedContainer();
        uploadContainer.addContainerProperty(COLUMN_STATUS, String.class, "Active");
        uploadContainer.addContainerProperty(COLUMN_FILE_NAME, String.class, null);
        uploadContainer.addContainerProperty(COLUMN_PROGRESS, Double.class, 0D);
        uploadContainer.addContainerProperty(COLUMN_REASON, String.class, "");
        uploadContainer.addContainerProperty(SPUILabelDefinitions.NAME_VERSION, String.class, "");
        return uploadContainer;
    }

    private HorizontalLayout getCaptionLayout() {
        final HorizontalLayout captionLayout = new HorizontalLayout();
        captionLayout.setSizeFull();
        captionLayout.setHeight("36px");
        captionLayout.addComponents(windowCaption, minimizeButton, resizeButton, closeButton);
        captionLayout.setExpandRatio(windowCaption, 1.0F);

        captionLayout.addStyleName("v-window-header");
        return captionLayout;
    }

    private void createStatusPopupHeaderComponents() {
        minimizeButton = getMinimizeButton();
        windowCaption = new Label("Upload status");
        closeButton = getCloseButton();
        resizeButton = getResizeButton();
    }

    private void setColumnWidth() {
        grid.getColumn(COLUMN_STATUS).setWidth(60);
        grid.getColumn(COLUMN_PROGRESS).setWidth(150);
        grid.getColumn(COLUMN_FILE_NAME).setWidth(200);
        grid.getColumn(COLUMN_REASON).setWidth(290);
        grid.getColumn(SPUILabelDefinitions.NAME_VERSION).setWidth(200);
    }

    private void resetColumnWidth() {
        grid.getColumn(COLUMN_STATUS).setWidthUndefined();
        grid.getColumn(COLUMN_PROGRESS).setWidthUndefined();
        grid.getColumn(COLUMN_FILE_NAME).setWidthUndefined();
        grid.getColumn(COLUMN_REASON).setWidthUndefined();
        grid.getColumn(SPUILabelDefinitions.NAME_VERSION).setWidthUndefined();
    }

    private static class StatusRenderer extends HtmlRenderer {

        private static final long serialVersionUID = -5365795450234970943L;

        @Override
        public JsonValue encode(final String value) {
            if (value == null) {
                return super.encode(getNullRepresentation());
            }

            final String result;
            switch (value) {
            case STATUS_FINISHED:
                result = "<div class=\"statusIconGreen\">" + FontAwesome.CHECK_CIRCLE.getHtml() + "</div>";
                break;
            case STATUS_FAILED:
                result = "<div class=\"statusIconRed\">" + FontAwesome.EXCLAMATION_CIRCLE.getHtml() + "</div>";
                break;
            case STATUS_INPROGRESS:
                result = "<div class=\"statusIconActive\"></div>";
                break;
            default:
                throw new IllegalArgumentException("Argument " + value + " wasn't expected.");
            }

            return super.encode(result);
        }
    }

    private boolean isFirstFileUpload() {
        return artifactUploadState.getAllFileUploadIdsFromOverallUploadProcessList().size() == 1;
    }

    private void openWindow() {
        UI.getCurrent().addWindow(this);
        center();
    }

    protected void maximizeWindow() {
        openWindow();
        restoreState();
        artifactUploadState.setStatusPopupMinimized(false);
    }

    private void minimizeWindow() {
        artifactUploadState.setStatusPopupMinimized(true);
        closeWindow();

        if (artifactUploadState.areAllUploadsFinished()) {
            cleanupStates();
        }
    }

    private void updateUploadProgressinforRowObject(final UploadStatusEvent event) {
        final FileUploadProgress fileUploadProgress = event.getFileUploadProgress();
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();

        updateUploadProgressInfoRowObject(fileUploadId);
    }

    /**
     * Called for every finished (succeeded or failed) upload.
     */
    private void onUploadFinished() {
        if (artifactUploadState.areAllUploadsFinished() && artifactUploadState.isStatusPopupMinimized()) {
            if (artifactUploadState.getFilesInFailedState().isEmpty()) {
                cleanupStates();
                closeWindow();
            } else {
                maximizeWindow();
            }
        }
    }

    private void cleanupStates() {
        uploads.removeAllItems();
        artifactUploadState.clearUploadDetails();
    }

    private void setPopupSizeInMinMode() {
        mainLayout.setWidth(900, Unit.PIXELS);
        mainLayout.setHeight(510, Unit.PIXELS);
    }

    private Button getMinimizeButton() {
        final Button minimizeBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.UPLOAD_STATUS_POPUP_MINIMIZE_BUTTON_ID, "", "", "", true, FontAwesome.MINUS,
                SPUIButtonStyleSmallNoBorder.class);
        minimizeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        minimizeBtn.addClickListener(event -> minimizeWindow());
        minimizeBtn.setEnabled(true);
        return minimizeBtn;
    }

    private Button getResizeButton() {
        final Button resizeBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.UPLOAD_STATUS_POPUP_RESIZE_BUTTON_ID, "", "", "", true, FontAwesome.EXPAND,
                SPUIButtonStyleSmallNoBorder.class);
        resizeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        resizeBtn.addClickListener(this::resizeWindow);
        return resizeBtn;
    }

    private void resizeWindow(final ClickEvent event) {
        if (FontAwesome.EXPAND.equals(event.getButton().getIcon())) {
            event.getButton().setIcon(FontAwesome.COMPRESS);
            setWindowMode(WindowMode.MAXIMIZED);
            resetColumnWidth();
            grid.getColumn(COLUMN_STATUS).setExpandRatio(0);
            grid.getColumn(COLUMN_PROGRESS).setExpandRatio(1);
            grid.getColumn(COLUMN_FILE_NAME).setExpandRatio(2);
            grid.getColumn(COLUMN_REASON).setExpandRatio(3);
            grid.getColumn(SPUILabelDefinitions.NAME_VERSION).setExpandRatio(4);
            mainLayout.setSizeFull();
        } else {
            event.getButton().setIcon(FontAwesome.EXPAND);
            setWindowMode(WindowMode.NORMAL);
            setColumnWidth();
            setPopupSizeInMinMode();
        }
    }

    private Button getCloseButton() {
        final Button closeBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.UPLOAD_STATUS_POPUP_CLOSE_BUTTON_ID, "", "", "", true, FontAwesome.TIMES,
                SPUIButtonStyleSmallNoBorder.class);
        closeBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        closeBtn.addClickListener(event -> onClose());
        return closeBtn;
    }

    private void onClose() {
        if (artifactUploadState.areAllUploadsFinished()) {
            cleanupStates();
            closeWindow();
        } else {
            minimizeWindow();
        }
    }

    private void closeWindow() {
        setWindowMode(WindowMode.NORMAL);
        setColumnWidth();
        setPopupSizeInMinMode();
        resizeButton.setIcon(FontAwesome.EXPAND);
        this.close();
    }

    @SuppressWarnings("unchecked")
    private void updateUploadProgressInfoRowObject(final FileUploadId fileUploadId) {
        Item item = uploads.getItem(fileUploadId);
        if (item == null) {
            item = grid.getContainerDataSource().addItem(fileUploadId);
            item.getItemProperty(COLUMN_FILE_NAME).setValue(fileUploadId.getFilename());
            item.getItemProperty(SPUILabelDefinitions.NAME_VERSION).setValue(HawkbitCommonUtil.getFormattedNameVersion(
                    fileUploadId.getSoftwareModule().getName(), fileUploadId.getSoftwareModule().getVersion()));
        }

        final String status;
        if (artifactUploadState.getFilesInFailedState().contains(fileUploadId)) {
            status = STATUS_FAILED;
        } else if (artifactUploadState.getFilesInSucceededState().contains(fileUploadId)) {
            status = STATUS_FINISHED;
        } else {
            status = STATUS_INPROGRESS;
        }
        item.getItemProperty(COLUMN_STATUS).setValue(status);

        final String failureReason = artifactUploadState.getFileUploadProgress(fileUploadId).getFailureReason();
        if (StringUtils.isNotBlank(failureReason)) {
            item.getItemProperty(COLUMN_REASON).setValue(failureReason);
        }

        final FileUploadProgress fileUploadProgress = artifactUploadState.getFileUploadProgress(fileUploadId);
        final long bytesRead = fileUploadProgress.getBytesRead();
        final long fileSize = fileUploadProgress.getContentLength();
        if (bytesRead > 0 && fileSize > 0) {
            item.getItemProperty(COLUMN_PROGRESS).setValue((double) bytesRead / (double) fileSize);
        }
    }
}

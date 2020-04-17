/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress.FileUploadStatus;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlButtonRenderer;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.renderers.ProgressBarRenderer;
import com.vaadin.ui.themes.ValoTheme;

import elemental.json.JsonValue;

/**
 * Window that shows the progress of all uploads.
 */
public class UploadProgressInfoWindow extends Window {

    private static final long serialVersionUID = 1L;

    private static final String COLUMN_PROGRESS = UIMessageIdProvider.CAPTION_ARTIFACT_UPLOAD_PROGRESS;
    private static final String COLUMN_FILE_NAME = UIMessageIdProvider.CAPTION_ARTIFACT_FILENAME;
    private static final String COLUMN_STATUS = UIMessageIdProvider.CAPTION_ARTIFACT_UPLOAD_STATUS;
    private static final String COLUMN_ACTION = UIMessageIdProvider.CAPTION_ARTIFACT_UPLOAD_ACTION;
    private static final String COLUMN_REASON = UIMessageIdProvider.CAPTION_ARTIFACT_UPLOAD_REASON;
    private static final String COLUMN_SOFTWARE_MODULE = UIMessageIdProvider.CAPTION_SOFTWARE_MODULE;

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

    private Button closeButton;

    private UploadFixed upload;

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
    void onEvent(final FileUploadProgress fileUploadProgress) {
        switch (fileUploadProgress.getFileUploadStatus()) {
        case UPLOAD_STARTED:
            ui.access(() -> onUploadStarted(fileUploadProgress));
            break;
        case UPLOAD_IN_PROGRESS:
        case UPLOAD_FAILED:
        case UPLOAD_SUCCESSFUL:
            ui.access(() -> updateUploadProgressInfoRowObject(fileUploadProgress));
            break;
        case UPLOAD_FINISHED:
            ui.access(this::onUploadFinished);
            break;
        default:
            break;
        }
    }

    private void onUploadStarted(final FileUploadProgress fileUploadProgress) {
        updateUploadProgressInfoRowObject(fileUploadProgress);

        if (isWindowNotAlreadyAttached()) {
            maximizeWindow();
        }

        grid.scrollTo(fileUploadProgress.getFileUploadId());
    }

    private boolean isWindowNotAlreadyAttached() {
        return !UI.getCurrent().getWindows().contains(this);
    }

    private void restoreState() {
        final Indexed container = grid.getContainerDataSource();
        container.removeAllItems();
        for (final FileUploadProgress fileUploadProgress : artifactUploadState
                .getAllFileUploadProgressValuesFromOverallUploadProcessList()) {
            updateUploadProgressInfoRowObject(fileUploadProgress);
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
        grid.getColumn(COLUMN_ACTION).setRenderer(new HtmlButtonRenderer(this::onCancel));

        grid.setColumnOrder(COLUMN_STATUS, COLUMN_ACTION, COLUMN_PROGRESS, COLUMN_FILE_NAME, COLUMN_SOFTWARE_MODULE,
                COLUMN_REASON);
        setColumnWidth();
        grid.getColumn(COLUMN_STATUS).setHeaderCaption(i18n.getMessage(COLUMN_STATUS));
        grid.getColumn(COLUMN_ACTION).setHeaderCaption(i18n.getMessage(COLUMN_ACTION));
        grid.getColumn(COLUMN_PROGRESS).setHeaderCaption(i18n.getMessage(COLUMN_PROGRESS));
        grid.getColumn(COLUMN_FILE_NAME).setHeaderCaption(i18n.getMessage(COLUMN_FILE_NAME));
        grid.getColumn(COLUMN_SOFTWARE_MODULE).setHeaderCaption(i18n.getMessage(COLUMN_SOFTWARE_MODULE));
        grid.getColumn(COLUMN_REASON).setHeaderCaption(i18n.getMessage(COLUMN_REASON));
        grid.setFrozenColumnCount(6);
    }

    private void onCancel(final RendererClickEvent event) {

        Item item = grid.getContainerDataSource().getItem(event.getItemId());
        if (STATUS_INPROGRESS.equals(item.getItemProperty(COLUMN_STATUS).getValue())) {
            Object fileName = item.getItemProperty(COLUMN_FILE_NAME).getValue();
            final ConfirmationDialog confirmDialog = new ConfirmationDialog(
                    i18n.getMessage(UIMessageIdProvider.CAPTION_ARTIFACT_UPLOAD_CANCEL_CONFIRM),
                    i18n.getMessage(UIMessageIdProvider.MESSAGE_UPLOAD_CANCEL, fileName),
                    i18n.getMessage(UIMessageIdProvider.BUTTON_OK), i18n.getMessage(UIMessageIdProvider.BUTTON_CANCEL),
                    ok -> {
                        if (ok) {
                            upload.interruptUpload();
                        }
                    });
            UI.getCurrent().addWindow(confirmDialog.getWindow());
            confirmDialog.getWindow().bringToFront();
        }
    }

    private Grid createGrid() {
        final Grid statusGrid = new Grid(uploads);
        statusGrid.addStyleName(SPUIStyleDefinitions.UPLOAD_STATUS_GRID);
        statusGrid.setId(UIComponentIdProvider.UPLOAD_STATUS_POPUP_GRID);
        statusGrid.setSelectionMode(SelectionMode.NONE);
        statusGrid.setHeaderVisible(true);
        statusGrid.setImmediate(true);
        statusGrid.setSizeFull();
        return statusGrid;
    }

    private static IndexedContainer getGridContainer() {
        final IndexedContainer uploadContainer = new IndexedContainer();
        uploadContainer.addContainerProperty(COLUMN_STATUS, String.class, "Active");
        uploadContainer.addContainerProperty(COLUMN_ACTION, String.class, FontAwesome.REMOVE.getHtml());
        uploadContainer.addContainerProperty(COLUMN_FILE_NAME, String.class, null);
        uploadContainer.addContainerProperty(COLUMN_PROGRESS, Double.class, 0D);
        uploadContainer.addContainerProperty(COLUMN_REASON, String.class, "");
        uploadContainer.addContainerProperty(COLUMN_SOFTWARE_MODULE, String.class, "");
        return uploadContainer;
    }

    private HorizontalLayout getCaptionLayout() {
        final HorizontalLayout captionLayout = new HorizontalLayout();
        captionLayout.setSizeFull();
        captionLayout.setHeight("36px");
        captionLayout.addComponents(windowCaption, closeButton);
        captionLayout.setExpandRatio(windowCaption, 1.0F);
        captionLayout.addStyleName("v-window-header");
        return captionLayout;
    }

    private void createStatusPopupHeaderComponents() {
        windowCaption = new Label(i18n.getMessage(UIMessageIdProvider.CAPTION_ARTIFACT_UPLOAD_POPUP));
        closeButton = getCloseButton();
    }

    private void setColumnWidth() {
        grid.getColumn(COLUMN_STATUS).setWidth(60);
        grid.getColumn(COLUMN_ACTION).setWidth(50);
        grid.getColumn(COLUMN_PROGRESS).setWidth(150);
        grid.getColumn(COLUMN_FILE_NAME).setWidth(200);
        grid.getColumn(COLUMN_REASON).setWidth(290);
        grid.getColumn(COLUMN_SOFTWARE_MODULE).setWidth(200);
    }

    private static class StatusRenderer extends HtmlRenderer {

        private static final long serialVersionUID = 1L;

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
        artifactUploadState.clearUploadTempData();
    }

    private void setPopupSizeInMinMode() {
        mainLayout.setWidth(950, Unit.PIXELS);
        mainLayout.setHeight(510, Unit.PIXELS);
    }

    private Button getCloseButton() {
        final Button closeBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.UPLOAD_STATUS_POPUP_CLOSE_BUTTON_ID, "", "", "", true, FontAwesome.TIMES,
                SPUIButtonStyleNoBorder.class);
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
        this.close();
    }

    @SuppressWarnings("unchecked")
    private void updateUploadProgressInfoRowObject(final FileUploadProgress fileUploadProgress) {
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();
        Item item = uploads.getItem(fileUploadId);
        if (item == null) {
            item = grid.getContainerDataSource().addItem(fileUploadId);
            item.getItemProperty(COLUMN_FILE_NAME).setValue(fileUploadId.getFilename());
            item.getItemProperty(COLUMN_SOFTWARE_MODULE).setValue(HawkbitCommonUtil.getFormattedNameVersion(
                    fileUploadId.getSoftwareModuleName(), fileUploadId.getSoftwareModuleVersion()));
        }

        final String status;
        final FileUploadStatus uploadStatus = fileUploadProgress.getFileUploadStatus();
        if (uploadStatus == FileUploadStatus.UPLOAD_FAILED) {
            status = STATUS_FAILED;
        } else if (uploadStatus == FileUploadStatus.UPLOAD_SUCCESSFUL) {
            status = STATUS_FINISHED;
        } else {
            status = STATUS_INPROGRESS;
        }
        item.getItemProperty(COLUMN_STATUS).setValue(status);
        item.getItemProperty(COLUMN_REASON).setValue(getFailureReason(fileUploadId));

        final long bytesRead = fileUploadProgress.getBytesRead();
        final long fileSize = fileUploadProgress.getContentLength();
        if (bytesRead > 0 && fileSize > 0) {
            item.getItemProperty(COLUMN_PROGRESS).setValue((double) bytesRead / (double) fileSize);
        }
    }

    /**
     * Returns the failure reason for the provided fileUploadId or an empty
     * string but never <code>null</code>.
     * 
     * @param fileUploadId
     * @return the failure reason or an empty String.
     */
    private String getFailureReason(final FileUploadId fileUploadId) {
        String failureReason = "";
        if (artifactUploadState.getFileUploadProgress(fileUploadId) != null) {
            failureReason = artifactUploadState.getFileUploadProgress(fileUploadId).getFailureReason();
        }
        if (StringUtils.isEmpty(failureReason)) {
            return "";
        }
        return failureReason;
    }
    
    /**
     * This method is to store "upload" object to invoke "interruptUpload" api on
     * CancelUpload Action
     * 
     * @param upload
     *            UploadFixed
     */
    public void setUpload(UploadFixed upload) {
        this.upload = upload;
    }
}

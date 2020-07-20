/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.ui.artifacts.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress.FileUploadStatus;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyUploadProgress;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyUploadProgress.ProgressSatus;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Window that shows the progress of all uploads.
 */
public class UploadProgressInfoWindow extends Window {
    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private final ArtifactUploadState artifactUploadState;

    private final UploadProgressGrid uploadProgressGrid;
    private final VerticalLayout mainLayout;

    private Label windowCaption;
    private Button closeButton;

    private final List<ProxyUploadProgress> uploads;

    UploadProgressInfoWindow(final VaadinMessageSource i18n, final ArtifactUploadState artifactUploadState) {
        this.i18n = i18n;
        this.artifactUploadState = artifactUploadState;

        setPopupProperties();
        createStatusPopupHeaderComponents();

        mainLayout = new VerticalLayout();
        mainLayout.setSpacing(Boolean.TRUE);
        mainLayout.setSizeUndefined();
        setPopupSizeInMinMode();

        uploads = new ArrayList<>();
        uploadProgressGrid = new UploadProgressGrid(i18n);
        uploadProgressGrid.setItems(uploads);

        mainLayout.addComponents(getCaptionLayout(), uploadProgressGrid);
        mainLayout.setExpandRatio(uploadProgressGrid, 1.0F);
        setContent(mainLayout);
    }

    /**
     * Updates the status of each file uploaded in the grid view
     *
     * @param fileUploadProgress
     *            FileUploadProgress
     */
    public void updateUploadProgressInfoRowObject(final FileUploadProgress fileUploadProgress) {
        final FileUploadId fileUploadId = fileUploadProgress.getFileUploadId();
        final ProxyUploadProgress gridUploadItem = uploads.stream()
                .filter(upload -> upload.getFileUploadId().equals(fileUploadId)).findAny()
                .orElse(new ProxyUploadProgress());

        gridUploadItem.setStatus(getStatusRepresentaion(fileUploadProgress.getFileUploadStatus()));
        gridUploadItem.setReason(getFailureReason(fileUploadId));

        final long bytesRead = fileUploadProgress.getBytesRead();
        final long fileSize = fileUploadProgress.getContentLength();
        if (bytesRead > 0 && fileSize > 0) {
            gridUploadItem.setProgress((double) bytesRead / (double) fileSize);
        }

        if (gridUploadItem.getFileUploadId() == null) {
            gridUploadItem.setFileUploadId(fileUploadId);
            uploads.add(gridUploadItem);
        }

        uploadProgressGrid.getDataProvider().refreshItem(gridUploadItem);
    }

    private static ProgressSatus getStatusRepresentaion(final FileUploadStatus uploadStatus) {
        if (uploadStatus == FileUploadStatus.UPLOAD_FAILED) {
            return ProgressSatus.FAILED;
        } else if (uploadStatus == FileUploadStatus.UPLOAD_SUCCESSFUL) {
            return ProgressSatus.FINISHED;
        } else {
            return ProgressSatus.INPROGRESS;
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
     * Starts the file upload process and maximize the upload view
     *
     * @param fileUploadProgress
     *            FileUploadProgress
     */
    public void onUploadStarted(final FileUploadProgress fileUploadProgress) {
        updateUploadProgressInfoRowObject(fileUploadProgress);

        if (isWindowNotAlreadyAttached()) {
            maximizeWindow();
        }
    }

    private boolean isWindowNotAlreadyAttached() {
        return !UI.getCurrent().getWindows().contains(this);
    }

    private void restoreState() {
        uploads.clear();
        for (final FileUploadProgress fileUploadProgress : artifactUploadState
                .getAllFileUploadProgressValuesFromOverallUploadProcessList()) {
            updateUploadProgressInfoRowObject(fileUploadProgress);
        }
    }

    private void setPopupProperties() {
        setId(UIComponentIdProvider.UPLOAD_STATUS_POPUP_ID);
        addStyleName(SPUIStyleDefinitions.UPLOAD_INFO);

        setResizable(false);
        setDraggable(true);
        setClosable(false);
        setModal(true);
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
    public void onUploadFinished() {
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
        uploads.clear();
        artifactUploadState.clearUploadTempData();
    }

    private void setPopupSizeInMinMode() {
        mainLayout.setWidth(900, Unit.PIXELS);
        mainLayout.setHeight(510, Unit.PIXELS);
    }

    private Button getCloseButton() {
        final Button closeBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.UPLOAD_STATUS_POPUP_CLOSE_BUTTON_ID, "", "", "", true, VaadinIcons.CLOSE,
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
        setPopupSizeInMinMode();
        close();
    }
}

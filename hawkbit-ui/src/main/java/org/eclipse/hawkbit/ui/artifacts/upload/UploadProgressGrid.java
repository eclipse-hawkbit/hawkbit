/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.ProgressStatusIconSupplier;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyUploadProgress;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Grid;
import com.vaadin.ui.renderers.ProgressBarRenderer;

/**
 * Grid for Upload Progress pop up info window.
 */
public class UploadProgressGrid extends Grid<ProxyUploadProgress> {
    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private static final String UPLOAD_PROGRESS_STATUS_ID = "uploadProgressStatus";
    private static final String UPLOAD_PROGRESS_BAR_ID = "uploadProgressBar";
    private static final String UPLOAD_PROGRESS_FILENAME_ID = "uploadProgressFileName";
    private static final String UPLOAD_PROGRESS_SM_ID = "uploadProgressSm";
    private static final String UPLOAD_PROGRESS_REASON_ID = "uploadProgressReason";

    private final ProgressStatusIconSupplier<ProxyUploadProgress> progressStatusIconSupplier;

    /**
     * Constructor for UploadProgressGrid
     *
     * @param i18n
     *          VaadinMessageSource
     */
    public UploadProgressGrid(final VaadinMessageSource i18n) {
        this.i18n = i18n;

        progressStatusIconSupplier = new ProgressStatusIconSupplier<>(i18n, ProxyUploadProgress::getStatus,
                UIComponentIdProvider.UPLOAD_STATUS_LABEL_ID);
        init();
    }

    private void init() {
        setId(UIComponentIdProvider.UPLOAD_STATUS_POPUP_GRID);
        addStyleName(SPUIStyleDefinitions.UPLOAD_STATUS_GRID);
        setSelectionMode(SelectionMode.NONE);
        setSizeFull();

        addColumns();
    }

    private void addColumns() {
        GridComponentBuilder.addIconColumn(this, progressStatusIconSupplier::getLabel, UPLOAD_PROGRESS_STATUS_ID,
                i18n.getMessage(UIMessageIdProvider.CAPTION_ARTIFACT_UPLOAD_STATUS));

        addColumn(ProxyUploadProgress::getProgress, new ProgressBarRenderer()).setId(UPLOAD_PROGRESS_BAR_ID)
                .setCaption(i18n.getMessage(UIMessageIdProvider.CAPTION_ARTIFACT_UPLOAD_PROGRESS)).setExpandRatio(1);

        GridComponentBuilder.addColumn(this, uploadProgress -> uploadProgress.getFileUploadId().getFilename())
                .setId(UPLOAD_PROGRESS_FILENAME_ID)
                .setCaption(i18n.getMessage(UIMessageIdProvider.CAPTION_ARTIFACT_FILENAME));

        GridComponentBuilder
                .addColumn(this,
                        uploadProgress -> HawkbitCommonUtil.getFormattedNameVersion(
                                uploadProgress.getFileUploadId().getSoftwareModuleName(),
                                uploadProgress.getFileUploadId().getSoftwareModuleVersion()))
                .setId(UPLOAD_PROGRESS_SM_ID).setCaption(i18n.getMessage(UIMessageIdProvider.CAPTION_SOFTWARE_MODULE));

        GridComponentBuilder.addColumn(this, ProxyUploadProgress::getReason).setId(UPLOAD_PROGRESS_REASON_ID)
                .setCaption(i18n.getMessage(UIMessageIdProvider.CAPTION_ARTIFACT_UPLOAD_REASON));

        getColumns().forEach(col -> col.setSortable(false));
    }
}

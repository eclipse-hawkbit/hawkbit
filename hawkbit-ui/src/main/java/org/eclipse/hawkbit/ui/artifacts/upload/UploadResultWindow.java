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

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleTiny;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Upload status popup.
 *
 *
 *
 *
 *
 */
public class UploadResultWindow implements Button.ClickListener {

    private static final long serialVersionUID = 5205927189362269027L;

    private List<UploadStatus> uploadResultList = new ArrayList<UploadStatus>();

    private Button closeBtn;

    private Table uploadResultTable;

    private Window uploadResultsWindow;

    private IndexedContainer tabelContainer;

    private final I18N i18n;

    private static final String FILE_NAME = "fileName";

    private static final String BASE_SW_MODULE = "baseSwModuleName";

    private static final String UPLOAD_RESULT = "uploadResult";

    private static final String REASON = "reason";

    /**
     * Initialize upload status popup.
     * 
     * @param uploadResultList
     *            upload status details
     * @param i18n
     *            I18N
     */
    public UploadResultWindow(final List<UploadStatus> uploadResultList, final I18N i18n) {
        this.uploadResultList = uploadResultList;
        this.i18n = i18n;
        createComponents();
        createLayout();
    }

    private void createComponents() {
        closeBtn = SPUIComponentProvider.getButton(SPUIComponetIdProvider.UPLOAD_ARTIFACT_RESULT_CLOSE,
                SPUILabelDefinitions.CLOSE, SPUILabelDefinitions.CLOSE, ValoTheme.BUTTON_PRIMARY, false, null,
                SPUIButtonStyleTiny.class);
        closeBtn.addClickListener(this);

        uploadResultTable = new Table();
        uploadResultTable.addStyleName("artifact-table");
        uploadResultTable.setSizeFull();
        uploadResultTable.setImmediate(true);
        uploadResultTable.setId(SPUIComponetIdProvider.UPLOAD_RESULT_TABLE);
        uploadResultTable.addStyleName(ValoTheme.TABLE_BORDERLESS);
        uploadResultTable.addStyleName(ValoTheme.TABLE_SMALL);
        uploadResultTable.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        uploadResultTable.addStyleName("accordion-tab-table-style");

        populateUploadResultTable();
    }

    private void populateUploadResultTable() {
        setTableContainer();
        Label statusLabel;
        Label reasonLabel;
        for (final UploadStatus uploadResult : uploadResultList) {
            final Item newItem = tabelContainer
                    .addItem(uploadResult.getBaseSwModuleName() + "/" + uploadResult.getFileName());
            newItem.getItemProperty(FILE_NAME).setValue(HawkbitCommonUtil.getFormatedLabel(uploadResult.getFileName()));
            newItem.getItemProperty(BASE_SW_MODULE)
                    .setValue(HawkbitCommonUtil.getFormatedLabel(uploadResult.getBaseSwModuleName()));

            if (uploadResult.getUploadResult().equals(SPUILabelDefinitions.SUCCESS)) {
                statusLabel = new Label(HawkbitCommonUtil.getFormatedLabel(i18n.get("upload.success")));
                statusLabel.addStyleName("validation-success");
                newItem.getItemProperty(UPLOAD_RESULT).setValue(statusLabel);
            } else {
                statusLabel = new Label(HawkbitCommonUtil.getFormatedLabel(i18n.get("upload.failed")));
                statusLabel.addStyleName("validation-failed");
                newItem.getItemProperty(UPLOAD_RESULT).setValue(statusLabel);
            }

            reasonLabel = HawkbitCommonUtil.getFormatedLabel(uploadResult.getReason());
            reasonLabel.setDescription(uploadResult.getReason());
            final String idStr = SPUIComponetIdProvider.UPLOAD_ERROR_REASON + uploadResult.getBaseSwModuleName() + "/"
                    + uploadResult.getFileName();
            reasonLabel.setId(idStr);
            newItem.getItemProperty(REASON).setValue(reasonLabel);
        }
    }

    private void setTableContainer() {
        tabelContainer = new IndexedContainer();

        tabelContainer.addContainerProperty(FILE_NAME, Label.class, null);
        tabelContainer.addContainerProperty(BASE_SW_MODULE, Label.class, null);
        tabelContainer.addContainerProperty(UPLOAD_RESULT, Label.class, null);
        tabelContainer.addContainerProperty(REASON, Label.class, null);

        uploadResultTable.setContainerDataSource(tabelContainer);
        uploadResultTable.setPageLength(10);
        uploadResultTable.setColumnHeader(FILE_NAME, i18n.get("upload.file.name"));
        uploadResultTable.setColumnHeader(BASE_SW_MODULE, i18n.get("upload.swModuleTable.header"));
        uploadResultTable.setColumnHeader(UPLOAD_RESULT, i18n.get("upload.result.status"));
        uploadResultTable.setColumnHeader(REASON, i18n.get("upload.reason"));

        uploadResultTable.setColumnExpandRatio(FILE_NAME, 0.2f);
        uploadResultTable.setColumnExpandRatio(BASE_SW_MODULE, 0.2f);
        uploadResultTable.setColumnExpandRatio(UPLOAD_RESULT, 0.12f);
        uploadResultTable.setColumnExpandRatio(REASON, 0.48f);
        final Object[] visibileColumn = { FILE_NAME, BASE_SW_MODULE, UPLOAD_RESULT, REASON };
        uploadResultTable.setVisibleColumns(visibileColumn);
    }

    private void createLayout() {
        final HorizontalLayout footer = new HorizontalLayout();
        footer.setSizeUndefined();
        footer.addStyleName("confirmation-window-footer");
        footer.setSpacing(true);
        footer.setMargin(false);
        footer.addComponents(closeBtn);
        footer.setComponentAlignment(closeBtn, Alignment.TOP_CENTER);

        final VerticalLayout uploadResultDetails = new VerticalLayout();
        uploadResultDetails.setWidth(SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_WIDTH + "px");
        uploadResultDetails.addStyleName("confirmation-popup");
        uploadResultDetails.addComponent(uploadResultTable);
        uploadResultDetails.setComponentAlignment(uploadResultTable, Alignment.MIDDLE_CENTER);
        uploadResultDetails.addComponent(footer);
        uploadResultDetails.setComponentAlignment(footer, Alignment.MIDDLE_CENTER);

        uploadResultsWindow = new Window();
        uploadResultsWindow.setContent(uploadResultDetails);
        uploadResultsWindow.setResizable(Boolean.FALSE);
        uploadResultsWindow.setClosable(Boolean.FALSE);
        uploadResultsWindow.setDraggable(Boolean.TRUE);
        uploadResultsWindow.setModal(true);
        uploadResultsWindow.setCaption(SPUILabelDefinitions.UPLOAD_RESULT);
        uploadResultsWindow.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);
    }

    @Override
    public void buttonClick(final ClickEvent event) {
        if (event.getComponent().getId().equals(SPUIComponetIdProvider.UPLOAD_ARTIFACT_RESULT_CLOSE)
                || event.getComponent().getId().equals(SPUIComponetIdProvider.UPLOAD_ARTIFACT_RESULT_POPUP_CLOSE)) {
            uploadResultsWindow.close();
        }

    }

    public Window getUploadResultsWindow() {
        return uploadResultsWindow;
    }

    public Table getUploadResultTable() {
        return uploadResultTable;
    }

}

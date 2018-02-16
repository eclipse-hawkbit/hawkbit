/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.state.CustomFile;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleTiny;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Artifact upload confirmation popup.
 *
 */
public class UploadConfirmationWindow implements Button.ClickListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(UploadConfirmationWindow.class);

    private static final String MD5_CHECKSUM = "md5Checksum";

    private static final String SHA1_CHECKSUM = "sha1Checksum";

    private static final String FILE_NAME = "fileName";

    private static final String SW_MODULE_NAME = "swModuleName";

    private static final String SIZE = "size";

    private static final String ACTION = "action";

    private static final String BASE_SOFTWARE_ID = "softwareModuleId";

    private static final String FILE_NAME_LAYOUT = "fileNameLayout";

    private static final String WARNING_ICON = "warningIcon";

    private static final String CUSTOM_FILE = "customFile";

    private static final String ARTIFACT_UPLOAD_EXCEPTION = "Artifact upload exception:";

    private static final String ALREADY_EXISTS_MSG = "upload.artifact.alreadyExists";

    private final VaadinMessageSource i18n;

    private Window window;

    private Button uploadBtn;

    private Button cancelBtn;

    private Table uploadDetailsTable;

    private final UploadLayout uploadLayout;

    private IndexedContainer tableContainer;

    private final List<UploadStatus> uploadResultList = new ArrayList<>();

    private VerticalLayout uploadArtifactDetails;

    private UploadResultWindow currentUploadResultWindow;

    private int redErrorLabelCount;

    private final ArtifactUploadState artifactUploadState;

    private final transient UIEventBus eventBus;

    private final transient ArtifactManagement artifactManagement;

    /**
     * Initialize the upload confirmation window.
     *
     * @param artifactUploadView
     *            reference of upload layout.
     * @param artifactUploadState
     *            reference of session variable {@link ArtifactUploadState}.
     */
    UploadConfirmationWindow(final UploadLayout artifactUploadView, final ArtifactUploadState artifactUploadState,
            final UIEventBus eventBus, final ArtifactManagement artifactManagement) {
        this.uploadLayout = artifactUploadView;
        this.artifactUploadState = artifactUploadState;
        this.eventBus = eventBus;
        this.artifactManagement = artifactManagement;
        i18n = artifactUploadView.getI18n();
        createRequiredComponents();
        buildLayout();
    }

    private boolean checkIfArtifactDetailsDisplayed(final Long bSoftwareModuleId) {
        return artifactUploadState.getSelectedBaseSwModuleId().map(moduleId -> moduleId.equals(bSoftwareModuleId))
                .orElse(false);
    }

    private Boolean preUploadValidation(final List<String> itemIds) {
        Boolean validationSuccess = true;
        for (final String itemId : itemIds) {
            final Item item = tableContainer.getItem(itemId);
            final String providedFileName = (String) item.getItemProperty(FILE_NAME).getValue();
            if (!StringUtils.hasText(providedFileName)) {
                validationSuccess = false;
                break;
            }
        }
        return validationSuccess;
    }

    private void createRequiredComponents() {
        uploadBtn = SPUIComponentProvider.getButton(UIComponentIdProvider.UPLOAD_BUTTON, SPUILabelDefinitions.SUBMIT,
                SPUILabelDefinitions.SUBMIT, ValoTheme.BUTTON_PRIMARY, false, null, SPUIButtonStyleTiny.class);
        uploadBtn.addClickListener(this);
        cancelBtn = SPUIComponentProvider.getButton(UIComponentIdProvider.UPLOAD_DISCARD_DETAILS_BUTTON,
                SPUILabelDefinitions.DISCARD, SPUILabelDefinitions.DISCARD, null, false, null,
                SPUIButtonStyleTiny.class);
        cancelBtn.addClickListener(this);

        uploadDetailsTable = new Table();
        uploadDetailsTable.addStyleName("artifact-table");
        uploadDetailsTable.setSizeFull();
        uploadDetailsTable.setId(UIComponentIdProvider.UPLOAD_ARTIFACT_DETAILS_TABLE);
        uploadDetailsTable.addStyleName(ValoTheme.TABLE_BORDERLESS);
        uploadDetailsTable.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        uploadDetailsTable.addStyleName(ValoTheme.TABLE_SMALL);

        setTableContainer();
        populateUploadDetailsTable();
    }

    /**
     * Warning icon is displayed, if an artifact exists with same provided file
     * name. Error icon is displayed, if file name entered is duplicate.
     *
     * @param warningIconLabel
     *            warning/error label
     * @param fileName
     *            provided file name
     * @param itemId
     *            item id of the current row
     */
    private void setWarningIcon(final Label warningIconLabel, final String fileName, final Object itemId) {
        final Item item = uploadDetailsTable.getItem(itemId);
        if (StringUtils.hasText(fileName)) {
            final String fileNameTrimmed = StringUtils.trimWhitespace(fileName);
            final Long baseSwId = (Long) item.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final Optional<Artifact> artifact = artifactManagement.getByFilenameAndSoftwareModule(fileNameTrimmed,
                    baseSwId);
            if (artifact.isPresent()) {
                warningIconLabel.setVisible(true);
                if (isErrorIcon(warningIconLabel)) {
                    warningIconLabel.removeStyleName(SPUIStyleDefinitions.ERROR_LABEL);
                    redErrorLabelCount--;
                }
                warningIconLabel.setDescription(i18n.getMessage(ALREADY_EXISTS_MSG));
                if (checkForDuplicate(fileNameTrimmed, itemId, baseSwId)) {
                    warningIconLabel.setDescription(i18n.getMessage("message.duplicate.filename"));
                    warningIconLabel.addStyleName(SPUIStyleDefinitions.ERROR_LABEL);
                    redErrorLabelCount++;
                }
            } else {
                warningIconLabel.setVisible(false);
                if (warningIconLabel.getStyleName().contains(SPUIStyleDefinitions.ERROR_LABEL)) {
                    warningIconLabel.removeStyleName(SPUIStyleDefinitions.ERROR_LABEL);
                    warningIconLabel.setDescription(i18n.getMessage(ALREADY_EXISTS_MSG));
                    redErrorLabelCount--;
                }
            }
        }
    }

    private Boolean checkForDuplicate(final String fileName, final Object itemId, final Long currentBaseSwId) {
        for (final Object newItemId : tableContainer.getItemIds()) {
            final Item newItem = tableContainer.getItem(newItemId);
            final Long newBaseSwId = (Long) newItem.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final String newFileName = (String) newItem.getItemProperty(FILE_NAME).getValue();
            if (!newItemId.equals(itemId) && newBaseSwId.equals(currentBaseSwId) && newFileName.equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    private void populateUploadDetailsTable() {
        for (final CustomFile customFile : uploadLayout.getFileSelected()) {
            final String swNameVersion = HawkbitCommonUtil.getFormattedNameVersion(
                    customFile.getBaseSoftwareModuleName(), customFile.getBaseSoftwareModuleVersion());
            final String itemId = swNameVersion + "/" + customFile.getFileName();
            final Item newItem = tableContainer.addItem(itemId);
            final SoftwareModule bSoftwareModule = artifactUploadState.getBaseSwModuleList().get(swNameVersion);
            newItem.getItemProperty(BASE_SOFTWARE_ID).setValue(bSoftwareModule.getId());

            addFileNameLayout(newItem, swNameVersion, customFile.getFileName(), itemId);

            newItem.getItemProperty(SW_MODULE_NAME).setValue(HawkbitCommonUtil.getFormatedLabel(swNameVersion));
            newItem.getItemProperty(SIZE).setValue(customFile.getFileSize());
            final Button deleteIcon = SPUIComponentProvider.getButton(
                    UIComponentIdProvider.UPLOAD_DELETE_ICON + "-" + itemId, "", SPUILabelDefinitions.DISCARD,
                    ValoTheme.BUTTON_TINY + " " + "blueicon", true, FontAwesome.TRASH_O,
                    SPUIButtonStyleSmallNoBorder.class);
            deleteIcon.addClickListener(this);
            deleteIcon.setData(itemId);
            newItem.getItemProperty(ACTION).setValue(deleteIcon);

            final TextField sha1 = createTextField(swNameVersion + "/" + customFile.getFileName() + "/sha1");

            final TextField md5 = createTextField(swNameVersion + "/" + customFile.getFileName() + "/md5");

            createTextField(swNameVersion + "/" + customFile.getFileName() + "/customFileName");

            newItem.getItemProperty(SHA1_CHECKSUM).setValue(sha1);
            newItem.getItemProperty(MD5_CHECKSUM).setValue(md5);
            newItem.getItemProperty(CUSTOM_FILE).setValue(customFile);
        }
    }

    private static TextField createTextField(final String id) {
        return new TextFieldBuilder().immediate(true).id(id).buildTextComponent();
    }

    private void addFileNameLayout(final Item newItem, final String baseSoftwareModuleNameVersion,
            final String customFileName, final String itemId) {
        final HorizontalLayout horizontalLayout = new HorizontalLayout();
        final TextField fileNameTextField = createTextField(
                baseSoftwareModuleNameVersion + "/" + customFileName + "/customFileName");
        fileNameTextField.setData(baseSoftwareModuleNameVersion + "/" + customFileName);
        fileNameTextField.setValue(customFileName);

        newItem.getItemProperty(FILE_NAME).setValue(fileNameTextField.getValue());

        final Label warningIconLabel = getWarningLabel();
        warningIconLabel.setId(baseSoftwareModuleNameVersion + "/" + customFileName + "/icon");
        setWarningIcon(warningIconLabel, fileNameTextField.getValue(), itemId);
        newItem.getItemProperty(WARNING_ICON).setValue(warningIconLabel);

        horizontalLayout.addComponent(fileNameTextField);
        horizontalLayout.setComponentAlignment(fileNameTextField, Alignment.MIDDLE_LEFT);
        horizontalLayout.addComponent(warningIconLabel);
        horizontalLayout.setComponentAlignment(warningIconLabel, Alignment.MIDDLE_RIGHT);
        newItem.getItemProperty(FILE_NAME_LAYOUT).setValue(horizontalLayout);

        fileNameTextField.addTextChangeListener(event -> onFileNameChange(event, warningIconLabel, newItem));
    }

    private void onFileNameChange(final TextChangeEvent event, final Label warningIconLabel, final Item newItem) {

        final String itemId = (String) ((TextField) event.getComponent()).getData();
        final String fileName = event.getText();

        final Boolean isWarningIconDisplayed = isWarningIcon(warningIconLabel);
        setWarningIcon(warningIconLabel, fileName, itemId);

        final Long currentSwId = (Long) newItem.getItemProperty(BASE_SOFTWARE_ID).getValue();
        final String oldFileName = (String) newItem.getItemProperty(FILE_NAME).getValue();
        newItem.getItemProperty(FILE_NAME).setValue(event.getText());

        // if warning was displayed prior and not displayed currently
        if (isWarningIconDisplayed && !warningIconLabel.isVisible()) {
            modifyIconOfSameSwId(itemId, currentSwId, oldFileName);
        }
        checkDuplicateEntry(itemId, currentSwId, event.getText(), oldFileName);
        enableOrDisableUploadBtn();
    }

    private void enableOrDisableUploadBtn() {
        if (redErrorLabelCount == 0) {
            uploadBtn.setEnabled(true);
        } else {
            uploadBtn.setEnabled(false);
        }
    }

    /**
     * If warning was displayed prior and not displayed currently ,the update
     * other warning labels accordingly.
     *
     * @param itemId
     *            id of row which is deleted/whose file name modified.
     * @param oldSwId
     *            software module id
     * @param oldFileName
     *            file name before modification
     */
    private void modifyIconOfSameSwId(final Object itemId, final Long oldSwId, final String oldFileName) {
        for (final Object rowId : tableContainer.getItemIds()) {
            final Item newItem = tableContainer.getItem(rowId);
            final Long newBaseSwId = (Long) newItem.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final String newFileName = (String) newItem.getItemProperty(FILE_NAME).getValue();
            if (!rowId.equals(itemId) && newBaseSwId.equals(oldSwId) && newFileName.equals(oldFileName)) {
                final HorizontalLayout layout = (HorizontalLayout) newItem.getItemProperty(FILE_NAME_LAYOUT).getValue();
                final Label warningLabel = (Label) layout.getComponent(1);
                if (warningLabel.isVisible()) {
                    warningLabel.removeStyleName(SPUIStyleDefinitions.ERROR_LABEL);
                    warningLabel.setDescription(i18n.getMessage(ALREADY_EXISTS_MSG));
                    newItem.getItemProperty(WARNING_ICON).setValue(warningLabel);
                    redErrorLabelCount--;
                    break;
                }
            }
        }
    }

    /**
     * Check if icon is warning icon and visible.
     *
     * @param icon
     *            label
     * @return Boolean
     */
    private static boolean isWarningIcon(final Label icon) {
        return !isErrorIcon(icon);
    }

    /**
     * Check if icon is error icon and visible.
     *
     * @param icon
     *            label
     * @return Boolean
     */
    private static boolean isErrorIcon(final Label icon) {
        return icon.isVisible() && icon.getStyleName().contains(SPUIStyleDefinitions.ERROR_LABEL);
    }

    private static Label getWarningLabel() {
        final Label warningIconLabel = new Label();
        warningIconLabel.addStyleName(ValoTheme.LABEL_SMALL);
        warningIconLabel.setHeightUndefined();
        warningIconLabel.setContentMode(ContentMode.HTML);
        warningIconLabel.setValue(FontAwesome.WARNING.getHtml());
        warningIconLabel.addStyleName("warningLabel");
        warningIconLabel.setVisible(false);
        return warningIconLabel;
    }

    private void newFileNameIsDuplicate(final Object itemId, final Long currentSwId, final String currentChangedText) {
        for (final Object rowId : tableContainer.getItemIds()) {
            final Item currentItem = tableContainer.getItem(itemId);
            final Item newItem = tableContainer.getItem(rowId);
            final Long newBaseSwId = (Long) newItem.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final String fileName = (String) newItem.getItemProperty(FILE_NAME).getValue();
            if (!rowId.equals(itemId) && newBaseSwId.equals(currentSwId) && fileName.equals(currentChangedText)) {
                final HorizontalLayout layout = (HorizontalLayout) currentItem.getItemProperty(FILE_NAME_LAYOUT)
                        .getValue();
                final Label iconLabel = (Label) layout.getComponent(1);
                if (!iconLabel.getStyleName().contains(SPUIStyleDefinitions.ERROR_LABEL)) {
                    iconLabel.setVisible(true);
                    iconLabel.setDescription(i18n.getMessage("message.duplicate.filename"));
                    iconLabel.addStyleName(SPUIStyleDefinitions.ERROR_LABEL);
                    redErrorLabelCount++;
                }
                break;
            }
        }
    }

    private void reValidateOtherFileNamesOfSameBaseSw(final Object itemId, final Long currentSwId,
            final String oldFileName) {
        Label warningLabel = null;
        Label errorLabel = null;
        int errorLabelCount = 0;
        int duplicateCount = 0;
        for (final Object rowId : tableContainer.getItemIds()) {
            final Item newItem = tableContainer.getItem(rowId);
            final Long newBaseSwId = (Long) newItem.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final String newFileName = (String) newItem.getItemProperty(FILE_NAME).getValue();
            if (!rowId.equals(itemId) && newBaseSwId.equals(currentSwId) && newFileName.equals(oldFileName)) {
                final HorizontalLayout layout = (HorizontalLayout) newItem.getItemProperty(FILE_NAME_LAYOUT).getValue();
                final Label icon = (Label) layout.getComponent(1);
                duplicateCount++;
                if (icon.isVisible()) {
                    if (!icon.getStyleName().contains(SPUIStyleDefinitions.ERROR_LABEL)) {
                        warningLabel = icon;
                        break;
                    }
                    errorLabel = icon;
                    errorLabelCount++;
                }
            }
        }
        hideErrorIcon(warningLabel, errorLabelCount, duplicateCount, errorLabel, oldFileName, currentSwId);
    }

    private void hideErrorIcon(final Label warningLabel, final int errorLabelCount, final int duplicateCount,
            final Label errorLabel, final String oldFileName, final Long currentSwId) {
        if (warningLabel == null && (errorLabelCount > 1 || (duplicateCount == 1 && errorLabelCount == 1))) {

            final Optional<Artifact> artifactList = artifactManagement.getByFilenameAndSoftwareModule(oldFileName,
                    currentSwId);
            if (errorLabel == null) {
                return;
            }
            errorLabel.removeStyleName(SPUIStyleDefinitions.ERROR_LABEL);
            errorLabel.setDescription(i18n.getMessage(ALREADY_EXISTS_MSG));
            if (!artifactList.isPresent()) {
                errorLabel.setVisible(false);
            }
            redErrorLabelCount--;
        }
    }

    private void checkDuplicateEntry(final Object itemId, final Long currentSwId, final String newChangedText,
            final String oldFileName) {
        /**
         * Check if newly entered file name is a duplicate.
         */
        newFileNameIsDuplicate(itemId, currentSwId, newChangedText);
        /**
         * After the current changed file name is validated ,other files of same
         * software module as has be revalidated. And icons are updated
         * accordingly.
         */
        reValidateOtherFileNamesOfSameBaseSw(itemId, currentSwId, oldFileName);

    }

    private void setTableContainer() {
        tableContainer = new IndexedContainer();
        tableContainer.addContainerProperty(FILE_NAME_LAYOUT, HorizontalLayout.class, null);
        tableContainer.addContainerProperty(SW_MODULE_NAME, Label.class, null);
        tableContainer.addContainerProperty(SHA1_CHECKSUM, TextField.class, null);
        tableContainer.addContainerProperty(MD5_CHECKSUM, TextField.class, null);
        tableContainer.addContainerProperty(SIZE, Long.class, null);
        tableContainer.addContainerProperty(ACTION, Button.class, "");
        tableContainer.addContainerProperty(FILE_NAME, String.class, null);
        tableContainer.addContainerProperty(BASE_SOFTWARE_ID, Long.class, null);
        tableContainer.addContainerProperty(WARNING_ICON, Label.class, null);
        tableContainer.addContainerProperty(CUSTOM_FILE, CustomFile.class, null);

        uploadDetailsTable.setContainerDataSource(tableContainer);
        uploadDetailsTable.setPageLength(10);
        uploadDetailsTable.setColumnHeader(FILE_NAME_LAYOUT, i18n.getMessage("upload.file.name"));
        uploadDetailsTable.setColumnHeader(SW_MODULE_NAME, i18n.getMessage("upload.swModuleTable.header"));
        uploadDetailsTable.setColumnHeader(SHA1_CHECKSUM, i18n.getMessage("upload.sha1"));
        uploadDetailsTable.setColumnHeader(MD5_CHECKSUM, i18n.getMessage("upload.md5"));
        uploadDetailsTable.setColumnHeader(SIZE, i18n.getMessage("upload.size"));
        uploadDetailsTable.setColumnHeader(ACTION, i18n.getMessage("upload.action"));

        uploadDetailsTable.setColumnExpandRatio(FILE_NAME_LAYOUT, 0.25F);
        uploadDetailsTable.setColumnExpandRatio(SW_MODULE_NAME, 0.17F);
        uploadDetailsTable.setColumnExpandRatio(SHA1_CHECKSUM, 0.2F);
        uploadDetailsTable.setColumnExpandRatio(MD5_CHECKSUM, 0.2F);
        uploadDetailsTable.setColumnExpandRatio(SIZE, 0.12F);
        uploadDetailsTable.setColumnExpandRatio(ACTION, 0.06F);

        final Object[] visibileColumn = { FILE_NAME_LAYOUT, SW_MODULE_NAME, SHA1_CHECKSUM, MD5_CHECKSUM, SIZE, ACTION };
        uploadDetailsTable.setVisibleColumns(visibileColumn);
    }

    private void buildLayout() {
        final HorizontalLayout footer = getFooterLayout();

        uploadArtifactDetails = new VerticalLayout();
        uploadArtifactDetails.setWidth(SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_WIDTH + "px");
        uploadArtifactDetails.addStyleName("confirmation-popup");
        uploadArtifactDetails.addComponent(uploadDetailsTable);
        uploadArtifactDetails.setComponentAlignment(uploadDetailsTable, Alignment.MIDDLE_CENTER);
        uploadArtifactDetails.addComponent(footer);
        uploadArtifactDetails.setComponentAlignment(footer, Alignment.MIDDLE_CENTER);

        window = new Window();
        window.setContent(uploadArtifactDetails);
        window.setResizable(Boolean.FALSE);
        window.setClosable(Boolean.TRUE);
        window.setDraggable(Boolean.TRUE);
        window.setModal(true);
        window.addCloseListener(event -> onPopupClose());
        window.setCaption(i18n.getMessage("header.caption.upload.details"));
        window.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);
    }

    private void onPopupClose() {
        uploadLayout.setCurrentUploadConfirmationwindow(null);
    }

    private HorizontalLayout getFooterLayout() {
        final HorizontalLayout footer = new HorizontalLayout();
        footer.setSizeUndefined();
        footer.addStyleName("confirmation-window-footer");
        footer.setSpacing(true);
        footer.setMargin(false);
        footer.addComponents(uploadBtn, cancelBtn);
        footer.setComponentAlignment(uploadBtn, Alignment.TOP_LEFT);
        footer.setComponentAlignment(cancelBtn, Alignment.TOP_RIGHT);
        return footer;
    }

    public Window getUploadConfirmationWindow() {
        return window;
    }

    @Override
    public void buttonClick(final ClickEvent event) {
        if (event.getComponent().getId().equals(UIComponentIdProvider.UPLOAD_ARTIFACT_DETAILS_CLOSE)) {
            window.close();
        } else if (event.getComponent().getId().equals(UIComponentIdProvider.UPLOAD_DISCARD_DETAILS_BUTTON)) {
            uploadLayout.clearUploadedFileDetails();
            window.close();
        } else if (event.getComponent().getId().equals(UIComponentIdProvider.UPLOAD_BUTTON)) {
            processArtifactUpload();
        } else if (event.getComponent().getId().startsWith(UIComponentIdProvider.UPLOAD_DELETE_ICON)) {
            final String itemId = ((Button) event.getComponent()).getData().toString();
            final Item item = uploadDetailsTable.getItem(((Button) event.getComponent()).getData());
            final Long swId = (Long) item.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final CustomFile customFile = (CustomFile) item.getItemProperty(CUSTOM_FILE).getValue();
            final String fileName = (String) item.getItemProperty(FILE_NAME).getValue();
            final Label warningIconLabel = (Label) item.getItemProperty(WARNING_ICON).getValue();
            final Boolean isWarningIconDisplayed = isWarningIcon(warningIconLabel);
            if (isWarningIconDisplayed) {
                modifyIconOfSameSwId(itemId, swId, fileName);
            } else if (isErrorIcon(warningIconLabel)) {
                redErrorLabelCount--;
            }
            reValidateOtherFileNamesOfSameBaseSw(((Button) event.getComponent()).getData(), swId, fileName);
            enableOrDisableUploadBtn();

            uploadDetailsTable.removeItem(((Button) event.getComponent()).getData());
            uploadLayout.getFileSelected().remove(customFile);
            uploadLayout.updateUploadCounts();
            if (uploadDetailsTable.getItemIds().isEmpty()) {
                window.close();
                uploadLayout.clearUploadedFileDetails();
            }
        }
    }

    // Exception squid:S3655 - Optional access is checked in
    // checkIfArtifactDetailsDispalyed subroutine
    @SuppressWarnings("squid:S3655")
    private void processArtifactUpload() {
        final List<String> itemIds = (List<String>) uploadDetailsTable.getItemIds();
        if (preUploadValidation(itemIds)) {
            Boolean refreshArtifactDetailsLayout = false;
            for (final String itemId : itemIds) {
                final String[] itemDet = itemId.split("/");
                final String baseSoftwareModuleNameVersion = itemDet[0];
                final String fileName = itemDet[1];
                final SoftwareModule bSoftwareModule = artifactUploadState.getBaseSwModuleList()
                        .get(baseSoftwareModuleNameVersion);
                for (final CustomFile customFile : uploadLayout.getFileSelected()) {
                    final String baseSwModuleNameVersion = HawkbitCommonUtil.getFormattedNameVersion(
                            customFile.getBaseSoftwareModuleName(), customFile.getBaseSoftwareModuleVersion());
                    if (customFile.getFileName().equals(fileName)
                            && baseSwModuleNameVersion.equals(baseSoftwareModuleNameVersion)) {
                        createArtifact(itemId, customFile.getFilePath(), artifactManagement, bSoftwareModule);
                    }
                }
                refreshArtifactDetailsLayout = checkIfArtifactDetailsDisplayed(bSoftwareModule.getId());
            }

            if (refreshArtifactDetailsLayout) {
                uploadLayout.refreshArtifactDetailsLayout(artifactUploadState.getSelectedBaseSwModuleId().get());
            }
            uploadLayout.clearFileList();
            window.close();
            // call upload result window
            currentUploadResultWindow = new UploadResultWindow(uploadResultList, i18n, eventBus);
            UI.getCurrent().addWindow(currentUploadResultWindow.getUploadResultsWindow());
            currentUploadResultWindow.getUploadResultsWindow().addCloseListener(event -> onResultDetailsPopupClose());
            uploadLayout.setResultPopupHeightWidth(Page.getCurrent().getBrowserWindowWidth(),
                    Page.getCurrent().getBrowserWindowHeight());
        } else {
            uploadLayout.getUINotification()
                    .displayValidationError(uploadLayout.getI18n().getMessage("message.error.noProvidedName"));
        }
    }

    private void onResultDetailsPopupClose() {
        currentUploadResultWindow = null;
    }

    private void createArtifact(final String itemId, final String filePath, final ArtifactManagement artifactManagement,
            final SoftwareModule baseSw) {

        final File newFile = new File(filePath);
        final Item item = tableContainer.getItem(itemId);
        // We have to make sure that null is assigned to sha1Checksum and
        // md5Checksum if no alphanumeric value is provided. Empty String will
        // fail
        final String sha1Checksum = HawkbitCommonUtil
                .trimAndNullIfEmpty(((TextField) item.getItemProperty(SHA1_CHECKSUM).getValue()).getValue());
        final String md5Checksum = HawkbitCommonUtil
                .trimAndNullIfEmpty(((TextField) item.getItemProperty(MD5_CHECKSUM).getValue()).getValue());
        final String providedFileName = (String) item.getItemProperty(FILE_NAME).getValue();
        final CustomFile customFile = (CustomFile) item.getItemProperty(CUSTOM_FILE).getValue();
        final String[] itemDet = itemId.split("/");
        final String swModuleNameVersion = itemDet[0];

        try (FileInputStream fis = new FileInputStream(newFile)) {

            artifactManagement.create(fis, baseSw.getId(), providedFileName, md5Checksum, sha1Checksum, true,
                    customFile.getMimeType());
            saveUploadStatus(providedFileName, swModuleNameVersion, SPUILabelDefinitions.SUCCESS, "");

        } catch (final ArtifactUploadFailedException | InvalidSHA1HashException | InvalidMD5HashException
                | FileNotFoundException e) {

            saveUploadStatus(providedFileName, swModuleNameVersion, SPUILabelDefinitions.FAILED, e.getMessage());
            LOG.error(ARTIFACT_UPLOAD_EXCEPTION, e);

        } catch (final IOException ex) {
            LOG.error(ARTIFACT_UPLOAD_EXCEPTION, ex);
        } finally {
            if (newFile.exists() && !newFile.delete()) {
                LOG.error("Could not delete temporary file: {}", newFile);
            }
        }
    }

    private void saveUploadStatus(final String fileName, final String baseSwModuleName, final String status,
            final String message) {
        final UploadStatus result = new UploadStatus();
        result.setFileName(fileName);
        result.setBaseSwModuleName(baseSwModuleName);
        result.setUploadResult(status);
        result.setReason(message);
        uploadResultList.add(result);
    }

    public Table getUploadDetailsTable() {
        return uploadDetailsTable;
    }

    public VerticalLayout getUploadArtifactDetails() {
        return uploadArtifactDetails;
    }

    public UploadResultWindow getCurrentUploadResultWindow() {
        return currentUploadResultWindow;
    }

    public void setCurrentUploadResultWindow(final UploadResultWindow currentUploadResultWindow) {
        this.currentUploadResultWindow = currentUploadResultWindow;
    }

}

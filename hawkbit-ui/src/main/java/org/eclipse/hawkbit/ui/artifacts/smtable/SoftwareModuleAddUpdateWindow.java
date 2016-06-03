/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.spring.events.EventBus;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Generates window for Software module add or update.
 */
@SpringComponent
@ViewScope
public class SoftwareModuleAddUpdateWindow extends CustomComponent implements Serializable {

    private static final long serialVersionUID = -5217675246477211483L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UINotification uiNotifcation;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient SoftwareManagement softwareManagement;

    private Label mandatoryLabel;

    private TextField nameTextField;

    private TextField versionTextField;

    private TextField vendorTextField;

    private ComboBox typeComboBox;

    private TextArea descTextArea;

    private CommonDialogWindow window;

    private String oldDescriptionValue;

    private String oldVendorValue;

    private Boolean editSwModule = Boolean.FALSE;

    private Long baseSwModuleId;

    /**
     * Create window for new software module.
     * 
     * @return reference of {@link com.vaadin.ui.Window} to add new software
     *         module.
     */
    public CommonDialogWindow createAddSoftwareModuleWindow() {

        editSwModule = Boolean.FALSE;
        createRequiredComponents();
        createWindow();
        return window;
    }

    /**
     * Create window for update software module.
     * 
     * @param baseSwModuleId
     *            is id of the software module to edit.
     * @return reference of {@link com.vaadin.ui.Window} to update software
     *         module.
     */
    public Window createUpdateSoftwareModuleWindow(final Long baseSwModuleId) {

        editSwModule = Boolean.TRUE;
        this.baseSwModuleId = baseSwModuleId;
        createRequiredComponents();
        createWindow();
        /* populate selected target values to edit. */
        populateValuesOfSwModule();
        nameTextField.setEnabled(false);
        versionTextField.setEnabled(false);
        typeComboBox.setEnabled(false);
        return window;
    }

    private void createRequiredComponents() {
        /* name textfield */
        nameTextField = SPUIComponentProvider.getTextField(i18n.get("textfield.name"), "", ValoTheme.TEXTFIELD_TINY,
                true, null, i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        nameTextField.setId(SPUIComponetIdProvider.SOFT_MODULE_NAME);

        /* version text field */
        versionTextField = SPUIComponentProvider.getTextField(i18n.get("textfield.version"), "",
                ValoTheme.TEXTFIELD_TINY, true, null, i18n.get("textfield.version"), true,
                SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        versionTextField.setId(SPUIComponetIdProvider.SOFT_MODULE_VERSION);

        /* Vendor text field */
        vendorTextField = SPUIComponentProvider.getTextField(i18n.get("textfield.vendor"), "", ValoTheme.TEXTFIELD_TINY,
                false, null, i18n.get("textfield.vendor"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        vendorTextField.setId(SPUIComponetIdProvider.SOFT_MODULE_VENDOR);

        descTextArea = SPUIComponentProvider.getTextArea(i18n.get("textfield.description"), "text-area-style",
                ValoTheme.TEXTAREA_TINY, false, null, i18n.get("textfield.description"),
                SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        descTextArea.setId(SPUIComponetIdProvider.ADD_SW_MODULE_DESCRIPTION);
        addDescriptionTextChangeListener();
        addVendorTextChangeListener();

        /* Label for mandatory symbol */
        mandatoryLabel = new Label(i18n.get("label.mandatory.field"));
        mandatoryLabel.setStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        mandatoryLabel.addStyleName(ValoTheme.LABEL_SMALL);

        typeComboBox = SPUIComponentProvider.getComboBox(i18n.get("upload.swmodule.type"), "", "", null, null, false,
                null, i18n.get("upload.swmodule.type"));
        typeComboBox.setId(SPUIComponetIdProvider.SW_MODULE_TYPE);
        typeComboBox.setStyleName(SPUIDefinitions.COMBO_BOX_SPECIFIC_STYLE + " " + ValoTheme.COMBOBOX_TINY);
        typeComboBox.setNewItemsAllowed(Boolean.FALSE);
        typeComboBox.setImmediate(Boolean.TRUE);

        populateTypeNameCombo();

        resetOldValues();
    }

    /**
    * 
    */
    private void populateTypeNameCombo() {
        typeComboBox.setContainerDataSource(HawkbitCommonUtil.createLazyQueryContainer(
                new BeanQueryFactory<SoftwareModuleTypeBeanQuery>(SoftwareModuleTypeBeanQuery.class)));
        typeComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);

    }

    private void resetOldValues() {
        oldDescriptionValue = null;
        oldVendorValue = null;
    }

    /**
     * Build the window content and get an instance of customDialogWindow
     * 
     */
    private void createWindow() {

        final Label madatoryStarLabel = new Label("*");
        madatoryStarLabel.setStyleName("v-caption v-required-field-indicator");
        madatoryStarLabel.setWidth(null);

        /*
         * The main layout of the window contains mandatory info, textboxes
         * (controller Id, name & description) and action buttons layout
         */
        addStyleName("lay-color");

        final FormLayout formLayout = new FormLayout();
        formLayout.addComponent(typeComboBox);
        formLayout.addComponent(mandatoryLabel);
        formLayout.addComponent(nameTextField);
        formLayout.addComponent(versionTextField);
        formLayout.addComponent(vendorTextField);
        formLayout.addComponent(descTextArea);

        setCompositionRoot(formLayout);

        /* add main layout to the window */
        window = SPUIComponentProvider.getWindow(i18n.get("upload.caption.add.new.swmodule"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW, this, event -> save(), event -> closeThisWindow(), null);
        window.getButtonsLayout().removeStyleName("actionButtonsMargin");
        nameTextField.focus();
    }

    /**
     * add a TextChangeListener to the description TextField
     */
    private void addDescriptionTextChangeListener() {
        descTextArea.addTextChangeListener(event -> {
            if (event.getText().equals(oldDescriptionValue) && vendorTextField.getValue().equals(oldVendorValue)) {
                window.setSaveButtonEnabled(hasDescriptionOrVendorChanged(event));
            } else {
                window.setSaveButtonEnabled(hasDescriptionOrVendorChanged(event));
            }
        });
    }

    /**
     * add a TextChangeListener to the vendor TextField
     */
    private void addVendorTextChangeListener() {
        vendorTextField.addTextChangeListener(event -> {
            if (event.getText().equals(oldVendorValue) && descTextArea.getValue().equals(oldDescriptionValue)) {
                window.setSaveButtonEnabled(hasDescriptionOrVendorChanged(event));
            } else {
                window.setSaveButtonEnabled(hasDescriptionOrVendorChanged(event));
            }
        });
    }

    /**
     * Add new SW module.
     */
    private void addNewBaseSoftware() {
        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(nameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(versionTextField.getValue());
        final String vendor = HawkbitCommonUtil.trimAndNullIfEmpty(vendorTextField.getValue());
        final String description = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
        final String type = typeComboBox.getValue() != null ? typeComboBox.getValue().toString() : null;
        if (mandatoryCheck(name, version, type)) {
            if (HawkbitCommonUtil.isDuplicate(name, version, type)) {
                uiNotifcation.displayValidationError(
                        i18n.get("message.duplicate.softwaremodule", new Object[] { name, version }));
            } else {
                final SoftwareModule newBaseSoftwareModule = HawkbitCommonUtil.addNewBaseSoftware(name, version, vendor,
                        softwareManagement.findSoftwareModuleTypeByName(type), description);
                if (newBaseSoftwareModule != null) {
                    /* display success message */
                    uiNotifcation.displaySuccess(i18n.get("message.save.success", new Object[] {
                            newBaseSoftwareModule.getName() + ":" + newBaseSoftwareModule.getVersion() }));
                    eventBus.publish(this,
                            new SoftwareModuleEvent(BaseEntityEventType.NEW_ENTITY, newBaseSoftwareModule));
                }
                // close the window
                closeThisWindow();
            }
        }
    }

    /**
     * updates a softwareModule
     */
    private void updateSwModule() {
        final String newDesc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
        final String newVendor = HawkbitCommonUtil.trimAndNullIfEmpty(vendorTextField.getValue());
        SoftwareModule newSWModule = softwareManagement.findSoftwareModuleById(baseSwModuleId);
        newSWModule.setVendor(newVendor);
        newSWModule.setDescription(newDesc);
        newSWModule = softwareManagement.updateSoftwareModule(newSWModule);
        if (newSWModule != null) {
            uiNotifcation.displaySuccess(i18n.get("message.save.success",
                    new Object[] { newSWModule.getName() + ":" + newSWModule.getVersion() }));

            eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.UPDATED_ENTITY, newSWModule));
        }
        closeThisWindow();
    }

    /**
     * fill the data of a softwareModule in the content of the window
     */
    private void populateValuesOfSwModule() {
        final SoftwareModule swModle = softwareManagement.findSoftwareModuleById(baseSwModuleId);
        nameTextField.setValue(swModle.getName());
        versionTextField.setValue(swModle.getVersion());
        vendorTextField.setValue(swModle.getVendor() == null ? HawkbitCommonUtil.SP_STRING_EMPTY
                : HawkbitCommonUtil.trimAndNullIfEmpty(swModle.getVendor()));
        descTextArea.setValue(swModle.getDescription() == null ? HawkbitCommonUtil.SP_STRING_EMPTY
                : HawkbitCommonUtil.trimAndNullIfEmpty(swModle.getDescription()));
        oldDescriptionValue = descTextArea.getValue();
        oldVendorValue = vendorTextField.getValue();
        if (swModle.getType().isDeleted()) {
            typeComboBox.addItem(swModle.getType().getName());
        }
        typeComboBox.setValue(swModle.getType().getName());
        window.setSaveButtonEnabled(Boolean.FALSE);
    }

    /**
     * Method to close window.
     */
    private void closeThisWindow() {
        window.close();
        UI.getCurrent().removeWindow(window);
    }

    /**
     * Validation check - Mandatory.
     * 
     * @param name
     *            as String
     * @param version
     *            as version
     * @return boolena as flag
     */
    private boolean mandatoryCheck(final String name, final String version, final String type) {
        boolean isValid = true;
        if (name == null || version == null || type == null) {
            if (name == null) {
                nameTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
            }
            if (version == null) {
                versionTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
            }
            if (type == null) {
                typeComboBox.addStyleName(SPUIStyleDefinitions.SP_COMBOFIELD_ERROR);
            }

            uiNotifcation.displayValidationError(i18n.get("message.mandatory.check"));
            isValid = false;
        }
        return isValid;
    }

    /**
     * saves or updates a softwareModule depending on the information if it is a
     * new softwareModule or an existing one
     */
    private void save() {
        if (editSwModule) {
            updateSwModule();
        } else {
            /* add new or update software module */
            addNewBaseSoftware();
        }
    }

    /**
     * Checks if the description and vendor have changed and set the button
     * enabled/disabled
     * 
     * @param event
     *            TextChangeEvent
     * @return Boolean
     */
    private boolean hasDescriptionOrVendorChanged(final TextChangeEvent event) {
        return !(event.getText().equals(oldDescriptionValue) && vendorTextField.getValue().equals(oldVendorValue));
    }

}

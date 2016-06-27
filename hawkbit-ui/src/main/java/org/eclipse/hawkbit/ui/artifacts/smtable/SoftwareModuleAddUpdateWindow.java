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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
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

    @Autowired
    private transient EntityFactory entityFactory;

    private TextField nameTextField;

    private TextField versionTextField;

    private TextField vendorTextField;

    private ComboBox typeComboBox;

    private TextArea descTextArea;

    private CommonDialogWindow window;

    private String originalDescriptionValue;

    private String originalVendorValue;

    private String originalNameValue;

    private String originalVersionValue;

    private String originalComboBoxValue;

    private Boolean editSwModule = Boolean.FALSE;

    private Long baseSwModuleId;

    private FormLayout formLayout;

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
        nameTextField.setId(SPUIComponentIdProvider.SOFT_MODULE_NAME);

        /* version text field */
        versionTextField = SPUIComponentProvider.getTextField(i18n.get("textfield.version"), "",
                ValoTheme.TEXTFIELD_TINY, true, null, i18n.get("textfield.version"), true,
                SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        versionTextField.setId(SPUIComponentIdProvider.SOFT_MODULE_VERSION);

        /* Vendor text field */
        vendorTextField = SPUIComponentProvider.getTextField(i18n.get("textfield.vendor"), "", ValoTheme.TEXTFIELD_TINY,
                false, null, i18n.get("textfield.vendor"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        vendorTextField.setId(SPUIComponentIdProvider.SOFT_MODULE_VENDOR);

        descTextArea = SPUIComponentProvider.getTextArea(i18n.get("textfield.description"), "text-area-style",
                ValoTheme.TEXTAREA_TINY, false, null, i18n.get("textfield.description"),
                SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        descTextArea.setId(SPUIComponentIdProvider.ADD_SW_MODULE_DESCRIPTION);

        typeComboBox = SPUIComponentProvider.getComboBox(i18n.get("upload.swmodule.type"), "", "", null, null, true,
                null, i18n.get("upload.swmodule.type"));
        typeComboBox.setId(SPUIComponentIdProvider.SW_MODULE_TYPE);
        typeComboBox.setStyleName(SPUIDefinitions.COMBO_BOX_SPECIFIC_STYLE + " " + ValoTheme.COMBOBOX_TINY);
        typeComboBox.setNewItemsAllowed(Boolean.FALSE);
        typeComboBox.setImmediate(Boolean.TRUE);
        populateTypeNameCombo();
    }

    private void populateTypeNameCombo() {
        typeComboBox.setContainerDataSource(HawkbitCommonUtil.createLazyQueryContainer(
                new BeanQueryFactory<SoftwareModuleTypeBeanQuery>(SoftwareModuleTypeBeanQuery.class)));
        typeComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
    }

    private void resetComponents() {

        vendorTextField.clear();
        nameTextField.clear();
        versionTextField.clear();
        descTextArea.clear();
        typeComboBox.clear();

        originalDescriptionValue = null;
        originalVendorValue = null;
        originalComboBoxValue = null;
        originalNameValue = null;
        originalVersionValue = null;
    }

    private void createWindow() {

        resetComponents();

        final Label madatoryStarLabel = new Label("*");
        madatoryStarLabel.setStyleName("v-caption v-required-field-indicator");
        madatoryStarLabel.setWidth(null);

        /*
         * The main layout of the window contains mandatory info, textboxes
         * (controller Id, name & description) and action buttons layout
         */
        addStyleName("lay-color");
        setSizeUndefined();

        formLayout = new FormLayout();
        formLayout.setCaption(null);
        formLayout.addComponent(typeComboBox);
        formLayout.addComponent(nameTextField);
        formLayout.addComponent(versionTextField);
        formLayout.addComponent(vendorTextField);
        formLayout.addComponent(descTextArea);

        setCompositionRoot(formLayout);

        /* add main layout to the window */
        window = SPUIComponentProvider.getWindow(i18n.get("upload.caption.add.new.swmodule"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW, this, event -> saveOrUpdate(), event -> closeThisWindow(), null,
                formLayout, i18n);
        window.getButtonsLayout().removeStyleName("actionButtonsMargin");
        typeComboBox.focus();
    }

    private Map<String, Boolean> getMandatoryFields(final FormLayout formLayout) {
        final Map<String, Boolean> requiredFields = new HashMap<>();
        final Iterator<Component> iterate = formLayout.iterator();
        while (iterate.hasNext()) {
            final Component c = iterate.next();
            if (c instanceof AbstractField && ((AbstractField) c).isRequired()) {
                requiredFields.put(c.getId(), Boolean.FALSE);
            }
        }
        return requiredFields;
    }

    private Map<String, Boolean> geEditedFields() {
        final Map<String, Boolean> editedFields = new HashMap<>();
        editedFields.put(typeComboBox.getId(), Boolean.FALSE);
        editedFields.put(nameTextField.getId(), Boolean.FALSE);
        editedFields.put(vendorTextField.getId(), Boolean.FALSE);
        editedFields.put(versionTextField.getId(), Boolean.FALSE);
        editedFields.put(descTextArea.getId(), Boolean.FALSE);
        return editedFields;
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

        if (!mandatoryCheck(name, version, type)) {
            return;
        }

        if (HawkbitCommonUtil.isDuplicate(name, version, type)) {
            uiNotifcation.displayValidationError(
                    i18n.get("message.duplicate.softwaremodule", new Object[] { name, version }));
        } else {
            final SoftwareModule newBaseSoftwareModule = HawkbitCommonUtil.addNewBaseSoftware(entityFactory, name,
                    version, vendor, softwareManagement.findSoftwareModuleTypeByName(type), description);
            if (newBaseSoftwareModule != null) {
                /* display success message */
                uiNotifcation.displaySuccess(i18n.get("message.save.success",
                        new Object[] { newBaseSoftwareModule.getName() + ":" + newBaseSoftwareModule.getVersion() }));
                eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.NEW_ENTITY, newBaseSoftwareModule));
            }
            // close the window
            closeThisWindow();
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
        originalDescriptionValue = descTextArea.getValue();
        originalVendorValue = vendorTextField.getValue();
        originalComboBoxValue = swModle.getType().getName();
        if (swModle.getType().isDeleted()) {
            typeComboBox.addItem(swModle.getType().getName());
        }
        typeComboBox.setValue(swModle.getType().getName());
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

    private void saveOrUpdate() {
        if (editSwModule) {
            updateSwModule();
        } else {
            addNewBaseSoftware();
        }
    }

    public FormLayout getFormLayout() {
        return formLayout;
    }

}

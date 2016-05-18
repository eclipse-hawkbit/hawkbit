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
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
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

import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Generates window for Software module add or update.
 * 
 *
 */
@SpringComponent
@ViewScope
public class SoftwareModuleAddUpdateWindow implements Serializable {

    private static final long serialVersionUID = -5217675246477211483L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UINotification uiNotifcation;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient SoftwareManagement softwareManagement;

    private Label madatoryLabel;

    private TextField nameTextField;

    private TextField versionTextField;

    private TextField vendorTextField;

    private Button saveSoftware;

    private Button closeWindow;

    private ComboBox typeComboBox;

    private TextArea descTextArea;

    private Window window;

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
    public Window createAddSoftwareModuleWindow() {
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
        nameTextField = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY, true, null,
                i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        nameTextField.setId(SPUIComponetIdProvider.SOFT_MODULE_NAME);

        /* version text field */
        versionTextField = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY, true, null,
                i18n.get("textfield.version"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        versionTextField.setId(SPUIComponetIdProvider.SOFT_MODULE_VERSION);

        /* Vendor text field */
        vendorTextField = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY, false, null,
                i18n.get("textfield.vendor"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        vendorTextField.setId(SPUIComponetIdProvider.SOFT_MODULE_VENDOR);

        descTextArea = SPUIComponentProvider.getTextArea("text-area-style", ValoTheme.TEXTAREA_TINY, false, null,
                i18n.get("textfield.description"), SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        descTextArea.setId(SPUIComponetIdProvider.ADD_SW_MODULE_DESCRIPTION);
        addDescriptionTextChangeListener();
        addVendorTextChangeListener();

        /* Label for mandatory symbol */
        madatoryLabel = new Label(i18n.get("label.mandatory.field"));
        madatoryLabel.setStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        madatoryLabel.addStyleName(ValoTheme.LABEL_SMALL);

        typeComboBox = SPUIComponentProvider.getComboBox("", "", null, null, false, null,
                i18n.get("upload.swmodule.type"));
        typeComboBox.setId(SPUIComponetIdProvider.SW_MODULE_TYPE);
        typeComboBox.setStyleName(SPUIDefinitions.COMBO_BOX_SPECIFIC_STYLE + " " + ValoTheme.COMBOBOX_TINY);
        typeComboBox.setNewItemsAllowed(Boolean.FALSE);
        typeComboBox.setImmediate(Boolean.TRUE);

        populateTypeNameCombo();

        /* save or update button */
        saveSoftware = SPUIComponentProvider.getButton(SPUIComponetIdProvider.SOFT_MODULE_SAVE, "", "", "", true,
                FontAwesome.SAVE, SPUIButtonStyleSmallNoBorder.class);
        saveSoftware.addClickListener(event -> {
            if (editSwModule) {
                updateSwModule();
            } else {
                /* add new or update software module */
                addNewBaseSoftware();
            }
        });

        /* close button */
        closeWindow = SPUIComponentProvider.getButton(SPUIComponetIdProvider.SOFT_MODULE_DISCARD, "", "", "", true,
                FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        /* Just close this window when this button is clicked */
        closeWindow.addClickListener(event -> closeThisWindow());

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
     * Keep UI components on Layout.
     * 
     * @return
     */
    private void createWindow() {
        /* action button layout (save & dicard) */
        final HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeFull();
        buttonsLayout.addComponents(saveSoftware, closeWindow);
        buttonsLayout.setComponentAlignment(saveSoftware, Alignment.BOTTOM_LEFT);
        buttonsLayout.setComponentAlignment(closeWindow, Alignment.BOTTOM_RIGHT);
        buttonsLayout.addStyleName("window-style");

        final Label madatoryStarLabel = new Label("*");
        madatoryStarLabel.setStyleName("v-caption v-required-field-indicator");
        madatoryStarLabel.setWidth(null);
        final HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setSizeFull();
        hLayout.addComponents(typeComboBox, madatoryStarLabel);
        hLayout.setComponentAlignment(typeComboBox, Alignment.TOP_LEFT);
        hLayout.setComponentAlignment(madatoryStarLabel, Alignment.TOP_RIGHT);
        hLayout.setExpandRatio(typeComboBox, 0.8f);

        /*
         * The main layout of the window contains mandatory info, textboxes
         * (controller Id, name & description) and action buttons layout
         */
        final VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSpacing(Boolean.TRUE);
        mainLayout.addStyleName("lay-color");
        mainLayout.addComponent(madatoryLabel);
        mainLayout.setComponentAlignment(madatoryLabel, Alignment.MIDDLE_LEFT);
        mainLayout.addComponent(hLayout);
        mainLayout.setComponentAlignment(hLayout, Alignment.MIDDLE_LEFT);
        mainLayout.addComponents(nameTextField, versionTextField, vendorTextField, descTextArea, buttonsLayout);
        
        /* add main layout to the window */
        window = SPUIComponentProvider.getWindow(i18n.get("upload.caption.add.new.swmodule"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW);
        window.setContent(mainLayout);
        window.setModal(true);
        nameTextField.focus();
    }

    private void addDescriptionTextChangeListener() {
        descTextArea.addTextChangeListener(event -> {
            if (event.getText().equals(oldDescriptionValue) && vendorTextField.getValue().equals(oldVendorValue)) {
                saveSoftware.setEnabled(false);
            } else {
                saveSoftware.setEnabled(true);
            }
        });
    }

    private void addVendorTextChangeListener() {
        vendorTextField.addTextChangeListener(event -> {
            if (event.getText().equals(oldVendorValue) && descTextArea.getValue().equals(oldDescriptionValue)) {
                saveSoftware.setEnabled(false);
            } else {
                saveSoftware.setEnabled(true);
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
        saveSoftware.setEnabled(Boolean.FALSE);
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
}

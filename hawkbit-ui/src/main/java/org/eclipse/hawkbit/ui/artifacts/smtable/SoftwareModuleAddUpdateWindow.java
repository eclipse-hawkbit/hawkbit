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

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIWindowDecorator;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
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

    private Boolean editSwModule = Boolean.FALSE;

    private Long baseSwModuleId;

    private FormLayout formLayout;

    /**
     * Initialize Distribution Add and Edit Window.
     */
    @PostConstruct
    void init() {
        createRequiredComponents();
    }

    /**
     * Create window for new software module.
     * 
     * @return reference of {@link com.vaadin.ui.Window} to add new software
     *         module.
     */
    public CommonDialogWindow createAddSoftwareModuleWindow() {
        return createUpdateSoftwareModuleWindow(null);
    }

    /**
     * Create window for update software module.
     * 
     * @param baseSwModuleId
     *            is id of the software module to edit.
     * @return reference of {@link com.vaadin.ui.Window} to update software
     *         module.
     */
    public CommonDialogWindow createUpdateSoftwareModuleWindow(final Long baseSwModuleId) {
        this.baseSwModuleId = baseSwModuleId;
        resetComponents();
        populateValuesOfSwModule();
        createWindow();
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
        editSwModule = Boolean.FALSE;
    }

    private void createWindow() {
        final Label madatoryStarLabel = new Label("*");
        madatoryStarLabel.setStyleName("v-caption v-required-field-indicator");
        madatoryStarLabel.setWidth(null);
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

        window = SPUIWindowDecorator.getWindow(i18n.get("upload.caption.add.new.swmodule"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW, this, event -> saveOrUpdate(), null, null, formLayout, i18n);
        window.getButtonsLayout().removeStyleName("actionButtonsMargin");
        window.setCloseListener(() -> !isDuplicate());

        nameTextField.setEnabled(!editSwModule);
        versionTextField.setEnabled(!editSwModule);
        typeComboBox.setEnabled(!editSwModule);

        typeComboBox.focus();
    }

    private void addNewBaseSoftware() {
        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(nameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(versionTextField.getValue());
        final String vendor = HawkbitCommonUtil.trimAndNullIfEmpty(vendorTextField.getValue());
        final String description = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
        final String type = typeComboBox.getValue() != null ? typeComboBox.getValue().toString() : null;

        if (isDuplicate()) {
            uiNotifcation.displayValidationError(
                    i18n.get("message.duplicate.softwaremodule", new Object[] { name, version }));
            return;
        }
        final SoftwareModule newBaseSoftwareModule = HawkbitCommonUtil.addNewBaseSoftware(entityFactory, name, version,
                vendor, softwareManagement.findSoftwareModuleTypeByName(type), description);
        if (newBaseSoftwareModule != null) {
            /* display success message */
            uiNotifcation.displaySuccess(i18n.get("message.save.success",
                    new Object[] { newBaseSoftwareModule.getName() + ":" + newBaseSoftwareModule.getVersion() }));
            eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.NEW_ENTITY, newBaseSoftwareModule));
        }
    }

    private boolean isDuplicate() {
        final String name = nameTextField.getValue();
        final String version = versionTextField.getValue();
        final String type = typeComboBox.getValue() != null ? typeComboBox.getValue().toString() : null;

        final SoftwareManagement swMgmtService = SpringContextHelper.getBean(SoftwareManagement.class);
        final SoftwareModule swModule = swMgmtService.findSoftwareModuleByNameAndVersion(name, version,
                swMgmtService.findSoftwareModuleTypeByName(type));
        return swModule != null;
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
    }

    /**
     * fill the data of a softwareModule in the content of the window
     */
    private void populateValuesOfSwModule() {
        if (baseSwModuleId == null) {
            return;
        }
        editSwModule = Boolean.TRUE;
        final SoftwareModule swModle = softwareManagement.findSoftwareModuleById(baseSwModuleId);
        nameTextField.setValue(swModle.getName());
        versionTextField.setValue(swModle.getVersion());
        vendorTextField.setValue(swModle.getVendor() == null ? HawkbitCommonUtil.SP_STRING_EMPTY
                : HawkbitCommonUtil.trimAndNullIfEmpty(swModle.getVendor()));
        descTextArea.setValue(swModle.getDescription() == null ? HawkbitCommonUtil.SP_STRING_EMPTY
                : HawkbitCommonUtil.trimAndNullIfEmpty(swModle.getDescription()));
        if (swModle.getType().isDeleted()) {
            typeComboBox.addItem(swModle.getType().getName());
        }
        typeComboBox.setValue(swModle.getType().getName());
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

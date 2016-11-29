/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

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
public class SoftwareModuleAddUpdateWindow extends CustomComponent {

    private static final long serialVersionUID = -5217675246477211483L;

    private final I18N i18n;

    private final UINotification uiNotifcation;

    private final transient EventBus.UIEventBus eventBus;

    private final SoftwareManagement softwareManagement;

    private final EntityFactory entityFactory;

    private TextField nameTextField;

    private TextField versionTextField;

    private TextField vendorTextField;

    private ComboBox typeComboBox;

    private TextArea descTextArea;

    private Boolean editSwModule = Boolean.FALSE;

    private Long baseSwModuleId;

    private FormLayout formLayout;

    public SoftwareModuleAddUpdateWindow(final I18N i18n, final UINotification uiNotifcation, final UIEventBus eventBus,
            final SoftwareManagement softwareManagement, final EntityFactory entityFactory) {
        this.i18n = i18n;
        this.uiNotifcation = uiNotifcation;
        this.eventBus = eventBus;
        this.softwareManagement = softwareManagement;
        this.entityFactory = entityFactory;

        createRequiredComponents();
    }

    /**
     * Save or update the sw module.
     */
    private final class SaveOnDialogCloseListener implements SaveDialogCloseListener {
        @Override
        public void saveOrUpdate() {
            if (editSwModule) {
                updateSwModule();
                return;
            }
            addNewBaseSoftware();
        }

        @Override
        public boolean canWindowSaveOrUpdate() {
            return editSwModule || !isDuplicate();
        }
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
        populateTypeNameCombo();
        populateValuesOfSwModule();
        return createWindow();
    }

    private void createRequiredComponents() {

        nameTextField = createTextField("textfield.name", UIComponentIdProvider.SOFT_MODULE_NAME);

        versionTextField = createTextField("textfield.version", UIComponentIdProvider.SOFT_MODULE_VERSION);

        vendorTextField = createTextField("textfield.vendor", UIComponentIdProvider.SOFT_MODULE_VENDOR);
        vendorTextField.setRequired(false);

        descTextArea = new TextAreaBuilder().caption(i18n.get("textfield.description")).style("text-area-style")
                .prompt(i18n.get("textfield.description")).id(UIComponentIdProvider.ADD_SW_MODULE_DESCRIPTION)
                .buildTextComponent();

        typeComboBox = SPUIComponentProvider.getComboBox(i18n.get("upload.swmodule.type"), "", null, null, true, null,
                i18n.get("upload.swmodule.type"));
        typeComboBox.setId(UIComponentIdProvider.SW_MODULE_TYPE);
        typeComboBox.setStyleName(SPUIDefinitions.COMBO_BOX_SPECIFIC_STYLE + " " + ValoTheme.COMBOBOX_TINY);
        typeComboBox.setNewItemsAllowed(Boolean.FALSE);
        typeComboBox.setImmediate(Boolean.TRUE);
    }

    private TextField createTextField(final String in18Key, final String id) {
        return new TextFieldBuilder().caption(i18n.get(in18Key)).required(true).prompt(i18n.get(in18Key))
                .immediate(true).id(id).buildTextComponent();
    }

    private void populateTypeNameCombo() {
        typeComboBox.setContainerDataSource(
                HawkbitCommonUtil.createLazyQueryContainer(new BeanQueryFactory<>(SoftwareModuleTypeBeanQuery.class)));
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

    private CommonDialogWindow createWindow() {
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

        final CommonDialogWindow window = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW)
                .caption(i18n.get("upload.caption.add.new.swmodule")).content(this).layout(formLayout).i18n(i18n)
                .saveDialogCloseListener(new SaveOnDialogCloseListener()).buildCommonDialogWindow();
        nameTextField.setEnabled(!editSwModule);
        versionTextField.setEnabled(!editSwModule);
        typeComboBox.setEnabled(!editSwModule);

        typeComboBox.focus();

        return window;
    }

    private void addNewBaseSoftware() {
        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(nameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(versionTextField.getValue());
        final String vendor = HawkbitCommonUtil.trimAndNullIfEmpty(vendorTextField.getValue());
        final String description = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
        final String type = typeComboBox.getValue() != null ? typeComboBox.getValue().toString() : null;

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

        if (swModule != null) {
            uiNotifcation.displayValidationError(
                    i18n.get("message.duplicate.softwaremodule", new Object[] { name, version }));
            return true;
        }
        return false;
    }

    /**
     * updates a softwareModule
     */
    private void updateSwModule() {
        final SoftwareModule newSWModule = softwareManagement.updateSoftwareModule(entityFactory.softwareModule()
                .update(baseSwModuleId).description(descTextArea.getValue()).vendor(vendorTextField.getValue()));
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

    public FormLayout getFormLayout() {
        return formLayout;
    }

}

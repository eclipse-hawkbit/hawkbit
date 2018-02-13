/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.EmptyStringValidator;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.google.common.collect.Sets;
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

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private final UINotification uiNotifcation;

    private final transient EventBus.UIEventBus eventBus;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private final transient EntityFactory entityFactory;

    private TextField nameTextField;

    private TextField versionTextField;

    private TextField vendorTextField;

    private ComboBox typeComboBox;

    private TextArea descTextArea;

    private Boolean editSwModule = Boolean.FALSE;

    private Long baseSwModuleId;

    private FormLayout formLayout;

    private final AbstractTable<SoftwareModule> softwareModuleTable;

    /**
     * Constructor for SoftwareModuleAddUpdateWindow
     * 
     * @param i18n
     *            I18N
     * @param uiNotifcation
     *            UINotification
     * @param eventBus
     *            UIEventBus
     * @param softwareModuleManagement
     *            management for {@link SoftwareModule}s
     * @param softwareModuleTypeManagement
     *            management for {@link SoftwareModuleType}s
     * @param entityFactory
     *            EntityFactory
     */
    public SoftwareModuleAddUpdateWindow(final VaadinMessageSource i18n, final UINotification uiNotifcation,
            final UIEventBus eventBus, final SoftwareModuleManagement softwareModuleManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement, final EntityFactory entityFactory,
            final AbstractTable<SoftwareModule> softwareModuleTable) {
        this.i18n = i18n;
        this.uiNotifcation = uiNotifcation;
        this.eventBus = eventBus;
        this.softwareModuleManagement = softwareModuleManagement;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.entityFactory = entityFactory;
        this.softwareModuleTable = softwareModuleTable;

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

        private void addNewBaseSoftware() {
            final String name = nameTextField.getValue();
            final String version = versionTextField.getValue();
            final String vendor = vendorTextField.getValue();
            final String description = descTextArea.getValue();
            final String type = typeComboBox.getValue() != null ? typeComboBox.getValue().toString() : null;

            final SoftwareModuleType softwareModuleTypeByName = softwareModuleTypeManagement.getByName(type)
                    .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, type));
            final SoftwareModuleCreate softwareModule = entityFactory.softwareModule().create()
                    .type(softwareModuleTypeByName).name(name).version(version).description(description).vendor(vendor);

            final SoftwareModule newSoftwareModule = softwareModuleManagement.create(softwareModule);
            eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.ADD_ENTITY, newSoftwareModule));
            uiNotifcation.displaySuccess(i18n.getMessage("message.save.success",
                    new Object[] { newSoftwareModule.getName() + ":" + newSoftwareModule.getVersion() }));
            softwareModuleTable.setValue(Sets.newHashSet(newSoftwareModule.getId()));
        }

        private boolean isDuplicate() {
            final String name = nameTextField.getValue();
            final String version = versionTextField.getValue();
            final String type = typeComboBox.getValue() != null ? typeComboBox.getValue().toString() : null;

            final Optional<Long> moduleType = softwareModuleTypeManagement.getByName(type)
                    .map(SoftwareModuleType::getId);
            if (moduleType.isPresent() && softwareModuleManagement
                    .getByNameAndVersionAndType(name, version, moduleType.get()).isPresent()) {
                uiNotifcation.displayValidationError(
                        i18n.getMessage("message.duplicate.softwaremodule", new Object[] { name, version }));
                return true;
            }
            return false;
        }

        /**
         * updates a softwareModule
         */
        private void updateSwModule() {
            final SoftwareModule newSWModule = softwareModuleManagement.update(entityFactory.softwareModule()
                    .update(baseSwModuleId).description(descTextArea.getValue()).vendor(vendorTextField.getValue()));
            if (newSWModule != null) {
                uiNotifcation.displaySuccess(i18n.getMessage("message.save.success",
                        new Object[] { newSWModule.getName() + ":" + newSWModule.getVersion() }));

                eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.UPDATED_ENTITY, newSWModule));
            }
        }
    }

    /**
     * Creates window for new software module.
     * 
     * @return reference of {@link com.vaadin.ui.Window} to add new software
     *         module.
     */
    public CommonDialogWindow createAddSoftwareModuleWindow() {
        return createUpdateSoftwareModuleWindow(null);
    }

    /**
     * Creates window for update software module.
     * 
     * @param baseSwModuleId
     *            id of the software module to edit.
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
        nameTextField.addValidator(new EmptyStringValidator(i18n));

        versionTextField = createTextField("textfield.version", UIComponentIdProvider.SOFT_MODULE_VERSION);
        versionTextField.addValidator(new EmptyStringValidator(i18n));

        vendorTextField = createTextField("textfield.vendor", UIComponentIdProvider.SOFT_MODULE_VENDOR);
        vendorTextField.setRequired(false);
        vendorTextField.setNullRepresentation("");

        descTextArea = new TextAreaBuilder().caption(i18n.getMessage("textfield.description")).style("text-area-style")
                .prompt(i18n.getMessage("textfield.description")).id(UIComponentIdProvider.ADD_SW_MODULE_DESCRIPTION)
                .buildTextComponent();
        descTextArea.setNullRepresentation("");

        typeComboBox = SPUIComponentProvider.getComboBox(i18n.getMessage("upload.swmodule.type"), "", null, null, true,
                null, i18n.getMessage("upload.swmodule.type"));
        typeComboBox.setId(UIComponentIdProvider.SW_MODULE_TYPE);
        typeComboBox.setStyleName(SPUIDefinitions.COMBO_BOX_SPECIFIC_STYLE + " " + ValoTheme.COMBOBOX_TINY);
        typeComboBox.setNewItemsAllowed(Boolean.FALSE);
        typeComboBox.setImmediate(Boolean.TRUE);
    }

    private TextField createTextField(final String in18Key, final String id) {
        return new TextFieldBuilder().caption(i18n.getMessage(in18Key)).required(true).prompt(i18n.getMessage(in18Key))
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
                .caption(i18n.getMessage("upload.caption.add.new.swmodule")).content(this).layout(formLayout).i18n(i18n)
                .saveDialogCloseListener(new SaveOnDialogCloseListener()).buildCommonDialogWindow();
        nameTextField.setEnabled(!editSwModule);
        versionTextField.setEnabled(!editSwModule);
        typeComboBox.setEnabled(!editSwModule);
        typeComboBox.focus();

        return window;
    }

    /**
     * fill the data of a softwareModule in the content of the window
     */
    private void populateValuesOfSwModule() {
        if (baseSwModuleId == null) {
            return;
        }
        editSwModule = Boolean.TRUE;
        softwareModuleManagement.get(baseSwModuleId).ifPresent(swModule -> {
            nameTextField.setValue(swModule.getName());
            versionTextField.setValue(swModule.getVersion());
            vendorTextField.setValue(swModule.getVendor());
            descTextArea.setValue(swModule.getDescription());

            if (swModule.getType().isDeleted()) {
                typeComboBox.addItem(swModule.getType().getName());
            }
            typeComboBox.setValue(swModule.getType().getName());
        });
    }

    public FormLayout getFormLayout() {
        return formLayout;
    }

}

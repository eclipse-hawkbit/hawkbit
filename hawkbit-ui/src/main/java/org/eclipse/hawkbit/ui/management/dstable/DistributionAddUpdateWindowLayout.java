/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class DistributionAddUpdateWindowLayout extends VerticalLayout {

    private static final long serialVersionUID = -5602182034230568435L;

    private static final Logger LOG = LoggerFactory.getLogger(DistributionAddUpdateWindowLayout.class);

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UINotification notificationMessage;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private transient SystemManagement systemManagement;

    @Autowired
    private transient TenantMetaDataRepository tenantMetaDataRepository;

    @Autowired
    private transient UiProperties uiProperties;

    private TextField distNameTextField;
    private TextField distVersionTextField;
    private Label madatoryLabel;
    private TextArea descTextArea;
    private CheckBox reqMigStepCheckbox;
    private ComboBox distsetTypeNameComboBox;
    private boolean editDistribution = Boolean.FALSE;
    private Long editDistId;
    private CommonDialogWindow addDistributionWindow;
    private String originalDistName;
    private String originalDistVersion;
    private String originalDistDescription;
    private Boolean originalReqMigStep;
    private String originalDistSetType;
    private final List<Component> changedComponents = new ArrayList<>();
    private ValueChangeListener reqMigStepCheckboxListerner;
    private TextChangeListener descTextAreaListener;
    private TextChangeListener distNameTextFieldListener;
    private TextChangeListener distVersionTextFieldListener;
    private ValueChangeListener distsetTypeNameComboBoxListener;

    private FormLayout formLayout;

    /**
     * Initialize Distribution Add and Edit Window.
     */
    @PostConstruct
    void init() {
        createRequiredComponents();
        buildLayout();
    }

    private void buildLayout() {

        /*
         * The main layout of the window contains mandatory info, textboxes
         * (controller Id, name & description) and action buttons layout
         */
        setSpacing(Boolean.TRUE);
        addStyleName("lay-color");
        setSizeUndefined();

        formLayout = new FormLayout();
        formLayout.addComponent(madatoryLabel);
        formLayout.addComponent(distsetTypeNameComboBox);
        formLayout.addComponent(distNameTextField);
        formLayout.addComponent(distVersionTextField);
        formLayout.addComponent(descTextArea);
        formLayout.addComponent(reqMigStepCheckbox);

        addComponents(formLayout);

        distNameTextField.focus();
    }

    /**
     * Create required UI components.
     */
    private void createRequiredComponents() {
        distNameTextField = SPUIComponentProvider.getTextField(i18n.get("textfield.name"), "", ValoTheme.TEXTFIELD_TINY,
                true, null, i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        distNameTextField.setId(SPUIComponetIdProvider.DIST_ADD_NAME);
        distNameTextField.setNullRepresentation("");

        distVersionTextField = SPUIComponentProvider.getTextField(i18n.get("textfield.version"), "",
                ValoTheme.TEXTFIELD_TINY, true, null, i18n.get("textfield.version"), true,
                SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        distVersionTextField.setId(SPUIComponetIdProvider.DIST_ADD_VERSION);
        distVersionTextField.setNullRepresentation("");

        distsetTypeNameComboBox = SPUIComponentProvider.getComboBox(i18n.get("label.combobox.type"), "", "", null, "",
                false, "", i18n.get("label.combobox.type"));
        distsetTypeNameComboBox.setImmediate(true);
        distsetTypeNameComboBox.setNullSelectionAllowed(false);
        distsetTypeNameComboBox.setId(SPUIComponetIdProvider.DIST_ADD_DISTSETTYPE);

        descTextArea = SPUIComponentProvider.getTextArea(i18n.get("textfield.description"), "text-area-style",
                ValoTheme.TEXTAREA_TINY, false, null, i18n.get("textfield.description"),
                SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        descTextArea.setId(SPUIComponetIdProvider.DIST_ADD_DESC);
        descTextArea.setNullRepresentation("");

        /* Label for mandatory symbol */
        madatoryLabel = new Label(i18n.get("label.mandatory.field"));
        madatoryLabel.setStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR + " " + ValoTheme.LABEL_SMALL);

        reqMigStepCheckbox = SPUIComponentProvider.getCheckBox(i18n.get("checkbox.dist.required.migration.step"),
                "dist-checkbox-style", null, false, "");
        reqMigStepCheckbox.addStyleName(ValoTheme.CHECKBOX_SMALL);
        reqMigStepCheckbox.setId(SPUIComponetIdProvider.DIST_ADD_MIGRATION_CHECK);

    }

    /**
     * Get the LazyQueryContainer instance for DistributionSetTypes.
     *
     * @return
     */
    private LazyQueryContainer getDistSetTypeLazyQueryContainer() {
        final Map<String, Object> queryConfig = new HashMap<>();
        final BeanQueryFactory<DistributionSetTypeBeanQuery> dtQF = new BeanQueryFactory<>(
                DistributionSetTypeBeanQuery.class);
        dtQF.setQueryConfiguration(queryConfig);

        final LazyQueryContainer disttypeContainer = new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.DIST_TYPE_SIZE, SPUILabelDefinitions.VAR_NAME), dtQF);

        disttypeContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", true, true);

        return disttypeContainer;
    }

    private void enableSaveButton() {
        addDistributionWindow.setSaveButtonEnabled(true);
    }

    private DistributionSetType getDefaultDistributionSetType() {
        final TenantMetaData tenantMetaData = tenantMetaDataRepository
                .findByTenantIgnoreCase(systemManagement.currentTenant());
        return tenantMetaData.getDefaultDsType();
    }

    private void disableSaveButton() {
        addDistributionWindow.setSaveButtonEnabled(false);
    }

    private void saveDistribution() {
        /* add new or update target */
        if (editDistribution) {
            updateDistribution();
        } else {
            addNewDistribution();
        }

    }

    /**
     * Update Distribution.
     */
    private void updateDistribution() {
        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(distNameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(distVersionTextField.getValue());
        final String distSetTypeName = HawkbitCommonUtil
                .trimAndNullIfEmpty((String) distsetTypeNameComboBox.getValue());

        if (mandatoryCheck(name, version, distSetTypeName) && duplicateCheck(name, version)) {
            final DistributionSet currentDS = distributionSetManagement.findDistributionSetByIdWithDetails(editDistId);
            final String desc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
            final boolean isMigStepReq = reqMigStepCheckbox.getValue();

            /* identify the changes */
            setDistributionValues(currentDS, name, version, distSetTypeName, desc, isMigStepReq);
            try {
                distributionSetManagement.updateDistributionSet(currentDS);
                notificationMessage.displaySuccess(i18n.get("message.new.dist.save.success",
                        new Object[] { currentDS.getName(), currentDS.getVersion() }));
                // update table row+details layout
                eventBus.publish(this, new DistributionTableEvent(BaseEntityEventType.UPDATED_ENTITY, currentDS));
            } catch (final EntityAlreadyExistsException entityAlreadyExistsException) {
                LOG.error("Update distribution failed {}", entityAlreadyExistsException);
                notificationMessage.displayValidationError(
                        i18n.get("message.distribution.no.update", currentDS.getName() + ":" + currentDS.getVersion()));
            }
            closeThisWindow();
        }
    }

    private void addListeners() {
        reqMigStepCheckboxListerner = event -> checkValueChanged(originalReqMigStep, event);
        descTextAreaListener = event -> checkValueChanged(originalDistDescription, event);
        distNameTextFieldListener = event -> checkValueChanged(originalDistName, event);
        distVersionTextFieldListener = event -> checkValueChanged(originalDistVersion, event);
        distsetTypeNameComboBoxListener = event -> checkValueChanged(originalDistSetType, event);
        reqMigStepCheckbox.addValueChangeListener(reqMigStepCheckboxListerner);
        descTextArea.addTextChangeListener(descTextAreaListener);
        distNameTextField.addTextChangeListener(distNameTextFieldListener);
        distVersionTextField.addTextChangeListener(distVersionTextFieldListener);
        distsetTypeNameComboBox.addValueChangeListener(distsetTypeNameComboBoxListener);
    }

    /**
     * Add new Distribution set.
     */
    private void addNewDistribution() {
        editDistribution = Boolean.FALSE;
        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(distNameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(distVersionTextField.getValue());
        final String distSetTypeName = HawkbitCommonUtil
                .trimAndNullIfEmpty((String) distsetTypeNameComboBox.getValue());

        if (mandatoryCheck(name, version, distSetTypeName) && duplicateCheck(name, version)) {
            final String desc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
            final boolean isMigStepReq = reqMigStepCheckbox.getValue();
            DistributionSet newDist = new DistributionSet();

            setDistributionValues(newDist, name, version, distSetTypeName, desc, isMigStepReq);
            newDist = distributionSetManagement.createDistributionSet(newDist);

            notificationMessage.displaySuccess(i18n.get("message.new.dist.save.success",
                    new Object[] { newDist.getName(), newDist.getVersion() }));
            /* close the window */
            closeThisWindow();

            eventBus.publish(this, new DistributionTableEvent(BaseEntityEventType.NEW_ENTITY, newDist));
        }
    }

    /**
     * Close window.
     */
    private void closeThisWindow() {
        addDistributionWindow.close();
        UI.getCurrent().removeWindow(addDistributionWindow);
    }

    /**
     * Set Values for Distribution set.
     *
     * @param distributionSet
     *            as reference
     * @param name
     *            as string
     * @param version
     *            as string
     * @param desc
     *            as string
     * @param isMigStepReq
     *            as string
     */
    private void setDistributionValues(final DistributionSet distributionSet, final String name, final String version,
            final String distSetTypeName, final String desc, final boolean isMigStepReq) {
        distributionSet.setName(name);
        distributionSet.setVersion(version);
        distributionSet.setType(distributionSetManagement.findDistributionSetTypeByName(distSetTypeName));
        distributionSet.setDescription(desc != null ? desc : "");
        distributionSet.setRequiredMigrationStep(isMigStepReq);
    }

    private boolean duplicateCheck(final String name, final String version) {
        final DistributionSet existingDs = distributionSetManagement.findDistributionSetByNameAndVersion(name, version);
        /*
         * Distribution should not exists with the same name & version. Display
         * error message, when the "existingDs" is not null and it is add window
         * (or) when the "existingDs" is not null and it is edit window and the
         * distribution Id of the edit window is different then the "existingDs"
         */
        if (existingDs != null && !existingDs.getId().equals(editDistId)) {
            distNameTextField.addStyleName("v-textfield-error");
            distVersionTextField.addStyleName("v-textfield-error");
            notificationMessage.displayValidationError(
                    i18n.get("message.duplicate.dist", new Object[] { existingDs.getName(), existingDs.getVersion() }));

            return false;
        } else {
            return true;
        }
    }

    /**
     * Mandatory Check.
     *
     * @param name
     *            as String
     * @param version
     *            as String
     * @param selectedJVM
     *            as String
     * @param selectedAgentHub
     *            as String
     * @param selectedOs
     *            as String
     * @return boolean as flag
     */
    private boolean mandatoryCheck(final String name, final String version, final String distSetTypeName) {

        if (name == null || version == null || distSetTypeName == null) {
            if (name == null) {
                distNameTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
            }
            if (version == null) {
                distVersionTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
            }
            if (distSetTypeName == null) {
                distsetTypeNameComboBox.addStyleName(SPUIStyleDefinitions.SP_COMBOFIELD_ERROR);
            }

            notificationMessage.displayValidationError(i18n.get("message.mandatory.check"));
            return false;
        }

        return true;
    }

    private void discardDistribution() {
        /* Just close this window */
        distsetTypeNameComboBox.removeValueChangeListener(distsetTypeNameComboBoxListener);
        closeThisWindow();
    }

    /**
     * clear all the fields.
     */
    public void resetComponents() {
        editDistribution = Boolean.FALSE;
        distNameTextField.clear();
        distNameTextField.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        distVersionTextField.clear();
        distVersionTextField.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        distsetTypeNameComboBox.removeStyleName(SPUIStyleDefinitions.SP_COMBOFIELD_ERROR);
        descTextArea.clear();
        reqMigStepCheckbox.clear();
        if (addDistributionWindow != null) {
            addDistributionWindow.setSaveButtonEnabled(true);
        }
        removeListeners();
        changedComponents.clear();
    }

    private void populateRequiredComponents() {
        populateDistSetTypeNameCombo();
    }

    private void removeListeners() {
        reqMigStepCheckbox.removeValueChangeListener(reqMigStepCheckboxListerner);
        descTextArea.removeTextChangeListener(descTextAreaListener);
        distNameTextField.removeTextChangeListener(distNameTextFieldListener);
        distVersionTextField.removeTextChangeListener(distVersionTextFieldListener);
    }

    public void setOriginalDistName(final String originalDistName) {
        this.originalDistName = originalDistName;
    }

    public void setOriginalDistVersion(final String originalDistVersion) {
        this.originalDistVersion = originalDistVersion;
    }

    public void setOriginalDistDescription(final String originalDistDescription) {
        this.originalDistDescription = originalDistDescription;
    }

    private void checkValueChanged(final String originalValue, final TextChangeEvent event) {
        if (editDistribution) {
            final String newValue = event.getText();
            if (!originalValue.equalsIgnoreCase(newValue)) {
                changedComponents.add(event.getComponent());
            } else {
                changedComponents.remove(event.getComponent());
            }
            enableDisableSaveButton();
        }
    }

    private void checkValueChanged(final Boolean originalValue, final ValueChangeEvent event) {
        if (editDistribution) {
            if (!originalValue.equals(event.getProperty().getValue())) {
                changedComponents.add(reqMigStepCheckbox);
            } else {
                changedComponents.remove(reqMigStepCheckbox);
            }
            enableDisableSaveButton();
        }
    }

    private void checkValueChanged(final String originalValue, final ValueChangeEvent event) {
        if (editDistribution) {
            if (!originalValue.equals(event.getProperty().getValue())) {
                changedComponents.add(distsetTypeNameComboBox);
            } else {
                changedComponents.remove(distsetTypeNameComboBox);
            }
            enableDisableSaveButton();
        }
    }

    private void enableDisableSaveButton() {
        if (changedComponents.isEmpty()) {
            disableSaveButton();
        } else {
            enableSaveButton();
        }
    }

    private void setOriginalReqMigStep(final Boolean originalReqMigStep) {
        this.originalReqMigStep = originalReqMigStep;
    }

    /**
     * populate data.
     *
     * @param editDistId
     */
    public void populateValuesOfDistribution(final Long editDistId) {
        this.editDistId = editDistId;
        editDistribution = Boolean.TRUE;
        addDistributionWindow.setSaveButtonEnabled(false);
        final DistributionSet distSet = distributionSetManagement.findDistributionSetByIdWithDetails(editDistId);
        if (distSet != null) {
            distNameTextField.setValue(distSet.getName());
            distVersionTextField.setValue(distSet.getVersion());
            if (distSet.getType().isDeleted()) {
                distsetTypeNameComboBox.addItem(distSet.getType().getName());
            }
            distsetTypeNameComboBox.setValue(distSet.getType().getName());
            reqMigStepCheckbox.setValue(distSet.isRequiredMigrationStep());
            if (distSet.getDescription() != null) {
                descTextArea.setValue(distSet.getDescription());
            }
            setOriginalDistName(distSet.getName());
            setOriginalDistVersion(distSet.getVersion());
            setOriginalDistDescription(distSet.getDescription());
            setOriginalReqMigStep(distSet.isRequiredMigrationStep());
            setOriginalDistSetTYpe(distSet.getType().getName());
            addListeners();
        }
    }

    public CommonDialogWindow getWindow() {
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        populateRequiredComponents();
        resetComponents();
        addDistributionWindow = SPUIComponentProvider.getWindow(i18n.get("caption.add.new.dist"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW, this, event -> saveDistribution(),
                event -> discardDistribution());
        addDistributionWindow.removeStyleName("actionButtonsMargin");

        return addDistributionWindow;
    }

    /**
     * Populate DistributionSet Type name combo.
     */
    public void populateDistSetTypeNameCombo() {
        distsetTypeNameComboBox.setContainerDataSource(getDistSetTypeLazyQueryContainer());
        distsetTypeNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        distsetTypeNameComboBox.setValue(getDefaultDistributionSetType().getName());
    }

    /**
     * @param originalDistSetTYpe
     *            the originalDistSetTYpe to set
     */
    public void setOriginalDistSetTYpe(final String originalDistSetType) {
        this.originalDistSetType = originalDistSetType;
    }
}

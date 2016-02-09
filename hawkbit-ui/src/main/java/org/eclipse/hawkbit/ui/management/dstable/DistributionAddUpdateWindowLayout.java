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
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent.DistributionComponentEvent;
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
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
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

    private Button saveDistribution;
    private Button discardDistribution;
    private TextField distNameTextField;
    private TextField distVersionTextField;
    private Label madatoryLabel;
    private TextArea descTextArea;
    private CheckBox reqMigStepCheckbox;
    private ComboBox distsetTypeNameComboBox;
    private boolean editDistribution = Boolean.FALSE;
    private Long editDistId = null;
    private Window addDistributionWindow;
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

    /**
     * Initialize Distribution Add and Edit Window.
     */
    @PostConstruct
    void init() {
        createRequiredComponents();
        buildLayout();
    }

    private void buildLayout() {
        /* action button layout ( save & discard ) */
        final HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeFull();
        buttonsLayout.setStyleName("dist-buttons-horz-layout");
        buttonsLayout.addComponents(this.saveDistribution, this.discardDistribution);
        buttonsLayout.setComponentAlignment(this.saveDistribution, Alignment.BOTTOM_LEFT);
        buttonsLayout.setComponentAlignment(this.discardDistribution, Alignment.BOTTOM_RIGHT);
        buttonsLayout.addStyleName("window-style");

        /*
         * The main layout of the window contains mandatory info, textboxes
         * (controller Id, name & description) and action buttons layout
         */
        setSpacing(Boolean.TRUE);
        addStyleName("lay-color");
        setSizeUndefined();
        addComponents(this.madatoryLabel, this.distsetTypeNameComboBox, this.distNameTextField,
                this.distVersionTextField, this.descTextArea, this.reqMigStepCheckbox);

        addComponent(buttonsLayout);
        setComponentAlignment(this.madatoryLabel, Alignment.MIDDLE_LEFT);

    }

    /**
     * Create required UI components.
     */
    private void createRequiredComponents() {
        this.distNameTextField = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY, true, null,
                this.i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        this.distNameTextField.setId(SPUIComponetIdProvider.DIST_ADD_NAME);
        this.distNameTextField.setNullRepresentation("");

        this.distVersionTextField = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY, true, null,
                this.i18n.get("textfield.version"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        this.distVersionTextField.setId(SPUIComponetIdProvider.DIST_ADD_VERSION);
        this.distVersionTextField.setNullRepresentation("");

        this.distsetTypeNameComboBox = SPUIComponentProvider.getComboBox("", "", null, "", false, "",
                this.i18n.get("label.combobox.type"));
        this.distsetTypeNameComboBox.setImmediate(true);
        this.distsetTypeNameComboBox.setNullSelectionAllowed(false);
        this.distsetTypeNameComboBox.setId(SPUIComponetIdProvider.DIST_ADD_DISTSETTYPE);

        this.descTextArea = SPUIComponentProvider.getTextArea("text-area-style", ValoTheme.TEXTAREA_TINY, false, null,
                this.i18n.get("textfield.description"), SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        this.descTextArea.setId(SPUIComponetIdProvider.DIST_ADD_DESC);
        this.descTextArea.setNullRepresentation("");

        /* Label for mandatory symbol */
        this.madatoryLabel = new Label(this.i18n.get("label.mandatory.field"));
        this.madatoryLabel.setStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR + " " + ValoTheme.LABEL_SMALL);

        this.reqMigStepCheckbox = SPUIComponentProvider.getCheckBox(
                this.i18n.get("checkbox.dist.required.migration.step"), "dist-checkbox-style", null, false, "");
        this.reqMigStepCheckbox.addStyleName(ValoTheme.CHECKBOX_SMALL);
        this.reqMigStepCheckbox.setId(SPUIComponetIdProvider.DIST_ADD_MIGRATION_CHECK);

        /* save or update button */
        this.saveDistribution = SPUIComponentProvider.getButton(SPUIComponetIdProvider.DIST_ADD_SAVE, "", "", "", true,
                FontAwesome.SAVE, SPUIButtonStyleSmallNoBorder.class);
        this.saveDistribution.addClickListener(event -> saveDistribution());

        /* close button */
        this.discardDistribution = SPUIComponentProvider.getButton(SPUIComponetIdProvider.DIST_ADD_DISCARD, "", "", "",
                true, FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        this.discardDistribution.addClickListener(event -> discardDistribution());
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
        this.saveDistribution.setEnabled(true);
    }

    private DistributionSetType getDefaultDistributionSetType() {
        final TenantMetaData tenantMetaData = this.tenantMetaDataRepository
                .findByTenantIgnoreCase(this.systemManagement.currentTenant());
        return tenantMetaData.getDefaultDsType();
    }

    private void disableSaveButton() {
        this.saveDistribution.setEnabled(false);
    }

    private void saveDistribution() {
        /* add new or update target */
        if (this.editDistribution) {
            updateDistribution();
        } else {
            addNewDistribution();
        }

    }

    /**
     * Update Distribution.
     */
    private void updateDistribution() {
        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(this.distNameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(this.distVersionTextField.getValue());
        final String distSetTypeName = HawkbitCommonUtil
                .trimAndNullIfEmpty((String) this.distsetTypeNameComboBox.getValue());

        if (mandatoryCheck(name, version, distSetTypeName) && duplicateCheck(name, version)) {
            final DistributionSet currentDS = this.distributionSetManagement
                    .findDistributionSetByIdWithDetails(this.editDistId);
            final String desc = HawkbitCommonUtil.trimAndNullIfEmpty(this.descTextArea.getValue());
            final boolean isMigStepReq = this.reqMigStepCheckbox.getValue();

            /* identify the changes */
            setDistributionValues(currentDS, name, version, distSetTypeName, desc, isMigStepReq);
            try {
                this.distributionSetManagement.updateDistributionSet(currentDS);
                this.notificationMessage.displaySuccess(this.i18n.get("message.new.dist.save.success",
                        new Object[] { currentDS.getName(), currentDS.getVersion() }));
                // update table row+details layout
                this.eventBus.publish(this,
                        new DistributionTableEvent(DistributionComponentEvent.EDIT_DISTRIBUTION, currentDS));
            } catch (final EntityAlreadyExistsException entityAlreadyExistsException) {
                LOG.error("Update distribution failed {}", entityAlreadyExistsException);
                this.notificationMessage.displayValidationError(this.i18n.get("message.distribution.no.update",
                        currentDS.getName() + ":" + currentDS.getVersion()));
            }
            closeThisWindow();
        }
    }

    private void addListeners() {
        this.reqMigStepCheckboxListerner = event -> checkValueChanged(this.originalReqMigStep, event);
        this.descTextAreaListener = event -> checkValueChanged(this.originalDistDescription, event);
        this.distNameTextFieldListener = event -> checkValueChanged(this.originalDistName, event);
        this.distVersionTextFieldListener = event -> checkValueChanged(this.originalDistVersion, event);
        this.distsetTypeNameComboBoxListener = event -> checkValueChanged(this.originalDistSetType, event);
        this.reqMigStepCheckbox.addValueChangeListener(this.reqMigStepCheckboxListerner);
        this.descTextArea.addTextChangeListener(this.descTextAreaListener);
        this.distNameTextField.addTextChangeListener(this.distNameTextFieldListener);
        this.distVersionTextField.addTextChangeListener(this.distVersionTextFieldListener);
        this.distsetTypeNameComboBox.addValueChangeListener(this.distsetTypeNameComboBoxListener);
    }

    /**
     * Add new Distribution set.
     */
    private void addNewDistribution() {
        this.editDistribution = Boolean.FALSE;
        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(this.distNameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(this.distVersionTextField.getValue());
        final String distSetTypeName = HawkbitCommonUtil
                .trimAndNullIfEmpty((String) this.distsetTypeNameComboBox.getValue());

        if (mandatoryCheck(name, version, distSetTypeName) && duplicateCheck(name, version)) {
            final String desc = HawkbitCommonUtil.trimAndNullIfEmpty(this.descTextArea.getValue());
            final boolean isMigStepReq = this.reqMigStepCheckbox.getValue();
            DistributionSet newDist = new DistributionSet();

            setDistributionValues(newDist, name, version, distSetTypeName, desc, isMigStepReq);
            newDist = this.distributionSetManagement.createDistributionSet(newDist);

            this.notificationMessage.displaySuccess(this.i18n.get("message.new.dist.save.success",
                    new Object[] { newDist.getName(), newDist.getVersion() }));
            /* close the window */
            closeThisWindow();

            this.eventBus.publish(this,
                    new DistributionTableEvent(DistributionComponentEvent.ADD_DISTRIBUTION, newDist));
        }
    }

    /**
     * Close window.
     */
    private void closeThisWindow() {
        this.addDistributionWindow.close();
        UI.getCurrent().removeWindow(this.addDistributionWindow);
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
        distributionSet.setType(this.distributionSetManagement.findDistributionSetTypeByName(distSetTypeName));
        distributionSet.setDescription(desc != null ? desc : "");
        distributionSet.setRequiredMigrationStep(isMigStepReq);
    }

    /**
     * Duplicate check-Name and version for {@link DistributionSet} unique.
     *
     * @param name
     *            as String
     * @param version
     *            as String
     * @return
     */
    private boolean duplicateCheck(final String name, final String version) {
        final DistributionSet existingDs = this.distributionSetManagement.findDistributionSetByNameAndVersion(name,
                version);
        /*
         * Distribution should not exists with the same name & version. Display
         * error message, when the "existingDs" is not null and it is add window
         * (or) when the "existingDs" is not null and it is edit window and the
         * distribution Id of the edit window is different then the "existingDs"
         */
        if (existingDs != null
                && (!this.editDistribution || this.editDistribution && !existingDs.getId().equals(this.editDistId))) {
            this.distNameTextField.addStyleName("v-textfield-error");
            this.distVersionTextField.addStyleName("v-textfield-error");
            this.notificationMessage.displayValidationError(this.i18n.get("message.duplicate.dist",
                    new Object[] { existingDs.getName(), existingDs.getVersion() }));

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
                this.distNameTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
            }
            if (version == null) {
                this.distVersionTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
            }
            if (distSetTypeName == null) {
                this.distsetTypeNameComboBox.addStyleName(SPUIStyleDefinitions.SP_COMBOFIELD_ERROR);
            }

            this.notificationMessage.displayValidationError(this.i18n.get("message.mandatory.check"));
            return false;
        }

        return true;
    }

    private void discardDistribution() {
        /* Just close this window */
        this.distsetTypeNameComboBox.removeValueChangeListener(this.distsetTypeNameComboBoxListener);
        closeThisWindow();
    }

    /**
     * clear all the fields.
     */
    public void resetComponents() {
        this.editDistribution = Boolean.FALSE;
        this.distNameTextField.clear();
        this.distNameTextField.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        this.distVersionTextField.clear();
        this.distVersionTextField.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        this.distsetTypeNameComboBox.removeStyleName(SPUIStyleDefinitions.SP_COMBOFIELD_ERROR);
        this.descTextArea.clear();
        this.reqMigStepCheckbox.clear();
        this.saveDistribution.setEnabled(true);
        removeListeners();
        this.changedComponents.clear();
    }

    private void populateRequiredComponents() {
        populateDistSetTypeNameCombo();
    }

    private void removeListeners() {
        this.reqMigStepCheckbox.removeValueChangeListener(this.reqMigStepCheckboxListerner);
        this.descTextArea.removeTextChangeListener(this.descTextAreaListener);
        this.distNameTextField.removeTextChangeListener(this.distNameTextFieldListener);
        this.distVersionTextField.removeTextChangeListener(this.distVersionTextFieldListener);
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
        if (this.editDistribution) {
            final String newValue = event.getText();
            if (!originalValue.equalsIgnoreCase(newValue)) {
                this.changedComponents.add(event.getComponent());
            } else {
                this.changedComponents.remove(event.getComponent());
            }
            enableDisableSaveButton();
        }
    }

    private void checkValueChanged(final Boolean originalValue, final ValueChangeEvent event) {
        if (this.editDistribution) {
            if (!originalValue.equals(event.getProperty().getValue())) {
                this.changedComponents.add(this.reqMigStepCheckbox);
            } else {
                this.changedComponents.remove(this.reqMigStepCheckbox);
            }
            enableDisableSaveButton();
        }
    }

    private void checkValueChanged(final String originalValue, final ValueChangeEvent event) {
        if (this.editDistribution) {
            if (!originalValue.equals(event.getProperty().getValue())) {
                this.changedComponents.add(this.distsetTypeNameComboBox);
            } else {
                this.changedComponents.remove(this.distsetTypeNameComboBox);
            }
            enableDisableSaveButton();
        }
    }

    private void enableDisableSaveButton() {
        if (this.changedComponents.isEmpty()) {
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
        this.editDistribution = Boolean.TRUE;
        this.saveDistribution.setEnabled(false);
        final DistributionSet distSet = this.distributionSetManagement.findDistributionSetByIdWithDetails(editDistId);
        if (distSet != null) {
            this.distNameTextField.setValue(distSet.getName());
            this.distVersionTextField.setValue(distSet.getVersion());
            if (distSet.getType().isDeleted()) {
                this.distsetTypeNameComboBox.addItem(distSet.getType().getName());
            }
            this.distsetTypeNameComboBox.setValue(distSet.getType().getName());
            this.reqMigStepCheckbox.setValue(distSet.isRequiredMigrationStep());
            if (distSet.getDescription() != null) {
                this.descTextArea.setValue(distSet.getDescription());
            }
            setOriginalDistName(distSet.getName());
            setOriginalDistVersion(distSet.getVersion());
            setOriginalDistDescription(distSet.getDescription());
            setOriginalReqMigStep(distSet.isRequiredMigrationStep());
            setOriginalDistSetTYpe(distSet.getType().getName());
            addListeners();
        }
    }

    public Window getWindow() {
        this.eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        populateRequiredComponents();
        resetComponents();
        this.addDistributionWindow = SPUIComponentProvider.getWindow(this.i18n.get("caption.add.new.dist"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW);
        this.addDistributionWindow.setContent(this);
        return this.addDistributionWindow;
    }

    /**
     * Populate DistributionSet Type name combo.
     */
    public void populateDistSetTypeNameCombo() {
        this.distsetTypeNameComboBox.setContainerDataSource(getDistSetTypeLazyQueryContainer());
        this.distsetTypeNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        this.distsetTypeNameComboBox.setValue(getDefaultDistributionSetType().getName());
    }

    /**
     * @param originalDistSetTYpe
     *            the originalDistSetTYpe to set
     */
    public void setOriginalDistSetTYpe(final String originalDistSetType) {
        this.originalDistSetType = originalDistSetType;
    }
}

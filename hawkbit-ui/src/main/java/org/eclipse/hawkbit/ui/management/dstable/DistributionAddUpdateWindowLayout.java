/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIWindowDecorator;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
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

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * WindowContent for adding/editing a Distribution
 */
@SpringComponent
@ViewScope
public class DistributionAddUpdateWindowLayout extends CustomComponent {

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
    private transient EntityFactory entityFactory;

    private TextField distNameTextField;
    private TextField distVersionTextField;
    private TextArea descTextArea;
    private CheckBox reqMigStepCheckbox;
    private ComboBox distsetTypeNameComboBox;
    private boolean editDistribution = Boolean.FALSE;
    private Long editDistId;
    private CommonDialogWindow window;
    private String originalDistName;
    private String originalDistVersion;
    private String originalDistDescription;
    private Boolean originalReqMigStep;
    private String originalDistSetType;

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
        addStyleName("lay-color");
        setSizeUndefined();

        formLayout = new FormLayout();
        formLayout.addComponent(distsetTypeNameComboBox);
        formLayout.addComponent(distNameTextField);
        formLayout.addComponent(distVersionTextField);
        formLayout.addComponent(descTextArea);
        formLayout.addComponent(reqMigStepCheckbox);

        setCompositionRoot(formLayout);
        distNameTextField.focus();
    }

    /**
     * Create required UI components.
     */
    private void createRequiredComponents() {
        distNameTextField = SPUIComponentProvider.getTextField(i18n.get("textfield.name"), "", ValoTheme.TEXTFIELD_TINY,
                true, null, i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        distNameTextField.setId(SPUIComponentIdProvider.DIST_ADD_NAME);
        distNameTextField.setNullRepresentation("");

        distVersionTextField = SPUIComponentProvider.getTextField(i18n.get("textfield.version"), "",
                ValoTheme.TEXTFIELD_TINY, true, null, i18n.get("textfield.version"), true,
                SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        distVersionTextField.setId(SPUIComponentIdProvider.DIST_ADD_VERSION);
        distVersionTextField.setNullRepresentation("");

        distsetTypeNameComboBox = SPUIComponentProvider.getComboBox(i18n.get("label.combobox.type"), "", "", null, "",
                false, "", i18n.get("label.combobox.type"));
        distsetTypeNameComboBox.setImmediate(true);
        distsetTypeNameComboBox.setNullSelectionAllowed(false);
        distsetTypeNameComboBox.setId(SPUIComponentIdProvider.DIST_ADD_DISTSETTYPE);
        populateDistSetTypeNameCombo();

        descTextArea = SPUIComponentProvider.getTextArea(i18n.get("textfield.description"), "text-area-style",
                ValoTheme.TEXTAREA_TINY, false, null, i18n.get("textfield.description"),
                SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        descTextArea.setId(SPUIComponentIdProvider.DIST_ADD_DESC);
        descTextArea.setNullRepresentation("");

        reqMigStepCheckbox = SPUIComponentProvider.getCheckBox(i18n.get("checkbox.dist.required.migration.step"),
                "dist-checkbox-style", null, false, "");
        reqMigStepCheckbox.addStyleName(ValoTheme.CHECKBOX_SMALL);
        reqMigStepCheckbox.setId(SPUIComponentIdProvider.DIST_ADD_MIGRATION_CHECK);
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

    private DistributionSetType getDefaultDistributionSetType() {
        final TenantMetaData tenantMetaData = systemManagement.getTenantMetadata();
        return tenantMetaData.getDefaultDsType();
    }

    private void saveDistribution() {
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
            DistributionSet newDist = entityFactory.generateDistributionSet();

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
        window.close();
        UI.getCurrent().removeWindow(window);
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

        originalDistDescription = null;
        originalDistName = null;
        originalDistSetType = null;
        originalDistVersion = null;
        originalReqMigStep = Boolean.FALSE;

    }

    private void populateRequiredComponents() {
        populateDistSetTypeNameCombo();
    }

    /**
     * populate data.
     *
     * @param editDistId
     */
    public void populateValuesOfDistribution(final Long editDistId) {
        this.editDistId = editDistId;
        editDistribution = Boolean.TRUE;
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
            originalDistName = distSet.getName();
            originalDistVersion = distSet.getVersion();
            originalDistDescription = distSet.getDescription();
            originalReqMigStep = distSet.isRequiredMigrationStep();
            originalDistSetType = distSet.getType().getName();
        }
    }

    public CommonDialogWindow getWindow() {
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        populateRequiredComponents();
        resetComponents();
        window = SPUIWindowDecorator.getWindow(i18n.get("caption.add.new.dist"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW, this, event -> saveDistribution(), event -> discardDistribution(),
                null, formLayout, i18n);
        window.getButtonsLayout().removeStyleName("actionButtonsMargin");
        return window;
    }

    /**
     * Populate DistributionSet Type name combo.
     */
    public void populateDistSetTypeNameCombo() {
        distsetTypeNameComboBox.setContainerDataSource(getDistSetTypeLazyQueryContainer());
        distsetTypeNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        distsetTypeNameComboBox.setValue(getDefaultDistributionSetType().getName());
    }

}

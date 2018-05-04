/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.CheckBox;

/**
 * Abstract class for Distribution Set Type Layout which is used for updating
 * and deleting Distribution Set Types, includes the combobox for selecting the
 * type to modify
 *
 */
public abstract class AbstractDistributionSetTypeLayoutForModify extends AbstractDistributionSetTypeLayout {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetManagement distributionSetManagement;

    private IndexedContainer originalSelectedTableContainer;

    private final String selectedTypeName;

    public AbstractDistributionSetTypeLayoutForModify(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DistributionSetManagement distributionSetManagement, final String selectedTypeName) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, distributionSetTypeManagement,
                softwareModuleTypeManagement);
        this.distributionSetManagement = distributionSetManagement;
        this.selectedTypeName = selectedTypeName;
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTagDetails(selectedTypeName);
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        disableFields();
    }

    @Override
    protected void disableFields() {
        getTypeKey().setEnabled(false);
        getTagName().setEnabled(false);
    }

    @Override
    protected boolean isUpdateAction() {
        return true;
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     * 
     * @param targetTagSelected
     *            as the selected tag from combo
     */
    private void setTagDetails(final String distSetTypeSelected) {
        getTagName().setValue(distSetTypeSelected);
        getTwinTables().createSourceTableData();
        getTwinTables().getSelectedTable().getContainerDataSource().removeAllItems();
        final Optional<DistributionSetType> selectedDistSetType = getDistributionSetTypeManagement()
                .getByName(distSetTypeSelected);
        selectedDistSetType.ifPresent(selectedType -> {
            getTagDesc().setValue(selectedType.getDescription());
            getTypeKey().setValue(selectedType.getKey());
            if (distributionSetManagement.countByTypeId(selectedType.getId()) <= 0) {
                getTwinTables().getDistTypeSelectLayout().setEnabled(true);
                getTwinTables().getSelectedTable().setEnabled(true);
            } else {
                getUiNotification().displayValidationError(
                        selectedType.getName() + "  " + getI18n().getMessage("message.error.dist.set.type.update"));
                getTwinTables().getDistTypeSelectLayout().setEnabled(false);
                getTwinTables().getSelectedTable().setEnabled(false);
            }

            createOriginalSelectedTableContainer();
            selectedType.getOptionalModuleTypes().forEach(swModuleType -> addTargetTableForUpdate(swModuleType, false));
            selectedType.getMandatoryModuleTypes().forEach(swModuleType -> addTargetTableForUpdate(swModuleType, true));
            setColorPickerComponentsColor(selectedType.getColour());
        });
        disableFields();
        if (isUpdateAction()) {
            getWindow().setOrginaleValues();
        }
    }

    private void createOriginalSelectedTableContainer() {
        originalSelectedTableContainer = new IndexedContainer();
        originalSelectedTableContainer
                .addContainerProperty(DistributionTypeSoftwareModuleSelectLayout.getDistTypeName(), String.class, "");
        originalSelectedTableContainer.addContainerProperty(
                DistributionTypeSoftwareModuleSelectLayout.getDistTypeDescription(), String.class, "");
        originalSelectedTableContainer.addContainerProperty(
                DistributionTypeSoftwareModuleSelectLayout.getDistTypeMandatory(), CheckBox.class, null);
    }

    @SuppressWarnings("unchecked")
    private void addTargetTableForUpdate(final SoftwareModuleType swModuleType, final boolean mandatory) {
        if (getTwinTables().getSelectedTableContainer() == null) {
            return;
        }
        final Item saveTblitem = getTwinTables().getSelectedTableContainer().addItem(swModuleType.getId());
        getTwinTables().getSourceTable().removeItem(swModuleType.getId());
        saveTblitem.getItemProperty(DistributionTypeSoftwareModuleSelectLayout.getDistTypeName())
                .setValue(swModuleType.getName());
        final CheckBox mandatoryCheckbox = new CheckBox("", mandatory);
        mandatoryCheckbox.setId(swModuleType.getName());
        saveTblitem.getItemProperty(DistributionTypeSoftwareModuleSelectLayout.getDistTypeMandatory())
                .setValue(mandatoryCheckbox);

        final Item originalItem = originalSelectedTableContainer.addItem(swModuleType.getId());
        originalItem.getItemProperty(DistributionTypeSoftwareModuleSelectLayout.getDistTypeName())
                .setValue(swModuleType.getName());
        originalItem.getItemProperty(DistributionTypeSoftwareModuleSelectLayout.getDistTypeMandatory())
                .setValue(mandatoryCheckbox);

        getWindow().updateAllComponents(mandatoryCheckbox);
    }

    public DistributionSetManagement getDistributionSetManagement() {
        return distributionSetManagement;
    }

}

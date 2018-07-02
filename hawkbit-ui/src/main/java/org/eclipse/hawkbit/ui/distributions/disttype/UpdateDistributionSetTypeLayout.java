/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent.DistributionSetTypeEnum;
import org.eclipse.hawkbit.ui.layouts.UpdateTag;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Window.CloseListener;

/**
 * Layout for the pop-up window which is created when updating a Distribution
 * Set Type on the Distributions View.
 *
 */
public class UpdateDistributionSetTypeLayout extends AbstractDistributionSetTypeLayout implements UpdateTag {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetManagement distributionSetManagement;

    private final String selectedTypeName;

    private IndexedContainer originalSelectedTableContainer;

    private final CloseListener closeListener;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param distributionSetTypeManagement
     *            DistributionSetTypeManagement
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param selectedTypeName
     *            the name of the distribution set type to update
     * @param closeListener
     *            CloseListener
     */
    public UpdateDistributionSetTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DistributionSetManagement distributionSetManagement, final String selectedTypeName,
            final CloseListener closeListener) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, distributionSetTypeManagement,
                softwareModuleTypeManagement);
        this.distributionSetManagement = distributionSetManagement;
        this.selectedTypeName = selectedTypeName;
        this.closeListener = closeListener;
        initUpdatePopup();
    }

    private void initUpdatePopup() {
        setTagDetails(selectedTypeName);
        getWindow().addCloseListener(closeListener);
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.update", getI18n().getMessage("caption.type"));
    }

    @Override
    protected void saveEntity() {
        updateDistributionSetType(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, getTagName().getValue())));
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
    public void setTagDetails(final String selectedEntity) {
        getTwinTables().createSourceTableData();
        getTwinTables().getSelectedTable().getContainerDataSource().removeAllItems();
        final Optional<DistributionSetType> selectedDistSetType = getDistributionSetTypeManagement()
                .getByName(selectedEntity);
        selectedDistSetType.ifPresent(selectedType -> {
            getTagName().setValue(selectedType.getName());
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
        getWindow().setOrginaleValues();
    }

    private void createOriginalSelectedTableContainer() {
        originalSelectedTableContainer = new IndexedContainer();
        originalSelectedTableContainer.addContainerProperty(
                DistributionSetTypeSoftwareModuleSelectLayout.getDistTypeName(), String.class, "");
        originalSelectedTableContainer.addContainerProperty(
                DistributionSetTypeSoftwareModuleSelectLayout.getDistTypeDescription(), String.class, "");
        originalSelectedTableContainer.addContainerProperty(
                DistributionSetTypeSoftwareModuleSelectLayout.getDistTypeMandatory(), CheckBox.class, null);
    }

    @SuppressWarnings("unchecked")
    private void addTargetTableForUpdate(final SoftwareModuleType swModuleType, final boolean mandatory) {
        if (getTwinTables().getSelectedTableContainer() == null) {
            return;
        }
        final Item saveTblitem = getTwinTables().getSelectedTableContainer().addItem(swModuleType.getId());
        getTwinTables().getSourceTable().removeItem(swModuleType.getId());
        saveTblitem.getItemProperty(DistributionSetTypeSoftwareModuleSelectLayout.getDistTypeName())
                .setValue(swModuleType.getName());
        final CheckBox mandatoryCheckbox = new CheckBox("", mandatory);
        mandatoryCheckbox.setId(swModuleType.getName());
        saveTblitem.getItemProperty(DistributionSetTypeSoftwareModuleSelectLayout.getDistTypeMandatory())
                .setValue(mandatoryCheckbox);

        final Item originalItem = originalSelectedTableContainer.addItem(swModuleType.getId());
        originalItem.getItemProperty(DistributionSetTypeSoftwareModuleSelectLayout.getDistTypeName())
                .setValue(swModuleType.getName());
        originalItem.getItemProperty(DistributionSetTypeSoftwareModuleSelectLayout.getDistTypeMandatory())
                .setValue(mandatoryCheckbox);

        getWindow().updateAllComponents(mandatoryCheckbox);
    }

    @SuppressWarnings("unchecked")
    private void updateDistributionSetType(final DistributionSetType existingType) {
        final List<Long> itemIds = (List<Long>) getTwinTables().getSelectedTable().getItemIds();
        final DistributionSetTypeUpdate update = getEntityFactory().distributionSetType().update(existingType.getId())
                .description(getTagDesc().getValue())
                .colour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));
        if (distributionSetManagement.countByTypeId(existingType.getId()) <= 0 && !CollectionUtils.isEmpty(itemIds)) {
            update.mandatory(itemIds.stream()
                    .filter(itemId -> DistributionSetTypeSoftwareModuleSelectLayout
                            .isMandatoryModuleType(getTwinTables().getSelectedTable().getItem(itemId)))
                    .collect(Collectors.toList()))
                    .optional(itemIds.stream()
                            .filter(itemId -> DistributionSetTypeSoftwareModuleSelectLayout
                                    .isOptionalModuleType(getTwinTables().getSelectedTable().getItem(itemId)))
                            .collect(Collectors.toList()));
        }

        final DistributionSetType updateDistSetType = getDistributionSetTypeManagement().update(update);

        getUiNotification().displaySuccess(getI18n().getMessage("message.update.success", updateDistSetType.getName()));
        getEventBus().publish(this,
                new DistributionSetTypeEvent(DistributionSetTypeEnum.UPDATE_DIST_SET_TYPE, updateDistSetType));
    }

    @Override
    protected boolean isUpdateAction() {
        return true;
    }

}

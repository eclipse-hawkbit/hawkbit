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
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.components.UpdateComboBoxForTagsAndTypes;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent.DistributionSetTypeEnum;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.CheckBox;

public class UpdateDistributionSetTypeLayout extends AbstractDistributionSetTypeLayout {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetManagement distributionSetManagement;

    private IndexedContainer originalSelectedTableContainer;

    private UpdateComboBoxForTagsAndTypes updateCombobox;

    public UpdateDistributionSetTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DistributionSetManagement distributionSetManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, distributionSetTypeManagement,
                softwareModuleTypeManagement);
        this.distributionSetManagement = distributionSetManagement;
        init();
        populateTagNameCombo();
    }

    @Override
    protected void createRequiredComponents() {
        super.createRequiredComponents();
        updateCombobox = new UpdateComboBoxForTagsAndTypes(
                getI18n().getMessage("label.choose.type", getI18n().getMessage("label.choose.tag.update")),
                getI18n().getMessage("label.combobox.type"));
        // TODO MR Can tagNameChosen be moved to updateCombobox??
        updateCombobox.getTagNameComboBox().addValueChangeListener(this::tagNameChosen);
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        getFormLayout().addComponent(updateCombobox, 0);
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

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.configure", getI18n().getMessage("caption.update"),
                getI18n().getMessage("caption.type"));
    }

    @Override
    protected void saveEntity() {
        updateDistributionSetType(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, getTagName().getValue())));
    }

    private void populateTagNameCombo() {
        updateCombobox.getTagNameComboBox().setContainerDataSource(getDistSetTypeLazyQueryContainer());
        updateCombobox.getTagNameComboBox().setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
    }

    /**
     * Get the LazyQueryContainer instance for DistributionSetTypes.
     * 
     * @return
     */
    private static LazyQueryContainer getDistSetTypeLazyQueryContainer() {

        final LazyQueryContainer disttypeContainer = HawkbitCommonUtil
                .createLazyQueryContainer(new BeanQueryFactory<>(DistributionSetTypeBeanQuery.class));
        disttypeContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", true, true);

        return disttypeContainer;
    }

    private void tagNameChosen(final ValueChangeEvent event) {
        final String tagSelected = (String) event.getProperty().getValue();
        if (tagSelected != null) {
            setTagDetails(tagSelected);
        } else {
            resetFields();
        }
        if (isUpdateAction()) {
            getWindow().setOrginaleValues();
        }
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

    /**
     * update distributionSet Type.
     */
    @SuppressWarnings("unchecked")
    private void updateDistributionSetType(final DistributionSetType existingType) {
        final List<Long> itemIds = (List<Long>) getTwinTables().getSelectedTable().getItemIds();
        final DistributionSetTypeUpdate update = getEntityFactory().distributionSetType().update(existingType.getId())
                .description(getTagDesc().getValue())
                .colour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));
        if (distributionSetManagement.countByTypeId(existingType.getId()) <= 0 && !CollectionUtils.isEmpty(itemIds)) {
            update.mandatory(itemIds.stream()
                    .filter(itemId -> DistributionTypeSoftwareModuleSelectLayout
                            .isMandatoryModuleType(getTwinTables().getSelectedTable().getItem(itemId)))
                    .collect(Collectors.toList()))
                    .optional(itemIds.stream()
                            .filter(itemId -> DistributionTypeSoftwareModuleSelectLayout
                                    .isOptionalModuleType(getTwinTables().getSelectedTable().getItem(itemId)))
                            .collect(Collectors.toList()));
        }
        final DistributionSetType updateDistSetType = getDistributionSetTypeManagement().update(update);

        getUiNotification().displaySuccess(getI18n().getMessage("message.update.success", updateDistSetType.getName()));
        getEventBus().publish(this,
                new DistributionSetTypeEvent(DistributionSetTypeEnum.UPDATE_DIST_SET_TYPE, updateDistSetType));
    }

    public UpdateComboBoxForTagsAndTypes getUpdateCombobox() {
        return updateCombobox;
    }

}

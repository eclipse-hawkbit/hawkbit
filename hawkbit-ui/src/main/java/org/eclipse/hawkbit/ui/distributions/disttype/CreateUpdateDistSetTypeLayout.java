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
import java.util.Set;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent.DistributionSetTypeEnum;
import org.eclipse.hawkbit.ui.layouts.CreateUpdateTypeLayout;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.AbstractSelect.ItemDescriptionGenerator;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Window for create update Distribution Set Type.
 */
@SpringComponent
@ViewScope
public class CreateUpdateDistSetTypeLayout extends CreateUpdateTypeLayout<DistributionSetType> {

    private static final long serialVersionUID = -5169398523815877767L;
    private static final String DIST_TYPE_NAME = "name";
    private static final String DIST_TYPE_DESCRIPTION = "description";
    private static final String DIST_TYPE_MANDATORY = "mandatory";
    private static final String STAR = " * ";

    @Autowired
    private transient SoftwareManagement softwareManagement;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private transient EntityFactory entityFactory;

    private HorizontalLayout distTypeSelectLayout;
    private Table sourceTable;
    private Table selectedTable;

    private IndexedContainer selectedTableContainer;
    private IndexedContainer sourceTableContainer;

    private IndexedContainer originalSelectedTableContainer;

    @Override
    protected void createRequiredComponents() {

        super.createRequiredComponents();

        tagName = createTextField("textfield.name", SPUIDefinitions.DIST_SET_TYPE_NAME,
                SPUIDefinitions.NEW_DISTRIBUTION_TYPE_NAME);

        typeKey = createTextField("textfield.key", SPUIDefinitions.DIST_SET_TYPE_KEY,
                SPUIDefinitions.NEW_DISTRIBUTION_TYPE_KEY);

        tagDesc = new TextAreaBuilder().caption(i18n.get("textfield.description"))
                .styleName(ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.DIST_SET_TYPE_DESC)
                .prompt(i18n.get("textfield.description")).immediate(true)
                .id(SPUIDefinitions.NEW_DISTRIBUTION_TYPE_DESC).buildTextComponent();
        tagDesc.setNullRepresentation("");
    }

    private TextField createTextField(final String in18Key, final String styleName, final String id) {
        return new TextFieldBuilder().caption(i18n.get(in18Key)).styleName(ValoTheme.TEXTFIELD_TINY + " " + styleName)
                .required(true).prompt(i18n.get(in18Key)).immediate(true).id(id).buildTextComponent();
    }

    @Override
    protected void buildLayout() {

        super.buildLayout();
        ColorPickerHelper.setRgbSliderValues(colorPickerLayout);
        getFormLayout().addComponent(typeKey, 4);

        distTypeSelectLayout = createTwinColumnLayout();

        final VerticalLayout twinTableLayout = new VerticalLayout();
        twinTableLayout.setSizeFull();
        twinTableLayout.addComponent(distTypeSelectLayout);

        mainLayout.addComponent(twinTableLayout, 2, 0);
    }

    private HorizontalLayout createTwinColumnLayout() {

        final HorizontalLayout twinColumnLayout = new HorizontalLayout();
        twinColumnLayout.setSizeFull();
        twinColumnLayout.setWidth("400px");

        buildSourceTable();
        buildSelectedTable();

        final VerticalLayout selectButtonLayout = new VerticalLayout();
        final Button selectButton = SPUIComponentProvider.getButton("select-dist-type", "", "", "arrow-button", true,
                FontAwesome.FORWARD, SPUIButtonStyleSmallNoBorder.class);
        selectButton.addClickListener(event -> addSMType());
        final Button unSelectButton = SPUIComponentProvider.getButton("unselect-dist-type", "", "", "arrow-button",
                true, FontAwesome.BACKWARD, SPUIButtonStyleSmallNoBorder.class);
        unSelectButton.addClickListener(event -> removeSMType());
        selectButtonLayout.addComponent(selectButton);
        selectButtonLayout.addComponent(unSelectButton);
        selectButtonLayout.setComponentAlignment(selectButton, Alignment.MIDDLE_CENTER);
        selectButtonLayout.setComponentAlignment(unSelectButton, Alignment.MIDDLE_CENTER);

        twinColumnLayout.addComponent(sourceTable);
        twinColumnLayout.addComponent(selectButtonLayout);
        twinColumnLayout.addComponent(selectedTable);
        twinColumnLayout.setComponentAlignment(sourceTable, Alignment.MIDDLE_LEFT);
        twinColumnLayout.setComponentAlignment(selectButtonLayout, Alignment.MIDDLE_CENTER);
        twinColumnLayout.setComponentAlignment(selectedTable, Alignment.MIDDLE_RIGHT);
        twinColumnLayout.setExpandRatio(sourceTable, 0.45F);
        twinColumnLayout.setExpandRatio(selectButtonLayout, 0.07F);
        twinColumnLayout.setExpandRatio(selectedTable, 0.48F);
        sourceTable.setVisibleColumns(new Object[] { DIST_TYPE_NAME });
        return twinColumnLayout;
    }

    private void buildSelectedTable() {

        selectedTable = new Table();
        selectedTable.setId(SPUIDefinitions.TWIN_TABLE_SELECTED_ID);
        selectedTable.setSelectable(true);
        selectedTable.setMultiSelect(true);
        selectedTable.setSortEnabled(false);
        selectedTable.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        selectedTable.addStyleName(ValoTheme.TABLE_NO_STRIPES);
        selectedTable.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        selectedTable.addStyleName(ValoTheme.TABLE_SMALL);
        selectedTable.addStyleName("dist_type_twin-table");
        selectedTable.setSizeFull();
        createSelectedTableContainer();
        selectedTable.setContainerDataSource(selectedTableContainer);
        addTooltTipToSelectedTable();
        selectedTable.setImmediate(true);
        selectedTable.setVisibleColumns(DIST_TYPE_NAME, DIST_TYPE_MANDATORY);
        selectedTable.setColumnHeaders(i18n.get("header.dist.twintable.selected"), STAR);
        selectedTable.setColumnExpandRatio(DIST_TYPE_NAME, 0.75F);
        selectedTable.setColumnExpandRatio(DIST_TYPE_MANDATORY, 0.25F);
        selectedTable.setRequired(true);
    }

    private void addTooltTipToSelectedTable() {

        selectedTable.setItemDescriptionGenerator(new ItemDescriptionGenerator() {
            private static final long serialVersionUID = 99432397408575324L;

            @Override
            public String generateDescription(final Component source, final Object itemId, final Object propertyId) {
                final Item item = selectedTable.getItem(itemId);
                final String description = (String) (item.getItemProperty(DIST_TYPE_DESCRIPTION).getValue());
                if (DIST_TYPE_NAME.equals(propertyId) && HawkbitCommonUtil.trimAndNullIfEmpty(description) != null) {
                    return i18n.get("label.description") + description;
                } else if (DIST_TYPE_MANDATORY.equals(propertyId)) {
                    return i18n.get("tooltip.check.for.mandatory");
                }
                return null;
            }
        });
    }

    /**
    *
    */
    private void buildSourceTable() {

        sourceTable = new Table();
        sourceTable.setId(SPUIDefinitions.TWIN_TABLE_SOURCE_ID);
        sourceTable.setSelectable(true);
        sourceTable.setMultiSelect(true);
        sourceTable.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        sourceTable.addStyleName(ValoTheme.TABLE_NO_STRIPES);
        sourceTable.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        sourceTable.addStyleName(ValoTheme.TABLE_SMALL);
        sourceTable.setImmediate(true);
        sourceTable.setSizeFull();
        sourceTable.addStyleName("dist_type_twin-table");
        sourceTable.setSortEnabled(false);
        sourceTableContainer = new IndexedContainer();
        sourceTableContainer.addContainerProperty(DIST_TYPE_NAME, String.class, "");
        sourceTableContainer.addContainerProperty(DIST_TYPE_DESCRIPTION, String.class, "");
        sourceTable.setContainerDataSource(sourceTableContainer);

        sourceTable.setVisibleColumns(new Object[] { DIST_TYPE_NAME });
        sourceTable.setColumnHeaders(i18n.get("header.dist.twintable.available"));
        sourceTable.setColumnExpandRatio(DIST_TYPE_NAME, 1.0F);
        getSourceTableData();
        addTooltip();
        sourceTable.select(sourceTable.firstItemId());
    }

    private void createSelectedTableContainer() {

        selectedTableContainer = new IndexedContainer();
        selectedTableContainer.addContainerProperty(DIST_TYPE_NAME, String.class, "");
        selectedTableContainer.addContainerProperty(DIST_TYPE_DESCRIPTION, String.class, "");
        selectedTableContainer.addContainerProperty(DIST_TYPE_MANDATORY, CheckBox.class, null);
    }

    private void createOriginalSelectedTableContainer() {

        originalSelectedTableContainer = new IndexedContainer();
        originalSelectedTableContainer.addContainerProperty(DIST_TYPE_NAME, String.class, "");
        originalSelectedTableContainer.addContainerProperty(DIST_TYPE_DESCRIPTION, String.class, "");
        originalSelectedTableContainer.addContainerProperty(DIST_TYPE_MANDATORY, CheckBox.class, null);
    }

    @SuppressWarnings("unchecked")
    private void addSMType() {

        final Set<Long> selectedIds = (Set<Long>) sourceTable.getValue();
        if (selectedIds == null) {
            return;
        }
        for (final Long id : selectedIds) {
            addTargetTableData(id);
        }
    }

    private void removeSMType() {

        @SuppressWarnings("unchecked")
        final Set<Long> selectedIds = (Set<Long>) selectedTable.getValue();
        if (selectedIds == null) {
            return;
        }
        for (final Long id : selectedIds) {
            addSourceTableData(id);
            selectedTable.removeItem(id);
        }
    }

    @SuppressWarnings("unchecked")
    private void getSourceTableData() {

        sourceTableContainer.removeAllItems();
        final Iterable<SoftwareModuleType> moduleTypeBeans = softwareManagement
                .findSoftwareModuleTypesAll(new PageRequest(0, 1_000));
        Item saveTblitem;
        for (final SoftwareModuleType swTypeTag : moduleTypeBeans) {
            saveTblitem = sourceTableContainer.addItem(swTypeTag.getId());
            saveTblitem.getItemProperty(DIST_TYPE_NAME).setValue(swTypeTag.getName());
            saveTblitem.getItemProperty(DIST_TYPE_DESCRIPTION).setValue(swTypeTag.getDescription());
        }
    }

    private void addTooltip() {

        sourceTable.setItemDescriptionGenerator(new ItemDescriptionGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String generateDescription(final Component source, final Object itemId, final Object propertyId) {
                final Item item = sourceTable.getItem(itemId);
                final String description = (String) item.getItemProperty(DIST_TYPE_DESCRIPTION).getValue();
                if (DIST_TYPE_NAME.equals(propertyId) && HawkbitCommonUtil.trimAndNullIfEmpty(description) != null) {
                    return i18n.get("label.description") + description;
                }
                return null;
            }
        });
    }

    private void addTargetTableData(final Long selectedId) {

        getSelectedTableItemData(selectedId);
        sourceTable.removeItem(selectedId);
    }

    @SuppressWarnings("unchecked")
    private void getSelectedTableItemData(final Long id) {

        Item saveTblitem;
        if (selectedTableContainer != null) {
            saveTblitem = selectedTableContainer.addItem(id);
            saveTblitem.getItemProperty(DIST_TYPE_NAME).setValue(
                    sourceTable.getContainerDataSource().getItem(id).getItemProperty(DIST_TYPE_NAME).getValue());
            final CheckBox mandatoryCheckBox = new CheckBox();
            window.updateAllComponents(mandatoryCheckBox);
            saveTblitem.getItemProperty(DIST_TYPE_MANDATORY).setValue(mandatoryCheckBox);
            saveTblitem.getItemProperty(DIST_TYPE_DESCRIPTION).setValue(
                    sourceTable.getContainerDataSource().getItem(id).getItemProperty(DIST_TYPE_DESCRIPTION).getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void addSourceTableData(final Long selectedId) {

        if (sourceTableContainer != null) {
            Item saveTblitem;
            saveTblitem = sourceTableContainer.addItem(selectedId);
            saveTblitem.getItemProperty(DIST_TYPE_NAME).setValue(selectedTable.getContainerDataSource()
                    .getItem(selectedId).getItemProperty(DIST_TYPE_NAME).getValue());
            saveTblitem.getItemProperty(DIST_TYPE_DESCRIPTION).setValue(selectedTable.getContainerDataSource()
                    .getItem(selectedId).getItemProperty(DIST_TYPE_DESCRIPTION).getValue());
        }
    }

    /**
     * Create new DistSet Type tag.
     */
    @SuppressWarnings("unchecked")
    private void createNewDistributionSetType() {

        final String colorPicked = ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview());
        final String typeNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        final String typeKeyValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeKey.getValue());
        final String typeDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());
        final List<Long> itemIds = (List<Long>) selectedTable.getItemIds();
        if (null != typeNameValue && null != typeKeyValue && null != itemIds && !itemIds.isEmpty()) {
            DistributionSetType newDistType = entityFactory.generateDistributionSetType(typeKeyValue, typeNameValue,
                    typeDescValue);
            for (final Long id : itemIds) {
                final Item item = selectedTable.getItem(id);
                final String distTypeName = (String) item.getItemProperty(DIST_TYPE_NAME).getValue();
                final CheckBox mandatoryCheckBox = (CheckBox) item.getItemProperty(DIST_TYPE_MANDATORY).getValue();
                final Boolean isMandatory = mandatoryCheckBox.getValue();
                final SoftwareModuleType swModuleType = softwareManagement.findSoftwareModuleTypeByName(distTypeName);
                checkMandatoryAndAddMandatoryModuleType(newDistType, isMandatory, swModuleType);
            }
            newDistType.setDescription(typeDescValue);
            newDistType.setColour(colorPicked);
            newDistType = distributionSetManagement.createDistributionSetType(newDistType);
            uiNotification.displaySuccess(i18n.get("message.save.success", new Object[] { newDistType.getName() }));
            eventBus.publish(this,
                    new DistributionSetTypeEvent(DistributionSetTypeEnum.ADD_DIST_SET_TYPE, newDistType));
        } else {
            uiNotification.displayValidationError(i18n.get("message.error.missing.typenameorkey"));
        }
    }

    /**
     * update distributionSet Type.
     */
    @SuppressWarnings("unchecked")
    private void updateDistributionSetType(final DistributionSetType existingType) {

        final List<Long> itemIds = (List<Long>) selectedTable.getItemIds();
        final String typeNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        final String typeKeyValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeKey.getValue());
        final String typeDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());
        /* remove all SW Module Types before update SW Module Types */
        final DistributionSetType updateDistSetType = removeSWModuleTypesFromDistSetType(existingType.getName());

        if (null != typeNameValue) {
            updateDistSetType.setName(typeNameValue);
            updateDistSetType.setKey(typeKeyValue);
            updateDistSetType.setDescription(typeDescValue);

            if (distributionSetManagement.countDistributionSetsByType(existingType) <= 0 && null != itemIds
                    && !itemIds.isEmpty()) {
                for (final Long id : itemIds) {
                    final Item item = selectedTable.getItem(id);
                    final CheckBox mandatoryCheckBox = (CheckBox) item.getItemProperty(DIST_TYPE_MANDATORY).getValue();
                    final Boolean isMandatory = mandatoryCheckBox.getValue();
                    final String distTypeName = (String) item.getItemProperty(DIST_TYPE_NAME).getValue();
                    final SoftwareModuleType swModuleType = softwareManagement
                            .findSoftwareModuleTypeByName(distTypeName);
                    checkMandatoryAndAddMandatoryModuleType(updateDistSetType, isMandatory, swModuleType);
                }
            }
            updateDistSetType.setColour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));
            distributionSetManagement.updateDistributionSetType(updateDistSetType);
            uiNotification
                    .displaySuccess(i18n.get("message.update.success", new Object[] { updateDistSetType.getName() }));
            eventBus.publish(this,
                    new DistributionSetTypeEvent(DistributionSetTypeEnum.UPDATE_DIST_SET_TYPE, updateDistSetType));
        } else {
            uiNotification.displayValidationError(i18n.get("message.tag.update.mandatory"));
        }
    }

    private static void checkMandatoryAndAddMandatoryModuleType(final DistributionSetType updateDistSetType,
            final Boolean isMandatory, final SoftwareModuleType swModuleType) {
        if (isMandatory) {
            updateDistSetType.addMandatoryModuleType(swModuleType);
        } else {
            updateDistSetType.addOptionalModuleType(swModuleType);
        }
    }

    private DistributionSetType removeSWModuleTypesFromDistSetType(final String selectedDistSetType) {

        final DistributionSetType distSetType = fetchDistributionSetType(selectedDistSetType);
        if (!distSetType.getMandatoryModuleTypes().isEmpty()) {
            for (final SoftwareModuleType smType : distSetType.getMandatoryModuleTypes()) {
                distSetType.removeModuleType(smType.getId());
            }
        }
        if (!distSetType.getOptionalModuleTypes().isEmpty()) {
            for (final SoftwareModuleType smType : distSetType.getOptionalModuleTypes()) {
                distSetType.removeModuleType(smType.getId());
            }
        }
        return distSetType;
    }

    /**
     * reset the components.
     */
    @Override
    protected void reset() {

        super.reset();
        selectedTable.removeAllItems();
        getSourceTableData();
    }

    /**
     * Listener for option group - Create tag/Update.
     *
     * @param event
     *            ValueChangeEvent
     */
    @Override
    protected void optionValueChanged(final ValueChangeEvent event) {

        super.optionValueChanged(event);

        if (updateTagStr.equals(event.getProperty().getValue())) {
            selectedTable.getContainerDataSource().removeAllItems();
            getSourceTableData();
            distTypeSelectLayout.setEnabled(false);
        } else {
            distTypeSelectLayout.setEnabled(true);
            selectedTable.setEnabled(true);
            selectedTable.getContainerDataSource().removeAllItems();
            sourceTable.getContainerDataSource().removeAllItems();
            getSourceTableData();
        }
    }

    /**
     * Populate DistributionSet Type name combo.
     */
    @Override
    public void populateTagNameCombo() {

        tagNameComboBox.setContainerDataSource(getDistSetTypeLazyQueryContainer());
        tagNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
    }

    /**
     * Get the LazyQueryContainer instance for DistributionSetTypes.
     * 
     * @return
     */
    private static LazyQueryContainer getDistSetTypeLazyQueryContainer() {

        final LazyQueryContainer disttypeContainer = HawkbitCommonUtil.createLazyQueryContainer(
                new BeanQueryFactory<DistributionSetTypeBeanQuery>(DistributionSetTypeBeanQuery.class));
        disttypeContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", true, true);

        return disttypeContainer;
    }

    @Override
    protected void resetTagNameField() {

        super.resetTagNameField();
        typeKey.setEnabled(false);
        typeKey.clear();
        selectedTable.removeAllItems();
        getSourceTableData();
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     * 
     * @param targetTagSelected
     *            as the selected tag from combo
     */
    @Override
    protected void setTagDetails(final String distSetTypeSelected) {

        tagName.setValue(distSetTypeSelected);
        getSourceTableData();
        selectedTable.getContainerDataSource().removeAllItems();
        final DistributionSetType selectedTypeTag = fetchDistributionSetType(distSetTypeSelected);
        if (null != selectedTypeTag) {
            tagDesc.setValue(selectedTypeTag.getDescription());
            typeKey.setValue(selectedTypeTag.getKey());
            if (distributionSetManagement.countDistributionSetsByType(selectedTypeTag) <= 0) {
                distTypeSelectLayout.setEnabled(true);
                selectedTable.setEnabled(true);
            } else {
                uiNotification.displayValidationError(
                        selectedTypeTag.getName() + "  " + i18n.get("message.error.dist.set.type.update"));
                distTypeSelectLayout.setEnabled(false);
                selectedTable.setEnabled(false);
            }

            createOriginalSelectedTableContainer();
            for (final SoftwareModuleType swModuleType : selectedTypeTag.getOptionalModuleTypes()) {
                addTargetTableforUpdate(swModuleType, false);
            }

            for (final SoftwareModuleType swModuleType : selectedTypeTag.getMandatoryModuleTypes()) {
                addTargetTableforUpdate(swModuleType, true);
            }
            setColorPickerComponentsColor(selectedTypeTag.getColour());
        }
    }

    private DistributionSetType fetchDistributionSetType(final String distTypeName) {

        return distributionSetManagement.findDistributionSetTypeByName(distTypeName);
    }

    @SuppressWarnings("unchecked")
    private void addTargetTableforUpdate(final SoftwareModuleType swModuleType, final boolean mandatory) {

        if (selectedTableContainer == null) {
            return;
        }
        final Item saveTblitem = selectedTableContainer.addItem(swModuleType.getId());
        sourceTable.removeItem(swModuleType.getId());
        saveTblitem.getItemProperty(DIST_TYPE_NAME).setValue(swModuleType.getName());
        final CheckBox mandatoryCheckbox = new CheckBox("", mandatory);
        mandatoryCheckbox.setId(swModuleType.getName());
        saveTblitem.getItemProperty(DIST_TYPE_MANDATORY).setValue(mandatoryCheckbox);

        final Item originalItem = originalSelectedTableContainer.addItem(swModuleType.getId());
        originalItem.getItemProperty(DIST_TYPE_NAME).setValue(swModuleType.getName());
        originalItem.getItemProperty(DIST_TYPE_MANDATORY).setValue(mandatoryCheckbox);

        window.updateAllComponents(mandatoryCheckbox);
    }

    @Override
    protected void updateEntity(final DistributionSetType entity) {
        updateDistributionSetType(entity);

    }

    @Override
    protected void createEntity() {
        createNewDistributionSetType();

    }

    @Override
    protected DistributionSetType findEntityByKey() {
        return distributionSetManagement.findDistributionSetTypeByKey(typeKey.getValue());
    }

    @Override
    protected DistributionSetType findEntityByName() {
        return distributionSetManagement.findDistributionSetTypeByName(tagName.getValue());
    }

    @Override
    protected String getDuplicateKeyErrorMessage(final DistributionSetType existingType) {
        return i18n.get("message.type.key.duplicate.check", new Object[] { existingType.getKey() });
    }

    @Override
    protected String getWindowCaption() {
        return i18n.get("caption.add.type");
    }

    @Override
    protected void createOptionGroup(final boolean hasCreatePermission, final boolean hasUpdatePermission) {

        super.createOptionGroup(hasCreatePermission, hasUpdatePermission);
        optiongroup.setId(SPUIDefinitions.CREATE_OPTION_GROUP_DISTRIBUTION_SET_TYPE_ID);
    }

}

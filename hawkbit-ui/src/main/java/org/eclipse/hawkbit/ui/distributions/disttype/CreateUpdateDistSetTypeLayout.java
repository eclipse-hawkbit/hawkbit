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
import org.eclipse.hawkbit.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.colorPicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorPicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent.DistributionSetTypeEnum;
import org.eclipse.hawkbit.ui.layouts.CreateUpdateTypeLayout;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Window for create update Distribution Set Type.
 */
@SpringComponent
@ViewScope
public class CreateUpdateDistSetTypeLayout extends CreateUpdateTypeLayout
        implements ColorChangeListener, ColorSelector {

    private static final long serialVersionUID = -5169398523815877767L;
    private static final Logger LOG = LoggerFactory.getLogger(CreateUpdateDistSetTypeLayout.class);

    private static final String DIST_TYPE_NAME = "name";
    private static final String DIST_TYPE_DESCRIPTION = "description";
    private static final String DIST_TYPE_MANDATORY = "mandatory";
    private static final String STAR = " * ";

    @Autowired
    private transient SoftwareManagement softwareManagement;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private transient DistributionSetRepository distributionSetRepository;

    private HorizontalLayout distTypeSelectLayout;
    private Table sourceTable;
    private Table selectedTable;

    private IndexedContainer selectedTablecontainer;
    private IndexedContainer sourceTablecontainer;

    @Override
    protected void createRequiredComponents() {

        super.createRequiredComponents();

        tagName = SPUIComponentProvider.getTextField(i18n.get("textfield.name"), "",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.DIST_SET_TYPE_NAME, true, "",
                i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        tagName.setId(SPUIDefinitions.NEW_DISTRIBUTION_TYPE_NAME);

        typeKey = SPUIComponentProvider.getTextField(i18n.get("textfield.key"), "",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.DIST_SET_TYPE_KEY, true, "", i18n.get("textfield.key"),
                true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        typeKey.setId(SPUIDefinitions.NEW_DISTRIBUTION_TYPE_KEY);

        tagDesc = SPUIComponentProvider.getTextArea(i18n.get("textfield.description"), "",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.DIST_SET_TYPE_DESC, false, "",
                i18n.get("textfield.description"), SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);

        tagDesc.setId(SPUIDefinitions.NEW_DISTRIBUTION_TYPE_DESC);
        tagDesc.setImmediate(true);
        tagDesc.setNullRepresentation("");
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
        twinColumnLayout.setExpandRatio(sourceTable, 0.45f);
        twinColumnLayout.setExpandRatio(selectButtonLayout, 0.07f);
        twinColumnLayout.setExpandRatio(selectedTable, 0.48f);
        sourceTable.setVisibleColumns(new Object[] { DIST_TYPE_NAME });
        return twinColumnLayout;
    }

    /**
    *
    */
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
        selectedTable.setContainerDataSource(selectedTablecontainer);
        addTooltTipToSelectedTable();
        selectedTable.setImmediate(true);
        selectedTable.setVisibleColumns(DIST_TYPE_NAME, DIST_TYPE_MANDATORY);
        selectedTable.setColumnHeaders(i18n.get("header.dist.twintable.selected"), STAR);
        selectedTable.setColumnExpandRatio(DIST_TYPE_NAME, 0.75f);
        selectedTable.setColumnExpandRatio(DIST_TYPE_MANDATORY, 0.25f);
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
        // sourceTable
        sourceTable.setSizeFull();
        sourceTable.addStyleName("dist_type_twin-table");
        sourceTable.setSortEnabled(false);
        sourceTablecontainer = new IndexedContainer();
        sourceTablecontainer.addContainerProperty(DIST_TYPE_NAME, String.class, "");
        sourceTablecontainer.addContainerProperty(DIST_TYPE_DESCRIPTION, String.class, "");
        sourceTable.setContainerDataSource(sourceTablecontainer);

        sourceTable.setVisibleColumns(new Object[] { DIST_TYPE_NAME });
        sourceTable.setColumnHeaders(i18n.get("header.dist.twintable.available"));
        sourceTable.setColumnExpandRatio(DIST_TYPE_NAME, 1.0f);
        getSourceTableData();
        addTooltip();
        sourceTable.select(sourceTable.firstItemId());
    }

    private void createSelectedTableContainer() {

        selectedTablecontainer = new IndexedContainer();
        selectedTablecontainer.addContainerProperty(DIST_TYPE_NAME, String.class, "");
        selectedTablecontainer.addContainerProperty(DIST_TYPE_DESCRIPTION, String.class, "");
        selectedTablecontainer.addContainerProperty(DIST_TYPE_MANDATORY, CheckBox.class, null);
    }

    @SuppressWarnings("unchecked")
    private void addSMType() {

        final Set<Long> selectedIds = (Set<Long>) sourceTable.getValue();
        if (null != selectedIds && !selectedIds.isEmpty()) {
            for (final Long id : selectedIds) {
                addTargetTableData(id);
            }
        }
    }

    private void removeSMType() {

        @SuppressWarnings("unchecked")
        final Set<Long> selectedIds = (Set<Long>) selectedTable.getValue();
        if (null != selectedIds && !selectedIds.isEmpty()) {
            for (final Long id : selectedIds) {
                addSourceTableData(id);
                selectedTable.removeItem(id);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void getSourceTableData() {

        sourceTablecontainer.removeAllItems();
        final Iterable<SoftwareModuleType> moduleTypeBeans = softwareManagement
                .findSoftwareModuleTypesAll(new PageRequest(0, 1_000));
        Item saveTblitem;
        for (final SoftwareModuleType swTypeTag : moduleTypeBeans) {
            saveTblitem = sourceTablecontainer.addItem(swTypeTag.getId());
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
        if (null != selectedTablecontainer) {
            saveTblitem = selectedTablecontainer.addItem(id);
            saveTblitem.getItemProperty(DIST_TYPE_NAME).setValue(
                    sourceTable.getContainerDataSource().getItem(id).getItemProperty(DIST_TYPE_NAME).getValue());
            saveTblitem.getItemProperty(DIST_TYPE_MANDATORY).setValue(new CheckBox());
            saveTblitem.getItemProperty(DIST_TYPE_DESCRIPTION).setValue(
                    sourceTable.getContainerDataSource().getItem(id).getItemProperty(DIST_TYPE_DESCRIPTION).getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void addSourceTableData(final Long selectedId) {

        if (null != sourceTablecontainer) {
            Item saveTblitem;
            saveTblitem = sourceTablecontainer.addItem(selectedId);
            selectedTable.getContainerDataSource().getItem(selectedId).getItemProperty(DIST_TYPE_NAME);
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
    private void crateNewDistributionSetType() {

        final String colorPicked = ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview());
        final String typeNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        final String typeKeyValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeKey.getValue());
        final String typeDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());
        final List<Long> itemIds = (List<Long>) selectedTable.getItemIds();
        if (null != typeNameValue && null != typeKeyValue && null != itemIds && !itemIds.isEmpty()) {
            DistributionSetType newDistType = new DistributionSetType(typeKeyValue, typeNameValue, typeDescValue);
            for (final Long id : itemIds) {
                final Item item = selectedTable.getItem(id);
                final String distTypeName = (String) item.getItemProperty(DIST_TYPE_NAME).getValue();
                final CheckBox mandatoryCheckBox = (CheckBox) item.getItemProperty(DIST_TYPE_MANDATORY).getValue();
                final Boolean isMandatory = mandatoryCheckBox.getValue();
                final SoftwareModuleType swModuleType = softwareManagement.findSoftwareModuleTypeByName(distTypeName);
                if (isMandatory) {
                    newDistType.addMandatoryModuleType(swModuleType);

                } else {
                    newDistType.addOptionalModuleType(swModuleType);
                }
            }
            if (null != typeDescValue) {
                newDistType.setDescription(typeDescValue);
            }

            newDistType.setColour(colorPicked);
            newDistType = distributionSetManagement.createDistributionSetType(newDistType);
            uiNotification.displaySuccess(i18n.get("message.save.success", new Object[] { newDistType.getName() }));
            closeWindow();
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
            updateDistSetType.setDescription(null != typeDescValue ? typeDescValue : null);

            if (distributionSetRepository.countByType(existingType) <= 0 && null != itemIds && !itemIds.isEmpty()) {
                for (final Long id : itemIds) {
                    final Item item = selectedTable.getItem(id);
                    final CheckBox mandatoryCheckBox = (CheckBox) item.getItemProperty(DIST_TYPE_MANDATORY).getValue();
                    final Boolean isMandatory = mandatoryCheckBox.getValue();
                    final String distTypeName = (String) item.getItemProperty(DIST_TYPE_NAME).getValue();
                    final SoftwareModuleType swModuleType = softwareManagement
                            .findSoftwareModuleTypeByName(distTypeName);
                    if (isMandatory) {
                        updateDistSetType.addMandatoryModuleType(swModuleType);

                    } else {
                        updateDistSetType.addOptionalModuleType(swModuleType);
                    }
                }
            }
            updateDistSetType.setColour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));
            distributionSetManagement.updateDistributionSetType(updateDistSetType);
            uiNotification
                    .displaySuccess(i18n.get("message.update.success", new Object[] { updateDistSetType.getName() }));
            closeWindow();
            eventBus.publish(this,
                    new DistributionSetTypeEvent(DistributionSetTypeEnum.UPDATE_DIST_SET_TYPE, updateDistSetType));

        } else {
            uiNotification.displayValidationError(i18n.get("message.tag.update.mandatory"));
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

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.components.colorpicker.HasColorChangeListener#
     * addColorChangeListener(com.vaadin
     * .ui.components.colorpicker.ColorChangeListener)
     */
    @Override
    public void addColorChangeListener(final ColorChangeListener listener) {

        LOG.info("in side addColorChangeListener() ");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.components.colorpicker.HasColorChangeListener#
     * removeColorChangeListener(com.
     * vaadin.ui.components.colorpicker.ColorChangeListener)
     */
    @Override
    public void removeColorChangeListener(final ColorChangeListener listener) {

        LOG.info("in side removeColorChangeListener() ");
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
    protected void createOptionValueChanged(final ValueChangeEvent event) {

        super.createOptionValueChanged(event);

        if ("Update Type".equals(event.getProperty().getValue())) {
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
    private LazyQueryContainer getDistSetTypeLazyQueryContainer() {

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

            if (distributionSetRepository.countByType(selectedTypeTag) <= 0) {
                distTypeSelectLayout.setEnabled(true);
                selectedTable.setEnabled(true);
                window.setSaveButtonEnabled(true);
            } else {
                uiNotification.displayValidationError(
                        selectedTypeTag.getName() + "  " + i18n.get("message.error.dist.set.type.update"));
                distTypeSelectLayout.setEnabled(false);
                selectedTable.setEnabled(false);
                window.setSaveButtonEnabled(false);
            }
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

        Item saveTblitem;
        if (null != selectedTablecontainer) {
            saveTblitem = selectedTablecontainer.addItem(swModuleType.getId());
            sourceTable.removeItem(swModuleType.getId());
            saveTblitem.getItemProperty(DIST_TYPE_NAME).setValue(swModuleType.getName());
            saveTblitem.getItemProperty(DIST_TYPE_MANDATORY).setValue(new CheckBox("", mandatory));
        }
    }

    /**
     * reset the tag name and tag description component border color.
     */
    @Override
    protected void restoreComponentStyles() {

        super.restoreComponentStyles();
        typeKey.removeStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeKey.addStyleName(SPUIDefinitions.DIST_SET_TYPE_KEY);
    }

    @Override
    protected void save(final ClickEvent event) {

        if (mandatoryValuesPresent()) {
            final DistributionSetType existingDistTypeByKey = distributionSetManagement
                    .findDistributionSetTypeByKey(typeKey.getValue());
            final DistributionSetType existingDistTypeByName = distributionSetManagement
                    .findDistributionSetTypeByName(tagName.getValue());
            if (optiongroup.getValue().equals(createTypeStr)) {
                if (!checkIsDuplicateByKey(existingDistTypeByKey) && !checkIsDuplicate(existingDistTypeByName)) {
                    crateNewDistributionSetType();
                }
            } else {
                updateDistributionSetType(existingDistTypeByKey);
            }
        }
    }

    @Override
    public void createWindow() {
        reset();
        window = SPUIComponentProvider.getWindow(i18n.get("caption.add.type"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW, this, event -> save(event), event -> discard(event), null);
    }

    @Override
    protected void previewButtonClicked() {
        if (!tagPreviewBtnClicked) {
            final String selectedOption = (String) optiongroup.getValue();
            if (null != selectedOption && selectedOption.equalsIgnoreCase(updateTypeStr)
                    && null != tagNameComboBox.getValue()) {

                final DistributionSetType existedDistType = distributionSetManagement
                        .findDistributionSetTypeByKey(tagNameComboBox.getValue().toString());
                if (null != existedDistType) {
                    getColorPickerLayout().setSelectedColor(existedDistType.getColour() != null
                            ? ColorPickerHelper.rgbToColorConverter(existedDistType.getColour())
                            : ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
                } else {
                    getColorPickerLayout().setSelectedColor(
                            ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
                }
            }
            getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
            mainLayout.addComponent(colorPickerLayout, 1, 0);
            mainLayout.setComponentAlignment(colorPickerLayout, Alignment.MIDDLE_CENTER);
        }
        tagPreviewBtnClicked = !tagPreviewBtnClicked;
    }

}

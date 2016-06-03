/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.layouts.CreateUpdateTypeLayout;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class CreateUpdateSoftwareTypeLayout extends CreateUpdateTypeLayout
        implements ColorChangeListener, ColorSelector {

    private static final long serialVersionUID = -5169398523815919367L;
    private static final Logger LOG = LoggerFactory.getLogger(CreateUpdateSoftwareTypeLayout.class);

    @Autowired
    private transient SoftwareManagement swTypeManagementService;

    private String singleAssignStr;
    private String multiAssignStr;
    private Label singleAssign;
    private Label multiAssign;
    private OptionGroup assignOptiongroup;

    @Override
    protected void addListeners() {
        super.addListeners();
        optiongroup.addValueChangeListener(this::createOptionValueChanged);
    }

    @Override
    protected void createRequiredComponents() {

        super.createRequiredComponents();

        singleAssignStr = i18n.get("label.singleAssign.type");
        multiAssignStr = i18n.get("label.multiAssign.type");
        singleAssign = SPUIComponentProvider.getLabel(singleAssignStr, null);
        multiAssign = SPUIComponentProvider.getLabel(multiAssignStr, null);

        tagName = SPUIComponentProvider.getTextField(i18n.get("textfield.name"), "",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TYPE_NAME, true, "", i18n.get("textfield.name"), true,
                SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        tagName.setId(SPUIDefinitions.NEW_SOFTWARE_TYPE_NAME);

        typeKey = SPUIComponentProvider.getTextField(i18n.get("textfield.key"), "",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TYPE_KEY, true, "", i18n.get("textfield.key"), true,
                SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        typeKey.setId(SPUIDefinitions.NEW_SOFTWARE_TYPE_KEY);

        tagDesc = SPUIComponentProvider.getTextArea(i18n.get("textfield.description"), "",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TYPE_DESC, false, "",
                i18n.get("textfield.description"), SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);

        tagDesc.setId(SPUIDefinitions.NEW_SOFTWARE_TYPE_DESC);
        tagDesc.setImmediate(true);
        tagDesc.setNullRepresentation("");

        singleMultiOptionGroup();
    }

    @Override
    protected void buildLayout() {

        super.buildLayout();
        ColorPickerHelper.setRgbSliderValues(colorPickerLayout);
        getFormLayout().addComponent(typeKey, 4);
        getFormLayout().addComponent(assignOptiongroup);
    }

    @Override
    public void createWindow() {
        reset();
        window = SPUIComponentProvider.getWindow(i18n.get("caption.add.type"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW, this, this::save, this::discard, null);
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

        if (updateTypeStr.equals(event.getProperty().getValue())) {
            assignOptiongroup.setEnabled(false);
        } else {
            assignOptiongroup.setEnabled(true);
        }
    }

    /**
     * reset the components.
     */
    @Override
    protected void reset() {

        super.reset();
        assignOptiongroup.select(singleAssignStr);
    }

    @Override
    protected void resetTagNameField() {

        super.resetTagNameField();
        typeKey.clear();
        tagDesc.clear();
        assignOptiongroup.select(singleAssignStr);
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     * 
     * @param targetTagSelected
     *            as the selected tag from combo
     */
    @Override
    protected void setTagDetails(final String targetTagSelected) {
        tagName.setValue(targetTagSelected);
        final SoftwareModuleType selectedTypeTag = swTypeManagementService
                .findSoftwareModuleTypeByName(targetTagSelected);
        if (null != selectedTypeTag) {
            tagDesc.setValue(selectedTypeTag.getDescription());
            typeKey.setValue(selectedTypeTag.getKey());
            if (selectedTypeTag.getMaxAssignments() == Integer.MAX_VALUE) {
                assignOptiongroup.setValue(multiAssignStr);
            } else {
                assignOptiongroup.setValue(singleAssignStr);
            }

            setColorPickerComponentsColor(selectedTypeTag.getColour());
        }
    }

    private void singleMultiOptionGroup() {
        final List<String> optionValues = new ArrayList<>();
        optionValues.add(singleAssign.getValue());
        optionValues.add(multiAssign.getValue());
        assignOptionGroupByValues(optionValues);
    }

    private void assignOptionGroupByValues(final List<String> tagOptions) {
        assignOptiongroup = new OptionGroup("", tagOptions);
        assignOptiongroup.setStyleName(ValoTheme.OPTIONGROUP_SMALL);
        assignOptiongroup.addStyleName("custom-option-group");
        assignOptiongroup.setNullSelectionAllowed(false);
        assignOptiongroup.select(tagOptions.get(0));
    }

    @Override
    protected void save(final ClickEvent event) {
        if (mandatoryValuesPresent()) {
            final SoftwareModuleType existingSMTypeByKey = swTypeManagementService
                    .findSoftwareModuleTypeByKey(typeKey.getValue());
            final SoftwareModuleType existingSMTypeByName = swTypeManagementService
                    .findSoftwareModuleTypeByName(tagName.getValue());
            if (optiongroup.getValue().equals(createTypeStr)) {
                if (!checkIsDuplicateByKey(existingSMTypeByKey) && !checkIsDuplicate(existingSMTypeByName)) {
                    createNewSWModuleType();
                }
            } else {

                updateSWModuleType(existingSMTypeByName);
            }
        }
    }

    /**
     * Create new tag.
     */
    private void createNewSWModuleType() {
        int assignNumber = 0;
        final String colorPicked = ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview());
        final String typeNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        final String typeKeyValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeKey.getValue());
        final String typeDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());
        final String assignValue = (String) assignOptiongroup.getValue();
        if (null != assignValue && assignValue.equalsIgnoreCase(singleAssignStr)) {
            assignNumber = 1;
        } else if (null != assignValue && assignValue.equalsIgnoreCase(multiAssignStr)) {
            assignNumber = Integer.MAX_VALUE;
        }

        if (null != typeNameValue && null != typeKeyValue) {
            SoftwareModuleType newSWType = new SoftwareModuleType(typeKeyValue, typeNameValue, typeDescValue,
                    assignNumber, colorPicked);
            if (null != typeDescValue) {
                newSWType.setDescription(typeDescValue);
            }

            newSWType.setColour(colorPicked);

            newSWType = swTypeManagementService.createSoftwareModuleType(newSWType);
            uiNotification.displaySuccess(i18n.get("message.save.success", new Object[] { newSWType.getName() }));
            closeWindow();
            eventBus.publish(this,
                    new SoftwareModuleTypeEvent(SoftwareModuleTypeEnum.ADD_SOFTWARE_MODULE_TYPE, newSWType));

        } else {
            uiNotification.displayValidationError(i18n.get("message.error.missing.typenameorkey"));

        }
    }

    /**
     * update tag.
     */
    private void updateSWModuleType(final SoftwareModuleType existingType) {

        final String typeNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        final String typeDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());
        if (null != typeNameValue) {
            existingType.setName(typeNameValue);

            existingType.setDescription(null != typeDescValue ? typeDescValue : null);

            existingType.setColour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));
            swTypeManagementService.updateSoftwareModuleType(existingType);
            uiNotification.displaySuccess(i18n.get("message.update.success", new Object[] { existingType.getName() }));
            closeWindow();
            eventBus.publish(this,
                    new SoftwareModuleTypeEvent(SoftwareModuleTypeEnum.UPDATE_SOFTWARE_MODULE_TYPE, existingType));

        } else {
            uiNotification.displayValidationError(i18n.get("message.tag.update.mandatory"));
        }

    }

    /**
     * Open color picker on click of preview button. Auto select the color based
     * on target tag if already selected.
     */
    @Override
    protected void previewButtonClicked() {
        if (!tagPreviewBtnClicked) {
            final String selectedOption = (String) optiongroup.getValue();
            if (StringUtils.isNotEmpty(selectedOption) && selectedOption.equalsIgnoreCase(updateTypeStr)) {
                if (null != tagNameComboBox.getValue()) {
                    final SoftwareModuleType typeSelected = swTypeManagementService
                            .findSoftwareModuleTypeByName(tagNameComboBox.getValue().toString());
                    if (null != typeSelected) {
                        getColorPickerLayout().setSelectedColor(typeSelected.getColour() != null
                                ? ColorPickerHelper.rgbToColorConverter(typeSelected.getColour())
                                : ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
                    }
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

    /**
     * Covert RGB code to {@Color}.
     * 
     * @param value
     *            RGB vale
     * @return Color
     */
    protected Color rgbToColorConverter(final String value) {
        if (value.startsWith("rgb")) {
            final String[] colors = value.substring(value.indexOf('(') + 1, value.length() - 1).split(",");
            final int red = Integer.parseInt(colors[0]);
            final int green = Integer.parseInt(colors[1]);
            final int blue = Integer.parseInt(colors[2]);
            if (colors.length > 3) {
                final int alpha = (int) (Double.parseDouble(colors[3]) * 255d);
                return new Color(red, green, blue, alpha);
            } else {
                return new Color(red, green, blue);
            }
        }
        return null;
    }

    @Override
    public void addColorChangeListener(final ColorChangeListener listener) {
        LOG.debug("inside addColorChangeListener");
    }

    @Override
    public void removeColorChangeListener(final ColorChangeListener listener) {
        LOG.debug("inside removeColorChangeListener");

    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    protected void populateTagNameCombo() {
        tagNameComboBox.setContainerDataSource(HawkbitCommonUtil.createLazyQueryContainer(
                new BeanQueryFactory<SoftwareModuleTypeBeanQuery>(SoftwareModuleTypeBeanQuery.class)));
        tagNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
    }

}

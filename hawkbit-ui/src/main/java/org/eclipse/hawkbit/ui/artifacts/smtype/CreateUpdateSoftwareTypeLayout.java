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

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum;
import org.eclipse.hawkbit.ui.colorPicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorPicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.CoordinatesToColor;
import org.eclipse.hawkbit.ui.common.PopupWindowHelp;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.management.tag.CreateUpdateTagLayout;
import org.eclipse.hawkbit.ui.management.tag.SpColorPickerPreview;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;

import com.google.common.base.Strings;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.AbstractColorPicker.Coordinates2Color;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class CreateUpdateSoftwareTypeLayout extends CreateUpdateTagLayout
        implements ColorChangeListener, ColorSelector {

    private static final long serialVersionUID = -5169398523815919367L;
    private static final Logger LOG = LoggerFactory.getLogger(CreateUpdateSoftwareTypeLayout.class);

    @Autowired
    private transient SoftwareManagement swTypeManagementService;

    @Autowired
    private transient UiProperties uiProperties;

    private VerticalLayout sliderLayout;
    private HorizontalLayout colorLayout;
    private String createTypeStr;
    private String updateTypeStr;
    private String singleAssignStr;
    private String multiAssignStr;
    private Label createType;
    private Label updateType;
    private Label singleAssign;
    private Label multiAssign;
    private Label comboLabel;
    private TextField typeKey;
    private OptionGroup createOptiongroup;
    private OptionGroup assignOptiongroup;
    private static final String TYPE_NAME_DYNAMIC_STYLE = "new-tag-name";
    private static final String TYPE_DESC_DYNAMIC_STYLE = "new-tag-desc";
    private static final String TAG_DYNAMIC_STYLE = "tag-color-preview";

    /** RGB color converter. */
    private final Coordinates2Color rgbConverter = new CoordinatesToColor();

    private void createComponents() {
        createTypeStr = i18n.get("label.create.type");
        updateTypeStr = i18n.get("label.update.type");
        singleAssignStr = i18n.get("label.singleAssign.type");
        multiAssignStr = i18n.get("label.multiAssign.type");
        createType = SPUIComponentProvider.getLabel(createTypeStr, null);
        updateType = SPUIComponentProvider.getLabel(updateTypeStr, null);
        singleAssign = SPUIComponentProvider.getLabel(singleAssignStr, null);
        multiAssign = SPUIComponentProvider.getLabel(multiAssignStr, null);
        comboLabel = SPUIComponentProvider.getLabel(i18n.get("label.choose.type"), null);
        madatoryLabel = getMandatoryLabel();
        colorLabel = SPUIComponentProvider.getLabel(i18n.get("label.choose.type.color"), null);
        colorLabel.addStyleName(SPUIDefinitions.COLOR_LABEL_STYLE);

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

        tagNameComboBox = SPUIComponentProvider.getComboBox(i18n.get("label.combobox.type"), "", "", null, null, false,
                "", i18n.get("label.combobox.type"));
        tagNameComboBox.addStyleName(SPUIDefinitions.FILTER_TYPE_COMBO_STYLE);
        tagNameComboBox.setImmediate(true);

        tagColorPreviewBtn = new Button();
        tagColorPreviewBtn.setId(SPUIComponetIdProvider.TAG_COLOR_PREVIEW_ID);
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        tagColorPreviewBtn.setStyleName(TAG_DYNAMIC_STYLE);

        ColorPickerHelper.setRgbSliderValues(colorPickerLayout);

        createUpdateOptionGroup();

        singleMultiOptionGroup();
    }

    @Override
    protected void buildLayout() {

        super.buildLayout();
        getFormLayout().addComponent(typeKey, 4);
        getFormLayout().addComponent(assignOptiongroup, 5);

        final HorizontalLayout mainLayout = new HorizontalLayout();

        mainLayout.addComponent(getFormLayout());

        colorLayout = new HorizontalLayout();
        sliderLayout = new VerticalLayout();
        final HorizontalLayout chooseColorLayout = new HorizontalLayout();
        chooseColorLayout.addComponents(colorLabel, tagColorPreviewBtn);
        chooseColorLayout.setComponentAlignment(colorLabel, Alignment.TOP_CENTER);
        chooseColorLayout.setComponentAlignment(tagColorPreviewBtn, Alignment.TOP_CENTER);
        sliderLayout.addComponent(chooseColorLayout);
        colorLayout.addComponent(sliderLayout);

        final VerticalLayout mainWindowLayout = new VerticalLayout();
        mainWindowLayout.addComponent(new PopupWindowHelp(uiProperties.getLinks().getDocumentation().getRoot()));
        mainWindowLayout.addComponent(mainLayout);
        mainWindowLayout.addComponent(colorLayout);
    }

    @Override
    public void createWindow() {
        reset();
        window = SPUIComponentProvider.getWindow(i18n.get("caption.add.type"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW, this, event -> save(event), event -> discard(event));
    }

    /**
     * Listener for option group - Create tag/Update.
     * 
     * @param event
     *            ValueChangeEvent
     */
    private void createOptionValueChanged(final ValueChangeEvent event) {
        if ("Update Type".equals(event.getProperty().getValue())) {
            tagName.clear();
            tagDesc.clear();
            typeKey.clear();
            typeKey.setEnabled(false);
            tagName.setEnabled(false);
            assignOptiongroup.setEnabled(false);

            populateTagNameCombo();
            // show target name combo
            comboLayout.addComponent(comboLabel);
            comboLayout.addComponent(tagNameComboBox);
        } else {
            typeKey.setEnabled(true);
            tagName.setEnabled(true);
            tagName.clear();
            tagDesc.clear();
            typeKey.clear();
            assignOptiongroup.setEnabled(true);
            // hide target name combo
            comboLayout.removeComponent(comboLabel);
            comboLayout.removeComponent(tagNameComboBox);
        }
        // close the color picker layout
        tagPreviewBtnClicked = false;
        // reset the selected color - Set defualt color
        restoreComponentStyles();
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        getColorPickerLayout().getSelPreview()
                .setColor(ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
        // remove the sliders and color picker layout

        // TODO MR
        // fieldLayout.removeComponent(sliders);
        // mainLayout.removeComponent(colorPickerLayout);
    }

    /**
     * reset the components.
     */
    @Override
    protected void reset() {

        super.reset();
        typeKey.clear();
        assignOptiongroup.select(singleAssignStr);
    }

    private void typeNameChosen(final ValueChangeEvent event) {
        final String tagSelected = (String) event.getProperty().getValue();
        if (null != tagSelected) {
            setTypeTagCombo(tagSelected);
        } else {
            resetTagNameField();
        }
    }

    private void resetTagNameField() {
        tagName.setEnabled(false);
        tagName.clear();

        typeKey.clear();
        tagDesc.clear();
        restoreComponentStyles();

        // fieldLayout.removeComponent(sliders);
        // mainLayout.removeComponent(colorPickerLayout);

        assignOptiongroup.select(singleAssignStr);
        // Default green color

        // TODO extra method
        colorPickerLayout.setSelectedColor(colorPickerLayout.getDefaultColor());
        colorPickerLayout.getSelPreview().setColor(colorPickerLayout.getSelectedColor());
        tagPreviewBtnClicked = false;
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     * 
     * @param targetTagSelected
     *            as the selected tag from combo
     */
    private void setTypeTagCombo(final String targetTagSelected) {
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

            // TODO extra method
            if (null == selectedTypeTag.getColour()) {
                getColorPickerLayout()
                        .setSelectedColor(ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
                getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
                getColorPickerLayout().getColorSelect().setColor(getColorPickerLayout().getSelectedColor());
                createDynamicStyleForComponents(tagName, typeKey, tagDesc, ColorPickerConstants.DEFAULT_COLOR);
                getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
            } else {
                getColorPickerLayout()
                        .setSelectedColor(ColorPickerHelper.rgbToColorConverter(selectedTypeTag.getColour()));
                getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
                getColorPickerLayout().getColorSelect().setColor(getColorPickerLayout().getSelectedColor());
                createDynamicStyleForComponents(tagName, typeKey, tagDesc, selectedTypeTag.getColour());
                getPreviewButtonColor(selectedTypeTag.getColour());
            }
        }
    }

    private void createUpdateOptionGroup() {
        final List<String> optionValues = new ArrayList<>();
        if (permChecker.hasCreateDistributionPermission()) {
            optionValues.add(createType.getValue());
        }
        if (permChecker.hasUpdateDistributionPermission()) {
            optionValues.add(updateType.getValue());
        }
        createOptionGroupByValues(optionValues);
    }

    private void singleMultiOptionGroup() {
        final List<String> optionValues = new ArrayList<>();
        optionValues.add(singleAssign.getValue());
        optionValues.add(multiAssign.getValue());
        assignOptionGroupByValues(optionValues);
    }

    private void createOptionGroupByValues(final List<String> tagOptions) {
        createOptiongroup = new OptionGroup("", tagOptions);
        createOptiongroup.setCaption(null);
        createOptiongroup.setStyleName(ValoTheme.OPTIONGROUP_SMALL);
        createOptiongroup.addStyleName("custom-option-group");
        createOptiongroup.setNullSelectionAllowed(false);
        if (!tagOptions.isEmpty()) {
            createOptiongroup.select(tagOptions.get(0));
        }
    }

    private void assignOptionGroupByValues(final List<String> tagOptions) {
        assignOptiongroup = new OptionGroup("", tagOptions);
        assignOptiongroup.setStyleName(ValoTheme.OPTIONGROUP_SMALL);
        assignOptiongroup.addStyleName("custom-option-group");
        assignOptiongroup.setNullSelectionAllowed(false);
        assignOptiongroup.select(tagOptions.get(0));
    }

    /**
     * Value change listeners implementations of sliders.
     */
    private void slidersValueChangeListeners() {
        redSlider.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = -8336732888800920839L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double red = (Double) event.getProperty().getValue();
                final Color newColor = new Color((int) red, selectedColor.getGreen(), selectedColor.getBlue());
                setColorToComponents(newColor);
            }
        });
        greenSlider.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1236358037766775663L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double green = (Double) event.getProperty().getValue();
                final Color newColor = new Color(selectedColor.getRed(), (int) green, selectedColor.getBlue());
                setColorToComponents(newColor);
            }
        });
        blueSlider.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 8466370763686043947L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double blue = (Double) event.getProperty().getValue();
                final Color newColor = new Color(selectedColor.getRed(), selectedColor.getGreen(), (int) blue);
                setColorToComponents(newColor);
            }
        });
    }

    // TODO extra method
    private void setColorToComponents(final Color newColor) {
        setColor(newColor);
        getColorPickerLayout().getColorSelect().setColor(newColor);
        getPreviewButtonColor(newColor.getCSS());
        createDynamicStyleForComponents(tagName, typeKey, tagDesc, newColor.getCSS());
    }

    /**
     * reset the tag name and tag description component border color.
     */
    private void restoreComponentStyles() {
        tagName.removeStyleName(TYPE_NAME_DYNAMIC_STYLE);
        tagDesc.removeStyleName(TYPE_DESC_DYNAMIC_STYLE);
        typeKey.removeStyleName(TYPE_NAME_DYNAMIC_STYLE);
        getPreviewButtonColor(DEFAULT_COLOR);
    }

    private void save() {
        if (mandatoryValuesPresent()) {
            final SoftwareModuleType existingType = swTypeManagementService
                    .findSoftwareModuleTypeByName(tagName.getValue());
            if (createOptiongroup.getValue().equals(createTypeStr)) {
                if (!checkIsKeyDuplicate(typeKey.getValue()) && !checkIsDuplicate(existingType)) {
                    createNewSWModuleType();
                }
            } else {

                updateSWModuleType(existingType);
            }
        }
    }

    private boolean checkIsKeyDuplicate(final String key) {
        final SoftwareModuleType existingKeyType = swTypeManagementService.findSoftwareModuleTypeByKey(key);
        if (existingKeyType != null) {
            uiNotification.displayValidationError(
                    i18n.get("message.type.key.swmodule.duplicate.check", new Object[] { existingKeyType.getKey() }));
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    private Boolean mandatoryValuesPresent() {
        if (Strings.isNullOrEmpty(tagName.getValue()) && Strings.isNullOrEmpty(typeKey.getValue())) {
            if (createOptiongroup.getValue().equals(createTypeStr)) {
                uiNotification.displayValidationError(SPUILabelDefinitions.MISSING_TYPE_NAME_KEY);
            }
            if (createOptiongroup.getValue().equals(updateTypeStr)) {
                if (null == tagNameComboBox.getValue()) {
                    uiNotification.displayValidationError(i18n.get("message.error.missing.tagName"));
                } else {
                    uiNotification.displayValidationError(SPUILabelDefinitions.MISSING_TAG_NAME);
                }
            }
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private Boolean checkIsDuplicate(final SoftwareModuleType existingType) {
        if (existingType != null) {
            uiNotification.displayValidationError(
                    i18n.get("message.tag.duplicate.check", new Object[] { existingType.getName() }));
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Create new tag.
     */
    private void createNewSWModuleType() {
        int assignNumber = 0;
        final String colorPicked = getColorPickedString();
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
     * Get color picked value in string.
     *
     * @return String of color picked value.
     */
    private String getColorPickedString() {
        return "rgb(" + getSelPreview().getColor().getRed() + "," + getSelPreview().getColor().getGreen() + ","
                + getSelPreview().getColor().getBlue() + ")";
    }

    /**
     * Color view.
     * 
     * @return ColorPickerPreview as UI
     */
    public SpColorPickerPreview getSelPreview() {
        return selPreview;
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

            existingType.setColour(getColorPickedString());
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
    private void previewButtonClicked() {
        if (!tagPreviewBtnClicked) {
            final String selectedOption = (String) createOptiongroup.getValue();
            if (null != selectedOption && selectedOption.equalsIgnoreCase(updateTypeStr)) {
                if (null != tagNameComboBox.getValue()) {

                    final SoftwareModuleType typeSelected = swTypeManagementService
                            .findSoftwareModuleTypeByName(tagNameComboBox.getValue().toString());
                    if (null != typeSelected) {
                        selectedColor = typeSelected.getColour() != null ? rgbToColorConverter(typeSelected.getColour())
                                : rgbToColorConverter(DEFAULT_COLOR);

                    }

                } else {
                    selectedColor = rgbToColorConverter(DEFAULT_COLOR);
                }
            }
            selPreview.setColor(selectedColor);
            fieldLayout.addComponent(sliders);
            mainLayout.addComponent(colorPickerLayout);
            mainLayout.setComponentAlignment(colorPickerLayout, Alignment.BOTTOM_CENTER);
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
    public void setColor(final Color color) {
        if (color == null) {
            return;
        }
        selectedColor = color;
        selPreview.setColor(selectedColor);
        final String colorPickedPreview = selPreview.getColor().getCSS();
        if (tagName.isEnabled() && null != colorSelect) {
            createDynamicStyleForComponents(tagName, typeKey, tagDesc, colorPickedPreview);
            colorSelect.setColor(selPreview.getColor());
        }

    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public void colorChanged(final ColorChangeEvent event) {
        setColor(event.getColor());
        for (final ColorSelector select : selectors) {
            if (!event.getSource().equals(select) && select.equals(this) && !select.getColor().equals(selectedColor)) {
                select.setColor(selectedColor);
            }
        }
        setRgbSliderValues(selectedColor);
        getPreviewButtonColor(event.getColor().getCSS());
        createDynamicStyleForComponents(tagName, typeKey, tagDesc, event.getColor().getCSS());
    }

    /**
     * Set tag name and desc field border color based on chosen color.
     * 
     * @param tagName
     * @param tagDesc
     * @param taregtTagColor
     */
    private void createDynamicStyleForComponents(final TextField tagName, final TextField typeKey,
            final TextArea tagDesc, final String typeTagColor) {
        getTargetDynamicStyles(typeTagColor);
        tagName.addStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeKey.addStyleName(TYPE_NAME_DYNAMIC_STYLE);
        tagDesc.addStyleName(TYPE_DESC_DYNAMIC_STYLE);
    }

    /**
     * Get target style - Dynamically as per the color picked, cannot be done
     * from the static css.
     * 
     * @param colorPickedPreview
     */
    private void getTargetDynamicStyles(final String colorPickedPreview) {
        Page.getCurrent().getJavaScript()
                .execute(HawkbitCommonUtil.changeToNewSelectedPreviewColor(colorPickedPreview));
    }

    @Override
    protected void save(final ClickEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void populateTagNameCombo() {
        tagNameComboBox.setContainerDataSource(HawkbitCommonUtil.createLazyQueryContainer(
                new BeanQueryFactory<SoftwareModuleTypeBeanQuery>(SoftwareModuleTypeBeanQuery.class)));
        tagNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
    }

    @Override
    protected void setTagDetails(final String tagSelected) {
        // TODO Auto-generated method stub

    }

}

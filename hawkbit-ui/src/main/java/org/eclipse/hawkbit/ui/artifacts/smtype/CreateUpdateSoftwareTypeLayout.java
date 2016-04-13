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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum;
import org.eclipse.hawkbit.ui.common.CoordinatesToColor;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.management.tag.SpColorPickerPreview;
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
import org.vaadin.spring.events.EventBus;

import com.google.common.base.Strings;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.AbstractColorPicker.Coordinates2Color;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;
import com.vaadin.ui.components.colorpicker.ColorPickerGradient;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class CreateUpdateSoftwareTypeLayout extends CustomComponent implements ColorChangeListener, ColorSelector {

    private static final long serialVersionUID = -5169398523815919367L;
    private static final Logger LOG = LoggerFactory.getLogger(CreateUpdateSoftwareTypeLayout.class);

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private I18N i18n;

    /**
     * Instance of ColorPickerPreview.
     */
    private SpColorPickerPreview selPreview;

    @Autowired
    private transient UINotification uiNotification;

    @Autowired
    private transient SoftwareManagement swTypeManagementService;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    private String createTypeStr;
    private String updateTypeStr;
    private String singleAssignStr;
    private String multiAssignStr;
    private Label createType;
    private Label updateType;
    private Label singleAssign;
    private Label multiAssign;
    private Label comboLabel;
    private Label colorLabel;
    private Label madatoryLabel;
    private TextField typeName;
    private TextField typeKey;
    private TextArea typeDesc;
    private Button saveTag;
    private Button discardTag;
    private Button tagColorPreviewBtn;
    private OptionGroup createOptiongroup;
    private OptionGroup assignOptiongroup;
    private ComboBox typeNameComboBox;
    protected static final String DEFAULT_COLOR = "rgb(44,151,32)";
    private static final String TYPE_NAME_DYNAMIC_STYLE = "new-tag-name";
    private static final String TYPE_DESC_DYNAMIC_STYLE = "new-tag-desc";
    private static final String TAG_DYNAMIC_STYLE = "tag-color-preview";
    private Set<ColorSelector> selectors;
    private Color selectedColor;
    private ColorPickerGradient colorSelect;
    private Slider redSlider;
    private Slider greenSlider;
    private Slider blueSlider;
    private Window swTypeWindow;
    protected boolean tagPreviewBtnClicked = false;
    private VerticalLayout comboLayout;
    private VerticalLayout sliders;
    private VerticalLayout colorPickerLayout;
    private HorizontalLayout mainLayout;
    private VerticalLayout fieldLayout;

    /** RGB color converter. */
    private final Coordinates2Color rgbConverter = new CoordinatesToColor();

    /**
     * Initialize the artifact details layout.
     */
    public void init() {
        createComponents();
        buildLayout();
        addListeners();
    }

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

        typeName = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TYPE_NAME,
                true, "", i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        typeName.setId(SPUIDefinitions.NEW_SOFTWARE_TYPE_NAME);

        typeKey = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TYPE_KEY,
                true, "", i18n.get("textfield.key"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        typeKey.setId(SPUIDefinitions.NEW_SOFTWARE_TYPE_KEY);

        typeDesc = SPUIComponentProvider.getTextArea("", ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TYPE_DESC,
                false, "", i18n.get("textfield.description"), SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);

        typeDesc.setId(SPUIDefinitions.NEW_SOFTWARE_TYPE_DESC);
        typeDesc.setImmediate(true);
        typeDesc.setNullRepresentation("");

        typeNameComboBox = SPUIComponentProvider.getComboBox("", "", null, null, false, "",
                i18n.get("label.combobox.type"));
        typeNameComboBox.addStyleName(SPUIDefinitions.FILTER_TYPE_COMBO_STYLE);
        typeNameComboBox.setImmediate(true);

        saveTag = SPUIComponentProvider.getButton(SPUIDefinitions.NEW_SW_TYPE_SAVE, "", "", "", true, FontAwesome.SAVE,
                SPUIButtonStyleSmallNoBorder.class);
        saveTag.addStyleName(ValoTheme.BUTTON_BORDERLESS);

        discardTag = SPUIComponentProvider.getButton(SPUIDefinitions.NEW_TARGET_TAG_DISRACD, "", "",
                "discard-button-style", true, FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        discardTag.addStyleName(ValoTheme.BUTTON_BORDERLESS);

        tagColorPreviewBtn = new Button();
        tagColorPreviewBtn.setId(SPUIComponetIdProvider.TAG_COLOR_PREVIEW_ID);
        getPreviewButtonColor(DEFAULT_COLOR);
        tagColorPreviewBtn.setStyleName(TAG_DYNAMIC_STYLE);

        selectors = new HashSet<>();
        selectedColor = new Color(44, 151, 32);
        selPreview = new SpColorPickerPreview(selectedColor);

        colorSelect = new ColorPickerGradient("rgb-gradient", rgbConverter);
        colorSelect.setColor(selectedColor);
        colorSelect.setWidth("220px");

        redSlider = createRGBSlider("", "red");
        greenSlider = createRGBSlider("", "green");
        blueSlider = createRGBSlider("", "blue");
        setRgbSliderValues(selectedColor);

        createUpdateOptionGroup();

        singleMultiOptionGroup();

    }

    private void buildLayout() {
        comboLayout = new VerticalLayout();

        sliders = new VerticalLayout();
        sliders.addComponents(redSlider, greenSlider, blueSlider);

        selectors.add(colorSelect);

        colorPickerLayout = new VerticalLayout();
        colorPickerLayout.setStyleName("rgb-vertical-layout");
        colorPickerLayout.addComponent(selPreview);
        colorPickerLayout.addComponent(colorSelect);

        fieldLayout = new VerticalLayout();
        fieldLayout.setSpacing(true);
        fieldLayout.setMargin(false);
        fieldLayout.setWidth("100%");
        fieldLayout.setHeight(null);
        fieldLayout.addComponent(createOptiongroup);
        fieldLayout.addComponent(comboLayout);
        fieldLayout.addComponent(madatoryLabel);
        fieldLayout.addComponent(typeName);
        fieldLayout.addComponent(typeKey);
        fieldLayout.addComponent(typeDesc);
        fieldLayout.addComponent(assignOptiongroup);

        final HorizontalLayout colorLabelLayout = new HorizontalLayout();
        colorLabelLayout.addComponents(colorLabel, tagColorPreviewBtn);
        fieldLayout.addComponent(colorLabelLayout);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.addComponent(saveTag);
        buttonLayout.addComponent(discardTag);
        buttonLayout.setComponentAlignment(discardTag, Alignment.BOTTOM_RIGHT);
        buttonLayout.setComponentAlignment(saveTag, Alignment.BOTTOM_LEFT);
        buttonLayout.addStyleName("window-style");
        buttonLayout.setWidth("152px");

        final VerticalLayout fieldButtonLayout = new VerticalLayout();
        fieldButtonLayout.addComponent(fieldLayout);
        fieldButtonLayout.addComponent(buttonLayout);

        mainLayout = new HorizontalLayout();
        mainLayout.addComponent(fieldButtonLayout);
        setCompositionRoot(mainLayout);
    }

    private void addListeners() {
        saveTag.addClickListener(event -> save());
        discardTag.addClickListener(event -> discard());
        colorSelect.addColorChangeListener(this);
        selPreview.addColorChangeListener(this);
        tagColorPreviewBtn.addClickListener(event -> previewButtonClicked());
        createOptiongroup.addValueChangeListener(event -> createOptionValueChanged(event));
        typeNameComboBox.addValueChangeListener(event -> typeNameChosen(event));
        slidersValueChangeListeners();
    }

    public Window getWindow() {
        reset();
        swTypeWindow = SPUIComponentProvider.getWindow(i18n.get("caption.add.type"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW);
        swTypeWindow.setContent(this);
        return swTypeWindow;

    }

    /**
     * Listener for option group - Create tag/Update.
     * 
     * @param event
     *            ValueChangeEvent
     */
    private void createOptionValueChanged(final ValueChangeEvent event) {
        if ("Update Type".equals(event.getProperty().getValue())) {
            typeName.clear();
            typeDesc.clear();
            typeKey.clear();
            typeKey.setEnabled(false);
            typeName.setEnabled(false);
            assignOptiongroup.setEnabled(false);

            populateTypeNameCombo();
            // show target name combo
            comboLayout.addComponent(comboLabel);
            comboLayout.addComponent(typeNameComboBox);
        } else {
            typeKey.setEnabled(true);
            typeName.setEnabled(true);
            typeName.clear();
            typeDesc.clear();
            typeKey.clear();
            assignOptiongroup.setEnabled(true);
            // hide target name combo
            comboLayout.removeComponent(comboLabel);
            comboLayout.removeComponent(typeNameComboBox);
        }
        // close the color picker layout
        tagPreviewBtnClicked = false;
        // reset the selected color - Set defualt color
        restoreComponentStyles();
        getPreviewButtonColor(DEFAULT_COLOR);
        selPreview.setColor(rgbToColorConverter(DEFAULT_COLOR));
        // remove the sliders and color picker layout
        fieldLayout.removeComponent(sliders);
        mainLayout.removeComponent(colorPickerLayout);

    }

    /**
     * Populate Software Module Type name combo.
     */
    public void populateTypeNameCombo() {
        typeNameComboBox.setContainerDataSource(HawkbitCommonUtil.createLazyQueryContainer(
                new BeanQueryFactory<SoftwareModuleTypeBeanQuery>(SoftwareModuleTypeBeanQuery.class)));
        typeNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
    }

    /**
     * reset the components.
     */
    private void reset() {
        typeName.setEnabled(true);
        typeName.clear();
        typeKey.clear();
        typeDesc.clear();
        restoreComponentStyles();

        // hide target name combo
        comboLayout.removeComponent(comboLabel);
        comboLayout.removeComponent(typeNameComboBox);
        fieldLayout.removeComponent(sliders);
        mainLayout.removeComponent(colorPickerLayout);

        createOptiongroup.select(createTypeStr);
        assignOptiongroup.select(singleAssignStr);
        // Default green color
        selectedColor = new Color(44, 151, 32);
        selPreview.setColor(selectedColor);
        tagPreviewBtnClicked = false;
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
        typeName.setEnabled(false);
        typeName.clear();

        typeKey.clear();
        typeDesc.clear();
        restoreComponentStyles();
        fieldLayout.removeComponent(sliders);
        mainLayout.removeComponent(colorPickerLayout);

        assignOptiongroup.select(singleAssignStr);
        // Default green color
        selectedColor = new Color(44, 151, 32);
        selPreview.setColor(selectedColor);
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
        typeName.setValue(targetTagSelected);
        final SoftwareModuleType selectedTypeTag = swTypeManagementService
                .findSoftwareModuleTypeByName(targetTagSelected);
        if (null != selectedTypeTag) {
            typeDesc.setValue(selectedTypeTag.getDescription());
            typeKey.setValue(selectedTypeTag.getKey());
            if (selectedTypeTag.getMaxAssignments() == Integer.MAX_VALUE) {
                assignOptiongroup.setValue(multiAssignStr);
            } else {
                assignOptiongroup.setValue(singleAssignStr);
            }

            if (null == selectedTypeTag.getColour()) {
                selectedColor = new Color(44, 151, 32);
                selPreview.setColor(selectedColor);
                colorSelect.setColor(selectedColor);
                createDynamicStyleForComponents(typeName, typeKey, typeDesc, DEFAULT_COLOR);
                getPreviewButtonColor(DEFAULT_COLOR);
            } else {
                selectedColor = rgbToColorConverter(selectedTypeTag.getColour());
                selPreview.setColor(selectedColor);
                colorSelect.setColor(selectedColor);
                createDynamicStyleForComponents(typeName, typeKey, typeDesc, selectedTypeTag.getColour());
                getPreviewButtonColor(selectedTypeTag.getColour());
            }

        }
    }

    /**
     * Dynamic styles for window.
     *
     * @param top
     *            int value
     * @param marginLeft
     *            int value
     */
    private void getPreviewButtonColor(final String color) {
        Page.getCurrent().getJavaScript().execute(HawkbitCommonUtil.getPreviewButtonColorScript(color));
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

    private Label getMandatoryLabel() {
        final Label label = new Label(i18n.get("label.mandatory.field"));
        label.setStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR + " " + ValoTheme.LABEL_SMALL);
        return label;
    }

    private Slider createRGBSlider(final String caption, final String styleName) {
        final Slider slider = new Slider(caption, 0, 255);
        slider.setImmediate(true);
        slider.setWidth("150px");
        slider.addStyleName(styleName);
        return slider;
    }

    private void setRgbSliderValues(final Color color) {
        try {
            final double redColorValue = color.getRed();
            redSlider.setValue(new Double(redColorValue));
            final double blueColorValue = color.getBlue();
            blueSlider.setValue(new Double(blueColorValue));
            final double greenColorValue = color.getGreen();
            greenSlider.setValue(new Double(greenColorValue));
        } catch (final ValueOutOfBoundsException e) {
            LOG.error("Unable to set RGB color value to " + color.getRed() + "," + color.getGreen() + ","
                    + color.getBlue(), e);
        }
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

    private void setColorToComponents(final Color newColor) {
        setColor(newColor);
        colorSelect.setColor(newColor);
        getPreviewButtonColor(newColor.getCSS());
        createDynamicStyleForComponents(typeName, typeKey, typeDesc, newColor.getCSS());
    }

    /**
     * reset the tag name and tag description component border color.
     */
    private void restoreComponentStyles() {
        typeName.removeStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeDesc.removeStyleName(TYPE_DESC_DYNAMIC_STYLE);
        typeKey.removeStyleName(TYPE_NAME_DYNAMIC_STYLE);
        getPreviewButtonColor(DEFAULT_COLOR);
    }

    private void save() {
        if (mandatoryValuesPresent()) {
            final SoftwareModuleType existingType = swTypeManagementService
                    .findSoftwareModuleTypeByName(typeName.getValue());
            if (createOptiongroup.getValue().equals(createTypeStr)) {
                if (!checkIsKeyDuplicate(typeKey.getValue()) && !checkIsDuplicate(existingType)) {
                    crateNewSWModuleType();
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

    private Boolean mandatoryValuesPresent() {
        if (Strings.isNullOrEmpty(typeName.getValue()) && Strings.isNullOrEmpty(typeKey.getValue())) {
            if (createOptiongroup.getValue().equals(createTypeStr)) {
                uiNotification.displayValidationError(SPUILabelDefinitions.MISSING_TYPE_NAME_KEY);
            }
            if (createOptiongroup.getValue().equals(updateTypeStr)) {
                if (null == typeNameComboBox.getValue()) {
                    uiNotification.displayValidationError(i18n.get("message.error.missing.typename"));
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

    private void closeWindow() {
        swTypeWindow.close();
        UI.getCurrent().removeWindow(swTypeWindow);
    }

    private void discard() {
        UI.getCurrent().removeWindow(swTypeWindow);
    }

    /**
     * Create new tag.
     */
    private void crateNewSWModuleType() {
        int assignNumber = 0;
        final String colorPicked = getColorPickedString();
        final String typeNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeName.getValue());
        final String typeKeyValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeKey.getValue());
        final String typeDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeDesc.getValue());
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

        final String typeNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeName.getValue());
        final String typeDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeDesc.getValue());
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
    private void previewButtonClicked() {
        if (!tagPreviewBtnClicked) {
            final String selectedOption = (String) createOptiongroup.getValue();
            if (null != selectedOption && selectedOption.equalsIgnoreCase(updateTypeStr)) {
                if (null != typeNameComboBox.getValue()) {

                    final SoftwareModuleType typeSelected = swTypeManagementService
                            .findSoftwareModuleTypeByName(typeNameComboBox.getValue().toString());
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
        if (typeName.isEnabled() && null != colorSelect) {
            createDynamicStyleForComponents(typeName, typeKey, typeDesc, colorPickedPreview);
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
        createDynamicStyleForComponents(typeName, typeKey, typeDesc, event.getColor().getCSS());
    }

    /**
     * Set tag name and desc field border color based on chosen color.
     * 
     * @param tagName
     * @param tagDesc
     * @param taregtTagColor
     */
    private void createDynamicStyleForComponents(final TextField typeName, final TextField typeKey,
            final TextArea typeDesc, final String typeTagColor) {
        getTargetDynamicStyles(typeTagColor);
        typeName.addStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeKey.addStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeDesc.addStyleName(TYPE_DESC_DYNAMIC_STYLE);
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

}

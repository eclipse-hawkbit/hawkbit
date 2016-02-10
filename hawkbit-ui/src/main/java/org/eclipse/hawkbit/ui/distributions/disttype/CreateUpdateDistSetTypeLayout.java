/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.common.CoordinatesToColor;
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent.DistributionSetTypeEnum;
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
import org.springframework.data.domain.PageRequest;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus;

import com.google.common.base.Strings;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.AbstractColorPicker.Coordinates2Color;
import com.vaadin.ui.AbstractSelect.ItemDescriptionGenerator;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;
import com.vaadin.ui.components.colorpicker.ColorPickerGradient;
import com.vaadin.ui.components.colorpicker.ColorPickerPreview;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Window for create update Distribution Set Type.
 * 
 *
 */
@SpringComponent
@ViewScope
public class CreateUpdateDistSetTypeLayout extends CustomComponent implements ColorChangeListener, ColorSelector {

    private static final long serialVersionUID = -5169398523815877767L;
    private static final Logger LOG = LoggerFactory.getLogger(CreateUpdateDistSetTypeLayout.class);

    private static final String TYPE_NAME_DYNAMIC_STYLE = "new-tag-name";
    private static final String TYPE_DESC_DYNAMIC_STYLE = "new-tag-desc";
    private static final String DIST_TYPE_NAME = "name";
    private static final String DIST_TYPE_DESCRIPTION = "description";
    private static final String DIST_TYPE_MANDATORY = "mandatory";
    private static final String STAR = " * ";
    protected static final String DEFAULT_COLOR = "rgb(44,151,32)";

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient UINotification uiNotification;

    @Autowired
    private transient SoftwareManagement softwareManagement;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private transient DistributionSetRepository distributionSetRepository;

    /**
     * Instance of ColorPickerPreview.
     */
    private SpColorPickerPreview selPreview;

    private String createDistTypeStr;
    private String updateDistTypeStr;
    private Label createDistType;
    private Label updateDistType;
    private Label comboLabel;
    private Label madatoryLabel;
    private Label colorLabel;
    private TextField typeName;
    private TextField typeKey;
    private TextArea typeDesc;
    private Button saveDistSetType;
    private Button discardDistSetType;
    private Button tagColorPreviewBtn;
    private OptionGroup createOptiongroup;
    private ComboBox typeNameComboBox;

    private Color selectedColor;
    private ColorPickerGradient colorSelect;
    private Slider redSlider;
    private Slider greenSlider;
    private Slider blueSlider;
    private Window distTypeWindow;
    private VerticalLayout comboLayout;
    private VerticalLayout sliders;
    private VerticalLayout colorPickerLayout;
    private VerticalLayout sliderLayout;
    private HorizontalLayout colorLayout;
    private HorizontalLayout distTypeSelectLayout;
    private Set<ColorSelector> selectors;
    private Table sourceTable;
    private Table selectedTable;

    private IndexedContainer selectedTablecontainer;
    private IndexedContainer sourceTablecontainer;

    /** RGB color converter. */
    private final Coordinates2Color rgbConverter = new CoordinatesToColor();

    /**
     * Initialize the dist type tag details layout.
     */
    public void init() {
        createComponents();
        buildLayout();
        addListeners();
    }

    private void createComponents() {
        createDistTypeStr = i18n.get("label.create.type");
        updateDistTypeStr = i18n.get("label.update.type");
        createDistType = SPUIComponentProvider.getLabel(createDistTypeStr, null);
        updateDistType = SPUIComponentProvider.getLabel(updateDistTypeStr, null);
        comboLabel = SPUIComponentProvider.getLabel(i18n.get("label.choose.type"), null);
        madatoryLabel = getMandatoryLabel();

        typeName = SPUIComponentProvider.getTextField("",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.DIST_SET_TYPE_NAME, true, "",
                i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        typeName.setId(SPUIDefinitions.NEW_DISTRIBUTION_TYPE_NAME);

        typeKey = SPUIComponentProvider.getTextField("",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.DIST_SET_TYPE_KEY, true, "", i18n.get("textfield.key"),
                true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        typeKey.setId(SPUIDefinitions.NEW_DISTRIBUTION_TYPE_KEY);

        typeDesc = SPUIComponentProvider.getTextArea("",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.DIST_SET_TYPE_DESC, false, "",
                i18n.get("textfield.description"), SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);

        typeDesc.setId(SPUIDefinitions.NEW_DISTRIBUTION_TYPE_DESC);
        typeDesc.setImmediate(true);
        typeDesc.setNullRepresentation("");

        typeNameComboBox = SPUIComponentProvider.getComboBox("", "", null, null, false, "",
                i18n.get("label.combobox.type"));
        typeNameComboBox.setId(SPUIDefinitions.NEW_DISTRIBUTION_SET_TYPE_NAME_COMBO);
        typeNameComboBox.addStyleName(SPUIDefinitions.FILTER_TYPE_COMBO_STYLE);
        typeNameComboBox.setImmediate(true);
        typeNameComboBox.setPageLength(SPUIDefinitions.DIST_TYPE_SIZE);

        colorLabel = SPUIComponentProvider.getLabel(i18n.get("label.choose.type.color"), null);
        colorLabel.addStyleName(SPUIDefinitions.COLOR_LABEL_STYLE);

        tagColorPreviewBtn = new Button();
        tagColorPreviewBtn.setId(SPUIComponetIdProvider.TAG_COLOR_PREVIEW_ID);
        getPreviewButtonColor(DEFAULT_COLOR);
        tagColorPreviewBtn.setStyleName("tag-color-preview");

        saveDistSetType = SPUIComponentProvider.getButton(SPUIDefinitions.NEW_DIST_SET_TYPE_SAVE, "", "", "", true,
                FontAwesome.SAVE, SPUIButtonStyleSmallNoBorder.class);
        saveDistSetType.addStyleName(ValoTheme.BUTTON_BORDERLESS);

        discardDistSetType = SPUIComponentProvider.getButton(SPUIDefinitions.NEW_DIST_SET_TYPE_COLSE, "", "",
                "discard-button-style", true, FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        discardDistSetType.addStyleName(ValoTheme.BUTTON_BORDERLESS);

        getPreviewButtonColor(DEFAULT_COLOR);

        selectors = new HashSet<ColorSelector>();
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

    }

    private void buildLayout() {
        colorPickerLayout = new VerticalLayout();
        colorPickerLayout.setSpacing(true);
        colorPickerLayout.addStyleName("color-picker-layout");
        colorPickerLayout.addStyleName("color-picker-layout-ds-type");
        distTypeSelectLayout = createTwinColumnLayout();

        sliders = new VerticalLayout();
        sliders.addComponents(redSlider, greenSlider, blueSlider);

        selectors.add(colorSelect);

        comboLayout = new VerticalLayout();
        final VerticalLayout fieldLayout = new VerticalLayout();
        fieldLayout.setSpacing(true);
        fieldLayout.setMargin(false);
        fieldLayout.addComponent(createOptiongroup);
        fieldLayout.addComponent(madatoryLabel);
        fieldLayout.addComponent(comboLayout);
        fieldLayout.addComponent(typeName);
        fieldLayout.addComponent(typeKey);
        fieldLayout.addComponent(typeDesc);

        final VerticalLayout colorPreviewGradientLayout = new VerticalLayout();
        colorPreviewGradientLayout.addComponent(selPreview);
        colorPreviewGradientLayout.addComponent(colorSelect);
        colorPreviewGradientLayout.setComponentAlignment(selPreview, Alignment.MIDDLE_CENTER);
        colorPreviewGradientLayout.setComponentAlignment(selPreview, Alignment.MIDDLE_CENTER);

        colorPickerLayout.addComponent(colorPreviewGradientLayout);
        colorPickerLayout.setComponentAlignment(colorPreviewGradientLayout, Alignment.MIDDLE_CENTER);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.addComponent(saveDistSetType);
        buttonLayout.addComponent(discardDistSetType);
        buttonLayout.setComponentAlignment(discardDistSetType, Alignment.BOTTOM_RIGHT);
        buttonLayout.setComponentAlignment(saveDistSetType, Alignment.BOTTOM_LEFT);
        buttonLayout.setImmediate(true);
        buttonLayout.addStyleName("window-style");
        buttonLayout.setSizeFull();

        final HorizontalLayout mainLayout = new HorizontalLayout();
        final VerticalLayout twinTableLayout = new VerticalLayout();
        twinTableLayout.setSizeFull();
        twinTableLayout.addComponent(distTypeSelectLayout);

        mainLayout.addComponent(fieldLayout);
        mainLayout.addComponent(twinTableLayout);

        colorLayout = new HorizontalLayout();
        sliderLayout = new VerticalLayout();
        final HorizontalLayout chooseColorLayout = new HorizontalLayout();
        chooseColorLayout.addComponents(colorLabel, tagColorPreviewBtn);
        chooseColorLayout.setComponentAlignment(colorLabel, Alignment.TOP_CENTER);
        chooseColorLayout.setComponentAlignment(tagColorPreviewBtn, Alignment.TOP_CENTER);
        sliderLayout.addComponent(chooseColorLayout);
        colorLayout.addComponent(sliderLayout);

        final VerticalLayout mainWindowLayout = new VerticalLayout();
        mainWindowLayout.addComponent(mainLayout);
        mainWindowLayout.addComponent(colorLayout);
        mainWindowLayout.addComponent(buttonLayout);
        setCompositionRoot(mainWindowLayout);
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
        Item saveTblitem = null;
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

    public Window getWindow() {
        reset();
        distTypeWindow = SPUIComponentProvider.getWindow(i18n.get("caption.add.type"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW);
        distTypeWindow.setContent(this);
        return distTypeWindow;

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
            optionValues.add(createDistType.getValue());
        }
        if (permChecker.hasUpdateDistributionPermission()) {
            optionValues.add(updateDistType.getValue());
        }
        createOptionGroup(optionValues);
    }

    private void createOptionGroup(final List<String> typeOptions) {
        createOptiongroup = new OptionGroup("", typeOptions);
        createOptiongroup.setId(SPUIDefinitions.CREATE_OPTION_GROUP_DISTRIBUTION_SET_TYPE_ID);
        createOptiongroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        createOptiongroup.addStyleName("custom-option-group");
        createOptiongroup.setNullSelectionAllowed(false);
        createOptiongroup.setCaption(null);
        if (!typeOptions.isEmpty()) {
            createOptiongroup.select(typeOptions.get(0));
        }
    }

    private void addListeners() {
        saveDistSetType.addClickListener(event -> save());
        discardDistSetType.addClickListener(event -> discard());
        colorSelect.addColorChangeListener(this);
        selPreview.addColorChangeListener(this);
        tagColorPreviewBtn.addClickListener(event -> previewButtonClicked());
        createOptiongroup.addValueChangeListener(event -> createOptionValueChanged(event));
        typeNameComboBox.addValueChangeListener(event -> typeNameChosen(event));
        slidersValueChangeListeners();
    }

    private void save() {
        if (mandatoryValuesPresent()) {
            final DistributionSetType existingDistTypeByKey = distributionSetManagement
                    .findDistributionSetTypeByKey(typeKey.getValue());
            final DistributionSetType existingDistTypeByName = distributionSetManagement
                    .findDistributionSetTypeByName(typeName.getValue());
            if (createOptiongroup.getValue().equals(createDistTypeStr)) {
                if (!checkIsDuplicateByKey(existingDistTypeByKey) && !checkIsDuplicate(existingDistTypeByName)) {
                    crateNewDistributionSetType();
                }
            } else {
                updateDistributionSetType(existingDistTypeByKey);
            }
        }
    }

    private Boolean mandatoryValuesPresent() {
        if (Strings.isNullOrEmpty(typeName.getValue())) {
            if (createOptiongroup.getValue().equals(createDistTypeStr)) {

                uiNotification.displayValidationError(SPUILabelDefinitions.MISSING_TYPE_NAME_KEY);
            }
            if (createOptiongroup.getValue().equals(updateDistTypeStr)) {
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

    private Boolean checkIsDuplicate(final DistributionSetType existingDistType) {
        if (existingDistType != null) {
            uiNotification.displayValidationError(
                    i18n.get("message.tag.duplicate.check", new Object[] { existingDistType.getName() }));
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean checkIsDuplicateByKey(final DistributionSetType existingDistType) {
        if (existingDistType != null) {
            uiNotification.displayValidationError(
                    i18n.get("message.type.key.duplicate.check", new Object[] { existingDistType.getKey() }));
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private void closeWindow() {
        distTypeWindow.close();
        UI.getCurrent().removeWindow(distTypeWindow);
    }

    private void discard() {
        UI.getCurrent().removeWindow(distTypeWindow);
    }

    /**
     * Create new DistSet Type tag.
     */
    @SuppressWarnings("unchecked")
    private void crateNewDistributionSetType() {
        final String colorPicked = getColorPickedSting();
        final String typeNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeName.getValue());
        final String typeKeyValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeKey.getValue());
        final String typeDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeDesc.getValue());
        final List<Long> itemIds = (List<Long>) selectedTable.getItemIds();
        if (null != typeNameValue && null != typeKeyValue && null != itemIds && !itemIds.isEmpty()) {
            DistributionSetType newDistType = new DistributionSetType(typeKeyValue, typeNameValue, typeDescValue);
            for (final Long id : itemIds) {
                final Item item = selectedTable.getItem(id);
                final String dist_type_name = (String) item.getItemProperty(DIST_TYPE_NAME).getValue();
                final CheckBox mandatoryCheckBox = (CheckBox) item.getItemProperty(DIST_TYPE_MANDATORY).getValue();
                final Boolean isMandatory = mandatoryCheckBox.getValue();
                final SoftwareModuleType swModuleType = softwareManagement.findSoftwareModuleTypeByName(dist_type_name);
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
        final String typeNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeName.getValue());
        final String typeKeyValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeKey.getValue());
        final String typeDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(typeDesc.getValue());
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
            updateDistSetType.setColour(getColorPickedSting());
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

    /**
     * Get color picked value in string.
     *
     * @return String of color picked value.
     */
    private String getColorPickedSting() {
        return "rgb(" + getSelPreview().getColor().getRed() + "," + getSelPreview().getColor().getGreen() + ","
                + getSelPreview().getColor().getBlue() + ")";
    }

    /**
     * Color view.
     * 
     * @return ColorPickerPreview as UI
     */
    public ColorPickerPreview getSelPreview() {
        return selPreview;
    }

    /**
     * Open color picker on click of preview button. Auto select the color based
     * on target tag if already selected.
     */
    private void previewButtonClicked() {
        final String selectedOption = (String) createOptiongroup.getValue();
        if (null != selectedOption && selectedOption.equalsIgnoreCase(updateDistTypeStr)
                && null != typeNameComboBox.getValue()) {

            final DistributionSetType existedDistType = distributionSetManagement
                    .findDistributionSetTypeByKey(typeNameComboBox.getValue().toString());
            if (null != existedDistType) {
                selectedColor = existedDistType.getColour() != null ? rgbToColorConverter(existedDistType.getColour())
                        : rgbToColorConverter(DEFAULT_COLOR);
            } else {
                selectedColor = rgbToColorConverter(DEFAULT_COLOR);
            }

        }
        selPreview.setColor(selectedColor);
        sliderLayout.addComponent(sliders);
        colorLayout.addComponent(colorPickerLayout);
        colorLayout.setComponentAlignment(colorPickerLayout, Alignment.MIDDLE_CENTER);
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

    /**
     * Value change listeners implementations of sliders.
     */
    private void slidersValueChangeListeners() {
        redSlider.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = -8336732883300920839L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double red = (Double) event.getProperty().getValue();
                final Color newColor = new Color((int) red, selectedColor.getGreen(), selectedColor.getBlue());
                setColorToComponents(newColor);
            }
        });
        greenSlider.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1236358037711775663L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double green = (Double) event.getProperty().getValue();
                final Color newColor = new Color(selectedColor.getRed(), (int) green, selectedColor.getBlue());
                setColorToComponents(newColor);
            }
        });
        blueSlider.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 8466370744686043947L;

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
     * Set tag name and desc field border color based on chosen color.
     *
     * @param tagName
     * @param tagDesc
     * @param taregtTagColor
     */
    private void createDynamicStyleForComponents(final TextField typeName, final TextField typeKey,
            final TextArea typeDesc, final String typeTagColor) {
        typeName.removeStyleName(SPUIDefinitions.TYPE_NAME);
        typeKey.removeStyleName(SPUIDefinitions.TYPE_KEY);
        typeDesc.removeStyleName(SPUIDefinitions.TYPE_DESC);
        getDistributionDynamicStyles(typeTagColor);
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
    private void getDistributionDynamicStyles(final String colorPickedPreview) {
        Page.getCurrent().getJavaScript()
                .execute(HawkbitCommonUtil.changeToNewSelectedPreviewColor(colorPickedPreview));
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
    private void reset() {
        typeName.setEnabled(true);
        typeName.clear();
        typeKey.clear();
        typeDesc.clear();
        colorLayout.removeComponent(colorPickerLayout);
        sliderLayout.removeComponent(sliders);
        restoreComponentStyles();
        comboLayout.removeComponent(comboLabel);
        comboLayout.removeComponent(typeNameComboBox);
        selectedTable.removeAllItems();
        getSourceTableData();
        createOptiongroup.select(createDistTypeStr);
        selectedColor = new Color(44, 151, 32);
        selPreview.setColor(selectedColor);

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
            selectedTable.getContainerDataSource().removeAllItems();
            getSourceTableData();

            typeKey.setEnabled(false);
            typeName.setEnabled(false);
            distTypeSelectLayout.setEnabled(false);
            populateTypeNameCombo();
            comboLayout.addComponent(comboLabel);
            comboLayout.addComponent(typeNameComboBox);
        } else {
            typeKey.setEnabled(true);
            typeName.setEnabled(true);
            saveDistSetType.setEnabled(true);
            distTypeSelectLayout.setEnabled(true);
            typeName.clear();
            typeDesc.clear();
            typeKey.clear();
            selectedTable.setEnabled(true);
            selectedTable.getContainerDataSource().removeAllItems();
            sourceTable.getContainerDataSource().removeAllItems();
            getSourceTableData();
            comboLayout.removeComponent(comboLabel);
            comboLayout.removeComponent(typeNameComboBox);
        }
        restoreComponentStyles();
        getPreviewButtonColor(DEFAULT_COLOR);
        selPreview.setColor(rgbToColorConverter(DEFAULT_COLOR));
    }

    /**
     * Populate DistributionSet Type name combo.
     */
    public void populateTypeNameCombo() {
        typeNameComboBox.setContainerDataSource(getDistSetTypeLazyQueryContainer());
        typeNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);

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

    private void typeNameChosen(final ValueChangeEvent event) {
        final String typeSelected = (String) event.getProperty().getValue();
        if (null != typeSelected) {
            setTypeTagCombo(typeSelected);
        } else {
            resetTypeFields();
        }
    }

    private void resetTypeFields() {
        typeName.setEnabled(false);
        typeKey.setEnabled(false);
        typeName.clear();
        typeKey.clear();
        typeDesc.clear();
        restoreComponentStyles();
        selectedTable.removeAllItems();
        getSourceTableData();
        restoreComponentStyles();
        getPreviewButtonColor(DEFAULT_COLOR);
        selPreview.setColor(rgbToColorConverter(DEFAULT_COLOR));

    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     * 
     * @param targetTagSelected
     *            as the selected tag from combo
     */
    private void setTypeTagCombo(final String distSetTypeSelected) {
        boolean mandatory = false;
        typeName.setValue(distSetTypeSelected);
        getSourceTableData();
        selectedTable.getContainerDataSource().removeAllItems();
        final DistributionSetType selectedTypeTag = fetchDistributionSetType(distSetTypeSelected);
        if (null != selectedTypeTag) {
            typeDesc.setValue(selectedTypeTag.getDescription());
            typeKey.setValue(selectedTypeTag.getKey());

            if (distributionSetRepository.countByType(selectedTypeTag) <= 0) {
                distTypeSelectLayout.setEnabled(true);
                selectedTable.setEnabled(true);
                saveDistSetType.setEnabled(true);
            } else {
                uiNotification.displayValidationError(
                        selectedTypeTag.getName() + "  " + i18n.get("message.error.dist.set.type.update"));
                distTypeSelectLayout.setEnabled(false);
                selectedTable.setEnabled(false);
                saveDistSetType.setEnabled(false);
            }
            for (final SoftwareModuleType swModuleType : selectedTypeTag.getOptionalModuleTypes()) {
                mandatory = false;
                addTargetTableforUpdate(swModuleType, mandatory);
            }

            for (final SoftwareModuleType swModuleType : selectedTypeTag.getMandatoryModuleTypes()) {
                mandatory = true;
                addTargetTableforUpdate(swModuleType, mandatory);
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.ui.components.colorpicker.ColorSelector#setColor(com.vaadin.
     * shared.ui.colorpicker .Color)
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.components.colorpicker.ColorSelector#getColor()
     */
    @Override
    public Color getColor() {

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.ui.components.colorpicker.ColorChangeListener#colorChanged(com
     * .vaadin.ui.components .colorpicker.ColorChangeEvent)
     */
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
     * reset the tag name and tag description component border color.
     */
    private void restoreComponentStyles() {
        typeName.removeStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeDesc.removeStyleName(TYPE_DESC_DYNAMIC_STYLE);
        typeKey.removeStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeName.addStyleName(SPUIDefinitions.DIST_SET_TYPE_NAME);
        typeDesc.addStyleName(SPUIDefinitions.DIST_SET_TYPE_DESC);
        typeKey.addStyleName(SPUIDefinitions.DIST_SET_TYPE_KEY);
        getPreviewButtonColor(DEFAULT_COLOR);
    }

}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.tag;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CoordinatesToColor;
import org.eclipse.hawkbit.ui.common.PopupWindowHelp;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.colorpicker.Color;
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
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;
import com.vaadin.ui.components.colorpicker.ColorPickerGradient;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract class for create/update target tag layout.
 */
public abstract class CreateUpdateTagLayout extends CustomComponent implements ColorChangeListener, ColorSelector {
    private static final long serialVersionUID = 4229177824620576456L;
    private static final Logger LOG = LoggerFactory.getLogger(CreateUpdateTagLayout.class);
    protected static final String DEFAULT_COLOR = "rgb(44,151,32)";
    private static final String TAG_NAME_DYNAMIC_STYLE = "new-tag-name";
    private static final String TAG_DESC_DYNAMIC_STYLE = "new-tag-desc";
    private static final String TAG_DYNAMIC_STYLE = "tag-color-preview";

    protected String createTagNw;
    protected String updateTagNw;

    @Autowired
    private transient UiProperties uiProperties;

    @Autowired
    protected I18N i18n;

    @Autowired
    protected transient TagManagement tagManagement;

    @Autowired
    protected transient EventBus.SessionEventBus eventBus;

    @Autowired
    protected SpPermissionChecker permChecker;
    /**
     * Local Instance of ColorPickerPreview.
     */
    protected SpColorPickerPreview selPreview;

    protected Label createTag;
    protected Label updateTag;
    private Label comboLabel;
    private Label colorLabel;
    private Label madatoryLabel;
    protected TextField tagName;
    protected TextArea tagDesc;
    private Button saveTag;
    private Button discardTag;
    private Button tagColorPreviewBtn;
    protected OptionGroup optiongroup;
    protected ComboBox tagNameComboBox;

    protected ColorPickerGradient colorSelect;
    private Set<ColorSelector> selectors;
    protected Color selectedColor;

    private Slider redSlider;
    private Slider greenSlider;
    private Slider blueSlider;

    private VerticalLayout comboLayout;
    private VerticalLayout sliders;
    private VerticalLayout colorPickerLayout;
    private HorizontalLayout mainLayout;
    private VerticalLayout fieldLayout;

    /** RGB color converter. */
    private final Coordinates2Color rgbConverter = new CoordinatesToColor();

    protected boolean tagPreviewBtnClicked = false;

    /**
     * Save new tag / update new tag.
     *
     * @param event
     */
    protected abstract void save(final Button.ClickEvent event);

    /**
     * Discard the changes and close the popup.
     *
     * @param event
     */
    protected abstract void discard(final Button.ClickEvent event);

    /**
     * create option group with Create tag/Update tag based on permissions.
     */
    protected abstract void createOptionGroup();

    /**
     * Populate target name combo.
     */
    protected abstract void populateTagNameCombo();

    protected abstract void setTagDetails(final String tagSelected);

    /**
     * Init the layout.
     */
    public void init() {
        createRequiredComponents();
        addListeners();
        buildLayout();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    private void createRequiredComponents() {
        createTagNw = i18n.get("label.create.tag");
        updateTagNw = i18n.get("label.update.tag");
        createTag = SPUIComponentProvider.getLabel(createTagNw, null);
        updateTag = SPUIComponentProvider.getLabel(i18n.get("label.update.tag"), null);
        comboLabel = SPUIComponentProvider.getLabel(i18n.get("label.choose.tag"), null);
        madatoryLabel = getMandatoryLabel();
        colorLabel = SPUIComponentProvider.getLabel(i18n.get("label.choose.tag.color"), null);
        colorLabel.addStyleName(SPUIDefinitions.COLOR_LABEL_STYLE);

        tagName = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TAG_NAME,
                true, "", i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        tagName.setId(SPUIDefinitions.NEW_TARGET_TAG_NAME);

        tagDesc = SPUIComponentProvider.getTextArea("", ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TAG_DESC,
                false, "", i18n.get("textfield.description"), SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);

        tagDesc.setId(SPUIDefinitions.NEW_TARGET_TAG_DESC);
        tagDesc.setImmediate(true);
        tagDesc.setNullRepresentation("");

        tagNameComboBox = SPUIComponentProvider.getComboBox("", "", null, null, false, "",
                i18n.get("label.combobox.tag"));
        tagNameComboBox.addStyleName(SPUIDefinitions.FILTER_TYPE_COMBO_STYLE);
        tagNameComboBox.setImmediate(true);

        saveTag = SPUIComponentProvider.getButton(SPUIDefinitions.NEW_TARGET_TAG_SAVE, "", "", "", true,
                FontAwesome.SAVE, SPUIButtonStyleSmallNoBorder.class);
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

        createOptionGroup();

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
        fieldLayout.setSpacing(false);
        fieldLayout.setMargin(false);
        fieldLayout.setWidth("100%");
        fieldLayout.setHeight(null);
        fieldLayout.addComponent(optiongroup);
        fieldLayout.addComponent(comboLayout);
        fieldLayout.addComponent(madatoryLabel);
        fieldLayout.addComponent(tagName);
        fieldLayout.addComponent(tagDesc);

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
        fieldButtonLayout.addComponent(new PopupWindowHelp(uiProperties.getLinks().getDocumentation().getRoot()));
        fieldButtonLayout.addComponent(fieldLayout);
        fieldButtonLayout.addComponent(buttonLayout);
        fieldButtonLayout.setComponentAlignment(buttonLayout, Alignment.BOTTOM_CENTER);

        mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.addComponent(fieldButtonLayout);

        setCompositionRoot(mainLayout);
    }

    private void addListeners() {
        saveTag.addClickListener(event -> save(event));
        discardTag.addClickListener(event -> discard(event));
        colorSelect.addColorChangeListener(this);
        selPreview.addColorChangeListener(this);
        tagColorPreviewBtn.addClickListener(event -> previewButtonClicked());
        optiongroup.addValueChangeListener(event -> optionValueChanged(event));
        tagNameComboBox.addValueChangeListener(event -> tagNameChosen(event));
        slidersValueChangeListeners();
    }

    /**
     * Open color picker on click of preview button. Auto select the color based
     * on target tag if already selected.
     */
    private void previewButtonClicked() {
        if (!tagPreviewBtnClicked) {
            setColor();
            selPreview.setColor(selectedColor);
            fieldLayout.addComponent(sliders);
            mainLayout.addComponent(colorPickerLayout);
            mainLayout.setComponentAlignment(colorPickerLayout, Alignment.BOTTOM_CENTER);
        }
        tagPreviewBtnClicked = !tagPreviewBtnClicked;
    }

    private void setColor() {
        final String selectedOption = (String) optiongroup.getValue();
        if (selectedOption == null || !selectedOption.equalsIgnoreCase(updateTagNw)) {
            return;
        }

        if (tagNameComboBox.getValue() == null) {
            selectedColor = rgbToColorConverter(DEFAULT_COLOR);
            return;
        }

        final TargetTag targetTagSelected = tagManagement.findTargetTag(tagNameComboBox.getValue().toString());

        if (targetTagSelected == null) {
            final DistributionSetTag distTag = tagManagement
                    .findDistributionSetTag(tagNameComboBox.getValue().toString());
            selectedColor = distTag.getColour() != null ? rgbToColorConverter(distTag.getColour())
                    : rgbToColorConverter(DEFAULT_COLOR);
        } else {
            selectedColor = targetTagSelected.getColour() != null ? rgbToColorConverter(targetTagSelected.getColour())
                    : rgbToColorConverter(DEFAULT_COLOR);
        }

    }

    /**
     * Covert RGB code to {@Color}.
     *
     * @param value
     *            RGB vale
     * @return Color
     */
    protected Color rgbToColorConverter(final String value) {
        if (!value.startsWith("rgb")) {
            return null;
        }
        // RGB color format rgb/rgba(255,255,255,0.1)
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

    private Label getMandatoryLabel() {
        final Label label = new Label(i18n.get("label.mandatory.field"));
        label.setStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR + " " + ValoTheme.LABEL_SMALL);
        return label;
    }

    private void tagNameChosen(final ValueChangeEvent event) {
        final String tagSelected = (String) event.getProperty().getValue();
        if (null != tagSelected) {
            setTagDetails(tagSelected);
        } else {
            resetTagNameField();
        }
    }

    private void resetTagNameField() {
        tagName.setEnabled(false);
        tagName.clear();

        tagDesc.clear();
        restoreComponentStyles();
        fieldLayout.removeComponent(sliders);
        mainLayout.removeComponent(colorPickerLayout);
        selectedColor = new Color(44, 151, 32);
        selPreview.setColor(selectedColor);
        tagPreviewBtnClicked = false;

    }

    /**
     * Listener for option group - Create tag/Update.
     *
     * @param event
     *            ValueChangeEvent
     */
    private void optionValueChanged(final ValueChangeEvent event) {
        if ("Update Tag".equals(event.getProperty().getValue())) {
            tagName.clear();
            tagDesc.clear();
            tagName.setEnabled(false);
            populateTagNameCombo();
            // show target name combo
            comboLayout.addComponent(comboLabel);
            comboLayout.addComponent(tagNameComboBox);
        } else {
            tagName.setEnabled(true);
            tagName.clear();
            tagDesc.clear();
            // hide target name combo
            comboLayout.removeComponent(comboLabel);
            comboLayout.removeComponent(tagNameComboBox);
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
     * reset the components.
     */
    protected void reset() {
        tagName.setEnabled(true);
        tagName.clear();
        tagDesc.clear();
        restoreComponentStyles();

        // hide target name combo
        comboLayout.removeComponent(comboLabel);
        comboLayout.removeComponent(tagNameComboBox);
        fieldLayout.removeComponent(sliders);
        mainLayout.removeComponent(colorPickerLayout);

        optiongroup.select(createTagNw);

        // Default green color
        selectedColor = new Color(44, 151, 32);
        selPreview.setColor(selectedColor);
        tagPreviewBtnClicked = false;
    }

    /**
     * On change of color in color picker ,change RGB sliders, components border
     * color and color of preview button.
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
        createDynamicStyleForComponents(tagName, tagDesc, event.getColor().getCSS());
    }

    /**
     * Dynamic styles for window.
     *
     * @param top
     *            int value
     * @param marginLeft
     *            int value
     */
    protected void getPreviewButtonColor(final String color) {
        Page.getCurrent().getJavaScript().execute(HawkbitCommonUtil.getPreviewButtonColorScript(color));
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
     * Set tag name and desc field border color based on chosen color.
     *
     * @param tagName
     * @param tagDesc
     * @param taregtTagColor
     */
    protected void createDynamicStyleForComponents(final TextField tagName, final TextArea tagDesc,
            final String taregtTagColor) {
        tagName.removeStyleName(SPUIDefinitions.TAG_NAME);
        tagDesc.removeStyleName(SPUIDefinitions.TAG_DESC);
        getTargetDynamicStyles(taregtTagColor);
        tagName.addStyleName(TAG_NAME_DYNAMIC_STYLE);
        tagDesc.addStyleName(TAG_DESC_DYNAMIC_STYLE);
    }

    /**
     * reset the tag name and tag description component border color.
     */
    private void restoreComponentStyles() {
        tagName.removeStyleName(TAG_NAME_DYNAMIC_STYLE);
        tagDesc.removeStyleName(TAG_DESC_DYNAMIC_STYLE);
        tagName.addStyleName(SPUIDefinitions.TAG_NAME);
        tagDesc.addStyleName(SPUIDefinitions.TAG_DESC);
        getPreviewButtonColor(DEFAULT_COLOR);
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
    public Color getColor() {
        return null;
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
            createDynamicStyleForComponents(tagName, tagDesc, colorPickedPreview);
            colorSelect.setColor(selPreview.getColor());
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
        createDynamicStyleForComponents(tagName, tagDesc, newColor.getCSS());
    }

    @Override
    public void addColorChangeListener(final ColorChangeListener listener) {
    }

    @Override
    public void removeColorChangeListener(final ColorChangeListener listener) {
    }

}

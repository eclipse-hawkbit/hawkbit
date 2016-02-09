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
import org.eclipse.hawkbit.ui.common.CoordinatesToColor;
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
        this.eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        this.eventBus.unsubscribe(this);
    }

    private void createRequiredComponents() {
        this.createTagNw = this.i18n.get("label.create.tag");
        this.updateTagNw = this.i18n.get("label.update.tag");
        this.createTag = SPUIComponentProvider.getLabel(this.createTagNw, null);
        this.updateTag = SPUIComponentProvider.getLabel(this.i18n.get("label.update.tag"), null);
        this.comboLabel = SPUIComponentProvider.getLabel(this.i18n.get("label.choose.tag"), null);
        this.madatoryLabel = getMandatoryLabel();
        this.colorLabel = SPUIComponentProvider.getLabel(this.i18n.get("label.choose.tag.color"), null);
        this.colorLabel.addStyleName(SPUIDefinitions.COLOR_LABEL_STYLE);

        this.tagName = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TAG_NAME,
                true, "", this.i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        this.tagName.setId(SPUIDefinitions.NEW_TARGET_TAG_NAME);

        this.tagDesc = SPUIComponentProvider.getTextArea("", ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TAG_DESC,
                false, "", this.i18n.get("textfield.description"), SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);

        this.tagDesc.setId(SPUIDefinitions.NEW_TARGET_TAG_DESC);
        this.tagDesc.setImmediate(true);
        this.tagDesc.setNullRepresentation("");

        this.tagNameComboBox = SPUIComponentProvider.getComboBox("", "", null, null, false, "",
                this.i18n.get("label.combobox.tag"));
        this.tagNameComboBox.addStyleName(SPUIDefinitions.FILTER_TYPE_COMBO_STYLE);
        this.tagNameComboBox.setImmediate(true);

        this.saveTag = SPUIComponentProvider.getButton(SPUIDefinitions.NEW_TARGET_TAG_SAVE, "", "", "", true,
                FontAwesome.SAVE, SPUIButtonStyleSmallNoBorder.class);
        this.saveTag.addStyleName(ValoTheme.BUTTON_BORDERLESS);

        this.discardTag = SPUIComponentProvider.getButton(SPUIDefinitions.NEW_TARGET_TAG_DISRACD, "", "",
                "discard-button-style", true, FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        this.discardTag.addStyleName(ValoTheme.BUTTON_BORDERLESS);

        this.tagColorPreviewBtn = new Button();
        this.tagColorPreviewBtn.setId(SPUIComponetIdProvider.TAG_COLOR_PREVIEW_ID);
        getPreviewButtonColor(DEFAULT_COLOR);
        this.tagColorPreviewBtn.setStyleName(TAG_DYNAMIC_STYLE);

        this.selectors = new HashSet<>();
        this.selectedColor = new Color(44, 151, 32);
        this.selPreview = new SpColorPickerPreview(this.selectedColor);

        this.colorSelect = new ColorPickerGradient("rgb-gradient", this.rgbConverter);
        this.colorSelect.setColor(this.selectedColor);
        this.colorSelect.setWidth("220px");

        this.redSlider = createRGBSlider("", "red");
        this.greenSlider = createRGBSlider("", "green");
        this.blueSlider = createRGBSlider("", "blue");
        setRgbSliderValues(this.selectedColor);

        createOptionGroup();

    }

    private void buildLayout() {
        this.comboLayout = new VerticalLayout();

        this.sliders = new VerticalLayout();
        this.sliders.addComponents(this.redSlider, this.greenSlider, this.blueSlider);

        this.selectors.add(this.colorSelect);

        this.colorPickerLayout = new VerticalLayout();
        this.colorPickerLayout.setStyleName("rgb-vertical-layout");
        this.colorPickerLayout.addComponent(this.selPreview);
        this.colorPickerLayout.addComponent(this.colorSelect);

        this.fieldLayout = new VerticalLayout();
        this.fieldLayout.setSpacing(false);
        this.fieldLayout.setMargin(false);
        this.fieldLayout.setWidth("100%");
        this.fieldLayout.setHeight(null);
        this.fieldLayout.addComponent(this.optiongroup);
        this.fieldLayout.addComponent(this.comboLayout);
        this.fieldLayout.addComponent(this.madatoryLabel);
        this.fieldLayout.addComponent(this.tagName);
        this.fieldLayout.addComponent(this.tagDesc);

        final HorizontalLayout colorLabelLayout = new HorizontalLayout();
        colorLabelLayout.addComponents(this.colorLabel, this.tagColorPreviewBtn);
        this.fieldLayout.addComponent(colorLabelLayout);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.addComponent(this.saveTag);
        buttonLayout.addComponent(this.discardTag);
        buttonLayout.setComponentAlignment(this.discardTag, Alignment.BOTTOM_RIGHT);
        buttonLayout.setComponentAlignment(this.saveTag, Alignment.BOTTOM_LEFT);
        buttonLayout.addStyleName("window-style");
        buttonLayout.setWidth("152px");

        final VerticalLayout fieldButtonLayout = new VerticalLayout();
        fieldButtonLayout.addComponent(this.fieldLayout);
        fieldButtonLayout.addComponent(buttonLayout);
        fieldButtonLayout.setComponentAlignment(buttonLayout, Alignment.BOTTOM_CENTER);

        this.mainLayout = new HorizontalLayout();
        this.mainLayout.addComponent(fieldButtonLayout);

        setCompositionRoot(this.mainLayout);

    }

    private void addListeners() {
        this.saveTag.addClickListener(event -> save(event));
        this.discardTag.addClickListener(event -> discard(event));
        this.colorSelect.addColorChangeListener(this);
        this.selPreview.addColorChangeListener(this);
        this.tagColorPreviewBtn.addClickListener(event -> previewButtonClicked());
        this.optiongroup.addValueChangeListener(event -> optionValueChanged(event));
        this.tagNameComboBox.addValueChangeListener(event -> tagNameChosen(event));
        slidersValueChangeListeners();
    }

    /**
     * Open color picker on click of preview button. Auto select the color based
     * on target tag if already selected.
     */
    private void previewButtonClicked() {
        if (!this.tagPreviewBtnClicked) {
            setColor();
            this.selPreview.setColor(this.selectedColor);
            this.fieldLayout.addComponent(this.sliders);
            this.mainLayout.addComponent(this.colorPickerLayout);
            this.mainLayout.setComponentAlignment(this.colorPickerLayout, Alignment.BOTTOM_CENTER);
        }
        this.tagPreviewBtnClicked = !this.tagPreviewBtnClicked;
    }

    private void setColor() {
        final String selectedOption = (String) this.optiongroup.getValue();
        if (selectedOption == null || !selectedOption.equalsIgnoreCase(this.updateTagNw)) {
            return;
        }

        if (this.tagNameComboBox.getValue() == null) {
            this.selectedColor = rgbToColorConverter(DEFAULT_COLOR);
            return;
        }

        final TargetTag targetTagSelected = this.tagManagement
                .findTargetTag(this.tagNameComboBox.getValue().toString());

        if (targetTagSelected == null) {
            final DistributionSetTag distTag = this.tagManagement
                    .findDistributionSetTag(this.tagNameComboBox.getValue().toString());
            this.selectedColor = distTag.getColour() != null ? rgbToColorConverter(distTag.getColour())
                    : rgbToColorConverter(DEFAULT_COLOR);
        } else {
            this.selectedColor = targetTagSelected.getColour() != null
                    ? rgbToColorConverter(targetTagSelected.getColour()) : rgbToColorConverter(DEFAULT_COLOR);
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
        final Label label = new Label(this.i18n.get("label.mandatory.field"));
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
        this.tagName.setEnabled(false);
        this.tagName.clear();

        this.tagDesc.clear();
        restoreComponentStyles();
        this.fieldLayout.removeComponent(this.sliders);
        this.mainLayout.removeComponent(this.colorPickerLayout);
        this.selectedColor = new Color(44, 151, 32);
        this.selPreview.setColor(this.selectedColor);
        this.tagPreviewBtnClicked = false;

    }

    /**
     * Listener for option group - Create tag/Update.
     *
     * @param event
     *            ValueChangeEvent
     */
    private void optionValueChanged(final ValueChangeEvent event) {
        if ("Update Tag".equals(event.getProperty().getValue())) {
            this.tagName.clear();
            this.tagDesc.clear();
            this.tagName.setEnabled(false);
            populateTagNameCombo();
            // show target name combo
            this.comboLayout.addComponent(this.comboLabel);
            this.comboLayout.addComponent(this.tagNameComboBox);
        } else {
            this.tagName.setEnabled(true);
            this.tagName.clear();
            this.tagDesc.clear();
            // hide target name combo
            this.comboLayout.removeComponent(this.comboLabel);
            this.comboLayout.removeComponent(this.tagNameComboBox);
        }
        // close the color picker layout
        this.tagPreviewBtnClicked = false;
        // reset the selected color - Set defualt color
        restoreComponentStyles();
        getPreviewButtonColor(DEFAULT_COLOR);
        this.selPreview.setColor(rgbToColorConverter(DEFAULT_COLOR));
        // remove the sliders and color picker layout
        this.fieldLayout.removeComponent(this.sliders);
        this.mainLayout.removeComponent(this.colorPickerLayout);

    }

    /**
     * reset the components.
     */
    protected void reset() {
        this.tagName.setEnabled(true);
        this.tagName.clear();
        this.tagDesc.clear();
        restoreComponentStyles();

        // hide target name combo
        this.comboLayout.removeComponent(this.comboLabel);
        this.comboLayout.removeComponent(this.tagNameComboBox);
        this.fieldLayout.removeComponent(this.sliders);
        this.mainLayout.removeComponent(this.colorPickerLayout);

        this.optiongroup.select(this.createTagNw);

        // Default green color
        this.selectedColor = new Color(44, 151, 32);
        this.selPreview.setColor(this.selectedColor);
        this.tagPreviewBtnClicked = false;
    }

    /**
     * On change of color in color picker ,change RGB sliders, components border
     * color and color of preview button.
     */
    @Override
    public void colorChanged(final ColorChangeEvent event) {
        setColor(event.getColor());
        for (final ColorSelector select : this.selectors) {
            if (!event.getSource().equals(select) && select.equals(this)
                    && !select.getColor().equals(this.selectedColor)) {
                select.setColor(this.selectedColor);
            }
        }
        setRgbSliderValues(this.selectedColor);
        getPreviewButtonColor(event.getColor().getCSS());
        createDynamicStyleForComponents(this.tagName, this.tagDesc, event.getColor().getCSS());
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
            this.redSlider.setValue(new Double(redColorValue));
            final double blueColorValue = color.getBlue();
            this.blueSlider.setValue(new Double(blueColorValue));
            final double greenColorValue = color.getGreen();
            this.greenSlider.setValue(new Double(greenColorValue));
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
        this.tagName.removeStyleName(TAG_NAME_DYNAMIC_STYLE);
        this.tagDesc.removeStyleName(TAG_DESC_DYNAMIC_STYLE);
        this.tagName.addStyleName(SPUIDefinitions.TAG_NAME);
        this.tagDesc.addStyleName(SPUIDefinitions.TAG_DESC);
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
        this.selectedColor = color;
        this.selPreview.setColor(this.selectedColor);
        final String colorPickedPreview = this.selPreview.getColor().getCSS();
        if (this.tagName.isEnabled() && null != this.colorSelect) {
            createDynamicStyleForComponents(this.tagName, this.tagDesc, colorPickedPreview);
            this.colorSelect.setColor(this.selPreview.getColor());
        }
    }

    /**
     * Value change listeners implementations of sliders.
     */
    private void slidersValueChangeListeners() {
        this.redSlider.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = -8336732888800920839L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double red = (Double) event.getProperty().getValue();
                final Color newColor = new Color((int) red, CreateUpdateTagLayout.this.selectedColor.getGreen(),
                        CreateUpdateTagLayout.this.selectedColor.getBlue());
                setColorToComponents(newColor);
            }
        });
        this.greenSlider.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1236358037766775663L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double green = (Double) event.getProperty().getValue();
                final Color newColor = new Color(CreateUpdateTagLayout.this.selectedColor.getRed(), (int) green,
                        CreateUpdateTagLayout.this.selectedColor.getBlue());
                setColorToComponents(newColor);
            }
        });
        this.blueSlider.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 8466370763686043947L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double blue = (Double) event.getProperty().getValue();
                final Color newColor = new Color(CreateUpdateTagLayout.this.selectedColor.getRed(),
                        CreateUpdateTagLayout.this.selectedColor.getGreen(), (int) blue);
                setColorToComponents(newColor);
            }
        });
    }

    private void setColorToComponents(final Color newColor) {
        setColor(newColor);
        this.colorSelect.setColor(newColor);
        getPreviewButtonColor(newColor.getCSS());
        createDynamicStyleForComponents(this.tagName, this.tagDesc, newColor.getCSS());
    }

    @Override
    public void addColorChangeListener(final ColorChangeListener listener) {
    }

    @Override
    public void removeColorChangeListener(final ColorChangeListener listener) {
    }

}

/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import org.eclipse.hawkbit.ui.common.CoordinatesToColor;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.google.common.primitives.Doubles;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.ColorPicker;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Slider;
import com.vaadin.ui.components.colorpicker.ColorPickerGradient;
import com.vaadin.ui.components.colorpicker.ColorPickerPreview;

/**
 * Defines the Layout for the Tag/Type ColorPicker
 */
public class ColorPickerComponent extends CustomField<Color> {
    private static final long serialVersionUID = 1L;

    private static final int RGB_START = 0;
    private static final int RGB_END = 255;

    private final CustomColorPicker colorPickerBtn;

    private final Slider redSlider;
    private final Slider greenSlider;
    private final Slider blueSlider;

    private final ColorPickerPreview preview;
    private final ColorPickerGradient gradient;

    private final GridLayout layout;

    private Color selectedColor;
    private boolean isColorUpdateInProgress;

    /**
     * Constructor for ColorPickerComponent
     *
     * @param coloPickerBtnId
     *            Id of colour picker button
     * @param coloPickerBtnCaption
     *            Caption for colour picker button
     */
    public ColorPickerComponent(final String coloPickerBtnId, final String coloPickerBtnCaption) {
        this.colorPickerBtn = new CustomColorPicker(coloPickerBtnId, coloPickerBtnCaption);

        this.redSlider = createRGBSlider("red");
        this.greenSlider = createRGBSlider("green");
        this.blueSlider = createRGBSlider("blue");

        this.preview = new ColorPickerPreview(Color.WHITE);
        this.gradient = new ColorPickerGradient("rgb-gradient", new CoordinatesToColor());

        this.layout = new GridLayout(2, 4);

        init();
        addValueChangeListeners();
    }

    /**
     * Custom colour picker
     */
    public class CustomColorPicker extends ColorPicker {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for CustomColorPicker
         *
         * @param id
         *            Id
         * @param caption
         *            Caption
         */
        public CustomColorPicker(final String id, final String caption) {
            super();

            setId(id);
            setCaption(caption);
        }

        @Override
        protected void showPopup(final boolean open) {
            layout.setVisible(open);
        }
    }

    private static Slider createRGBSlider(final String styleName) {
        final Slider slider = new Slider(RGB_START, RGB_END);

        slider.setWidth("150px");
        slider.addStyleName(styleName);

        return slider;
    }

    private void init() {
        gradient.setWidth("220px");

        layout.setId(UIComponentIdProvider.COLOR_PICKER_LAYOUT);
        layout.setStyleName("rgb-vertical-layout");
        layout.setVisible(false);

        layout.addComponent(redSlider, 0, 1);
        layout.addComponent(greenSlider, 0, 2);
        layout.addComponent(blueSlider, 0, 3);

        layout.addComponent(preview, 1, 0);
        layout.addComponent(gradient, 1, 1, 1, 3);
    }

    private void addValueChangeListeners() {
        redSlider.addValueChangeListener(event -> colorUpdated(
                new Color(event.getValue().intValue(), selectedColor.getGreen(), selectedColor.getBlue())));
        greenSlider.addValueChangeListener(event -> colorUpdated(
                new Color(selectedColor.getRed(), event.getValue().intValue(), selectedColor.getBlue())));
        blueSlider.addValueChangeListener(event -> colorUpdated(
                new Color(selectedColor.getRed(), selectedColor.getGreen(), event.getValue().intValue())));

        preview.addValueChangeListener(event -> colorUpdated(event.getValue()));
        gradient.addValueChangeListener(event -> colorUpdated(event.getValue()));
    }

    private void colorUpdated(final Color newColor) {
        if (!isColorUpdateInProgress) {
            setValue(newColor);
        }
    }

    @Override
    public Color getValue() {
        return selectedColor;
    }

    @Override
    protected Component initContent() {
        return layout;
    }

    @Override
    protected void doSetValue(final Color value) {
        isColorUpdateInProgress = true;

        selectedColor = value;

        colorPickerBtn.setValue(selectedColor);

        redSlider.setValue(sanitizeSliderRGBValue(selectedColor.getRed()));
        greenSlider.setValue(sanitizeSliderRGBValue(selectedColor.getGreen()));
        blueSlider.setValue(sanitizeSliderRGBValue(selectedColor.getBlue()));

        preview.setValue(selectedColor);
        gradient.setValue(selectedColor);

        isColorUpdateInProgress = false;
    }

    private static double sanitizeSliderRGBValue(final int colorValue) {
        return Doubles.constrainToRange(colorValue, RGB_START, RGB_END);
    }

    /**
     * @return Colour picker button
     */
    public CustomColorPicker getColorPickerBtn() {
        return colorPickerBtn;
    }
}

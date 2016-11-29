/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.colorpicker;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.hawkbit.ui.common.CoordinatesToColor;
import org.eclipse.hawkbit.ui.management.tag.SpColorPickerPreview;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.AbstractColorPicker.Coordinates2Color;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Slider;
import com.vaadin.ui.components.colorpicker.ColorPickerGradient;
import com.vaadin.ui.components.colorpicker.ColorSelector;

/**
 * 
 * Defines the Layout for the ColorPicker
 *
 */
public class ColorPickerLayout extends GridLayout {

    private static final long serialVersionUID = -7025970080613796692L;

    private SpColorPickerPreview selPreview;

    private ColorPickerGradient colorSelect;
    private Set<ColorSelector> selectors;
    private Color selectedColor;

    /** RGB color converter. */
    private final Coordinates2Color rgbConverter = new CoordinatesToColor();

    private Slider redSlider;
    private Slider greenSlider;
    private Slider blueSlider;

    public ColorPickerLayout() {

        setColumns(2);
        setRows(4);
        setId(UIComponentIdProvider.COLOR_PICKER_LAYOUT);

        init();

        setStyleName("rgb-vertical-layout");

        addComponent(redSlider, 0, 1);
        addComponent(greenSlider, 0, 2);
        addComponent(blueSlider, 0, 3);

        addComponent(selPreview, 1, 0);
        addComponent(colorSelect, 1, 1, 1, 3);
    }

    private void init() {

        selectors = new HashSet<>();
        selectedColor = getDefaultColor();
        selPreview = new SpColorPickerPreview(selectedColor);

        colorSelect = new ColorPickerGradient("rgb-gradient", rgbConverter);
        colorSelect.setColor(selectedColor);
        colorSelect.setWidth("220px");

        redSlider = createRGBSlider("", "red");
        redSlider.setId(UIComponentIdProvider.COLOR_PICKER_RED_SLIDER);
        greenSlider = createRGBSlider("", "green");
        blueSlider = createRGBSlider("", "blue");

        selectors.add(colorSelect);
    }

    private Slider createRGBSlider(final String caption, final String styleName) {
        final Slider slider = new Slider(caption, 0, 255);
        slider.setImmediate(true);
        slider.setWidth("150px");
        slider.addStyleName(styleName);
        return slider;
    }

    public SpColorPickerPreview getSelPreview() {
        return selPreview;
    }

    public void setSelPreview(final SpColorPickerPreview selPreview) {
        this.selPreview = selPreview;
    }

    public ColorPickerGradient getColorSelect() {
        return colorSelect;
    }

    public void setColorSelect(final ColorPickerGradient colorSelect) {
        this.colorSelect = colorSelect;
    }

    public Set<ColorSelector> getSelectors() {
        return selectors;
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(final Color selectedColor) {
        this.selectedColor = selectedColor;
    }

    public Coordinates2Color getRgbConverter() {
        return rgbConverter;
    }

    public Color getDefaultColor() {
        return new Color(44, 151, 32);
    }

    public Slider getRedSlider() {
        return redSlider;
    }

    public void setRedSlider(final Slider redSlider) {
        this.redSlider = redSlider;
    }

    public Slider getGreenSlider() {
        return greenSlider;
    }

    public void setGreenSlider(final Slider greenSlider) {
        this.greenSlider = greenSlider;
    }

    public Slider getBlueSlider() {
        return blueSlider;
    }

    public void setBlueSlider(final Slider blueSlider) {
        this.blueSlider = blueSlider;
    }

}

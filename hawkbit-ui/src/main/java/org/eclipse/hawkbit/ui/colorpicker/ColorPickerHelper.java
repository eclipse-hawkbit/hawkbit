/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.colorpicker;

import org.eclipse.hawkbit.ui.management.tag.SpColorPickerPreview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;

/**
 * Contains helper methods for the ColorPickerLayout to handle the ColorPicker
 *
 */
public final class ColorPickerHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ColorPickerHelper.class);

    private ColorPickerHelper() {

    }

    /**
     * Get color picked value as string.
     * 
     * @param preview
     *            the color picker preview
     * @return String of color picked value.
     */
    public static String getColorPickedString(final SpColorPickerPreview preview) {

        final Color color = preview.getColor();
        return "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
    }

    /**
     * Covert RGB code to {@Color}.
     *
     * @param value
     *            RGB vale
     * @return Color
     */
    public static Color rgbToColorConverter(final String value) {

        if (StringUtils.isEmpty(value) || (!StringUtils.isEmpty(value) && !value.startsWith("rgb"))) {
            throw new IllegalArgumentException(
                    "String to convert is empty or of invalid format - value: '" + value + "'");
        }

        // RGB color format rgb/rgba(255,255,255,0.1)
        final String[] colors = value.substring(value.indexOf('(') + 1, value.length() - 1).split(",");
        final int red = Integer.parseInt(colors[0]);
        final int green = Integer.parseInt(colors[1]);
        final int blue = Integer.parseInt(colors[2]);
        if (colors.length > 3) {
            final int alpha = (int) (Double.parseDouble(colors[3]) * 255D);
            return new Color(red, green, blue, alpha);
        }
        return new Color(red, green, blue);
    }

    /**
     * 
     * Gets the selectedColor in the ColorPickerLayout and sets the slider
     * values
     * 
     * @param colorPickerLayout
     *            colorPickerLayout
     */
    public static void setRgbSliderValues(final ColorPickerLayout colorPickerLayout) {

        try {
            final double redColorValue = colorPickerLayout.getSelectedColor().getRed();
            colorPickerLayout.getRedSlider().setValue(Double.valueOf(redColorValue));
            final double blueColorValue = colorPickerLayout.getSelectedColor().getBlue();
            colorPickerLayout.getBlueSlider().setValue(Double.valueOf(blueColorValue));
            final double greenColorValue = colorPickerLayout.getSelectedColor().getGreen();
            colorPickerLayout.getGreenSlider().setValue(Double.valueOf(greenColorValue));
        } catch (final ValueOutOfBoundsException e) {
            LOG.error("Unable to set RGB color value to " + colorPickerLayout.getSelectedColor().getRed() + ","
                    + colorPickerLayout.getSelectedColor().getGreen() + ","
                    + colorPickerLayout.getSelectedColor().getBlue(), e);
        }
    }

}

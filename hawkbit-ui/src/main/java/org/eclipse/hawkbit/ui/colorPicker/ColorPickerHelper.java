package org.eclipse.hawkbit.ui.colorPicker;

import org.eclipse.hawkbit.ui.management.tag.SpColorPickerPreview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;

public class ColorPickerHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ColorPickerHelper.class);

    /**
     * Get color picked value in string.
     *
     * @return String of color picked value.
     */
    public static String getColorPickedString(final SpColorPickerPreview preview) {
        return "rgb(" + preview.getColor().getRed() + "," + preview.getColor().getGreen() + ","
                + preview.getColor().getBlue() + ")";
    }

    /**
     * Covert RGB code to {@Color}.
     *
     * @param value
     *            RGB vale
     * @return Color
     */
    public static Color rgbToColorConverter(final String value) {
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

    public static void setRgbSliderValues(final ColorPickerLayout colorPickerLayout) {
        try {
            final double redColorValue = colorPickerLayout.getSelectedColor().getRed();
            colorPickerLayout.getRedSlider().setValue(new Double(redColorValue));
            final double blueColorValue = colorPickerLayout.getSelectedColor().getBlue();
            colorPickerLayout.getBlueSlider().setValue(new Double(blueColorValue));
            final double greenColorValue = colorPickerLayout.getSelectedColor().getGreen();
            colorPickerLayout.getGreenSlider().setValue(new Double(greenColorValue));
        } catch (final ValueOutOfBoundsException e) {
            LOG.error("Unable to set RGB color value to " + colorPickerLayout.getSelectedColor().getRed() + ","
                    + colorPickerLayout.getSelectedColor().getGreen() + ","
                    + colorPickerLayout.getSelectedColor().getBlue(), e);
        }
    }

}

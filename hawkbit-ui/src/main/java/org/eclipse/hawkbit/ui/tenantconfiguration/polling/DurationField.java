package org.eclipse.hawkbit.ui.tenantconfiguration.polling;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.vaadin.data.Property;
import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.DateField;

/**
 * This class represents a Field which is optimized to enter a time duration in
 * form HH:mm:ss (see {@link #DEFAULT_DURATION_FORMAT}). It uses the vaadin
 * DateField as a basic element, but the format is optimized for the duration
 * input. For a correct view of the popup it is recommended not to display the
 * css-class "v-datefield-calendarpanel-header" and
 * "v-datefield-calendarpanel-body" (see systemconfig.scss}
 */
public class DurationField extends DateField {

    private static final long serialVersionUID = 1L;

    private static String CSS_STYLE_NAME = "durationfield";

    private static String DEFAULT_DURATION_FORMAT = "HH:mm:ss";
    private static String ADDITIONAL_DURATION_FORMAT = "HHmmss";

    private SimpleDateFormat default_format = new SimpleDateFormat(DEFAULT_DURATION_FORMAT);
    private SimpleDateFormat additional_format = new SimpleDateFormat(ADDITIONAL_DURATION_FORMAT);

    /**
     * Creates a DurationField
     */
    public DurationField() {

        default_format.setLenient(false);
        additional_format.setLenient(false);

        this.setResolution(Resolution.SECOND);
        this.setDateFormat(DEFAULT_DURATION_FORMAT);
        this.addStyleName(CSS_STYLE_NAME);

        // needed that popup shows a 24h clock
        this.setLocale(Locale.GERMANY);
        // adds empty change Listener, but is needed that field reacts on
        // pressed enter
        this.addValueChangeListener(this);
    }

    @Override
    protected Date handleUnparsableDateString(String value) throws ConversionException {

        try {
            return default_format.parse(value);

        } catch (ParseException e1) {
            try {
                return additional_format.parse(value);
            } catch (ParseException e2) {
                // if Parsing is not possible ConversionException is thrown
            }
        }
        throw new ConversionException("input is not in HH:MM:SS format.");
    }

    /**
     * Sets the duration value as a String
     * 
     * @param duration
     *            duration as String in format HH:mm:ss, only values <= 23:59:59
     *            are excepted
     * @throws ParseException
     *             Exception is thrown, when String parameter is in wrong
     *             format.
     */
    public void setValueAsString(String duration) throws ParseException {
        super.setValue(default_format.parse(duration));
    }

    /**
     * Gets the duration value as a formated String
     * 
     * @return duration as String in format HH:mm:ss
     */
    public String getValueAsString() {
        return default_format.format(super.getValue());
    }

    @Override
    public void valueChange(Property.ValueChangeEvent event) {
        // does nothing, but method overrides super methods and is needed that
        // parsing works correctly on pressed enter key
    }
}

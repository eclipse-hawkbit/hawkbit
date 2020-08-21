/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.polling;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Result;
import com.vaadin.shared.ui.datefield.DateTimeResolution;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.themes.ValoTheme;

/**
 * This class represents a Field which is optimized to enter a time duration in
 * form HH:mm:ss (see {@link #DURATION_FORMAT_STIRNG}). It uses the vaadin
 * DateField as a basic element, but the format is optimized for the duration
 * input. For a correct view of the popup it is recommended not to display the
 * css-class "v-datefield-calendarpanel-header" and
 * "v-datefield-calendarpanel-body" (see systemconfig.scss}
 */
public class DurationField extends DateTimeField {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DurationField.class);

    private static final String CSS_STYLE_NAME = "durationfield";
    private VaadinMessageSource i18n;
    private static final String ADDITIONAL_DURATION_STRING = "HHmmss";
    private static final String DURATION_FORMAT_STIRNG = "HH:mm:ss";

    private static final Duration MAXIMUM_DURATION = Duration.ofHours(23).plusMinutes(59).plusSeconds(59);

    private transient LocalDateTime minimumDuration;
    private transient LocalDateTime maximumDuration;

    /**
     * Creates a DurationField
     */
    protected DurationField() {
        this.setResolution(DateTimeResolution.SECOND);
        this.setDateFormat(DURATION_FORMAT_STIRNG);
        this.addStyleName(CSS_STYLE_NAME);
        this.addStyleName(ValoTheme.TEXTFIELD_TINY);
        this.setWidth("100px");

        // needed that popup shows a 24h clock
        this.setLocale(Locale.GERMANY);
    }

    /**
     * Sets the message source
     *
     * @param i18n
     *            VaadinMessageSource
     */
    public void setI18n(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    /**
     * This method is called to handle a non-empty date string from the client
     * if the client could not parse it as a Date. In the current case two
     * different parsing schemas are tried. If parsing is not possible a
     * ConversionException is thrown which marks the DurationField as invalid.
     *
     * @return the Result of the specified date
     */
    @Override
    protected Result<LocalDateTime> handleUnparsableDateString(final String value) {
        try {
            return Result.ok(LocalTime.parse(value, DateTimeFormatter.ofPattern(DURATION_FORMAT_STIRNG))
                    .atDate(LocalDate.of(1, 1, 1)));
        } catch (final DateTimeParseException e) {
            LOG.trace("Parsing date with format {} failed in UI: {}", DURATION_FORMAT_STIRNG, e.getMessage());
            try {
                final String adaptedValue = "000000".substring(Math.min(value.length(), 6)) + value;
                final LocalTime parsedTime = LocalTime.parse(adaptedValue,
                        DateTimeFormatter.ofPattern(ADDITIONAL_DURATION_STRING));
                return Result.ok(parsedTime.atDate(LocalDate.of(1, 1, 1)));
            } catch (final DateTimeParseException ex) {
                LOG.trace("Parsing date with format {} failed in UI: {}", ADDITIONAL_DURATION_STRING, ex.getMessage());
                return Result.error(i18n.getMessage("configuration.datetime.format.invalid"));
            }
        }
    }

    @Override
    protected void doSetValue(final LocalDateTime value) {
        final LocalDateTime sanitizedValue = sanitizeValue(value);
        super.doSetValue(sanitizedValue);
    }

    /**
     * Sanitize the input date time value
     *
     * @param value
     *            Input date time value
     *
     * @return Validated date time value within range of minimum and maximum
     *         duration
     */
    public LocalDateTime sanitizeValue(final LocalDateTime value) {
        if (value == null && minimumDuration != null) {
            return minimumDuration;
        }
        if (value != null && minimumDuration != null && maximumDuration != null
                && minimumDuration.isBefore(maximumDuration)) {

            if (compareTimeOfDates(value, maximumDuration) > 0) {
                return maximumDuration;
            }

            if (compareTimeOfDates(minimumDuration, value) > 0) {
                return minimumDuration;
            }
        }

        return value;
    }

    /**
     * Because parsing done by base class returns a different date than parsing
     * done by the user or converting duration to a date. But for the
     * DurationField comparison only the time is important. This function helps
     * comparing the time and ignores the values for day, month and year.
     *
     * @param d1
     *            date, which time will compared with the time of d2
     * @param d2
     *            date, which time will compared with the time of d1
     *
     * @return the value 0 if the time represented d1 is equal to the time
     *         represented by d2; a value less than 0 if the time of d1 is
     *         before the time of d2; and a value greater than 0 if the time of
     *         d1 is after the time represented by d2.
     */
    private static int compareTimeOfDates(final LocalDateTime d1, final LocalDateTime d2) {
        return d1.toLocalTime().compareTo(d2.toLocalTime());
    }

    /**
     * Sets the duration value
     *
     * @param duration
     *            duration, only values less then 23:59:59 are excepted
     */

    public void setDuration(@NotNull final Duration duration) {
        if (duration.compareTo(MAXIMUM_DURATION) > 0) {
            throw new IllegalArgumentException("The duaration has to be smaller than 23:59:59.");
        }
        super.setValue(durationToLocalDateTime(duration));
    }

    /**
     * Sets the minimal allowed duration value as a String
     *
     * @param minimumDuration
     *            minimum Duration, only values smaller 23:59:59 are excepted
     */
    public void setMinimumDuration(@NotNull final Duration minimumDuration) {
        if (minimumDuration.compareTo(MAXIMUM_DURATION) > 0) {
            throw new IllegalArgumentException("The minimum duaration has to be smaller than 23:59:59.");
        }
        this.minimumDuration = durationToLocalDateTime(minimumDuration);
    }

    /**
     * Sets the maximum allowed duration value as a String
     *
     * @param maximumDuration
     *            maximumDuration, only values smaller 23:59:59 are excepted
     */
    public void setMaximumDuration(@NotNull final Duration maximumDuration) {
        if (maximumDuration.compareTo(MAXIMUM_DURATION) > 0) {
            throw new IllegalArgumentException("The maximum duaration has to be smaller than 23:59:59.");
        }
        this.maximumDuration = durationToLocalDateTime(maximumDuration);
    }

    private static LocalDateTime durationToLocalDateTime(final Duration duration) {
        if (duration.compareTo(MAXIMUM_DURATION) > 0) {
            throw new IllegalArgumentException("The duaration has to be smaller than 23:59:59.");
        }
        return LocalTime.ofNanoOfDay(duration.toNanos()).atDate(LocalDate.of(1, 1, 1));
    }
}

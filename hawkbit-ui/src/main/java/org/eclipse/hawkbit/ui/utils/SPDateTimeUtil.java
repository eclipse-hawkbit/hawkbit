/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.eclipse.hawkbit.repository.model.BaseEntity;

import com.google.common.collect.Maps;
import com.vaadin.server.WebBrowser;

/**
 * Common Util to get date/time related information.
 */
public final class SPDateTimeUtil {

    private static final String DURATION_FORMAT = "y','M','d','H','m','s";
    private static final Map<Integer, CalendarI18N> DURATION_I18N = Maps.newHashMapWithExpectedSize(6);

    static {
        DURATION_I18N.put(0, CalendarI18N.YEAR);
        DURATION_I18N.put(1, CalendarI18N.MONTH);
        DURATION_I18N.put(2, CalendarI18N.DAY);
        DURATION_I18N.put(3, CalendarI18N.HOUR);
        DURATION_I18N.put(4, CalendarI18N.MINUTE);
        DURATION_I18N.put(5, CalendarI18N.SECOND);
    }

    private SPDateTimeUtil() {

    }

    /**
     * Get browser time zone.
     *
     * @return TimeZone
     */
    public static TimeZone getBrowserTimeZone() {
        final WebBrowser webBrowser = com.vaadin.server.Page.getCurrent().getWebBrowser();
        final String[] timeZones = TimeZone.getAvailableIDs(webBrowser.getRawTimezoneOffset());
        TimeZone tz = TimeZone.getDefault();
        for (final String string : timeZones) {
            final TimeZone t = TimeZone.getTimeZone(string);
            if (t.getRawOffset() == webBrowser.getRawTimezoneOffset()) {
                tz = t;
            }
        }
        return tz;
    }

    /**
     * Get time zone id .ZoneId.SHORT_IDS used get id if time zone is
     * abbreviated like 'IST'.
     *
     * @param tz
     * @return ZoneId
     */
    public static ZoneId getTimeZoneId(final TimeZone tz) {
        return ZoneId.of(tz.getID(), ZoneId.SHORT_IDS);
    }

    /**
     * Get formatted date with browser time zone.
     *
     * @param lastQueryDate
     * @return String formatted date
     */
    public static String getFormattedDate(final Long lastQueryDate) {
        return formatDate(lastQueryDate, null);
    }

    /**
     * Get formatted date with browser time zone.
     *
     * @param lastQueryDate
     * @param datePattern
     *            pattern how to format the date (cp. {@code SimpleDateFormat})
     * @return String formatted date
     */
    public static String getFormattedDate(final Long lastQueryDate, final String datePattern) {
        return formatDate(lastQueryDate, null, datePattern);
    }

    /**
     * Get formatted date 'created at' by entity.
     *
     * @param baseEntity
     *            the entity
     * @return String formatted date
     */
    public static String formatCreatedAt(final BaseEntity baseEntity) {
        if (baseEntity == null) {
            return "";
        }
        return formatDate(baseEntity.getCreatedAt(), "");
    }

    /**
     * Get formatted date 'last modified at' by entity.
     *
     * @param baseEntity
     *            the entity
     * @return String formatted date
     */
    public static String formatLastModifiedAt(final BaseEntity baseEntity) {
        if (baseEntity == null) {
            return "";
        }
        return formatDate(baseEntity.getLastModifiedAt(), "");
    }

    /**
     * Get formatted date 'last modified at' by entity.
     *
     * @param baseEntity
     *            the entity
     * @param datePattern
     *            pattern how to format the date (cp. {@code SimpleDateFormat})
     * @return String formatted date
     */
    public static String formatLastModifiedAt(final BaseEntity baseEntity, final String datePattern) {
        if (baseEntity == null) {
            return "";
        }
        return formatDate(baseEntity.getLastModifiedAt(), "", datePattern);
    }

    private static String formatDate(final Long lastQueryDate, final String defaultString, final String datePattern) {
        if (lastQueryDate != null) {
            final SimpleDateFormat format = new SimpleDateFormat(datePattern);
            format.setTimeZone(getBrowserTimeZone());
            return format.format(new Date(lastQueryDate));
        }
        return defaultString;
    }

    private static String formatDate(final Long lastQueryDate, final String defaultString) {
        return formatDate(lastQueryDate, defaultString, SPUIDefinitions.LAST_QUERY_DATE_FORMAT);
    }

    /**
     * Creates a formatted string of a duration in format '1 year 2 months 3
     * days 4 hours 5 minutes 6 seconds' zero values will be ignored in the
     * formatted string.
     *
     * @param startMillis
     *            the start milliseconds of the duration
     * @param endMillis
     *            the end milliseconds of the duration
     * @param i18N
     *            the i18n service to determine the correct string for e.g.
     *            'year'
     * @return a formatted string for duration label
     */
    public static String getDurationFormattedString(final long startMillis, final long endMillis,
            final VaadinMessageSource i18N) {
        final String formatDuration = DurationFormatUtils.formatPeriod(startMillis, endMillis, DURATION_FORMAT, false,
                getBrowserTimeZone());

        final StringBuilder formattedDuration = new StringBuilder();
        final String[] split = formatDuration.split(",");

        for (int index = 0; index < split.length; index++) {
            if (index != 0 && formattedDuration.length() > 0) {
                formattedDuration.append(' ');
            }
            final int value = Integer.parseInt(split[index]);
            if (value != 0) {
                final String suffix = (value == 1) ? i18N.getMessage(DURATION_I18N.get(index).getSingle())
                        : i18N.getMessage(DURATION_I18N.get(index).getPlural());
                formattedDuration.append(value).append(' ').append(suffix);
            }

        }
        return formattedDuration.toString();

    }

    /**
     * Enum to get the i18n key for single or plural calendar labels.
     *
     *
     *
     *
     */
    private enum CalendarI18N {
        /**
         *
         */
        YEAR("calendar.year", "calendar.years"),
        /**
         *
         */
        MONTH("calendar.month", "calendar.months"),
        /**
         *
         */
        DAY("calendar.days", "calendar.days"),
        /**
         *
         */
        HOUR("calendar.hour", "calendar.hours"),
        /**
         *
         */
        MINUTE("calendar.minute", "calendar.minutes"),
        /**
         *
         */
        SECOND("calendar.second", "calendar.seconds");

        private final String single;
        private final String plural;

        /**
         *
         */
        CalendarI18N(final String single, final String plural) {
            this.single = single;
            this.plural = plural;
        }

        /**
         * @return the single
         */
        public String getSingle() {
            return single;
        }

        /**
         * @return the plural
         */
        public String getPlural() {
            return plural;
        }
    }
}

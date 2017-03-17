/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.report.model.DataReportSeries;
import org.eclipse.hawkbit.repository.report.model.InnerOuterDataReportSeries;
import org.eclipse.hawkbit.repository.report.model.ListReportSeries;
import org.eclipse.hawkbit.repository.report.model.SeriesTime;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service layer for generating hawkBit statistics and reports.
 *
 */
public interface ReportManagement {

    /**
     * Data base format.
     *
     *
     *
     * @param <T>
     */
    public interface DateType<T> {
        /**
         * @param s
         * @return T
         */
        T format(String s);

        /**
         * h2 format.
         *
         * @return String
         */
        String h2Format();

        /**
         * mysql format.
         *
         * @return String
         */
        String mySqlFormat();
    }

    /**
     * Return DateTypes.
     */
    public static final class DateTypes implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final PerMonth PER_MONTH = new PerMonth();

        private DateTypes() {
            // Utility class
        }

        /**
         * @return PerMonth
         */
        public static PerMonth perMonth() {
            return PER_MONTH;
        }

    }

    /**
     * Gives the date format based on DB H2 or mySql.
     *
     *
     *
     */
    public static final class PerMonth implements DateType<LocalDate>, Serializable {
        private static final long serialVersionUID = 1L;
        private static final String DATE_PATTERN = "yyyy-MM";

        @Override
        public LocalDate format(final String s) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            final YearMonth ym = YearMonth.parse(s, formatter);
            return ym.atDay(1);
        }

        @Override
        public String h2Format() {
            return DATE_PATTERN;
        }

        @Override
        public String mySqlFormat() {
            return "%Y-%m";
        }

    }

    /**
     * Generates a report of the top x distribution set assigned usage as a list
     * of {@link InnerOuterDataReportSeries} which is ideal for generate a donut
     * chart out of it. The inner series contains the distribution set names and
     * total count usage. The outer series contains each version usage and its
     * usage count. {@code inner: ds1:5 -> outer: vers 0.0.0:3, vers 1.0.0:2}
     * {@code inner: ds2:1 -> outer: vers 0.0.1:1}
     *
     * The top x entries are seperated within the series, the rest of the
     * distribution sets usage are summarized to a "misc" series.
     *
     * @param topXEntries
     *            the top entries which should be shown, the rest distribution
     *            set entries are summarized as "misc"
     * @return a list of inner and outer series of distribution set usage
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<InnerOuterDataReportSeries<String>> distributionUsageAssigned(int topXEntries);

    /**
     * Generates a report of the top x distribution set installed usage as a
     * list of {@link InnerOuterDataReportSeries} which is ideal for generate a
     * donut chart out of it. The inner series contains the distribution set
     * names and total count usage. The outer series contains each version usage
     * and its usage count.
     * {@code inner: ds1:5 -> outer: vers 0.0.0:3, vers 1.0.0:2}
     * {@code inner: ds2:1 -> outer: vers 0.0.1:1}
     *
     * The top x entries are seperated within the series, the rest of the
     * distribution sets usage are summarized to a "misc" series.
     *
     * @param topXEntries
     *            the top entries which should be shown, the rest distribution
     *            set entries are summarized as "misc"
     * @return a list of inner and outer series of distribution set usage
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<InnerOuterDataReportSeries<String>> distributionUsageInstalled(int topXEntries);

    /**
     * Generates report for feedback over period.
     *
     * @param dateType
     *            {@link PerMonth}
     * @param from
     *            start date
     * @param to
     *            end date
     * @return <T> DataReportSeries<T> ListReportSeries list of action status
     *         count
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    <T extends Serializable> DataReportSeries<T> feedbackReceivedOverTime(@NotNull DateType<T> dateType,
            @NotNull LocalDateTime from, @NotNull LocalDateTime to);

    /**
     * Generates report for target created over period.
     *
     * @param dateType
     *            {@link PerMonth}
     * @param from
     *            start date
     * @param to
     *            end date
     * @return <T> DataReportSeries<T> ListReportSeries list of target created
     *         count
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    <T extends Serializable> DataReportSeries<T> targetsCreatedOverPeriod(@NotNull DateType<T> dateType,
            @NotNull LocalDateTime from, @NotNull LocalDateTime to);

    /**
     * Generates a report as a {@link ListReportSeries} targets polled based on
     * the {@link Target#getLastTargetQuery()} within an hour, day, week, month,
     * year, more than a year, never.
     *
     * The order of the numbers within the {@link DataReportSeries} is the order
     * hour, day, week, month, year, more than a year, never.
     *
     * @return a {@link DataReportSeries} which contains the number of targets
     *         which have not been polled in the last hour, day, ... year,more
     *         than a year, never.
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DataReportSeries<SeriesTime> targetsLastPoll();

    /**
     * Generates a report of all targets of their current update status count.
     * For each {@link TargetUpdateStatus} an total count of targets which are
     * in this status currently.
     *
     * @return a data report series which contains the target count for each
     *         target update status
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DataReportSeries<TargetUpdateStatus> targetStatus();

}

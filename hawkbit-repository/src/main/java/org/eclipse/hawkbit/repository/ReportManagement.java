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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.report.model.DataReportSeries;
import org.eclipse.hawkbit.report.model.DataReportSeriesItem;
import org.eclipse.hawkbit.report.model.InnerOuterDataReportSeries;
import org.eclipse.hawkbit.report.model.ListReportSeries;
import org.eclipse.hawkbit.report.model.SeriesTime;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSet_;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetInfo_;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Target_;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Service layer for generating SP reportings.
 *
 *
 *
 *
 */
@Transactional(readOnly = true)
@Validated
@Service
@ConfigurationProperties
public class ReportManagement {

    @Value("${spring.jpa.database}")
    private String databaseType;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final String H2_TARGET_CREATED_SQL_TEMPLATE = "SELECT TO_CHAR( DATEADD('second', target0_.created_at / 1000, DATE '1970-01-01'), '%s') AS col_0_0_, count(target0_.controller_id) AS col_1_0_ from sp_target target0_ WHERE TO_CHAR(DATEADD('second', target0_.created_at / 1000, DATE '1970-01-01'),'%s') BETWEEN TO_CHAR('%s', '%s') and TO_CHAR('%s', '%s') AND UPPER(target0_.tenant)=UPPER('%s') GROUP BY TO_CHAR(DATEADD('second', target0_.created_at / 1000, DATE '1970-01-01'), '%s')";

    private static final String H2_CONTROLLER_FRRDBACK_SQL_TEMPLATE = "SELECT TO_CHAR(DATEADD('second', action_.created_at / 1000, DATE '1970-01-01'), '%s') AS col_0_0_, count(action_.id) AS col_1_0_ FROM sp_action action_ WHERE TO_CHAR(DATEADD('second', action_.created_at / 1000, DATE '1970-01-01'), '%s') BETWEEN TO_CHAR('%s', '%s') AND  TO_CHAR('%s', '%s') AND UPPER(action_.tenant)=UPPER('%s') GROUP BY TO_CHAR(DATEADD('second', action_.created_at / 1000, DATE '1970-01-01'), '%s')";

    private static final String MYSQL_TARGET_CREATED_SQL_TEMPLATE = "SELECT DATE_FORMAT(FROM_UNIXTIME(target0_.created_at / 1000), '%s') AS col_0_0_, COUNT(target0_.controller_id) AS col_1_0_ FROM sp_target target0_ WHERE DATE_FORMAT(FROM_UNIXTIME(target0_.created_at / 1000),'%s') BETWEEN DATE_FORMAT('%s', '%s') AND DATE_FORMAT('%s', '%s') AND UPPER(target0_.tenant)=UPPER('%s') GROUP BY DATE_FORMAT(FROM_UNIXTIME(target0_.created_at / 1000), '%s')";

    private static final String MYSQL_CONTROLLER_FRRDBACK_SQL_TEMPLATE = "SELECT DATE_FORMAT(FROM_UNIXTIME(action_.created_at / 1000), '%s') AS col_0_0_, COUNT(action_.id) as col_1_0_ FROM sp_action action_ WHERE DATE_FORMAT(FROM_UNIXTIME(action_.created_at / 1000),'%s') BETWEEN DATE_FORMAT('%s', '%s') AND DATE_FORMAT('%s', '%s') AND UPPER(action_.tenant)=UPPER('%s') GROUP BY DATE_FORMAT(FROM_UNIXTIME(action_.created_at / 1000), '%s')";

    private static final String MYSQL_DB_TYPE = "MYSQL";

    private static final String H2_DB_TYPE = "H2";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TenantAware tenantAware;

    /**
     * Generates a report of all targets of their current update status count.
     * For each {@link TargetUpdateStatus} an total count of targets which are
     * in this status currently.
     *
     * @return a data report series which contains the target count for each
     *         target update status
     */
    @Cacheable("targetStatus")
    public DataReportSeries<TargetUpdateStatus> targetStatus() {

        final CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final Root<Target> targetRoot = query.from(Target.class);
        final Join<Target, TargetInfo> targetInfo = targetRoot.join(Target_.targetInfo);
        final Expression<Long> countColumn = cb.count(targetInfo.get(TargetInfo_.targetId));
        final CriteriaQuery<Object[]> multiselect = query
                .multiselect(targetInfo.get(TargetInfo_.updateStatus), countColumn)
                .groupBy(targetInfo.get(TargetInfo_.updateStatus))
                .orderBy(cb.desc(targetInfo.get(TargetInfo_.updateStatus)));

        // | col1 | col2 |
        // | U_STATUS | COUNT |
        final List<Object[]> resultList = this.entityManager.createQuery(multiselect).getResultList();

        final List<DataReportSeriesItem<TargetUpdateStatus>> reportSeriesItems = resultList.stream()
                .map(r -> new DataReportSeriesItem<TargetUpdateStatus>((TargetUpdateStatus) r[0], (Long) r[1]))
                .collect(Collectors.toList());

        return new DataReportSeries<>("Target Status Overview", reportSeriesItems);
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
    @Cacheable("distributionUsageAssigned")
    public List<InnerOuterDataReportSeries<String>> distributionUsageAssigned(final int topXEntries) {

        // top X entries distribution usage
        final CriteriaBuilder cbTopX = this.entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> queryTopX = cbTopX.createQuery(Object[].class);
        final Root<DistributionSet> rootTopX = queryTopX.from(DistributionSet.class);
        final ListJoin<DistributionSet, Target> joinTopX = rootTopX.join(DistributionSet_.assignedToTargets,
                JoinType.LEFT);
        final Expression<Long> countColumn = cbTopX.count(joinTopX);
        // top x usage query
        final CriteriaQuery<Object[]> groupBy = queryTopX
                .multiselect(rootTopX.get(DistributionSet_.name), rootTopX.get(DistributionSet_.version), countColumn)
                .where(cbTopX.equal(rootTopX.get(DistributionSet_.deleted), false))
                .groupBy(rootTopX.get(DistributionSet_.name), rootTopX.get(DistributionSet_.version))
                .orderBy(cbTopX.desc(countColumn), cbTopX.asc(rootTopX.get(DistributionSet_.name)));
        // | col1 | col2 | col3 |
        // | NAME | VER | COUNT |
        final List<Object[]> resultListTop = this.entityManager.createQuery(groupBy).getResultList();
        // end of top X entries distribution usage

        return mapDistirbutionUsageResultToDataReport(topXEntries, resultListTop);
    }

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
    @Cacheable("distributionUsageInstalled")
    public List<InnerOuterDataReportSeries<String>> distributionUsageInstalled(final int topXEntries) {
        // top X entries distribution usage
        final CriteriaBuilder cbTopX = this.entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> queryTopX = cbTopX.createQuery(Object[].class);
        final Root<DistributionSet> rootTopX = queryTopX.from(DistributionSet.class);
        final ListJoin<DistributionSet, TargetInfo> joinTopX = rootTopX.join(DistributionSet_.installedAtTargets,
                JoinType.LEFT);
        final Expression<Long> countColumn = cbTopX.count(joinTopX);
        // top x usage query
        final CriteriaQuery<Object[]> groupBy = queryTopX
                .multiselect(rootTopX.get(DistributionSet_.name), rootTopX.get(DistributionSet_.version), countColumn)
                .where(cbTopX.equal(rootTopX.get(DistributionSet_.deleted), false))
                .groupBy(rootTopX.get(DistributionSet_.name), rootTopX.get(DistributionSet_.version))
                .orderBy(cbTopX.desc(countColumn), cbTopX.asc(rootTopX.get(DistributionSet_.name)));
        // | col1 | col2 | col3 |
        // | NAME | VER | COUNT |
        final List<Object[]> resultListTop = this.entityManager.createQuery(groupBy).getResultList();
        // end of top X entries distribution usage

        return mapDistirbutionUsageResultToDataReport(topXEntries, resultListTop);
    }

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
    @Cacheable("targetsCreatedOverPeriod")
    public <T extends Serializable> DataReportSeries<T> targetsCreatedOverPeriod(final DateType<T> dateType,
            final LocalDateTime from, final LocalDateTime to) {
        final Query createNativeQuery = this.entityManager
                .createNativeQuery(getTargetsCreatedQueryTemplate(dateType, from, to));
        final List<Object[]> resultList = createNativeQuery.getResultList();

        final List<DataReportSeriesItem<T>> reportItems = resultList.stream()
                .map(r -> new DataReportSeriesItem<>(dateType.format((String) r[0]), ((Number) r[1]).longValue()))
                .collect(Collectors.toList());

        return new DataReportSeries<>("CreatedTargets", reportItems);
    }

    private String getTargetsCreatedQueryTemplate(final DateType<?> dateType, final LocalDateTime from,
            final LocalDateTime to) {
        switch (this.databaseType) {
        case H2_DB_TYPE:
            return String.format(H2_TARGET_CREATED_SQL_TEMPLATE, dateTimeFormatToSqlFormat(dateType),
                    dateTimeFormatToSqlFormat(dateType), from.format(DATE_FORMAT), dateTimeFormatToSqlFormat(dateType),
                    to.format(DATE_FORMAT), dateTimeFormatToSqlFormat(dateType), this.tenantAware.getCurrentTenant(),
                    dateTimeFormatToSqlFormat(dateType));
        case MYSQL_DB_TYPE:
            return String.format(MYSQL_TARGET_CREATED_SQL_TEMPLATE, dateTimeFormatToSqlFormat(dateType),
                    dateTimeFormatToSqlFormat(dateType), from.toString(), dateTimeFormatToSqlFormat(dateType),
                    to.toString(), dateTimeFormatToSqlFormat(dateType), this.tenantAware.getCurrentTenant(),
                    dateTimeFormatToSqlFormat(dateType));
        default:
            return null;
        }
    }

    private String getFeedbackReceivedQueryTemplate(final DateType<?> dateType, final LocalDateTime from,
            final LocalDateTime to) {
        switch (this.databaseType) {
        case H2_DB_TYPE:
            return String.format(H2_CONTROLLER_FRRDBACK_SQL_TEMPLATE, dateTimeFormatToSqlFormat(dateType),
                    dateTimeFormatToSqlFormat(dateType), from.format(DATE_FORMAT), dateTimeFormatToSqlFormat(dateType),
                    to.format(DATE_FORMAT), dateTimeFormatToSqlFormat(dateType), this.tenantAware.getCurrentTenant(),
                    dateTimeFormatToSqlFormat(dateType));
        case MYSQL_DB_TYPE:
            return String.format(MYSQL_CONTROLLER_FRRDBACK_SQL_TEMPLATE, dateTimeFormatToSqlFormat(dateType),
                    dateTimeFormatToSqlFormat(dateType), from.toString(), dateTimeFormatToSqlFormat(dateType),
                    to.toString(), dateTimeFormatToSqlFormat(dateType), this.tenantAware.getCurrentTenant(),
                    dateTimeFormatToSqlFormat(dateType));
        default:
            return null;
        }
    }

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
    @Cacheable("feedbackReceivedOverTime")
    public <T extends Serializable> DataReportSeries<T> feedbackReceivedOverTime(final DateType<T> dateType,
            final LocalDateTime from, final LocalDateTime to) {
        final Query createNativeQuery = this.entityManager
                .createNativeQuery(getFeedbackReceivedQueryTemplate(dateType, from, to));
        final List<Object[]> resultList = createNativeQuery.getResultList();

        final List<DataReportSeriesItem<T>> reportItems = resultList.stream()
                .map(r -> new DataReportSeriesItem<>(dateType.format((String) r[0]), ((Number) r[1]).longValue()))
                .collect(Collectors.toList());

        return new DataReportSeries<>("FeedbackRecieved", reportItems);
    }

    /**
     * Generates a report as a {@link ListReportSeries} targets polled based on
     * the {@link TargetStatus#getLastTargetQuery()} within an hour, day, week,
     * month, year, more than a year, never.
     *
     * The order of the numbers within the {@link DataReportSeries} is the order
     * hour, day, week, month, year, more than a year, never.
     *
     * @return a {@link DataReportSeries} which contains the number of targets
     *         which have not been polled in the last hour, day, ... year,more
     *         than a year, never.
     *
     */
    @Cacheable("targetsLastPoll")
    public DataReportSeries<SeriesTime> targetsLastPoll() {

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime beforeHour = now.minusHours(1);
        final LocalDateTime beforeDay = now.minusDays(1);
        final LocalDateTime beforeWeek = now.minusWeeks(1);
        final LocalDateTime beforeMonth = now.minusMonths(1);
        final LocalDateTime beforeYear = now.minusYears(1);

        final CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        final List<DataReportSeriesItem<SeriesTime>> resultList = new ArrayList<>();

        // hours
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.HOUR, this.entityManager
                .createQuery(createCountSelectTargetsLastPoll(cb, beforeHour, now)).getSingleResult()));
        // days
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.DAY, this.entityManager
                .createQuery(createCountSelectTargetsLastPoll(cb, beforeDay, beforeHour)).getSingleResult()));
        // weeks
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.WEEK, this.entityManager
                .createQuery(createCountSelectTargetsLastPoll(cb, beforeWeek, beforeDay)).getSingleResult()));
        // months
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.MONTH, this.entityManager
                .createQuery(createCountSelectTargetsLastPoll(cb, beforeMonth, beforeWeek)).getSingleResult()));
        // years
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.YEAR, this.entityManager
                .createQuery(createCountSelectTargetsLastPoll(cb, beforeYear, beforeMonth)).getSingleResult()));
        // years
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.MORE_THAN_YEAR, this.entityManager
                .createQuery(createCountSelectTargetsLastPoll(cb, null, beforeYear)).getSingleResult()));
        // never
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.NEVER,
                this.entityManager.createQuery(createCountSelectTargetsLastPoll(cb, null, null)).getSingleResult()));

        return new DataReportSeries<>("TargetLastPoll", resultList);
    }

    private CriteriaQuery<Long> createCountSelectTargetsLastPoll(final CriteriaBuilder cb, final LocalDateTime from,
            final LocalDateTime to) {

        Long start = null;
        Long end = null;
        if (from != null) {
            start = from.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (to != null) {
            end = to.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        // count select statement
        final CriteriaQuery<Long> countSelect = cb.createQuery(Long.class);
        final Root<Target> countSelectRoot = countSelect.from(Target.class);
        final Join<Target, TargetInfo> targetInfoJoin = countSelectRoot.join(Target_.targetInfo);
        countSelect.select(cb.count(countSelectRoot));
        if (start != null && end != null) {
            countSelect.where(cb.between(targetInfoJoin.get(TargetInfo_.lastTargetQuery), start, end));
        } else if (from == null && to != null) {
            countSelect.where(cb.lessThanOrEqualTo(targetInfoJoin.get(TargetInfo_.lastTargetQuery), end));
        } else {
            countSelect.where(cb.isNull(targetInfoJoin.get(TargetInfo_.lastTargetQuery)));
        }
        return countSelect;
    }

    private List<InnerOuterDataReportSeries<String>> mapDistirbutionUsageResultToDataReport(final int topXEntries,
            final List<Object[]> resultListTop) {
        final List<InnerOuterDataReportSeries<String>> innerOuterReport = new ArrayList<>();
        final Map<DSName, InnerOuter> map = new LinkedHashMap<>();

        int topXCounter = 0;

        for (final Object[] objects : resultListTop) {

            final boolean containsInnerOuter = map.containsKey(new DSName((String) objects[0]));
            final String name = containsInnerOuter || topXCounter < topXEntries ? (String) objects[0] : null;
            final DSName dsName = new DSName(name);
            final String version = containsInnerOuter || topXCounter < topXEntries ? (String) objects[1] : null;
            final Long count = (Long) objects[2];

            InnerOuter innerouter = map.get(dsName);
            if (innerouter == null) {
                topXCounter++;
                innerouter = new InnerOuter(dsName);
                map.put(new DSName(name), innerouter);
            }
            innerouter.addOuter(new DSName(version), count);
        }

        for (final InnerOuter inner : map.values()) {
            final List<DataReportSeriesItem<String>> outerReportItems = new ArrayList<>();

            if (inner.name.getName() != null) {
                for (final InnerOuter outer : inner.outer) {
                    outerReportItems.add(outer.toItem());
                }
            } else {
                outerReportItems.add(new DataReportSeriesItem<String>("misc", inner.count));
            }

            innerOuterReport.add(new InnerOuterDataReportSeries<String>(
                    new DataReportSeries<>("DS-Name", Collections.singletonList(inner.toItem())),
                    new DataReportSeries<>("DS-Version", outerReportItems)));
        }

        return innerOuterReport;
    }

    private final class InnerOuter {
        final DSName name;
        long count;
        final List<InnerOuter> outer;

        private InnerOuter(final DSName idName) {
            this.name = idName;
            this.outer = new ArrayList<>();
        }

        private InnerOuter(final DSName idName, final long count) {
            this.name = idName;
            this.count = count;
            this.outer = new ArrayList<>();
        }

        private void addOuter(final DSName idName, final long count) {
            this.outer.add(new InnerOuter(idName, count));
            this.count += count;
        }

        private DataReportSeriesItem<String> toItem() {
            return new DataReportSeriesItem<>(this.name.getName() != null ? this.name.getName() : "misc", this.count);
        }
    }

    /**
     * Object contains the name and the id of an entity.
     *
     *
     *
     *
     */
    private static final class DSName {

        private final String name;

        /**
         * @param id
         *            the ID of an entity
         * @param name
         *            the name of an entity
         */
        private DSName(final String name) {
            this.name = name;
        }

        /**
         * @return the name
         */
        private String getName() {
            return this.name;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (this.name == null ? 0 : this.name.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object obj) { // NOSONAR - as this is
                                                  // generated
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DSName other = (DSName) obj;
            if (this.name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!this.name.equals(other.name)) {
                return false;
            }
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "DSName [name=" + this.name + "]";
        }
    }

    private String dateTimeFormatToSqlFormat(final DateType<?> datatype) {
        switch (this.databaseType) {
        case H2_DB_TYPE:
            return datatype.h2Format();
        case MYSQL_DB_TYPE:
            return datatype.mySqlFormat();
        default:
            return null;
        }
    }

    /**
     * Return DateTypes.
     *
     *
     *
     */
    public static final class DateTypes implements Serializable {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private static final PerMonth PER_MONTH = new PerMonth();

        private DateTypes() {

        }

        /**
         * @return PerMonth
         */
        public static PerMonth perMonth() {
            return PER_MONTH;
        }
    }

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
     * Gives the date format based on DB H2 or mySql.
     *
     *
     *
     */
    public static final class PerMonth implements DateType<LocalDate>, Serializable {
        private static final long serialVersionUID = 1L;
        private static final String DATE_PATTERN = "yyyy-MM";

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.hawkbit.server.repository.ReportManagement.DateType#
         * format(java. lang.String)
         */
        @Override
        public LocalDate format(final String s) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            final YearMonth ym = YearMonth.parse(s, formatter);
            return ym.atDay(1);
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.hawkbit.server.repository.ReportManagement.DateType#
         * h2Format()
         */
        @Override
        public String h2Format() {
            return DATE_PATTERN;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.hawkbit.server.repository.ReportManagement.DateType#
         * mySqlFormat( )
         */
        @Override
        public String mySqlFormat() {
            return "%Y-%m";
        }

    }
}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.io.Serializable;
import java.time.LocalDateTime;
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

import org.eclipse.hawkbit.repository.ReportManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.report.model.DataReportSeries;
import org.eclipse.hawkbit.repository.report.model.DataReportSeriesItem;
import org.eclipse.hawkbit.repository.report.model.InnerOuterDataReportSeries;
import org.eclipse.hawkbit.repository.report.model.SeriesTime;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link ReportManagement}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
@Service
public class JpaReportManagement implements ReportManagement {

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

    @Override
    @Cacheable("targetStatus")
    public DataReportSeries<TargetUpdateStatus> targetStatus() {

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);
        final Join<JpaTarget, JpaTargetInfo> targetInfo = targetRoot.join(JpaTarget_.targetInfo);
        final Expression<Long> countColumn = cb.count(targetInfo.get(JpaTargetInfo_.targetId));
        final CriteriaQuery<Object[]> multiselect = query
                .multiselect(targetInfo.get(JpaTargetInfo_.updateStatus), countColumn)
                .groupBy(targetInfo.get(JpaTargetInfo_.updateStatus))
                .orderBy(cb.desc(targetInfo.get(JpaTargetInfo_.updateStatus)));

        // | col1 | col2 |
        // | U_STATUS | COUNT |
        final List<Object[]> resultList = entityManager.createQuery(multiselect).getResultList();

        final List<DataReportSeriesItem<TargetUpdateStatus>> reportSeriesItems = resultList.stream()
                .map(r -> new DataReportSeriesItem<TargetUpdateStatus>((TargetUpdateStatus) r[0], (Long) r[1]))
                .collect(Collectors.toList());

        return new DataReportSeries<>("Target Status Overview", reportSeriesItems);
    }

    @Override
    @Cacheable("distributionUsageAssigned")
    public List<InnerOuterDataReportSeries<String>> distributionUsageAssigned(final int topXEntries) {

        // top X entries distribution usage
        final CriteriaBuilder cbTopX = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> queryTopX = cbTopX.createQuery(Object[].class);
        final Root<JpaDistributionSet> rootTopX = queryTopX.from(JpaDistributionSet.class);
        final ListJoin<JpaDistributionSet, JpaTarget> joinTopX = rootTopX.join(JpaDistributionSet_.assignedToTargets,
                JoinType.LEFT);
        final Expression<Long> countColumn = cbTopX.count(joinTopX);
        // top x usage query
        final CriteriaQuery<Object[]> groupBy = queryTopX
                .multiselect(rootTopX.get(JpaDistributionSet_.name), rootTopX.get(JpaDistributionSet_.version),
                        countColumn)
                .where(cbTopX.equal(rootTopX.get(JpaDistributionSet_.deleted), false))
                .groupBy(rootTopX.get(JpaDistributionSet_.name), rootTopX.get(JpaDistributionSet_.version))
                .orderBy(cbTopX.desc(countColumn), cbTopX.asc(rootTopX.get(JpaDistributionSet_.name)));
        // | col1 | col2 | col3 |
        // | NAME | VER | COUNT |
        final List<Object[]> resultListTop = entityManager.createQuery(groupBy).getResultList();
        // end of top X entries distribution usage

        return mapDistirbutionUsageResultToDataReport(topXEntries, resultListTop);
    }

    @Override
    @Cacheable("distributionUsageInstalled")
    public List<InnerOuterDataReportSeries<String>> distributionUsageInstalled(final int topXEntries) {
        // top X entries distribution usage
        final CriteriaBuilder cbTopX = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> queryTopX = cbTopX.createQuery(Object[].class);
        final Root<JpaDistributionSet> rootTopX = queryTopX.from(JpaDistributionSet.class);
        final ListJoin<JpaDistributionSet, JpaTargetInfo> joinTopX = rootTopX
                .join(JpaDistributionSet_.installedAtTargets, JoinType.LEFT);
        final Expression<Long> countColumn = cbTopX.count(joinTopX);
        // top x usage query
        final CriteriaQuery<Object[]> groupBy = queryTopX
                .multiselect(rootTopX.get(JpaDistributionSet_.name), rootTopX.get(JpaDistributionSet_.version),
                        countColumn)
                .where(cbTopX.equal(rootTopX.get(JpaDistributionSet_.deleted), false))
                .groupBy(rootTopX.get(JpaDistributionSet_.name), rootTopX.get(JpaDistributionSet_.version))
                .orderBy(cbTopX.desc(countColumn), cbTopX.asc(rootTopX.get(JpaDistributionSet_.name)));
        // | col1 | col2 | col3 |
        // | NAME | VER | COUNT |
        final List<Object[]> resultListTop = entityManager.createQuery(groupBy).getResultList();
        // end of top X entries distribution usage

        return mapDistirbutionUsageResultToDataReport(topXEntries, resultListTop);
    }

    @Override
    @Cacheable("targetsCreatedOverPeriod")
    public <T extends Serializable> DataReportSeries<T> targetsCreatedOverPeriod(final DateType<T> dateType,
            final LocalDateTime from, final LocalDateTime to) {
        final Query createNativeQuery = entityManager
                .createNativeQuery(getTargetsCreatedQueryTemplate(dateType, from, to));
        final List<Object[]> resultList = createNativeQuery.getResultList();

        final List<DataReportSeriesItem<T>> reportItems = resultList.stream()
                .map(r -> new DataReportSeriesItem<>(dateType.format((String) r[0]), ((Number) r[1]).longValue()))
                .collect(Collectors.toList());

        return new DataReportSeries<>("CreatedTargets", reportItems);
    }

    private String getTargetsCreatedQueryTemplate(final DateType<?> dateType, final LocalDateTime from,
            final LocalDateTime to) {
        switch (databaseType) {
        case H2_DB_TYPE:
            return String.format(H2_TARGET_CREATED_SQL_TEMPLATE, dateTimeFormatToSqlFormat(dateType),
                    dateTimeFormatToSqlFormat(dateType), from.format(DATE_FORMAT), dateTimeFormatToSqlFormat(dateType),
                    to.format(DATE_FORMAT), dateTimeFormatToSqlFormat(dateType), tenantAware.getCurrentTenant(),
                    dateTimeFormatToSqlFormat(dateType));
        case MYSQL_DB_TYPE:
            return String.format(MYSQL_TARGET_CREATED_SQL_TEMPLATE, dateTimeFormatToSqlFormat(dateType),
                    dateTimeFormatToSqlFormat(dateType), from.toString(), dateTimeFormatToSqlFormat(dateType),
                    to.toString(), dateTimeFormatToSqlFormat(dateType), tenantAware.getCurrentTenant(),
                    dateTimeFormatToSqlFormat(dateType));
        default:
            return null;
        }
    }

    private String getFeedbackReceivedQueryTemplate(final DateType<?> dateType, final LocalDateTime from,
            final LocalDateTime to) {
        switch (databaseType) {
        case H2_DB_TYPE:
            return String.format(H2_CONTROLLER_FRRDBACK_SQL_TEMPLATE, dateTimeFormatToSqlFormat(dateType),
                    dateTimeFormatToSqlFormat(dateType), from.format(DATE_FORMAT), dateTimeFormatToSqlFormat(dateType),
                    to.format(DATE_FORMAT), dateTimeFormatToSqlFormat(dateType), tenantAware.getCurrentTenant(),
                    dateTimeFormatToSqlFormat(dateType));
        case MYSQL_DB_TYPE:
            return String.format(MYSQL_CONTROLLER_FRRDBACK_SQL_TEMPLATE, dateTimeFormatToSqlFormat(dateType),
                    dateTimeFormatToSqlFormat(dateType), from.toString(), dateTimeFormatToSqlFormat(dateType),
                    to.toString(), dateTimeFormatToSqlFormat(dateType), tenantAware.getCurrentTenant(),
                    dateTimeFormatToSqlFormat(dateType));
        default:
            return null;
        }
    }

    @Override
    @Cacheable("feedbackReceivedOverTime")
    public <T extends Serializable> DataReportSeries<T> feedbackReceivedOverTime(final DateType<T> dateType,
            final LocalDateTime from, final LocalDateTime to) {
        final Query createNativeQuery = entityManager
                .createNativeQuery(getFeedbackReceivedQueryTemplate(dateType, from, to));
        @SuppressWarnings("unchecked")
        final List<Object[]> resultList = createNativeQuery.getResultList();

        final List<DataReportSeriesItem<T>> reportItems = resultList.stream()
                .map(r -> new DataReportSeriesItem<>(dateType.format((String) r[0]), ((Number) r[1]).longValue()))
                .collect(Collectors.toList());

        return new DataReportSeries<>("FeedbackRecieved", reportItems);
    }

    @Override
    @Cacheable("targetsLastPoll")
    public DataReportSeries<SeriesTime> targetsLastPoll() {

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime beforeHour = now.minusHours(1);
        final LocalDateTime beforeDay = now.minusDays(1);
        final LocalDateTime beforeWeek = now.minusWeeks(1);
        final LocalDateTime beforeMonth = now.minusMonths(1);
        final LocalDateTime beforeYear = now.minusYears(1);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final List<DataReportSeriesItem<SeriesTime>> resultList = new ArrayList<>();

        // hours
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.HOUR,
                entityManager.createQuery(createCountSelectTargetsLastPoll(cb, beforeHour, now)).getSingleResult()));
        // days
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.DAY, entityManager
                .createQuery(createCountSelectTargetsLastPoll(cb, beforeDay, beforeHour)).getSingleResult()));
        // weeks
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.WEEK, entityManager
                .createQuery(createCountSelectTargetsLastPoll(cb, beforeWeek, beforeDay)).getSingleResult()));
        // months
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.MONTH, entityManager
                .createQuery(createCountSelectTargetsLastPoll(cb, beforeMonth, beforeWeek)).getSingleResult()));
        // years
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.YEAR, entityManager
                .createQuery(createCountSelectTargetsLastPoll(cb, beforeYear, beforeMonth)).getSingleResult()));
        // years
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.MORE_THAN_YEAR,
                entityManager.createQuery(createCountSelectTargetsLastPoll(cb, null, beforeYear)).getSingleResult()));
        // never
        resultList.add(new DataReportSeriesItem<SeriesTime>(SeriesTime.NEVER,
                entityManager.createQuery(createCountSelectTargetsLastPoll(cb, null, null)).getSingleResult()));

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
        final Root<JpaTarget> countSelectRoot = countSelect.from(JpaTarget.class);
        final Join<JpaTarget, JpaTargetInfo> targetInfoJoin = countSelectRoot.join(JpaTarget_.targetInfo);
        countSelect.select(cb.count(countSelectRoot));
        if (start != null && end != null) {
            countSelect.where(cb.between(targetInfoJoin.get(JpaTargetInfo_.lastTargetQuery), start, end));
        } else if (from == null && to != null) {
            countSelect.where(cb.lessThanOrEqualTo(targetInfoJoin.get(JpaTargetInfo_.lastTargetQuery), end));
        } else {
            countSelect.where(cb.isNull(targetInfoJoin.get(JpaTargetInfo_.lastTargetQuery)));
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

    private static final class InnerOuter {
        final DSName name;
        long count;
        final List<InnerOuter> outer;

        private InnerOuter(final DSName idName) {
            name = idName;
            outer = new ArrayList<>();
        }

        private InnerOuter(final DSName idName, final long count) {
            name = idName;
            this.count = count;
            outer = new ArrayList<>();
        }

        private void addOuter(final DSName idName, final long count) {
            outer.add(new InnerOuter(idName, count));
            this.count += count;
        }

        private DataReportSeriesItem<String> toItem() {
            return new DataReportSeriesItem<>(name.getName() != null ? name.getName() : "misc", count);
        }
    }

    /**
     * Object contains the name and the id of an entity.
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
            return name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (name == null ? 0 : name.hashCode());
            return result;
        }

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
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "DSName [name=" + name + "]";
        }
    }

    private String dateTimeFormatToSqlFormat(final DateType<?> datatype) {
        switch (databaseType) {
        case H2_DB_TYPE:
            return datatype.h2Format();
        case MYSQL_DB_TYPE:
            return datatype.mySqlFormat();
        default:
            return null;
        }
    }

}

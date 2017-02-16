/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.ReportManagement;
import org.eclipse.hawkbit.repository.ReportManagement.DateTypes;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.report.model.DataReportSeries;
import org.eclipse.hawkbit.repository.report.model.DataReportSeriesItem;
import org.eclipse.hawkbit.repository.report.model.InnerOuterDataReportSeries;
import org.eclipse.hawkbit.repository.report.model.SeriesTime;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithSpringAuthorityRule;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import com.google.common.collect.Lists;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Report Management")
public class ReportManagementTest extends AbstractJpaIntegrationTest {

    private static final String TEST_MESSAGE = "some message";

    @Autowired
    private ReportManagement reportManagement;

    @Autowired
    private AuditingHandler auditingHandler;

    @After
    public void afterTest() {
        auditingHandler.setDateTimeProvider(CurrentDateTimeProvider.INSTANCE);
    }

    @Test
    @Description("Tests correct statistics calculation including a correct cache evict.")
    public void targetsCreatedOverPeriod() {

        // the maximum months going back from now
        // create more targets than asking the report so we can check if the
        // report is returning the
        // correct timeframe
        final int maxMonthBackAmountCreateTargets = 10;
        final int maxMonthBackAmountReportTargets = 4;

        final DynamicDateTimeProvider dynamicDateTimeProvider = new DynamicDateTimeProvider();
        auditingHandler.setDateTimeProvider(dynamicDateTimeProvider);

        for (int month = 0; month < maxMonthBackAmountCreateTargets; month++) {
            dynamicDateTimeProvider.nowMinusMonths(month);
            testdataFactory.createTarget("t" + month);
        }

        final LocalDateTime to = LocalDateTime.now();
        final LocalDateTime from = to.minusMonths(maxMonthBackAmountReportTargets);

        DataReportSeries<LocalDate> targetsCreatedOverPeriod = reportManagement
                .targetsCreatedOverPeriod(DateTypes.perMonth(), from, to);

        // +1 because we go back #maxMonthBackAmountReportTargets but in the
        // report the current month
        // is included for sure, so from this month we go back
        assertThat(targetsCreatedOverPeriod.getData()).as("created over period has wrong size")
                .hasSize(maxMonthBackAmountReportTargets + 1);
        for (final DataReportSeriesItem<LocalDate> reportItem : targetsCreatedOverPeriod.getData()) {
            // only one target is created for each month
            assertThat(reportItem.getData().intValue()).as("Target for each month").isEqualTo(1);
        }

        // check cache evict
        for (int month = 0; month < maxMonthBackAmountCreateTargets; month++) {
            dynamicDateTimeProvider.nowMinusMonths(month);
            testdataFactory.createTarget("t2" + month);
        }
        targetsCreatedOverPeriod = reportManagement.targetsCreatedOverPeriod(DateTypes.perMonth(), from, to);
        for (final DataReportSeriesItem<LocalDate> reportItem : targetsCreatedOverPeriod.getData()) {
            assertThat(reportItem.getData().intValue()).as("Target for each month").isEqualTo(2);
        }
    }

    @Test
    @Description("Tests correct statistics calculation including a correct cache evict.")
    public void targetsFeedbackOverPeriod() {

        // the maximum months going back from now
        // create more targets than asking the report so we can check if the
        // report is returning the
        // correct timeframe
        final int maxMonthBackAmountCreateTargets = 10;
        final int maxMonthBackAmountReportTargets = 4;

        final LocalDateTime to = LocalDateTime.now();
        final LocalDateTime from = to.minusMonths(maxMonthBackAmountReportTargets);

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds");

        final DynamicDateTimeProvider dynamicDateTimeProvider = new DynamicDateTimeProvider();
        auditingHandler.setDateTimeProvider(dynamicDateTimeProvider);

        for (int month = 0; month < maxMonthBackAmountCreateTargets; month++) {
            dynamicDateTimeProvider.nowMinusMonths(month);
            final Target createTarget = testdataFactory.createTarget("t" + month);
            final DistributionSetAssignmentResult result = assignDistributionSet(distributionSet,
                    Lists.newArrayList(createTarget));
            controllerManagament.registerRetrieved(result.getActions().get(0),
                    "Controller retrieved update action and should start now the download.");
        }
        DataReportSeries<LocalDate> feedbackReceivedOverTime = reportManagement
                .feedbackReceivedOverTime(DateTypes.perMonth(), from, to);
        // +1 because we go back #maxMonthBackAmountReportTargets but in the
        // report the current month
        // is included for sure, so from this month we go back
        assertThat(feedbackReceivedOverTime.getData()).as("feedback receiver has wrong data size")
                .hasSize(maxMonthBackAmountReportTargets + 1);
        for (final DataReportSeriesItem<LocalDate> reportItem : feedbackReceivedOverTime.getData()) {
            // only one target feedback is created for each month
            assertThat(reportItem.getData().intValue()).as("data size is wrong").isEqualTo(1);
        }

        // check cache evict
        for (int month = 0; month < maxMonthBackAmountCreateTargets; month++) {
            dynamicDateTimeProvider.nowMinusMonths(month);
            final Target createTarget = testdataFactory.createTarget("t2" + month);
            final DistributionSetAssignmentResult result = assignDistributionSet(distributionSet,
                    Lists.newArrayList(createTarget));
            controllerManagament.registerRetrieved(result.getActions().get(0),
                    "Controller retrieved update action and should start now the download.");
        }
        feedbackReceivedOverTime = reportManagement.feedbackReceivedOverTime(DateTypes.perMonth(), from, to);
        for (final DataReportSeriesItem<LocalDate> reportItem : feedbackReceivedOverTime.getData()) {
            assertThat(reportItem.getData().intValue()).as("report item has wrong data size").isEqualTo(2);
        }

    }

    @Test
    @Description("Tests correct statistics calculation including a correct cache evict.")
    public void distributionUsageInstalled() {
        final Target knownTarget1 = testdataFactory.createTarget("t1");
        final Target knownTarget2 = testdataFactory.createTarget("t2");
        final Target knownTarget3 = testdataFactory.createTarget("t3");
        final Target knownTarget4 = testdataFactory.createTarget("t4");
        final Target knownTarget5 = testdataFactory.createTarget("t5");

        final SoftwareModule ah = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_APP);
        final SoftwareModule jvm = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_RT);
        final SoftwareModule os = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_OS);

        final DistributionSet distributionSet1 = testdataFactory.createDistributionSet("ds1", "0.0.0", standardDsType,
                Lists.newArrayList(os, jvm, ah));
        final DistributionSet distributionSet11 = testdataFactory.createDistributionSet("ds1", "0.0.1", standardDsType,
                Lists.newArrayList(os, jvm, ah));
        final DistributionSet distributionSet2 = testdataFactory.createDistributionSet("ds2", "0.0.2", standardDsType,
                Lists.newArrayList(os, jvm, ah));
        final DistributionSet distributionSet3 = testdataFactory.createDistributionSet("ds3", "0.0.3", standardDsType,
                Lists.newArrayList(os, jvm, ah));

        // ds1(0.0.0)=[target1,target2], ds1(0.0.1)=[target3]
        assignDistributionSet(distributionSet1.getId(), knownTarget1.getControllerId());
        assignDistributionSet(distributionSet1.getId(), knownTarget2.getControllerId());
        assignDistributionSet(distributionSet11.getId(), knownTarget3.getControllerId());

        // ds2=[target4]
        assignDistributionSet(distributionSet2.getId(), knownTarget4.getControllerId());

        // ds3=[target5] --> ONLY ASSIGNED AND NOT INSTALLED
        assignDistributionSet(distributionSet3.getId(), knownTarget5.getControllerId());

        // set installed status
        testdataFactory.sendUpdateActionStatusToTargets(
                Lists.newArrayList(knownTarget1, knownTarget2, knownTarget3, knownTarget4), Status.FINISHED,
                Collections.singletonList(TEST_MESSAGE));

        List<InnerOuterDataReportSeries<String>> distributionUsage = reportManagement.distributionUsageInstalled(100);

        for (final InnerOuterDataReportSeries<String> innerOuterDataReportSeries : distributionUsage) {

            // innerseries only have one data of the name and the total count
            final DataReportSeriesItem<String> dataReportSeriesItem = innerOuterDataReportSeries.getInnerSeries()
                    .getData()[0];
            if (dataReportSeriesItem.getType().equals("ds1")) {
                // total count of three because ds1 has two different versions
                assertThat(dataReportSeriesItem.getData()).as("Version/Item type of DistributionSet 1 in statistics")
                        .isEqualTo(3L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(Arrays.stream(outerData).map(DataReportSeriesItem::getType).collect(Collectors.toList()))
                        .as("versio item contains wrong version").contains("0.0.0", "0.0.1");
            } else if (dataReportSeriesItem.getType().equals("ds2")) {
                assertThat(dataReportSeriesItem.getData()).as("Version/Item type of DistributionSet 2 in statistics")
                        .isEqualTo(1L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(outerData).as("Version/Item type has wrong size").hasSize(1);
                assertThat(outerData[0].getType()).as("Version/Item type of DistributionSet 2 in statistics")
                        .isEqualTo("0.0.2");
            } else if (dataReportSeriesItem.getType().equals("ds3")) {

                assertThat(dataReportSeriesItem.getData()).as("Version/Item type of DistributionSet 3 in statistics")
                        .isEqualTo(0L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(outerData).as("Version/Item type has wrong size").hasSize(1);
                assertThat(outerData[0].getType()).as("Version/Item type of DistributionSet 3 in statistics")
                        .isEqualTo("0.0.3");
            } else {
                fail("no assertion count for distribution set " + dataReportSeriesItem.getType());
            }
        }

        // Test cache evict
        final Target knownTarget6 = testdataFactory.createTarget("t6");
        assignDistributionSet(distributionSet1.getId(), knownTarget6.getControllerId());
        testdataFactory.sendUpdateActionStatusToTargets(Lists.newArrayList(knownTarget6), Status.FINISHED,
                Collections.singletonList(TEST_MESSAGE));
        distributionUsage = reportManagement.distributionUsageInstalled(100);
        for (final InnerOuterDataReportSeries<String> innerOuterDataReportSeries : distributionUsage) {
            final DataReportSeriesItem<String> dataReportSeriesItem = innerOuterDataReportSeries.getInnerSeries()
                    .getData()[0];
            if (dataReportSeriesItem.getType().equals("ds1")) {
                assertThat(dataReportSeriesItem.getData()).as("Data report item number").isEqualTo(4L);
            }
        }
    }

    @Test
    @Description("Tests correct statistics calculation including a correct cache evict.")
    public void targetStatusReport() {

        final long knownErrorCount = 5;
        final long knownSyncCount = 4;
        final long knownPendingCount = 3;
        final long knownRegCount = 2;
        final long knownUnknownCount = 1;

        createTargetsWithStatus("error", knownErrorCount, TargetUpdateStatus.ERROR);
        createTargetsWithStatus("snyc", knownSyncCount, TargetUpdateStatus.IN_SYNC);
        createTargetsWithStatus("pending", knownPendingCount, TargetUpdateStatus.PENDING);
        createTargetsWithStatus("reg", knownRegCount, TargetUpdateStatus.REGISTERED);
        createTargetsWithStatus("unknown", knownUnknownCount, TargetUpdateStatus.UNKNOWN);

        DataReportSeries<TargetUpdateStatus> targetStatus = reportManagement.targetStatus();
        for (final DataReportSeriesItem<TargetUpdateStatus> reportItem : targetStatus.getData()) {

            switch (reportItem.getType()) {
            case ERROR:
                assertThat(reportItem.getData()).as("ERROR count for targets in statistics").isEqualTo(knownErrorCount);
                break;
            case IN_SYNC:
                assertThat(reportItem.getData()).as("IN_SYNC count for targets in statistics")
                        .isEqualTo(knownSyncCount);
                break;
            case PENDING:
                assertThat(reportItem.getData()).as("PENDING count for targets in statistics")
                        .isEqualTo(knownPendingCount);
                break;
            case REGISTERED:
                assertThat(reportItem.getData()).as("REGISTERED count for targets in statistics")
                        .isEqualTo(knownRegCount);
                break;
            case UNKNOWN:
                assertThat(reportItem.getData()).as("UNKNOWN count for targets in statistics")
                        .isEqualTo(knownUnknownCount);
                break;
            default:
                fail("missing case for unknown target update status " + reportItem.getType());
            }
        }

        // test cache evict
        createTargetsWithStatus("error2", knownErrorCount, TargetUpdateStatus.ERROR);
        createTargetsWithStatus("snyc2", knownSyncCount, TargetUpdateStatus.IN_SYNC);
        createTargetsWithStatus("pending2", knownPendingCount, TargetUpdateStatus.PENDING);
        createTargetsWithStatus("reg2", knownRegCount, TargetUpdateStatus.REGISTERED);
        createTargetsWithStatus("unknown2", knownUnknownCount, TargetUpdateStatus.UNKNOWN);

        targetStatus = reportManagement.targetStatus();
        for (final DataReportSeriesItem<TargetUpdateStatus> reportItem : targetStatus.getData()) {

            switch (reportItem.getType()) {
            case ERROR:
                assertThat(reportItem.getData()).as("ERROR count for targets in statistics")
                        .isEqualTo(knownErrorCount * 2);
                break;
            case IN_SYNC:
                assertThat(reportItem.getData()).as("IN_SYNC count for targets in statistics")
                        .isEqualTo(knownSyncCount * 2);
                break;
            case PENDING:
                assertThat(reportItem.getData()).as("PENDING count for targets in statistics")
                        .isEqualTo(knownPendingCount * 2);
                break;
            case REGISTERED:
                assertThat(reportItem.getData()).as("REGISTERED count for targets in statistics")
                        .isEqualTo(knownRegCount * 2);
                break;
            case UNKNOWN:
                assertThat(reportItem.getData()).as("UNKNOWN count for targets in statistics")
                        .isEqualTo(knownUnknownCount * 2);
                break;
            default:
                fail("missing case for unknown target update status " + reportItem.getType());
            }
        }
    }

    @Test
    @Description("Tests correct statistics calculation including a correct cache evict.")
    public void topXDistributionUsage() {
        final Target knownTarget1 = testdataFactory.createTarget("t1");
        final Target knownTarget2 = testdataFactory.createTarget("t2");
        final Target knownTarget3 = testdataFactory.createTarget("t3");
        final Target knownTarget4 = testdataFactory.createTarget("t4");

        final SoftwareModule ah = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_APP);
        final SoftwareModule jvm = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_RT);
        final SoftwareModule os = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_OS);

        final DistributionSet distributionSet1 = testdataFactory.createDistributionSet("ds1", "0.0.0", standardDsType,
                Lists.newArrayList(os, jvm, ah));
        final DistributionSet distributionSet11 = testdataFactory.createDistributionSet("ds1", "0.0.1", standardDsType,
                Lists.newArrayList(os, jvm, ah));
        final DistributionSet distributionSet2 = testdataFactory.createDistributionSet("ds2", "0.0.2", standardDsType,
                Lists.newArrayList(os, jvm, ah));
        final DistributionSet distributionSet3 = testdataFactory.createDistributionSet("ds3", "0.0.3", standardDsType,
                Lists.newArrayList(os, jvm, ah));

        // ds1(0.0.0)=[target1,target2], ds1(0.0.1)=[target3]
        assignDistributionSet(distributionSet1.getId(), knownTarget1.getControllerId());
        assignDistributionSet(distributionSet1.getId(), knownTarget2.getControllerId());
        assignDistributionSet(distributionSet11.getId(), knownTarget3.getControllerId());

        // ds2=[target4]
        assignDistributionSet(distributionSet2.getId(), knownTarget4.getControllerId());

        // expect: ds1(0.0.0)=[target1,target2], ds1(0.0.1)=[target3],
        // ds2=[target4], ds3=[]

        List<InnerOuterDataReportSeries<String>> distributionUsage = reportManagement.distributionUsageAssigned(100);

        for (final InnerOuterDataReportSeries<String> innerOuterDataReportSeries : distributionUsage) {

            // innerseries only have one data of the name and the total count
            final DataReportSeriesItem<String> dataReportSeriesItem = innerOuterDataReportSeries.getInnerSeries()
                    .getData()[0];
            if (dataReportSeriesItem.getType().equals("ds1")) {

                // total count of three because ds1 has two different versions
                assertThat(dataReportSeriesItem.getData()).as("Total count of DistributionSet 1 in statistics")
                        .isEqualTo(3L);

                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(Arrays.stream(outerData).map(DataReportSeriesItem::getType).collect(Collectors.toList()))
                        .as("Out series contains wrong version").contains("0.0.0", "0.0.1");

            } else if (dataReportSeriesItem.getType().equals("ds2")) {
                assertThat(dataReportSeriesItem.getData()).as("Total count of DistributionSet 2 in statistics")
                        .isEqualTo(1L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(outerData).as("out series has wrong size").hasSize(1);
                assertThat(outerData[0].getType()).as("Version/Item type of DistributionSet 2 in statistics")
                        .isEqualTo("0.0.2");

            } else if (dataReportSeriesItem.getType().equals("ds3")) {
                assertThat(dataReportSeriesItem.getData()).as("Total count of DistributionSet 3 in statistics")
                        .isEqualTo(0L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(outerData).as("out series has wrong size").hasSize(1);
                assertThat(outerData[0].getType()).as("Version/Item type of DistributionSet 3 in statistics")
                        .isEqualTo("0.0.3");
            } else {
                fail("no assertion count for distribution set " + dataReportSeriesItem.getType());
            }
        }

        // test cache evict
        final Target knownTarget5 = testdataFactory.createTarget("t5");
        assignDistributionSet(distributionSet1.getId(), knownTarget5.getControllerId());
        distributionUsage = reportManagement.distributionUsageAssigned(100);
        for (final InnerOuterDataReportSeries<String> innerOuterDataReportSeries : distributionUsage) {
            final DataReportSeriesItem<String> dataReportSeriesItem = innerOuterDataReportSeries.getInnerSeries()
                    .getData()[0];
            if (dataReportSeriesItem.getType().equals("ds1")) {
                assertThat(dataReportSeriesItem.getData()).as("Total count of DistributionSet 1 in statistics")
                        .isEqualTo(4L);
            }
        }
    }

    @Test
    @Description("Tests correct statistics calculation including a correct cache evict.")
    public void lastPollTargets() {
        // --- prepare ---
        final LocalDateTime now = LocalDateTime.now();
        final int knownTargetsPollLastHour = 2;
        final int knownTargetsPollLastDay = 3;
        final int knownTargetsPollLastWeek = 4;
        final int knownTargetsPollLastMonth = 5;
        final int knownTargetsPollLastYear = 6;
        final int knownTargetsNeverPoll = 7;
        // never
        createTargets("neverPoll", knownTargetsNeverPoll, null);
        // hour
        createTargets("hourPoll", knownTargetsPollLastHour, now.minusMinutes(59));
        // day
        createTargets("dayPoll", knownTargetsPollLastDay, now.minusHours(23));
        // week
        createTargets("weekPoll", knownTargetsPollLastWeek, now.minusDays(6));
        // month
        createTargets("monthPoll", knownTargetsPollLastMonth, now.minusWeeks(3));
        // year
        createTargets("yearPoll", knownTargetsPollLastYear, now.minusMonths(11));

        // --- Test ---
        DataReportSeries<SeriesTime> targetsNotLastPoll = reportManagement.targetsLastPoll();
        DataReportSeriesItem<SeriesTime>[] data = targetsNotLastPoll.getData();

        // --- Verfiy ---

        // verify hour
        assertThat(data[0].getType()).as("Series time").isEqualTo(SeriesTime.HOUR);
        assertThat(data[0].getData()).as("Targets poll last hour").isEqualTo((long) knownTargetsPollLastHour);
        // verify day
        assertThat(data[1].getType()).as("Series time").isEqualTo(SeriesTime.DAY);
        assertThat(data[1].getData()).as("Targets poll last day").isEqualTo((long) knownTargetsPollLastDay);
        // verify week
        assertThat(data[2].getType()).as("Series time").isEqualTo(SeriesTime.WEEK);
        assertThat(data[2].getData()).as("Targets poll last week").isEqualTo((long) knownTargetsPollLastWeek);

        // test cache evict
        createTargets("hourPoll2", knownTargetsPollLastHour, now.minusMinutes(59));
        targetsNotLastPoll = reportManagement.targetsLastPoll();
        data = targetsNotLastPoll.getData();
        assertThat(data[0].getType()).as("Series time").isEqualTo(SeriesTime.HOUR);
        assertThat(data[0].getData()).as("Targets poll last hour").isEqualTo((long) knownTargetsPollLastHour * 2);

    }

    @Test
    @WithUser(tenantId = "mytenant", allSpPermissions = true)
    @Description("Ensures that targets created report is tenant aware and only creates a report for the current tenant.")
    public void targetsCreatedOverPeriodMultiTenancyAware() throws Exception {
        final int targetCreateAmount = 10;

        // create targets for another tenant
        securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("user", "anotherTenant"), () -> {
            for (int index = 0; index < targetCreateAmount; index++) {
                testdataFactory.createTarget("t" + index);
            }
            return null;
        });

        // ensure targets has been created for 'anotherTenant'
        final Slice<Target> targetsForAnotherTenant = securityRule.runAs(
                WithSpringAuthorityRule.withUserAndTenant("user", "anotherTenant"),
                () -> targetManagement.findTargetsAll(new PageRequest(0, 1000)));
        assertThat(targetsForAnotherTenant).as("targets has wrong size").hasSize(targetCreateAmount);

        final LocalDateTime to = LocalDateTime.now();
        final LocalDateTime from = to.minusMonths(targetCreateAmount);
        // now retrieve the report for the 'mytenant'
        final DataReportSeries<LocalDate> targetsCreatedOverPeriod = reportManagement
                .targetsCreatedOverPeriod(DateTypes.perMonth(), from, to);
        assertThat(targetsCreatedOverPeriod.getData()).as("final no targets should final be created for this tenant")
                .hasSize(0);

    }

    private void createTargets(final String prefix, final int amount, final LocalDateTime lastTargetQuery) {
        for (int index = 0; index < amount; index++) {
            final JpaTarget createTarget = (JpaTarget) testdataFactory.createTarget(prefix + index);
            if (lastTargetQuery != null) {
                final JpaTargetInfo targetInfo = (JpaTargetInfo) createTarget.getTargetInfo();
                targetInfo.setNew(false);
                targetInfo
                        .setLastTargetQuery(lastTargetQuery.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                targetInfoRepository.save(targetInfo);
            }
        }
    }

    private void createTargetsWithStatus(final String prefix, final long amount, final TargetUpdateStatus status) {
        for (int index = 0; index < amount; index++) {
            final JpaTarget target = new JpaTarget(prefix + index);
            final Target sTarget = targetRepository.save(target);
            final JpaTargetInfo targetInfo = (JpaTargetInfo) sTarget.getTargetInfo();
            targetInfo.setUpdateStatus(status);
            targetInfoRepository.save(targetInfo);
        }
    }

    private class DynamicDateTimeProvider implements DateTimeProvider {

        private Calendar datetime = Calendar.getInstance();

        @Override
        public Calendar getNow() {
            return datetime;
        }

        public void now() {
            datetime = Calendar.getInstance();
        }

        public void nowMinusMonths(final int amount) {
            datetime = Calendar.getInstance();
            datetime.add(Calendar.MONTH, -amount);
        }

        /**
         * @param datetime
         *            the datetime to set
         */
        public void setDatetime(final Calendar datetime) {
            this.datetime = datetime;
        }
    }
}

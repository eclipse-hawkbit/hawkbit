/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithSpringAuthorityRule;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.report.model.DataReportSeries;
import org.eclipse.hawkbit.report.model.DataReportSeriesItem;
import org.eclipse.hawkbit.report.model.InnerOuterDataReportSeries;
import org.eclipse.hawkbit.report.model.SeriesTime;
import org.eclipse.hawkbit.repository.ReportManagement.DateTypes;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
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
public class ReportManagementTest extends AbstractIntegrationTest {

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
            targetManagement.createTarget(new Target("t" + month));
        }

        final LocalDateTime to = LocalDateTime.now();
        final LocalDateTime from = to.minusMonths(maxMonthBackAmountReportTargets);

        DataReportSeries<LocalDate> targetsCreatedOverPeriod = reportManagement
                .targetsCreatedOverPeriod(DateTypes.perMonth(), from, to);

        // +1 because we go back #maxMonthBackAmountReportTargets but in the
        // report the current month
        // is included for sure, so from this month we go back
        assertThat(targetsCreatedOverPeriod.getData()).hasSize(maxMonthBackAmountReportTargets + 1);
        for (final DataReportSeriesItem<LocalDate> reportItem : targetsCreatedOverPeriod.getData()) {
            // only one target is created for each month
            assertThat(reportItem.getData().intValue()).isEqualTo(1);
        }

        // check cache evict
        for (int month = 0; month < maxMonthBackAmountCreateTargets; month++) {
            dynamicDateTimeProvider.nowMinusMonths(month);
            targetManagement.createTarget(new Target("t2" + month));
        }
        targetsCreatedOverPeriod = reportManagement.targetsCreatedOverPeriod(DateTypes.perMonth(), from, to);
        for (final DataReportSeriesItem<LocalDate> reportItem : targetsCreatedOverPeriod.getData()) {
            assertThat(reportItem.getData().intValue()).isEqualTo(2);
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

        final DistributionSet distributionSet = TestDataUtil.generateDistributionSet("ds", softwareManagement,
                distributionSetManagement);

        final DynamicDateTimeProvider dynamicDateTimeProvider = new DynamicDateTimeProvider();
        auditingHandler.setDateTimeProvider(dynamicDateTimeProvider);

        for (int month = 0; month < maxMonthBackAmountCreateTargets; month++) {
            dynamicDateTimeProvider.nowMinusMonths(month);
            final Target createTarget = targetManagement.createTarget(new Target("t" + month));
            final DistributionSetAssignmentResult result = deploymentManagement.assignDistributionSet(distributionSet,
                    Lists.newArrayList(createTarget));
            controllerManagament.registerRetrieved(result.getActions().get(0),
                    "Controller retrieved update action and should start now the download.");
        }
        DataReportSeries<LocalDate> feedbackReceivedOverTime = reportManagement
                .feedbackReceivedOverTime(DateTypes.perMonth(), from, to);
        // +1 because we go back #maxMonthBackAmountReportTargets but in the
        // report the current month
        // is included for sure, so from this month we go back
        assertThat(feedbackReceivedOverTime.getData()).hasSize(maxMonthBackAmountReportTargets + 1);
        for (final DataReportSeriesItem<LocalDate> reportItem : feedbackReceivedOverTime.getData()) {
            // only one target feedback is created for each month
            assertThat(reportItem.getData().intValue()).isEqualTo(1);
        }

        // check cache evict
        for (int month = 0; month < maxMonthBackAmountCreateTargets; month++) {
            dynamicDateTimeProvider.nowMinusMonths(month);
            final Target createTarget = targetManagement.createTarget(new Target("t2" + month));
            final DistributionSetAssignmentResult result = deploymentManagement.assignDistributionSet(distributionSet,
                    Lists.newArrayList(createTarget));
            controllerManagament.registerRetrieved(result.getActions().get(0),
                    "Controller retrieved update action and should start now the download.");
        }
        feedbackReceivedOverTime = reportManagement.feedbackReceivedOverTime(DateTypes.perMonth(), from, to);
        for (final DataReportSeriesItem<LocalDate> reportItem : feedbackReceivedOverTime.getData()) {
            assertThat(reportItem.getData().intValue()).isEqualTo(2);
        }

    }

    @Test
    @Description("Tests correct statistics calculation including a correct cache evict.")
    public void distributionUsageInstalled() {
        final Target knownTarget1 = targetManagement.createTarget(new Target("t1"));
        final Target knownTarget2 = targetManagement.createTarget(new Target("t2"));
        final Target knownTarget3 = targetManagement.createTarget(new Target("t3"));
        final Target knownTarget4 = targetManagement.createTarget(new Target("t4"));
        final Target knownTarget5 = targetManagement.createTarget(new Target("t5"));

        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.1", null, ""));
        final SoftwareModule jvm = softwareManagement
                .createSoftwareModule(new SoftwareModule(runtimeType, "oracle-jre", "1.7.2", null, ""));
        final SoftwareModule os = softwareManagement
                .createSoftwareModule(new SoftwareModule(osType, "poky", "3.0.2", null, ""));

        final DistributionSet distributionSet1 = distributionSetManagement
                .createDistributionSet(TestDataUtil.buildDistributionSet("ds1", "0.0.0", standardDsType, os, jvm, ah));
        final DistributionSet distributionSet11 = distributionSetManagement
                .createDistributionSet(TestDataUtil.buildDistributionSet("ds1", "0.0.1", standardDsType, os, jvm, ah));
        final DistributionSet distributionSet2 = distributionSetManagement
                .createDistributionSet(TestDataUtil.buildDistributionSet("ds2", "0.0.2", standardDsType, os, jvm, ah));
        final DistributionSet distributionSet3 = distributionSetManagement
                .createDistributionSet(TestDataUtil.buildDistributionSet("ds3", "0.0.3", standardDsType, os, jvm, ah));

        // ds1(0.0.0)=[target1,target2], ds1(0.0.1)=[target3]
        deploymentManagement.assignDistributionSet(distributionSet1.getId(), knownTarget1.getControllerId());
        deploymentManagement.assignDistributionSet(distributionSet1.getId(), knownTarget2.getControllerId());
        deploymentManagement.assignDistributionSet(distributionSet11.getId(), knownTarget3.getControllerId());

        // ds2=[target4]
        deploymentManagement.assignDistributionSet(distributionSet2.getId(), knownTarget4.getControllerId());

        // ds3=[target5] --> ONLY ASSIGNED AND NOT INSTALLED
        deploymentManagement.assignDistributionSet(distributionSet3.getId(), knownTarget5.getControllerId());

        // set installed status
        sendUpdateActionStatusToTargets(distributionSet1, Lists.newArrayList(knownTarget1, knownTarget2),
                Status.FINISHED, "some message");
        sendUpdateActionStatusToTargets(distributionSet11, Lists.newArrayList(knownTarget3), Status.FINISHED,
                "some message");
        sendUpdateActionStatusToTargets(distributionSet2, Lists.newArrayList(knownTarget4), Status.FINISHED,
                "some message");

        List<InnerOuterDataReportSeries<String>> distributionUsage = reportManagement.distributionUsageInstalled(100);

        for (final InnerOuterDataReportSeries<String> innerOuterDataReportSeries : distributionUsage) {

            // innerseries only have one data of the name and the total count
            final DataReportSeriesItem<String> dataReportSeriesItem = innerOuterDataReportSeries.getInnerSeries()
                    .getData()[0];
            if (dataReportSeriesItem.getType().equals("ds1")) {
                // total count of three because ds1 has two different versions
                assertThat(dataReportSeriesItem.getData()).isEqualTo(3L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(Arrays.stream(outerData).map(DataReportSeriesItem::getType).collect(Collectors.toList()))
                        .contains("0.0.0", "0.0.1");
            } else if (dataReportSeriesItem.getType().equals("ds2")) {
                assertThat(dataReportSeriesItem.getData()).isEqualTo(1L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(outerData).hasSize(1);
                assertThat(outerData[0].getType()).isEqualTo("0.0.2");

            } else if (dataReportSeriesItem.getType().equals("ds3")) {
                assertThat(dataReportSeriesItem.getData()).isEqualTo(0L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(outerData).hasSize(1);
                assertThat(outerData[0].getType()).isEqualTo("0.0.3");
            } else {
                fail("no assertion count for distribution set " + dataReportSeriesItem.getType());
            }
        }

        // Test cache evict
        final Target knownTarget6 = targetManagement.createTarget(new Target("t6"));
        deploymentManagement.assignDistributionSet(distributionSet1.getId(), knownTarget6.getControllerId());
        sendUpdateActionStatusToTargets(distributionSet1, Lists.newArrayList(knownTarget6), Status.FINISHED,
                "some message");
        distributionUsage = reportManagement.distributionUsageInstalled(100);
        for (final InnerOuterDataReportSeries<String> innerOuterDataReportSeries : distributionUsage) {
            final DataReportSeriesItem<String> dataReportSeriesItem = innerOuterDataReportSeries.getInnerSeries()
                    .getData()[0];
            if (dataReportSeriesItem.getType().equals("ds1")) {
                assertThat(dataReportSeriesItem.getData()).isEqualTo(4L);

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
                assertThat(reportItem.getData()).isEqualTo(knownErrorCount);
                break;
            case IN_SYNC:
                assertThat(reportItem.getData()).isEqualTo(knownSyncCount);
                break;
            case PENDING:
                assertThat(reportItem.getData()).isEqualTo(knownPendingCount);
                break;
            case REGISTERED:
                assertThat(reportItem.getData()).isEqualTo(knownRegCount);
                break;
            case UNKNOWN:
                assertThat(reportItem.getData()).isEqualTo(knownUnknownCount);
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
                assertThat(reportItem.getData()).isEqualTo(knownErrorCount * 2);
                break;
            case IN_SYNC:
                assertThat(reportItem.getData()).isEqualTo(knownSyncCount * 2);
                break;
            case PENDING:
                assertThat(reportItem.getData()).isEqualTo(knownPendingCount * 2);
                break;
            case REGISTERED:
                assertThat(reportItem.getData()).isEqualTo(knownRegCount * 2);
                break;
            case UNKNOWN:
                assertThat(reportItem.getData()).isEqualTo(knownUnknownCount * 2);
                break;
            default:
                fail("missing case for unknown target update status " + reportItem.getType());
            }
        }
    }

    @Test
    @Description("Tests correct statistics calculation including a correct cache evict.")
    public void topXDistributionUsage() {

        final Target knownTarget1 = targetManagement.createTarget(new Target("t1"));
        final Target knownTarget2 = targetManagement.createTarget(new Target("t2"));
        final Target knownTarget3 = targetManagement.createTarget(new Target("t3"));
        final Target knownTarget4 = targetManagement.createTarget(new Target("t4"));

        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.1", null, ""));
        final SoftwareModule jvm = softwareManagement
                .createSoftwareModule(new SoftwareModule(runtimeType, "oracle-jre", "1.7.2", null, ""));
        final SoftwareModule os = softwareManagement
                .createSoftwareModule(new SoftwareModule(osType, "poky", "3.0.2", null, ""));

        final DistributionSet distributionSet1 = distributionSetManagement
                .createDistributionSet(TestDataUtil.buildDistributionSet("ds1", "0.0.0", standardDsType, os, jvm, ah));
        final DistributionSet distributionSet11 = distributionSetManagement
                .createDistributionSet(TestDataUtil.buildDistributionSet("ds1", "0.0.1", standardDsType, os, jvm, ah));
        final DistributionSet distributionSet2 = distributionSetManagement
                .createDistributionSet(TestDataUtil.buildDistributionSet("ds2", "0.0.2", standardDsType, os, jvm, ah));
        final DistributionSet distributionSet3 = distributionSetManagement
                .createDistributionSet(TestDataUtil.buildDistributionSet("ds3", "0.0.3", standardDsType, os, jvm, ah));

        // ds1(0.0.0)=[target1,target2], ds1(0.0.1)=[target3]
        deploymentManagement.assignDistributionSet(distributionSet1.getId(), knownTarget1.getControllerId());
        deploymentManagement.assignDistributionSet(distributionSet1.getId(), knownTarget2.getControllerId());
        deploymentManagement.assignDistributionSet(distributionSet11.getId(), knownTarget3.getControllerId());

        // ds2=[target4]
        deploymentManagement.assignDistributionSet(distributionSet2.getId(), knownTarget4.getControllerId());

        // expect: ds1(0.0.0)=[target1,target2], ds1(0.0.1)=[target3],
        // ds2=[target4], ds3=[]

        List<InnerOuterDataReportSeries<String>> distributionUsage = reportManagement.distributionUsageAssigned(100);

        for (final InnerOuterDataReportSeries<String> innerOuterDataReportSeries : distributionUsage) {

            // innerseries only have one data of the name and the total count
            final DataReportSeriesItem<String> dataReportSeriesItem = innerOuterDataReportSeries.getInnerSeries()
                    .getData()[0];
            if (dataReportSeriesItem.getType().equals("ds1")) {
                // total count of three because ds1 has two different versions
                assertThat(dataReportSeriesItem.getData()).isEqualTo(3L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(Arrays.stream(outerData).map(DataReportSeriesItem::getType).collect(Collectors.toList()))
                        .contains("0.0.0", "0.0.1");
            } else if (dataReportSeriesItem.getType().equals("ds2")) {
                assertThat(dataReportSeriesItem.getData()).isEqualTo(1L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(outerData).hasSize(1);
                assertThat(outerData[0].getType()).isEqualTo("0.0.2");

            } else if (dataReportSeriesItem.getType().equals("ds3")) {
                assertThat(dataReportSeriesItem.getData()).isEqualTo(0L);
                final DataReportSeriesItem<String>[] outerData = innerOuterDataReportSeries.getOuterSeries().getData();
                assertThat(outerData).hasSize(1);
                assertThat(outerData[0].getType()).isEqualTo("0.0.3");
            } else {
                fail("no assertion count for distribution set " + dataReportSeriesItem.getType());
            }
        }

        // test cache evict
        final Target knownTarget5 = targetManagement.createTarget(new Target("t5"));
        deploymentManagement.assignDistributionSet(distributionSet1.getId(), knownTarget5.getControllerId());
        distributionUsage = reportManagement.distributionUsageAssigned(100);
        for (final InnerOuterDataReportSeries<String> innerOuterDataReportSeries : distributionUsage) {
            final DataReportSeriesItem<String> dataReportSeriesItem = innerOuterDataReportSeries.getInnerSeries()
                    .getData()[0];
            if (dataReportSeriesItem.getType().equals("ds1")) {
                assertThat(dataReportSeriesItem.getData()).isEqualTo(4L);
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

        // for( final DataReportSeriesItem<SeriesTime> dataReportSeriesItem :
        // data ) {
        // System.out.println( dataReportSeriesItem.getData() );
        // }

        // --- Verfiy ---

        // verify hour
        assertThat(data[0].getType()).isEqualTo(SeriesTime.HOUR);
        assertThat(data[0].getData()).isEqualTo((long) knownTargetsPollLastHour);
        // verify day
        assertThat(data[1].getType()).isEqualTo(SeriesTime.DAY);
        assertThat(data[1].getData()).isEqualTo((long) knownTargetsPollLastDay);
        // verify week
        assertThat(data[2].getType()).isEqualTo(SeriesTime.WEEK);
        assertThat(data[2].getData()).isEqualTo((long) knownTargetsPollLastWeek);

        // test cache evict
        createTargets("hourPoll2", knownTargetsPollLastHour, now.minusMinutes(59));
        targetsNotLastPoll = reportManagement.targetsLastPoll();
        data = targetsNotLastPoll.getData();
        assertThat(data[0].getType()).isEqualTo(SeriesTime.HOUR);
        assertThat(data[0].getData()).isEqualTo((long) knownTargetsPollLastHour * 2);

    }

    @Test
    @WithUser(tenantId = "mytenant", allSpPermissions = true)
    @Description("Ensures that targets created report is tenant aware and only creates a report for the current tenant.")
    public void targetsCreatedOverPeriodMultiTenancyAware() throws Exception {
        final int targetCreateAmount = 10;

        // create targets for another tenant
        securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("user", "anotherTenant"), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (int index = 0; index < targetCreateAmount; index++) {
                    targetManagement.createTarget(new Target("t" + index));
                }
                return null;
            }
        });

        // ensure targets has been created for 'anotherTenant'
        final Slice<Target> targetsForAnotherTenant = securityRule.runAs(
                WithSpringAuthorityRule.withUserAndTenant("user", "anotherTenant"), new Callable<Slice<Target>>() {
                    @Override
                    public Slice<Target> call() throws Exception {
                        return targetManagement.findTargetsAll(new PageRequest(0, 1000));
                    }
                });
        assertThat(targetsForAnotherTenant).hasSize(targetCreateAmount);

        final LocalDateTime to = LocalDateTime.now();
        final LocalDateTime from = to.minusMonths(targetCreateAmount);
        // now retrieve the report for the 'mytenant'
        final DataReportSeries<LocalDate> targetsCreatedOverPeriod = reportManagement
                .targetsCreatedOverPeriod(DateTypes.perMonth(), from, to);
        // final no targets should final be created for this tenant
        assertThat(targetsCreatedOverPeriod.getData()).hasSize(0);

    }

    private void createTargets(final String prefix, final int amount, final LocalDateTime lastTargetQuery) {
        for (int index = 0; index < amount; index++) {
            final Target target = new Target(prefix + index);
            final Target createTarget = targetManagement.createTarget(target);
            if (lastTargetQuery != null) {
                final TargetInfo targetInfo = createTarget.getTargetInfo();
                targetInfo.setNew(false);
                targetInfo
                        .setLastTargetQuery(lastTargetQuery.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                targetInfoRepository.save(targetInfo);
            }
        }
    }

    private void createTargetsWithStatus(final String prefix, final long amount, final TargetUpdateStatus status) {
        for (int index = 0; index < amount; index++) {
            final Target target = new Target(prefix + index);
            final Target sTarget = targetRepository.save(target);
            final TargetInfo targetInfo = sTarget.getTargetInfo();
            targetInfo.setUpdateStatus(status);
            targetInfoRepository.save(targetInfo);
        }
    }

    private List<Target> sendUpdateActionStatusToTargets(final DistributionSet dsA, final Iterable<Target> targs,
            final Status status, final String... msgs) {
        final List<Target> result = new ArrayList<Target>();
        for (final Target t : targs) {
            final List<Action> findByTarget = actionRepository.findByTarget(t);
            for (final Action action : findByTarget) {
                result.add(sendUpdateActionStatusToTarget(status, action, t, msgs));
            }
        }
        return result;
    }

    private Target sendUpdateActionStatusToTarget(final Status status, final Action updActA, final Target t,
            final String... msgs) {
        updActA.setStatus(status);

        final ActionStatus statusMessages = new ActionStatus();
        statusMessages.setAction(updActA);
        statusMessages.setOccurredAt(System.currentTimeMillis());
        statusMessages.setStatus(status);
        for (final String msg : msgs) {
            statusMessages.addMessage(msg);
        }
        controllerManagament.addUpdateActionStatus(statusMessages, updActA);
        return targetManagement.findTargetByControllerID(t.getControllerId());
    }

    private class DynamicDateTimeProvider implements DateTimeProvider {

        private Calendar datetime = Calendar.getInstance();

        /*
         * (non-Javadoc)
         * 
         * @see org.springframework.data.auditing.DateTimeProvider#getNow()
         */
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

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TENANT_CONFIGURATION;
import static org.eclipse.hawkbit.auth.SpRole.CONTROLLER_ROLE;
import static org.eclipse.hawkbit.auth.SpRole.SYSTEM_ROLE;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

import jakarta.validation.constraints.NotEmpty;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.eclipse.hawkbit.artifact.ArtifactStorage;
import org.eclipse.hawkbit.artifact.exception.ArtifactStoreException;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.matcher.EventVerifier;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@ActiveProfiles({ "test" })
@ExtendWith({ TestLoggerExtension.class, SharedSqlTestDatabaseExtension.class })
@WithUser(principal = "bumlux", allSpPermissions = true, authorities = { CONTROLLER_ROLE, SYSTEM_ROLE })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = { TestConfiguration.class })
// destroy the context after each test class because otherwise we get problem when context is
// refreshed we e.g. get two instances of CacheManager which leads to very strange test failures.
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
// Cleaning repository will fire "delete" events. We won't count them to the
// test execution. So, the order execution between EventVerifier and Cleanup is important!
@TestExecutionListeners(
        listeners = { EventVerifier.class, CleanupTestExecutionListener.class },
        mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(TestdataFactory.class)
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
public abstract class AbstractIntegrationTest {

    protected static final Pageable PAGE = PageRequest.of(0, 500, Sort.by(Direction.ASC, "id"));
    protected static final Pageable UNPAGED = Pageable.unpaged();

    protected static final URI LOCALHOST = URI.create("http://127.0.0.1");
    protected static final int DEFAULT_TEST_WEIGHT = 500;
    protected static final Random RND = TestdataFactory.RND;

    /**
     * Number of {@link DistributionSetType}s that exist in every test case. One
     * generated by using
     * {@link TestdataFactory#findOrCreateDefaultTestDsType()} and three
     * {@link SystemManagement#getTenantMetadata()};
     */
    protected static final int DEFAULT_DS_TYPES = RepositoryConstants.DEFAULT_DS_TYPES_IN_TENANT + 1;

    @Autowired
    protected SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement;
    @Autowired
    protected SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement;
    @Autowired
    protected DistributionSetManagement<? extends DistributionSet> distributionSetManagement;
    @Autowired
    protected DistributionSetTagManagement<? extends DistributionSetTag> distributionSetTagManagement;
    @Autowired
    protected DistributionSetTypeManagement<? extends DistributionSetType> distributionSetTypeManagement;
    @Autowired
    protected ControllerManagement controllerManagement;
    @Autowired
    protected TargetManagement<? extends Target> targetManagement;
    @Autowired
    protected TargetTypeManagement<? extends TargetType> targetTypeManagement;
    @Autowired
    protected TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement;
    @Autowired
    protected TargetTagManagement<? extends TargetTag> targetTagManagement;
    @Autowired
    protected DeploymentManagement deploymentManagement;
    @Autowired
    protected ConfirmationManagement confirmationManagement;
    @Autowired
    protected DistributionSetInvalidationManagement distributionSetInvalidationManagement;
    @Autowired
    protected ArtifactManagement artifactManagement;
    @Autowired
    protected SystemManagement systemManagement;
    @Autowired
    protected RolloutManagement rolloutManagement;
    @Autowired
    protected RolloutHandler rolloutHandler;
    @Autowired
    protected RolloutGroupManagement rolloutGroupManagement;
    @Autowired
    protected ArtifactStorage artifactStorage;
    @Autowired
    protected QuotaManagement quotaManagement;

    protected SoftwareModuleType osType;
    protected SoftwareModuleType appType;
    protected SoftwareModuleType runtimeType;

    protected DistributionSetType standardDsType;

    @Autowired
    protected TestdataFactory testdataFactory;
    @Autowired
    protected ApplicationEventPublisher eventPublisher;
    private static final String ARTIFACT_DIRECTORY = createTempDir().getAbsolutePath() + "/" + randomString(20);

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("org.eclipse.hawkbit.artifact.fs.path", ARTIFACT_DIRECTORY);
    }

    @AfterAll
    public static void afterClass() {
        if (new File(ARTIFACT_DIRECTORY).exists()) {
            try {
                FileUtils.deleteDirectory(new File(ARTIFACT_DIRECTORY));
            } catch (final IOException | IllegalArgumentException e) {
                log.warn("Cannot delete file-directory", e);
            }
        }
    }

    @BeforeEach
    public void beforeAll() throws Exception {
        final String description = "Updated description.";

        osType = SecurityContextSwitch
                .callAsPrivileged(() -> testdataFactory.findOrCreateSoftwareModuleType(TestdataFactory.SM_TYPE_OS));
        osType = SecurityContextSwitch.callAsPrivileged(() -> softwareModuleTypeManagement
                .update(SoftwareModuleTypeManagement.Update.builder().id(osType.getId()).description(description).build()));

        appType = SecurityContextSwitch.callAsPrivileged(
                () -> testdataFactory.findOrCreateSoftwareModuleType(TestdataFactory.SM_TYPE_APP, Integer.MAX_VALUE));
        appType = SecurityContextSwitch.callAsPrivileged(() -> softwareModuleTypeManagement
                .update(SoftwareModuleTypeManagement.Update.builder().id(appType.getId()).description(description).build()));

        runtimeType = SecurityContextSwitch
                .callAsPrivileged(() -> testdataFactory.findOrCreateSoftwareModuleType(TestdataFactory.SM_TYPE_RT));
        runtimeType = SecurityContextSwitch.callAsPrivileged(() -> softwareModuleTypeManagement
                .update(SoftwareModuleTypeManagement.Update.builder().id(runtimeType.getId()).description(description).build()));

        standardDsType = SecurityContextSwitch.callAsPrivileged(() -> testdataFactory.findOrCreateDefaultTestDsType());

        // publish the reset counter market event to reset the counters after
        // setup. The setup is transparent by the test and its @ExpectedEvent
        // counting so we reset the counter here after the setup. Note that this
        // approach is only working when using a single-thread executor in the
        // ApplicationEventMultiCaster which the TestConfiguration is doing so
        // the order of the events keep the same.
        EventVerifier.publishResetMarkerEvent(eventPublisher);

    }

    @AfterEach
    public void cleanUp() {
        if (new File(ARTIFACT_DIRECTORY).exists()) {
            try {
                FileUtils.cleanDirectory(new File(ARTIFACT_DIRECTORY));
            } catch (final IOException | IllegalArgumentException e) {
                log.warn("Cannot cleanup file-directory", e);
            }
        }
    }

    protected static TenantConfigurationManagement tenantConfigurationManagement() {
        return TenantConfigHelper.getTenantConfigurationManagement();
    }

    /**
     * Gets a valid cron expression describing a schedule with a single
     * maintenance window, starting specified number of minutes after current
     * time.
     *
     * @param minutesToAdd is the number of minutes after the current time
     * @return {@link String} containing a valid cron expression.
     */
    protected static String getTestSchedule(final int minutesToAdd) {
        ZonedDateTime currentTime = ZonedDateTime.now();
        currentTime = currentTime.plusMinutes(minutesToAdd);
        return String.format("%d %d %d %d %d ? %d", currentTime.getSecond(), currentTime.getMinute(),
                currentTime.getHour(), currentTime.getDayOfMonth(), currentTime.getMonthValue(), currentTime.getYear());
    }

    protected static String getTestDuration(final int duration) {
        return String.format("%02d:%02d:00", duration / 60, duration % 60);
    }

    protected static String getTestTimeZone() {
        final ZonedDateTime currentTime = ZonedDateTime.now();
        return currentTime.getOffset().getId().replace("Z", "+00:00");
    }

    private static final Duration AT_LEAST = Duration.ofMillis(Integer.getInteger("hawkbit.it.rest.await.atLeastMs", 5));
    private static final Duration POLL_INTERVAL = Duration.ofMillis(Integer.getInteger("hawkbit.it.rest.await.pollIntervalMs", 10));
    private static final Duration TIMEOUT = Duration.ofMillis(Integer.getInteger("hawkbit.it.rest.await.timeoutMs", 200));

    // default wait condition factory
    protected ConditionFactory await() {
        return Awaitility.await().atLeast(AT_LEAST).pollInterval(POLL_INTERVAL).atMost(TIMEOUT);
    }

    protected DistributionSetType defaultDsType() {
        return systemManagement.getTenantMetadata().getDefaultDsType();
    }

    protected SoftwareModuleType getASmType() {
        return defaultDsType().getMandatoryModuleTypes().stream().findAny().orElseThrow();
    }

    protected static Action getFirstAssignedAction(final DistributionSetAssignmentResult distributionSetAssignmentResult) {
        return distributionSetAssignmentResult.getAssignedEntity().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("expected one assigned action, found none"));
    }

    protected static Long getFirstAssignedActionId(final DistributionSetAssignmentResult distributionSetAssignmentResult) {
        return getFirstAssignedAction(distributionSetAssignmentResult).getId();
    }

    protected static Target getFirstAssignedTarget(final DistributionSetAssignmentResult assignment) {
        return getFirstAssignedAction(assignment).getTarget();
    }

    protected static Comparator<Target> controllerIdComparator() {
        return Comparator.comparing(Target::getControllerId);
    }

    protected static String randomString(final int len) {
        return TestdataFactory.randomString(len);
    }

    protected static byte[] randomBytes(final int len) {
        return TestdataFactory.randomBytes(len);
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final long dsId, final String controllerId) {
        return assignDistributionSet(dsId, controllerId, ActionType.FORCED);
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final long dsId, final String controllerId, final ActionType actionType) {
        return assignDistributionSet(dsId, Collections.singletonList(controllerId), actionType);
    }

    protected DistributionSetAssignmentResult assignDistributionSet(
            final long dsId, final String controllerId, final ActionType actionType, final long forcedTime) {
        return assignDistributionSet(dsId, Collections.singletonList(controllerId), actionType, forcedTime);
    }

    protected DistributionSetAssignmentResult assignDistributionSet(
            final long dsId, final List<String> controllerIds, final ActionType actionType) {
        return assignDistributionSet(dsId, controllerIds, actionType, RepositoryModelConstants.NO_FORCE_TIME);
    }

    protected DistributionSetAssignmentResult assignDistributionSet(
            final long dsId, final List<String> controllerIds, final ActionType actionType, final long forcedTime) {
        return assignDistributionSet(dsId, controllerIds, actionType, forcedTime, null);
    }

    protected DistributionSetAssignmentResult assignDistributionSet(
            final long dsId, final List<String> controllerIds, final ActionType actionType, final long forcedTime, final Integer weight) {
        final boolean confirmationFlowActive = isConfirmationFlowActive();

        final List<DeploymentRequest> deploymentRequests = controllerIds.stream()
                .map(id -> DeploymentRequest.builder(id, dsId)
                        .actionType(actionType).forceTime(forcedTime).weight(weight).confirmationRequired(confirmationFlowActive)
                        .build())
                .toList();
        final List<DistributionSetAssignmentResult> results = deploymentManagement.assignDistributionSets(deploymentRequests, null);
        assertThat(results).hasSize(1);
        return results.get(0);
    }

    protected List<DistributionSetAssignmentResult> assignDistributionSets(final List<DeploymentRequest> requests) {
        final List<DistributionSetAssignmentResult> distributionSetAssignmentResults =
                deploymentManagement.assignDistributionSets(requests, null);
        assertThat(distributionSetAssignmentResults).hasSize(requests.size());
        return distributionSetAssignmentResults;
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final DistributionSet ds, final List<Target> targets) {
        return assignDistributionSet(ds.getId(), targets.stream().map(Target::getControllerId).toList(), ActionType.FORCED);
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final Long dsId, final List<String> targetIds, final int weight) {
        return assignDistributionSet(dsId, targetIds, ActionType.FORCED, RepositoryModelConstants.NO_FORCE_TIME, weight);
    }

    protected DistributionSetAssignmentResult makeAssignment(final DeploymentRequest request) {
        final List<DistributionSetAssignmentResult> results = deploymentManagement.assignDistributionSets(List.of(request), null);
        assertThat(results).hasSize(1);
        return results.get(0);
    }

    /**
     * Test helper method to assign distribution set to a target with a
     * maintenance schedule.
     *
     * @param dsID is the ID for the distribution set being assigned
     * @param controllerId is the ID for the controller to which the distribution set is
     *         being assigned
     * @param maintenanceWindowSchedule is the cron expression to be used for scheduling the
     *         maintenance window. Expression has 6 mandatory fields and 1
     *         last optional field: "second minute hour dayofmonth month
     *         weekday year"
     * @param maintenanceWindowDuration in HH:mm:ss format specifying the duration of a maintenance
     *         window, for example 00:30:00 for 30 minutes
     * @param maintenanceWindowTimeZone is the time zone specified as +/-hh:mm offset from UTC, for
     *         example +02:00 for CET summer time and +00:00 for UTC. The
     *         start time of a maintenance window calculated based on the
     *         cron expression is relative to this time zone
     * @return result of the assignment as { @link
     *         DistributionSetAssignmentResult}.
     */
    protected DistributionSetAssignmentResult assignDistributionSetWithMaintenanceWindow(final long dsID,
            final String controllerId, final String maintenanceWindowSchedule, final String maintenanceWindowDuration,
            final String maintenanceWindowTimeZone) {

        return makeAssignment(DeploymentRequest.builder(controllerId, dsID)
                .maintenance(maintenanceWindowSchedule, maintenanceWindowDuration, maintenanceWindowTimeZone)
                .confirmationRequired(true).build());
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final DistributionSet pset, final Target target) {
        return assignDistributionSet(pset, Collections.singletonList(target));
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final long dsId, final String targetId, final int weight) {
        return assignDistributionSet(dsId, Collections.singletonList(targetId), weight);
    }

    protected void enableMultiAssignments() {
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED, true);
    }

    protected void enableConfirmationFlow() {
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.USER_CONFIRMATION_ENABLED, true);
    }

    protected void disableConfirmationFlow() {
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.USER_CONFIRMATION_ENABLED, false);
    }

    protected boolean isConfirmationFlowActive() {
        return SecurityContextSwitch.getAs(
                SecurityContextSwitch.withUser("as_system", READ_TENANT_CONFIGURATION),
                () -> tenantConfigurationManagement()
                        .getConfigurationValue(TenantConfigurationKey.USER_CONFIRMATION_ENABLED, Boolean.class)
                        .getValue());
    }

    protected Long getOsModule(final DistributionSet ds) {
        return findFirstModuleByType(ds, osType).orElseThrow(NoSuchElementException::new).getId();
    }

    protected Optional<SoftwareModule> findFirstModuleByType(final DistributionSet ds, final SoftwareModuleType type) {
        return ds.getModules().stream().filter(module -> module.getType().equals(type)).findAny();
    }

    protected Optional<? extends SoftwareModule> findFirstModuleByType(final DistributionSetManagement.Create dsCreate,
            final SoftwareModuleType type) {
        return dsCreate.getModules().stream().filter(module -> module.getType().equals(type)).findAny();
    }

    protected Action prepareFinishedUpdate() {
        return prepareFinishedUpdate(TestdataFactory.DEFAULT_CONTROLLER_ID, "", false);
    }

    protected Action prepareFinishedUpdate(final String controllerId, final String distributionSet, final boolean isRequiredMigrationStep) {
        final DistributionSet ds = testdataFactory.createDistributionSet(distributionSet, isRequiredMigrationStep);
        Target savedTarget = testdataFactory.createTarget(controllerId);
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId(), ActionType.FORCED));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);

        if (savedAction.getStatus() == Action.Status.WAIT_FOR_CONFIRMATION) {
            confirmationManagement.confirmAction(savedAction.getId(), null, null);
        }

        controllerManagement.addUpdateActionStatus(
                ActionStatusCreate.builder().actionId(savedAction.getId()).status(Action.Status.RUNNING).build());
        controllerManagement.addUpdateActionStatus(
                ActionStatusCreate.builder().actionId(savedAction.getId()).status(Action.Status.FINISHED).build());

        return controllerManagement.findActionWithDetails(savedAction.getId())
                .orElseThrow(() -> new EntityNotFoundException(Action.class, savedAction.getId()));
    }

    protected void enableBatchAssignments() {
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.BATCH_ASSIGNMENTS_ENABLED, true);
    }

    protected void disableBatchAssignments() {
        tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.BATCH_ASSIGNMENTS_ENABLED, false);
    }

    protected boolean isConfirmationFlowEnabled() {
        return tenantConfigurationManagement().getConfigurationValue(TenantConfigurationKey.USER_CONFIRMATION_ENABLED, Boolean.class)
                .getValue();
    }

    // ensure that next action will get current time millis AFTER got from the previous
    protected void waitNextMillis() {
        final long createTime = System.currentTimeMillis();
        while (System.currentTimeMillis() == createTime) {
            try {
                Thread.sleep(1);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @SuppressWarnings("java:S4042")
    private static File createTempDir() {
        try {
            final File file = Files.createTempDirectory(System.currentTimeMillis() + "_").toFile();
            file.deleteOnExit();
            if (!file.setReadable(true, true) || !file.setWritable(true, true)) {
                if (file.delete()) { // try to delete immediately, if failed - on exit
                    throw new IOException("Can't set proper permissions!");
                } else {
                    throw new IOException("Can't set proper permissions (failed to delete the file immediately(!");
                }
            }
            // try, if not supported - ok
            if (!file.setExecutable(false)) {
                log.debug("Can't remove executable permissions for temp file {}", file);
            }
            if (!file.setExecutable(true, true)) {
                log.debug("Can't set executable permissions for temp directory {} for the owner", file);
            }
            return file;
        } catch (final IOException e) {
            throw new ArtifactStoreException("Cannot create temp file", e);
        }
    }

    protected List<? extends Target> findByUpdateStatus(final TargetUpdateStatus status, final Pageable pageable) {
        return targetManagement.findAll(pageable).stream().filter(target -> status.equals(target.getUpdateStatus())).toList();
    }

    protected TargetType findTargetTypeByName(@NotEmpty String name) {
        return targetTypeManagement.findByRsql("name==" + name, UNPAGED).stream().findAny()
                .orElseThrow(() -> new EntityNotFoundException(TargetType.class, name));
    }

    @SafeVarargs
    protected static <T> Collection<T> concat(final Collection<T>... targets) {
        final List<T> result = new ArrayList<>();
        List.of(targets).forEach(result::addAll);
        return result;
    }
}
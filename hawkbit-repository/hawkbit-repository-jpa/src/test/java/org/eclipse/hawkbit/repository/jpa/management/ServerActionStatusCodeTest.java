/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_ASSIGNED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_AUTO_CONFIRMATION;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_CANCELED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_CANCELING;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_OFFLINE_ASSIGNED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_RETRIEVED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_SKIPPED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_STATUS_CODE_WAIT_FOR_CONFIRMATION;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Configurable status codes for action statuses written by the server
 */
class ServerActionStatusCodeTest extends AbstractJpaIntegrationTest {

    private static final int CANCELING_CODE = 9001;
    private static final int CANCELED_CODE = 9002;
    private static final int WAIT_FOR_CONFIRMATION_CODE = 9003;
    private static final int AUTO_CONFIRMATION_CODE = 9004;
    private static final int ASSIGNED_CODE = 9006;
    private static final int OFFLINE_ASSIGNED_CODE = 9007;
    private static final int SKIPPED_CODE = 9008;
    private static final int RETRIEVED_CODE = 9009;

    @Test
    void noKeyDefinedWritesNoCode() {
        assertThat(tenantConfigurationManagement().getConfigurationValue(ACTION_STATUS_CODE_CANCELING, Integer.class).getValue()).isNull();
        final Long actionId = assignToNewTarget("nokey");

        deploymentManagement.cancelAction(actionId);

        final ActionStatus latest = latestStatus(actionId);
        assertThat(latest.getStatus()).isEqualTo(Status.CANCELING);
        assertThat(latest.getCode()).isEmpty();
        assertThat(lastStatusCode(actionId)).isEmpty();
    }

    @Test
    void maintenanceWindowLapseUsesCancelingCodeAndServerMessage() {
        setCode(ACTION_STATUS_CODE_CANCELING, CANCELING_CODE);
        final DistributionSet ds = testdataFactory.createDistributionSet("mw");
        final Target target = testdataFactory.createTarget("mw");
        final Long actionId = getFirstAssignedActionId(assignDistributionSetWithMaintenanceWindow(
                ds.getId(), target.getControllerId(), getTestSchedule(10), getTestDuration(10), getTestTimeZone()));

        // what DDI calls on a lapsed maintenance window
        controllerManagement.cancelAction(deploymentManagement.findAction(actionId).orElseThrow());

        final ActionStatus latest = latestStatus(actionId);
        assertThat(latest.getStatus()).isEqualTo(Status.CANCELING);
        assertThat(latest.getCode()).contains(CANCELING_CODE);
        assertThat(messages(latest)).containsExactly(
                RepositoryConstants.SERVER_MESSAGE_PREFIX + "cancelation requested due to lapsed maintenance window");
        assertThat(lastStatusCode(actionId)).contains(CANCELING_CODE);
    }

    @Test
    void manualCancelUsesCancelingCode() {
        setCode(ACTION_STATUS_CODE_CANCELING, CANCELING_CODE);
        final Long actionId = assignToNewTarget("manual");

        deploymentManagement.cancelAction(actionId);

        final ActionStatus latest = latestStatus(actionId);
        assertThat(latest.getStatus()).isEqualTo(Status.CANCELING);
        assertThat(latest.getCode()).contains(CANCELING_CODE);
        assertThat(lastStatusCode(actionId)).contains(CANCELING_CODE);
    }

    @Test
    void serverCodeOverwritesDeviceCode() {
        final Long actionId = assignToNewTarget("overwrite");
        controllerManagement.addUpdateActionStatus(ActionStatusCreate.builder().actionId(actionId).status(Status.RUNNING).code(42).build());
        assertThat(lastStatusCode(actionId)).contains(42);

        setCode(ACTION_STATUS_CODE_CANCELING, CANCELING_CODE);
        deploymentManagement.cancelAction(actionId);

        assertThat(lastStatusCode(actionId)).contains(CANCELING_CODE);
    }

    @Test
    void forceQuitUsesCanceledCode() {
        setCode(ACTION_STATUS_CODE_CANCELED, CANCELED_CODE);
        final Long actionId = assignToNewTarget("forcequit");
        deploymentManagement.cancelAction(actionId);

        deploymentManagement.forceQuitAction(actionId);

        final ActionStatus latest = latestStatus(actionId);
        assertThat(latest.getStatus()).isEqualTo(Status.CANCELED);
        assertThat(latest.getCode()).contains(CANCELED_CODE);
        assertThat(lastStatusCode(actionId)).contains(CANCELED_CODE);
    }

    @Test
    void newAssignmentOverridesWithCancelingCode() {
        setCode(ACTION_STATUS_CODE_CANCELING, CANCELING_CODE);
        final Target target = testdataFactory.createTarget("override");
        final DistributionSet ds1 = testdataFactory.createDistributionSet("override1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("override2");
        final Long firstActionId = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), target.getControllerId()));

        assignDistributionSet(ds2.getId(), target.getControllerId());

        final ActionStatus latest = latestStatus(firstActionId);
        assertThat(latest.getStatus()).isEqualTo(Status.CANCELING);
        assertThat(latest.getCode()).contains(CANCELING_CODE);
        assertThat(lastStatusCode(firstActionId)).contains(CANCELING_CODE);
    }

    @Test
    void autoCloseUsesCanceledCode() {
        tenantConfigurationManagement().addOrUpdateConfiguration(REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, true);
        setCode(ACTION_STATUS_CODE_CANCELED, CANCELED_CODE);
        final Target target = testdataFactory.createTarget("autoclose");
        final DistributionSet ds1 = testdataFactory.createDistributionSet("autoclose1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("autoclose2");
        final Long firstActionId = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), target.getControllerId()));

        assignDistributionSet(ds2.getId(), target.getControllerId());

        final ActionStatus latest = latestStatus(firstActionId);
        assertThat(latest.getStatus()).isEqualTo(Status.CANCELED);
        assertThat(latest.getCode()).contains(CANCELED_CODE);
        assertThat(lastStatusCode(firstActionId)).contains(CANCELED_CODE);
    }

    @Test
    void rolloutCancelAndForceQuitUseCodes() {
        setCode(ACTION_STATUS_CODE_CANCELING, CANCELING_CODE);
        setCode(ACTION_STATUS_CODE_CANCELED, CANCELED_CODE);

        final Rollout softRollout = startRolloutOverNewTargets("soft", 3);
        rolloutManagement.cancelActiveActionsForRollouts(softRollout, ActionCancellationType.SOFT);
        assertRolloutActions(softRollout, Status.CANCELING, CANCELING_CODE);

        final Rollout forceRollout = startRolloutOverNewTargets("force", 3);
        rolloutManagement.cancelActiveActionsForRollouts(forceRollout, ActionCancellationType.FORCE);
        assertRolloutActions(forceRollout, Status.CANCELED, CANCELED_CODE);
    }

    @Test
    void waitForConfirmationCode() {
        enableConfirmationFlow();
        setCode(ACTION_STATUS_CODE_WAIT_FOR_CONFIRMATION, WAIT_FOR_CONFIRMATION_CODE);
        final List<Target> targets = testdataFactory.createTargets("wfc", 2);
        final DistributionSet ds = testdataFactory.createDistributionSet("wfc");

        final List<Long> actionIds = assignDistributionSet(ds, targets).getAssignedEntity().stream().map(Action::getId).toList();

        assertThat(actionIds).hasSize(2).allSatisfy(actionId -> {
            assertThat(deploymentManagement.findAction(actionId).orElseThrow().getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
            final ActionStatus initial = latestStatus(actionId);
            assertThat(initial.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
            assertThat(initial.getCode()).contains(WAIT_FOR_CONFIRMATION_CODE);
            assertThat(lastStatusCode(actionId)).contains(WAIT_FOR_CONFIRMATION_CODE);
        });
    }

    @Test
    void autoConfirmationCodeOnActivation() {
        enableConfirmationFlow();
        setCode(ACTION_STATUS_CODE_AUTO_CONFIRMATION, AUTO_CONFIRMATION_CODE);
        final Target target = testdataFactory.createTarget("autoconfact");
        final DistributionSet ds = testdataFactory.createDistributionSet("autoconfact");
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds.getId(), target.getControllerId()));
        assertThat(deploymentManagement.findAction(actionId).orElseThrow().getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);

        confirmationManagement.activateAutoConfirmation(target.getControllerId(), null, null);

        final ActionStatus latest = latestStatus(actionId);
        assertThat(latest.getStatus()).isEqualTo(Status.RUNNING);
        assertThat(latest.getCode()).contains(AUTO_CONFIRMATION_CODE);
        assertThat(lastStatusCode(actionId)).contains(AUTO_CONFIRMATION_CODE);
    }

    @Test
    void autoConfirmationCodeOnAssignment() {
        enableConfirmationFlow();
        setCode(ACTION_STATUS_CODE_AUTO_CONFIRMATION, AUTO_CONFIRMATION_CODE);
        setCode(ACTION_STATUS_CODE_WAIT_FOR_CONFIRMATION, WAIT_FOR_CONFIRMATION_CODE);
        final Target target = testdataFactory.createTarget("autoconfassign");
        final DistributionSet ds = testdataFactory.createDistributionSet("autoconfassign");
        confirmationManagement.activateAutoConfirmation(target.getControllerId(), null, null);

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds.getId(), target.getControllerId()));

        assertThat(deploymentManagement.findAction(actionId).orElseThrow().getStatus()).isEqualTo(Status.RUNNING);
        final List<ActionStatus> statuses = statuses(actionId);
        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getCode()).contains(AUTO_CONFIRMATION_CODE);
        assertThat(lastStatusCode(actionId)).contains(AUTO_CONFIRMATION_CODE);
    }

    @Test
    void assignedCodeOnDirectAssignment() {
        setCode(ACTION_STATUS_CODE_ASSIGNED, ASSIGNED_CODE);
        final List<Target> targets = testdataFactory.createTargets("assigned", 2);
        final DistributionSet ds = testdataFactory.createDistributionSet("assigned");

        final List<Long> actionIds = assignDistributionSet(ds, targets).getAssignedEntity().stream().map(Action::getId).toList();

        assertThat(actionIds).hasSize(2).allSatisfy(actionId -> {
            final ActionStatus initial = latestStatus(actionId);
            assertThat(initial.getStatus()).isEqualTo(Status.RUNNING);
            assertThat(initial.getCode()).contains(ASSIGNED_CODE);
            assertThat(lastStatusCode(actionId)).contains(ASSIGNED_CODE);
        });
    }

    @Test
    void assignedCodeOnRolloutGroupStart() {
        setCode(ACTION_STATUS_CODE_ASSIGNED, ASSIGNED_CODE);

        final Rollout rollout = startRolloutOverNewTargets("rolloutassigned", 3);

        final List<Action> actions = rolloutActions(rollout);
        assertThat(actions).hasSize(3).allSatisfy(action -> {
            assertThat(action.getStatus()).isEqualTo(Status.RUNNING);
            final ActionStatus initial = latestStatus(action.getId());
            assertThat(initial.getStatus()).isEqualTo(Status.RUNNING);
            assertThat(initial.getCode()).contains(ASSIGNED_CODE);
            assertThat(lastStatusCode(action.getId())).contains(ASSIGNED_CODE);
        });
    }

    @Test
    void initiatorConfirmationUsesAssignedCode() {
        enableConfirmationFlow();
        setCode(ACTION_STATUS_CODE_ASSIGNED, ASSIGNED_CODE);
        setCode(ACTION_STATUS_CODE_WAIT_FOR_CONFIRMATION, WAIT_FOR_CONFIRMATION_CODE);
        final Target target = testdataFactory.createTarget("initiator");
        final DistributionSet ds = testdataFactory.createDistributionSet("initiator");

        final Long actionId = getFirstAssignedActionId(makeAssignment(
                DeploymentRequest.builder(target.getControllerId(), ds.getId()).confirmationRequired(false).build()));

        assertThat(deploymentManagement.findAction(actionId).orElseThrow().getStatus()).isEqualTo(Status.RUNNING);
        final ActionStatus initial = latestStatus(actionId);
        assertThat(initial.getCode()).contains(ASSIGNED_CODE);
        assertThat(lastStatusCode(actionId)).contains(ASSIGNED_CODE);
    }

    @Test
    void assignedIsNoFallbackForWaiting() {
        enableConfirmationFlow();
        setCode(ACTION_STATUS_CODE_ASSIGNED, ASSIGNED_CODE);
        final Target target = testdataFactory.createTarget("nofallback");
        final DistributionSet ds = testdataFactory.createDistributionSet("nofallback");

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds.getId(), target.getControllerId()));

        assertThat(deploymentManagement.findAction(actionId).orElseThrow().getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
        assertThat(latestStatus(actionId).getCode()).isEmpty();
        assertThat(lastStatusCode(actionId)).isEmpty();
    }

    @Test
    void offlineAssignmentUsesOfflineCode() {
        setCode(ACTION_STATUS_CODE_OFFLINE_ASSIGNED, OFFLINE_ASSIGNED_CODE);
        final Long actionId = offlineAssignToNewTarget("offline");

        final ActionStatus latest = latestStatus(actionId);
        assertThat(latest.getStatus()).isEqualTo(Status.FINISHED);
        assertThat(latest.getCode()).contains(OFFLINE_ASSIGNED_CODE);
        assertThat(messages(latest)).contains(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Action reported as offline deployment");
        assertThat(lastStatusCode(actionId)).contains(OFFLINE_ASSIGNED_CODE);
    }

    @Test
    void offlineAssignmentWithoutKeyUnchanged() {
        final Long actionId = offlineAssignToNewTarget("offlinenokey");

        final ActionStatus latest = latestStatus(actionId);
        assertThat(latest.getStatus()).isEqualTo(Status.FINISHED);
        assertThat(latest.getCode()).isEmpty();
        assertThat(lastStatusCode(actionId)).isEmpty();
    }

    @Test
    void skippedCodeWhenDsAlreadyAssigned() {
        setCode(ACTION_STATUS_CODE_SKIPPED, SKIPPED_CODE);
        // the distribution set is assigned directly first (a direct assignment after the rollout creation would cancel the
        // scheduled rollout action), then the rollout over the same target is started
        final String controllerId = "skipped";
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("skipped");
        testdataFactory.createTarget(controllerId);
        final Long directActionId = getFirstAssignedActionId(assignDistributionSet(ds.getId(), controllerId));

        final Rollout rollout = testdataFactory.createRolloutByVariables(
                "skippedRollout", "description", 1, "controllerId==" + controllerId, ds, "50", "5");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final Action rolloutAction = deploymentManagement.findActionsByTarget(controllerId, PAGE).getContent().stream()
                .filter(action -> !action.getId().equals(directActionId)).findAny().orElseThrow();
        assertThat(rolloutAction.getStatus()).isEqualTo(Status.FINISHED);
        final ActionStatus latest = latestStatus(rolloutAction.getId());
        assertThat(latest.getStatus()).isEqualTo(Status.RUNNING);
        assertThat(latest.getCode()).contains(SKIPPED_CODE);
        assertThat(messages(latest)).containsExactly(
                RepositoryConstants.SERVER_MESSAGE_PREFIX + "Distribution Set is already assigned. Skipping this action.");
        assertThat(lastStatusCode(rolloutAction.getId())).contains(SKIPPED_CODE);
    }

    @Test
    void retrievedCodeUpdatesStatusAndLastCode() {
        setCode(ACTION_STATUS_CODE_RETRIEVED, RETRIEVED_CODE);
        final Long actionId = assignToNewTarget("retrieved");

        controllerManagement.registerRetrieved(actionId, "retrieved");

        assertThat(deploymentManagement.findAction(actionId).orElseThrow().getStatus()).isEqualTo(Status.RETRIEVED);
        final ActionStatus latest = latestStatus(actionId);
        assertThat(latest.getStatus()).isEqualTo(Status.RETRIEVED);
        assertThat(latest.getCode()).contains(RETRIEVED_CODE);
        assertThat(lastStatusCode(actionId)).contains(RETRIEVED_CODE);
    }

    @Test
    void retrievedWhileCancelingKeepsLastCode() {
        setCode(ACTION_STATUS_CODE_RETRIEVED, RETRIEVED_CODE);
        setCode(ACTION_STATUS_CODE_CANCELING, CANCELING_CODE);
        final Long actionId = assignToNewTarget("retrievedcanceling");
        deploymentManagement.cancelAction(actionId);

        controllerManagement.registerRetrieved(actionId, "cancel retrieved");

        assertThat(deploymentManagement.findAction(actionId).orElseThrow().getStatus()).isEqualTo(Status.CANCELING);
        final ActionStatus latest = latestStatus(actionId);
        assertThat(latest.getStatus()).isEqualTo(Status.RETRIEVED);
        assertThat(latest.getCode()).contains(RETRIEVED_CODE);
        assertThat(lastStatusCode(actionId)).contains(CANCELING_CODE);
    }

    @Test
    void retrievedAgainAfterDeviceFeedbackKeepsDeviceCode() {
        setCode(ACTION_STATUS_CODE_RETRIEVED, RETRIEVED_CODE);
        final Long actionId = assignToNewTarget("retrievedagain");
        controllerManagement.registerRetrieved(actionId, "retrieved");
        controllerManagement.addUpdateActionStatus(ActionStatusCreate.builder().actionId(actionId).status(Status.RUNNING).code(42).build());

        controllerManagement.registerRetrieved(actionId, "retrieved again");

        assertThat(deploymentManagement.findAction(actionId).orElseThrow().getStatus()).isEqualTo(Status.RETRIEVED);
        final ActionStatus latest = latestStatus(actionId);
        assertThat(latest.getStatus()).isEqualTo(Status.RETRIEVED);
        assertThat(latest.getCode()).contains(RETRIEVED_CODE);
        assertThat(lastStatusCode(actionId)).contains(42);
    }

    @Test
    void repeatedRetrieveWritesNoSecondStatus() {
        setCode(ACTION_STATUS_CODE_RETRIEVED, RETRIEVED_CODE);
        final Long actionId = assignToNewTarget("retrievedtwice");

        controllerManagement.registerRetrieved(actionId, "retrieved");
        controllerManagement.registerRetrieved(actionId, "retrieved");

        assertThat(statuses(actionId)).filteredOn(status -> status.getStatus() == Status.RETRIEVED).hasSize(1);
    }

    @Test
    void removedKeyStopsWritingCode() {
        setCode(ACTION_STATUS_CODE_CANCELING, CANCELING_CODE);
        final Long actionIdA = assignToNewTarget("removedA");
        deploymentManagement.cancelAction(actionIdA);
        assertThat(lastStatusCode(actionIdA)).contains(CANCELING_CODE);

        tenantConfigurationManagement().deleteConfiguration(ACTION_STATUS_CODE_CANCELING);
        final Long actionIdB = assignToNewTarget("removedB");
        deploymentManagement.cancelAction(actionIdB);

        assertThat(latestStatus(actionIdB).getCode()).isEmpty();
        assertThat(lastStatusCode(actionIdB)).isEmpty();
    }

    @Test
    void nonIntegerValueRejected() {
        assertThatExceptionOfType(TenantConfigurationValidatorException.class)
                .isThrownBy(() -> tenantConfigurationManagement().addOrUpdateConfiguration(ACTION_STATUS_CODE_CANCELING, "abc"));
    }

    private static void setCode(final String key, final int code) {
        tenantConfigurationManagement().addOrUpdateConfiguration(key, code);
    }

    private Long assignToNewTarget(final String name) {
        final Target target = testdataFactory.createTarget(name);
        final DistributionSet ds = testdataFactory.createDistributionSet(name);
        return getFirstAssignedActionId(assignDistributionSet(ds.getId(), target.getControllerId()));
    }

    private Long offlineAssignToNewTarget(final String name) {
        final Target target = testdataFactory.createTarget(name);
        final DistributionSet ds = testdataFactory.createDistributionSet(name);
        return getFirstAssignedActionId(deploymentManagement
                .offlineAssignedDistributionSets(List.of(new SimpleEntry<>(target.getControllerId(), ds.getId()))).get(0));
    }

    private Rollout startRolloutOverNewTargets(final String prefix, final int count) {
        testdataFactory.createTargets(prefix, count);
        final DistributionSet ds = testdataFactory.createDistributionSet(prefix);
        final Rollout rollout = testdataFactory.createRolloutByVariables(
                prefix + "Rollout", "description", 1, "controllerId==" + prefix + "*", ds, "50", "5");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        return rollout;
    }

    // re-reads the actions of the rollout from the DB
    private List<Action> rolloutActions(final Rollout rollout) {
        return deploymentManagement.findActions("rollout.id==" + rollout.getId(), PAGE).getContent();
    }

    private void assertRolloutActions(final Rollout rollout, final Status expectedStatus, final int expectedCode) {
        assertThat(rolloutActions(rollout)).hasSize(3).allSatisfy(action -> {
            assertThat(action.getStatus()).isEqualTo(expectedStatus);
            final ActionStatus latest = latestStatus(action.getId());
            assertThat(latest.getStatus()).isEqualTo(expectedStatus);
            assertThat(latest.getCode()).contains(expectedCode);
            assertThat(action.getLastActionStatusCode()).contains(expectedCode);
        });
    }

    private List<ActionStatus> statuses(final long actionId) {
        return deploymentManagement.findActionStatusByAction(actionId, PAGE).getContent();
    }

    private ActionStatus latestStatus(final long actionId) {
        final List<ActionStatus> statuses = statuses(actionId);
        assertThat(statuses).isNotEmpty();
        return statuses.get(statuses.size() - 1);
    }

    private List<String> messages(final ActionStatus status) {
        return deploymentManagement.findMessagesByActionStatusId(status.getId(), PAGE).getContent();
    }

    // re-reads the action from the DB
    private Optional<Integer> lastStatusCode(final long actionId) {
        return deploymentManagement.findAction(actionId).orElseThrow().getLastActionStatusCode();
    }
}

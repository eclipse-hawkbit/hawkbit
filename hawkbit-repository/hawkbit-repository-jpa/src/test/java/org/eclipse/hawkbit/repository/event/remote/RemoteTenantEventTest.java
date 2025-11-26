/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionProperties;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.jupiter.api.Test;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: RemoteTenantAwareEvent Tests
 */
class RemoteTenantEventTest extends AbstractRemoteEventTest {

    private static final String TENANT_DEFAULT = "DEFAULT";

    /**
     * Verifies that a testMultiActionAssignEvent can be properly serialized and deserialized
     */
    @Test
    void testMultiActionAssignEvent() {
        final List<String> controllerIds = List.of("id0", "id1", "id2", "id3", "id4loooooooooooooooooooooooooooooooooooonnnnnnnnnnnnnnnnnng");
        final List<Action> actions = controllerIds.stream().map(this::createAction).toList();

        final MultiActionAssignEvent assignEvent = new MultiActionAssignEvent(TENANT_DEFAULT, actions);

        final MultiActionAssignEvent remoteAssignEventProtoStuff = createProtoStuffEvent(assignEvent);
        assertThat(assignEvent).isEqualTo(remoteAssignEventProtoStuff);
        assertThat(remoteAssignEventProtoStuff.getControllerIds()).containsExactlyElementsOf(controllerIds);

        final MultiActionAssignEvent remoteAssignEventJackson = createJacksonEvent(assignEvent);
        assertThat(assignEvent).isEqualTo(remoteAssignEventJackson);
        assertThat(remoteAssignEventJackson.getControllerIds()).containsExactlyElementsOf(controllerIds);
    }

    /**
     * Verifies that a MultiActionCancelEvent can be properly serialized and deserialized
     */
    @Test
    void testMultiActionCancelEvent() {
        final List<String> controllerIds = List.of("id0", "id1", "id2", "id3", "id4loooooooooooooooooooooooooooooooooooonnnnnnnnnnnnnnnnnng");
        final List<Action> actions = controllerIds.stream().map(this::createAction).toList();

        final MultiActionCancelEvent cancelEvent = new MultiActionCancelEvent(TENANT_DEFAULT, actions);

        final MultiActionCancelEvent remoteCancelEventProtoStuff = createProtoStuffEvent(cancelEvent);
        assertThat(cancelEvent).isEqualTo(remoteCancelEventProtoStuff);
        assertThat(remoteCancelEventProtoStuff.getControllerIds()).containsExactlyElementsOf(controllerIds);

        final MultiActionCancelEvent remoteCancelEventJackson = createJacksonEvent(cancelEvent);
        assertThat(cancelEvent).isEqualTo(remoteCancelEventJackson);
        assertThat(remoteCancelEventJackson.getControllerIds()).containsExactlyElementsOf(controllerIds);
    }

    /**
     * Verifies that a DownloadProgressEvent can be properly serialized and deserialized
     */
    @Test
    void reloadDownloadProgressByRemoteEvent() {
        final DownloadProgressEvent downloadProgressEvent = new DownloadProgressEvent(TENANT_DEFAULT, 1L, 3L);

        final DownloadProgressEvent remoteEventProtoStuff = createProtoStuffEvent(downloadProgressEvent);
        assertThat(downloadProgressEvent).isEqualTo(remoteEventProtoStuff);

        final DownloadProgressEvent remoteEventJackson = createJacksonEvent(downloadProgressEvent);
        assertThat(downloadProgressEvent).isEqualTo(remoteEventJackson);
    }

    /**
     * Verifies that a TargetAssignDistributionSetEvent can be properly serialized and deserialized
     */
    @Test
    void testTargetAssignDistributionSetEvent() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        final JpaAction generateAction = new JpaAction();
        generateAction.setActionType(ActionType.FORCED);
        generateAction.setTarget(testdataFactory.createTarget("Test"));
        generateAction.setDistributionSet(dsA);
        generateAction.setStatus(Status.RUNNING);
        generateAction.setInitiatedBy(AccessContext.actor());
        generateAction.setWeight(1000);

        final Action action = actionRepository.save(generateAction);

        final TargetAssignDistributionSetEvent assignmentEvent = new TargetAssignDistributionSetEvent(
                action.getTenant(), dsA.getId(), List.of(action), action.isMaintenanceWindowAvailable());

        final TargetAssignDistributionSetEvent remoteEventProtoStuff = createProtoStuffEvent(assignmentEvent);
        assertTargetAssignDistributionSetEvent(action, remoteEventProtoStuff);

        final TargetAssignDistributionSetEvent remoteEventJackson = createJacksonEvent(assignmentEvent);
        assertTargetAssignDistributionSetEvent(action, remoteEventJackson);
    }

    /**
     * Verifies that a TargetAssignDistributionSetEvent can be properly serialized and deserialized
     */
    @Test
    void testCancelTargetAssignmentEvent() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        final JpaAction generateAction = new JpaAction();
        generateAction.setActionType(ActionType.FORCED);
        generateAction.setTarget(testdataFactory.createTarget("Test"));
        generateAction.setDistributionSet(dsA);
        generateAction.setStatus(Status.RUNNING);
        generateAction.setInitiatedBy(AccessContext.actor());
        generateAction.setWeight(1000);

        final Action action = actionRepository.save(generateAction);

        final CancelTargetAssignmentEvent cancelEvent = new CancelTargetAssignmentEvent(action);

        final CancelTargetAssignmentEvent remoteEventProtoStuff = createProtoStuffEvent(cancelEvent);
        assertCancelTargetAssignmentEvent(action, remoteEventProtoStuff);

        final CancelTargetAssignmentEvent remoteEventJackson = createJacksonEvent(cancelEvent);
        assertCancelTargetAssignmentEvent(action, remoteEventJackson);
    }

    private Action createAction(final String controllerId) {
        final JpaAction generateAction = new JpaAction();
        generateAction.setId(1L);
        generateAction.setActionType(ActionType.FORCED);
        generateAction.setTarget(testdataFactory.createTarget(controllerId));
        generateAction.setStatus(Status.RUNNING);
        return generateAction;
    }

    private void assertTargetAssignDistributionSetEvent(final Action action, final TargetAssignDistributionSetEvent underTest) {
        assertThat(underTest.getActions()).hasSize(1);
        final ActionProperties actionProperties = underTest.getActions().get(action.getTarget().getControllerId());
        assertThat(actionProperties).isNotNull();
        assertThat(actionProperties).usingRecursiveComparison().comparingOnlyFields().isEqualTo(new ActionProperties(action));
        assertThat(underTest.getDistributionSetId()).isEqualTo(action.getDistributionSet().getId());
    }

    private void assertCancelTargetAssignmentEvent(final Action action, final CancelTargetAssignmentEvent underTest) {
        assertThat(underTest.getActions()).hasSize(1);
        final ActionProperties actionProperties = underTest.getActions().get(action.getTarget().getControllerId());
        assertThat(actionProperties).isNotNull();
        assertThat(actionProperties).usingRecursiveComparison().comparingOnlyFields().isEqualTo(new ActionProperties(action));
        assertThat(underTest.getActionPropertiesForController(action.getTarget().getControllerId())).isPresent();
    }
}
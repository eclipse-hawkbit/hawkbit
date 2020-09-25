/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionProperties;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("RemoteTenantAwareEvent Tests")
public class RemoteTenantAwareEventTest extends AbstractRemoteEventTest {

    private static final String TENANT_DEFAULT = "DEFAULT";

    private static final String APPLICATION_ID_DEFAULT = "Node";

    @Test
    @Description("Verifies that a MultiActionEvent can be properly serialized and deserialized")
    public void testMultiActionEvent() {

        final List<String> controllerIds = Arrays.asList("id0", "id1", "id2", "id3",
                "id4loooooooooooooooooooooooooooooooooooonnnnnnnnnnnnnnnnnng");
        final MultiActionEvent event = new MultiActionEvent(TENANT_DEFAULT, APPLICATION_ID_DEFAULT, controllerIds);

        final MultiActionEvent remoteEventProtoStuff = createProtoStuffEvent(event);
        assertThat(event).isEqualTo(remoteEventProtoStuff);
        assertThat(remoteEventProtoStuff.getControllerIds()).containsExactlyElementsOf(controllerIds);

        final MultiActionEvent remoteEventJackson = createJacksonEvent(event);
        assertThat(event).isEqualTo(remoteEventJackson);
        assertThat(remoteEventJackson.getControllerIds()).containsExactlyElementsOf(controllerIds);

    }

    @Test
    @Description("Verifies that a DownloadProgressEvent can be properly serialized and deserialized")
    public void reloadDownloadProgessByRemoteEvent() {
        final DownloadProgressEvent downloadProgressEvent = new DownloadProgressEvent(TENANT_DEFAULT, 1L, 3L,
                APPLICATION_ID_DEFAULT);

        final DownloadProgressEvent remoteEventProtoStuff = createProtoStuffEvent(downloadProgressEvent);
        assertThat(downloadProgressEvent).isEqualTo(remoteEventProtoStuff);

        final DownloadProgressEvent remoteEventJackson = createJacksonEvent(downloadProgressEvent);
        assertThat(downloadProgressEvent).isEqualTo(remoteEventJackson);
    }

    @Test
    @Description("Verifies that a TargetAssignDistributionSetEvent can be properly serialized and deserialized")
    public void testTargetAssignDistributionSetEvent() {

        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        final JpaAction generateAction = new JpaAction();
        generateAction.setActionType(ActionType.FORCED);
        generateAction.setTarget(testdataFactory.createTarget("Test"));
        generateAction.setDistributionSet(dsA);
        generateAction.setStatus(Status.RUNNING);
        generateAction.setTriggeredBy(tenantAware.getCurrentUsername());

        final Action action = actionRepository.save(generateAction);

        final TargetAssignDistributionSetEvent assignmentEvent = new TargetAssignDistributionSetEvent(
                action.getTenant(), dsA.getId(), Arrays.asList(action), serviceMatcher.getServiceId(),
                action.isMaintenanceWindowAvailable());

        final TargetAssignDistributionSetEvent remoteEventProtoStuff = createProtoStuffEvent(assignmentEvent);
        assertTargetAssignDistributionSetEvent(action, remoteEventProtoStuff);

        final TargetAssignDistributionSetEvent remoteEventJackson = createJacksonEvent(assignmentEvent);
        assertTargetAssignDistributionSetEvent(action, remoteEventJackson);
    }

    private void assertTargetAssignDistributionSetEvent(final Action action,
            final TargetAssignDistributionSetEvent underTest) {

        assertThat(underTest.getActions().size()).isEqualTo(1);
        final ActionProperties actionProperties = underTest.getActions().get(action.getTarget().getControllerId());
        assertThat(actionProperties).isNotNull();
        assertThat(actionProperties).isEqualToComparingFieldByField(new ActionProperties(action));
        assertThat(underTest.getDistributionSetId()).isEqualTo(action.getDistributionSet().getId());
    }
}

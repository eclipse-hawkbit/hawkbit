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
import org.eclipse.hawkbit.repository.model.Target;
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
    @Description("Verifies that multi action event works")
    public void testMultiActionEvent() {

        final List<String> controllerIds = Arrays.asList("id0", "id1", "id2", "id3",
                "id4loooooooooooooooooooooooooooooooooooonnnnnnnnnnnnnnnnnng");
        final MultiActionEvent event = new MultiActionEvent(TENANT_DEFAULT, APPLICATION_ID_DEFAULT, controllerIds);

        final MultiActionEvent remoteEventProtoStuff = createProtoStuffEvent(event);
        assertThat(event).isEqualTo(remoteEventProtoStuff);

        final MultiActionEvent remoteEventJackson = createJacksonEvent(event);
        assertThat(event).isEqualTo(remoteEventJackson);

    }

    @Test
    @Description("Verifies that the download progress reloading by remote events works")
    public void reloadDownloadProgessByRemoteEvent() {
        final DownloadProgressEvent downloadProgressEvent = new DownloadProgressEvent(TENANT_DEFAULT, 1L, 3L,
                APPLICATION_ID_DEFAULT);

        DownloadProgressEvent remoteEvent = createProtoStuffEvent(downloadProgressEvent);
        assertThat(downloadProgressEvent).isEqualTo(remoteEvent);

        remoteEvent = createJacksonEvent(downloadProgressEvent);
        assertThat(downloadProgressEvent).isEqualTo(remoteEvent);
    }

    @Test
    @Description("Verifies that target assignment event works")
    public void testTargetAssignDistributionSetEvent() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final JpaAction generateAction = new JpaAction();
        generateAction.setActionType(ActionType.FORCED);
        final Target target = testdataFactory.createTarget("Test");
        generateAction.setTarget(target);
        generateAction.setDistributionSet(dsA);
        generateAction.setStatus(Status.RUNNING);
        final Action action = actionRepository.save(generateAction);

        final TargetAssignDistributionSetEvent assignmentEvent = new TargetAssignDistributionSetEvent(
                action.getTenant(), dsA.getId(), Arrays.asList(action), serviceMatcher.getServiceId(),
                action.isMaintenanceWindowAvailable());

        TargetAssignDistributionSetEvent underTest = createProtoStuffEvent(assignmentEvent);
        assertTargetAssignDistributionSetEvent(action, underTest);

        underTest = createJacksonEvent(assignmentEvent);
        assertTargetAssignDistributionSetEvent(action, underTest);
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

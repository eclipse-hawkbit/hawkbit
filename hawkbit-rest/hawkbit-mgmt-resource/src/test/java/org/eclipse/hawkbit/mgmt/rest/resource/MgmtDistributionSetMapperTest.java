/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentResponseBody;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;
import org.mockito.Mockito;

import io.qameta.allure.Description;

public class MgmtDistributionSetMapperTest {

    @Test
    @Description("MgmtDistributionSetMapper.toResponse should limit the alreadyAssignedActions in the body to the " +
            "limit defined by the quotaManagement")
    public void testAlreadyAssignedActionsLimitInResponseBody() {
        // GIVEN
        final QuotaManagement quotaManagement = Mockito.mock(QuotaManagement.class);
        final int maxAlreadyAssignedCountInBody = 1;
        doReturn(maxAlreadyAssignedCountInBody).when(quotaManagement).getMaxAlreadyAssignedActionsInAssignmentResult();
        final DistributionSetAssignmentResult assignmentResult = createAssignmentResult();
        
        // WHEN
        final MgmtTargetAssignmentResponseBody responseBody = MgmtDistributionSetMapper.toResponse(assignmentResult,
                quotaManagement);

        // THEN
        assertEquals(0, responseBody.getAssigned());
        assertEquals(2, responseBody.getAlreadyAssigned());
        assertEquals(maxAlreadyAssignedCountInBody, responseBody.getAlreadyAssignedActions().size());
        assertEquals(2, responseBody.getTotal());
    }

    private DistributionSetAssignmentResult createAssignmentResult() {
        final Action action = Mockito.mock(Action.class);
        final Target target = Mockito.mock(Target.class);

        doReturn(target).when(action).getTarget();
        doReturn("controllerId").when(target).getControllerId();
        doReturn(1L).when(action).getId();

        return new DistributionSetAssignmentResult(null, Arrays.asList(target, target), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Arrays.asList(action, action));
    }
}

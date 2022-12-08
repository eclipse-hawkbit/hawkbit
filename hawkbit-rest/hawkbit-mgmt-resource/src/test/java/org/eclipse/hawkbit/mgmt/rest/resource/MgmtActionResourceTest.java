/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Management API")
@Story("Action Resource")
class MgmtActionResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String JSON_PATH_ROOT = "$";
    private static final String JSON_PATH_FIELD_CONTENT = ".content";
    private static final String JSON_PATH_FIELD_SIZE = ".size";
    private static final String JSON_PATH_FIELD_TOTAL = ".total";

    private static final String JSON_PATH_PAGED_LIST_CONTENT = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTENT;
    private static final String JSON_PATH_PAGED_LIST_SIZE = JSON_PATH_ROOT + JSON_PATH_FIELD_SIZE;
    private static final String JSON_PATH_PAGED_LIST_TOTAL = JSON_PATH_ROOT + JSON_PATH_FIELD_TOTAL;

    @Test
    @Description("Verifies that actions can be filtered based on action status.")
    void filterActionsByStatus() throws Exception {

        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Target createTarget = testdataFactory.createTarget("knownTargetId");

        assignDistributionSet(dsA, Collections.singletonList(createTarget));

        final String rsqlPendingStatus = "status==pending";
        final String rsqlFinishedStatus = "status==finished";
        final String rsqlPendingOrFinishedStatus = rsqlFinishedStatus + "," + rsqlPendingStatus;
        // pending status one result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlPendingStatus,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].status", equalTo("pending")));

        // finished status none result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlFinishedStatus,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0))).andExpect(jsonPath("size", equalTo(0)));

        // pending or finished status one result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlPendingOrFinishedStatus,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].status", equalTo("pending")));

    }

    @Test
    @Description("Verifies that actions can be filtered based on distribution set fields.")
    void filterActionsByDistributionSet() throws Exception {

        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Target createTarget = testdataFactory.createTarget("knownTargetId");

        assignDistributionSet(dsA, Collections.singletonList(createTarget));

        final String rsqlDsName = "distributionSet.name==" + dsA.getName() + "*";
        final String rsqlDsVersion = "distributionSet.version==" + dsA.getVersion();

        mvc.perform(
                get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsName, createTarget.getControllerId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)));

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsVersion,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("size", equalTo(1)));

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsName + "," + rsqlDsVersion,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("size", equalTo(1)));

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=distributionSet.name==FooBar",
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0))).andExpect(jsonPath("size", equalTo(0)));
    }

    @Test
    @Description("Verifies that actions can be filtered based on target fields.")
    void filterActionsByTarget() throws Exception {

        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Target createTarget = testdataFactory.createTarget("knownTargetId");

        assignDistributionSet(dsA, Collections.singletonList(createTarget));

        final String rsqlTargetName = "target.name==knownTargetId";

        // pending status one result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlTargetName,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("size", equalTo(1)));
    }

    @Test
    @Description("Verifies that all available actions are returned if the complete collection is requested.")
    void getActions() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING,
                "ID:ASC")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[1].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("update")))
                .andExpect(jsonPath("content.[1].status", equalTo("pending")))
                .andExpect(jsonPath("content.[1]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    @Test
    @Description("Verifies that the get request for actions returns an empty collection if no assignments have been done yet.")
    void getActionsWithEmptyResult() throws Exception {
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(0)))
                .andExpect(jsonPath("content", hasSize(0))).andExpect(jsonPath("total", equalTo(0)));
    }

    @Test
    @Description("Verifies paging is respected as expected.")
    void getMultipleActionsWithPagingLimitRequestParameter() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        // page 1: one entry
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // page 2: one entry
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("update")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    @Test
    @Description("Verifies that the actions resource is read-only.")
    void invalidRequestsOnActionResource() throws Exception {
        final String knownTargetId = "targetId";

        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        final Long actionId = actions.get(0).getId();

        // ensure specific action cannot be accessed via the actions resource
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "/" + actionId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(post(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    private String generateActionSelfLink(final String knownTargetId, final Long actionId) {
        return "http://localhost" + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId;
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverride(final String knownTargetId) {
        return generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(knownTargetId, null, null, null);
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(final String knownTargetId,
            final String schedule, final String duration, final String timezone) {
        final Target target = testdataFactory.createTarget(knownTargetId);

        final Iterator<DistributionSet> sets = testdataFactory.createDistributionSets(2).iterator();
        final DistributionSet one = sets.next();
        final DistributionSet two = sets.next();

        // Update
        if (schedule == null) {
            final List<Target> updatedTargets = assignDistributionSet(one, Collections.singletonList(target))
                    .getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
            // 2nd update
            // sleep 10ms to ensure that we can sort by reportedAt
            Awaitility.await().atMost(Duration.ONE_HUNDRED_MILLISECONDS).atLeast(5, TimeUnit.MILLISECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSet(two, updatedTargets);
        } else {
            final List<Target> updatedTargets = assignDistributionSetWithMaintenanceWindow(one.getId(),
                    target.getControllerId(), schedule, duration, timezone).getAssignedEntity().stream()
                            .map(Action::getTarget).collect(Collectors.toList());
            // 2nd update
            // sleep 10ms to ensure that we can sort by reportedAt
            Awaitility.await().atMost(Duration.ONE_HUNDRED_MILLISECONDS).atLeast(5, TimeUnit.MILLISECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSetWithMaintenanceWindow(two.getId(), updatedTargets.get(0).getControllerId(), schedule,
                    duration, timezone);
        }

        // two updates, one cancellation
        final List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE)
                .getContent();

        assertThat(actions).hasSize(2);
        return actions;
    }

}

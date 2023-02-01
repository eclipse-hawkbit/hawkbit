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
import org.eclipse.hawkbit.mgmt.rest.api.MgmtActionRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Integration test for the {@link MgmtActionRestApi}.
 */
@Feature("Component Tests - Management API")
@Story("Action Resource")
class MgmtActionResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String JSON_PATH_ROOT = "$";
    private static final String JSON_PATH_FIELD_CONTENT = ".content";
    private static final String JSON_PATH_FIELD_SIZE = ".size";
    private static final String JSON_PATH_FIELD_TOTAL = ".total";

    private static final String JSON_PATH_FIELD_ID = ".id";

    private static final String JSON_PATH_PAGED_LIST_CONTENT = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTENT;
    private static final String JSON_PATH_PAGED_LIST_SIZE = JSON_PATH_ROOT + JSON_PATH_FIELD_SIZE;
    private static final String JSON_PATH_PAGED_LIST_TOTAL = JSON_PATH_ROOT + JSON_PATH_FIELD_TOTAL;

    private static final String JSON_PATH_ACTION_ID = JSON_PATH_ROOT + JSON_PATH_FIELD_ID;

    @Test
    @Description("Verifies that actions can be filtered based on action status.")
    void filterActionsByStatus() throws Exception {

        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        assignDistributionSet(dsA, Collections.singletonList(testdataFactory.createTarget("knownTargetId")));

        final String rsqlPendingStatus = "status==pending";
        final String rsqlFinishedStatus = "status==finished";
        final String rsqlPendingOrFinishedStatus = rsqlFinishedStatus + "," + rsqlPendingStatus;

        // pending status one result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlPendingStatus))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1))).andExpect(jsonPath("content[0].status", equalTo("pending")));

        // finished status none result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlFinishedStatus))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("size", equalTo(0)));

        // pending or finished status one result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlPendingOrFinishedStatus))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1))).andExpect(jsonPath("content[0].status", equalTo("pending")));

    }

    @Test
    @Description("Verifies that actions can be filtered based on the detailed action status.")
    void filterActionsByDetailStatus() throws Exception {

        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        assignDistributionSet(dsA, Collections.singletonList(testdataFactory.createTarget("knownTargetId")));

        final String rsqlPendingStatus = "detailStatus==running";
        final String rsqlFinishedStatus = "detailStatus==finished";
        final String rsqlPendingOrFinishedStatus = rsqlFinishedStatus + "," + rsqlPendingStatus;

        // running status one result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlPendingStatus))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].detailStatus", equalTo("running")))
                .andExpect(jsonPath("content[0].status", equalTo("pending")));

        // finished status none result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlFinishedStatus))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("size", equalTo(0)));

        // running or finished status one result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlPendingOrFinishedStatus))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].detailStatus", equalTo("running")))
                .andExpect(jsonPath("content[0].status", equalTo("pending")));

    }

    @Test
    @Description("Verifies that actions can be filtered based on the action status code that was reported last.")
    void filterActionsByLastStatusCode() throws Exception {

        // assign a distribution set to three targets
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(dsA,
                testdataFactory.createTargets("target1", "target2", "target3"));
        final List<Action> actions = assignmentResult.getAssignedEntity();
        assertThat(actions).hasSize(3);

        // then simulate a status update with code 200 for the first action
        final Action action = actions.get(0);
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).code(200)
                .message("Update succeeded").status(Status.FINISHED));

        // verify that one result is returned if the actions are filtered for
        // status code 200
        final String rsqlStatusCode = "lastStatusCode==200";
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlStatusCode))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1))).andExpect(jsonPath("content[0].status", equalTo("finished")));

        // verify no result is returned if we filter for a non-existing status
        // code
        final String rsqlWrongStatusCode = "lastStatusCode==999";
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlWrongStatusCode))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("size", equalTo(0)));
    }

    @Test
    @Description("Verifies that actions can be filtered based on distribution set fields.")
    void filterActionsByDistributionSet() throws Exception {

        // prepare test
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds, Collections.singletonList(testdataFactory.createTarget("knownTargetId")));

        final String rsqlDsName = "distributionSet.name==" + ds.getName() + "*";
        final String rsqlDsVersion = "distributionSet.version==" + ds.getVersion();
        final String rsqlDsId = "distributionSet.id==" + ds.getId();

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsName)
                .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1))).andExpect(jsonPath("content.[0]._links.distributionset.name",
                        equalTo(ds.getName() + ":" + ds.getVersion())));

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsVersion))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)));

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsName + "," + rsqlDsVersion))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)));

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=distributionSet.name==FooBar"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("size", equalTo(0)));

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)));
    }

    @Test
    @Description("Verifies that actions can be filtered based on rollout fields.")
    void filterActionsByRollout() throws Exception {

        // prepare test
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Target target0 = testdataFactory.createTarget("t0");

        // manual assignment
        assignDistributionSet(ds, Collections.singletonList(target0));

        // rollout
        final Target target1 = testdataFactory.createTarget("t1");
        final Rollout rollout = testdataFactory.createRolloutByVariables("TestRollout", "TestDesc", 1,
                "name==" + target1.getName(), ds, "50", "5");
        rolloutManagement.start(rollout.getId());
        rolloutManagement.handleRollouts();

        final String rsqlRolloutName = "rollout.name==" + rollout.getName();
        final String rsqlRolloutId = "rollout.id==" + rollout.getId();

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlRolloutName)
                .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content.[0]._links.target.name", equalTo(target1.getName()))).andExpect(jsonPath(
                        "content.[0]._links.distributionset.name", equalTo(ds.getName() + ":" + ds.getVersion())));

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlRolloutId)
                .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content.[0]._links.target.name", equalTo(target1.getName()))).andExpect(jsonPath(
                        "content.[0]._links.distributionset.name", equalTo(ds.getName() + ":" + ds.getVersion())));
    }

    @Test
    @Description("Verifies that actions can be filtered based on target fields.")
    void filterActionsByTarget() throws Exception {

        // prepare test
        final Target target = testdataFactory.createTarget("knownTargetId");
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds, Collections.singletonList(target));

        final String rsqlTargetName = "target.name==knownTargetId";

        // pending status one result
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlTargetName)
                .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content.[0]._links.target.name", equalTo(target.getName()))).andExpect(jsonPath(
                        "content.[0]._links.distributionset.name", equalTo(ds.getName() + ":" + ds.getVersion())));
    }

    @Test
    @Description("Verifies that all available actions are returned if the complete collection is requested.")
    void getActions() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        final Action action0 = actions.get(0);
        final Action action1 = actions.get(1);
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING,
                "ID:ASC")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())

                // verify action 1
                .andExpect(jsonPath("content.[1].id", equalTo(action1.getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("update")))
                .andExpect(jsonPath("content.[1].status", equalTo("pending")))
                .andExpect(jsonPath("content.[1].detailStatus", equalTo("running")))
                .andExpect(jsonPath("content.[1]._links.self.href",
                        equalTo(generateActionLink(knownTargetId, action1.getId()))))

                // verify action 0
                .andExpect(jsonPath("content.[0].id", equalTo(action0.getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[1].detailStatus", equalTo("running")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionLink(knownTargetId, action0.getId()))))

                // verify collection properties
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    @Test
    @Description("Verifies that a full representation of all actions is returned if the collection is requested for representation mode 'full'.")
    void getActionsFullRepresentation() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        final Action action0 = actions.get(0);
        final Action action1 = actions.get(1);
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())

                // verify action 1
                .andExpect(jsonPath("content.[1].id", equalTo(action1.getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("update")))
                .andExpect(jsonPath("content.[1].status", equalTo("pending")))
                .andExpect(jsonPath("content.[1].detailStatus", equalTo("running")))
                .andExpect(jsonPath("content.[1]._links.self.href",
                        equalTo(generateActionLink(knownTargetId, action1.getId()))))
                .andExpect(jsonPath("content.[1]._links.target.href", equalTo(generateTargetLink(knownTargetId))))
                .andExpect(jsonPath("content.[1]._links.distributionset.href",
                        equalTo(generateDistributionSetLink(action1))))

                // verify action 0
                .andExpect(jsonPath("content.[0].id", equalTo(action0.getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0].detailStatus", equalTo("canceling")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionLink(knownTargetId, action0.getId()))))
                .andExpect(jsonPath("content.[0]._links.target.href", equalTo(generateTargetLink(knownTargetId))))
                .andExpect(jsonPath("content.[0]._links.distributionset.href",
                        equalTo(generateDistributionSetLink(action0))))

                // verify collection properties
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
        final Action action0 = actions.get(0);
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())

                // verify action 0
                .andExpect(jsonPath("content.[0].id", equalTo(action0.getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0].detailStatus", equalTo(action0.getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionLink(knownTargetId, action0.getId()))))

                // verify collection properties
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // page 2: one entry
        final Action action1 = actions.get(1);
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())

                // verify action 1
                .andExpect(jsonPath("content.[0].id", equalTo(action1.getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("update")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0].detailStatus", equalTo(action1.getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionLink(knownTargetId, action1.getId()))))

                // verify collection properties
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

        // not allowed methods
        mvc.perform(post(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Description("Verifies that the correct action is returned")
    void shouldRetrieveCorrectActionById() throws Exception {
        final String knownTargetId = "targetId";

        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        final Long actionId = actions.get(0).getId();

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "/" + actionId))
            .andDo(MockMvcResultPrinter.print())
            .andExpect(status().isOk())
            .andExpect(jsonPath(JSON_PATH_ACTION_ID, equalTo(actionId.intValue())));
    }

    @Test
    @Description("Verifies that NOT_FOUND is returned when there is no such action.")
    void requestActionThatDoesNotExistsLeadsToNotFound() throws Exception {
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "/" + 101))
            .andDo(MockMvcResultPrinter.print())
            .andExpect(status().isNotFound());
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

    private static String generateActionLink(final String targetId, final Long actionId) {
        return "http://localhost" + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId;
    }

    private static String generateTargetLink(final String targetId) {
        return "http://localhost" + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetId;
    }

    private static String generateDistributionSetLink(final Action action) {
        return "http://localhost" + MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/"
                + action.getDistributionSet().getId();
    }

}

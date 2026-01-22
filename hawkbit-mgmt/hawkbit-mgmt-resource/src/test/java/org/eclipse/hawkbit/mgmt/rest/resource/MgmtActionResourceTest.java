/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.ACTION_V1_REQUEST_MAPPING;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_SEARCH;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
import org.springframework.test.web.servlet.ResultActions;

/**
 * Integration test for the {@link MgmtActionRestApi}.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Action Resource
 */
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

    /**
     * Handles the GET request of retrieving a specific action.
     */
    @Test
    void getAction() throws Exception {
        getAction(false);
    }

    /**
     * Handles the GET request of retrieving a specific action with external reference.
     */
    @Test
    void getActionExtRef() throws Exception {
        getAction(true);
    }

    /**
     * Verifies that actions can be filtered based on action status.
     */
    @Test
    void filterActionsByStatus() throws Exception {
        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        assignDistributionSet(dsA, Collections.singletonList(testdataFactory.createTarget("knownTargetId")));

        final String rsqlPendingStatus = "active==true";
        final String rsqlFinishedStatus = "active==false";
        final String rsqlPendingOrFinishedStatus = rsqlFinishedStatus + "," + rsqlPendingStatus;

        // pending status one result
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlPendingStatus))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].active", equalTo(true)));

        // finished status none result
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlFinishedStatus))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("size", equalTo(0)));

        // pending or finished status one result
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlPendingOrFinishedStatus))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].active", equalTo(true)));

    }

    /**
     * Verifies that actions can be filtered based on the detailed action status.
     */
    @Test
    void filterActionsByDetailStatus() throws Exception {
        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        assignDistributionSet(dsA, Collections.singletonList(testdataFactory.createTarget("knownTargetId")));

        final String rsqlPendingStatus = "status==running";
        final String rsqlFinishedStatus = "status==finished";
        final String rsqlPendingOrFinishedStatus = rsqlFinishedStatus + "," + rsqlPendingStatus;

        // running status one result
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlPendingStatus))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].active", equalTo(true)))
                .andExpect(jsonPath("content[0].status", equalTo("running")));

        // finished status none result
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlFinishedStatus))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("size", equalTo(0)));

        // running or finished status one result
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlPendingOrFinishedStatus))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].active", equalTo(true)))
                .andExpect(jsonPath("content[0].status", equalTo("running")));
    }

    /**
     * Verifies that actions can be filtered based on extRef.
     */
    @Test
    void filterActionsByExternalRef() throws Exception {
        // prepare test
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        final Action action0 = actions.get(0);
        final Action action1 = actions.get(1);

        final List<String> externalRefs = new ArrayList<>(2);
        externalRefs.add("extRef");
        externalRefs.add("extRef#123_1");
        controllerManagement.updateActionExternalRef(action0.getId(), externalRefs.get(0));
        controllerManagement.updateActionExternalRef(action1.getId(), externalRefs.get(1));

        final String rsqlExtRef = "externalref==extRef";
        final String rsqlExtRefWildcard = "externalref==extRef*";
        final String rsqlExtRefNoMatch = "externalref==234extRef";

        // pending status one result
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlExtRef))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].externalRef", equalTo(externalRefs.get(0))));

        // finished status none result
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlExtRefWildcard))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(2)))
                .andExpect(jsonPath("size", equalTo(2)));

        // pending or finished status one result
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlExtRefNoMatch))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("size", equalTo(0)));
    }

    /**
     * Verifies that actions can be filtered based on the action status code that was reported last.
     */
    @Test
    void filterActionsByLastStatusCode() throws Exception {
        // assign a distribution set to three targets
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(
                dsA, testdataFactory.createTargets("target1", "target2", "target3"));
        final List<Action> actions = assignmentResult.getAssignedEntity();
        assertThat(actions).hasSize(3);

        // then simulate a status update with code 200 for the first action
        final Action action = actions.get(0);
        controllerManagement.addUpdateActionStatus(Action.ActionStatusCreate.builder()
                .actionId(action.getId()).code(200).messages(List.of("Update succeeded")).status(Status.FINISHED).build());

        // verify that one result is returned if the actions are filtered for status code 200
        final String rsqlStatusCode = "lastStatusCode==200";
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlStatusCode))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].status", equalTo("finished")));

        // verify no result is returned if we filter for a non-existing status code
        final String rsqlWrongStatusCode = "lastStatusCode==999";
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlWrongStatusCode))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("size", equalTo(0)));
    }

    /**
     * Verifies that actions can be filtered based on distribution set fields.
     */
    @Test
    void filterActionsByDistributionSet() throws Exception {
        // prepare test
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds, Collections.singletonList(testdataFactory.createTarget("knownTargetId")));

        final String rsqlDsName = "distributionSet.name==" + ds.getName() + "*";
        final String rsqlDsVersion = "distributionSet.version==" + ds.getVersion();
        final String rsqlDsId = "distributionSet.id==" + ds.getId();

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsName)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content.[0]._links.distributionset.name",
                        equalTo(ds.getName() + ":" + ds.getVersion())));

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsVersion))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)));

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsName + "," + rsqlDsVersion))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)));

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=distributionSet.name==FooBar"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("size", equalTo(0)));

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlDsId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)));
    }

    /**
     * Verifies that actions can be filtered based on rollout fields.
     */
    @Test
    void filterActionsByRollout() throws Exception {
        // prepare test
        final DistributionSet ds = testdataFactory.createDistributionSetLocked();
        final Target target0 = testdataFactory.createTarget("t0");

        // manual assignment
        assignDistributionSet(ds, Collections.singletonList(target0));

        // rollout
        final Target target1 = testdataFactory.createTarget("t1");
        final Rollout rollout = testdataFactory.createRolloutByVariables(
                "TestRollout", "TestDesc", 1, "name==" + target1.getName(), ds, "50", "5");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final String rsqlRolloutName = "rollout.name==" + rollout.getName();
        final String rsqlRolloutId = "rollout.id==" + rollout.getId();

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlRolloutName)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content.[0]._links.target.name", equalTo(target1.getName())))
                .andExpect(jsonPath(
                        "content.[0]._links.distributionset.name", equalTo(ds.getName() + ":" + ds.getVersion())));

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlRolloutId)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content.[0]._links.target.name", equalTo(target1.getName())))
                .andExpect(jsonPath("content.[0]._links.distributionset.name", equalTo(ds.getName() + ":" + ds.getVersion())));
    }

    /**
     * Verifies that actions can be filtered based on target fields.
     */
    @Test
    void filterActionsByTargetProperties() throws Exception {
        // prepare test
        final Target target = testdataFactory.createTarget("knownTargetId", "knownTargetName", "http://0.0.0.0");
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds, Collections.singletonList(target));

        verifyResultsByTargetPropertyFilter(target, ds, "target.controllerId==knownTargetId");
        verifyResultsByTargetPropertyFilter(target, ds, "target.name==knownTargetName");
        verifyResultsByTargetPropertyFilter(target, ds, "target.updateStatus==pending");
        verifyResultsByTargetPropertyFilter(target, ds, "target.address==http://0.0.0.0");
    }

    /**
     * Verifies that all available actions are returned if the complete collection is requested.
     */
    @Test
    void getActions() throws Exception {
        getActions(false);
    }

    /**
     * Verifies that all available actions (whit ext refs) are returned if the complete collection is requested.
     */
    @Test
    void getActionsExtRef() throws Exception {
        getActions(true);
    }

    /**
     * Verifies that a full representation of all actions is returned if the collection is requested for representation mode 'full'.
     */
    @Test
    void getActionsFullRepresentation() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        final Action action0 = actions.get(0);
        final Action action1 = actions.get(1);
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())

                // verify action 1
                .andExpect(jsonPath("content.[1].id", equalTo(action1.getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("update")))
                .andExpect(jsonPath("content.[1].active", equalTo(true)))
                .andExpect(jsonPath("content.[1].status", equalTo("running")))
                .andExpect(jsonPath("content.[1]._links.self.href",
                        equalTo(generateActionLink(knownTargetId, action1.getId()))))
                .andExpect(jsonPath("content.[1]._links.target.href", equalTo(generateTargetLink(knownTargetId))))
                .andExpect(jsonPath("content.[1]._links.distributionset.href",
                        equalTo(generateDistributionSetLink(action1))))

                // verify action 0
                .andExpect(jsonPath("content.[0].id", equalTo(action0.getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].active", equalTo(true)))
                .andExpect(jsonPath("content.[0].status", equalTo("canceling")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionLink(knownTargetId, action0.getId()))))
                .andExpect(jsonPath("content.[0]._links.target.href", equalTo(generateTargetLink(knownTargetId))))
                .andExpect(jsonPath("content.[0]._links.distributionset.href", equalTo(generateDistributionSetLink(action0))))

                // verify collection properties
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    /**
     * Verifies that the get request for actions returns an empty collection if no assignments have been done yet.
     */
    @Test
    void getActionsWithEmptyResult() throws Exception {
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(0)))
                .andExpect(jsonPath("content", hasSize(0)))
                .andExpect(jsonPath("total", equalTo(0)));
    }

    /**
     * Verifies paging is respected as expected.
     */
    @Test
    void getMultipleActionsWithPagingLimitRequestParameter() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        // page 1: one entry
        final Action action0 = actions.get(0);
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())

                // verify action 0
                .andExpect(jsonPath("content.[0].id", equalTo(action0.getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].active", equalTo(true)))
                .andExpect(jsonPath("content.[0].status", equalTo(action0.getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionLink(knownTargetId, action0.getId()))))

                // verify collection properties
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // page 2: one entry
        final Action action1 = actions.get(1);
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())

                // verify action 1
                .andExpect(jsonPath("content.[0].id", equalTo(action1.getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("update")))
                .andExpect(jsonPath("content.[0].active", equalTo(true)))
                .andExpect(jsonPath("content.[0].status", equalTo(action1.getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("content.[0]._links.self.href", equalTo(generateActionLink(knownTargetId, action1.getId()))))

                // verify collection properties
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    /**
     * Verifies that the actions resource is read-only.
     */
    @Test
    void invalidRequestsOnActionResource() throws Exception {
        final String knownTargetId = "targetId";

        generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        // not allowed methods
        mvc.perform(post(ACTION_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());
        mvc.perform(put(ACTION_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());
    }

    /**
     * Verifies that the correct action is returned
     */
    @Test
    void shouldRetrieveCorrectActionById() throws Exception {
        final String knownTargetId = "targetId";

        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        final Long actionId = actions.get(0).getId();

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "/" + actionId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ACTION_ID, equalTo(actionId.intValue())));
    }

    /**
     * Verifies that NOT_FOUND is returned when there is no such action.
     */
    @Test
    void requestActionThatDoesNotExistsLeadsToNotFound() throws Exception {
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "/" + 101)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    void shouldSuccessfullyDeleteSingleAction() throws Exception {
        List<DistributionSetAssignmentResult> assignmentResults = createTargetsAndPerformAssignment(2);
        Action action1 = assignmentResults.get(0).getAssignedEntity().get(0);
        Action action2 = assignmentResults.get(1).getAssignedEntity().get(0);

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                // verify action 1
                .andExpect(jsonPath("content.[0].id", equalTo(action1.getId().intValue())))
                .andExpect(jsonPath("content.[1].id", equalTo(action2.getId().intValue())));

        mvc.perform(delete(ACTION_V1_REQUEST_MAPPING + "/" + action1.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "/" + action1.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSuccessfullyDeleteMultipleActions() throws Exception {
        final List<DistributionSetAssignmentResult> assignmentResults = createTargetsAndPerformAssignment(4);

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                // verify action 1
                .andExpect(jsonPath("content.[0].id", equalTo(assignmentResults.get(0).getAssignedEntity().get(0).getId().intValue())))
                .andExpect(jsonPath("content.[1].id", equalTo(assignmentResults.get(1).getAssignedEntity().get(0).getId().intValue())))
                .andExpect(jsonPath("content.[2].id", equalTo(assignmentResults.get(2).getAssignedEntity().get(0).getId().intValue())))
                .andExpect(jsonPath("content.[3].id", equalTo(assignmentResults.get(3).getAssignedEntity().get(0).getId().intValue())));

        final List<Long> actionIdsToDelete = new ArrayList<>();
        final long deletedActionId1 = assignmentResults.get(2).getAssignedEntity().get(0).getId();
        actionIdsToDelete.add(deletedActionId1);
        final long deletedActionId2 = assignmentResults.get(3).getAssignedEntity().get(0).getId();
        actionIdsToDelete.add(deletedActionId2);

        mvc.perform(delete(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)
                        .content(toJson(actionIdsToDelete)).contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "/" + deletedActionId1))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "/" + deletedActionId2))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        final Action deletedAction3 = assignmentResults.get(1).getAssignedEntity().get(0);
        final String rsql = "target.name==" + deletedAction3.getTarget().getName();

        mvc.perform(delete(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, rsql).contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "/" + deletedAction3.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "/" + assignmentResults.get(0).getAssignedEntity().get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    void shouldReceiveBadRequestWhenNeeded() throws Exception {
        // bad request on both empty parameters
        mvc.perform(delete(ACTION_V1_REQUEST_MAPPING).contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());

        // bad request when both parameters are present
        mvc.perform(delete(ACTION_V1_REQUEST_MAPPING).contentType(APPLICATION_JSON)
                        .param(REQUEST_PARAMETER_SEARCH, "target.name==test")
                        .content(toJson(List.of(1, 2, 3)))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private static String generateActionLink(final String targetId, final Long actionId) {
        return "http://localhost" + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING +
                "/" + targetId + "/" + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId;
    }

    private static String generateTargetLink(final String targetId) {
        return "http://localhost" + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetId;
    }

    private static String generateDistributionSetLink(final Action action) {
        return "http://localhost" + MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + action.getDistributionSet().getId();
    }

    private List<DistributionSetAssignmentResult> createTargetsAndPerformAssignment(int n) {
        final List<Target> targets = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            targets.add(testdataFactory.createTarget("target-" + i));
        }

        final List<DistributionSetAssignmentResult> results = new ArrayList<>();
        for (Target target : targets) {
            DistributionSet distributionSet = testdataFactory.createDistributionSet();
            results.add(assignDistributionSet(distributionSet.getId(), target.getControllerId()));
        }
        return results;
    }

    private void verifyResultsByTargetPropertyFilter(final Target target, final DistributionSet ds, final String rsqlTargetFilter)
            throws Exception {
        // pending status one result
        mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "?q=" + rsqlTargetFilter)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content.[0]._links.target.name", equalTo(target.getName())))
                .andExpect(jsonPath("content.[0]._links.distributionset.name", equalTo(ds.getName() + ":" + ds.getVersion())));
    }

    private void getActions(final boolean withExternalRef) throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        final Action action0 = actions.get(0);
        final Action action1 = actions.get(1);

        final List<String> externalRefs = new ArrayList<>(2);
        if (withExternalRef) {
            externalRefs.add("extRef#123_0");
            externalRefs.add("extRef#123_1");
            controllerManagement.updateActionExternalRef(action0.getId(), externalRefs.get(0));
            controllerManagement.updateActionExternalRef(action1.getId(), externalRefs.get(1));
        }

        final ResultActions resultActions =
                mvc.perform(get(ACTION_V1_REQUEST_MAPPING).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                        .andDo(MockMvcResultPrinter.print())
                        .andExpect(status().isOk())
                        // verify action 1
                        .andExpect(jsonPath("content.[1].id", equalTo(action1.getId().intValue())))
                        .andExpect(jsonPath("content.[1].type", equalTo("update")))
                        .andExpect(jsonPath("content.[1].active", equalTo(true)))
                        .andExpect(jsonPath("content.[1].status", equalTo("running")))
                        .andExpect(jsonPath("content.[1]._links.self.href",
                                equalTo(generateActionLink(knownTargetId, action1.getId()))))
                        // verify action 0
                        .andExpect(jsonPath("content.[0].id", equalTo(action0.getId().intValue())))
                        .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                        .andExpect(jsonPath("content.[0].active", equalTo(true)))
                        .andExpect(jsonPath("content.[1].status", equalTo("running")))
                        .andExpect(jsonPath("content.[0]._links.self.href", equalTo(generateActionLink(knownTargetId, action0.getId()))))
                        // verify collection properties
                        .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                        .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                        .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

        if (withExternalRef) {
            resultActions
                    .andExpect(jsonPath("content.[1].externalRef", equalTo(externalRefs.get(1))))
                    .andExpect(jsonPath("content.[0].externalRef", equalTo(externalRefs.get(0))));
        }
    }

    private void getAction(final boolean withExternalRef) throws Exception {
        final String knownTargetId = "targetId";
        // prepare ds
        final DistributionSet ds = testdataFactory.createDistributionSet();
        // rollout
        final Target target = testdataFactory.createTarget(knownTargetId);
        final Rollout rollout = testdataFactory.createRolloutByVariables(
                "TestRollout", "TestDesc", 1, "name==" + target.getName(), ds, "50", "5");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
        assertThat(actions).hasSize(1);
        final String externalRef = "externalRef#123";
        if (withExternalRef) {
            controllerManagement.updateActionExternalRef(actions.get(0).getId(), externalRef);
        }

        final ResultActions resultActions = mvc.perform(get(ACTION_V1_REQUEST_MAPPING + "/{actionId}", actions.get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        if (withExternalRef) {
            resultActions.andExpect(jsonPath("externalRef", equalTo(externalRef)));
        }
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverride(final String knownTargetId) {
        return generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(knownTargetId, null, null, null);
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(
            final String knownTargetId, final String schedule, final String duration, final String timezone) {
        final Target target = testdataFactory.createTarget(knownTargetId);

        final Iterator<DistributionSet> sets = testdataFactory.createDistributionSets(2).iterator();
        final DistributionSet one = sets.next();
        final DistributionSet two = sets.next();

        // Update
        if (schedule == null) {
            final List<Target> updatedTargets = assignDistributionSet(one, Collections.singletonList(target))
                    .getAssignedEntity().stream().map(Action::getTarget).toList();
            // 2nd update
            // sleep 10ms to ensure that we can sort by reportedAt
            await().until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSet(two, updatedTargets);
        } else {
            final List<Target> updatedTargets =
                    assignDistributionSetWithMaintenanceWindow(one.getId(), target.getControllerId(), schedule, duration, timezone)
                            .getAssignedEntity().stream()
                            .map(Action::getTarget)
                            .toList();
            // 2nd update
            // sleep 10ms to ensure that we can sort by reportedAt
            await().until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSetWithMaintenanceWindow(two.getId(), updatedTargets.get(0).getControllerId(), schedule, duration, timezone);
        }

        // two updates, one cancellation
        final List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();

        assertThat(actions).hasSize(2);
        return actions;
    }
}
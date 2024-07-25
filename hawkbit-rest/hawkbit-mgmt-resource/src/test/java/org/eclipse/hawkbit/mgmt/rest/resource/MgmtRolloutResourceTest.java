/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.RolloutTestApprovalStrategy;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

/**
 * Tests for covering the {@link MgmtRolloutResource}.
 */
@Feature("Component Tests - Management API")
@Story("Rollout Resource")
class MgmtRolloutResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String HREF_ROLLOUT_PREFIX = "http://localhost/rest/v1/rollouts/";

    @Autowired
    private RolloutManagement rolloutManagement;

    @Autowired
    private RolloutGroupManagement rolloutGroupManagement;

    @Autowired
    private RolloutTestApprovalStrategy approvalStrategy;

    @Test
    @Description("Testing that creating rollout with wrong body returns bad request")
    void createRolloutWithInvalidBodyReturnsBadRequest() throws Exception {
        mvc.perform(post("/rest/v1/rollouts").content("invalid body").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rest.body.notReadable")));
    }

    @Test
    @Description("Testing that creating rollout with insufficient permission returns forbidden")
    @WithUser(allSpPermissions = true, removeFromAllPermission = "CREATE_ROLLOUT")
    void createRolloutWithInsufficientPermissionReturnsForbidden() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout("name", "desc", 10, dsA.getId(), "name==test", null))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is(403)).andReturn();
    }

    @Test
    @Description("Testing that creating rollout with not existing distribution set returns not found")
    void createRolloutWithNotExistingDistributionSetReturnsNotFound() throws Exception {
        mvc.perform(post("/rest/v1/rollouts").content(JsonBuilder.rollout("name", "desc", 10, 1234, "name==test", null))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound()).andReturn();
    }

    @Test
    @Description("Testing that creating rollout with not valid formed target filter query returns bad request")
    void createRolloutWithNotWellFormedFilterReturnsBadRequest() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout("name", "desc", 5, dsA.getId(), "name=test", null))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rest.param.rsqlParamSyntax")))
                .andReturn();
    }

    @Test
    @Description("Ensures that the repository refuses to create rollout without a defined target filter set.")
    void missingTargetFilterQueryInRollout() throws Exception {

        final String targetFilterQuery = null;

        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout("rollout1", "desc", 10, dsA.getId(), targetFilterQuery, null))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rest.param.rsqlParamSyntax")))
                .andReturn();

    }

    @Test
    @Description("Testing that rollout can be created")
    void createRollout() throws Exception {
        testdataFactory.createTargets(20, "target", "rollout");

        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        postRollout("rollout1", 5, dsA.getId(), "id==target*", 20, Action.ActionType.FORCED);
    }

    @Test
    @Description("Verifies that rollout cannot be created if too many rollout groups are specified.")
    void createRolloutWithTooManyRolloutGroups() throws Exception {

        final int maxGroups = quotaManagement.getMaxRolloutGroupsPerRollout();
        testdataFactory.createTargets(20, "target", "rollout");

        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout("rollout1", "rollout1Desc", maxGroups + 1,
                        testdataFactory.createDistributionSet("ds").getId(), "id==target*",
                        new RolloutGroupConditionBuilder().withDefaults().build()))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

    }

    @Test
    @Description("Verifies that rollout cannot be created if the 'max targets per rollout group' quota would be violated for one of the groups.")
    void createRolloutFailsIfRolloutGroupQuotaIsViolated() throws Exception {

        final int maxTargets = quotaManagement.getMaxTargetsPerRolloutGroup();
        testdataFactory.createTargets(maxTargets + 1, "target", "rollout");

        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout("rollout1", "rollout1Desc", 1,
                        testdataFactory.createDistributionSet("ds").getId(), "id==target*",
                        new RolloutGroupConditionBuilder().withDefaults().build()))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

    }

    @Test
    @Description("Testing that rollout can be created with groups")
    void createRolloutWithGroupDefinitions() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("ro");

        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "ro-target", "rollout");

        final float percentTargetsInGroup1 = 20;
        final float percentTargetsInGroup2 = 100;

        final List<RolloutGroup> rolloutGroups = Arrays.asList(
                entityFactory.rolloutGroup().create().name("Group1").description("Group1desc")
                        .targetPercentage(percentTargetsInGroup1).build(),
                entityFactory.rolloutGroup().create().name("Group2").description("Group2desc")
                        .targetPercentage(percentTargetsInGroup2).build());

        final RolloutGroupConditions rolloutGroupConditions = new RolloutGroupConditionBuilder().withDefaults().build();

        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rolloutWithGroups("rollout2", "desc", null, dsA.getId(), "id==ro-target*",
                        rolloutGroupConditions, rolloutGroups))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andReturn();

    }

    @Test
    @Description("Testing dynamic rollouts with default dynamic group definition")
    void createDynamicRolloutWithDefaultDynamicGroupDefinition() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("ro");

        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "rollout-dynamic", "rollout");

        postRollout("rollout1", 1, dsA.getId(), "name==rollout*", 10, Action.ActionType.FORCED,
            null, null, true, null, 1);

        rolloutHandler.handleAll();

        final Rollout rollout = rolloutManagement.findByRsql(PAGE, "name==rollout1", false).getContent().get(0);

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        mvc.perform(get("/rest/v1/rollouts?representation=full").accept(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)))
            .andExpect(jsonPath("content[0].id", equalTo(rollout.getId().intValue())))
            .andExpect(jsonPath("content[0].name", equalTo("rollout1")))
            .andExpect(jsonPath("content[0].status", equalTo("running")))
            .andExpect(jsonPath("content[0].targetFilterQuery", equalTo("name==rollout*")))
            .andExpect(jsonPath("content[0].distributionSetId", equalTo(dsA.getId().intValue())))
            .andExpect(jsonPath("content[0].totalTargets", equalTo(10)))
            .andExpect(jsonPath("content[0].totalTargetsPerStatus").exists())
            .andExpect(jsonPath("content[0].totalTargetsPerStatus.running", equalTo(10)))
            .andExpect(jsonPath("content[0].totalGroups", equalTo(2)));

        final int amountOfDynamicTargets = 2;
        List<Target> additionalTargets = testdataFactory.createTargets(amountOfDynamicTargets, "rollout-dynamic-addition-", "rollout");

        rolloutHandler.handleAll();

        final List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent();
        groups.forEach(group -> {
            if (!group.getName().contains("-dynamic")) {
                rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE, group.getId()).forEach(target -> deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).forEach(action -> {

                    deploymentManagement.cancelAction(action.getId());
                    awaitActionStatus(action.getId(), Status.CANCELING);

                    deploymentManagement.forceQuitAction(action.getId());
                    awaitActionStatus(action.getId(), Status.CANCELED);
                }));
            }
        });

        awaitPendingDeviceInRollout(additionalTargets.get(0).getControllerId());

        mvc.perform(get("/rest/v1/rollouts?representation=full").accept(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)))
            .andExpect(jsonPath("content[0].id", equalTo(rollout.getId().intValue())))
            .andExpect(jsonPath("content[0].name", equalTo("rollout1")))
            .andExpect(jsonPath("content[0].status", equalTo("running")))
            .andExpect(jsonPath("content[0].targetFilterQuery", equalTo("name==rollout*")))
            .andExpect(jsonPath("content[0].distributionSetId", equalTo(dsA.getId().intValue())))
            .andExpect(jsonPath("content[0].totalTargets", equalTo(11)))
            .andExpect(jsonPath("content[0].totalTargetsPerStatus").exists())
            .andExpect(jsonPath("content[0].totalTargetsPerStatus.running", equalTo(1)))
            .andExpect(jsonPath("content[0].totalTargetsPerStatus.notstarted", equalTo(0)))
            .andExpect(jsonPath("content[0].totalTargetsPerStatus.scheduled", equalTo(0)))
            .andExpect(jsonPath("content[0].totalTargetsPerStatus.cancelled", equalTo(10)))
            .andExpect(jsonPath("content[0].totalTargetsPerStatus.finished", equalTo(0)))
            .andExpect(jsonPath("content[0].totalTargetsPerStatus.error", equalTo(0)))
            .andExpect(jsonPath("content[0].deleted", equalTo(false)))
            .andExpect(jsonPath("content[0].totalGroups", equalTo(3)))
            .andExpect(jsonPath("content[0]._links.self.href", startsWith(HREF_ROLLOUT_PREFIX)));
    }

    @Test
    @Description("Testing that no rollout with groups that have illegal percentages can be created")
    void createRolloutWithTooLowPercentage() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("ro2");

        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "ro-target", "rollout");

        final List<RolloutGroup> rolloutGroups = Arrays.asList(
                entityFactory.rolloutGroup().create().name("Group1").description("Group1desc").targetPercentage(0F)
                        .build(),
                entityFactory.rolloutGroup().create().name("Group2").description("Group2desc").targetPercentage(100F)
                        .build());

        final RolloutGroupConditions rolloutGroupConditions = new RolloutGroupConditionBuilder().withDefaults().build();

        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rolloutWithGroups("rollout4", "desc", null, dsA.getId(), "id==ro-target*",
                        rolloutGroupConditions, rolloutGroups))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.repo.constraintViolation")));

    }

    @Test
    @Description("Testing that no rollout with groups that have illegal percentages can be created")
    void createRolloutWithTooHighPercentage() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("ro2");

        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "ro-target", "rollout");

        final List<RolloutGroup> rolloutGroups = Arrays.asList(
                entityFactory.rolloutGroup().create().name("Group1").description("Group1desc").targetPercentage(1F)
                        .build(),
                entityFactory.rolloutGroup().create().name("Group2").description("Group2desc").targetPercentage(101F)
                        .build());

        final RolloutGroupConditions rolloutGroupConditions = new RolloutGroupConditionBuilder().withDefaults().build();

        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rolloutWithGroups("rollout4", "desc", null, dsA.getId(), "id==ro-target*",
                        rolloutGroupConditions, rolloutGroups))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.repo.constraintViolation")));

    }

    @Test
    @Description("Testing that rollout can be updated")
    void updateRollout() throws Exception {
        testdataFactory.createTargets(4, "rollout", "description");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        // create a running rollout for the created targets
        final Rollout rollout = rolloutManagement.create(
                entityFactory
                        .rollout()
                        .create()
                        .name("rollout1")
                        .distributionSetId(dsA.getId())
                        .targetFilterQuery("controllerId==rollout*"),
                4, false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());


        mvc.perform(put("/rest/v1/rollouts/" + rollout.getId()).content(
                                new JSONObject().put("name", "newName").put("description", "newDesc").toString()
                        ).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("newName")))
                .andExpect(jsonPath("$.description", equalTo("newDesc")));
    }

    @Test
    @Description("Testing the empty list is returned if no rollout exists")
    void noRolloutReturnsEmptyList() throws Exception {
        mvc.perform(get("/rest/v1/rollouts").accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(0))).andExpect(jsonPath("$.total", equalTo(0)));
    }

    @Test
    @Description("Retrieves single rollout from management API including extra data that is delivered only for single rollout access.")
    void retrieveSingleRollout() throws Exception {
        testdataFactory.createTargets(20, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = rolloutManagement.create(
                entityFactory.rollout().create().name("rollout1").distributionSetId(dsA.getId())
                        .targetFilterQuery("controllerId==rollout*"),
                4, false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());

        retrieveAndVerifyRolloutInCreating(dsA, rollout);
        retrieveAndVerifyRolloutInReady(rollout);
        retrieveAndVerifyRolloutInStarting(rollout);
        retrieveAndVerifyRolloutInRunning(rollout);
    }

    @Test
    @Description("Retrieves the list of rollouts with representation mode 'full'.")
    void retrieveRolloutListFullRepresentation() throws Exception {
        testdataFactory.createTargets(20, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create a running rollout for the created targets
        final Rollout rollout = rolloutManagement.create(
                entityFactory.rollout().create().name("rollout1").distributionSetId(dsA.getId())
                        .targetFilterQuery("controllerId==rollout*"),
                4, false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());

        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // request the list of rollouts with full representation
        mvc.perform(get("/rest/v1/rollouts?representation=full").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)))
                .andExpect(jsonPath("content[0].id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("content[0].name", equalTo("rollout1")))
                .andExpect(jsonPath("content[0].status", equalTo("running")))
                .andExpect(jsonPath("content[0].targetFilterQuery", equalTo("controllerId==rollout*")))
                .andExpect(jsonPath("content[0].distributionSetId", equalTo(dsA.getId().intValue())))
                .andExpect(jsonPath("content[0].totalTargets", equalTo(20)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus").exists())
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.running", equalTo(5)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.notstarted", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.scheduled", equalTo(15)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.error", equalTo(0)))
                .andExpect(jsonPath("content[0].deleted", equalTo(false)))
                .andExpect(jsonPath("content[0].totalGroups", equalTo(4)))
                .andExpect(jsonPath("content[0]._links.self.href", startsWith(HREF_ROLLOUT_PREFIX)));
    }

    @Test
    @Description("Retrieves the list of rollouts with representation mode 'full'.")
    void retrieveRolloutListFullRepresentationWithFilter() throws Exception {
        testdataFactory.createTargets(20, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create a running rollout for the created targets
        final Rollout rollout = rolloutManagement.create(
                entityFactory.rollout().create().name("rollout1").distributionSetId(dsA.getId())
                        .targetFilterQuery("controllerId==rollout*"),
                4, false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());

        rolloutManagement.create(
                entityFactory.rollout().create().name("rollout2").distributionSetId(dsA.getId())
                        .targetFilterQuery("controllerId==rollout*"),
                4, false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());

        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // request the list of rollouts with full representation
        mvc.perform(get("/rest/v1/rollouts?q=name==rollout1&representation=full").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)))
                .andExpect(jsonPath("content[0].id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("content[0].name", equalTo("rollout1")))
                .andExpect(jsonPath("content[0].status", equalTo("running")))
                .andExpect(jsonPath("content[0].targetFilterQuery", equalTo("controllerId==rollout*")))
                .andExpect(jsonPath("content[0].distributionSetId", equalTo(dsA.getId().intValue())))
                .andExpect(jsonPath("content[0].totalTargets", equalTo(20)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus").exists())
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.running", equalTo(5)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.notstarted", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.scheduled", equalTo(15)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.error", equalTo(0)))
                .andExpect(jsonPath("content[0].deleted", equalTo(false)))
                .andExpect(jsonPath("content[0].totalGroups", equalTo(4)))
                .andExpect(jsonPath("content[0]._links.self.href", startsWith(HREF_ROLLOUT_PREFIX)));
    }

    @Test
    @Description("Handles the GET request of retrieving a single rollout.")
    public void getRollout() throws Exception {
        enableMultiAssignments();
        approvalStrategy.setApprovalNeeded(true);
        try {
            approvalStrategy.setApproveDecidedBy("exampleUsername");

            testdataFactory.createTargets(4, "rollout", "description");
            final DistributionSet dsA = testdataFactory.createDistributionSet("");
            // create a running rollout for the created targets
            final Rollout rollout = rolloutManagement.create(
                    entityFactory
                            .rollout()
                            .create()
                            .name("rollout1")
                            .distributionSetId(dsA.getId())
                            .targetFilterQuery("controllerId==rollout*"),
                    4, false, new RolloutGroupConditionBuilder().withDefaults()
                            .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());

            rolloutHandler.handleAll();

            rolloutManagement.approveOrDeny(rollout.getId(), Rollout.ApprovalDecision.APPROVED, "Approved remark.");

            mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}", rollout.getId())
                            .accept(MediaTypes.HAL_JSON_VALUE))
                    .andDo(MockMvcResultPrinter.print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaTypes.HAL_JSON));
        } finally {
            approvalStrategy.setApprovalNeeded(false);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @Description("Verify the confirmation required flag is not part of the rollout parent entity")
    void verifyConfirmationFlagIsNeverPartOfRolloutEntity(final boolean confirmationFlowActive) throws Exception {
        testdataFactory.createTargets(20, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = rolloutManagement.create(
                entityFactory.rollout().create().name("rollout1").distributionSetId(dsA.getId())
                        .targetFilterQuery("controllerId==rollout*"),
                4, false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());

        mvc.perform(get("/rest/v1/rollouts/" + rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("$.confirmationRequired").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @Description("Verify the confirmation required flag will be set based on the feature state")
    void verifyConfirmationStateIfNotProvided(final boolean confirmationFlowActive) throws Exception {
        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        testdataFactory.createTargets(20, "target", "rollout");

        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        postRollout("rollout1", 5, dsA.getId(), "id==target*", 20, Action.ActionType.FORCED);

        final List<Rollout> content = rolloutManagement.findAll(PAGE, false).getContent();
        assertThat(content).hasSizeGreaterThan(0).allSatisfy(rollout -> {
            assertThat(rolloutGroupManagement.findByRollout(PAGE, rollout.getId()))
                    .describedAs("Confirmation required flag depends on feature active.")
                    .allMatch(group -> group.isConfirmationRequired() == confirmationFlowActive);
        });
    }

    @Test
    @Description("Confirmation required flag will be read from the Rollout, if specified.")
    void verifyRolloutGroupWillUseRolloutPropertyFirst() throws Exception {
        enableConfirmationFlow();

        final DistributionSet dsA = testdataFactory.createDistributionSet("ro");

        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "ro-target", "rollout");

        final float percentTargetsInGroup1 = 20;
        final float percentTargetsInGroup2 = 100;

        final RolloutGroupConditions rolloutGroupConditions = new RolloutGroupConditionBuilder().withDefaults().build();

        final List<String> rolloutGroups = Arrays.asList(
                JsonBuilder.rolloutGroup("Group1", "Group1desc", null, percentTargetsInGroup1, true,
                        rolloutGroupConditions),
                JsonBuilder.rolloutGroup("Group2", "Group1desc", null, percentTargetsInGroup2, null,
                        rolloutGroupConditions));

        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout("rollout2", "desc", null, dsA.getId(), "id==ro-target*",
                        rolloutGroupConditions, rolloutGroups, null, null, null, null, false))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andReturn();

        final List<Rollout> content = rolloutManagement.findAll(PAGE, false).getContent();
        assertThat(content).hasSize(1).allSatisfy(rollout -> {
            final List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent();
            assertThat(groups).hasSize(2).allMatch(group -> {
                if (group.getName().equals("Group1")) {
                    return group.isConfirmationRequired();
                } else if (group.getName().equals("Group2")) {
                    return !group.isConfirmationRequired();
                }
                return false;
            });
        });
    }

    @Test
    @Description("Confirmation required flag will be read from the tenant config (confirmation flow state), if never specified.")
    void verifyRolloutGroupWillUseConfigIfNotProvidedWithRollout() throws Exception {
        enableConfirmationFlow();

        final DistributionSet dsA = testdataFactory.createDistributionSet("ro");

        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "ro-target", "rollout");

        final float percentTargetsInGroup1 = 20;
        final float percentTargetsInGroup2 = 100;

        final RolloutGroupConditions rolloutGroupConditions = new RolloutGroupConditionBuilder().withDefaults().build();

        final List<String> rolloutGroups = Arrays.asList(
                JsonBuilder.rolloutGroup("Group1", "Group1desc", null, percentTargetsInGroup1, false,
                        rolloutGroupConditions),
                JsonBuilder.rolloutGroup("Group2", "Group1desc", null, percentTargetsInGroup2, null,
                        rolloutGroupConditions));

        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout("rollout2", "desc", null, dsA.getId(), "id==ro-target*",
                        rolloutGroupConditions, rolloutGroups, null, null, null, null, null))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andReturn();

        final List<Rollout> content = rolloutManagement.findAll(PAGE, false).getContent();
        assertThat(content).hasSize(1).allSatisfy(rollout -> {
            final List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent();
            assertThat(groups).hasSize(2).allMatch(group -> {
                if (group.getName().equals("Group1")) {
                    return !group.isConfirmationRequired();
                } else if (group.getName().equals("Group2")) {
                    return group.isConfirmationRequired();
                }
                return false;
            });
        });
    }

    @Step
    private void retrieveAndVerifyRolloutInRunning(final Rollout rollout) throws Exception {
        rolloutHandler.handleAll();

        mvc.perform(get("/rest/v1/rollouts/" + rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("$.name", equalTo("rollout1"))).andExpect(jsonPath("$.status", equalTo("running")))
                .andExpect(jsonPath("$.totalTargetsPerStatus.running", equalTo(5)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.notstarted", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.scheduled", equalTo(15)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.error", equalTo(0)))
                .andExpect(jsonPath("$.deleted", equalTo(false))).andExpect(jsonPath("$.totalGroups", equalTo(4)));
    }

    @Step
    private void retrieveAndVerifyRolloutInStarting(final Rollout rollout) throws Exception {
        rolloutManagement.start(rollout.getId());

        mvc.perform(get("/rest/v1/rollouts/" + rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("$.name", equalTo("rollout1"))).andExpect(jsonPath("$.status", equalTo("starting")))
                .andExpect(jsonPath("$.totalTargetsPerStatus.running", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.notstarted", equalTo(20)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.scheduled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.error", equalTo(0)))
                .andExpect(jsonPath("$.deleted", equalTo(false))).andExpect(jsonPath("$.totalGroups", equalTo(4)));
    }

    @Step
    private void retrieveAndVerifyRolloutInReady(final Rollout rollout) throws Exception {
        rolloutHandler.handleAll();

        mvc.perform(get("/rest/v1/rollouts/" + rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("$.name", equalTo("rollout1"))).andExpect(jsonPath("$.status", equalTo("ready")))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("bumlux")))
                .andExpect(jsonPath("$.lastModifiedAt", not(equalTo(0))))
                .andExpect(jsonPath("$.totalTargetsPerStatus.running", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.notstarted", equalTo(20)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.scheduled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.error", equalTo(0)))
                .andExpect(jsonPath("$.deleted", equalTo(false))).andExpect(jsonPath("$.totalGroups", equalTo(4)));
    }

    @Step
    private void retrieveAndVerifyRolloutInCreating(final DistributionSet dsA, final Rollout rollout) throws Exception {
        mvc.perform(get("/rest/v1/rollouts/" + rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("$.name", equalTo("rollout1"))).andExpect(jsonPath("$.status", equalTo("creating")))
                .andExpect(jsonPath("$.targetFilterQuery", equalTo("controllerId==rollout*")))
                .andExpect(jsonPath("$.distributionSetId", equalTo(dsA.getId().intValue())))
                .andExpect(jsonPath("$.createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("$.createdAt", not(equalTo(0))))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("bumlux")))
                .andExpect(jsonPath("$.lastModifiedAt", not(equalTo(0))))
                .andExpect(jsonPath("$.totalTargets", equalTo(20)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.running", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.notstarted", equalTo(20)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.scheduled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.error", equalTo(0)))
                .andExpect(jsonPath("$._links.self.href", startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath("$._links.start.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith("/start"))))
                .andExpect(jsonPath("$._links.pause.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith("/pause"))))
                .andExpect(
                        jsonPath("$._links.resume.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith("/resume"))))
                .andExpect(jsonPath("$._links.triggerNextGroup.href",
                        allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith("/triggerNextGroup"))))
                .andExpect(jsonPath("$._links.groups.href",
                        allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString("/deploygroups"))))
                .andExpect(jsonPath("$.deleted", equalTo(false))).andExpect(jsonPath("$.totalGroups", equalTo(4)));
    }

    @Test
    @Description("Testing that rollout paged list contains rollouts")
    void rolloutPagedListContainsAllRollouts() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        testdataFactory.createTargets(20, "target", "rollout");

        // setup - create 2 rollouts
        postRollout("rollout1", 5, dsA.getId(), "id==target*", 20, Action.ActionType.FORCED);
        postRollout("rollout2", 5, dsA.getId(), "id==target-0001*", 10, Action.ActionType.FORCED);

        // Run here, because Scheduler is disabled during tests
        rolloutHandler.handleAll();

        mvc.perform(get("/rest/v1/rollouts").accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(2))).andExpect(jsonPath("$.total", equalTo(2)))
                .andExpect(jsonPath("content[0].name", equalTo("rollout1")))
                .andExpect(jsonPath("content[0].status", equalTo("ready")))
                .andExpect(jsonPath("content[0].targetFilterQuery", equalTo("id==target*")))
                .andExpect(jsonPath("content[0].distributionSetId", equalTo(dsA.getId().intValue())))
                .andExpect(jsonPath("content[0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("content[0].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("content[0].lastModifiedBy", equalTo("bumlux")))
                .andExpect(jsonPath("content[0].lastModifiedAt", not(equalTo(0))))
                .andExpect(jsonPath("content[0].totalTargets", equalTo(20)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus").doesNotExist())
                .andExpect(jsonPath("content[0]._links.self.href", startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath("content[1].name", equalTo("rollout2")))
                .andExpect(jsonPath("content[1].status", equalTo("ready")))
                .andExpect(jsonPath("content[1].targetFilterQuery", equalTo("id==target-0001*")))
                .andExpect(jsonPath("content[1].distributionSetId", equalTo(dsA.getId().intValue())))
                .andExpect(jsonPath("content[1].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("content[1].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("content[1].lastModifiedBy", equalTo("bumlux")))
                .andExpect(jsonPath("content[1].lastModifiedAt", not(equalTo(0))))
                .andExpect(jsonPath("content[1].totalTargets", equalTo(10)))
                .andExpect(jsonPath("content[1].totalTargetsPerStatus").doesNotExist())
                .andExpect(jsonPath("content[1]._links.self.href", startsWith(HREF_ROLLOUT_PREFIX)));
    }

    @Test
    void retrieveRolloutWithStartAtAndForcedTimeResponseFields() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Long startAt = 21L;
        final Long forcetime = 45L;

        testdataFactory.createTargets(20, "target", "rollout");
        postRollout("rollout1", 5, dsA.getId(), "id==target*", 20, Action.ActionType.TIMEFORCED, startAt, forcetime);
        postRollout("rollout2", 5, dsA.getId(), "id==target-0001*", 10, Action.ActionType.TIMEFORCED, startAt,
                forcetime);

        // Run here, because Scheduler is disabled during tests
        rolloutHandler.handleAll();

        retrieveAndCompareRolloutsContent(dsA, "/rest/v1/rollouts", false, true, startAt, forcetime);
        retrieveAndCompareRolloutsContent(dsA, "/rest/v1/rollouts?representation=full", true, true, startAt, forcetime);
    }

    @Test
    @Description("Testing representation mode of rollout paged list")
    void rolloutPagedListRepresentationMode() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        testdataFactory.createTargets(20, "target", "rollout");

        // setup - create 2 rollouts
        postRollout("rollout1", 5, dsA.getId(), "id==target*", 20, Action.ActionType.FORCED);
        postRollout("rollout2", 5, dsA.getId(), "id==target-0001*", 10, Action.ActionType.FORCED);

        // Run here, because Scheduler is disabled during tests
        rolloutHandler.handleAll();

        retrieveAndCompareRolloutsContent(dsA, "/rest/v1/rollouts", false);
        retrieveAndCompareRolloutsContent(dsA, "/rest/v1/rollouts?representation=full", true);
    }

    @Test
    @Description("Testing that rollout paged list is limited by the query param limit")
    void rolloutPagedListIsLimitedToQueryParam() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        testdataFactory.createTargets(20, "target", "rollout");

        // setup - create 2 rollouts
        postRollout("rollout1", 5, dsA.getId(), "id==target*", 20, Action.ActionType.FORCED);
        postRollout("rollout2", 5, dsA.getId(), "id==target*", 20, Action.ActionType.FORCED);

        // Run here, because Scheduler is disabled during tests
        rolloutHandler.handleAll();

        mvc.perform(get("/rest/v1/rollouts?limit=1").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(2)));
    }

    @ParameterizedTest
    @MethodSource("confirmationOptions")
    @Description("Testing that rollout paged list is limited by the query param limit")
    void retrieveRolloutGroupsForSpecificRollout(final boolean confirmationFlowEnabled,
            final boolean confirmationRequired) throws Exception {
        // setup
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        if (confirmationFlowEnabled) {
            enableConfirmationFlow();
        }

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*",
                confirmationRequired);

        // retrieve rollout groups from created rollout
        mvc.perform(
                get("/rest/v1/rollouts/{rolloutId}/deploygroups", rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(4))).andExpect(jsonPath("$.total", equalTo(4)))
                .andExpect(jsonPath("$.content[0].status", equalTo("ready")))
                .andExpect(jsonPath("$.content[1].status", equalTo("ready")))
                .andExpect(jsonPath("$.content[2].status", equalTo("ready")))
                .andExpect(jsonPath("$.content[3].status", equalTo("ready")))
                .andExpect(isConfirmationFlowEnabled()
                        ? jsonPath("$.content[0].confirmationRequired", equalTo(confirmationRequired))
                        : jsonPath("confirmationRequired").doesNotExist())
                .andExpect(isConfirmationFlowEnabled()
                        ? jsonPath("$.content[1].confirmationRequired", equalTo(confirmationRequired))
                        : jsonPath("confirmationRequired").doesNotExist())
                .andExpect(isConfirmationFlowEnabled()
                        ? jsonPath("$.content[2].confirmationRequired", equalTo(confirmationRequired))
                        : jsonPath("confirmationRequired").doesNotExist())
                .andExpect(isConfirmationFlowEnabled()
                        ? jsonPath("$.content[3].confirmationRequired", equalTo(confirmationRequired))
                        : jsonPath("confirmationRequired").doesNotExist());
    }

    @Test
    @Description("The relation between deploy group and rollout should be validated.")
    void deployGroupsShouldValidateRelationWithRollout() throws Exception {
        // setup
        final int amountTargets = 8;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout1 = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*",
            false);

        final Rollout rollout2 = createRollout("rollout2", 1, dsA.getId(), "controllerId==rollout*",
            false);

        rolloutManagement.start(rollout1.getId());
        rolloutManagement.start(rollout2.getId());
        rolloutHandler.handleAll();

        final RolloutGroup firstGroup = rolloutGroupManagement
            .findByRollout(PageRequest.of(0, 1, Direction.ASC, "id"), rollout1.getId()).getContent().get(0);

        // make request for firstGroupId and the rolloutId of the second rollout (the one with no groups)
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}", rollout2.getId(), firstGroup.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Testing that starting the rollout switches the state to starting and then to running")
    void startingRolloutSwitchesIntoRunningState() throws Exception {
        // setup
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check rollout is in starting state
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}", rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("status", equalTo("starting")));

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // check rollout is in running state
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}", rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("status", equalTo("running")));
    }

    @Test
    @Description("Testing that pausing the rollout switches the state to paused")
    void pausingRolloutSwitchesIntoPausedState() throws Exception {
        // setup
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // pausing rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/pause", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check rollout is in running state
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}", rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("status", equalTo("paused")));
    }

    @Test
    @Description("Testing that resuming the rollout switches the state to running")
    void resumingRolloutSwitchesIntoRunningState() throws Exception {
        // setup
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // pausing rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/pause", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // resume rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/resume", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check rollout is in running state
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}", rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("status", equalTo("running")));
    }

    @Test
    @Description("Testing that an already started rollout cannot be started again and returns bad request")
    void startingAlreadyStartedRolloutReturnsBadRequest() throws Exception {
        // setup
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // starting rollout - already started should lead into bad request
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rollout.illegalstate")));
    }

    @Test
    @Description("Testing that resuming a rollout which is not started leads to bad request")
    void resumingNotStartedRolloutReturnsBadRequest() throws Exception {
        // setup
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // resume not yet started rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/resume", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rollout.illegalstate")));
    }

    @Test
    @Description("Testing that starting rollout the first rollout group is in running state")
    void startingRolloutFirstRolloutGroupIsInRunningState() throws Exception {
        // setup
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 2, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // retrieve rollout groups from created rollout - 2 groups exists
        // (amountTargets / groupSize = 2)
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups?sort=ID:ASC", rollout.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(2))).andExpect(jsonPath("$.total", equalTo(2)))
                .andExpect(jsonPath("$.content[0].status", equalTo("running")))
                .andExpect(jsonPath("$.content[1].status", equalTo("scheduled")));
    }

    @ParameterizedTest
    @MethodSource("confirmationOptions")
    @Description("Testing that a single rollout group can be retrieved")
    void retrieveSingleRolloutGroup(final boolean confirmationFlowEnabled, final boolean confirmationRequired)
            throws Exception {
        // setup
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        if (confirmationFlowEnabled) {
            enableConfirmationFlow();
        }

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = rolloutManagement.create(
                entityFactory.rollout().create().name("rollout1").distributionSetId(dsA.getId())
                        .targetFilterQuery("controllerId==rollout*"),
                4, confirmationRequired, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());

        final RolloutGroup firstGroup = rolloutGroupManagement
                .findByRollout(PageRequest.of(0, 1, Direction.ASC, "id"), rollout.getId()).getContent().get(0);
        final RolloutGroup secondGroup = rolloutGroupManagement
                .findByRollout(PageRequest.of(1, 1, Direction.ASC, "id"), rollout.getId()).getContent().get(0);

        retrieveAndVerifyRolloutGroupInCreating(rollout, firstGroup);
        retrieveAndVerifyRolloutGroupInReady(rollout, firstGroup);
        retrieveAndVerifyRolloutGroupInRunningAndScheduled(rollout, firstGroup, secondGroup, confirmationFlowEnabled,
                confirmationRequired);
    }

    private static Stream<Arguments> confirmationOptions() {
        return Stream.of(Arguments.of(true, false), Arguments.of(true, true), Arguments.of(false, true));
    }

    @Step
    private void retrieveAndVerifyRolloutGroupInRunningAndScheduled(final Rollout rollout,
            final RolloutGroup firstGroup, final RolloutGroup secondGroup, final boolean confirmationFlowEnabled,
            final boolean confirmationRequired) throws Exception {
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}", rollout.getId(), firstGroup.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("status", equalTo("running")))
                .andExpect(jsonPath("$.totalTargetsPerStatus.running", equalTo(5)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.notstarted", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.scheduled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.error", equalTo(0)))
                .andExpect(isConfirmationFlowEnabled() ? jsonPath("confirmationRequired", equalTo(confirmationRequired))
                        : jsonPath("confirmationRequired").doesNotExist());

        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}", rollout.getId(), secondGroup.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("status", equalTo("scheduled")))
                .andExpect(jsonPath("$.totalTargetsPerStatus.running", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.notstarted", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.scheduled", equalTo(5)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.error", equalTo(0)))
                .andExpect(isConfirmationFlowEnabled() ? jsonPath("confirmationRequired", equalTo(confirmationRequired))
                        : jsonPath("confirmationRequired").doesNotExist());
        ;
    }

    @Step
    private void retrieveAndVerifyRolloutGroupInReady(final Rollout rollout, final RolloutGroup firstGroup)
            throws Exception {
        rolloutHandler.handleAll();
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}", rollout.getId(), firstGroup.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("status", equalTo("ready")))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("bumlux")))
                .andExpect(jsonPath("$.lastModifiedAt", not(equalTo(0))))
                .andExpect(jsonPath("$.totalTargets", equalTo(5)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.running", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.notstarted", equalTo(5)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.scheduled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.error", equalTo(0)))
                .andExpect(isConfirmationFlowEnabled() ? jsonPath("confirmationRequired").exists()
                        : jsonPath("confirmationRequired").doesNotExist());
        ;
    }

    @Step
    private void retrieveAndVerifyRolloutGroupInCreating(final Rollout rollout, final RolloutGroup firstGroup)
            throws Exception {
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}", rollout.getId(), firstGroup.getId())
                .accept(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("id", equalTo(firstGroup.getId().intValue())))
                .andExpect(isConfirmationFlowEnabled() ? jsonPath("confirmationRequired").exists()
                        : jsonPath("confirmationRequired").doesNotExist())
                .andExpect(jsonPath("status", equalTo("creating"))).andExpect(jsonPath("name", endsWith("1")))
                .andExpect(jsonPath("description", endsWith("1")))
                .andExpect(jsonPath("$.targetFilterQuery", equalTo("")))
                .andExpect(jsonPath("$.targetPercentage", equalTo(25.0)))
                .andExpect(jsonPath("$.createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("$.createdAt", not(equalTo(0))))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo("bumlux")))
                .andExpect(jsonPath("$.lastModifiedAt", not(equalTo(0))))
                .andExpect(jsonPath("$.totalTargets", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.running", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.notstarted", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.scheduled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("$.totalTargetsPerStatus.error", equalTo(0)))
                .andExpect(jsonPath("$._links.self.href", equalTo(
                        HREF_ROLLOUT_PREFIX + rollout.getId() + "/deploygroups/" + firstGroup.getId().intValue())));
    }

    @Test
    @Description("Testing that the targets of rollout group can be retrieved")
    void retrieveTargetsFromRolloutGroup() throws Exception {
        // setup
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 2, dsA.getId(), "controllerId==rollout*");

        final RolloutGroup firstGroup = rolloutGroupManagement
                .findByRollout(PageRequest.of(0, 1, Direction.ASC, "id"), rollout.getId()).getContent().get(0);

        // retrieve targets from the first rollout group with known ID
        mvc.perform(
                get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}/targets", rollout.getId(), firstGroup.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(5))).andExpect(jsonPath("$.total", equalTo(5)));
    }

    @Test
    @Description("Handles the GET request of retrieving a all targets of a specific deploy group of a rollout.")
    public void getRolloutDeployGroupTargetsWithParameters() throws Exception {
        testdataFactory.createTargets(4, "rollout", "description");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Rollout rollout = createRollout("rollout1", 2, dsA.getId(), "controllerId==rollout*");
        final RolloutGroup firstRolloutGroup = rolloutGroupManagement
                .findByRollout(PageRequest.of(0, 1), rollout.getId()).getContent().get(0);

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deploygroups/{deployGroupId}/targets",
                                rollout.getId(), firstRolloutGroup.getId()).param("offset", "0").param("limit", "2")
                                .param("sort", "name:ASC").param("q", "controllerId==exampleTarget0")
                                .accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON));
    }

    @Test
    @Description("Testing that the targets of rollout group can be retrieved with rsql query param")
    void retrieveTargetsFromRolloutGroupWithQuery() throws Exception {
        // setup
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 2, dsA.getId(), "controllerId==rollout*");

        final RolloutGroup firstGroup = rolloutGroupManagement
                .findByRollout(PageRequest.of(0, 1, Direction.ASC, "id"), rollout.getId()).getContent().get(0);

        final String targetInGroup = rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE, firstGroup.getId())
                .getContent().get(0).getControllerId();

        // retrieve targets from the first rollout group with known ID
        mvc.perform(
                get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}/targets", rollout.getId(), firstGroup.getId())
                        .accept(MediaType.APPLICATION_JSON).param("q", "controllerId==" + targetInGroup))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)));
    }

    @Test
    @Description("Testing that the targets of rollout group can be retrieved after the rollout has been started")
    void retrieveTargetsFromRolloutGroupAfterRolloutIsStarted() throws Exception {
        // setup
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 2, dsA.getId(), "controllerId==rollout*");

        rolloutManagement.start(rollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        final RolloutGroup firstGroup = rolloutGroupManagement
                .findByRollout(PageRequest.of(0, 1, Direction.ASC, "id"), rollout.getId()).getContent().get(0);

        // retrieve targets from the first rollout group with known ID
        mvc.perform(
                get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}/targets", rollout.getId(), firstGroup.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(5))).andExpect(jsonPath("$.total", equalTo(5)));
    }

    @Test
    @Description("Start the rollout in async mode")
    void startingRolloutSwitchesIntoRunningStateAsync() throws Exception {

        final int amountTargets = 50;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // check if running
        awaitRunningState(rollout.getId());
    }

    private void awaitRunningState(final Long rolloutId) {
        Awaitility.await().atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofMillis(100)).with()
                .until(() -> SecurityContextSwitch
                        .runAsPrivileged(
                                () -> rolloutManagement.get(rolloutId).orElseThrow(NoSuchElementException::new))
                        .getStatus().equals(RolloutStatus.RUNNING));
    }

    private void awaitActionStatus(final Long actionId, final Status status) {
        Awaitility.await().atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofMillis(100)).with()
            .until(() -> SecurityContextSwitch
                .runAsPrivileged(
                    () -> deploymentManagement.findAction(actionId).orElseThrow(NoSuchElementException::new))
                .getStatus().equals(status));
    }

    private void awaitPendingDeviceInRollout(final String controllerId) {
        Awaitility.await().atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofMillis(100)).with()
            .until(() -> {
                Optional<Target> maybeTarget = SecurityContextSwitch.runAsPrivileged(() -> {
                    rolloutHandler.handleAll();
                    List<Target> targets = targetManagement.findByRsql(PAGE, "controllerId==" + controllerId).getContent();
                    if (targets.size() == 1) {
                        return Optional.of(targets.get(0));
                    } else {
                        return Optional.empty();
                    }
                });
                return maybeTarget.isPresent() && maybeTarget.get().getUpdateStatus().equals(TargetUpdateStatus.PENDING);
            });
    }

    @Test
    @Description("Deletion of a rollout")
    void deleteRollout() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "rolloutDelete", "rolloutDelete");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rolloutDelete", 4, dsA.getId(), "controllerId==rolloutDelete*");

        mvc.perform(delete("/rest/v1/rollouts/{rolloutid}", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertStatusIs(rollout, RolloutStatus.DELETING);
    }

    @Test
    @Description("Soft deletion of a rollout: soft deletion appears when already running rollout is being deleted")
    void deleteRunningRollout() throws Exception {
        final Rollout rollout = testdataFactory.createSoftDeletedRollout("softDeletedRollout");

        mvc.perform(get("/rest/v1/rollouts/{rolloutid}", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted", equalTo(true)));

        assertStatusIs(rollout, RolloutStatus.DELETED);
    }

    private void assertStatusIs(final Rollout rollout, final RolloutStatus expected) {
        final Optional<Rollout> updatedRollout = rolloutManagement.get(rollout.getId());
        assertThat(updatedRollout).get().extracting(Rollout::getStatus).isEqualTo(expected);
    }

    @Test
    @Description("Testing that rollout paged list with rsql parameter")
    void getRolloutWithRSQLParam() throws Exception {

        final int amountTargetsRollout1 = 25;
        final int amountTargetsRollout2 = 25;
        final int amountTargetsRollout3 = 25;
        final int amountTargetsOther = 25;
        testdataFactory.createTargets(amountTargetsRollout1, "rollout1", "rollout1");
        testdataFactory.createTargets(amountTargetsRollout2, "rollout2", "rollout2");
        testdataFactory.createTargets(amountTargetsRollout3, "rollout3", "rollout3");
        testdataFactory.createTargets(amountTargetsOther, "other1", "other1");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        createRollout("rollout1", 5, dsA.getId(), "controllerId==rollout1*");
        final Rollout rollout2 = createRollout("rollout2", 5, dsA.getId(), "controllerId==rollout2*");
        createRollout("rollout3", 5, dsA.getId(), "controllerId==rollout3*");
        createRollout("other1", 5, dsA.getId(), "controllerId==other1*");

        mvc.perform(get("/rest/v1/rollouts").param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==*2")
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)))
                .andExpect(jsonPath("$.content[0].name", equalTo(rollout2.getName())));

        mvc.perform(get("/rest/v1/rollouts").accept(MediaType.APPLICATION_JSON)
                .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==rollout*"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(3))).andExpect(jsonPath("$.total", equalTo(3)));

        mvc.perform(get("/rest/v1/rollouts").accept(MediaType.APPLICATION_JSON)
                .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==*1")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(2))).andExpect(jsonPath("$.total", equalTo(2)));

    }

    @Test
    @Description("Testing that rolloutgroup paged list with rsql parameter")
    void retrieveRolloutGroupsForSpecificRolloutWithRSQLParam() throws Exception {
        // setup
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // retrieve rollout groups from created rollout
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups", rollout.getId())
                .accept(MediaType.APPLICATION_JSON).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==group-1"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)))
                .andExpect(jsonPath("$.content[0].name", equalTo("group-1")))
                .andExpect(jsonPath("$.content[0].totalTargetsPerStatus").doesNotExist());

        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups", rollout.getId())
                .accept(MediaType.APPLICATION_JSON).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==group*"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(4))).andExpect(jsonPath("$.total", equalTo(4)));

        mvc.perform(
                get("/rest/v1/rollouts/{rolloutId}/deploygroups", rollout.getId()).accept(MediaType.APPLICATION_JSON)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==group-1,name==group-2"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(2))).andExpect(jsonPath("$.total", equalTo(2)))
                .andExpect(jsonPath("$.content[0].totalTargetsPerStatus").doesNotExist())
                .andExpect(jsonPath("$.content[1].totalTargetsPerStatus").doesNotExist());

    }

    @Test
    @Description("Testing that the list of rollout groups can be requested with representation mode 'full'.")
    void retrieveRolloutGroupsFullRepresentation() throws Exception {

        testdataFactory.createTargets(20, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // create a running rollout for the created targets
        final Rollout rollout = rolloutManagement.create(
                entityFactory.rollout().create().name("rollout1").distributionSetId(dsA.getId())
                        .targetFilterQuery("controllerId==rollout*"),
                4, false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());

        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // retrieve the rollout groups of the created rollout
        // filter for the first group by RSQL
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups", rollout.getId())
                .accept(MediaType.APPLICATION_JSON).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==group-1")
                .param("representation", "full")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.total", equalTo(1)))
                .andExpect(jsonPath("$.content[0].name", equalTo("group-1")))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus").exists())
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.running", equalTo(5)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.notstarted", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.scheduled", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.cancelled", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.error", equalTo(0)));
    }

    @Test
    @Description("Verifies that a DOWNLOAD_ONLY rollout is possible")
    void createDownloadOnlyRollout() throws Exception {
        testdataFactory.createTargets(20, "target", "rollout");

        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        postRollout("rollout1", 5, dsA.getId(), "id==target*", 20, Action.ActionType.DOWNLOAD_ONLY);
    }

    @Test
    @Description("A rollout create request containing a weight is always accepted when weight is valid.")
    void weightValidation() throws Exception {
        testdataFactory.createTargets(4, "rollout", "description");
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int weight = 66;

        final String invalideWeightRequest = JsonBuilder.rollout("withWeight", "d", 2, dsId, "id==rollout*",
                new RolloutGroupConditionBuilder().withDefaults().build(), null, null, Action.WEIGHT_MIN - 1, null,
                null, null);
        final String valideWeightRequest = JsonBuilder.rollout("withWeight", "d", 2, dsId, "id==rollout*",
                new RolloutGroupConditionBuilder().withDefaults().build(), null, null, weight, null, null, null);
        final String valideWeightRequestMultiAssignment = JsonBuilder.rollout("withWeightMultiAssignment", "d", 2, dsId, "id==rollout*",
                new RolloutGroupConditionBuilder().withDefaults().build(), null, null, weight, null, null, null);

        mvc.perform(post("/rest/v1/rollouts").content(valideWeightRequest).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());
        enableMultiAssignments();
        mvc.perform(post("/rest/v1/rollouts").content(invalideWeightRequest).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.repo.constraintViolation")));
        mvc.perform(post("/rest/v1/rollouts").content(valideWeightRequestMultiAssignment).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());

        final List<Rollout> rollouts = rolloutManagement.findAll(PAGE, false).getContent();
        assertThat(rollouts).hasSize(2);
        assertThat(rollouts.get(0).getWeight()).get().isEqualTo(weight);
    }

    @Test
    @Description("Handles the POST request of approving a rollout.")
    public void approveRollout() throws Exception {
        approvalStrategy.setApprovalNeeded(true);
        try {
            testdataFactory.createTargets(4, "rollout", "description");
            final DistributionSet dsA = testdataFactory.createDistributionSet("");
            final Rollout rollout = createRollout("rollout1", 3, dsA.getId(), "controllerId==rollout*", false);
            mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/approve", rollout.getId())
                            .accept(MediaTypes.HAL_JSON_VALUE))
                    .andDo(MockMvcResultPrinter.print())
                    .andExpect(status().isOk());
        } finally {
            approvalStrategy.setApprovalNeeded(false);
        }
    }

    @Test
    @Description("Handles the POST request of denying a rollout. Required Permission: " + SpPermission.APPROVE_ROLLOUT)
    public void denyRollout() throws Exception {
        approvalStrategy.setApprovalNeeded(true);
        try {
            testdataFactory.createTargets(4, "rollout", "description");
            final DistributionSet dsA = testdataFactory.createDistributionSet("");
            final Rollout rollout = createRollout("rollout1", 3, dsA.getId(), "controllerId==rollout*", false);
            mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deny", rollout.getId())
                            .accept(MediaTypes.HAL_JSON_VALUE))
                    .andDo(MockMvcResultPrinter.print())
                    .andExpect(status().isOk());
        } finally {
            approvalStrategy.setApprovalNeeded(false);
        }
    }

    @Test
    @Description("Check if approvalDecidedBy and approvalRemark are present when rollout is approved")
    public void validateIfApprovalFieldsArePresentAfterApproval() throws Exception {
        approvalStrategy.setApprovalNeeded(true);
        approvalStrategy.setApproveDecidedBy("testUser");
        final int amountTargets = 2;
        final String remark = "Some remark";
        final List<Target> targets = testdataFactory.createTargets(amountTargets, "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Rollout rollout = createRollout("rollout1", 3, dsA.getId(), "controllerId==rollout*", false);

        rolloutHandler.handleAll();

        mvc.perform(get("/rest/v1/rollouts/{rolloutid}", rollout.getId())).andDo(MockMvcResultPrinter.print())
            .andExpect(status().isOk()).andExpect(jsonPath("$.status", equalTo("waiting_for_approval")));

        rolloutManagement.approveOrDeny(rollout.getId(), Rollout.ApprovalDecision.APPROVED, remark);

        mvc.perform(get("/rest/v1/rollouts/{rolloutid}", rollout.getId())).andDo(MockMvcResultPrinter.print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", equalTo("ready")))
            .andExpect(jsonPath("$.approvalRemark", equalTo(remark)))
            .andExpect(jsonPath("$.approveDecidedBy", equalTo("testUser")));

        // revert
        approvalStrategy.setApprovalNeeded(false);
    }

    private void postRollout(final String name, final int groupSize, final Long distributionSetId,
            final String targetFilterQuery, final int targets, final Action.ActionType type) throws Exception {
        postRollout(name, groupSize, distributionSetId, targetFilterQuery, targets, type, null, null);
    }

    private void postRollout(final String name, final int groupSize, final Long distributionSetId,
            final String targetFilterQuery, final int targets, final Action.ActionType type, final Long startTime,
            final Long forceTime) throws Exception {
        postRollout(name, groupSize, distributionSetId, targetFilterQuery, targets, type, startTime, forceTime, false, null, 0);
    }

    private void postRollout(final String name, final int groupSize, final Long distributionSetId,
                             final String targetFilterQuery, final int targets, final Action.ActionType type, final Long startTime,
                             final Long forceTime, boolean isDynamic, String dynamicGroupSuffix, int dynamicGroupTargetsCount) throws Exception {
        final String actionType = MgmtRestModelMapper.convertActionType(type).getName();
        final String rollout = JsonBuilder.rollout(name, "desc", groupSize, distributionSetId, targetFilterQuery,
            new RolloutGroupConditionBuilder().withDefaults().build(), null, actionType, null, startTime, forceTime,
            null, isDynamic, dynamicGroupSuffix, dynamicGroupTargetsCount);

        mvc.perform(post("/rest/v1/rollouts").content(rollout).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", equalTo(name))).andExpect(jsonPath("$.status", equalTo("creating")))
            .andExpect(jsonPath("$.type", equalTo(actionType)))
            .andExpect(jsonPath("$.targetFilterQuery", equalTo(targetFilterQuery)))
            .andExpect(jsonPath("$.description", equalTo("desc")))
            .andExpect(jsonPath("$.distributionSetId", equalTo(distributionSetId.intValue())))
            .andExpect(jsonPath("$.createdBy", equalTo("bumlux")))
            .andExpect(jsonPath("$.createdAt", not(equalTo(0))))
            .andExpect(jsonPath("$.lastModifiedBy", equalTo("bumlux")))
            .andExpect(jsonPath("$.lastModifiedAt", not(equalTo(0))))
            .andExpect(jsonPath("$.totalTargets", equalTo(targets)))
            .andExpect(startTime != null ? jsonPath("$.startAt", equalTo(startTime.intValue()))
                : jsonPath("$.startAt").doesNotExist())
            .andExpect(forceTime != null ? jsonPath("$.forcetime", equalTo(forceTime.intValue()))
                : jsonPath("$.forcetime", equalTo(0)))
            .andExpect(jsonPath("$.totalTargetsPerStatus.running", equalTo(0)))
            .andExpect(jsonPath("$.totalTargetsPerStatus.notstarted", equalTo(targets)))
            .andExpect(jsonPath("$.totalTargetsPerStatus.scheduled", equalTo(0)))
            .andExpect(jsonPath("$.totalTargetsPerStatus.cancelled", equalTo(0)))
            .andExpect(jsonPath("$.totalTargetsPerStatus.finished", equalTo(0)))
            .andExpect(jsonPath("$.totalTargetsPerStatus.error", equalTo(0)))
            .andExpect(jsonPath("$._links.self.href", startsWith(HREF_ROLLOUT_PREFIX)))
            .andExpect(jsonPath("$._links.start.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith("/start"))))
            .andExpect(jsonPath("$._links.pause.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith("/pause"))))
            .andExpect(jsonPath("$.dynamic", equalTo(isDynamic)))
            .andExpect(
                jsonPath("$._links.resume.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith("/resume"))))
            .andExpect(jsonPath("$._links.groups.href",
                allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString("/deploygroups"))));
    }

    private Rollout createRollout(final String name, final int amountGroups, final long distributionSetId,
            final String targetFilterQuery) {
        return createRollout(name, amountGroups, distributionSetId, targetFilterQuery, false);
    }

    private Rollout createRollout(final String name, final int amountGroups, final long distributionSetId,
            final String targetFilterQuery, final boolean confirmationRequired) {
        final Rollout rollout = rolloutManagement.create(
                entityFactory.rollout().create().name(name).distributionSetId(distributionSetId).targetFilterQuery(targetFilterQuery),
                amountGroups, confirmationRequired, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());

        // Run here, because Scheduler is disabled during tests
        rolloutHandler.handleAll();

        return rolloutManagement.get(rollout.getId()).orElseThrow(NoSuchElementException::new);
    }

    @Test
    @Description("Trigger next rollout group")
    void triggeringNextGroupRollout() throws Exception {
        // setup
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/triggerNextGroup", rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        final List<RolloutGroupStatus> groupStatus = rolloutGroupManagement.findByRollout(PAGE, rollout.getId())
                .getContent().stream().map(RolloutGroup::getStatus).collect(Collectors.toList());
        assertThat(groupStatus).containsExactly(RolloutGroupStatus.RUNNING, RolloutGroupStatus.RUNNING,
                RolloutGroupStatus.SCHEDULED, RolloutGroupStatus.SCHEDULED);
    }

    @Test
    @Description("Trigger next rollout group if rollout is in wrong state")
    void triggeringNextGroupRolloutWrongState() throws Exception {
        final int amountTargets = 3;
        final List<Target> targets = testdataFactory.createTargets(amountTargets, "rollout");
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        // CREATING state
        final Rollout rollout = createRollout("rollout1", 3, dsA.getId(), "controllerId==rollout*", false);
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

        // READY state
        rolloutHandler.handleAll();
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

        // STARTING state
        rolloutManagement.start(rollout.getId());
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

        // RUNNING state
        rolloutHandler.handleAll();
        triggerNextGroupAndExpect(rollout, status().isOk());

        // PAUSED state
        rolloutManagement.pauseRollout(rollout.getId());
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

        rolloutManagement.resumeRollout(rollout.getId());
        triggerNextGroupAndExpect(rollout, status().isOk());

        // last group already running
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

        // FINISHED state
        setTargetsStatus(targets, Status.FINISHED);
        rolloutHandler.handleAll();
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

    }

    @Test
    @Description("Retry rollout test scenario")
    public void retryRolloutTest() throws Exception {

        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final List<Target> successTargets = testdataFactory.createTargets("retryRolloutTargetSuccess-", 6);
        final List<Target> failedTargets = testdataFactory.createTargets("retryRolloutTargetFailed-", 4);

        final List<Target> allTargets = new ArrayList<>(successTargets);
        allTargets.addAll(failedTargets);

        postRollout("rolloutToBeRetried", 1, dsA.getId(), "id==retryRolloutTarget*", 10, Action.ActionType.FORCED);

        Rollout rollout = rolloutManagement.getByName("rolloutToBeRetried").orElseThrow();

        // no scheduler so invoke here
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        // no scheduler so invoke here
        rolloutHandler.handleAll();


        testdataFactory.sendUpdateActionStatusToTargets(successTargets, Status.FINISHED, "Finished successfully!");
        testdataFactory.sendUpdateActionStatusToTargets(failedTargets, Status.ERROR, "Finished error!");

        rolloutHandler.handleAll();

        for (Target target : allTargets) {
            final List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
            for (Action action : actions) {
                if (action.getTarget().getControllerId().startsWith("retryRolloutTargetFailed")) {
                    Assertions.assertEquals(Status.ERROR, action.getStatus());
                } else {
                    Assertions.assertEquals(Status.FINISHED, action.getStatus());
                }
                Assertions.assertEquals(rollout.getId(), action.getRollout().getId());
            }
        }

        //retry rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/retry", rollout.getId())).andDo(MockMvcResultPrinter.print())
            .andExpect(status().is(201));

        //search for _retried suffix
        Rollout retriedRollout = rolloutManagement.getByName(rollout.getName() + "_retry").orElseThrow();
        //assert 4 targets involved
        rolloutHandler.handleAll();

        rolloutManagement.start(retriedRollout.getId());
        rolloutHandler.handleAll();

        for (Target target : failedTargets) {
            // for failed targets - check for 2 actions - one from old rollout and one from the retried
            List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
            Assertions.assertEquals(2, actions.size());
            Assertions.assertEquals(Status.ERROR, actions.get(0).getStatus());
            Assertions.assertEquals(rollout.getId(), actions.get(0).getRollout().getId());
            Assertions.assertEquals(Status.RUNNING, actions.get(1).getStatus());
            Assertions.assertEquals(retriedRollout.getId(), actions.get(1).getRollout().getId());
        }

        for (Target target : successTargets) {
            //ensure no other actions from the success targets are created
            List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();
            Assertions.assertEquals(1, actions.size());
            Assertions.assertEquals(rollout.getId(), actions.get(0).getRollout().getId());
        }
    }

    @Test
    @Description("Retrying a running rollout should not be allowed.")
    public void retryNotFinishedRolloutShouldNotBeAllowed() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        testdataFactory.createTargets("retryRolloutTarget-", 10);
        postRollout("rolloutToBeRetried", 1, dsA.getId(), "id==retryRolloutTarget*", 10, Action.ActionType.FORCED);
        Rollout rollout = rolloutManagement.getByName("rolloutToBeRetried").orElseThrow();
        // no scheduler so invoke here
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        // no scheduler so invoke here
        rolloutHandler.handleAll();

        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/retry", rollout.getId())).andDo(MockMvcResultPrinter.print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Retrying a non-existing rollout should lead to NOT FOUND.")
    public void retryNonExistingRolloutShouldLeadToNotFound() throws Exception {
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/retry", 6782623)).andDo(MockMvcResultPrinter.print())
            .andExpect(status().isNotFound());
    }

    private void triggerNextGroupAndExpect(final Rollout rollout, final ResultMatcher expect) throws Exception {
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/triggerNextGroup", rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(expect);
    }

    private void setTargetsStatus(final List<Target> targets, final Status status) {
        for (final Target target : targets) {
            final Long action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).toList().get(0)
                    .getId();
            controllerManagement
                    .addUpdateActionStatus(entityFactory.actionStatus().create(action).status(status).message("test"));
        }
    }

    private void retrieveAndCompareRolloutsContent(final DistributionSet dsA, final String urlTemplate,
            final boolean isFullRepresentation) throws Exception {
        retrieveAndCompareRolloutsContent(dsA, urlTemplate, isFullRepresentation, false, null, null);
    }

    private Rollout getRollout(final long rolloutId) {
        return rolloutManagement.get(rolloutId).orElseThrow(NoSuchElementException::new);
    }

    private void retrieveAndCompareRolloutsContent(final DistributionSet dsA, final String urlTemplate,
            final boolean isFullRepresentation, final boolean isStartTypeScheduled, final Long startAt,
            final Long forcetime) throws Exception {
        mvc.perform(get(urlTemplate).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content", hasSize(2))).andExpect(jsonPath("$.total", equalTo(2)))
                .andExpect(jsonPath("content[0].name", equalTo("rollout1")))
                .andExpect(jsonPath("content[0].status", equalTo("ready")))
                .andExpect(jsonPath("content[0].targetFilterQuery", equalTo("id==target*")))
                .andExpect(jsonPath("content[0].distributionSetId", equalTo(dsA.getId().intValue())))
                .andExpect(jsonPath("content[0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("content[0].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("content[0].lastModifiedBy", equalTo("bumlux")))
                .andExpect(jsonPath("content[0].lastModifiedAt", not(equalTo(0))))
                .andExpect(jsonPath("content[0].totalTargets", equalTo(20)))
                .andExpect(jsonPath("content[0].forcetime", equalTo(isStartTypeScheduled ? forcetime.intValue() : 0)))
                .andExpect(isFullRepresentation ? jsonPath("$.content[0].totalTargetsPerStatus").exists()
                        : jsonPath("content[0].totalTargetsPerStatus").doesNotExist())
                .andExpect(isFullRepresentation ? jsonPath("$.content[0].totalGroups", equalTo(5))
                        : jsonPath("content[0].totalGroups").doesNotExist())
                .andExpect(isFullRepresentation && isStartTypeScheduled
                        ? jsonPath("$.content[0].startAt", equalTo(startAt.intValue()))
                        : jsonPath("$.content[0].startAt").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[0]._links.start.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[0]._links.start.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[0]._links.pause.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[0]._links.pause.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[0]._links.resume.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[0]._links.resume.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[0]._links.triggerNextGroup.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[0]._links.triggerNextGroup.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[0]._links.approve.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[0]._links.approve.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[0]._links.deny.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[0]._links.deny.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[0]._links.groups.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[0]._links.groups.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[0]._links.distributionset.href",
                                startsWith("http://localhost/rest/v1/distributionsets/"))
                        : jsonPath("content[0]._links.distributionset.href").doesNotExist())
                .andExpect(jsonPath("content[0]._links.self.href", startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath("content[1].name", equalTo("rollout2")))
                .andExpect(jsonPath("content[1].status", equalTo("ready")))
                .andExpect(jsonPath("content[1].targetFilterQuery", equalTo("id==target-0001*")))
                .andExpect(jsonPath("content[1].distributionSetId", equalTo(dsA.getId().intValue())))
                .andExpect(jsonPath("content[1].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("content[1].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("content[1].lastModifiedBy", equalTo("bumlux")))
                .andExpect(jsonPath("content[1].lastModifiedAt", not(equalTo(0))))
                .andExpect(jsonPath("content[1].totalTargets", equalTo(10)))
                .andExpect(jsonPath("content[1].forcetime", equalTo(isStartTypeScheduled ? forcetime.intValue() : 0)))
                .andExpect(isFullRepresentation ? jsonPath("$.content[1].totalTargetsPerStatus").exists()
                        : jsonPath("content[1].totalTargetsPerStatus").doesNotExist())
                .andExpect(isFullRepresentation ? jsonPath("$.content[1].totalGroups", equalTo(5))
                        : jsonPath("content[1].totalGroups").doesNotExist())
                .andExpect(isFullRepresentation && isStartTypeScheduled
                        ? jsonPath("content[1].startAt", equalTo(startAt.intValue()))
                        : jsonPath("content[1].startAt").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[1]._links.start.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[1]._links.start.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[1]._links.pause.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[1]._links.pause.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[1]._links.resume.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[1]._links.resume.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[1]._links.triggerNextGroup.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[1]._links.triggerNextGroup.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[1]._links.approve.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[1]._links.approve.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[1]._links.deny.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[1]._links.deny.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[1]._links.groups.href", startsWith(HREF_ROLLOUT_PREFIX))
                        : jsonPath("content[1]._links.groups.href").doesNotExist())
                .andExpect(isFullRepresentation
                        ? jsonPath("$.content[1]._links.distributionset.href",
                                startsWith("http://localhost/rest/v1/distributionsets/"))
                        : jsonPath("content[1]._links.distributionset.href").doesNotExist())
                .andExpect(jsonPath("content[1]._links.self.href", startsWith(HREF_ROLLOUT_PREFIX)));
    }

}

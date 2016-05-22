/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.rest.util.SuccessCondition;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Tests for covering the {@link MgmtRolloutResource}.
 */
@Features("Component Tests - Management API")
@Stories("Rollout Resource")
public class MgmtRolloutResourceTest extends AbstractRestIntegrationTest {

    @Autowired
    private RolloutManagement rolloutManagement;

    @Autowired
    private RolloutGroupManagement rolloutGroupManagement;

    @Test
    @Description("Testing that creating rollout with wrong body returns bad request")
    public void createRolloutWithInvalidBodyReturnsBadRequest() throws Exception {
        mvc.perform(post("/rest/v1/rollouts").content("invalid body").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rest.body.notReadable")));
    }

    @Test
    @Description("Testing that creating rollout with insufficient permission returns forbidden")
    @WithUser(allSpPermissions = true, removeFromAllPermission = "ROLLOUT_MANAGEMENT")
    public void createRolloutWithInsufficientPermissionReturnsForbidden() throws Exception {
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout("name", "desc", 10, dsA.getId(), "name==test", null))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is(403)).andReturn();
    }

    @Test
    @Description("Testing that creating rollout with not exisiting distribution set returns not found")
    public void createRolloutWithNotExistingDistributionSetReturnsNotFound() throws Exception {
        mvc.perform(post("/rest/v1/rollouts").content(JsonBuilder.rollout("name", "desc", 10, 1234, "name==test", null))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound()).andReturn();
    }

    @Test
    @Description("Testing that creating rollout with not valid formed target filter query returns bad request")
    public void createRolloutWithNotWellFormedFilterReturnsBadRequest() throws Exception {
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout("name", "desc", 10, dsA.getId(), "name=test", null))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rest.param.rsqlParamSyntax")))
                .andReturn();
    }

    @Description("Ensures that the repository refuses to create rollout without a defined target filter set.")
    public void missingTargetFilterQueryInRollout() throws Exception {

        final String targetFilterQuery = null;

        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout("rollout1", "desc", 10, dsA.getId(), targetFilterQuery, null))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rest.param.rsqlParamSyntax")))
                .andReturn();

    }

    @Test
    @Description("Testing that rollout can be created")
    public void createRollout() throws Exception {
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        postRollout("rollout1", 10, dsA.getId(), "name==target1");
    }

    @Test
    @Description("Testing the empty list is returned if no rollout exists")
    public void noRolloutReturnsEmptyList() throws Exception {
        mvc.perform(get("/rest/v1/rollouts")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(0))).andExpect(jsonPath("$total", equalTo(0)));
    }

    @Test
    @Description("Testing that rollout paged list contains rollouts")
    public void rolloutPagedListContainsAllRollouts() throws Exception {
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        // setup - create 2 rollouts
        postRollout("rollout1", 10, dsA.getId(), "name==target1");
        postRollout("rollout2", 5, dsA.getId(), "name==target2");

        mvc.perform(get("/rest/v1/rollouts")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(2))).andExpect(jsonPath("$total", equalTo(2)))
                .andExpect(jsonPath("content[0].name", equalTo("rollout1")))
                .andExpect(jsonPath("content[0].status", equalTo("ready")))
                .andExpect(jsonPath("content[0].targetFilterQuery", equalTo("name==target1")))
                .andExpect(jsonPath("content[0].distributionSetId", equalTo(dsA.getId().intValue())))
                .andExpect(jsonPath("content[1].name", equalTo("rollout2")))
                .andExpect(jsonPath("content[1].status", equalTo("ready")))
                .andExpect(jsonPath("content[1].targetFilterQuery", equalTo("name==target2")))
                .andExpect(jsonPath("content[1].distributionSetId", equalTo(dsA.getId().intValue())));
    }

    @Test
    @Description("Testing that rollout paged list is limited by the query param limit")
    public void rolloutPagedListIsLimitedToQueryParam() throws Exception {
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        // setup - create 2 rollouts
        postRollout("rollout1", 10, dsA.getId(), "name==target1");
        postRollout("rollout2", 5, dsA.getId(), "name==target2");

        mvc.perform(get("/rest/v1/rollouts?limit=1")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(1))).andExpect(jsonPath("$total", equalTo(2)));
    }

    @Test
    @Description("Testing that rollout paged list is limited by the query param limit")
    public void retrieveRolloutGroupsForSpecificRollout() throws Exception {
        // setup
        final int amountTargets = 20;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // retrieve rollout groups from created rollout
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups", rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(4))).andExpect(jsonPath("$total", equalTo(4)))
                .andExpect(jsonPath("$content[0].status", equalTo("ready")))
                .andExpect(jsonPath("$content[1].status", equalTo("ready")))
                .andExpect(jsonPath("$content[2].status", equalTo("ready")))
                .andExpect(jsonPath("$content[3].status", equalTo("ready")));
    }

    @Test
    @Description("Testing that starting the rollout switches the state to running")
    public void startingRolloutSwitchesIntoRunningState() throws Exception {
        // setup
        final int amountTargets = 20;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check rollout is in running state
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("status", equalTo("running")));
    }

    @Test
    @Description("Testing that pausing the rollout switches the state to paused")
    public void pausingRolloutSwitchesIntoPausedState() throws Exception {
        // setup
        final int amountTargets = 20;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // pausing rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/pause", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check rollout is in running state
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("status", equalTo("paused")));
    }

    @Test
    @Description("Testing that resuming the rollout switches the state to running")
    public void resumingRolloutSwitchesIntoRunningState() throws Exception {
        // setup
        final int amountTargets = 20;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // pausing rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/pause", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // resume rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/resume", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check rollout is in running state
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("status", equalTo("running")));
    }

    @Test
    @Description("Testing that an already started rollout cannot be started again and returns bad request")
    public void startingAlreadyStartedRolloutReturnsBadRequest() throws Exception {
        // setup
        final int amountTargets = 20;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // starting rollout - already started should lead into bad request
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rollout.illegalstate")));
    }

    @Test
    @Description("Testing that resuming a rollout which is not started leads to bad request")
    public void resumingNotStartedRolloutReturnsBadRequest() throws Exception {
        // setup
        final int amountTargets = 20;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // resume not yet started rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/resume", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rollout.illegalstate")));
    }

    @Test
    @Description("Testing that starting rollout the first rollout group is in running state")
    public void startingRolloutFirstRolloutGroupIsInRunningState() throws Exception {
        // setup
        final int amountTargets = 10;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 2, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // retrieve rollout groups from created rollout - 2 groups exists
        // (amountTargets / groupSize = 2)
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups?sort=ID:ASC", rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(2))).andExpect(jsonPath("$total", equalTo(2)))
                .andExpect(jsonPath("$content[0].status", equalTo("running")))
                .andExpect(jsonPath("$content[1].status", equalTo("scheduled")));
    }

    @Test
    @Description("Testing that a single rollout group can be retrieved")
    public void retrieveSingleRolloutGroup() throws Exception {
        // setup
        final int amountTargets = 10;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        final RolloutGroup firstGroup = rolloutGroupManagement
                .findRolloutGroupsByRolloutId(rollout.getId(), new PageRequest(0, 1, Direction.ASC, "id")).getContent()
                .get(0);

        // retrieve single rollout group with known ID
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}", rollout.getId(), firstGroup.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id", equalTo(firstGroup.getId().intValue())))
                .andExpect(jsonPath("status", equalTo("ready"))).andExpect(jsonPath("name", notNullValue()));
    }

    @Test
    @Description("Testing that the targets of rollout group can be retrieved")
    public void retrieveTargetsFromRolloutGroup() throws Exception {
        // setup
        final int amountTargets = 10;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 2, dsA.getId(), "controllerId==rollout*");

        final RolloutGroup firstGroup = rolloutGroupManagement
                .findRolloutGroupsByRolloutId(rollout.getId(), new PageRequest(0, 1, Direction.ASC, "id")).getContent()
                .get(0);

        // retrieve targets from the first rollout group with known ID
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}/targets", rollout.getId(),
                firstGroup.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(5))).andExpect(jsonPath("$total", equalTo(5)));
    }

    @Test
    @Description("Testing that the targets of rollout group can be retrieved with rsql query param")
    public void retrieveTargetsFromRolloutGroupWithQuery() throws Exception {
        // setup
        final int amountTargets = 10;
        final List<Target> targets = targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 2, dsA.getId(), "controllerId==rollout*");

        final RolloutGroup firstGroup = rolloutGroupManagement
                .findRolloutGroupsByRolloutId(rollout.getId(), new PageRequest(0, 1, Direction.ASC, "id")).getContent()
                .get(0);

        // retrieve targets from the first rollout group with known ID
        mvc.perform(
                get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}/targets", rollout.getId(), firstGroup.getId())
                        .param("q", "controllerId==" + targets.get(0).getControllerId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(1))).andExpect(jsonPath("$total", equalTo(1)));
    }

    @Test
    @Description("Testing that the targets of rollout group can be retrieved after the rollout has been started")
    public void retrieveTargetsFromRolloutGroupAfterRolloutIsStarted() throws Exception {
        // setup
        final int amountTargets = 10;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 2, dsA.getId(), "controllerId==rollout*");

        rolloutManagement.startRollout(rollout);

        final RolloutGroup firstGroup = rolloutGroupManagement
                .findRolloutGroupsByRolloutId(rollout.getId(), new PageRequest(0, 1, Direction.ASC, "id")).getContent()
                .get(0);

        // retrieve targets from the first rollout group with known ID
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups/{groupId}/targets", rollout.getId(),
                firstGroup.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(5))).andExpect(jsonPath("$total", equalTo(5)));
    }

    @Test
    @Description("Start the rollout in async mode")
    public void startingRolloutSwitchesIntoRunningStateAsync() throws Exception {

        final int amountTargets = 1000;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // starting rollout
        mvc.perform(post("/rest/v1/rollouts/{rolloutId}/start", rollout.getId())
                .param(MgmtRestConstants.REQUEST_PARAMETER_ASYNC, "true")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check if running
        assertThat(doWithTimeout(() -> getRollout(rollout.getId()), result -> success(result), 60_000, 100))
                .isNotNull();
    }

    @Test
    @Description("Testing that rollout paged list with rsql parameter")
    public void getRolloutWithRSQLParam() throws Exception {

        final int amountTargetsRollout1 = 25;
        final int amountTargetsRollout2 = 25;
        final int amountTargetsRollout3 = 25;
        final int amountTargetsOther = 25;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargetsRollout1, "rollout1", "rollout1"));
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargetsRollout2, "rollout2", "rollout2"));
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargetsRollout3, "rollout3", "rollout3"));
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargetsOther, "other1", "other1"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        createRollout("rollout1", 5, dsA.getId(), "controllerId==rollout1*");
        final Rollout rollout2 = createRollout("rollout2", 5, dsA.getId(), "controllerId==rollout2*");
        createRollout("rollout3", 5, dsA.getId(), "controllerId==rollout3*");
        createRollout("other1", 5, dsA.getId(), "controllerId==other1*");

        mvc.perform(get("/rest/v1/rollouts").param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==*2"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(1))).andExpect(jsonPath("$total", equalTo(1)))
                .andExpect(jsonPath("$content[0].name", equalTo(rollout2.getName())));

        mvc.perform(get("/rest/v1/rollouts").param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==rollout*"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(3))).andExpect(jsonPath("$total", equalTo(3)));

        mvc.perform(get("/rest/v1/rollouts").param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==*1"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(2))).andExpect(jsonPath("$total", equalTo(2)));

    }

    @Test
    @Description("Testing that rolloutgroup paged list with rsql parameter")
    public void retrieveRolloutGroupsForSpecificRolloutWithRSQLParam() throws Exception {
        // setup
        final int amountTargets = 20;
        targetManagement.createTargets(TestDataUtil.buildTargetFixtures(amountTargets, "rollout", "rollout"));
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRollout("rollout1", 4, dsA.getId(), "controllerId==rollout*");

        // retrieve rollout groups from created rollout
        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups", rollout.getId())
                .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==group-1")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(1))).andExpect(jsonPath("$total", equalTo(1)))
                .andExpect(jsonPath("$content[0].name", equalTo("group-1")));

        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups", rollout.getId())
                .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==group*")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(4))).andExpect(jsonPath("$total", equalTo(4)));

        mvc.perform(get("/rest/v1/rollouts/{rolloutId}/deploygroups", rollout.getId())
                .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==group-1,name==group-2"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content", hasSize(2))).andExpect(jsonPath("$total", equalTo(2)));

    }

    protected <T> T doWithTimeout(final Callable<T> callable, final SuccessCondition<T> successCondition,
            final long timeout, final long pollInterval) throws Exception // NOPMD
    {

        if (pollInterval < 0) {
            throw new IllegalArgumentException("pollInterval must non negative");
        }

        long duration = 0;
        Exception exception = null;
        T returnValue = null;
        while (untilTimeoutReached(timeout, duration)) {
            try {
                returnValue = callable.call();
                // clear exception
                exception = null;
            } catch (final Exception ex) {
                exception = ex;
            }
            Thread.sleep(pollInterval);
            duration += pollInterval > 0 ? pollInterval : 1;
            if (exception == null && successCondition.success(returnValue)) {
                return returnValue;
            } else {
                returnValue = null;
            }
        }
        if (exception != null) {
            throw exception;
        }
        return returnValue;
    }

    protected boolean untilTimeoutReached(final long timeout, final long duration) {
        return duration <= timeout || timeout < 0;
    }

    private void postRollout(final String name, final int groupSize, final long distributionSetId,
            final String targetFilterQuery) throws Exception {
        mvc.perform(post("/rest/v1/rollouts")
                .content(JsonBuilder.rollout(name, "desc", groupSize, distributionSetId, targetFilterQuery, null))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andReturn();
    }

    private Rollout createRollout(final String name, final int amountGroups, final long distributionSetId,
            final String targetFilterQuery) {
        final Rollout rollout = rolloutManagement.generateRollout();
        rollout.setDistributionSet(distributionSetManagement.findDistributionSetById(distributionSetId));
        rollout.setName(name);
        rollout.setTargetFilterQuery(targetFilterQuery);
        return rolloutManagement.createRollout(rollout, amountGroups, new RolloutGroupConditionBuilder()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "100").build());
    }

    protected boolean success(final Rollout result) {
        if (null != result && result.getStatus() == RolloutStatus.RUNNING) {
            return true;
        }
        return false;
    }

    public Rollout getRollout(final Long rolloutId) throws Exception {
        return rolloutManagement.findRolloutById(rolloutId);
    }

}

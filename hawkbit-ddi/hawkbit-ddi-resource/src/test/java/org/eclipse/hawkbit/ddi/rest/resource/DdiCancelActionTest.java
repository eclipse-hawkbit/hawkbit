/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.ddi.json.model.DdiResult;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.integration.json.JsonPathUtils;

/**
 * Test cancel action from the controller.
 * <p/>
 * Feature: Component Tests - Direct Device Integration API<br/>
 * Story: Cancel Action Resource
 */
class DdiCancelActionTest extends AbstractDDiApiIntegrationTest {

    @Autowired
    ActionStatusRepository actionStatusRepository;

    /**
     * Tests that the cancel action resource can be used with CBOR.
     */
    @Test
    void cancelActionCbor() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        testdataFactory.createTarget();
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));
        final Action cancelAction = deploymentManagement.cancelAction(actionId);

        // check that we can get the cancel action as CBOR
        final byte[] result = mvc
                .perform(get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId(), tenantAware.getCurrentTenant())
                        .accept(DdiRestConstants.MEDIA_TYPE_CBOR))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(DdiRestConstants.MEDIA_TYPE_CBOR))
                .andReturn().getResponse()
                .getContentAsByteArray();
        assertThat(JsonPathUtils.<String> evaluate(cborToJson(result), "$.id"))
                .isEqualTo(String.valueOf(cancelAction.getId()));
        assertThat(JsonPathUtils.<String> evaluate(cborToJson(result), "$.cancelAction.stopId"))
                .isEqualTo(String.valueOf(actionId));

        // and submit feedback as CBOR
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(jsonToCbor(getJsonProceedingCancelActionFeedback()))
                        .contentType(DdiRestConstants.MEDIA_TYPE_CBOR).accept(DdiRestConstants.MEDIA_TYPE_CBOR))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Test of the controller can continue a started update even after a cancel command if it so desires.
     */
    @Test
    void rootRsCancelActionButContinueAnyway() throws Exception {
        // prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        implicitLock(ds);

        final Action cancelAction = deploymentManagement.cancelAction(actionId);

        // controller rejects cancellation
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonRejectedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        final long current = System.currentTimeMillis();

        // get update action anyway
        mvc.perform(
                        get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/deploymentBase/" + actionId,
                                tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(actionId))))
                .andExpect(jsonPath("$.deployment.download", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.update", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='jvm')].version",
                        contains(findFirstModuleByType(ds, runtimeType).orElseThrow().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].version",
                        contains(findFirstModuleByType(ds, osType).orElseThrow().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='bApp')].version",
                        contains(findFirstModuleByType(ds, appType).orElseThrow().getVersion())));

        // and finish it
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/deploymentBase/"
                        + actionId + "/feedback", tenantAware.getCurrentTenant()).content(
                                getJsonActionFeedback(DdiStatus.ExecutionStatus.CLOSED, DdiResult.FinalResult.NONE,
                                        Collections.singletonList("message")))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check database after test
        assertThat(deploymentManagement.findAssignedDistributionSet(TestdataFactory.DEFAULT_CONTROLLER_ID).orElseThrow())
                .isEqualTo(ds);
        assertThat(deploymentManagement.findInstalledDistributionSet(TestdataFactory.DEFAULT_CONTROLLER_ID).orElseThrow())
                .isEqualTo(ds);
        assertThat(
                targetManagement.getByControllerId(TestdataFactory.DEFAULT_CONTROLLER_ID).getInstallationDate())
                .isGreaterThanOrEqualTo(current);

    }

    /**
     * Test for cancel operation of an update action.
     */
    @Test
    void rootRsCancelAction() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final long timeBeforeFirstPoll = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/{controller}", tenantAware.getCurrentTenant(),
                        TestdataFactory.DEFAULT_CONTROLLER_ID).accept(MediaTypes.HAL_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                + TestdataFactory.DEFAULT_CONTROLLER_ID + "/deploymentBase/" + actionId)));
        final long timeAfterFirstPoll = System.currentTimeMillis() + 1;
        assertThat(targetManagement.getByControllerId(TestdataFactory.DEFAULT_CONTROLLER_ID).getLastTargetQuery())
                .isBetween(timeBeforeFirstPoll, timeAfterFirstPoll);

        // Retrieved is reported

        List<Action> activeActionsByTarget = deploymentManagement
                .findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent();

        assertThat(activeActionsByTarget).hasSize(1);
        assertThat(activeActionsByTarget.get(0).getStatus()).isEqualTo(Status.RUNNING);
        final Action cancelAction = deploymentManagement.cancelAction(actionId);

        activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)
                .getContent();

        // the canceled action should still be active!
        assertThat(cancelAction.isActive()).isTrue();
        assertThat(activeActionsByTarget).hasSize(1);
        assertThat(activeActionsByTarget.get(0).getStatus()).isEqualTo(Status.CANCELING);

        final long timeBefore2ndPoll = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/{controller}", tenantAware.getCurrentTenant(),
                        TestdataFactory.DEFAULT_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.cancelAction.href",
                        equalTo("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/" + cancelAction.getId())));
        final long timeAfter2ndPoll = System.currentTimeMillis() + 1;
        assertThat(targetManagement.getByControllerId(TestdataFactory.DEFAULT_CONTROLLER_ID).getLastTargetQuery())
                .isBetween(timeBefore2ndPoll, timeAfter2ndPoll);

        mvc.perform(get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId(), tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(cancelAction.getId()))))
                .andExpect(jsonPath("$.cancelAction.stopId", equalTo(String.valueOf(actionId))));
        assertThat(targetManagement.getByControllerId(TestdataFactory.DEFAULT_CONTROLLER_ID).getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());

        // controller confirmed cancelled action, should not be active anymore
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON)
                        .content(getJsonClosedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)
                .getContent();
        assertThat(activeActionsByTarget).isEmpty();
        final Action canceledAction = deploymentManagement.findAction(cancelAction.getId()).orElseThrow();
        assertThat(canceledAction.isActive()).isFalse();
        assertThat(canceledAction.getStatus()).isEqualTo(Status.CANCELED);

    }

    /**
     * Tests various bad requests and if the server handles them as expected.
     */
    @Test
    void badCancelAction() throws Exception {

        // not allowed methods
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/1",
                        tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/1",
                        tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/1",
                        tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // non existing target
        mvc.perform(get("/{tenant}/controller/v1/34534543/cancelAction/1", tenantAware.getCurrentTenant())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        createCancelAction("34534543");

        // wrong media type
        mvc.perform(get("/{tenant}/controller/v1/34534543/cancelAction/1", tenantAware.getCurrentTenant())
                        .accept(MediaType.APPLICATION_ATOM_XML))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotAcceptable());

    }

    /**
     * Tests the feedback channel of the cancel operation.
     */
    @Test
    void rootRsCancelActionFeedback() throws Exception {

        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));

        // cancel action manually
        final Action cancelAction = deploymentManagement.cancelAction(actionId);
        assertThat(countActionStatusAll()).isEqualTo(2);

        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonProceedingCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);
        assertThat(countActionStatusAll()).isEqualTo(3);

        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonResumedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);
        assertThat(countActionStatusAll()).isEqualTo(4);

        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonScheduledCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(countActionStatusAll()).isEqualTo(5);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);

        // cancellation canceled -> should remove the action from active
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonCanceledCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(countActionStatusAll()).isEqualTo(6);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);

        // cancellation rejected -> action still active until controller close
        // it
        // with finished or
        // error
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonRejectedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(countActionStatusAll()).isEqualTo(7);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);

        // update closed -> should remove the action from active
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/deploymentBase/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonClosedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(countActionStatusAll()).isEqualTo(8);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).isEmpty();
    }

    /**
     * Tests the feeback chanel of for multiple open cancel operations on the same target.
     */
    @SuppressWarnings("java:S5961")
    @Test
    void multipleCancelActionFeedback() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final DistributionSet ds3 = testdataFactory.createDistributionSet("3", true);

        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));
        implicitLock(ds);
        final Long actionId2 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));
        implicitLock(ds2);
        final Long actionId3 = getFirstAssignedActionId(assignDistributionSet(ds3.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));
        implicitLock(ds3);

        assertThat(countActionStatusAll()).isEqualTo(3);

        // 3 update actions, 0 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(3);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(3);
        final Action cancelAction = deploymentManagement.cancelAction(actionId);
        final Action cancelAction2 = deploymentManagement.cancelAction(actionId2);

        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(3);
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())).isEqualTo(3);
        mvc.perform(get(
                        "/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/" + cancelAction.getId(),
                        tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(cancelAction.getId()))))
                .andExpect(jsonPath("$.cancelAction.stopId", equalTo(String.valueOf(actionId))));
        assertThat(countActionStatusAll()).isEqualTo(6);

        mvc.perform(get("/{tenant}/controller/v1/{controllerId}", tenantAware.getCurrentTenant(),
                        TestdataFactory.DEFAULT_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.cancelAction.href",
                        equalTo("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/" + cancelAction.getId())));

        // now lets return feedback for the first cancelation
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonClosedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(countActionStatusAll()).isEqualTo(7);

        // 1 update actions, 1 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(2);
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())).isEqualTo(3);
        mvc.perform(get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction2.getId(), tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(cancelAction2.getId()))))
                .andExpect(jsonPath("$.cancelAction.stopId", equalTo(String.valueOf(actionId2))));
        assertThat(countActionStatusAll()).isEqualTo(8);

        mvc.perform(get("/{tenant}/controller/v1/{controller}", tenantAware.getCurrentTenant(),
                        TestdataFactory.DEFAULT_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.cancelAction.href",
                        equalTo("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/" + cancelAction2.getId())));

        // now lets return feedback for the second cancelation
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction2.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonClosedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(countActionStatusAll()).isEqualTo(9);

        assertThat(deploymentManagement.findAssignedDistributionSet(TestdataFactory.DEFAULT_CONTROLLER_ID).orElseThrow()).isEqualTo(ds3);
        mvc.perform(
                        get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/deploymentBase/" + actionId3,
                                tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(countActionStatusAll()).isEqualTo(10);

        // 1 update actions, 0 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);

        final Action cancelAction3 = deploymentManagement.cancelAction(actionId3);

        // action is in cancelling state
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).hasSize(1);
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())).isEqualTo(3);
        assertThat(deploymentManagement.findAssignedDistributionSet(TestdataFactory.DEFAULT_CONTROLLER_ID).orElseThrow()).isEqualTo(ds3);

        mvc.perform(get(
                "/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/" + cancelAction3.getId(),
                        tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(cancelAction3.getId()))))
                .andExpect(jsonPath("$.cancelAction.stopId", equalTo(String.valueOf(actionId3))));
        assertThat(countActionStatusAll()).isEqualTo(12);

        // now lets return feedback for the third cancelation
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction3.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonClosedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        assertThat(countActionStatusAll()).isEqualTo(13);

        // final status
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE)).isEmpty();
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())).isEqualTo(3);
    }

    /**
     * Tests the feeback channel closing for too many feedbacks, i.e. denial of service prevention.
     */
    @Test
    void tooMuchCancelActionFeedback() throws Exception {
        testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));

        final Action cancelAction = deploymentManagement.cancelAction(actionId);

        final String feedback = getJsonProceedingCancelActionFeedback();
        // assignDistributionSet creates an ActionStatus and cancel action
        // stores an action status, so
        // only 97 action status left
        for (int i = 0; i < 98; i++) {
            mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                            + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant()).content(feedback)
                            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant()).content(feedback)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * test the correct rejection of various invalid feedback requests
     */
    @Test
    void badCancelActionFeedback() throws Exception {
        final Action cancelAction = createCancelAction(TestdataFactory.DEFAULT_CONTROLLER_ID);
        createCancelAction("4715");

        // not allowed methods
        mvc.perform(put("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonClosedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // bad content type
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonClosedCancelActionFeedback()).contentType(MediaType.APPLICATION_ATOM_XML)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // bad body
        String invalidFeedback = "{\"status\":{\"execution\":\"546456456\",\"result\":{\"finished\":\"none\",\"progress\":{\"cnt\":2,\"of\":5}},\"details\":\"none\"]}}";
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant()).content(invalidFeedback)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // non existing target
        mvc.perform(post("/{tenant}/controller/v1/12345/cancelAction/" + cancelAction.getId() + "/feedback",
                        tenantAware.getCurrentTenant()).content(getJsonClosedCancelActionFeedback())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // invalid action
        invalidFeedback = "{\"id\":\"sdfsdfsdfs\",\"status\":{\"execution\":\"closed\",\"result\":{\"finished\":\"none\",\"progress\":{\"cnt\":2,\"of\":5}},\"details\":\"details\"]}}";
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant()).content(invalidFeedback)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // finaly, get it right :)
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                        + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(getJsonClosedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    private Action createCancelAction(final String targetid) {
        final DistributionSet ds = testdataFactory.createDistributionSet(targetid);
        final Target savedTarget = testdataFactory.createTarget(targetid);
        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget);
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds, toAssign));

        return deploymentManagement.cancelAction(actionId);
    }

    private long countActionStatusAll() {
        return actionStatusRepository.count();
    }
}

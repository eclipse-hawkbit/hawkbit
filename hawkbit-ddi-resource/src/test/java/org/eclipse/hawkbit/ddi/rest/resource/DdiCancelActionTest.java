/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.List;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test cancel action from the controller.
 */
@Features("Component Tests - Direct Device Integration API")
@Stories("Cancel Action Resource")
public class DdiCancelActionTest extends AbstractDDiApiIntegrationTest {

    @Test
    @Description("Test of the controller can continue a started update even after a cancel command if it so desires.")
    public void rootRsCancelActionButContinueAnyway() throws Exception {
        // prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = assignDistributionSet(ds.getId(), savedTarget.getControllerId()).getActions().get(0);

        final Action cancelAction = deploymentManagement.cancelAction(actionId);

        // controller rejects cancelation
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "rejected"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        final long current = System.currentTimeMillis();

        // get update action anyway
        mvc.perform(
                get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/deploymentBase/" + actionId,
                        tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(actionId))))
                .andExpect(jsonPath("$.deployment.download", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.update", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==jvm)].version",
                        contains(ds.findFirstModuleByType(runtimeType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==os)].version",
                        contains(ds.findFirstModuleByType(osType).get().getVersion())))
                .andExpect(jsonPath("$.deployment.chunks[?(@.part==bApp)].version",
                        contains(ds.findFirstModuleByType(appType).get().getVersion())));

        // and finish it
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/deploymentBase/"
                + actionId + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(actionId.toString(), "closed", "success"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // check database after test
        assertThat(deploymentManagement.getAssignedDistributionSet(TestdataFactory.DEFAULT_CONTROLLER_ID).get())
                .isEqualTo(ds);
        assertThat(deploymentManagement.getInstalledDistributionSet(TestdataFactory.DEFAULT_CONTROLLER_ID).get())
                .isEqualTo(ds);
        assertThat(
                targetManagement.getByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).get().getInstallationDate())
                        .isGreaterThanOrEqualTo(current);

    }

    @Test
    @Description("Test for cancel operation of a update action.")
    public void rootRsCancelAction() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = assignDistributionSet(ds.getId(), savedTarget.getControllerId()).getActions().get(0);

        long current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/{controller}", tenantAware.getCurrentTenant(),
                TestdataFactory.DEFAULT_CONTROLLER_ID).accept(APPLICATION_JSON_HAL_UTF))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                + TestdataFactory.DEFAULT_CONTROLLER_ID + "/deploymentBase/" + actionId)));
        Thread.sleep(1); // is required: otherwise processing the next line is
        // often too fast and
        // the following assert will fail
        assertThat(targetManagement.getByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(targetManagement.getByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        // Retrieved is reported

        List<Action> activeActionsByTarget = deploymentManagement
                .findActiveActionsByTarget(PAGE, savedTarget.getControllerId()).getContent();

        assertThat(activeActionsByTarget).hasSize(1);
        assertThat(activeActionsByTarget.get(0).getStatus()).isEqualTo(Status.RUNNING);
        final Action cancelAction = deploymentManagement.cancelAction(actionId);

        activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent();

        // the canceled action should still be active!
        assertThat(cancelAction.isActive()).isTrue();
        assertThat(activeActionsByTarget).hasSize(1);
        assertThat(activeActionsByTarget.get(0).getStatus()).isEqualTo(Status.CANCELING);

        current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/{controller}", tenantAware.getCurrentTenant(),
                TestdataFactory.DEFAULT_CONTROLLER_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.cancelAction.href",
                        equalTo("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/" + cancelAction.getId())));
        Thread.sleep(1); // is required: otherwise processing the next line is
        // often too fast and
        // the following assert will fail
        assertThat(targetManagement.getByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(targetManagement.getByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        current = System.currentTimeMillis();
        assertThat(targetManagement.getByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        mvc.perform(get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId(), tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(cancelAction.getId()))))
                .andExpect(jsonPath("$.cancelAction.stopId", equalTo(String.valueOf(actionId))));
        assertThat(targetManagement.getByControllerID(TestdataFactory.DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());

        // controller confirmed cancelled action, should not be active anymore
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON)
                        .content(JsonBuilder.cancelActionFeedback(actionId.toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent();
        assertThat(activeActionsByTarget).hasSize(0);
        final Action canceledAction = deploymentManagement.findAction(cancelAction.getId()).get();
        assertThat(canceledAction.isActive()).isFalse();
        assertThat(canceledAction.getStatus()).isEqualTo(Status.CANCELED);

    }

    @Test
    @Description("Tests various bad requests and if the server handles them as expected.")
    public void badCancelAction() throws Exception {

        // not allowed methods
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/1",
                tenantAware.getCurrentTenant())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/1",
                tenantAware.getCurrentTenant())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/1",
                tenantAware.getCurrentTenant())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // non existing target
        mvc.perform(get("/{tenant}/controller/v1/34534543/cancelAction/1", tenantAware.getCurrentTenant())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        createCancelAction("34534543");

        // wrong media type
        mvc.perform(get("/{tenant}/controller/v1/34534543/cancelAction/1", tenantAware.getCurrentTenant())
                .accept(MediaType.APPLICATION_ATOM_XML)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotAcceptable());

    }

    private Action createCancelAction(final String targetid) {
        final DistributionSet ds = testdataFactory.createDistributionSet(targetid);
        final Target savedTarget = testdataFactory.createTarget(targetid);
        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget);
        final Long actionId = assignDistributionSet(ds, toAssign).getActions().get(0);

        return deploymentManagement.cancelAction(actionId);
    }

    @Test
    @Description("Tests the feedback channel of the cancel operation.")
    public void rootRsCancelActionFeedback() throws Exception {

        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID).getActions()
                .get(0);

        // cancel action manually
        final Action cancelAction = deploymentManagement.cancelAction(actionId);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "proceeding"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(3);

        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "resumed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(4);

        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "scheduled"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(5);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        // cancellation canceled -> should remove the action from active
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "canceled"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(6);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        // cancellation rejected -> action still active until controller close
        // it
        // with finished or
        // error
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "rejected"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(7);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        // update closed -> should remove the action from active
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/deploymentBase/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(8);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(0);
    }

    @Test
    @Description("Tests the feeback chanel of for multiple open cancel operations on the same target.")
    public void multipleCancelActionFeedback() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final DistributionSet ds3 = testdataFactory.createDistributionSet("3", true);

        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID).getActions()
                .get(0);
        final Long actionId2 = assignDistributionSet(ds2.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID).getActions()
                .get(0);
        final Long actionId3 = assignDistributionSet(ds3.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID).getActions()
                .get(0);

        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(3);

        // 3 update actions, 0 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(3);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(3);
        final Action cancelAction = deploymentManagement.cancelAction(actionId);
        final Action cancelAction2 = deploymentManagement.cancelAction(actionId2);

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(3);
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())).isEqualTo(3);
        mvc.perform(get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId(), tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(cancelAction.getId()))))
                .andExpect(jsonPath("$.cancelAction.stopId", equalTo(String.valueOf(actionId))));
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(6);

        mvc.perform(get("/{tenant}/controller/v1/{controllerId}", tenantAware.getCurrentTenant(),
                TestdataFactory.DEFAULT_CONTROLLER_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.cancelAction.href",
                        equalTo("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/" + cancelAction.getId())));

        // now lets return feedback for the first cancelation
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(7);

        // 1 update actions, 1 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())).isEqualTo(3);
        mvc.perform(get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction2.getId(), tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(cancelAction2.getId()))))
                .andExpect(jsonPath("$.cancelAction.stopId", equalTo(String.valueOf(actionId2))));
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(8);

        mvc.perform(get("/{tenant}/controller/v1/{controller}", tenantAware.getCurrentTenant(),
                TestdataFactory.DEFAULT_CONTROLLER_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.cancelAction.href",
                        equalTo("http://localhost/" + tenantAware.getCurrentTenant() + "/controller/v1/"
                                + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/" + cancelAction2.getId())));

        // now lets return feedback for the second cancelation
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction2.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction2.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(9);

        assertThat(deploymentManagement.getAssignedDistributionSet(TestdataFactory.DEFAULT_CONTROLLER_ID).get())
                .isEqualTo(ds3);
        mvc.perform(
                get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/deploymentBase/" + actionId3,
                        tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(10);

        // 1 update actions, 0 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action cancelAction3 = deploymentManagement.cancelAction(actionId3);

        // action is in cancelling state
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())).isEqualTo(3);
        assertThat(deploymentManagement.getAssignedDistributionSet(TestdataFactory.DEFAULT_CONTROLLER_ID).get())
                .isEqualTo(ds3);

        mvc.perform(get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction3.getId(), tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", equalTo(String.valueOf(cancelAction3.getId()))))
                .andExpect(jsonPath("$.cancelAction.stopId", equalTo(String.valueOf(actionId3))));
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(12);

        // now lets return feedback for the third cancelation
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction3.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction3.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON_UTF8))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(13);

        // final status
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(0);
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())).isEqualTo(3);
    }

    @Test
    @Description("Tests the feeback channel closing for too many feedbacks, i.e. denial of service prevention.")
    public void tooMuchCancelActionFeedback() throws Exception {
        testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Long actionId = assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID).getActions()
                .get(0);

        final Action cancelAction = deploymentManagement.cancelAction(actionId);

        final String feedback = JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "proceeding");
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
                .andExpect(status().isForbidden());
    }

    @Test
    @Description("test the correct rejection of various invalid feedback requests")
    public void badCancelActionFeedback() throws Exception {
        final Action cancelAction = createCancelAction(TestdataFactory.DEFAULT_CONTROLLER_ID);
        final Action cancelAction2 = createCancelAction("4715");

        // not allowed methods
        mvc.perform(put("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(get("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // bad content type
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_ATOM_XML).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isUnsupportedMediaType());

        // bad body
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "546456456"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // non existing target
        mvc.perform(post("/{tenant}/controller/v1/12345/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // invalid action
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback("sdfsdfsdfs", "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback("1234", "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // right action but for wrong target
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction2.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // finally get it right :)
        mvc.perform(post("/{tenant}/controller/v1/" + TestdataFactory.DEFAULT_CONTROLLER_ID + "/cancelAction/"
                + cancelAction.getId() + "/feedback", tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

    }
}

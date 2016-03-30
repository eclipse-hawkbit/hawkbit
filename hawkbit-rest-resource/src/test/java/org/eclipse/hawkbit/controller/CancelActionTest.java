/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
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

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.MockMvcResultPrinter;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.resource.JsonBuilder;
import org.junit.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@ActiveProfiles({ "im", "test" })
@Features("Component Tests - Direct Device Integration API")
@Stories("Cancel Action Resource")
public class CancelActionTest extends AbstractIntegrationTest {

    @Test
    @Description("Test of the controller can continue a started update even after a cancel command if it so desires.")
    public void rootRsCancelActionButContinueAnyway() throws Exception {
        // prepare test data
        final Target target = new Target("4712");
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        final Target savedTarget = targetManagement.createTarget(target);

        final List<Target> toAssign = new ArrayList<Target>();
        toAssign.add(savedTarget);

        final Action updateAction = deploymentManagement
                .findActionWithDetails(deploymentManagement.assignDistributionSet(ds, toAssign).getActions().get(0));

        final Action cancelAction = deploymentManagement.cancelAction(updateAction,
                targetManagement.findTargetByControllerID(savedTarget.getControllerId()));

        // controller rejects cancelation
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "rejected"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        final long current = System.currentTimeMillis();

        // get update action anyway
        mvc.perform(get("/{tenant}/controller/v1/4712/deploymentBase/" + updateAction.getId(),
                tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$id", equalTo(String.valueOf(updateAction.getId()))))
                .andExpect(jsonPath("$deployment.download", equalTo("forced")))
                .andExpect(jsonPath("$deployment.update", equalTo("forced")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==jvm)][0].version",
                        equalTo(ds.findFirstModuleByType(runtimeType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].version",
                        equalTo(ds.findFirstModuleByType(osType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==bApp)][0].version",
                        equalTo(ds.findFirstModuleByType(appType).getVersion())));

        // and finish it
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + updateAction.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(
                        JsonBuilder.deploymentActionFeedback(updateAction.getId().toString(), "closed", "success"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // check database after test
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet().getId())
                .isEqualTo(ds.getId());
        assertThat(targetManagement.findTargetByControllerIDWithDetails("4712").getTargetInfo()
                .getInstalledDistributionSet().getId()).isEqualTo(ds.getId());
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getInstallationDate())
                .isGreaterThanOrEqualTo(current);

    }

    @Test
    @Description("Test for cancel operation of a update action.")
    public void rootRsCancelAction() throws Exception {
        final Target target = new Target("4712");
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        final Target savedTarget = targetManagement.createTarget(target);

        final List<Target> toAssign = new ArrayList<Target>();
        toAssign.add(savedTarget);

        final Action updateAction = deploymentManagement
                .findActionWithDetails(deploymentManagement.assignDistributionSet(ds, toAssign).getActions().get(0));

        long current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$_links.deploymentBase.href",
                        startsWith("http://localhost/" + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/deploymentBase/" + updateAction.getId())));
        Thread.sleep(1); // is required: otherwise processing the next line is
        // often too fast and
        // the following assert will fail
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        // Retrieved is reported

        List<Action> activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(savedTarget);

        assertThat(activeActionsByTarget).hasSize(1);
        assertThat(activeActionsByTarget.get(0).getStatus()).isEqualTo(Status.RUNNING);
        final Action cancelAction = deploymentManagement.cancelAction(updateAction,
                targetManagement.findTargetByControllerID(savedTarget.getControllerId()));

        activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(savedTarget);

        // the canceled action should still be active!
        assertThat(cancelAction.isActive()).isTrue();
        assertThat(activeActionsByTarget).hasSize(1);
        assertThat(activeActionsByTarget.get(0).getStatus()).isEqualTo(Status.CANCELING);

        current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$_links.cancelAction.href",
                        equalTo("http://localhost/" + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/cancelAction/" + cancelAction.getId())));
        Thread.sleep(1); // is required: otherwise processing the next line is
        // often too fast and
        // the following assert will fail
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        current = System.currentTimeMillis();
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        mvc.perform(
                get("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId(), tenantAware.getCurrentTenant())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$id", equalTo(String.valueOf(cancelAction.getId()))))
                .andExpect(jsonPath("$cancelAction.stopId", equalTo(String.valueOf(updateAction.getId()))));
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        // controller confirmed cancelled action, should not be active anymore
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON)
                        .content(JsonBuilder.cancelActionFeedback(updateAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        activeActionsByTarget = deploymentManagement.findActiveActionsByTarget(savedTarget);
        assertThat(activeActionsByTarget).hasSize(0);
        final Action canceledAction = deploymentManagement.findAction(cancelAction.getId());
        assertThat(canceledAction.isActive()).isFalse();
        assertThat(canceledAction.getStatus()).isEqualTo(Status.CANCELED);

    }

    @Test
    @Description("Tests various bad requests and if the server handles them as expected.")
    public void badCancelAction() throws Exception {

        // not allowed methods
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/{tenant}/controller/v1/4712/cancelAction/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/4712/cancelAction/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

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
        final Target target = new Target(targetid);
        final DistributionSet ds = TestDataUtil.generateDistributionSet(targetid, softwareManagement,
                distributionSetManagement);
        final Target savedTarget = targetManagement.createTarget(target);
        final List<Target> toAssign = new ArrayList<Target>();
        toAssign.add(savedTarget);
        final Action updateAction = deploymentManagement
                .findActionWithDetails(deploymentManagement.assignDistributionSet(ds, toAssign).getActions().get(0));

        return deploymentManagement.cancelAction(updateAction,
                targetManagement.findTargetByControllerID(savedTarget.getControllerId()));
    }

    @Test
    @Description("Tests the feedback channel of the cancel operation.")
    public void rootRsCancelActionFeedback() throws Exception {

        final Target target = new Target("4712");
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        final Target savedTarget = targetManagement.createTarget(target);

        final Action updateAction = deploymentManagement.findActionWithDetails(
                deploymentManagement.assignDistributionSet(ds.getId(), new String[] { "4712" }).getActions().get(0));

        // cancel action manually
        final Action cancelAction = deploymentManagement.cancelAction(updateAction,
                targetManagement.findTargetByControllerID(savedTarget.getControllerId()));
        assertThat(actionStatusRepository.findAll()).hasSize(2);

        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);
        long current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "proceeding"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);
        assertThat(actionStatusRepository.findAll()).hasSize(3);

        current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "resumed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);
        assertThat(actionStatusRepository.findAll()).hasSize(4);

        current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "scheduled"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(actionStatusRepository.findAll()).hasSize(5);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);

        // cancelation canceled -> should remove the action from active
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);
        current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "canceled"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(actionStatusRepository.findAll()).hasSize(6);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);

        // cancelation rejected -> action still active until controller close it
        // with finished or
        // error
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);
        current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "rejected"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(actionStatusRepository.findAll()).hasSize(7);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);

        // cancelaction closed -> should remove the action from active
        current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(actionStatusRepository.findAll()).hasSize(8);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(0);
    }

    @Test
    @Description("Tests the feeback chanel of for multiple open cancel operations on the same target.")
    public void multipleCancelActionFeedback() throws Exception {
        final Target target = new Target("4712");
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement, true);
        final DistributionSet ds2 = TestDataUtil.generateDistributionSet("2", softwareManagement,
                distributionSetManagement, true);
        final DistributionSet ds3 = TestDataUtil.generateDistributionSet("3", softwareManagement,
                distributionSetManagement, true);

        final Target savedTarget = targetManagement.createTarget(target);

        final Action updateAction = deploymentManagement.findActionWithDetails(
                deploymentManagement.assignDistributionSet(ds.getId(), new String[] { "4712" }).getActions().get(0));
        final Action updateAction2 = deploymentManagement.findActionWithDetails(
                deploymentManagement.assignDistributionSet(ds2.getId(), new String[] { "4712" }).getActions().get(0));
        final Action updateAction3 = deploymentManagement.findActionWithDetails(
                deploymentManagement.assignDistributionSet(ds3.getId(), new String[] { "4712" }).getActions().get(0));

        assertThat(actionStatusRepository.findAll()).hasSize(3);

        // 3 update actions, 0 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(3);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(3);
        final Action cancelAction = deploymentManagement.cancelAction(updateAction,
                targetManagement.findTargetByControllerID(savedTarget.getControllerId()));
        final Action cancelAction2 = deploymentManagement.cancelAction(updateAction2,
                targetManagement.findTargetByControllerID(savedTarget.getControllerId()));

        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(3);
        assertThat(deploymentManagement.findActionsByTarget(savedTarget)).hasSize(3);
        mvc.perform(
                get("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId(), tenantAware.getCurrentTenant())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$id", equalTo(String.valueOf(cancelAction.getId()))))
                .andExpect(jsonPath("$cancelAction.stopId", equalTo(String.valueOf(updateAction.getId()))));
        assertThat(actionStatusRepository.findAll()).hasSize(6);

        mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$_links.cancelAction.href",
                        equalTo("http://localhost/" + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/cancelAction/" + cancelAction.getId())));

        // now lets return feedback for the first cancelation
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(actionStatusRepository.findAll()).hasSize(7);

        // 1 update actions, 1 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(2);
        assertThat(deploymentManagement.findActionsByTarget(savedTarget)).hasSize(3);
        mvc.perform(get("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction2.getId(),
                tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$id", equalTo(String.valueOf(cancelAction2.getId()))))
                .andExpect(jsonPath("$cancelAction.stopId", equalTo(String.valueOf(updateAction2.getId()))));
        assertThat(actionStatusRepository.findAll()).hasSize(8);

        mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$_links.cancelAction.href",
                        equalTo("http://localhost/" + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/cancelAction/" + cancelAction2.getId())));

        // now lets return feedback for the second cancelation
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction2.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction2.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(actionStatusRepository.findAll()).hasSize(9);

        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet()).isEqualTo(ds3);
        mvc.perform(get("/{tenant}/controller/v1/4712/deploymentBase/" + updateAction3.getId(),
                tenantAware.getCurrentTenant())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(actionStatusRepository.findAll()).hasSize(10);

        // 1 update actions, 0 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);

        final Action cancelAction3 = deploymentManagement.cancelAction(actionRepository.findOne(updateAction3.getId()),
                targetManagement.findTargetByControllerID(savedTarget.getControllerId()));

        // action is in cancelling state
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);
        assertThat(deploymentManagement.findActionsByTarget(savedTarget)).hasSize(3);
        assertThat(targetManagement.findTargetByControllerID("4712").getAssignedDistributionSet()).isEqualTo(ds3);

        mvc.perform(get("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction3.getId(),
                tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$id", equalTo(String.valueOf(cancelAction3.getId()))))
                .andExpect(jsonPath("$cancelAction.stopId", equalTo(String.valueOf(updateAction3.getId()))));
        assertThat(actionStatusRepository.findAll()).hasSize(12);

        // now lets return feedback for the third cancelation
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction3.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction3.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(actionStatusRepository.findAll()).hasSize(13);

        // final status
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(0);
        assertThat(deploymentManagement.findActionsByTarget(savedTarget)).hasSize(3);
    }

    @Test
    @Description("Tests the feeback channel closing for too many feedbacks, i.e. denial of service prevention.")
    public void tooMuchCancelActionFeedback() throws Exception {
        final Target target = targetManagement.createTarget(new Target("4712"));
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        final List<Target> toAssign = new ArrayList<Target>();
        toAssign.add(target);

        final Action action = deploymentManagement.findActionWithDetails(
                deploymentManagement.assignDistributionSet(ds.getId(), new String[] { "4712" }).getActions().get(0));

        final Action cancelAction = deploymentManagement.cancelAction(action,
                targetManagement.findTargetByControllerID(target.getControllerId()));

        final String feedback = JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "proceeding");
        // assignDistributionSet creates an ActionStatus and cancel action
        // stores an action status, so
        // only 97 action status left
        for (int i = 0; i < 98; i++) {
            mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                    tenantAware.getCurrentTenant()).content(feedback).contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(feedback).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Description("test the correct rejection of various invalid feedback requests")
    public void badCancelActionFeedback() throws Exception {
        final Action cancelAction = createCancelAction("4712");
        final Action cancelAction2 = createCancelAction("4715");

        // not allowed methods
        mvc.perform(put("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(get("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // bad content type
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_ATOM_XML).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isUnsupportedMediaType());

        // bad body
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
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
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(JsonBuilder.cancelActionFeedback("sdfsdfsdfs", "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(JsonBuilder.cancelActionFeedback("1234", "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // right action but for wrong target
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction2.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // finally get it right :)
        mvc.perform(post("/{tenant}/controller/v1/4712/cancelAction/" + cancelAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.cancelActionFeedback(cancelAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

    }
}

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
import static org.hamcrest.Matchers.hasSize;
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
import java.util.concurrent.Callable;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.MockMvcResultPrinter;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithSpringAuthorityRule;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.rest.resource.JsonBuilder;
import org.eclipse.hawkbit.util.IpUtil;
import org.junit.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@ActiveProfiles({ "im", "test" })
@Features("Component Tests - Controller RESTful API")
@Stories("Root Poll Resource")
// TODO: fully document tests -> @Description for long text and reasonable
// method name as short text
public class RootControllerTest extends AbstractIntegrationTest {

    @Test()
    @Description("Ensures that software modules are not found, when target does not exists ")
    public void testSoftwareModulesIfTargetNotExists() throws Exception {
        final String targetNotExist = "targetNotExist";
        mvc.perform(get("/{tenant}/controller/v1/{targetNotExist}/softwaremodules/", tenantAware.getCurrentTenant(),
                targetNotExist)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

    @Test()
    @Description("Ensures that software modules are not found, when assigned distribution set exists with no modules")
    public void testSoftwareModulesEmptyIfDistributionSetNotExists() throws Exception {
        mvc.perform(get("/{tenant}/controller/v1/{targetExist}/softwaremodules/", tenantAware.getCurrentTenant(),
                TestDataUtil.createTarget(targetManagement).getName())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test()
    @Description("Ensures that software modules are found, when assigned distribution set exists with modules")
    public void testAvailableSoftwareModulesWithNoArtifacts() throws Exception {
        final Target target = TestDataUtil.createTarget(targetManagement);
        final DistributionSet distributionSet = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        deploymentManagement.assignDistributionSet(distributionSet.getId(), new String[] { target.getName() });

        final int modulesSize = distributionSet.getModules().size();

        mvc.perform(get("/{tenant}/controller/v1/{targetExist}/softwaremodules/", tenantAware.getCurrentTenant(),
                target.getName())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(modulesSize)));
    }

    @Test()
    @Description("Ensures that targets cannot be created e.g. in plug'n play scenarios when tenant does not exists but can be created if the tenant exists.")
    @WithUser(tenantId = "tenantDoesNotExists", allSpPermissions = true, authorities = "ROLE_CONTROLLER", autoCreateTenant = false)
    public void targetCannotBeRegisteredIfTenantDoesNotExistsButWhenExists() throws Exception {

        mvc.perform(get("/default-tenant/", tenantAware.getCurrentTenant())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // create tenant
        systemManagement.getTenantMetadata("tenantDoesNotExists");

        mvc.perform(get("/{}/controller/v1/aControllerId", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // delete tenant again
        systemManagement.deleteTenant("tenantDoesNotExists");

        mvc.perform(get("/{}/controller/v1/aControllerId", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

    }

    @Test
    @WithUser(principal = "knownPrincipal", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET,
            SpPermission.CREATE_TARGET })
    public void targetPollDoesNotModifyAuditData() throws Exception {
        // create target first with "knownPrincipal" user and audit data
        final String knownTargetControllerId = "target1";
        final String knownCreatedBy = "knownPrincipal";
        targetManagement.createTarget(new Target(knownTargetControllerId));
        final Target findTargetByControllerID = targetManagement.findTargetByControllerID(knownTargetControllerId);
        assertThat(findTargetByControllerID.getCreatedBy()).isEqualTo(knownCreatedBy);
        assertThat(findTargetByControllerID.getCreatedAt()).isNotNull();

        // make a poll, audit information should not be changed, run as
        // controller principal!
        securityRule.runAs(
                WithSpringAuthorityRule.withUser("controller", SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS),
                new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        mvc.perform(get("/{tenant}/controller/v1/" + knownTargetControllerId,
                                tenantAware.getCurrentTenant())).andDo(MockMvcResultPrinter.print())
                                .andExpect(status().isOk());
                        return null;
                    }
                });

        // verify that audit information has not changed
        final Target targetVerify = targetManagement.findTargetByControllerID(knownTargetControllerId);
        assertThat(targetVerify.getCreatedBy()).isEqualTo(findTargetByControllerID.getCreatedBy());
        assertThat(targetVerify.getCreatedAt()).isEqualTo(findTargetByControllerID.getCreatedAt());
        assertThat(targetVerify.getLastModifiedBy()).isEqualTo(findTargetByControllerID.getLastModifiedBy());
        assertThat(targetVerify.getLastModifiedAt()).isEqualTo(findTargetByControllerID.getLastModifiedAt());

    }

    @Test
    public void rootRsWithoutId() throws Exception {
        mvc.perform(get("/controller/v1/")).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    public void rootRsPlugAndPlay() throws Exception {

        final long current = System.currentTimeMillis();
        mvc.perform(get("/default-tenant/controller/v1/4711")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")));
        assertThat(targetManagement.findTargetByControllerID("4711").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        assertThat(targetRepository.findByControllerId("4711").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.REGISTERED);

        // not allowed methods
        mvc.perform(post("/default-tenant/controller/v1/4711")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/default-tenant/controller/v1/4711")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/default-tenant/controller/v1/4711")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void rootRsNotModified() throws Exception {
        final String etag = mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00"))).andReturn().getResponse()
                .getHeader("ETag");

        mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant()).header("If-None-Match", etag))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotModified());

        final Target target = targetRepository.findByControllerId("4711");
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        deploymentManagement.assignDistributionSet(ds.getId(), new String[] { "4711" });

        final Action updateAction = deploymentManagement.findActiveActionsByTarget(target).get(0);
        final String etagWithFirstUpdate = mvc
                .perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant())
                        .header("If-None-Match", etag).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$_links.deploymentBase.href",
                        startsWith("http://localhost/" + tenantAware.getCurrentTenant()
                                + "/controller/v1/4711/deploymentBase/" + updateAction.getId())))
                .andReturn().getResponse().getHeader("ETag");

        assertThat(etagWithFirstUpdate).isNotNull();

        mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant()).header("If-None-Match",
                etagWithFirstUpdate)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotModified());

        // now lets finish the update
        mvc.perform(post("/{tenant}/controller/v1/4711/deploymentBase/" + updateAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(updateAction.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // we are again at the original state
        mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant()).header("If-None-Match", etag))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotModified());

        // Now another deployment
        final DistributionSet ds2 = TestDataUtil.generateDistributionSet("2", softwareManagement,
                distributionSetManagement);

        deploymentManagement.assignDistributionSet(ds2.getId(), new String[] { "4711" });

        final Action updateAction2 = deploymentManagement.findActiveActionsByTarget(target).get(0);

        mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant())
                .header("If-None-Match", etagWithFirstUpdate).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$_links.deploymentBase.href",
                        startsWith("http://localhost/" + tenantAware.getCurrentTenant()
                                + "/controller/v1/4711/deploymentBase/" + updateAction2.getId())))
                .andReturn().getResponse().getHeader("ETag");
    }

    @Test
    public void rootRsPrecommissioned() throws Exception {
        final Target target = new Target("4711");
        targetManagement.createTarget(target);

        assertThat(targetRepository.findByControllerId("4711").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.UNKNOWN);

        final long current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/4711", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")));
        Thread.sleep(1); // is required: otherwise processing the next line is
                         // often too fast and
                         // the following assert will fail
        assertThat(targetManagement.findTargetByControllerID("4711").getTargetInfo().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(targetManagement.findTargetByControllerID("4711").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        assertThat(targetRepository.findByControllerId("4711").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.REGISTERED);
    }

    @Test
    public void rootRsPlugAndPlayIpAddress() throws Exception {
        // test
        final String knownControllerId1 = "0815";
        mvc.perform(get("/{tenant}/controller/v1/{controllerId}", tenantAware.getCurrentTenant(), knownControllerId1))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // verify
        final Target target = targetManagement.findTargetByControllerID(knownControllerId1);
        assertThat(target.getTargetInfo().getAddress()).isEqualTo(IpUtil.createHttpUri("127.0.0.1"));

    }

    @Test
    @Description("Controller trys to finish an update process after it has been finished by an error action status.")
    public void tryToFinishAnUpdateProcessAfterItHasBeenFinished() throws Exception {

        // mock
        final Target target = new Target("911");
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        Target savedTarget = targetManagement.createTarget(target);
        final List<Target> toAssign = new ArrayList<Target>();
        toAssign.add(savedTarget);
        savedTarget = deploymentManagement.assignDistributionSet(ds, toAssign).getAssignedTargets().iterator().next();
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);
        mvc.perform(post("/{tenant}/controller/v1/911/deploymentBase/" + savedAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(savedAction.getId().toString(), "proceeding"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(post("/{tenant}/controller/v1/911/deploymentBase/" + savedAction.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(
                        JsonBuilder.deploymentActionFeedback(savedAction.getId().toString(), "closed", "failure"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(post("/{tenant}/controller/v1/911/deploymentBase/" + savedAction.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(
                        JsonBuilder.deploymentActionFeedback(savedAction.getId().toString(), "closed", "success"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isGone());
    }

}

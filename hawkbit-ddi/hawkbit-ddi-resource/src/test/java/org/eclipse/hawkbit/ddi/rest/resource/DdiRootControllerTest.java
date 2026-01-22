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
import static org.eclipse.hawkbit.auth.SpPermission.TENANT_CONFIGURATION;
import static org.eclipse.hawkbit.auth.SpRole.CONTROLLER_ROLE_ANONYMOUS;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.callAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.getAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withController;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.ddi.json.model.DdiResult;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.TenantConfigurationDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationCreatedEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.utils.IpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Test the root controller resources.
 * <p/>
 * Feature: Component Tests - Direct Device Integration API<br/>
 * Story: Root Poll Resource
 */
class DdiRootControllerTest extends AbstractDDiApiIntegrationTest {

    private static final String TARGET_COMPLETED_INSTALLATION_MSG = "Target completed installation.";
    private static final String TARGET_PROCEEDING_INSTALLATION_MSG = "Target proceeding installation.";
    private static final String TARGET_SCHEDULED_INSTALLATION_MSG = "Target scheduled installation.";

    @Autowired
    private HawkbitSecurityProperties securityProperties;

    /**
     * Ensure that the root poll resource is available as CBOR
     */
    @Test
    void rootPollResourceCbor() throws Exception {
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), 4711).accept(DdiRestConstants.MEDIA_TYPE_APPLICATION_CBOR))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(content().contentType(DdiRestConstants.MEDIA_TYPE_APPLICATION_CBOR))
                .andExpect(status().isOk());
    }

    /**
     * Ensures that the API returns JSON when no Accept header is specified by the client.
     */
    @Test
    void apiReturnsJSONByDefault() throws Exception {
        final MvcResult result = mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), 4711))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andReturn();
        // verify that we did not specify a content-type in the request, in case there are any default values
        assertThat(result.getRequest().getHeader("Accept")).isNull();
    }

    /**
     * Ensures that target poll request does not change audit data on the entity.
     */
    @Test
    @WithUser(principal = "knownPrincipal", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.CREATE_TARGET })
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void targetPollDoesNotModifyAuditData() throws Exception {
        // create target first with "knownPrincipal" user and audit data
        final String knownTargetControllerId = "target1";
        final String knownCreatedBy = "knownPrincipal";
        testdataFactory.createTarget(knownTargetControllerId);
        final Target findTargetByControllerID = targetManagement.getByControllerId(knownTargetControllerId);
        assertThat(findTargetByControllerID.getCreatedBy()).isEqualTo(knownCreatedBy);
        // make a poll, audit information should not be changed, run as controller principal!
        callAs(withController("controller", CONTROLLER_ROLE_ANONYMOUS),
                () -> {
                    mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), knownTargetControllerId))
                            .andDo(MockMvcResultPrinter.print())
                            .andExpect(status().isOk());
                    return null;
                });
        // verify that audit information has not changed
        final Target targetVerify = targetManagement.getByControllerId(knownTargetControllerId);
        assertThat(targetVerify.getCreatedBy()).isEqualTo(findTargetByControllerID.getCreatedBy());
        assertThat(targetVerify.getCreatedAt()).isEqualTo(findTargetByControllerID.getCreatedAt());
        assertThat(targetVerify.getLastModifiedBy()).isEqualTo(findTargetByControllerID.getLastModifiedBy());
        assertThat(targetVerify.getLastModifiedAt()).isEqualTo(findTargetByControllerID.getLastModifiedAt());
    }

    /**
     * Ensures that server returns a not found response in case of empty controller ID.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void rootRsWithoutId() throws Exception {
        mvc.perform(get("/controller/v1/"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Ensures that the system creates a new target in plug and play manner, i.e. target is authenticated but does not exist yet.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void rootRsPlugAndPlay() throws Exception {
        final long current = System.currentTimeMillis();
        final String controllerId = "4711";

        mvc.perform(get(CONTROLLER_BASE, "default-tenant", controllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")));
        assertThat(targetManagement.getByControllerId(controllerId)).satisfies(target -> {
            assertThat(target.getLastTargetQuery()).isGreaterThanOrEqualTo(current);
            assertThat(target.getUpdateStatus()).isEqualTo(TargetUpdateStatus.REGISTERED);
        });

        // not allowed methods
        mvc.perform(post(CONTROLLER_BASE, "default-tenant", controllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(CONTROLLER_BASE, "default-tenant", controllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(CONTROLLER_BASE, "default-tenant", controllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Ensures that tenant specific polling time, which is saved in the db, is delivered to the controller.
     */
    @Test
    @WithUser(principal = "knownpricipal")
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1),
            @Expect(type = TenantConfigurationDeletedEvent.class, count = 1) })
    void pollWithModifiedGlobalPollingTime() throws Exception {
        withPollingTime("00:02:00", () -> callAs(
                withUser("controller", CONTROLLER_ROLE_ANONYMOUS),
                () -> {
                    mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), 4711))
                            .andDo(MockMvcResultPrinter.print())
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MediaTypes.HAL_JSON))
                            .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:02:00")));
                    return null;
                }));
    }

    /**
     * Ensures that tenant specific polling time, which is saved in the db, is delivered to the controller.
     */
    @Test
    @WithUser(principal = "knownpricipal")
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1), // assign to group
            @Expect(type = TargetPollEvent.class, count = 2),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1),
            @Expect(type = TenantConfigurationDeletedEvent.class, count = 1) })
    void pollWithModifiedWithOverridesGlobalPollingTime() throws Exception {
        SecurityContextSwitch.asPrivileged(() -> {
            final Target target = testdataFactory.createTarget("not4711");
            targetManagement.assignTargetsWithGroup("Europe", List.of(target.getControllerId()));
            return null;
        });

        withPollingTime("00:02:00, controllerid == 4711 -> 00:01:00, group == 'Europe' -> 00:05:05", () -> callAs(
                withUser("controller", CONTROLLER_ROLE_ANONYMOUS),
                () -> {
                    mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), 4711))
                            .andDo(MockMvcResultPrinter.print())
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MediaTypes.HAL_JSON))
                            .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")));

                    mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), "not4711"))
                            .andDo(MockMvcResultPrinter.print())
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MediaTypes.HAL_JSON))
                            .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:05:05")));
                    return null;
                }));
    }

    /**
     * Ensures that etag check results in not modified response if provided etag by client is identical to entity in repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 6),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 3),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6), // implicit lock
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void rootRsNotModified() throws Exception {
        final String controllerId = "4711";
        final String etag = mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andReturn().getResponse()
                .getHeader("ETag");
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId).header("If-None-Match", etag))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotModified());

        final Target target = targetManagement.getByControllerId(controllerId);
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds.getId(), controllerId);

        final Action updateAction = deploymentManagement.findActiveActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);
        final String etagWithFirstUpdate = mvc
                .perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId)
                        .header("If-None-Match", etag)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(new RequestOnHawkbitDefaultPortPostProcessor()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink("4711", updateAction.getId().toString()))))
                .andReturn().getResponse().getHeader("ETag");
        assertThat(etagWithFirstUpdate).isNotNull();
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId).header("If-None-Match",
                        etagWithFirstUpdate).with(new RequestOnHawkbitDefaultPortPostProcessor()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotModified());

        // now lets finish the update
        sendDeploymentActionFeedback(target, updateAction, "closed", null)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        // as the update was installed, and we always receive the installed action, the
        // original state cannot be restored
        final String etagAfterInstallation = mvc
                .perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId)
                        .header("If-None-Match", etag).accept(MediaType.APPLICATION_JSON)
                        .with(new RequestOnHawkbitDefaultPortPostProcessor()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.installedBase.href",
                        startsWith(installedBaseLink("4711", updateAction.getId().toString()))))
                .andReturn().getResponse().getHeader("ETag");

        // Now another deployment
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2");
        assignDistributionSet(ds2.getId(), controllerId);
        final Action updateAction2 = deploymentManagement.findActiveActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId)
                        .header("If-None-Match", etagAfterInstallation).accept(MediaType.APPLICATION_JSON)
                        .with(new RequestOnHawkbitDefaultPortPostProcessor()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$._links.installedBase.href",
                        startsWith(installedBaseLink("4711", updateAction.getId().toString()))))
                .andExpect(jsonPath("$._links.deploymentBase.href",
                        startsWith(deploymentBaseLink("4711", updateAction2.getId().toString()))))
                .andReturn().getResponse().getHeader("ETag");
    }

    /**
     * Ensures that the target state machine of a precomissioned target switches from
     * UNKNOWN to REGISTERED when the target polls for the first time.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void rootRsPreCommissioned() throws Exception {
        final String controllerId = "4711";
        testdataFactory.createTarget(controllerId);
        assertThat(targetManagement.getByControllerId(controllerId).getUpdateStatus()).isEqualTo(TargetUpdateStatus.UNKNOWN);
        final long current = System.currentTimeMillis();
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:01:00")));
        assertThat(targetManagement.getByControllerId(controllerId)).satisfies(target -> {
            assertThat(target.getLastTargetQuery()).isLessThanOrEqualTo(System.currentTimeMillis());
            assertThat(target.getLastTargetQuery()).isGreaterThanOrEqualTo(current);
            assertThat(target.getUpdateStatus()).isEqualTo(TargetUpdateStatus.REGISTERED);
        });
    }

    /**
     * Ensures that the source IP address of the polling target is correctly stored in repository
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void rootRsPlugAndPlayIpAddress() throws Exception {
        securityProperties.getClients().setTrackRemoteIp(true);
        // test
        final String knownControllerId1 = "0815";
        final long create = System.currentTimeMillis();
        // make a poll, audit information should be set on plug and play
        callAs(withController("controller", CONTROLLER_ROLE_ANONYMOUS),
                () -> {
                    mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), knownControllerId1))
                            .andDo(MockMvcResultPrinter.print())
                            .andExpect(status().isOk());
                    return null;
                });
        // verify
        assertThat(targetManagement.getByControllerId(knownControllerId1)).satisfies(target -> {
            assertThat(target.getAddress()).isEqualTo(IpUtil.createHttpUri("127.0.0.1").toString());
            assertThat(target.getCreatedBy()).isEqualTo("CONTROLLER_PLUG_AND_PLAY");
            assertThat(target.getCreatedAt()).isGreaterThanOrEqualTo(create);
            assertThat(target.getLastModifiedBy()).isEqualTo("CONTROLLER_PLUG_AND_PLAY");
            assertThat(target.getLastModifiedAt()).isGreaterThanOrEqualTo(create);
        });
    }

    /**
     * Ensures that the source IP address of the polling target is not stored in repository if disabled
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1) })
    void rootRsIpAddressNotStoredIfDisabled() throws Exception {
        securityProperties.getClients().setTrackRemoteIp(false);
        // test
        final String knownControllerId1 = "0815";
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), knownControllerId1))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        // verify
        final Target target = targetManagement.getByControllerId(knownControllerId1);
        assertThat(target.getAddress()).isEqualTo(IpUtil.createHttpUri("***").toString());
        securityProperties.getClients().setTrackRemoteIp(true);
    }

    /**
     * Controller trys to finish an update process after it has been finished by an error action status.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2) })
    void tryToFinishAnUpdateProcessAfterItHasBeenFinished() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("911");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        sendDeploymentActionFeedback(savedTarget, savedAction, "proceeding", null)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        sendDeploymentActionFeedback(savedTarget, savedAction, "closed", "failure")
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        sendDeploymentActionFeedback(savedTarget, savedAction, "closed", "success")
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isGone());
    }

    /**
     * Controller sends attribute update request after device successfully closed software update.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 6),
            @Expect(type = TargetPollEvent.class, count = 4),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void attributeUpdateRequestSendingAfterSuccessfulDeployment() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("1");
        final Target savedTarget = testdataFactory.createTarget("922");
        final Map<String, String> attributes = Collections.singletonMap("AttributeKey", "AttributeValue");
        assertThatAttributesUpdateIsRequested(savedTarget.getControllerId());
        mvc.perform(put(CONTROLLER_BASE + "/configData", AccessContext.tenant(), savedTarget.getControllerId())
                        .content(JsonBuilder.configData(attributes).toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        assertThatAttributesUpdateIsNotRequested(savedTarget.getControllerId());
        assertAttributesUpdateNotRequestedAfterFailedDeployment(savedTarget, ds);
        assertAttributesUpdateRequestedAfterSuccessfulDeployment(savedTarget, ds);
    }

    /**
     * Test to verify that only a specific count of messages are returned based on the input actionHistory for getControllerDeploymentActionFeedback endpoint.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void testActionHistoryCount() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("911");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        sendDeploymentActionFeedback(savedTarget, savedAction, "scheduled", null, TARGET_SCHEDULED_INSTALLATION_MSG)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        sendDeploymentActionFeedback(savedTarget, savedAction, "proceeding", null, TARGET_PROCEEDING_INSTALLATION_MSG)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        sendDeploymentActionFeedback(savedTarget, savedAction, "closed", "success", TARGET_COMPLETED_INSTALLATION_MSG)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        mvc.perform(get(DEPLOYMENT_BASE + "?actionHistory=2", AccessContext.tenant(), 911, savedAction.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString(TARGET_COMPLETED_INSTALLATION_MSG))))
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString(TARGET_PROCEEDING_INSTALLATION_MSG))))
                .andExpect(jsonPath("$.actionHistory.messages", not(hasItem(containsString(TARGET_SCHEDULED_INSTALLATION_MSG)))));
    }

    /**
     * Test to verify that a zero input value of actionHistory results in no action history appended for getControllerDeploymentActionFeedback endpoint.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void testActionHistoryZeroInput() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("911");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        sendDeploymentActionFeedback(savedTarget, savedAction, "scheduled", null, TARGET_SCHEDULED_INSTALLATION_MSG)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        sendDeploymentActionFeedback(savedTarget, savedAction, "proceeding", null, TARGET_PROCEEDING_INSTALLATION_MSG)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        sendDeploymentActionFeedback(savedTarget, savedAction, "closed", "success", TARGET_COMPLETED_INSTALLATION_MSG)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        mvc.perform(get(DEPLOYMENT_BASE + "?actionHistory=0", AccessContext.tenant(), 911, savedAction.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages").doesNotExist());
        mvc.perform(get(DEPLOYMENT_BASE + "?actionHistory", AccessContext.tenant(), 911, savedAction.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages").doesNotExist());
    }

    /**
     * Test to verify that entire action history is returned if the input value for actionHistory is -1, for getControllerDeploymentActionFeedback endpoint.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1), // implicit lock
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 3), // implicit lock
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1) })
    void testActionHistoryNegativeInput() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("911");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        sendDeploymentActionFeedback(savedTarget, savedAction, "scheduled", null, TARGET_SCHEDULED_INSTALLATION_MSG)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        sendDeploymentActionFeedback(savedTarget, savedAction, "proceeding", null, TARGET_PROCEEDING_INSTALLATION_MSG)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        sendDeploymentActionFeedback(savedTarget, savedAction, "closed", "success", TARGET_COMPLETED_INSTALLATION_MSG)
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        mvc.perform(get(DEPLOYMENT_BASE + "?actionHistory=-1", AccessContext.tenant(), 911, savedAction.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString(TARGET_SCHEDULED_INSTALLATION_MSG))))
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString(TARGET_PROCEEDING_INSTALLATION_MSG))))
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString(TARGET_COMPLETED_INSTALLATION_MSG))));
    }

    /**
     * Test the polling time based on different maintenance window start and end time.
     */
    @Test
    void sleepTimeResponseForDifferentMaintenanceWindowParameters() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        getAs(withUser("tenantadmin", TENANT_CONFIGURATION),
                () -> {
                    tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.POLLING_TIME, "00:05:00");
                    return null;
                });

        final Target savedTarget = testdataFactory.createTarget("1911");
        assignDistributionSetWithMaintenanceWindow(
                ds.getId(), savedTarget.getControllerId(), getTestSchedule(16),
                getTestDuration(10), getTestTimeZone()).getAssignedEntity().iterator().next();

        mvc.perform(get(CONTROLLER_BASE, "default-tenant", "1911"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.config.polling.sleep", greaterThanOrEqualTo("00:05:00")));

        final Target savedTarget1 = testdataFactory.createTarget("2911");
        final DistributionSet ds1 = testdataFactory.createDistributionSet("1");
        assignDistributionSetWithMaintenanceWindow(
                ds1.getId(), savedTarget1.getControllerId(), getTestSchedule(10),
                getTestDuration(10), getTestTimeZone()).getAssignedEntity().iterator().next();
        mvc.perform(get(CONTROLLER_BASE, "default-tenant", "2911"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.config.polling.sleep", lessThan("00:05:00")))
                .andExpect(jsonPath("$.config.polling.sleep", greaterThanOrEqualTo("00:03:00")));

        final Target savedTarget2 = testdataFactory.createTarget("3911");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2");
        assignDistributionSetWithMaintenanceWindow(
                ds2.getId(), savedTarget2.getControllerId(), getTestSchedule(5),
                getTestDuration(5), getTestTimeZone()).getAssignedEntity().iterator().next();

        mvc.perform(get(CONTROLLER_BASE, "default-tenant", "3911"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.config.polling.sleep", lessThan("00:02:00")));

        final Target savedTarget3 = testdataFactory.createTarget("4911");
        final DistributionSet ds3 = testdataFactory.createDistributionSet("3");
        assignDistributionSetWithMaintenanceWindow(
                ds3.getId(), savedTarget3.getControllerId(), getTestSchedule(-5),
                getTestDuration(15), getTestTimeZone()).getAssignedEntity().iterator().next();
        mvc.perform(get(CONTROLLER_BASE, "default-tenant", "4911"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.config.polling.sleep", equalTo("00:05:00")));
    }

    /**
     * Test download and update values before maintenance window start time.
     */
    @Test
    void downloadAndUpdateStatusBeforeMaintenanceWindowStartTime() throws Exception {
        Target savedTarget = testdataFactory.createTarget("1911");
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        savedTarget = getFirstAssignedTarget(assignDistributionSetWithMaintenanceWindow(
                ds.getId(), savedTarget.getControllerId(), getTestSchedule(2), getTestDuration(1), getTestTimeZone()));
        mvc.perform(get(CONTROLLER_BASE, "default-tenant", "1911")).andExpect(status().isOk());

        final Action action = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        mvc.perform(get(DEPLOYMENT_BASE, AccessContext.tenant(), "1911", action.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deployment.download", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.update", equalTo("skip")))
                .andExpect(jsonPath("$.deployment.maintenanceWindow", equalTo("unavailable")));
    }

    /**
     * Test download and update values after maintenance window start time.
     */
    @Test
    void downloadAndUpdateStatusDuringMaintenanceWindow() throws Exception {
        Target savedTarget = testdataFactory.createTarget("1911");
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        savedTarget = getFirstAssignedTarget(assignDistributionSetWithMaintenanceWindow(
                ds.getId(), savedTarget.getControllerId(), getTestSchedule(-5), getTestDuration(10), getTestTimeZone()));

        mvc.perform(get(CONTROLLER_BASE, "default-tenant", "1911")).andExpect(status().isOk());

        final Action action = deploymentManagement.findActiveActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        mvc.perform(get(DEPLOYMENT_BASE, AccessContext.tenant(), "1911", action.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deployment.download", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.update", equalTo("forced")))
                .andExpect(jsonPath("$.deployment.maintenanceWindow", equalTo("available")));
    }

    /**
     * Assign multiple DS in multi-assignment mode. The earliest active Action is exposed to the controller.
     */
    @Test
    void earliestActionIsExposedToControllerInMultiAssignMode() throws Exception {
        enableMultiAssignments();
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds1 = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        final DistributionSet ds2 = testdataFactory.createDistributionSet(UUID.randomUUID().toString());
        final Action action1 = getFirstAssignedAction(assignDistributionSet(ds1.getId(), target.getControllerId(), 56));
        final Long action2Id = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), target.getControllerId(), 34));

        assertDeploymentActionIsExposedToTarget(target.getControllerId(), action1.getId());
        sendDeploymentActionFeedback(target, action1, "closed", "success");
        assertDeploymentActionIsExposedToTarget(target.getControllerId(), action2Id);
    }

    /**
     * The system should not create a new target because of a too long controller id.
     */
    @Test
    void rootRsWithInvalidControllerId() throws Exception {
        final String invalidControllerId = randomString(Target.CONTROLLER_ID_MAX_SIZE + 1);
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), invalidControllerId)).andExpect(status().isBadRequest());
    }

    private void assertAttributesUpdateNotRequestedAfterFailedDeployment(Target target, final DistributionSet ds) throws Exception {
        target = getFirstAssignedTarget(assignDistributionSet(ds.getId(), target.getControllerId()));
        final Action action = deploymentManagement.findActiveActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);
        sendDeploymentActionFeedback(target, action, "closed", "failure").andExpect(status().isOk());
        assertThatAttributesUpdateIsNotRequested(target.getControllerId());
    }

    private void assertAttributesUpdateRequestedAfterSuccessfulDeployment(Target target, final DistributionSet ds) throws Exception {
        target = getFirstAssignedTarget(assignDistributionSet(ds.getId(), target.getControllerId()));
        final Action action = deploymentManagement.findActiveActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);
        sendDeploymentActionFeedback(target, action, "closed", null).andExpect(status().isOk());
        assertThatAttributesUpdateIsRequested(target.getControllerId());
    }

    private void assertThatAttributesUpdateIsRequested(final String targetControllerId) throws Exception {
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), targetControllerId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.configData.href").isNotEmpty());
    }

    private void assertThatAttributesUpdateIsNotRequested(final String targetControllerId) throws Exception {
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), targetControllerId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.configData").doesNotExist());
    }

    private ResultActions sendDeploymentActionFeedback(
            final Target target, final Action action, final String execution, String finished, String message) throws Exception {
        if (finished == null) {
            finished = "none";
        }
        if (message == null) {
            message = randomString(1000);
        }

        final String feedback = getJsonActionFeedback(DdiStatus.ExecutionStatus.valueOf(execution.toUpperCase()),
                DdiResult.FinalResult.valueOf(finished.toUpperCase()), Collections.singletonList(message));
        return mvc.perform(post(DEPLOYMENT_FEEDBACK, AccessContext.tenant(), target.getControllerId(), action.getId())
                .content(feedback).contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions sendDeploymentActionFeedback(final Target target, final Action action, final String execution, final String finished)
            throws Exception {
        return sendDeploymentActionFeedback(target, action, execution, finished, null);
    }

    private void assertDeploymentActionIsExposedToTarget(final String controllerId, final long expectedActionId) throws Exception {
        final String expectedDeploymentBaseLink = String.format(
                "/%s/controller/v1/%s/deploymentBase/%d",
                AccessContext.tenant(), controllerId, expectedActionId);
        mvc.perform(get(CONTROLLER_BASE, AccessContext.tenant(), controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.deploymentBase.href", containsString(expectedDeploymentBaseLink)));
    }

    private void withPollingTime(final String pollingTime, final Callable<Void> runnable) throws Exception {
        getAs(withUser("tenantadmin", TENANT_CONFIGURATION),
                () -> {
                    tenantConfigurationManagement().addOrUpdateConfiguration(TenantConfigurationKey.POLLING_TIME, pollingTime);
                    return null;
                });
        try {
            runnable.call();
        } finally {
            getAs(withUser("tenantadmin", TENANT_CONFIGURATION),
                    () -> {
                        tenantConfigurationManagement().deleteConfiguration(TenantConfigurationKey.POLLING_TIME);
                        return null;
                    });
        }
    }
}
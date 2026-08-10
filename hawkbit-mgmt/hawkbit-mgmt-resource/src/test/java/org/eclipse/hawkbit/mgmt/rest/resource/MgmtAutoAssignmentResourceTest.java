/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.hawkbit.repository.model.Action.ActionType.FORCED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.APPROVAL_DENIED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.PAUSED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.READY;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.RUNNING;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.WAITING_FOR_APPROVAL;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTO_ASSIGNMENT_APPROVAL_ENABLED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRestModelMapper;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Tests for covering the {@link MgmtAutoAssignmentResource}.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Auto Assignment Resource
 */
@TestPropertySource(
        locations = "classpath:/mgmt-test.properties")
class MgmtAutoAssignmentResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String HREF_AUTO_ASSIGNMENT_PREFIX = "http://localhost/rest/v1/autoassignments/";

    @Autowired
    private TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement;

    @Autowired
    private RepositoryProperties repositoryProperties;

    @BeforeEach
    void reset() throws Exception {
        SecurityContextSwitch.asPrivileged(() -> {
            tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, false);
            return null;
        });
    }

    /**
     * Try to create an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void createAutoAssignmentWithPermission() throws Exception {
        createAutoAssignment("autoAssignment-suff", 201);
    }

    /**
     * Try to create an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void createAutoAssignmentWithoutPermission() throws Exception {
        createAutoAssignment("autoAssignment-insuff", 403);
    }

    void createAutoAssignment(final String name, final int expectedStatus) throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();

        postAutoAssignment(name, "name==*", ds.getId(), System.currentTimeMillis(), FORCED, 100, false, expectedStatus);
    }

    /**
     * Try to create an auto assignment
     */
    @Test
    void createAutoAssignment() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();

        postAutoAssignment("tfq", "name==*", ds.getId(), 201);
    }

    /**
     * Try to create an auto assignment with an invalid body
     */
    @Test
    void createAutoAssignmentInvalid() throws Exception {
        mvc.perform(post("/rest/v1/autoassignments").content("invalid body").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode", equalTo("hawkbit.server.error.rest.body.notReadable")));
    }

    /**
     * Proves that omitting the "required" primitive distributionSetId is NOT rejected as a validation error.
     */
    @Test
    void createAutoAssignmentWithMissingDistributionSetId() throws Exception {
        // valid body except distributionSetId is entirely omitted from the JSON
        final String body = "{\"name\":\"missingDs\",\"targetFilterQuery\":\"name==*\"}";

        mvc.perform(post("/rest/v1/autoassignments").content(body).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    /**
     * Ensures that creating an auto assignment whose query addresses too many targets is rejected with HTTP 429.
     */
    @Test
    void createAutoAssignmentWithQueryThatExceedsQuota() throws Exception {
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target");

        final DistributionSet ds = testdataFactory.createDistributionSet();

        final String body = JsonBuilder.autoAssignment(
                "exceedsQuota", "controllerId==target*", ds.getId(), null, null, null, false);

        mvc.perform(post("/rest/v1/autoassignments").content(body).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));
    }

    /**
     * Reads a single auto assignment with a GET request
     */
    @Test
    void readSingleAutoAssignment() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final AutoAssignment autoAssignment = createAutoAssignmentEntity("ds", "name==*", ds);

        getAutoAssignment(autoAssignment.getId(), 200);
    }

    /**
     * Reads a single auto assignment with a GET request, that doesn't exist
     */
    @Test
    void readSingleAutoAssignmentNotFound() throws Exception {
        getAutoAssignment(-1L, 404);
    }

    /**
     * Read multiple auto assignments with a GET request
     */
    @Test
    void readMultipleAutoAssignments() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();

        final AutoAssignment firstAutoAssignment = createAutoAssignmentEntity("dsFirst", "name==a*", ds);
        final AutoAssignment secondAutoAssignment = createAutoAssignmentEntity("dsSecond", "name==b*", ds);

        getAutoAssignments(List.of(firstAutoAssignment, secondAutoAssignment), 200);
    }

    /**
     * Approve an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void approveAutoAssignmentWithPermission() throws Exception {
        SecurityContextSwitch.asPrivileged(() -> {
            tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);
            return null;
        });

        final long autoAssignmentId = createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(WAITING_FOR_APPROVAL);
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/approve", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(READY);
    }

    /**
     * Approve an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void approveAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(() -> {
            tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);
            return createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();
        });

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/approve", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> autoAssignmentManagement.get(autoAssignmentId).getStatus()))
                .isEqualTo(WAITING_FOR_APPROVAL);
    }

    /**
     * Deny an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void denyAutoAssignmentWithPermission() throws Exception {
        SecurityContextSwitch.asPrivileged(() -> {
            tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);
            return null;
        });

        final long autoAssignmentId = createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(WAITING_FOR_APPROVAL);
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/deny", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(APPROVAL_DENIED);
    }

    /**
     * Deny an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void denyAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(() -> {
            tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);
            return createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();
        });

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/deny", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> autoAssignmentManagement.get(autoAssignmentId).getStatus()))
                .isEqualTo(WAITING_FOR_APPROVAL);
    }

    /**
     * Start an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void startAutoAssignmentWithPermission() throws Exception {
        final long autoAssignmentId = createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(READY);
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/start", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(RUNNING);
    }

    /**
     * Starting an already running auto assignment is an illegal state transition and returns 400 (not 500)
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void startAutoAssignmentInIllegalStateFailsWithBadRequest() throws Exception {
        final long autoAssignmentId = createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();
        autoAssignmentManagement.start(autoAssignmentId);
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(RUNNING);

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/start", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isConflict());
    }

    /**
     * Starting a non-existing auto assignment returns 404
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void startNonExistingAutoAssignmentFailsWithNotFound() throws Exception {
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/start", 1234L))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Start an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void startAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(
                () -> createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId());

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/start", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> autoAssignmentManagement.get(autoAssignmentId).getStatus()))
                .isEqualTo(READY);
    }

    /**
     * Pause an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void pauseAutoAssignmentWithPermission() throws Exception {
        final long autoAssignmentId = createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();
        autoAssignmentManagement.start(autoAssignmentId);

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(RUNNING);
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/pause", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(PAUSED);
    }

    /**
     * Pause an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void pauseAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(() -> {
            final long id = createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();
            autoAssignmentManagement.start(id);
            return id;
        });

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/pause", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> autoAssignmentManagement.get(autoAssignmentId).getStatus()))
                .isEqualTo(RUNNING);
    }

    /**
     * Resume an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void resumeAutoAssignmentWithPermission() throws Exception {
        final long autoAssignmentId = createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();
        autoAssignmentManagement.start(autoAssignmentId);
        autoAssignmentManagement.pause(autoAssignmentId);

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(PAUSED);
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/resume", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(RUNNING);
    }

    /**
     * Resume an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void resumeAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(() -> {
            final long id = createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();
            autoAssignmentManagement.start(id);
            autoAssignmentManagement.pause(id);
            return id;
        });

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/resume", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> autoAssignmentManagement.get(autoAssignmentId).getStatus()))
                .isEqualTo(PAUSED);
    }

    /**
     * Deletes an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void deleteAutoAssignmentWithPermission() throws Exception {
        final long autoAssignmentId = createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId();

        mvc.perform(MockMvcRequestBuilders.delete("/rest/v1/autoassignments/{autoAssignmentId}", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(SecurityContextSwitch.asPrivileged(() -> autoAssignmentManagement.find(autoAssignmentId))).isEmpty();

        getAutoAssignment(autoAssignmentId, 404);
    }

    /**
     * Deletes an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void deleteAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(
                () -> createAutoAssignmentEntity("ds", "name==*", testdataFactory.createDistributionSet()).getId());

        mvc.perform(MockMvcRequestBuilders.delete("/rest/v1/autoassignments/{autoAssignmentId}", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        SecurityContextSwitch.asPrivileged(() -> {
            assertThat(autoAssignmentManagement.find(autoAssignmentId)).isPresent();
            return null;
        });
    }

    /**
     * Deletes a non-existent auto assignment
     */
    @Test
    void deleteAutoAssignmentInvalid() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/rest/v1/autoassignments/{autoAssignmentId}", -1))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    private AutoAssignment createAutoAssignmentEntity(final String name, final String query, final DistributionSet ds) {
        return autoAssignmentManagement.create(
                AutoAssignmentManagement.Create.builder().name(name).targetFilterQuery(query).distributionSet(ds).build());
    }

    private long postAutoAssignment(final String name, final String query, final long distributionSetId, final int expectedStatus)
            throws Exception {
        return postAutoAssignment(name, query, distributionSetId, null, null, null, false, expectedStatus);
    }

    private long postAutoAssignment(final String name, final String query, final long distributionSetId,
            final Long startAt, final ActionType actionType, final Integer weight, final Boolean confirmationRequired, final int expectedStatus)
            throws Exception {

        final String type = actionType != null ? MgmtRestModelMapper.convertActionType(actionType).getName() : null;
        final String autoAssignment = JsonBuilder.autoAssignment(name, query, distributionSetId,
                startAt, type, weight, confirmationRequired);

        final ResultActions response = mvc.perform(post("/rest/v1/autoassignments").content(autoAssignment).contentType(
                MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is(expectedStatus));

        if (expectedStatus != 201) {
            return -1;
        }

        final MvcResult result = response
                .andExpect(startAt != null ? jsonPath("$.startAt", equalTo(startAt)) : jsonPath("$.startAt").doesNotExist())
                .andExpect(jsonPath("$.actionType", equalTo(type != null ? type : "forced")))
                .andExpect(jsonPath("$.weight", equalTo(weight != null ? weight : repositoryProperties.getActionWeightIfAbsent())))
                .andExpect(jsonPath("$.confirmationRequired", equalTo(confirmationRequired != null
                        ? confirmationRequired
                        : TenantConfigHelper
                                .isUserConfirmationFlowEnabled())))
                .andExpect(jsonPath("$._links.self.href", startsWith(HREF_AUTO_ASSIGNMENT_PREFIX)))
                .andReturn();

        return OBJECT_MAPPER
                .readerFor(MgmtAutoAssignmentResponseBody.class)
                .<MgmtAutoAssignmentResponseBody> readValue(result.getResponse().getContentAsString())
                .getId();
    }

    private void getAutoAssignments(final List<AutoAssignment> autoAssignments, final int expectedStatus) throws Exception {
        final ResultActions response = mvc.perform(get("/rest/v1/autoassignments")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is(expectedStatus));

        if (expectedStatus != 200) {
            return;
        }

        response
                .andExpect(jsonPath("$.total", equalTo(autoAssignments.size())))
                .andExpect(jsonPath("$.size", equalTo(autoAssignments.size())))
                .andExpect(jsonPath("$.content", hasSize(autoAssignments.size())));

        for (final AutoAssignment autoAssignment : autoAssignments) {
            final String selector = "$.content[?(@.id==" + autoAssignment.getId() + ")]";
            response
                    .andExpect(jsonPath(selector + ".targetFilterQuery", contains(autoAssignment.getTargetFilterQuery())))
                    .andExpect(jsonPath(selector + ".distributionSetId",
                            contains(autoAssignment.getDistributionSet().getId().intValue())))
                    .andExpect(jsonPath(selector + ".status",
                            contains(autoAssignment.getStatus().toString().toLowerCase())))
                    .andExpect(jsonPath(selector + "._links.self.href", contains(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX))));
        }
    }

    private void getAutoAssignment(final Long autoAssignmentId, final int expectedStatus) throws Exception {
        final ResultActions response = mvc.perform(get("/rest/v1/autoassignments/" + autoAssignmentId)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is(expectedStatus));

        if (expectedStatus != 200) {
            return;
        }

        final AutoAssignment autoAssignment = autoAssignmentManagement.get(autoAssignmentId);
        response
                .andExpect(jsonPath("$.id", equalTo(autoAssignment.getId().intValue())))
                .andExpect(jsonPath("$.targetFilterQuery", equalTo(autoAssignment.getTargetFilterQuery())))
                .andExpect(jsonPath("$.distributionSetId",
                        equalTo(autoAssignment.getDistributionSet().getId().intValue())))
                .andExpect(jsonPath("$.status",
                        equalTo(autoAssignment.getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("$._links.self.href", startsWith(HREF_AUTO_ASSIGNMENT_PREFIX)))
                .andExpect(autoAssignment.getStatus() == READY
                        ? jsonPath("$._links.start.href", allOf(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX), endsWith("/start")))
                        : jsonPath("$._links.start.href").doesNotExist())
                .andExpect(autoAssignment.getStatus() == RUNNING
                        ? jsonPath("$._links.pause.href", allOf(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX), endsWith("/pause")))
                        : jsonPath("$._links.pause.href").doesNotExist())
                .andExpect(autoAssignment.getStatus() == PAUSED
                        ? jsonPath("$._links.resume.href", allOf(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX), endsWith("/resume")))
                        : jsonPath("$._links.resume.href").doesNotExist())
                .andExpect(autoAssignment.getStatus() == WAITING_FOR_APPROVAL
                        ? jsonPath("$._links.approve.href", allOf(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX), endsWith("/approve")))
                        : jsonPath("$._links.approve.href").doesNotExist())
                .andExpect(autoAssignment.getStatus() == WAITING_FOR_APPROVAL
                        ? jsonPath("$._links.deny.href", allOf(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX), endsWith("/deny")))
                        : jsonPath("$._links.deny.href").doesNotExist())
                .andReturn();
    }
}
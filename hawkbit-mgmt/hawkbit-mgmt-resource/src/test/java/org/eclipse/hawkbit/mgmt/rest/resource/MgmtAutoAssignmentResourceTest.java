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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.eclipse.hawkbit.repository.model.Action.ActionType.FORCED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.APPROVAL_DENIED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.PAUSED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.READY;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.RUNNING;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.WAITING_FOR_APPROVAL;
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
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtAutoAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRestModelMapper;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
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
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN})
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
     * Try to create an auto assignment with an existing target filter, but the query doesn't match
     */
    @Test
    void createAutoAssignmentExistingTargetFilterQueryNotMatching() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        SecurityContextSwitch.asPrivileged(() -> targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder().name("tfq").query("name==*").build()));


        postAutoAssignment("tfq", "name==target-*", ds.getId(), 400);
    }

    /**
     * Try to create an auto assignment with an existing target filter, should be successful
     */
    @Test
    void createAutoAssignmentExistingTarget() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        targetFilterQueryManagement.create(TargetFilterQueryManagement.Create.builder().name("tfq").query("name==*").build());

        postAutoAssignment("tfq", "name==*", ds.getId(), 201);
    }

    /**
     * Try to create an auto assignment without an existing target filter
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
     * Reads a single auto assignment with a GET request
     */
    @Test
    void readSingleAutoAssignment() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

        final TargetFilterQuery autoAssignment = targetFilterQueryManagement.updateAutoAssignDS(
                new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()));

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

        final Long firstFilterId = targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder().name("dsFirst").query("name==a*").build()).getId();
        final Long secondFilterId = targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder().name("dsSecond").query("name==b*").build()).getId();

        final TargetFilterQuery firstAutoAssignment = targetFilterQueryManagement.updateAutoAssignDS(
                new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(firstFilterId).ds(ds.getId()));
        final TargetFilterQuery secondAutoAssignment = targetFilterQueryManagement.updateAutoAssignDS(
                new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(secondFilterId).ds(ds.getId()));

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

        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

        final long autoAssignmentId = targetFilterQueryManagement.updateAutoAssignDS(
                new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();

        assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()).isEqualTo(WAITING_FOR_APPROVAL);
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/approve",  autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()).isEqualTo(READY);
    }

    /**
     * Approve an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void approveAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(() -> {
            tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);

            final DistributionSet ds = testdataFactory.createDistributionSet();
            final Long filterId = targetFilterQueryManagement.create(
                    TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

            return targetFilterQueryManagement.updateAutoAssignDS(
                    new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();
        });

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/approve", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()))
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

        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

        final long autoAssignmentId = targetFilterQueryManagement.updateAutoAssignDS(
                new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();

        assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()).isEqualTo(WAITING_FOR_APPROVAL);
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/deny",  autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()).isEqualTo(APPROVAL_DENIED);
    }

    /**
     * Deny an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void denyAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(() -> {
            tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);

            final DistributionSet ds = testdataFactory.createDistributionSet();
            final Long filterId = targetFilterQueryManagement.create(
                    TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

            return targetFilterQueryManagement.updateAutoAssignDS(
                    new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();
        });

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/deny", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()))
                .isEqualTo(WAITING_FOR_APPROVAL);
    }

    /**
     * Start an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void startAutoAssignmentWithPermission() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

        final long autoAssignmentId = targetFilterQueryManagement.updateAutoAssignDS(
                new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();

        assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()).isEqualTo(READY);
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/start",  autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()).isEqualTo(RUNNING);
    }

    /**
     * Start an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void startAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(() -> {
            final DistributionSet ds = testdataFactory.createDistributionSet();
            final Long filterId = targetFilterQueryManagement.create(
                    TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

            return targetFilterQueryManagement.updateAutoAssignDS(
                    new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();
        });

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/start", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()))
                .isEqualTo(READY);
    }

    /**
     * Pause an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void pauseAutoAssignmentWithPermission() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

        final long autoAssignmentId = targetFilterQueryManagement.updateAutoAssignDS(
                new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();
        targetFilterQueryManagement.startAutoAssignDS(autoAssignmentId);

        assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()).isEqualTo(RUNNING);
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/pause",  autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()).isEqualTo(PAUSED);
    }

    /**
     * Pause an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void pauseAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(() -> {
            final DistributionSet ds = testdataFactory.createDistributionSet();
            final Long filterId = targetFilterQueryManagement.create(
                    TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

            return targetFilterQueryManagement.updateAutoAssignDS(
                    new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();
        });

        SecurityContextSwitch.asPrivileged(() -> targetFilterQueryManagement.startAutoAssignDS(autoAssignmentId));

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/pause", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()))
                .isEqualTo(RUNNING);
    }

    /**
     * Resume an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void resumeAutoAssignmentWithPermission() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

        final long autoAssignmentId = targetFilterQueryManagement.updateAutoAssignDS(
                new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();
        targetFilterQueryManagement.startAutoAssignDS(autoAssignmentId);
        targetFilterQueryManagement.pauseAutoAssignDS(autoAssignmentId);

        assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()).isEqualTo(PAUSED);
        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/resume",  autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()).isEqualTo(RUNNING);
    }

    /**
     * Resume an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void resumeAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(() -> {
            final DistributionSet ds = testdataFactory.createDistributionSet();
            final Long filterId = targetFilterQueryManagement.create(
                    TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

            return targetFilterQueryManagement.updateAutoAssignDS(
                    new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();
        });

        SecurityContextSwitch.asPrivileged(() -> {
            targetFilterQueryManagement.startAutoAssignDS(autoAssignmentId);
            targetFilterQueryManagement.pauseAutoAssignDS(autoAssignmentId);
            return null;
        });

        mvc.perform(post("/rest/v1/autoassignments/{autoAssignmentId}/resume", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignStatus()))
                .isEqualTo(PAUSED);
    }

    /**
     * Deletes an auto assignment with sufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.TARGET_ADMIN, SpRole.REPOSITORY_ADMIN })
    void deleteAutoAssignmentWithPermission() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(
                TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

        final long autoAssignmentId = targetFilterQueryManagement.updateAutoAssignDS(
                new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();

        mvc.perform(MockMvcRequestBuilders.delete("/rest/v1/autoassignments/{autoAssignmentId}", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(SecurityContextSwitch.asPrivileged(
                () -> targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignDistributionSet()))
                .isNull();

        getAutoAssignment(autoAssignmentId, 404);
    }

    /**
     * Deletes an auto assignment with insufficient permissions
     */
    @Test
    @WithUser(principal = "bumlux", authorities = { SpRole.REPOSITORY_ADMIN })
    void deleteAutoAssignmentWithoutPermission() throws Exception {
        final long autoAssignmentId = SecurityContextSwitch.asPrivileged(() -> {
                    final DistributionSet ds = testdataFactory.createDistributionSet();
                    final Long filterId = targetFilterQueryManagement.create(
                            TargetFilterQueryManagement.Create.builder().name("ds").query("name==*").build()).getId();

                    return targetFilterQueryManagement.updateAutoAssignDS(
                            new TargetFilterQueryManagement.AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())).getId();

                });

        mvc.perform(MockMvcRequestBuilders.delete("/rest/v1/autoassignments/{autoAssignmentId}", autoAssignmentId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        SecurityContextSwitch.asPrivileged(() -> {
            assertThatNoException().isThrownBy(() -> targetFilterQueryManagement.get(autoAssignmentId));
            assertThat(targetFilterQueryManagement.get(autoAssignmentId).getAutoAssignDistributionSet()).isNotNull();
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

    private long postAutoAssignment(final String name, final String targetFilterQuery, final long distributionSetId, final int expectedStatus) throws Exception {
        return postAutoAssignment(name, targetFilterQuery, distributionSetId, null, null, null, false, expectedStatus);
    }

    private long postAutoAssignment(final String name, final String targetFilterQuery, final long distributionSetId,
    final Long startAt, final ActionType actionType, final Integer weight, final Boolean confirmationRequired, final int expectedStatus) throws Exception{

        final String type = actionType != null ? MgmtRestModelMapper.convertActionType(actionType).getName() : null;
        final String autoAssignment = JsonBuilder.autoAssignment(name, targetFilterQuery, distributionSetId,
                startAt, type, weight, confirmationRequired);

        final ResultActions response = mvc.perform(post("/rest/v1/autoassignments").content(autoAssignment).contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is(expectedStatus));
//                .andExpect(jsonPath("$.name", equalTo(name)))
//                .andExpect(jsonPath("$.createdBy", equalTo("bumlux")))
//                .andExpect(jsonPath("$.createdAt", not(equalTo(0))))
//                .andExpect(jsonPath("$.lastModifiedBy", equalTo("bumlux")))
//                .andExpect(jsonPath("$.lastModifiedAt", not(equalTo(0))))
//                .andExpect(jsonPath("$.lastModifiedBy", equalTo("bumlux")))

        if (expectedStatus != 201) {
            return -1;
        }

        final MvcResult result = response
                .andExpect(startAt != null ? jsonPath("$.startAt", equalTo(startAt)) : jsonPath("$.startAt").doesNotExist())
                .andExpect(jsonPath("$.actionType", equalTo(type != null ? type : "forced")))
                .andExpect(jsonPath("$.weight", equalTo(weight != null ? weight : repositoryProperties.getActionWeightIfAbsent())))
                .andExpect(jsonPath("$.confirmationRequired", equalTo(confirmationRequired != null ? confirmationRequired : TenantConfigHelper
                        .isUserConfirmationFlowEnabled())))
                .andExpect(jsonPath("$._links.self.href", startsWith(HREF_AUTO_ASSIGNMENT_PREFIX)))
                .andReturn();

        return OBJECT_MAPPER
                .readerFor(MgmtAutoAssignmentResponseBody.class)
                .<MgmtAutoAssignmentResponseBody> readValue(result.getResponse().getContentAsString())
                .getId();
    }

    private void getAutoAssignments(final List<TargetFilterQuery> autoAssignments, final int expectedStatus) throws Exception {
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

        for (final TargetFilterQuery autoAssignment : autoAssignments) {
            final String selector = "$.content[?(@.id==" + autoAssignment.getId() + ")]";
            response
                    .andExpect(jsonPath(selector + ".targetFilterQuery", contains(autoAssignment.getQuery())))
                    .andExpect(jsonPath(selector + ".distributionSetId",
                            contains(autoAssignment.getAutoAssignDistributionSet().getId().intValue())))
                    .andExpect(jsonPath(selector + ".status",
                            contains(autoAssignment.getAutoAssignStatus().toString().toLowerCase())))
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

        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.get(autoAssignmentId);
        response
                .andExpect(jsonPath("$.id", equalTo(targetFilterQuery.getId().intValue())))
                .andExpect(jsonPath("$.targetFilterQuery", equalTo(targetFilterQuery.getQuery())))
                .andExpect(jsonPath("$.distributionSetId",
                        equalTo(targetFilterQuery.getAutoAssignDistributionSet().getId().intValue())))
                .andExpect(jsonPath("$.status",
                        equalTo(targetFilterQuery.getAutoAssignStatus().toString().toLowerCase())))
                .andExpect(jsonPath("$._links.self.href", startsWith(HREF_AUTO_ASSIGNMENT_PREFIX)))
                .andExpect(targetFilterQuery.getAutoAssignStatus() == READY
                        ? jsonPath("$._links.start.href", allOf(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX), endsWith("/start")))
                        : jsonPath("$._links.start.href").doesNotExist())
                .andExpect(targetFilterQuery.getAutoAssignStatus() == RUNNING
                        ? jsonPath("$._links.pause.href", allOf(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX), endsWith("/pause")))
                        : jsonPath("$._links.pause.href").doesNotExist())
                .andExpect(targetFilterQuery.getAutoAssignStatus() == PAUSED
                        ? jsonPath("$._links.resume.href", allOf(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX), endsWith("/resume")))
                        : jsonPath("$._links.resume.href").doesNotExist())
                .andExpect(targetFilterQuery.getAutoAssignStatus() == WAITING_FOR_APPROVAL
                        ? jsonPath("$._links.approve.href", allOf(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX), endsWith("/approve")))
                        : jsonPath("$._links.approve.href").doesNotExist())
                .andExpect(targetFilterQuery.getAutoAssignStatus() == WAITING_FOR_APPROVAL
                        ? jsonPath("$._links.deny.href", allOf(startsWith(HREF_AUTO_ASSIGNMENT_PREFIX), endsWith("/deny")))
                        : jsonPath("$._links.deny.href").doesNotExist())
                .andReturn();
    }
}

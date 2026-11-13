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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Test for {@link MgmtActionStatusCodeResource}.
 */
class MgmtActionStatusCodeResourceTest extends AbstractManagementApiIntegrationTest {

    @Test
    void createReturns201WithDefaultInfoSeverity() throws Exception {
        final String response = mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":1234,\"name\":\"DOWNLOAD_HASH_MISMATCH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", equalTo(1234)))
                .andExpect(jsonPath("$.severity", equalTo("INFO")))
                .andReturn().getResponse().getContentAsString();

        final Long id = extractId(response);
        assertThat(response).contains("/rest/v1/action-status-codes/" + id);
    }

    @Test
    void createDuplicateReturns409() throws Exception {
        // First creation
        mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":1234,\"name\":\"DOWNLOAD_HASH_MISMATCH\"}"))
                .andExpect(status().isCreated());

        // Second creation with same code
        mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":1234,\"name\":\"ANOTHER_NAME\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_REPO_ENTITY_ALREADY_EXISTS.getKey())));
    }

    @Test
    void createWithoutNameReturns400() throws Exception {
        mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":1234}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById() throws Exception {
        final String response = mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":1234,\"name\":\"TEST_CODE\",\"description\":\"Test description\",\"severity\":\"ERROR\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        final Long id = extractId(response);

        mvc.perform(get("/rest/v1/action-status-codes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id.intValue())))
                .andExpect(jsonPath("$.code", equalTo(1234)))
                .andExpect(jsonPath("$.name", equalTo("TEST_CODE")))
                .andExpect(jsonPath("$.description", equalTo("Test description")))
                .andExpect(jsonPath("$.severity", equalTo("ERROR")));
    }

    @Test
    void getUnknownIdReturns404() throws Exception {
        mvc.perform(get("/rest/v1/action-status-codes/9999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getNonNumericIdReturns400() throws Exception {
        mvc.perform(get("/rest/v1/action-status-codes/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateKeepsCodeAndChangesSeverity() throws Exception {
        final String response = mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":1234,\"name\":\"TEST_CODE\",\"severity\":\"INFO\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        final Long id = extractId(response);

        mvc.perform(put("/rest/v1/action-status-codes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"WARN\",\"code\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(1234)))
                .andExpect(jsonPath("$.severity", equalTo("WARN")));
    }

    @Test
    void listFilteredBySeverityAndSorted() throws Exception {
        // Create multiple codes with different severities
        mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":100,\"name\":\"CODE_100\",\"severity\":\"ERROR\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":200,\"name\":\"CODE_200\",\"severity\":\"INFO\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":300,\"name\":\"CODE_300\",\"severity\":\"ERROR\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/rest/v1/action-status-codes")
                        .param("q", "severity==ERROR")
                        .param("sort", "code:DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", equalTo(2)))
                .andExpect(jsonPath("$.content[0].code", equalTo(300)))
                .andExpect(jsonPath("$.content[1].code", equalTo(100)));
    }

    @Test
    void deleteReturns204AndEntryIsGone() throws Exception {
        final String response = mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":1234,\"name\":\"TEST_CODE\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        final Long id = extractId(response);

        mvc.perform(delete("/rest/v1/action-status-codes/" + id))
                .andExpect(status().isNoContent());
        mvc.perform(get("/rest/v1/action-status-codes/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUnknownIdReturns404() throws Exception {
        mvc.perform(delete("/rest/v1/action-status-codes/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_REPO_ENTITY_NOT_EXISTS.getKey())));
    }

    @Test
    @WithUser(authorities = { "READ_TARGET", "CREATE_TARGET", "UPDATE_TARGET" })
    void deleteWithoutDeleteTargetReturns403() throws Exception {
        final String response = mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":1234,\"name\":\"TEST_CODE\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(delete("/rest/v1/action-status-codes/" + extractId(response)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletedCodeInUseIsUndocumentedAgain() throws Exception {
        final var target = testdataFactory.createTarget("target789");
        final var ds = testdataFactory.createDistributionSet("ds3");
        final long actionId = assignDistributionSet(ds, List.of(target))
                .getAssignedEntity().get(0).getId();
        controllerManagement.addUpdateActionStatus(
                Action.ActionStatusCreate.builder()
                        .actionId(actionId)
                        .status(Action.Status.RUNNING)
                        .code(100)
                        .build()
        );

        final String response = mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":100,\"name\":\"DOCUMENTED\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/rest/v1/action-status-codes/undocumented"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", equalTo(0)));

        mvc.perform(delete("/rest/v1/action-status-codes/" + extractId(response)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/rest/v1/action-status-codes/undocumented"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", equalTo(1)))
                .andExpect(jsonPath("$.content[0].code", equalTo(100)));

        // can be documented again
        mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":100,\"name\":\"DOCUMENTED_AGAIN\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithUser(authorities = { "READ_TARGET" })
    void createWithoutCreateTargetReturns403() throws Exception {
        mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":1234,\"name\":\"TEST_CODE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void undocumentedListsReportedCodes() throws Exception {
        // Create target and distribution set
        final String controllerId = "target123";
        final var target = testdataFactory.createTarget(controllerId);
        final var ds = testdataFactory.createDistributionSet("ds1");

        // Assign distribution set to target
        final long actionId = assignDistributionSet(ds, List.of(target))
                .getAssignedEntity().get(0).getId();

        // Report codes 100 and 200
        controllerManagement.addUpdateActionStatus(
                Action.ActionStatusCreate.builder()
                        .actionId(actionId)
                        .status(Action.Status.RUNNING)
                        .code(100)
                        .build()
        );
        controllerManagement.addUpdateActionStatus(
                Action.ActionStatusCreate.builder()
                        .actionId(actionId)
                        .status(Action.Status.RUNNING)
                        .code(200)
                        .build()
        );

        // Document code 200
        mvc.perform(post("/rest/v1/action-status-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":200,\"name\":\"DOCUMENTED\"}"))
                .andExpect(status().isCreated());

        // GET /undocumented should return only code 100
        mvc.perform(get("/rest/v1/action-status-codes/undocumented"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", equalTo(1)))
                .andExpect(jsonPath("$.content.length()", equalTo(1)))
                .andExpect(jsonPath("$.content[0].code", equalTo(100)));
    }

    @Test
    void undocumentedPathIsNotParsedAsCode() throws Exception {
        // GET /undocumented with empty data should return 200, not 400
        mvc.perform(get("/rest/v1/action-status-codes/undocumented"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", equalTo(0)));
    }

    @Test
    void undocumentedPaging() throws Exception {
        // Create target and distribution set
        final String controllerId = "target456";
        final var target = testdataFactory.createTarget(controllerId);
        final var ds = testdataFactory.createDistributionSet("ds2");

        // Assign distribution set to target
        final long actionId = assignDistributionSet(ds, List.of(target))
                .getAssignedEntity().get(0).getId();

        // Report codes 100 and 300
        controllerManagement.addUpdateActionStatus(
                Action.ActionStatusCreate.builder()
                        .actionId(actionId)
                        .status(Action.Status.RUNNING)
                        .code(100)
                        .build()
        );
        controllerManagement.addUpdateActionStatus(
                Action.ActionStatusCreate.builder()
                        .actionId(actionId)
                        .status(Action.Status.RUNNING)
                        .code(300)
                        .build()
        );

        // Test paging: offset=1&limit=1 over [100, 300] should return [300]
        mvc.perform(get("/rest/v1/action-status-codes/undocumented")
                        .param("offset", "1")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", equalTo(2)))
                .andExpect(jsonPath("$.content.length()", equalTo(1)))
                .andExpect(jsonPath("$.content[0].code", equalTo(300)));
    }

    @Test
    void undocumentedSanitizesPagingParams() throws Exception {
        // Test limit=0 returns 200 (sanitized to default)
        mvc.perform(get("/rest/v1/action-status-codes/undocumented")
                        .param("limit", "0"))
                .andExpect(status().isOk());

        // Test offset=-1 returns 200 (sanitized to 0)
        mvc.perform(get("/rest/v1/action-status-codes/undocumented")
                        .param("offset", "-1"))
                .andExpect(status().isOk());
    }

    private Long extractId(final String jsonResponse) {
        try {
            return Long.parseLong(jsonResponse.split("\"id\":")[1].split(",")[0]);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract id from response: " + jsonResponse, e);
        }
    }
}

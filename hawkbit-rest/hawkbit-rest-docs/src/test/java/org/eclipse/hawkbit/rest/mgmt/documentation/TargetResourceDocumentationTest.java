/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.mgmt.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.documentation.AbstractApiRestDocumentation;
import org.eclipse.hawkbit.rest.documentation.ApiModelPropertiesGeneric;
import org.eclipse.hawkbit.rest.documentation.MgmtApiModelProperties;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Documentation generation for Management API for {@link Target}.
 *
 */
@Feature("Spring Rest Docs Tests - Target")
@Story("Target Resource")
public class TargetResourceDocumentationTest extends AbstractApiRestDocumentation {

    private final String targetId = "137";

    @Override
    public String getResourceName() {
        return "targets";
    }

    @Test
    @Description("Handles the GET request of retrieving all targets within SP. Required Permission: READ_TARGET.")
    public void getTargets() throws Exception {
        createTargetByGivenNameWithAttributes(targetId, createDistributionSet());

        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(responseFields(
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("content").description(MgmtApiModelProperties.TARGET_LIST),
                        fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                        fieldWithPath("content[].address").description(MgmtApiModelProperties.ADDRESS),
                        fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                        fieldWithPath("content[].name").description(ApiModelPropertiesGeneric.NAME),
                        fieldWithPath("content[].description").description(ApiModelPropertiesGeneric.DESCRPTION),
                        fieldWithPath("content[].controllerId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        fieldWithPath("content[].updateStatus").description(MgmtApiModelProperties.UPDATE_STATUS)
                                .type("enum").attributes(
                                        key("value").value("['error', 'in_sync', 'pending', 'registered', 'unknown']")),
                        fieldWithPath("content[].securityToken").description(MgmtApiModelProperties.SECURITY_TOKEN),
                        fieldWithPath("content[].requestAttributes")
                                .description(MgmtApiModelProperties.REQUEST_ATTRIBUTES),
                        fieldWithPath("content[].installedAt").description(MgmtApiModelProperties.INSTALLED_AT),
                        fieldWithPath("content[].lastModifiedAt")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                        fieldWithPath("content[].lastModifiedBy")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                        fieldWithPath("content[].ipAddress").description(MgmtApiModelProperties.IP_ADDRESS)
                                .type("String"),
                        fieldWithPath("content[].lastControllerRequestAt")
                                .description(MgmtApiModelProperties.LAST_REQUEST_AT).type("Number"),
                        fieldWithPath("content[]._links.self").ignored())));
    }

    @Test
    @Description("Handles the GET request of retrieving all targets within SP based by parameter. Required Permission: READ_TARGET.")
    public void getTargetsWithParameters() throws Exception {
        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "?limit=10&sort=name:ASC&offset=0&q=name==a"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(getFilterRequestParamter()));
    }

    @Test
    @Description("Handles the POST request of creating new targets within SP. The request body must always be a list of targets. Required Permission: CREATE_TARGET.")
    public void postTargets() throws Exception {
        final String target = createTargetJsonForPostRequest("123456", "controllerId", "test");

        mockMvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)
                .contentType(MediaType.APPLICATION_JSON).content(target)).andExpect(status().isCreated())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(requestFields(
                        requestFieldWithPath("[]controllerId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        requestFieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                        optionalRequestFieldWithPath("[]description").description(ApiModelPropertiesGeneric.DESCRPTION),
                        optionalRequestFieldWithPath("[]address").description(MgmtApiModelProperties.ADDRESS),
                        optionalRequestFieldWithPath("[]securityToken")
                                .description(MgmtApiModelProperties.SECURITY_TOKEN)),
                        responseFields(fieldWithPath("[]controllerId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("[]description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("[]address").description(MgmtApiModelProperties.ADDRESS),
                                fieldWithPath("[]createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("[]lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                                fieldWithPath("[]lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                                fieldWithPath("[]ipAddress").description(MgmtApiModelProperties.IP_ADDRESS),
                                fieldWithPath("[]createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("[]updateStatus").description(MgmtApiModelProperties.UPDATE_STATUS)
                                        .type("enum")
                                        .attributes(key("value")
                                                .value("['error', 'in_sync', 'pending', 'registered', 'unknown']")),
                                fieldWithPath("[]securityToken").description(MgmtApiModelProperties.SECURITY_TOKEN),
                                fieldWithPath("[]requestAttributes")
                                        .description(MgmtApiModelProperties.REQUEST_ATTRIBUTES),
                                fieldWithPath("[]_links.self").ignored())));
    }

    @Test
    @Description("Handles the DELETE request of deleting a single target within SP. Required Permission: DELETE_TARGET.")
    public void deleteTarget() throws Exception {
        final Target target = testdataFactory.createTarget(targetId);

        mockMvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", target.getControllerId()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print()).andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving a single target within SP. Required Permission: READ_TARGET.")
    public void getTarget() throws Exception {
        final Target target = createTargetByGivenNameWithAttributes(targetId, createDistributionSet());

        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", target.getControllerId()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getResponseFieldTarget(false)));
    }

    @Test
    @Description("Handles the PUT request of updating a target within SP. Required Permission: UPDATE_TARGET.")
    public void putTarget() throws Exception {
        final Target target = createTargetByGivenNameWithAttributes(targetId, createDistributionSet());
        final String targetAsJson = createJsonTarget(targetId, "newTargetName", "I've been updated");

        mockMvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", target.getControllerId())
                .contentType(MediaType.APPLICATION_JSON).content(targetAsJson)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(optionalRequestFieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                optionalRequestFieldWithPath("description")
                                        .description(ApiModelPropertiesGeneric.DESCRPTION),
                                optionalRequestFieldWithPath("controllerId")
                                        .description(ApiModelPropertiesGeneric.ITEM_ID),
                                optionalRequestFieldWithPath("address").description(MgmtApiModelProperties.ADDRESS),
                                optionalRequestFieldWithPath("securityToken")
                                        .description(MgmtApiModelProperties.SECURITY_TOKEN),
                                optionalRequestFieldWithPath("requestAttributes")
                                        .description(MgmtApiModelProperties.REQUEST_ATTRIBUTES)),
                        getResponseFieldTarget(false)));
    }

    @Test
    @Description("Handles the GET request of retrieving the full action history of a specific target. Required Permission: READ_TARGET.")
    public void getActionsFromTarget() throws Exception {
        enableMultiAssignments();
        generateActionForTarget(targetId);

        mockMvc.perform(
                get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/" + MgmtRestConstants.TARGET_V1_ACTIONS,
                        targetId))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("content[]").description(MgmtApiModelProperties.ACTION_LIST),
                                fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("content[].lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                                fieldWithPath("content[].lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("String"),
                                fieldWithPath("content[].type").description(MgmtApiModelProperties.ACTION_TYPE)
                                        .attributes(key("value").value("['update', 'cancel']")),

                                fieldWithPath("content[].status")
                                        .description(MgmtApiModelProperties.ACTION_EXECUTION_STATUS)
                                        .attributes(key("value").value("['finished', 'pending']")),
                                fieldWithPath("content[]._links").description(MgmtApiModelProperties.LINK_TO_ACTION),
                                fieldWithPath("content[].id").description(MgmtApiModelProperties.ACTION_ID),
                                fieldWithPath("content[].weight").description(MgmtApiModelProperties.ACTION_WEIGHT))));
    }

    @Test
    @Description("Handles the GET request of retrieving the full action history of a specific target with maintenance window. Required Permission: READ_TARGET.")
    public void getActionsFromTargetWithMaintenanceWindow() throws Exception {
        enableMultiAssignments();
        generateActionForTarget(targetId, true, false, getTestSchedule(2), getTestDuration(1), getTestTimeZone());

        mockMvc.perform(
                get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/" + MgmtRestConstants.TARGET_V1_ACTIONS,
                        targetId))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("content[]").description(MgmtApiModelProperties.ACTION_LIST),
                                fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("content[].lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                                fieldWithPath("content[].lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("String"),
                                fieldWithPath("content[].type").description(MgmtApiModelProperties.ACTION_TYPE)
                                        .attributes(key("value").value("['update', 'cancel']")),

                                fieldWithPath("content[].status")
                                        .description(MgmtApiModelProperties.ACTION_EXECUTION_STATUS)
                                        .attributes(key("value").value("['finished', 'pending']")),
                                fieldWithPath("content[]._links").description(MgmtApiModelProperties.LINK_TO_ACTION),
                                fieldWithPath("content[].id").description(MgmtApiModelProperties.ACTION_ID),
                                fieldWithPath("content[].weight").description(MgmtApiModelProperties.ACTION_WEIGHT),
                                fieldWithPath("content[].maintenanceWindow")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW),
                                fieldWithPath("content[].maintenanceWindow.schedule")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_SCHEDULE),
                                fieldWithPath("content[].maintenanceWindow.duration")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_DURATION),
                                fieldWithPath("content[].maintenanceWindow.timezone")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_TIMEZONE),
                                fieldWithPath("content[].maintenanceWindow.nextStartAt")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_NEXT_START_AT))));
    }

    @Test
    @Description("Handles the GET request of retrieving all targets within SP based by parameter. Required Permission: READ_TARGET.")
    public void getActionsFromTargetWithParameters() throws Exception {
        generateActionForTarget(targetId);

        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "?limit=10&sort=id:ASC&offset=0&q=status==pending"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(requestParameters(
                        parameterWithName("limit").attributes(key("type").value("query"))
                                .description(ApiModelPropertiesGeneric.LIMIT),
                        parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                        parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                        parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL))));
    }

    @Test
    @Description("Cancels an active action, only active actions can be deleted. Required Permission: UPDATE_TARGET.")
    public void deleteActionFromTarget() throws Exception {
        final Action actions = generateActionForTarget(targetId, false);

        mockMvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/{actionId}", targetId, actions.getId()))
                .andExpect(status().isNoContent()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("actionId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Optionally force quits an active action, only active actions can be deleted. Required Permission: UPDATE_TARGET.")
    public void deleteActionFromTargetWithParameters() throws Exception {
        final Action action = generateActionForTarget(targetId, false);
        deploymentManagement.cancelAction(action.getId());
        
        mockMvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/{actionId}?force=true", targetId, action.getId()))
                .andExpect(status().isNoContent()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("actionId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestParameters(parameterWithName("force").description(MgmtApiModelProperties.FORCE))));
    }

    @Test
    @Description("Handles the GET request of retrieving a specific action on a specific target. Required Permission: READ_TARGET.")
    public void getActionFromTarget() throws Exception {
        enableMultiAssignments();
        final Action action = generateActionForTarget(targetId, true, true);
        assertThat(deploymentManagement.findAction(action.getId()).get().getActionType())
                .isEqualTo(ActionType.TIMEFORCED);

        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/{actionId}", targetId, action.getId()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("actionId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("id").description(MgmtApiModelProperties.ACTION_ID),
                                fieldWithPath("weight").description(MgmtApiModelProperties.ACTION_WEIGHT),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("String"),
                                fieldWithPath("type").description(MgmtApiModelProperties.ACTION_TYPE)
                                        .attributes(key("value").value("['update', 'cancel']")),
                                fieldWithPath("forceType").description(MgmtApiModelProperties.ACTION_FORCE_TYPE)
                                        .attributes(key("value").value("['forced', 'soft', 'timeforced']")),
                                fieldWithPath("forceTime").description(MgmtApiModelProperties.ACTION_FORCE_TIME)
                                        .type("String"),
                                fieldWithPath("status").description(MgmtApiModelProperties.ACTION_EXECUTION_STATUS)
                                        .attributes(key("value").value("['finished', 'pending']")),
                                fieldWithPath("_links.self").ignored(),
                                fieldWithPath("_links.distributionset").description(MgmtApiModelProperties.LINK_TO_DS),
                                fieldWithPath("_links.status")
                                        .description(MgmtApiModelProperties.LINKS_ACTION_STATUSES))));
    }

    @Test
    @Description("Handles the GET request of retrieving a specific action on a specific target. Required Permission: READ_TARGET.")
    public void getActionFromTargetWithMaintenanceWindow() throws Exception {
        enableMultiAssignments();
        final Action action = generateActionForTarget(targetId, true, true, getTestSchedule(2), getTestDuration(1),
                getTestTimeZone());

        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/{actionId}", targetId, action.getId()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("actionId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("id").description(MgmtApiModelProperties.ACTION_ID),
                                fieldWithPath("weight").description(MgmtApiModelProperties.ACTION_WEIGHT),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("String"),
                                fieldWithPath("type").description(MgmtApiModelProperties.ACTION_TYPE)
                                        .attributes(key("value").value("['update', 'cancel']")),
                                fieldWithPath("forceType").description(MgmtApiModelProperties.ACTION_FORCE_TYPE)
                                        .attributes(key("value").value("['forced', 'soft', 'timeforced']")),
                                fieldWithPath("forceTime").description(MgmtApiModelProperties.ACTION_FORCE_TIME)
                                        .type("String"),
                                fieldWithPath("status").description(MgmtApiModelProperties.ACTION_EXECUTION_STATUS)
                                        .attributes(key("value").value("['finished', 'pending']")),
                                fieldWithPath("maintenanceWindow")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW),
                                fieldWithPath("maintenanceWindow.schedule")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_SCHEDULE),
                                fieldWithPath("maintenanceWindow.duration")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_DURATION),
                                fieldWithPath("maintenanceWindow.timezone")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_TIMEZONE),
                                fieldWithPath("maintenanceWindow.nextStartAt")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_NEXT_START_AT),
                                fieldWithPath("_links.self").ignored(),
                                fieldWithPath("_links.distributionset").description(MgmtApiModelProperties.LINK_TO_DS),
                                fieldWithPath("_links.status")
                                        .description(MgmtApiModelProperties.LINKS_ACTION_STATUSES))));
    }

    @Test
    @Description("Handles the PUT request to switch an action from soft to forced. Required Permission: UPDATE_TARGET.")
    public void switchActionToForced() throws Exception {
        final Target target = testdataFactory.createTarget(targetId);
        final DistributionSet set = testdataFactory.createDistributionSet();
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(set.getId(), target.getControllerId(), ActionType.SOFT));
        assertThat(deploymentManagement.findAction(actionId).get().getActionType()).isEqualTo(ActionType.SOFT);

        final Map<String, Object> body = new HashMap<>();
        body.put("forceType", "forced");

        mockMvc.perform(
                put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/" + MgmtRestConstants.TARGET_V1_ACTIONS
                        + "/{actionId}", targetId, actionId).content(this.objectMapper.writeValueAsString(body))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("actionId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(
                                requestFieldWithPath("forceType").description(MgmtApiModelProperties.ACTION_FORCED)),
                        responseFields(fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("id").description(MgmtApiModelProperties.ACTION_ID),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("String"),
                                fieldWithPath("type").description(MgmtApiModelProperties.ACTION_TYPE)
                                        .attributes(key("value").value("['update', 'cancel']")),
                                fieldWithPath("forceType").description(MgmtApiModelProperties.ACTION_FORCE_TYPE)
                                        .attributes(key("value").value("['forced', 'soft', 'timeforced']")),
                                fieldWithPath("status").description(MgmtApiModelProperties.ACTION_EXECUTION_STATUS)
                                        .attributes(key("value").value("['finished', 'pending']")),
                                fieldWithPath("_links.self").ignored(),
                                fieldWithPath("_links.distributionset").description(MgmtApiModelProperties.LINK_TO_DS),
                                fieldWithPath("_links.status")
                                        .description(MgmtApiModelProperties.LINKS_ACTION_STATUSES))));
    }

    @Test
    @Description("Handles the GET request of retrieving a specific action on a specific target. Required Permission: READ_TARGET.")
    public void getStatusFromAction() throws Exception {
        final Action action = generateActionForTarget(targetId);

        mockMvc.perform(
                get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/" + MgmtRestConstants.TARGET_V1_ACTIONS
                        + "/{actionId}/" + MgmtRestConstants.TARGET_V1_ACTION_STATUS, targetId, action.getId()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("actionId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("content[]").description(MgmtApiModelProperties.ACTION_STATUS_LIST),
                                fieldWithPath("content[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("content[].messages")
                                        .description(MgmtApiModelProperties.ACTION_STATUS_MESSAGES).type("String"),
                                fieldWithPath("content[].reportedAt")
                                        .description(MgmtApiModelProperties.ACTION_STATUS_REPORTED_AT).type("String"),
                                fieldWithPath("content[].type").description(MgmtApiModelProperties.ACTION_STATUS_TYPE)
                                        .attributes(key("value").value(
                                                "['finished', 'error', 'warning', 'pending', 'running', 'canceled', 'retrieved', 'canceling']")))));
    }

    @Test
    @Description("Handles the GET request of retrieving all targets within SP based by parameter. Required Permission: READ_TARGET.")
    public void getStatusFromActionWithParameters() throws Exception {
        final Action action = generateActionForTarget(targetId);

        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                + MgmtRestConstants.TARGET_V1_ACTION_STATUS + "?limit=10&sort=id:ASC&offset=0"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        requestParameters(parameterWithName("limit").description(ApiModelPropertiesGeneric.LIMIT),
                                parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                                parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET))));
    }

    @Test
    @Description("Handles the GET request of retrieving the assigned distribution set of an specific target. Required Permission: READ_TARGET.")
    public void getAssignedDistributionSetFromAction() throws Exception {
        generateActionForTarget(targetId);

        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/"
                + MgmtRestConstants.TARGET_V1_ASSIGNED_DISTRIBUTION_SET, targetId)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getResponseFieldsDistributionSet(false)));
    }
    
    @Test
    @Description("Handles the POST request for assigning a distribution set to a specific target. Required Permission: READ_REPOSITORY and UPDATE_TARGET.")
    public void postAssignDistributionSetToTarget() throws Exception {
        // create target and ds, and assign ds
        testdataFactory.createTarget(targetId + "-old");
        final DistributionSet set = testdataFactory.createDistributionSet("one");
        assignDistributionSet(set.getId(), targetId + "-old");

        testdataFactory.createTarget(targetId);

        final long forceTime = System.currentTimeMillis();
        final String body = new JSONObject().put("id", set.getId()).put("type", "timeforced")
                .put("forcetime", forceTime)
                .put("maintenanceWindow", new JSONObject().put("schedule", getTestSchedule(100))
                        .put("duration", getTestDuration(10)).put("timezone", getTestTimeZone()))
                .toString();

        mockMvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/"
                + MgmtRestConstants.TARGET_V1_ASSIGNED_DISTRIBUTION_SET, targetId).content(body)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestParameters(parameterWithName("offline")
                                .description(MgmtApiModelProperties.OFFLINE_UPDATE).optional()),
                        requestFields(
                                requestFieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                requestFieldWithPathMandatoryInMultiAssignMode("weight")
                                        .description(MgmtApiModelProperties.ASSIGNMENT_WEIGHT)
                                        .type(JsonFieldType.NUMBER).attributes(key("value").value("0 - 1000")),
                                optionalRequestFieldWithPath("forcetime").description(MgmtApiModelProperties.FORCETIME),
                                optionalRequestFieldWithPath("maintenanceWindow")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW),
                                optionalRequestFieldWithPath("maintenanceWindow.schedule")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_SCHEDULE),
                                optionalRequestFieldWithPath("maintenanceWindow.duration")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_DURATION),
                                optionalRequestFieldWithPath("maintenanceWindow.timezone")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_TIMEZONE),
                                optionalRequestFieldWithPath("type").description(MgmtApiModelProperties.ASSIGNMENT_TYPE)
                                        .attributes(key("value").value("['soft', 'forced','timeforced', 'downloadonly']"))),
                        responseFields(getDsAssignmentResponseFieldDescriptors())));
    }

    @Test
    @Description("Handles the POST request for assigning distribution sets to a specific target. Required Permission: READ_REPOSITORY and UPDATE_TARGET.")
    public void postAssignDistributionSetsToTarget() throws Exception {
        // create target and ds, and assign ds
        final List<DistributionSet> sets = testdataFactory.createDistributionSets(2);
        testdataFactory.createTarget(targetId);

        final long forceTime = System.currentTimeMillis();
        final JSONArray body = new JSONArray();
        body.put(new JSONObject().put("id", sets.get(1).getId()).put("weight", 500).put("type", "timeforced")
                .put("forcetime", forceTime)
                .put("maintenanceWindow", new JSONObject().put("schedule", getTestSchedule(100))
                        .put("duration", getTestDuration(10)).put("timezone", getTestTimeZone())))
                .toString();
        body.put(new JSONObject().put("id", sets.get(0).getId()).put("type", "forced").put("weight", 800));

        enableMultiAssignments();
        mockMvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/"
                + MgmtRestConstants.TARGET_V1_ASSIGNED_DISTRIBUTION_SET, targetId).content(body.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestParameters(parameterWithName("offline")
                                .description(MgmtApiModelProperties.OFFLINE_UPDATE).optional()),
                        requestFields(
                                requestFieldWithPath("[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                requestFieldWithPathMandatoryInMultiAssignMode("[].weight")
                                        .description(MgmtApiModelProperties.ASSIGNMENT_WEIGHT)
                                        .attributes(key("value").value("0 - 1000")),
                                optionalRequestFieldWithPath("[].forcetime")
                                        .description(MgmtApiModelProperties.FORCETIME),
                                optionalRequestFieldWithPath("[].maintenanceWindow")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW),
                                optionalRequestFieldWithPath("[].maintenanceWindow.schedule")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_SCHEDULE),
                                optionalRequestFieldWithPath("[].maintenanceWindow.duration")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_DURATION),
                                optionalRequestFieldWithPath("[].maintenanceWindow.timezone")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_TIMEZONE),
                                optionalRequestFieldWithPath("[].type")
                                        .description(MgmtApiModelProperties.ASSIGNMENT_TYPE)
                                        .attributes(key("[].value")
                                                .value("['soft', 'forced','timeforced', 'downloadonly']"))),
                        responseFields(getDsAssignmentResponseFieldDescriptors())));
    }

    private static FieldDescriptor[] getDsAssignmentResponseFieldDescriptors() {
        final FieldDescriptor[] descriptors = {
                fieldWithPath("assigned").description(MgmtApiModelProperties.DS_NEW_ASSIGNED_TARGETS),
            fieldWithPath("alreadyAssigned").type(JsonFieldType.NUMBER)
            .description(MgmtApiModelProperties.DS_ALREADY_ASSIGNED_TARGETS),
    fieldWithPath("assignedActions").type(JsonFieldType.ARRAY)
            .description(MgmtApiModelProperties.DS_NEW_ASSIGNED_ACTIONS),
    fieldWithPath("assignedActions.[].id").type(JsonFieldType.NUMBER)
            .description(MgmtApiModelProperties.ACTION_ID),
    fieldWithPath("assignedActions.[]._links.self").type(JsonFieldType.OBJECT)
            .description(MgmtApiModelProperties.LINK_TO_ACTION),
    fieldWithPath("total").type(JsonFieldType.NUMBER)
                        .description(MgmtApiModelProperties.DS_TOTAL_ASSIGNED_TARGETS) };
        return descriptors;
    }

    @Test
    @Description("Handles the GET request of retrieving the attributes of a specific target. Reponse is a key/value list. Required Permission: READ_TARGET.")
    public void getControllerAttributes() throws Exception {
        // create target with attributes
        final Map<String, String> knownControllerAttrs = new HashMap<>();
        knownControllerAttrs.put("a", "1");
        knownControllerAttrs.put("b", "2");
        final Target target = testdataFactory.createTarget(targetId);
        controllerManagement.updateControllerAttributes(targetId, knownControllerAttrs, null);

        // test query target over rest resource
        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/attributes", target.getName()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.a", equalTo("1"))).andExpect(jsonPath("$.b", equalTo("2")))
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving the installed distribution set of an specific target. Required Permission: READ_TARGET.")
    public void getInstalledDistributionSetFromTarget() throws Exception {
        final Target target = createTargetByGivenNameWithAttributes(targetId, createDistributionSet());

        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/"
                + MgmtRestConstants.TARGET_V1_INSTALLED_DISTRIBUTION_SET, target.getName())).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getResponseFieldsDistributionSet(false)));
    }

    @Test
    @Description("Get a paged list of meta data for a target with standard page size." + " Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getMetadata() throws Exception {
        final int totalMetadata = 4;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final Target testTarget = testdataFactory.createTarget(targetId);
        for (int index = 0; index < totalMetadata; index++) {
            targetManagement.createMetaData(testTarget.getControllerId(), Lists.newArrayList(
                    entityFactory.generateTargetMetadata(knownKeyPrefix + index, knownValuePrefix + index)));
        }

        mockMvc.perform(
                get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata", testTarget.getControllerId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("content").description(MgmtApiModelProperties.META_DATA),
                                fieldWithPath("content[].key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("content[].value").description(MgmtApiModelProperties.META_DATA_VALUE))));
    }

    @Test
    @Description("Get a paged list of meta data for a target with defined page size and sorting by name descending and key starting with 'known'."
            + " Required Permission: " + SpPermission.READ_REPOSITORY)
    public void getMetadataWithParameters() throws Exception {
        final int totalMetadata = 4;

        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final Target testTarget = testdataFactory.createTarget(targetId);
        for (int index = 0; index < totalMetadata; index++) {
            targetManagement.createMetaData(testTarget.getControllerId(), Lists.newArrayList(
                    entityFactory.generateTargetMetadata(knownKeyPrefix + index, knownValuePrefix + index)));
        }

        mockMvc.perform(
                get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata", testTarget.getControllerId())
                        .param("offset", "1").param("limit", "2").param("sort", "key:DESC").param("q", "key==known*"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        requestParameters(
                                parameterWithName("limit").attributes(key("type").value("query"))
                                        .description(ApiModelPropertiesGeneric.LIMIT),
                                parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                                parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                                parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL)),
                        responseFields(fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("content").description(MgmtApiModelProperties.META_DATA),
                                fieldWithPath("content[].key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("content[].value").description(MgmtApiModelProperties.META_DATA_VALUE))));
    }

    @Test
    @Description("Get a single meta data value for a meta data key." + " Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getMetadataValue() throws Exception {

        // prepare and create metadata
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final Target testTarget = testdataFactory.createTarget(targetId);
        targetManagement.createMetaData(testTarget.getControllerId(),
                Collections.singletonList(entityFactory.generateTargetMetadata(knownKey, knownValue)));

        mockMvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata/{metadatakey}",
                testTarget.getControllerId(), knownKey)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("metadatakey").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("value").description(MgmtApiModelProperties.META_DATA_VALUE))));
    }

    @Test
    @Description("Update a single meta data value for specific key." + " Required Permission: "
            + SpPermission.UPDATE_REPOSITORY)
    public void updateMetadata() throws Exception {
        // prepare and create metadata for update
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final String updateValue = "valueForUpdate";

        final Target testTarget = testdataFactory.createTarget(targetId);
        targetManagement.createMetaData(testTarget.getControllerId(),
                Collections.singletonList(entityFactory.generateTargetMetadata(knownKey, knownValue)));

        final JSONObject jsonObject = new JSONObject().put("key", knownKey).put("value", updateValue);

        mockMvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata/{metadatakey}",
                testTarget.getControllerId(), knownKey)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonObject.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("metadatakey").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(requestFieldWithPath("key").description(MgmtApiModelProperties.META_DATA_KEY),
                                requestFieldWithPath("value").description(MgmtApiModelProperties.META_DATA_VALUE)),
                        responseFields(fieldWithPath("key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("value").description(MgmtApiModelProperties.META_DATA_VALUE))));

    }

    @Test
    @Description("Delete a single meta data." + " Required Permission: " + SpPermission.UPDATE_REPOSITORY)
    public void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final Target testTarget = testdataFactory.createTarget(targetId);
        targetManagement.createMetaData(testTarget.getControllerId(),
                Collections.singletonList(entityFactory.generateTargetMetadata(knownKey, knownValue)));

        mockMvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata/{key}",
                testTarget.getControllerId(), knownKey)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("key").description(ApiModelPropertiesGeneric.ITEM_ID))));

    }

    @Test
    @Description("Create a list of meta data entries" + " Required Permission: " + SpPermission.READ_REPOSITORY
            + " and " + SpPermission.UPDATE_TARGET)
    public void createMetadata() throws Exception {

        final Target testTarget = testdataFactory.createTarget(targetId);

        final String knownKey1 = "knownKey1";
        final String knownKey2 = "knownKey2";

        final String knownValue1 = "knownValue1";
        final String knownValue2 = "knownValue2";

        final JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        jsonArray.put(new JSONObject().put("key", knownKey2).put("value", knownValue2));

        mockMvc.perform(
                post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata", testTarget.getControllerId())
                        .contentType(MediaType.APPLICATION_JSON).content(jsonArray.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(requestFieldWithPath("[]key").description(MgmtApiModelProperties.META_DATA_KEY),
                                optionalRequestFieldWithPath("[]value")
                                        .description(MgmtApiModelProperties.META_DATA_VALUE))));
    }

    private String createTargetJsonForPostRequest(final String controllerId, final String name,
            final String description) throws JsonProcessingException {
        final Map<String, Object> target = new HashMap<>();
        target.put("controllerId", controllerId);
        target.put("description", description);
        target.put("name", name);
        target.put("address", "https://192.168.0.1");
        target.put("securityToken", "2345678DGGDGFTDzztgf");
        return "[" + this.objectMapper.writeValueAsString(target) + "]";
    }

    private String createJsonTarget(final String controllerId, final String name, final String description)
            throws JsonProcessingException {
        final Map<String, Object> target = new HashMap<>();
        target.put("controllerId", controllerId);
        target.put("description", description);
        target.put("name", name);
        target.put("address", "https://192.168.0.1");
        target.put("securityToken", "2345678DGGDGFTDzztgf");
        target.put("requestAttributes", true);
        return this.objectMapper.writeValueAsString(target);
    }

    private Action generateActionForTarget(final String knownControllerId) throws Exception {
        return generateActionForTarget(knownControllerId, true, false, null, null, null);
    }

    private Action generateActionForTarget(final String knownControllerId, final boolean inSync) throws Exception {
        return generateActionForTarget(knownControllerId, inSync, false, null, null, null);
    }

    private Action generateActionForTarget(final String knownControllerId, final boolean inSync,
            final boolean timeforced) throws Exception {
        return generateActionForTarget(knownControllerId, inSync, timeforced, null, null, null);
    }

    private Action generateActionForTarget(final String knownControllerId, final boolean inSync,
            final boolean timeforced, final String maintenanceWindowSchedule, final String maintenanceWindowDuration,
            final String maintenanceWindowTimeZone) throws Exception {
        final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, ActionStatusFields.ID.getFieldName());

        createTargetByGivenNameWithAttributes(knownControllerId, inSync, timeforced, createDistributionSet(),
                maintenanceWindowSchedule, maintenanceWindowDuration, maintenanceWindowTimeZone);

        final List<Action> actions = deploymentManagement.findActionsAll(pageRequest).getContent();

        assertThat(actions).hasSize(1);
        return actions.get(0);
    }
}

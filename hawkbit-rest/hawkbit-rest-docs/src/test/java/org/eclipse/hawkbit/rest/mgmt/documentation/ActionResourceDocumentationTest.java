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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.rest.documentation.AbstractApiRestDocumentation;
import org.eclipse.hawkbit.rest.documentation.ApiModelPropertiesGeneric;
import org.eclipse.hawkbit.rest.documentation.MgmtApiModelProperties;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.restdocs.payload.JsonFieldType;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Documentation generation for Management API for {@link Action}.
 */
@Feature("Spring Rest Docs Tests - Action")
@Story("Action Resource")
public class ActionResourceDocumentationTest extends AbstractApiRestDocumentation {

    private final String targetId = "target137";

    @Override
    public String getResourceName() {
        return "actions";
    }

    @Test
    @Description("Handles the GET request of retrieving all actions. Required Permission: READ_TARGET.")
    public void getActions() throws Exception {
        enableMultiAssignments();
        final Action action = generateRolloutActionForTarget(targetId);
        provideCodeFeedback(action, 200);

        mockMvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(responseFields(
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
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

                        fieldWithPath("content[].status").description(MgmtApiModelProperties.ACTION_EXECUTION_STATUS)
                                .attributes(key("value").value("['finished', 'pending']")),
                        fieldWithPath("content[].detailStatus").description(MgmtApiModelProperties.ACTION_DETAIL_STATUS)
                                .attributes(key("value").value(
                                        "['finished', 'error', 'running', 'warning', 'scheduled', 'canceling', 'canceled', 'download', 'downloaded', 'retrieved', 'cancel_rejected']")),
                        optionalRequestFieldWithPath("content[].lastStatusCode")
                                .description(MgmtApiModelProperties.ACTION_LAST_STATUS_CODE).type("Integer"),
                        fieldWithPath("content[]._links").description(MgmtApiModelProperties.LINK_TO_ACTION),
                        fieldWithPath("content[].id").description(MgmtApiModelProperties.ACTION_ID),
                        fieldWithPath("content[].weight").description(MgmtApiModelProperties.ACTION_WEIGHT),
                        fieldWithPath("content[].rollout").description(MgmtApiModelProperties.ACTION_ROLLOUT),
                        fieldWithPath("content[].rolloutName")
                                .description(MgmtApiModelProperties.ACTION_ROLLOUT_NAME))));
    }

    @Test
    @Description("Handles the GET request of retrieving all actions based on parameters. Required Permission: READ_TARGET.")
    public void getActionsWithParameters() throws Exception {
        final Action action = generateRolloutActionForTarget(targetId);

        mockMvc.perform(
                get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "?limit=10&sort=id:ASC&offset=0&q=rollout.id=="
                        + action.getRollout().getId() + "&representation=full"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(requestParameters(
                        parameterWithName("limit").attributes(key("type").value("query"))
                                .description(ApiModelPropertiesGeneric.LIMIT),
                        parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                        parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                        parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL),
                        parameterWithName("representation").description(MgmtApiModelProperties.REPRESENTATION_MODE))));
    }

    @Test
    @Description("Handles the GET request of retrieving a specific action.")
    public void getAction() throws Exception {
        final Action action = generateRolloutActionForTarget(targetId);
        provideCodeFeedback(action, 200);

        assertThat(deploymentManagement.findAction(action.getId()).get().getActionType())
            .isEqualTo(Action.ActionType.FORCED);

        mockMvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING + "/{actionId}", action.getId()))
            .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
            .andDo(this.document.document(
                pathParameters(parameterWithName("actionId").description(ApiModelPropertiesGeneric.ITEM_ID)),
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
                    fieldWithPath("detailStatus").description(MgmtApiModelProperties.ACTION_DETAIL_STATUS)
                        .attributes(key("value").value(
                            "['finished', 'error', 'running', 'warning', 'scheduled', 'canceling', 'canceled', 'download', 'downloaded', 'retrieved', 'cancel_rejected']")),
                    optionalRequestFieldWithPath("lastStatusCode")
                        .description(MgmtApiModelProperties.ACTION_LAST_STATUS_CODE).type("Integer"),
                    fieldWithPath("rollout").description(MgmtApiModelProperties.ACTION_ROLLOUT),
                    fieldWithPath("rolloutName").description(MgmtApiModelProperties.ACTION_ROLLOUT_NAME),
                    fieldWithPath("_links.self").ignored(),
                    fieldWithPath("_links.distributionset").description(MgmtApiModelProperties.LINK_TO_DS),
                    fieldWithPath("_links.status")
                        .description(MgmtApiModelProperties.LINKS_ACTION_STATUSES),
                    fieldWithPath("_links.rollout").description(MgmtApiModelProperties.LINK_TO_ROLLOUT),
                    fieldWithPath("_links.target").description(MgmtApiModelProperties.LINK_TO_TARGET))));
    }

    private Action generateRolloutActionForTarget(final String knownControllerId) throws Exception {
        return generateActionForTarget(knownControllerId, true, false, null, null, null, true);
    }

    private Action generateActionForTarget(final String knownControllerId, final boolean inSync,
            final boolean timeforced, final String maintenanceWindowSchedule, final String maintenanceWindowDuration,
            final String maintenanceWindowTimeZone, final boolean createRollout) throws Exception {
        final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, ActionStatusFields.ID.getFieldName());

        createTargetByGivenNameWithAttributes(knownControllerId, inSync, timeforced, createDistributionSet(),
                maintenanceWindowSchedule, maintenanceWindowDuration, maintenanceWindowTimeZone, createRollout);

        final List<Action> actions = deploymentManagement.findActionsAll(pageRequest).getContent();

        assertThat(actions).hasSize(1);
        return actions.get(0);
    }

}

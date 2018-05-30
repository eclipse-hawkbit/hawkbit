/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.mgmt.documentation;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.test.util.RolloutTestApprovalStrategy;
import org.eclipse.hawkbit.rest.documentation.AbstractApiRestDocumentation;
import org.eclipse.hawkbit.rest.documentation.ApiModelPropertiesGeneric;
import org.eclipse.hawkbit.rest.documentation.DocumenationResponseFieldsSnippet;
import org.eclipse.hawkbit.rest.documentation.MgmtApiModelProperties;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.fasterxml.jackson.core.JsonProcessingException;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Documentation generation for Rollout API for {@link Rollout}.
 *
 */
@Features("Spring Rest Docs Tests - Rollout")
@Stories("Rollout Resource")
public class RolloutResourceDocumentationTest extends AbstractApiRestDocumentation {

    @Autowired
    private RolloutTestApprovalStrategy approvalStrategy;

    @Override
    @Before
    public void setUp() {
        this.resourceName = "rollouts";
        super.setUp();
        arrayPrefix = "content[].";
        approvalStrategy.setApprovalNeeded(false);
    }

    @Test
    @Description("Handles the GET request of retrieving all rollouts. Required Permission: "
            + SpPermission.READ_ROLLOUT)
    public void getRollouts() throws Exception {

        createRolloutEntity();

        mockMvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING).accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andDo(this.document.document(getRolloutResponseFields(true, false,
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("content").description(MgmtApiModelProperties.ROLLOUT_LIST))));
    }

    @Test
    @Description("Handles the GET request of retrieving all rollouts. Required Permission: "
            + SpPermission.READ_ROLLOUT)
    public void getRolloutsWithParameters() throws Exception {
        createRolloutEntity();

        mockMvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING).param("offset", "0").param("limit", "2")
                .param("sort", "id:DESC").param("q", "name==exampleRollout*").accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andDo(this.document.document(getFilterRequestParamter()));
    }

    private Snippet getRolloutResponseFields(final boolean isArray, final boolean withDetails,
            final FieldDescriptor... descriptors) throws JsonProcessingException {
        final String arrayPrefix = getArrayPrefix(isArray);
        final List<FieldDescriptor> allFieldDescriptor = new ArrayList<>();
        allFieldDescriptor.addAll(Arrays.asList(descriptors));

        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "id").description(ApiModelPropertiesGeneric.ITEM_ID));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "name").description(ApiModelPropertiesGeneric.NAME));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "deleted").description(ApiModelPropertiesGeneric.DELETED));
        allFieldDescriptor
                .add(fieldWithPath(arrayPrefix + "description").description(ApiModelPropertiesGeneric.DESCRPTION));
        allFieldDescriptor
                .add(fieldWithPath(arrayPrefix + "createdBy").description(ApiModelPropertiesGeneric.CREATED_BY));
        allFieldDescriptor
                .add(fieldWithPath(arrayPrefix + "createdAt").description(ApiModelPropertiesGeneric.CREATED_AT));
        allFieldDescriptor.add(
                fieldWithPath(arrayPrefix + "lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY));
        allFieldDescriptor.add(
                fieldWithPath(arrayPrefix + "lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "targetFilterQuery")
                .description(MgmtApiModelProperties.ROLLOUT_FILTER_QUERY));
        allFieldDescriptor.add(
                fieldWithPath(arrayPrefix + "distributionSetId").description(MgmtApiModelProperties.ROLLOUT_DS_ID));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "status").description(MgmtApiModelProperties.ROLLOUT_STATUS)
                .attributes(key("value").value("['creating','ready','paused','running','finished']")));
        allFieldDescriptor.add(
                fieldWithPath(arrayPrefix + "totalTargets").description(MgmtApiModelProperties.ROLLOUT_TOTAL_TARGETS));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.self").ignored());
        if (withDetails) {
            allFieldDescriptor.add(fieldWithPath(arrayPrefix + "totalTargetsPerStatus")
                    .description(MgmtApiModelProperties.ROLLOUT_TOTAL_TARGETS_PER_STATUS));
            allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.start")
                    .description(MgmtApiModelProperties.ROLLOUT_LINKS_START_SYNC));
            allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.pause")
                    .description(MgmtApiModelProperties.ROLLOUT_LINKS_PAUSE));
            allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.resume")
                    .description(MgmtApiModelProperties.ROLLOUT_LINKS_RESUME));
            allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.groups")
                    .description(MgmtApiModelProperties.ROLLOUT_LINKS_GROUPS));
            allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.approve")
                    .description(MgmtApiModelProperties.ROLLOUT_LINKS_APPROVE));
            allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.deny")
                    .description(MgmtApiModelProperties.ROLLOUT_LINKS_DENY));
        }

        return new DocumenationResponseFieldsSnippet(allFieldDescriptor);
    }

    @Test
    @Description("Handles the GET request of retrieving a single rollout. Required Permission: "
            + SpPermission.READ_ROLLOUT)
    public void getRollout() throws Exception {

        final Rollout rollout = createRolloutEntity();

        mockMvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}", rollout.getId())
                .accept(MediaTypes.HAL_JSON_VALUE)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andDo(this.document.document(getRolloutResponseFields(false, true),
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the POST request of creating a rollout. Required Permission: " + SpPermission.CREATE_ROLLOUT)
    public void createRollout() throws Exception {

        testdataFactory.createTargets(20, "targets-");

        final String name = "exampleRollout";
        final String description = "Rollout for all named targets";
        final int groupSize = 10;
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final String targetFilter = "id==targets-*";
        final RolloutGroupConditions rolloutGroupConditions = new RolloutGroupConditionBuilder()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "50")
                .successAction(RolloutGroupSuccessAction.NEXTGROUP, "")
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, "80")
                .errorAction(RolloutGroupErrorAction.PAUSE, "").build();
        mockMvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING)
                .content(JsonBuilder.rollout(name, description, groupSize, dsId, targetFilter, rolloutGroupConditions))
                .contentType(APPLICATION_JSON_HAL_UTF).accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andDo(this.document.document(requestFields(
                        requestFieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                        requestFieldWithPath("distributionSetId").description(MgmtApiModelProperties.ROLLOUT_DS_ID),
                        requestFieldWithPath("targetFilterQuery")
                                .description(MgmtApiModelProperties.ROLLOUT_FILTER_QUERY),
                        requestFieldWithPath("amountGroups").description(MgmtApiModelProperties.ROLLOUT_AMOUNT_GROUPS),
                        optionalRequestFieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                        optionalRequestFieldWithPath("successCondition")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION),
                        optionalRequestFieldWithPath("successCondition.condition")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION_CONDITION)
                                .attributes(key("value").value("['threshold']")),
                        optionalRequestFieldWithPath("successCondition.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION_EXP),
                        optionalRequestFieldWithPath("successAction")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION),
                        optionalRequestFieldWithPath("successAction.action")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION_ACTION)
                                .attributes(key("value").value("['nextgroup']")),
                        optionalRequestFieldWithPath("successAction.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION_EXP),
                        optionalRequestFieldWithPath("errorCondition")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION),
                        optionalRequestFieldWithPath("errorCondition.condition")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION_CONDITION)
                                .attributes(key("value").value("['threshold']")),
                        optionalRequestFieldWithPath("errorCondition.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION_EXP),
                        optionalRequestFieldWithPath("errorAction")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION),
                        optionalRequestFieldWithPath("errorAction.action")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION_ACTION)
                                .attributes(key("value").value("['pause']")),
                        optionalRequestFieldWithPath("errorAction.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION_EXP)),

                        getRolloutResponseFields(false, true)));

    }

    @Test
    @Description("Handles the POST request of creating a rollout with a groups definition. Required Permission: "
            + SpPermission.CREATE_ROLLOUT)
    public void createRolloutWithGroupsDefinition() throws Exception {

        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, "targets-", "rollout");

        final List<RolloutGroup> rolloutGroups = new ArrayList<>(2);
        final float percentTargetsInGroup1 = 20;
        final float percentTargetsInGroup2 = 100;

        final RolloutGroup group1 = entityFactory.rolloutGroup().create().name("Group1").description("Group1desc")
                .targetFilterQuery("name==targets-1*").targetPercentage(percentTargetsInGroup1)
                .conditions(new RolloutGroupConditionBuilder()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "90")
                        .successAction(RolloutGroupSuccessAction.NEXTGROUP, "")
                        .errorCondition(RolloutGroupErrorCondition.THRESHOLD, "30")
                        .errorAction(RolloutGroupErrorAction.PAUSE, "").build())
                .build();
        rolloutGroups.add(group1);

        final RolloutGroup group2 = entityFactory.rolloutGroup().create().name("Group2").description("Group2desc")
                .targetPercentage(percentTargetsInGroup2).build();
        rolloutGroups.add(group2);

        final String name = "exampleRolloutWithDefinedGroups";
        final String description = "Rollout for all named targets";
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final String targetFilter = "id==targets-*";
        final RolloutGroupConditions rolloutGroupConditions = new RolloutGroupConditionBuilder()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "50")
                .successAction(RolloutGroupSuccessAction.NEXTGROUP, "")
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, "80")
                .errorAction(RolloutGroupErrorAction.PAUSE, "").build();
        mockMvc.perform(
                post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING)
                        .content(JsonBuilder.rollout(name, description, null, dsId, targetFilter,
                                rolloutGroupConditions, rolloutGroups))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andDo(this.document.document(requestFields(
                        requestFieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                        requestFieldWithPath("distributionSetId").description(MgmtApiModelProperties.ROLLOUT_DS_ID),
                        requestFieldWithPath("targetFilterQuery")
                                .description(MgmtApiModelProperties.ROLLOUT_FILTER_QUERY),
                        optionalRequestFieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                        optionalRequestFieldWithPath("successCondition")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION),
                        optionalRequestFieldWithPath("successCondition.condition")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION_CONDITION)
                                .attributes(key("value").value("['threshold']")),
                        optionalRequestFieldWithPath("successCondition.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION_EXP),
                        optionalRequestFieldWithPath("successAction")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION),
                        optionalRequestFieldWithPath("successAction.action")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION_ACTION)
                                .attributes(key("value").value("['nextgroup']")),
                        optionalRequestFieldWithPath("successAction.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION_EXP),
                        optionalRequestFieldWithPath("errorCondition")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION),
                        optionalRequestFieldWithPath("errorCondition.condition")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION_CONDITION)
                                .attributes(key("value").value("['threshold']")),
                        optionalRequestFieldWithPath("errorCondition.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION_EXP),
                        optionalRequestFieldWithPath("errorAction")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION),
                        optionalRequestFieldWithPath("errorAction.action")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION_ACTION)
                                .attributes(key("value").value("['pause']")),
                        optionalRequestFieldWithPath("errorAction.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION_EXP),

                        requestFieldWithPath("groups").description(MgmtApiModelProperties.ROLLOUT_GROUPS),
                        requestFieldWithPath("groups[].name").description(ApiModelPropertiesGeneric.NAME),
                        requestFieldWithPath("groups[].description").description(ApiModelPropertiesGeneric.DESCRPTION),
                        optionalRequestFieldWithPath("groups[].targetFilterQuery")
                                .description(MgmtApiModelProperties.ROLLOUT_GROUP_FILTER_QUERY),
                        optionalRequestFieldWithPath("groups[].targetPercentage")
                                .description(MgmtApiModelProperties.ROLLOUT_GROUP_TARGET_PERCENTAGE)
                                .attributes(key("value").value("0..100")),
                        optionalRequestFieldWithPath("groups[].successCondition")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION),
                        optionalRequestFieldWithPath("groups[].successCondition.condition")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION_CONDITION)
                                .attributes(key("value").value("['threshold']")),
                        optionalRequestFieldWithPath("groups[].successCondition.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION_EXP),
                        optionalRequestFieldWithPath("groups[].successAction")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION),
                        optionalRequestFieldWithPath("groups[].successAction.action")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION_ACTION)
                                .attributes(key("value").value("['nextgroup']")),
                        optionalRequestFieldWithPath("groups[].successAction.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION_EXP),
                        optionalRequestFieldWithPath("groups[].errorCondition")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION),
                        optionalRequestFieldWithPath("groups[].errorCondition.condition")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION_CONDITION)
                                .attributes(key("value").value("['threshold']")),
                        optionalRequestFieldWithPath("groups[].errorCondition.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION_EXP),
                        optionalRequestFieldWithPath("groups[].errorAction")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION),
                        optionalRequestFieldWithPath("groups[].errorAction.action")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION_ACTION)
                                .attributes(key("value").value("['pause']")),
                        optionalRequestFieldWithPath("groups[].errorAction.expression")
                                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION_EXP)),
                        getRolloutResponseFields(false, true)));
    }

    @Test
    @Description("Handles the DELETE request of deleting a rollout within SP. Required Permission: "
            + SpPermission.DELETE_ROLLOUT)
    public void deleteRollout() throws Exception {
        final Rollout rollout = createRolloutEntity();
        this.mockMvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}", rollout.getId()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print()).andDo(this.document.document(
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.NAME))));
    }

    @Test
    @Description("Handles the POST request of starting a rollout. Required Permission: " + SpPermission.HANDLE_ROLLOUT)
    public void startRollout() throws Exception {
        final Rollout rollout = createRolloutEntity();
        mockMvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/start", rollout.getId())
                .accept(MediaTypes.HAL_JSON_VALUE)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the POST request of pausing a rollout. Required Permission: " + SpPermission.HANDLE_ROLLOUT)
    public void pauseRollout() throws Exception {
        final Rollout rollout = createRolloutEntity();
        rolloutManagement.start(rollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutManagement.handleRollouts();

        mockMvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/pause", rollout.getId())
                .accept(MediaTypes.HAL_JSON_VALUE)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the POST request of resuming a rollout. Required Permission: " + SpPermission.HANDLE_ROLLOUT)
    public void resumeRollout() throws Exception {
        final Rollout rollout = createRolloutEntity();
        rolloutManagement.start(rollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutManagement.handleRollouts();

        rolloutManagement.pauseRollout(rollout.getId());
        mockMvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/resume", rollout.getId())
                .accept(MediaTypes.HAL_JSON_VALUE)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the POST request of approving a rollout. Required Permission: "
            + SpPermission.APPROVE_ROLLOUT)
    public void approveRollout() throws Exception {
        approvalStrategy.setApprovalNeeded(true);
        final Rollout rollout = createRolloutEntity();
        mockMvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/approve", rollout.getId())
                .accept(MediaTypes.HAL_JSON_VALUE)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the POST request of denying a rollout. Required Permission: " + SpPermission.APPROVE_ROLLOUT)
    public void denyRollout() throws Exception {
        approvalStrategy.setApprovalNeeded(true);
        final Rollout rollout = createRolloutEntity();
        mockMvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deny", rollout.getId())
                .accept(MediaTypes.HAL_JSON_VALUE)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving the deploy groups of a rollout. Required Permission: "
            + SpPermission.READ_ROLLOUT)
    public void getRolloutDeployGroups() throws Exception {
        final Rollout rollout = createRolloutEntity();

        mockMvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deploygroups", rollout.getId())
                .accept(MediaTypes.HAL_JSON_VALUE)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andDo(this.document.document(
                        getRolloutDeployGroupResponseFields(true, false,
                                fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("content").description(MgmtApiModelProperties.ROLLOUT_LIST)),
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving a deploy group of a rollout. Required Permission: "
            + SpPermission.READ_ROLLOUT)
    public void getRolloutDeployGroup() throws Exception {
        final Rollout rollout = createRolloutEntity();
        final RolloutGroup firstRolloutGroup = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent()
                .get(0);

        mockMvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deploygroups/{deployGroupId}",
                rollout.getId(), firstRolloutGroup.getId()).accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andDo(this.document.document(getRolloutDeployGroupResponseFields(false, true),
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("deployGroupId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    private Snippet getRolloutDeployGroupResponseFields(final boolean isArray, final boolean withDetails,
            final FieldDescriptor... descriptors) throws JsonProcessingException {
        final String arrayPrefix = getArrayPrefix(isArray);
        final List<FieldDescriptor> allFieldDescriptor = new ArrayList<>();
        allFieldDescriptor.addAll(Arrays.asList(descriptors));

        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "id").description(ApiModelPropertiesGeneric.ITEM_ID));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "name").description(ApiModelPropertiesGeneric.NAME));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "description").optional()
                .description(ApiModelPropertiesGeneric.DESCRPTION));
        allFieldDescriptor
                .add(fieldWithPath(arrayPrefix + "createdBy").description(ApiModelPropertiesGeneric.CREATED_BY));
        allFieldDescriptor
                .add(fieldWithPath(arrayPrefix + "createdAt").description(ApiModelPropertiesGeneric.CREATED_AT));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "lastModifiedBy").optional().type(JsonFieldType.STRING)
                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "lastModifiedAt").optional().type(JsonFieldType.NUMBER)
                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT));
        allFieldDescriptor
                .add(fieldWithPath(arrayPrefix + "status").description(MgmtApiModelProperties.ROLLOUT_GROUP_STATUS)
                        .attributes(key("value").value("['creating','ready','paused','running','finished']")));

        allFieldDescriptor.add(
                fieldWithPath(arrayPrefix + "totalTargets").description(MgmtApiModelProperties.ROLLOUT_TOTAL_TARGETS));

        if (withDetails) {
            allFieldDescriptor.add(fieldWithPath(arrayPrefix + "totalTargetsPerStatus")
                    .description(MgmtApiModelProperties.ROLLOUT_TOTAL_TARGETS_PER_STATUS));
        }

        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "targetFilterQuery")
                .description(MgmtApiModelProperties.ROLLOUT_GROUP_FILTER_QUERY));

        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "targetPercentage")
                .description(MgmtApiModelProperties.ROLLOUT_GROUP_TARGET_PERCENTAGE));

        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "successCondition")
                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "successCondition.condition")
                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION_CONDITION));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "successCondition.expression")
                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_CONDITION_EXP));

        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "successAction")
                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "successAction.action")
                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION_ACTION));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "successAction.expression")
                .description(MgmtApiModelProperties.ROLLOUT_SUCCESS_ACTION_EXP));

        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "errorCondition")
                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "errorCondition.condition")
                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION_CONDITION));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "errorCondition.expression")
                .description(MgmtApiModelProperties.ROLLOUT_ERROR_CONDITION_EXP));

        allFieldDescriptor.add(
                fieldWithPath(arrayPrefix + "errorAction").description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "errorAction.action")
                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION_ACTION));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "errorAction.expression")
                .description(MgmtApiModelProperties.ROLLOUT_ERROR_ACTION_EXP));

        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.self").ignored());

        return new DocumenationResponseFieldsSnippet(allFieldDescriptor);
    }

    @Test
    @Description("Handles the GET request of retrieving a deploy group of a rollout. Required Permission: "
            + SpPermission.READ_ROLLOUT)
    public void getRolloutDeployGroupWithParameters() throws Exception {
        final Rollout rollout = createRolloutEntity();
        final RolloutGroup firstRolloutGroup = rolloutGroupManagement
                .findByRollout(new PageRequest(0, 1), rollout.getId()).getContent().get(0);

        mockMvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deploygroups/{deployGroupId}",
                rollout.getId(), firstRolloutGroup.getId()).param("offset", "0").param("limit", "2")
                        .param("sort", "id:DESC").param("q", "id==" + firstRolloutGroup.getId())
                        .accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andDo(this.document.document(
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("deployGroupId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getFilterRequestParamter()));
    }

    @Test
    @Description("Handles the GET request of retrieving a all targets of a specific deploy group of a rollout. Required Permissions: "
            + SpPermission.READ_ROLLOUT + ", " + SpPermission.READ_TARGET)
    public void getRolloutDeployGroupTargets() throws Exception {
        final Rollout rollout = createRolloutEntity();
        final RolloutGroup firstRolloutGroup = rolloutGroupManagement
                .findByRollout(new PageRequest(0, 1), rollout.getId()).getContent().get(0);

        mockMvc.perform(
                get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deploygroups/{deployGroupId}/targets",
                        rollout.getId(), firstRolloutGroup.getId()).accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andDo(this.document.document(
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("deployGroupId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("content").description(MgmtApiModelProperties.TARGET_LIST),
                                fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("content[].address").optional().type(String.class)
                                        .description(MgmtApiModelProperties.ADDRESS),
                                fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("content[].name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("content[].description").optional().type(String.class)
                                        .description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("content[].controllerId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("content[].updateStatus")
                                        .description(MgmtApiModelProperties.UPDATE_STATUS).type("enum")
                                        .attributes(key("value")
                                                .value("['error', 'in_sync', 'pending', 'registered', 'unknown']")),
                                fieldWithPath("content[].securityToken")
                                        .description(MgmtApiModelProperties.SECURITY_TOKEN),
                                fieldWithPath("content[].installedAt").optional().type(Long.class)
                                        .description(MgmtApiModelProperties.INSTALLED_AT),
                                fieldWithPath("content[].lastModifiedAt").optional().type(Long.class)
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                                fieldWithPath("content[].lastModifiedBy").optional().type(String.class)
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                                fieldWithPath("content[].ipAddress").optional().type(String.class)
                                        .description(MgmtApiModelProperties.IP_ADDRESS).type("String"),
                                fieldWithPath("content[].lastControllerRequestAt").optional().type(Long.class)
                                        .description(MgmtApiModelProperties.LAST_REQUEST_AT).type("Number"),
                                fieldWithPath("content[]._links.self").ignored())));
    }

    @Test
    @Description("Handles the GET request of retrieving a all targets of a specific deploy group of a rollout. Required Permission: "
            + SpPermission.READ_ROLLOUT + ", " + SpPermission.READ_TARGET)
    public void getRolloutDeployGroupTargetsWithParameters() throws Exception {
        final Rollout rollout = createRolloutEntity();
        final RolloutGroup firstRolloutGroup = rolloutGroupManagement
                .findByRollout(new PageRequest(0, 1), rollout.getId()).getContent().get(0);

        mockMvc.perform(
                get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deploygroups/{deployGroupId}/targets",
                        rollout.getId(), firstRolloutGroup.getId()).param("offset", "0").param("limit", "2")
                                .param("sort", "name:ASC").param("q", "controllerId==exampleTarget0")
                                .accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_HAL_UTF))
                .andDo(this.document.document(
                        pathParameters(parameterWithName("rolloutId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("deployGroupId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getFilterRequestParamter()));
    }

    private Rollout createRolloutEntity() {
        testdataFactory.createTargets(20, "exampleTarget");
        final Rollout rollout = rolloutManagement.create(
                entityFactory.rollout().create().name("exampleRollout")
                        .targetFilterQuery("controllerId==exampleTarget*").set(testdataFactory.createDistributionSet()),
                10, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "10").build());

        // Run here, because Scheduler is disabled during tests
        rolloutManagement.handleRollouts();

        return rolloutManagement
                .update(entityFactory.rollout().update(rollout.getId()).description("exampleDescription"));
    }

}

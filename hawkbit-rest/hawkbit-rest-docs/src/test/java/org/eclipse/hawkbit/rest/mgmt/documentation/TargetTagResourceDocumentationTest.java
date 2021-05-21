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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtAssignedTargetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.rest.documentation.AbstractApiRestDocumentation;
import org.eclipse.hawkbit.rest.documentation.ApiModelPropertiesGeneric;
import org.eclipse.hawkbit.rest.documentation.DocumenationResponseFieldsSnippet;
import org.eclipse.hawkbit.rest.documentation.MgmtApiModelProperties;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Documentation generation for Management API for {@link TargetTag}.
 * 
 */
@Feature("Spring Rest Docs Tests - TargetTag")
@Story("TargetTag Resource")
public class TargetTagResourceDocumentationTest extends AbstractApiRestDocumentation {

    private DistributionSet distributionSet;

    @Override
    public String getResourceName() {
        return "targettag";
    }

    @BeforeEach
    public void setUp() {
        distributionSet = createDistributionSet();
    }

    @Test
    @Description("Handles the GET request of retrieving all target tags")
    public void getTargetTags() throws Exception {
        createTargetTagEntitiy();
        arrayPrefix = "content[].";
        mockMvc.perform(get(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(getResponseFieldTargetTag(true,
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("content").description(MgmtApiModelProperties.TARGET_LIST))));
    }

    @Test
    @Description("Handles the GET request of retrieving all targets tags within SP based by parameter")
    public void getTargetTagsWithParameters() throws Exception {
        createTargetTagEntitiy();
        mockMvc.perform(get(
                MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING + "?limit=10&sort=name:ASC&offset=0&q=name==targetTag"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(getFilterRequestParamter()));
    }

    @Test
    @Description("Handles the GET request of retrieving all target tag within SP. Required Permission: READ_TARGET.")
    public void getTargetTag() throws Exception {
        final Long tagId = createTargetTagId();
        mockMvc.perform(get(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING + "/{targetTagId}", tagId))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetTagId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getResponseFieldTargetTag(false)));
    }

    @Test
    @Description(" Handles the POST request of creating new target tags. The request body must always be a list of tags")
    public void createTargetTags() throws Exception {
        final MgmtTagRequestBodyPut bodyPut = new MgmtTagRequestBodyPut();
        bodyPut.setColour("red");
        bodyPut.setDescription("target tag description");
        bodyPut.setName("target tag");
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(Arrays.asList(bodyPut));
        this.mockMvc
                .perform(post(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING).content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(getRequestFieldsTargetTag(true), getResponseFieldTargetTag(true)));
    }

    @Test
    @Description("Handles the PUT request of updating a single target tag.")
    public void updateTagretTag() throws Exception {
        final Long tagId = createTargetTagId();
        final MgmtTagRequestBodyPut bodyPut = new MgmtTagRequestBodyPut();
        bodyPut.setColour("red");
        bodyPut.setDescription("target tag description");
        bodyPut.setName("target tag");
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(bodyPut);

        this.mockMvc
                .perform(put(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING + "/{targetTagId}", tagId).content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetTagId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getRequestFieldsTargetTag(false), getResponseFieldTargetTag(false)));

    }

    @Test
    @Description("Handles the DELETE request for a single target tag")
    public void deleteTargetTag() throws Exception {
        final Long tagId = createTargetTagId();
        this.mockMvc
                .perform(delete(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING + "/{targetTagId}", tagId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("targetTagId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving all assigned targets by the given")
    public void getAssignedTargets() throws Exception {
        final TargetTag tag = createTargetTagEntitiy();
        final Target target = createTargetByGivenNameWithAttributes("Target1", distributionSet);
        targetManagement.assignTag(Arrays.asList(target.getControllerId()), tag.getId());

        arrayPrefix = "content[].";
        this.mockMvc
                .perform(get(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING
                        + MgmtRestConstants.TARGET_TAG_TARGETS_REQUEST_MAPPING, tag.getId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetTagId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getResponseFieldTarget(true,
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("content").description(MgmtApiModelProperties.TARGET_LIST))));
    }

    @Test
    @Description("Handles the POST request to toggle the assignment of targets by the given tag id")
    public void toggleTagAssignment() throws Exception {
        final Target assignTarget = createTargetByGivenNameWithAttributes("AssignedTarget", distributionSet);
        final Target unassignTarget = createTargetByGivenNameWithAttributes("UnassignedTarget", distributionSet);
        final Long tagId = createTargetTagId();
        final MgmtAssignedTargetRequestBody assignedTargetRequestBody1 = new MgmtAssignedTargetRequestBody();
        assignedTargetRequestBody1
                .setControllerId(createTargetByGivenNameWithAttributes("Target1", distributionSet).getControllerId());
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(Arrays.asList(assignedTargetRequestBody1));

        this.mockMvc
                .perform(post(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING
                        + MgmtRestConstants.TARGET_TAG_TARGETS_REQUEST_MAPPING + "/toggleTagAssignment", tagId)
                                .content(json).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetTagId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getRequestFieldsTargetAssignment(true),
                        getResponseAssignmentResult(assignTarget, unassignTarget)

        ));
    }

    @Test
    @Description("Handles the POST request to assign targets to the given tag id")
    public void assignTargets() throws Exception {
        final Long tagId = createTargetTagId();
        final MgmtAssignedTargetRequestBody assignedTargetRequestBody1 = new MgmtAssignedTargetRequestBody();
        assignedTargetRequestBody1
                .setControllerId(createTargetByGivenNameWithAttributes("Target1", distributionSet).getControllerId());
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(Arrays.asList(assignedTargetRequestBody1));

        this.mockMvc
                .perform(post(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING
                        + MgmtRestConstants.TARGET_TAG_TARGETS_REQUEST_MAPPING, tagId).content(json)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetTagId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getRequestFieldsTargetAssignment(true), getResponseFieldTarget(true)));
    }

    @Test
    @Description("Handles the DELETE request to unassign one target from the given tag id")
    public void unassignTarget() throws Exception {
        final Long tagId = createTargetTagId();
        final Target target = testdataFactory.createTarget();

        this.mockMvc
                .perform(delete(
                        MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING
                                + MgmtRestConstants.TARGET_TAG_TARGETS_REQUEST_MAPPING + "/{controllerId}",
                        tagId, target.getControllerId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetTagId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("controllerId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    private Snippet getRequestFieldsTargetTag(final boolean isArray) {
        final String arrayPrefix = getArrayPrefix(isArray);

        return requestFields(requestFieldWithPath(arrayPrefix + "name").description(ApiModelPropertiesGeneric.NAME),
                requestFieldWithPath(arrayPrefix + "description").description(ApiModelPropertiesGeneric.DESCRPTION),
                optionalRequestFieldWithPath(arrayPrefix + "colour").description(ApiModelPropertiesGeneric.COLOUR));
    }

    private Snippet getRequestFieldsTargetAssignment(final boolean isArray) {
        final String arrayPrefix = getArrayPrefix(isArray);

        return requestFields(
                requestFieldWithPath(arrayPrefix + "controllerId").description(ApiModelPropertiesGeneric.ITEM_ID));
    }

    private Snippet getResponseFieldTargetTag(final boolean isArray) throws JsonProcessingException {
        return getResponseFieldTargetTag(isArray, new FieldDescriptor[0]);
    }

    private Snippet getResponseFieldTargetTag(final boolean isArray, final FieldDescriptor... descriptors)
            throws JsonProcessingException {
        final String arrayPrefix = getArrayPrefix(isArray);
        final List<FieldDescriptor> allFieldDescriptor = new ArrayList<>();
        allFieldDescriptor.addAll(Arrays.asList(descriptors));

        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "id").description(ApiModelPropertiesGeneric.ITEM_ID));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "name").description(ApiModelPropertiesGeneric.NAME));
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
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "colour").description(ApiModelPropertiesGeneric.COLOUR));
        allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.self").ignored());

        if (!isArray) {
            allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.assignedTargets")
                    .description(MgmtApiModelProperties.LINKS_ASSIGNED_TARGETS));
        }

        return new DocumenationResponseFieldsSnippet(allFieldDescriptor);
    }

    private Snippet getResponseAssignmentResult(final Target assignTarget, final Target unAssignTarget)
            throws JsonProcessingException {
        return responseFields(
                fieldWithPath("assignedTargets").description(MgmtApiModelProperties.ASSIGNED_TARGETS)
                        .type("Array[Object]"),

                fieldWithPath("unassignedTargets").description(MgmtApiModelProperties.UN_ASSIGNED_TARGETS)
                        .type("Array[Object]"));
    }

    private TargetTag createTargetTagEntitiy(final String name) {
        final TargetTag updateTargetTag = targetTagManagement.create(entityFactory.tag().create().name(name));
        return targetTagManagement.update(entityFactory.tag().update(updateTargetTag.getId())
                .description("My name is " + name).colour("default"));
    }

    private TargetTag createTargetTagEntitiy() {
        return createTargetTagEntitiy("targetTag");
    }

    private Long createTargetTagId() {
        return createTargetTagEntitiy().getId();
    }

}

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

import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtAssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
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
 * Documentation generation for Management API for {@link DistributionSetTag}.
 * 
 */
@Feature("Spring Rest Docs Tests - DistributionSetTag")
@Story("DistributionSet Tag Resource")
public class DistributionSetTagResourceDocumentationTest extends AbstractApiRestDocumentation {

    private DistributionSet distributionSet;

    @Override
    public String getResourceName() {
        return "distributionsettag";
    }

    @BeforeEach
    public void setUp() {
        distributionSet = createDistributionSet();
    }

    @Test
    @Description("Handles the GET request of retrieving all distribution set tags")
    public void getDistributionSetTags() throws Exception {
        createDistributionSetTag();
        arrayPrefix = "content[].";
        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(getResponseFieldsTag(true,
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("content").description(MgmtApiModelProperties.TARGET_LIST))));
    }

    @Test
    @Description("Handles the GET request of retrieving all distribution set tags based by parameter")
    public void getDistributionSetTagsWithParameters() throws Exception {
        createDistributionSetTag();
        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING
                + "?limit=10&sort=name:ASC&offset=0&q=name==DsTag")).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print()).andDo(this.document.document(getFilterRequestParamter()));
    }

    @Test
    @Description("Handles the GET request of retrieving a single distribution set tag")
    public void getDistributionSetTag() throws Exception {
        final Long tagId = createDistributionSetTagId();

        mockMvc.perform(
                get(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + "/{distributionsetTagId}", tagId))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionsetTagId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getResponseFieldsTag(false)));
    }

    @Test
    @Description("Handles the POST request of creating new distribution set tag. The request body must always be a list of tags.")
    public void createDistributionSetTags() throws Exception {
        final MgmtTagRequestBodyPut bodyPut = createDSBody();
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(Arrays.asList(bodyPut));
        this.mockMvc
                .perform(post(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING).content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(getRequestFieldsTag(true), getResponseFieldsTag(true)));
    }

    @Test
    @Description("Handles the PUT request of updating a single distribution set tag.")
    public void updateDistributionSetTag() throws Exception {
        final Long tagId = createDistributionSetTagId();
        final MgmtTagRequestBodyPut bodyPut = createDSBody();
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(bodyPut);

        this.mockMvc
                .perform(
                        put(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + "/{distributionsetTagId}", tagId)
                                .content(json).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("distributionsetTagId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getRequestFieldsTag(false), getResponseFieldsTag(false)));

    }

    private MgmtTagRequestBodyPut createDSBody() {
        final MgmtTagRequestBodyPut bodyPut = new MgmtTagRequestBodyPut();
        bodyPut.setColour("red");
        bodyPut.setDescription("ds tag description");
        bodyPut.setName("ds tag");
        return bodyPut;
    }

    @Test
    @Description("Handles the DELETE request for a single distribution set tag")
    public void deleteDistributionSetTag() throws Exception {
        final Long tagId = createDistributionSetTagId();
        this.mockMvc
                .perform(delete(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + "/{distributionsetTagId}",
                        tagId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionsetTagId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving all assigned distribution sets by the given tag id.")
    public void getAssignedDistributionSets() throws Exception {
        final DistributionSetTag tag = createDistributionSetTag();
        distributionSetManagement.assignTag(Arrays.asList(distributionSet.getId()), tag.getId());

        arrayPrefix = "content[].";
        this.mockMvc
                .perform(get(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING
                        + MgmtRestConstants.DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING, tag.getId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("distributionsetTagId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getResponseFieldsDistributionSet(true,
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("content").description(MgmtApiModelProperties.TARGET_LIST))));
    }

    @Test
    @Description("andles the POST request to toggle the assignment of distribution sets by the given tag id.")
    public void toggleTagAssignment() throws Exception {
        final DistributionSet unassignDS = testdataFactory.createDistributionSet("unassign");
        final Long tagId = createDistributionSetTagId();
        final MgmtAssignedDistributionSetRequestBody body = new MgmtAssignedDistributionSetRequestBody();
        body.setDistributionSetId(distributionSet.getId());
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(Arrays.asList(body));

        this.mockMvc
                .perform(post(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING
                        + MgmtRestConstants.DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING
                        + "/toggleTagAssignment", tagId).content(json).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("distributionsetTagId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getRequestFieldsDistributionSetAssignment(true),
                        getResponseAssignmentResult(distributionSet, unassignDS)

        ));
    }

    @Test
    @Description("Handles the POST request to assign distribution sets to the given tag id.")
    public void assignDistributionSets() throws Exception {
        final Long tagId = createDistributionSetTagId();
        final MgmtAssignedDistributionSetRequestBody body = new MgmtAssignedDistributionSetRequestBody();
        body.setDistributionSetId(distributionSet.getId());
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(Arrays.asList(body));

        this.mockMvc
                .perform(post(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING
                        + MgmtRestConstants.DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING, tagId).content(json)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("distributionsetTagId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getRequestFieldsDistributionSetAssignment(true), getResponseFieldsDistributionSet(true)));
    }

    @Test
    @Description("Handles the DELETE request to unassign one distribution set from the given tag id.")
    public void unassignDistributionSet() throws Exception {
        final Long tagId = createDistributionSetTagId();

        this.mockMvc
                .perform(delete(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING
                        + MgmtRestConstants.DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING
                        + "/{distributionsetId}", tagId, distributionSet.getId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionsetTagId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("distributionsetId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    private Snippet getRequestFieldsTag(final boolean isArray) {
        final String arrayPrefix = getArrayPrefix(isArray);

        return requestFields(requestFieldWithPath(arrayPrefix + "name").description(ApiModelPropertiesGeneric.NAME),
                requestFieldWithPath(arrayPrefix + "description").description(ApiModelPropertiesGeneric.DESCRPTION),
                optionalRequestFieldWithPath(arrayPrefix + "colour").description(ApiModelPropertiesGeneric.COLOUR));
    }

    private Snippet getRequestFieldsDistributionSetAssignment(final boolean isArray) {
        final String arrayPrefix = getArrayPrefix(isArray);

        return requestFields(requestFieldWithPath(arrayPrefix + "id").description(ApiModelPropertiesGeneric.ITEM_ID));
    }

    private Snippet getResponseFieldsTag(final boolean isArray, final FieldDescriptor... descriptors)
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
            allFieldDescriptor.add(fieldWithPath(arrayPrefix + "_links.assignedDistributionSets")
                    .description(MgmtApiModelProperties.LINKS_ASSIGNED_DISTRIBUTION_SETS));
        }

        return new DocumenationResponseFieldsSnippet(allFieldDescriptor);
    }

    private Snippet getResponseFieldsTag(final boolean isArray) throws JsonProcessingException {
        return getResponseFieldsTag(isArray, new FieldDescriptor[0]);
    }

    private Snippet getResponseAssignmentResult(final DistributionSet assignment, final DistributionSet unassignment)
            throws JsonProcessingException {
        return responseFields(
                fieldWithPath("assignedDistributionSets").description(MgmtApiModelProperties.ASSIGNED_DISTRIBUTION_SETS)
                        .type("Array[Object]"),

                fieldWithPath("unassignedDistributionSets")
                        .description(MgmtApiModelProperties.UN_ASSIGNED_DISTRIBUTION_SETS).type("Array[Object]"));
    }

    private DistributionSetTag createDistributionSetTag() {
        final DistributionSetTag updateTag = distributionSetTagManagement
                .create(entityFactory.tag().create().name("DsTag"));
        return distributionSetTagManagement.update(entityFactory.tag().update(updateTag.getId())
                .description("My name is " + updateTag.getName()).colour("default"));
    }

    private Long createDistributionSetTagId() {
        return createDistributionSetTag().getId();
    }

}

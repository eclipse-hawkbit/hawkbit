/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.mgmt.documentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.rest.documentation.AbstractApiRestDocumentation;
import org.eclipse.hawkbit.rest.documentation.ApiModelPropertiesGeneric;
import org.eclipse.hawkbit.rest.documentation.MgmtApiModelProperties;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation generation for Management API for {@link TargetType}.
 *
 */
@Feature("Spring Rest Docs Tests - TargetType")
@Story("TargetTypes Resource")
public class TargetTypesDocumentationTest extends AbstractApiRestDocumentation {
    @Override
    public String getResourceName() {
        return "targettypes";
    }

    @Test
    @Description("Handles the GET request of retrieving all target types within Hawkbit. Required Permission: "
            + SpPermission.READ_TARGET)
    public void getTargetTypes() throws Exception {
        testdataFactory.findOrCreateTargetType("targetType1");
        testdataFactory.createTargetType("targetType2", Collections.singletonList(standardDsType));

        mockMvc.perform(get(MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(responseFields(
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("content").description(MgmtApiModelProperties.TARGET_TYPE_LIST),
                        fieldWithPath("content[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                        fieldWithPath("content[].name").description(ApiModelPropertiesGeneric.NAME),
                        fieldWithPath("content[].description").description(ApiModelPropertiesGeneric.DESCRPTION),
                        fieldWithPath("content[].colour").description(ApiModelPropertiesGeneric.COLOUR),
                        fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                        fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                        fieldWithPath("content[].lastModifiedAt")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                        fieldWithPath("content[].lastModifiedBy")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                        fieldWithPath("content[]._links.self").ignored())));
    }

    @Test
    @Description("Handles the GET request of retrieving all target types within Hawkbit with a defined page size and "
            + "offset, sorted by name in descending order and filtered down to all targets which name starts with 'targetType'. "
            + "Required Permission: " + SpPermission.READ_TARGET)
    public void getTargetTypesWithParameters() throws Exception {
        testdataFactory.findOrCreateTargetType("targetType1");
        testdataFactory.createTargetType("targetType2", Collections.singletonList(standardDsType));

        mockMvc.perform(get(MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING).accept(MediaType.APPLICATION_JSON)
                .param("offset", "0").param("limit", "2").param("sort", "name:ASC").param("q", "name==targetType*"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(getFilterRequestParamter()));
    }

    @Test
    @Description("Handles the GET request for a single target type within Hawkbit. Required Permission: "
            + SpPermission.READ_TARGET)
    public void getTargetType() throws Exception {
        TargetType testType = testdataFactory.createTargetType("TargetType", Collections.singletonList(standardDsType));

        mockMvc.perform(get(MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}", testType.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("targetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("colour").description(ApiModelPropertiesGeneric.COLOUR),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("Number"),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("_links.compatibledistributionsettypes.href")
                                        .description(MgmtApiModelProperties.LINK_COMPATIBLE_DS_TYPES),
                                fieldWithPath("_links.self").ignored())));
    }

    @Test
    @Description("Handles the POST request for creating new target types within Hawkbit. The request body "
            + "must always be a list of types. Required Permission: " + SpPermission.CREATE_TARGET)
    public void postTargetTypes() throws Exception {
        final List<TargetType> types = new ArrayList<>();
        types.add(entityFactory.targetType().create().name("targetType1").description("targetType1 description")
                .colour("#ffffff").build());
        types.add(entityFactory.targetType().create().name("targetType2").description("targetType2 description")
                .colour("#000000").compatible(Collections.singletonList(standardDsType.getId())).build());

        mockMvc.perform(post(MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING)
                .content(JsonBuilder.targetTypesCreatableFieldsOnly(types)).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andDo(this.document.document(
                        requestFields(requestFieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                                optionalRequestFieldWithPath("[]description")
                                        .description(ApiModelPropertiesGeneric.DESCRPTION),
                                optionalRequestFieldWithPath("[]colour").description(ApiModelPropertiesGeneric.COLOUR),
                                optionalRequestFieldWithPath("[]compatibledistributionsettypes")
                                        .description(MgmtApiModelProperties.COMPATIBLE_DS_TYPES)),
                        responseFields(fieldWithPath("[]id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("[]description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("[]colour").description(ApiModelPropertiesGeneric.COLOUR),
                                fieldWithPath("[]createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("[]createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("[]lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                                fieldWithPath("[]lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                                fieldWithPath("[]_links.self").ignored())));
    }

    @Test
    @Description("Handles the DELETE request of retrieving a single target type within Hawkbit. Required Permission: "
            + SpPermission.DELETE_TARGET)
    public void deleteTargetType() throws Exception {
        TargetType testType = testdataFactory.createTargetType("TargetType", Collections.singletonList(standardDsType));

        mockMvc.perform(delete(MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}", testType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("targetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the PUT request for a single target type within Hawkbit. " + "Required Permission: "
            + SpPermission.UPDATE_TARGET)
    public void putTargetType() throws Exception {
        TargetType testType = testdataFactory.createTargetType("targetType", Collections.singletonList(standardDsType));
        final String body = new JSONObject().put("description", "an updated description").put("name", "updatedTypeName")
                .put("colour", "#aaafff").toString();

        this.mockMvc
                .perform(put(MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}", testType.getId())
                        .content(body).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("targetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(
                                optionalRequestFieldWithPath("description")
                                        .description(ApiModelPropertiesGeneric.DESCRPTION),
                                optionalRequestFieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                optionalRequestFieldWithPath("colour").description(ApiModelPropertiesGeneric.COLOUR)),
                        responseFields(fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("colour").description(ApiModelPropertiesGeneric.COLOUR),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("Number"),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("_links.compatibledistributionsettypes.href")
                                        .description(MgmtApiModelProperties.LINK_COMPATIBLE_DS_TYPES),
                                fieldWithPath("_links.self").ignored())));
    }

    @Test
    @Description("Handles the GET request of retrieving the list of compatible distribution set types in that target type. "
            + "Required Permission: " + SpPermission.READ_TARGET + " and " + SpPermission.READ_REPOSITORY)
    public void getCompatibleDistributionSetTypes() throws Exception {
        TargetType testType = testdataFactory.createTargetType("targetType", Collections.singletonList(standardDsType));

        mockMvc.perform(
                get(MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}/compatibledistributionsettypes",
                        testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("targetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("[]id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("[]key").description(MgmtApiModelProperties.DS_TYPE_KEY),
                                fieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("[]description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("[]colour").description(ApiModelPropertiesGeneric.COLOUR),
                                fieldWithPath("[]createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("[]createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("[]lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                                fieldWithPath("[]lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                                fieldWithPath("[]deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("[]_links.self").ignored())));
    }

    @Test
    @Description("Handles the POST request for adding a compatible distribution set type to a target type."
            + " Required Permission: " + SpPermission.UPDATE_TARGET + " and " + SpPermission.READ_REPOSITORY)
    public void postCompatibleDistributionSetTypes() throws Exception {
        final DistributionSetType dsType1 = distributionSetTypeManagement.create(
                entityFactory.distributionSetType().create().key("test1").name("TestName1").description("Desc1"));
        final DistributionSetType dsType2 = distributionSetTypeManagement.create(
                entityFactory.distributionSetType().create().key("test2").name("TestName2").description("Desc2"));
        final TargetType targetType = testdataFactory.createTargetType("targetType",
                Collections.singletonList(standardDsType));

        mockMvc.perform(
                post(MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}/compatibledistributionsettypes",
                        targetType.getId())
                                .content("[{\"id\":" + dsType1.getId() + "},{\"id\":" + dsType2.getId() + "}]")
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("targetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(requestFieldWithPath("[]id").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the DELETE request to unassign the list of compatible distribution set types in that target type. "
            + SpPermission.UPDATE_TARGET + " and " + SpPermission.READ_REPOSITORY)
    public void deleteCompatibleDistributionSetType() throws Exception {
        final DistributionSetType dsType = distributionSetTypeManagement.create(
                entityFactory.distributionSetType().create().key("test1").name("TestName1").description("Desc1"));
        final TargetType targetType = testdataFactory.createTargetType("targetType", Collections.singletonList(dsType));

        mockMvc.perform(delete(
                MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING
                        + "/{targetTypeId}/compatibledistributionsettypes/{distributionSetTypeId}",
                targetType.getId(), dsType.getId()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("targetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("distributionSetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

}

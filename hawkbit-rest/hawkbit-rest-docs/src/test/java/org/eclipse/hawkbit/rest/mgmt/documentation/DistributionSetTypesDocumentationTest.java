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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.documentation.AbstractApiRestDocumentation;
import org.eclipse.hawkbit.rest.documentation.ApiModelPropertiesGeneric;
import org.eclipse.hawkbit.rest.documentation.MgmtApiModelProperties;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Documentation generation for Management API for {@link DistributionSetType}.
 * 
 */
@Feature("Spring Rest Docs Tests - DistributionSetType")
@Story("DistributionSetTypes Resource")
public class DistributionSetTypesDocumentationTest extends AbstractApiRestDocumentation {

    @Override
    public String getResourceName() {
        return "distributionsettypes";
    }

    @Test
    @Description("Handles the GET request of retrieving all distribution set types within SP. Required Permission: READ_REPOSITORY.")
    public void getDistributionSetTypes() throws Exception {

        distributionSetTypeManagement.update(entityFactory.distributionSetType()
                .update(testdataFactory.createDistributionSet().getType().getId()).description("Desc1234"));

        mockMvc.perform(
                get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total", equalTo(4)))
                .andDo(this.document.document(responseFields(
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("content").description(MgmtApiModelProperties.TARGET_LIST),
                        fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                        fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                        fieldWithPath("content[].description").description(ApiModelPropertiesGeneric.DESCRPTION),
                        fieldWithPath("content[].key").description(MgmtApiModelProperties.DS_TYPE_KEY),
                        fieldWithPath("content[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                        fieldWithPath("content[].lastModifiedAt")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                        fieldWithPath("content[].lastModifiedBy")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                        fieldWithPath("content[].name").description(ApiModelPropertiesGeneric.NAME),
                        fieldWithPath("content[]._links.self").ignored())));
    }

    @Test
    @Description("Handles the GET request of retrieving all distribution set types within SP with a defined page size and offset, sortet by name in descending order and filtered down to all targets which name starts with 'a'. Required Permission: READ_REPOSITORY.")
    public void getDistributionSetTypesWithParameters() throws Exception {

        distributionSetTypeManagement.update(entityFactory.distributionSetType()
                .update(testdataFactory.createDistributionSet().getType().getId()).description("Desc1234"));
        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING).accept(MediaType.APPLICATION_JSON)
                .param("offset", "1").param("limit", "2").param("sort", "name:DESC").param("q", "name==a*"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(getFilterRequestParamter()));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Handles the POST request for creating new distribution set types within SP. The request body "
            + "must always be a list of types. Required Permission: CREATE_REPOSITORY.")
    public void postDistributionSetTypes() throws Exception {

        assertThat(distributionSetTypeManagement.count()).isEqualTo(4);

        final List<DistributionSetType> types = new ArrayList<>();
        types.add(entityFactory.distributionSetType().create().key("test1").name("TestName1").description("Desc1")
                .mandatory(osType).optional(runtimeType).build());

        mockMvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING)
                .content(JsonBuilder.distributionSetTypesCreateValidFieldsOnly(types))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andDo(
                        this.document.document(
                                requestFields(
                                        requestFieldWithPath("[]key").description(MgmtApiModelProperties.DS_TYPE_KEY),
                                        requestFieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                                        optionalRequestFieldWithPath("[]description")
                                                .description(ApiModelPropertiesGeneric.DESCRPTION),
                                        optionalRequestFieldWithPath("[]mandatorymodules")
                                                .description(MgmtApiModelProperties.DS_TYPE_MANDATORY_MODULES),
                                        optionalRequestFieldWithPath("[]optionalmodules")
                                                .description(MgmtApiModelProperties.DS_TYPE_OPTIONAL_MODULES)),
                                responseFields(

                                        fieldWithPath("[]createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                        fieldWithPath("[]createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                        fieldWithPath("[].description")
                                                .description(ApiModelPropertiesGeneric.DESCRPTION),
                                        fieldWithPath("[]key").description(MgmtApiModelProperties.DS_TYPE_KEY),
                                        fieldWithPath("[]id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                        fieldWithPath("[]lastModifiedAt")
                                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                                        fieldWithPath("[]lastModifiedBy")
                                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                                        fieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                                        fieldWithPath("[]deleted").description(ApiModelPropertiesGeneric.DELETED),
                                        fieldWithPath("[]_links.self").ignored())));
    }

    @Test
    @Description("Handles the GET request of retrieving a single distribution set type within SP. Required Permission: READ_REPOSITORY")
    public void deleteDistributionSetType() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.update(entityFactory.distributionSetType()
                .update(testdataFactory.createDistributionSet().getType().getId()).description("Desc1234"));

        mockMvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}",
                testType.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionSetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the DELETE request for a single distribution set type within SP. Required Permission: DELETE_REPOSITORY.")
    public void getDistributionSetType() throws Exception {

        final DistributionSetType testType = distributionSetTypeManagement.update(entityFactory.distributionSetType()
                .update(testdataFactory.createDistributionSet().getType().getId()).description("Desc1234"));

        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}",
                testType.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("distributionSetTypeId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("key").description(MgmtApiModelProperties.DS_TYPE_KEY),
                                fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("Number"),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("_links.self").ignored(),
                                fieldWithPath("_links.mandatorymodules")
                                        .description(MgmtApiModelProperties.LINK_TO_MANDATORY_SMT),
                                fieldWithPath("_links.optionalmodules")
                                        .description(MgmtApiModelProperties.LINK_TO_OPTIONAL_SMT))));
    }

    @Test
    @Description("Handles the GET request of retrieving all distribution set types within SP based on parameter. Required Permission: READ_REPOSITORY.")
    public void getDistributionSetTypesWithParameter() throws Exception {

        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING
                + "?limit=10&sort=name:ASC&offset=0&q=name==a")).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(requestParameters(
                        parameterWithName("limit").attributes(key("type").value("query"))
                                .description(ApiModelPropertiesGeneric.LIMIT),
                        parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                        parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                        parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL))));
    }

    @Test
    @Description("Handles the PUT request for a single distribution set type within SP. Required Permission: UPDATE_REPOSITORY.")
    public void putDistributionSetType() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.update(entityFactory.distributionSetType()
                .update(testdataFactory.createDistributionSet().getType().getId()).description("Desc1234"));
        final String body = new JSONObject().put("description", "an updated desription").toString();

        this.mockMvc
                .perform(put(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}",
                        testType.getId()).content(body).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("distributionSetTypeId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(
                                requestFieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION)),
                        responseFields(fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("key").description(MgmtApiModelProperties.DS_TYPE_KEY),
                                fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("Number"),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("_links.self").ignored(),
                                fieldWithPath("_links.mandatorymodules")
                                        .description(MgmtApiModelProperties.LINK_TO_MANDATORY_SMT),
                                fieldWithPath("_links.optionalmodules")
                                        .description(MgmtApiModelProperties.LINK_TO_OPTIONAL_SMT))));

    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Handles the GET request of retrieving the list of mandatory software module types in that distribution set type. Required Permission: READ_REPOSITORY.")
    public void getMandatoryModules() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("test1").name("TestName1").description("Desc1").mandatory(osType).optional(runtimeType));

        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING
                + "/{distributionSetTypeId}/mandatorymoduletypes", testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                .andDo(this.document.document(
                        pathParameters(parameterWithName("distributionSetTypeId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("[]createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("[]createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("[]description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("[]key").description(MgmtApiModelProperties.SM_TYPE_KEY),
                                fieldWithPath("[]maxAssignments")
                                        .description(MgmtApiModelProperties.SMT_MAX_ASSIGNMENTS),
                                fieldWithPath("[]id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("[]lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                                fieldWithPath("[]lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                                fieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("[]deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("[]_links.self").ignored())));

    }

    @Test
    @Description("Handles the POST request for adding a mandatory software module type to a distribution set type."
            + "Note that a DS type cannot be changed after it has been used by a DS. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY + " and " + SpPermission.READ_REPOSITORY)
    public void postMandatoryModule() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(
                entityFactory.distributionSetType().create().key("test1").name("TestName1").description("Desc1"));

        mockMvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING
                + "/{distributionSetTypeId}/mandatorymoduletypes", testType.getId())
                        .content("{\"id\":" + osType.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("distributionSetTypeId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(requestFieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving the list of software module types in that distribution set. "
            + "Note that a DS type cannot be changed after it has been used by a DS. Required permissions: "
            + SpPermission.UPDATE_REPOSITORY + " and " + SpPermission.READ_REPOSITORY)
    public void deleteMandatoryModule() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("test1").name("TestName1").description("Desc1").mandatory(osType).optional(runtimeType));
        mockMvc.perform(delete(
                MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING
                        + "/{distributionSetTypeId}/mandatorymoduletypes/{softwareModuleTypeId}",
                testType.getId(), osType.getId()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionSetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("softwareModuleTypeId").description(ApiModelPropertiesGeneric.ITEM_ID))));

    }

    @Test
    @Description("Handles the GET request of retrieving the single mandatory software module type in that distribution set type. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getMandatoryModule() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("test1").name("TestName1").description("Desc1").mandatory(osType).optional(runtimeType));

        mockMvc.perform(get(
                MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING
                        + "/{distributionSetTypeId}/mandatorymoduletypes/{softwareModuleTypeId}",
                testType.getId(), osType.getId()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionSetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("softwareModuleTypeId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("key").description(MgmtApiModelProperties.SM_TYPE_KEY),
                                fieldWithPath("maxAssignments").description(MgmtApiModelProperties.SMT_MAX_ASSIGNMENTS),
                                fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("Number"),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("_links.self").ignored())));
    }

    @Test
    @Description("Handles the GET request of retrieving the list of optional software module types in that distribution set type. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getOptionalModules() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("test1").name("TestName1").description("Desc1").mandatory(osType).optional(runtimeType));
        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING
                + "/{distributionSetTypeId}/optionalmoduletypes", testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(
                        pathParameters(parameterWithName("distributionSetTypeId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("[]createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("[]createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("[]description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("[]key").description(MgmtApiModelProperties.SM_TYPE_KEY),
                                fieldWithPath("[]maxAssignments")
                                        .description(MgmtApiModelProperties.SMT_MAX_ASSIGNMENTS),
                                fieldWithPath("[]id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("[]lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                                fieldWithPath("[]lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                                fieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("[]deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("[]_links.self").ignored())));
    }

    @Test
    @Description("Handles the POST request for adding  an optional software module type to a distribution set type."
            + "Note that a DS type cannot be changed after it has been used by a DS. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY + " and " + SpPermission.READ_REPOSITORY)
    public void postOptionalModule() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(
                entityFactory.distributionSetType().create().key("test1").name("TestName1").description("Desc1"));
        mockMvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING
                + "/{distributionSetTypeId}/optionalmoduletypes", testType.getId())
                        .content("{\"id\":" + appType.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("distributionSetTypeId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(requestFieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID))));

    }

    @Test
    @Description("Handles DELETE request for removing an optional module from the distribution set type."
            + "Note that a DS type cannot be changed after it has been used by a DS. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY + " and " + SpPermission.READ_REPOSITORY)
    public void deleteOptionalModule() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("test1").name("TestName1").description("Desc1").mandatory(osType).optional(runtimeType));
        mockMvc.perform(delete(
                MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING
                        + "/{distributionSetTypeId}/optionalmoduletypes/{softwareModuleTypeId}",
                testType.getId(), appType.getId()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionSetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("softwareModuleTypeId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving the single optional software module type in that distribution set type. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getOptionalModule() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType()
                .create().key("test1").name("TestName1").description("Desc1").mandatory(osType).optional(appType));

        mockMvc.perform(get(
                MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING
                        + "/{distributionSetTypeId}/optionalmoduletypes/{softwareModuleTypeId}",
                testType.getId(), appType.getId()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionSetTypeId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("softwareModuleTypeId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("key").description(MgmtApiModelProperties.SM_TYPE_KEY),
                                fieldWithPath("maxAssignments").description(MgmtApiModelProperties.SMT_MAX_ASSIGNMENTS),
                                fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("Number"),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("_links.self").ignored())));
    }

}

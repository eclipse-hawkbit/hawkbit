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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
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
 * Documentation generation for Management API for {@link SoftwareModuleType}.
 */
@Feature("Spring Rest Docs Tests - SoftwareModuleType")
@Story("Softwaremoduletypes Resource")
public class SoftwaremoduleTypesDocumentationTest extends AbstractApiRestDocumentation {

    @Override
    public String getResourceName() {
        return "softwaremoduletypes";
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Handles the GET request of retrieving all software module types within SP. Required Permission: READ_REPOSITORY.")
    public void getSoftwareModuleTypes() throws Exception {

        final SoftwareModuleType testType = testdataFactory.findOrCreateSoftwareModuleType("test123");
        softwareModuleTypeManagement
                .update(entityFactory.softwareModuleType().update(testType.getId()).description("Desc1234"));

        mockMvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(responseFields(
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("content").description(MgmtApiModelProperties.TARGET_LIST),
                        fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                        fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                        fieldWithPath("content[].description").description(ApiModelPropertiesGeneric.DESCRPTION),
                        fieldWithPath("content[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                        fieldWithPath("content[].lastModifiedAt")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                        fieldWithPath("content[].lastModifiedBy")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                        fieldWithPath("content[].name").description(ApiModelPropertiesGeneric.NAME),
                        fieldWithPath("content[]._links.self").ignored())));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Handles the GET request of retrieving all software module types within SP with parametsrs. In this case the first 10 result in ascending order by name wher the name starts with 'a'. Required Permission: READ_REPOSITORY.")
    public void getSoftwareModuleTypesWithParameters() throws Exception {
        final SoftwareModuleType testType = testdataFactory.findOrCreateSoftwareModuleType("test123");
        softwareModuleTypeManagement
                .update(entityFactory.softwareModuleType().update(testType.getId()).description("Desc1234"));

        mockMvc.perform(get(
                MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING + "?limit=10&sort=name:ASC&offset=0&q=name==a")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(getFilterRequestParamter()));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Handles the POST request of creating new software module types within SP. The request body must "
            + "always be a list of module types. Required Permission: CREATE_REPOSITORY.")
    public void postSoftwareModuleTypes() throws Exception {

        final List<SoftwareModuleType> types = Arrays.asList(entityFactory.softwareModuleType().create().key("test1")
                .name("TestName1").description("Desc1").build());

        this.mockMvc
                .perform(post(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING)
                        .content(JsonBuilder.softwareModuleTypesCreatableFieldsOnly(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        requestFields(
                                optionalRequestFieldWithPath("[]description")
                                        .description(ApiModelPropertiesGeneric.DESCRPTION),
                                optionalRequestFieldWithPath("[]key").description(MgmtApiModelProperties.SMT_KEY),
                                optionalRequestFieldWithPath("[]maxAssignments")
                                        .description(MgmtApiModelProperties.SM_MAX_ASSIGNMENTS),
                                requestFieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME)),
                        responseFields(fieldWithPath("[]key").description(MgmtApiModelProperties.SMT_KEY),
                                fieldWithPath("[]maxAssignments")
                                        .description(MgmtApiModelProperties.SMT_MAX_ASSIGNMENTS),
                                fieldWithPath("[]createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("[]createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("[]description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("[]deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("[]id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("[]lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("Number"),
                                fieldWithPath("[]lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                                fieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),

                                fieldWithPath("[]_links.self").ignored())));

    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Handles the DELETE request for a single software module Type within SP. Required Permission: DELETE_REPOSITORY.")
    public void deleteSoftwareModuleType() throws Exception {
        final SoftwareModuleType testType = softwareModuleTypeManagement.create(
                entityFactory.softwareModuleType().create().key("test1").name("TestName1").description("Desc1"));
        assertThat(softwareModuleTypeManagement.count()).isEqualTo(4);
        this.mockMvc
                .perform(delete(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING + "/{softwareModuleTypeID}",
                        testType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("softwareModuleTypeID").description(ApiModelPropertiesGeneric.ITEM_ID))));
        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Handles the GET request of retrieving a single software module type within SP. Required Permission: READ_REPOSITORY.")
    public void getSoftwareModuleType() throws Exception {
        final SoftwareModuleType testType = testdataFactory.findOrCreateSoftwareModuleType("test123");
        softwareModuleTypeManagement
                .update(entityFactory.softwareModuleType().update(testType.getId()).description("Desc1234"));

        this.mockMvc
                .perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING + "/{softwareModuleTypeID}",
                        testType.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("softwareModuleTypeID")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("key").description(MgmtApiModelProperties.SMT_KEY),
                                fieldWithPath("maxAssignments").description(MgmtApiModelProperties.SMT_MAX_ASSIGNMENTS),
                                fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("Number"),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),

                                fieldWithPath("_links.self").ignored())));
    }

    @Test
    @Description("Handles the PUT request for a single software module type within SP. Required Permission: UPDATE_REPOSITORY.")
    public void putSoftwareModuleType() throws Exception {
        final SoftwareModuleType testType = testdataFactory.findOrCreateSoftwareModuleType("test123");

        final String body = new JSONObject().put("description", "a new description").toString();

        this.mockMvc
                .perform(put(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING + "/{softwareModuleTypeID}",
                        testType.getId()).content(body.toString()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("softwareModuleTypeID")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(
                                requestFieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION)),
                        responseFields(fieldWithPath("key").description(MgmtApiModelProperties.SMT_KEY),
                                fieldWithPath("maxAssignments").description(MgmtApiModelProperties.SMT_MAX_ASSIGNMENTS),
                                fieldWithPath("createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("deleted").description(ApiModelPropertiesGeneric.DELETED),
                                fieldWithPath("id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT)
                                        .type("Number"),
                                fieldWithPath("lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY)
                                        .type("String"),
                                fieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("_links.self").ignored())));

    }

}

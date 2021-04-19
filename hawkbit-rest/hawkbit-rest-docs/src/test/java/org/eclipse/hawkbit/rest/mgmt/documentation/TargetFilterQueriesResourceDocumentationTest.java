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
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.rest.documentation.AbstractApiRestDocumentation;
import org.eclipse.hawkbit.rest.documentation.ApiModelPropertiesGeneric;
import org.eclipse.hawkbit.rest.documentation.MgmtApiModelProperties;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Documentation generation for Management API for {@link TargetFilterQuery}.
 */
@Feature("Spring Rest Docs Tests - TargetFilterQuery")
@Story("TargetFilterQuery Resource")
public class TargetFilterQueriesResourceDocumentationTest extends AbstractApiRestDocumentation {

    private static final String EXAMPLE_TFQ_NAME = "filter1";
    private static final String EXAMPLE_TFQ_QUERY = "name==*";

    @Override
    public String getResourceName() {
        return "targetfilters";
    }

    @Test
    @Description("Handles the GET request of retrieving all target filter queries within SP. Required Permission: READ_TARGET.")
    public void getTargetFilterQueries() throws Exception {

        createTargetFilterQueryWithDS(createDistributionSet());

        mockMvc.perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(responseFields(
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("content").description(MgmtApiModelProperties.TARGET_FILTER_QUERIES_LIST),
                        fieldWithPath("content[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                        fieldWithPath("content[].name").description(ApiModelPropertiesGeneric.NAME),
                        fieldWithPath("content[].query").description(MgmtApiModelProperties.TARGET_FILTER_QUERY),
                        fieldWithPath("content[].autoAssignDistributionSet")
                                .description(MgmtApiModelProperties.TARGET_FILTER_QUERY_AUTO_ASSIGN_DS_ID)
                                .type(JsonFieldType.NUMBER.toString()),
                        fieldWithPath("content[].autoAssignActionType")
                                .description(MgmtApiModelProperties.ACTION_FORCE_TYPE)
                                .type(JsonFieldType.STRING.toString())
                                .attributes(key("value").value("['forced', 'soft', 'downloadonly']")),
                        fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                        fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                        fieldWithPath("content[].lastModifiedAt")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                        fieldWithPath("content[].lastModifiedBy")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                        fieldWithPath("content[]._links.self").ignored())));
    }

    @Test
    @Description("Handles the GET request of retrieving all target filter queries within SP based by parameter. Required Permission: READ_TARGET.")
    public void getTargetFilterQueriesWithParameters() throws Exception {

        mockMvc.perform(
                get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "?limit=10&sort=name:ASC&offset=0&q=name==*1"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(requestParameters(
                        parameterWithName("limit").attributes(key("type").value("query"))
                                .description(ApiModelPropertiesGeneric.LIMIT),
                        parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                        parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                        parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL))));
    }

    @Test
    @Description("Handles the POST request of creating a new target filter query within SP. Required Permission: CREATE_TARGET.")
    public void postTargetFilterQuery() throws Exception {
        final String tfqJson = createTargetFilterQueryJson(EXAMPLE_TFQ_NAME, EXAMPLE_TFQ_QUERY);

        this.mockMvc
                .perform(post(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON).content(tfqJson))
                .andExpect(status().isCreated()).andDo(MockMvcResultPrinter.print()).andDo(
                        this.document.document(
                                requestFields(requestFieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                        requestFieldWithPath("query")
                                                .description(MgmtApiModelProperties.TARGET_FILTER_QUERY)),
                                getResponseFieldTargetFilterQuery(false)));

    }

    @Test
    @Description("Handles the DELETE request of deleting a single target filter query within SP. Required Permission: DELETE_TARGET.")
    public void deleteTargetFilterQuery() throws Exception {
        final TargetFilterQuery tfq = createTargetFilterQuery();
        this.mockMvc
                .perform(delete(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/{targetFilterQueryId}",
                        tfq.getId()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("targetFilterQueryId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving a single target filter query within SP. Required Permission: READ_TARGET.")
    public void getTargetFilterQuery() throws Exception {
        final TargetFilterQuery tfq = createTargetFilterQueryWithDS(createDistributionSet());

        mockMvc.perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/{targetFilterQueryId}", tfq.getId()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetFilterQueryId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getResponseFieldTargetFilterQuery(false)));
    }

    @Test
    @Description("Handles the PUT request of updating a target filter query within SP. Required Permission: UPDATE_TARGET.")
    public void putTargetFilterQuery() throws Exception {
        final TargetFilterQuery tfq = createTargetFilterQuery();
        final String targetAsJson = createTargetFilterQueryJson("newFilterName", "attribute.hwRevision==2");
        this.mockMvc
                .perform(put(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/{targetFilterQueryId}", tfq.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(targetAsJson))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetFilterQueryId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(optionalRequestFieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                optionalRequestFieldWithPath("query")
                                        .description(MgmtApiModelProperties.TARGET_FILTER_QUERY)),
                        getResponseFieldTargetFilterQuery(false)));

    }

    @Test
    @Description("Handles the GET request of retrieving a the auto assign distribution set of a target filter query within SP. Required Permission: READ_TARGET.")
    public void getAssignDS() throws Exception {
        final TargetFilterQuery tfq = createTargetFilterQueryWithDS(createDistributionSet());

        mockMvc.perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/{targetFilterQueryId}/autoAssignDS",
                tfq.getId())).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetFilterQueryId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getResponseFieldsDistributionSet(false)));
    }

    @Test
    @Description("Handles the POST request of setting a distribution set for auto assignment within SP. Required Permission: CREATE_TARGET.")
    public void postAutoAssignDS() throws Exception {
        enableMultiAssignments();
        final TargetFilterQuery tfq = createTargetFilterQuery();
        final DistributionSet distributionSet = createDistributionSet();
        final String autoAssignBody = new JSONObject().put("id", distributionSet.getId())
                .put("type", MgmtActionType.SOFT.getName()).put("weight", 200).toString();

        this.mockMvc
                .perform(
                        post(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/{targetFilterQueryId}/autoAssignDS",
                                tfq.getId()).contentType(MediaType.APPLICATION_JSON).content(autoAssignBody.toString()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(parameterWithName("targetFilterQueryId")
                                .description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(requestFieldWithPath("id").description(MgmtApiModelProperties.DS_ID),
                                optionalRequestFieldWithPath("type")
                                        .description(MgmtApiModelProperties.ACTION_FORCE_TYPE)
                                        .attributes(key("value").value("['forced', 'soft', 'downloadonly']")),
                                requestFieldWithPathMandatoryInMultiAssignMode("weight")
                                        .description(MgmtApiModelProperties.RESULTING_ACTIONS_WEIGHT)
                                        .attributes(key("value").value("0 - 1000"))),
                        getResponseFieldTargetFilterQuery(false)));
    }

    @Test
    @Description("Handles the DELETE request of deleting the auto assign distribution set from a target filter query within SP. Required Permission: DELETE_TARGET.")
    public void deleteAutoAssignDS() throws Exception {
        final TargetFilterQuery tfq = createTargetFilterQueryWithDS(createDistributionSet());
        this.mockMvc
                .perform(delete(
                        MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/{targetFilterQueryId}/autoAssignDS",
                        tfq.getId()))
                .andExpect(status().isNoContent()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("targetFilterQueryId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    private Snippet getResponseFieldTargetFilterQuery(final boolean isArray) {
        final String arrayPrefix = getArrayPrefix(isArray);
        return responseFields(fieldWithPath(arrayPrefix + "id").description(ApiModelPropertiesGeneric.ITEM_ID),
                fieldWithPath(arrayPrefix + "name").description(ApiModelPropertiesGeneric.NAME),
                fieldWithPath(arrayPrefix + "query").description(MgmtApiModelProperties.TARGET_FILTER_QUERY),
                fieldWithPath(arrayPrefix + "autoAssignDistributionSet")
                        .description(MgmtApiModelProperties.TARGET_FILTER_QUERY_AUTO_ASSIGN_DS_ID)
                        .type(JsonFieldType.NUMBER.toString()),
                fieldWithPath(arrayPrefix + "autoAssignActionType")
                        .description(MgmtApiModelProperties.ACTION_FORCE_TYPE).type(JsonFieldType.STRING.toString())
                        .attributes(key("value").value("['forced', 'soft', 'downloadonly']")),
                fieldWithPath(arrayPrefix + "autoAssignWeight")
                        .description(MgmtApiModelProperties.RESULTING_ACTIONS_WEIGHT)
                        .type(JsonFieldType.NUMBER.toString()),
                fieldWithPath(arrayPrefix + "createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                fieldWithPath(arrayPrefix + "createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                fieldWithPath(arrayPrefix + "lastModifiedAt").description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                fieldWithPath(arrayPrefix + "lastModifiedBy").description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                fieldWithPath(arrayPrefix + "_links.self").ignored(), fieldWithPath(arrayPrefix + "_links.autoAssignDS")
                        .description(MgmtApiModelProperties.TARGET_FILTER_QUERY_LINK_AUTO_ASSIGN_DS));
    }

    private String createTargetFilterQueryJson(final String name, final String query)
            throws JsonProcessingException {
        final Map<String, Object> target = new HashMap<>();
        target.put("name", name);
        target.put("query", query);
        return objectMapper.writeValueAsString(target);
    }

    private TargetFilterQuery createTargetFilterQuery() {
        return targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(EXAMPLE_TFQ_NAME).query(EXAMPLE_TFQ_QUERY));
    }

    private TargetFilterQuery createTargetFilterQueryWithDS(final DistributionSet distributionSet) {
        final TargetFilterQuery targetFilterQuery = createTargetFilterQuery();
        return targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                .updateAutoAssign(targetFilterQuery.getId()).ds(distributionSet.getId()));
    }
}

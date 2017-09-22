/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.rest.exception.MessageNotReadableException;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Spring MVC Tests against the MgmtTargetResource.
 *
 */
@Features("Component Tests - Management API")
@Stories("Target Filter Query Resource")
public class MgmtTargetFilterQueryResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String JSON_PATH_ROOT = "$";

    // fields, attributes
    private static final String JSON_PATH_FIELD_ID = ".id";
    private static final String JSON_PATH_FIELD_NAME = ".name";
    private static final String JSON_PATH_FIELD_QUERY = ".query";
    private static final String JSON_PATH_FIELD_CONTENT = ".content";
    private static final String JSON_PATH_FIELD_SIZE = ".size";
    private static final String JSON_PATH_FIELD_TOTAL = ".total";
    private static final String JSON_PATH_FIELD_AUTO_ASSIGN_DS = ".autoAssignDistributionSet";

    // target
    // $.field
    static final String JSON_PATH_PAGED_LIST_CONTENT = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTENT;
    static final String JSON_PATH_PAGED_LIST_SIZE = JSON_PATH_ROOT + JSON_PATH_FIELD_SIZE;
    static final String JSON_PATH_PAGED_LIST_TOTAL = JSON_PATH_ROOT + JSON_PATH_FIELD_TOTAL;

    private static final String JSON_PATH_NAME = JSON_PATH_ROOT + JSON_PATH_FIELD_NAME;
    private static final String JSON_PATH_ID = JSON_PATH_ROOT + JSON_PATH_FIELD_ID;
    private static final String JSON_PATH_QUERY = JSON_PATH_ROOT + JSON_PATH_FIELD_QUERY;
    private static final String JSON_PATH_AUTO_ASSIGN_DS = JSON_PATH_ROOT + JSON_PATH_FIELD_AUTO_ASSIGN_DS;

    @Test
    @Description("Ensures that deletion is executed if permitted.")
    public void deleteTargetFilterQueryReturnsOK() throws Exception {
        final String filterName = "filter_01";
        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery(filterName, "name=test_01");

        mvc.perform(delete(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + filterQuery.getId()))
                .andExpect(status().isOk());

        assertThat(targetFilterQueryManagement.get(filterQuery.getId())).isNotPresent();
    }

    @Test
    @Description("Ensures that deletion is refused with not found if target does not exist.")
    public void deleteTargetWhichDoesNotExistsLeadsToEntityNotFound() throws Exception {
        final String notExistingId = "4395";

        mvc.perform(delete(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + notExistingId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that update is refused with not found if target does not exist.")
    public void updateTargetWhichDoesNotExistsLeadsToEntityNotFound() throws Exception {
        final String notExistingId = "4395";
        mvc.perform(put(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + notExistingId).content("{}")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that update request is reflected by repository.")
    public void updateTargetFilterQueryQuery() throws Exception {
        final String filterName = "filter_02";
        final String filterQuery = "name=test_02";
        final String filterQuery2 = "name=test_02_changed";
        final String body = new JSONObject().put("query", filterQuery2).toString();

        // prepare
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(filterName, filterQuery);

        mvc.perform(put(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + tfq.getId()).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(tfq.getId().intValue())))
                .andExpect(jsonPath("$.query", equalTo(filterQuery2)))
                .andExpect(jsonPath("$.name", equalTo(filterName)));

        final TargetFilterQuery tfqCheck = targetFilterQueryManagement.get(tfq.getId()).get();
        assertThat(tfqCheck.getQuery()).isEqualTo(filterQuery2);
        assertThat(tfqCheck.getName()).isEqualTo(filterName);
    }

    @Test
    @Description("Ensures that update request is reflected by repository.")
    public void updateTargetFilterQueryName() throws Exception {
        final String filterName = "filter_03";
        final String filterName2 = "filter_03_changed";
        final String filterQuery = "name=test_03";
        final String body = new JSONObject().put("name", filterName2).toString();

        // prepare
        final TargetFilterQuery tfq = targetFilterQueryManagement.create(
                entityFactory.targetFilterQuery().create().name(filterName).query(filterQuery));

        mvc.perform(put(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + tfq.getId()).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(tfq.getId().intValue())))
                .andExpect(jsonPath("$.query", equalTo(filterQuery)))
                .andExpect(jsonPath("$.name", equalTo(filterName2)));

        final TargetFilterQuery tfqCheck = targetFilterQueryManagement.get(tfq.getId()).get();
        assertThat(tfqCheck.getQuery()).isEqualTo(filterQuery);
        assertThat(tfqCheck.getName()).isEqualTo(filterName2);
    }

    @Test
    @Description("Ensures that request returns list of filters in defined format.")
    public void getTargetFilterQueryWithoutAdditionalRequestParameters() throws Exception {
        final int knownTargetAmount = 3;
        final String idA = "a";
        final String idB = "b";
        final String idC = "c";
        final String testQuery = "name=test";

        createSingleTargetFilterQuery(idA, testQuery);
        createSingleTargetFilterQuery(idB, testQuery);
        createSingleTargetFilterQuery(idC, testQuery);

        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(knownTargetAmount)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name==" + idA + ")].name", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name==" + idA + ")].query", contains(testQuery)))
                // idB
                .andExpect(jsonPath("$.content.[?(@.name==" + idB + ")].name", contains(idB)))
                .andExpect(jsonPath("$.content.[?(@.name==" + idB + ")].query", contains(testQuery)))
                // idC
                .andExpect(jsonPath("$.content.[?(@.name==" + idC + ")].name", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name==" + idC + ")].query", contains(testQuery)));
    }

    @Test
    @Description("Ensures that request returns list of filters in defined format in size reduced by given limit parameter.")
    public void getTargetWithPagingLimitRequestParameter() throws Exception {
        final int limitSize = 1;
        final int knownTargetAmount = 3;
        final String idA = "a";
        final String idB = "b";
        final String idC = "c";
        final String testQuery = "name=test";

        createSingleTargetFilterQuery(idA, testQuery);
        createSingleTargetFilterQuery(idB, testQuery);
        createSingleTargetFilterQuery(idC, testQuery);

        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name==" + idA + ")].name", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name==" + idA + ")].query", contains(testQuery)));
    }

    @Test
    @Description("Ensures that request returns list of filters in defined format in size reduced by given limit and offset parameter.")
    public void getTargetWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int knownTargetAmount = 5;
        final int offsetParam = 2;
        final int expectedSize = knownTargetAmount - offsetParam;
        final String idC = "c";
        final String idD = "d";
        final String idE = "e";
        final String testQuery = "name=test";

        createSingleTargetFilterQuery("a", testQuery);
        createSingleTargetFilterQuery("b", testQuery);
        createSingleTargetFilterQuery(idC, testQuery);
        createSingleTargetFilterQuery(idD, testQuery);
        createSingleTargetFilterQuery(idE, testQuery);

        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(knownTargetAmount)))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name==" + idC + ")].name", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name==" + idC + ")].query", contains(testQuery)))
                // idB
                .andExpect(jsonPath("$.content.[?(@.name==" + idD + ")].name", contains(idD)))
                .andExpect(jsonPath("$.content.[?(@.name==" + idD + ")].query", contains(testQuery)))
                // idC
                .andExpect(jsonPath("$.content.[?(@.name==" + idE + ")].name", contains(idE)))
                .andExpect(jsonPath("$.content.[?(@.name==" + idE + ")].query", contains(testQuery)));
    }

    @Test
    public void getSingleTarget() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownQuery = "name=test01";
        final String knownName = "someName";
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);
        final String hrefPrefix = "http://localhost" + MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/"
                + tfq.getId();
        // test
        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + tfq.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownName)))
                .andExpect(jsonPath(JSON_PATH_QUERY, equalTo(knownQuery)))
                .andExpect(jsonPath("$._links.self.href", equalTo(hrefPrefix)))
                .andExpect(jsonPath("$._links.autoAssignDS.href", equalTo(hrefPrefix + "/autoAssignDS")));
    }

    @Test
    public void getSingleTargetNoExistsResponseNotFound() throws Exception {
        final String targetIdNotExists = "546546";
        // test

        final MvcResult mvcResult = mvc
                .perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + targetIdNotExists))
                .andExpect(status().isNotFound()).andReturn();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_ENTITY_NOT_EXISTS.getKey());
    }

    @Test
    public void createTargetFilterQueryWithBadPayloadBadRequest() throws Exception {
        final String notJson = "abc";

        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING).content(notJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetFilterQueryManagement.count()).isEqualTo(0);

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(MessageNotReadableException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getKey());
    }

    @Test
    public void setAutoAssignDistributionSetToTargetFilterQuery() throws Exception {

        final String knownQuery = "name=test05";
        final String knownName = "filter05";

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + tfq.getId() + "/autoAssignDS")
                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(
                targetFilterQueryManagement.get(tfq.getId()).get().getAutoAssignDistributionSet())
                        .isEqualTo(set);

        final String hrefPrefix = "http://localhost" + MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/"
                + tfq.getId();

        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + tfq.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownName)))
                .andExpect(jsonPath(JSON_PATH_QUERY, equalTo(knownQuery)))
                .andExpect(jsonPath(JSON_PATH_AUTO_ASSIGN_DS, equalTo(set.getId().intValue())))
                .andExpect(jsonPath("$._links.self.href", equalTo(hrefPrefix)))
                .andExpect(jsonPath("$._links.autoAssignDS.href", equalTo(hrefPrefix + "/autoAssignDS")));
    }

    @Test
    public void deleteAutoAssignDistributionSetOfTargetFilterQuery() throws Exception {

        final String knownQuery = "name=test06";
        final String knownName = "filter06";
        final String dsName = "testDS";

        final DistributionSet set = testdataFactory.createDistributionSet(dsName);
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);
        targetFilterQueryManagement.updateAutoAssignDS(tfq.getId(), set.getId());

        assertThat(
                targetFilterQueryManagement.get(tfq.getId()).get().getAutoAssignDistributionSet())
                        .isEqualTo(set);

        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + tfq.getId() + "/autoAssignDS"))
                .andExpect(status().isOk()).andExpect(jsonPath(JSON_PATH_NAME, equalTo(dsName)));

        mvc.perform(delete(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + tfq.getId() + "/autoAssignDS"))
                .andExpect(status().isNoContent());

        assertThat(
                targetFilterQueryManagement.get(tfq.getId()).get().getAutoAssignDistributionSet())
                        .isNull();

        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING + "/" + tfq.getId() + "/autoAssignDS"))
                .andExpect(status().isNoContent());

    }

    private TargetFilterQuery createSingleTargetFilterQuery(final String name, final String query) {
        return targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(name).query(query));
    }

}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.rest.util.MockMvcResultPrinter.print;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRestModelMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.ResourceUtility;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.Create;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.DeletedException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.rest.exception.MessageNotReadableException;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriUtils;

/**
 * Spring MVC Tests against the MgmtTargetResource.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Target Filter Query Resource
 */
public class MgmtTargetFilterQueryResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String JSON_PATH_ROOT = "$";

    // fields, attributes
    private static final String JSON_PATH_FIELD_ID = ".id";
    private static final String JSON_PATH_FIELD_NAME = ".name";
    private static final String JSON_PATH_FIELD_QUERY = ".query";
    private static final String JSON_PATH_FIELD_CONFIRMATION_REQUIRED = ".confirmationRequired";
    private static final String JSON_PATH_FIELD_CONTENT = ".content";
    // target
    // $.field
    static final String JSON_PATH_PAGED_LIST_CONTENT = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTENT;
    private static final String JSON_PATH_FIELD_SIZE = ".size";
    static final String JSON_PATH_PAGED_LIST_SIZE = JSON_PATH_ROOT + JSON_PATH_FIELD_SIZE;
    private static final String JSON_PATH_FIELD_TOTAL = ".total";
    static final String JSON_PATH_PAGED_LIST_TOTAL = JSON_PATH_ROOT + JSON_PATH_FIELD_TOTAL;
    private static final String JSON_PATH_FIELD_AUTO_ASSIGN_DS = ".autoAssignDistributionSet";
    private static final String JSON_PATH_FIELD_AUTO_ASSIGN_ACTION_TYPE = ".autoAssignActionType";
    private static final String JSON_PATH_FIELD_EXCEPTION_CLASS = ".exceptionClass";
    private static final String JSON_PATH_FIELD_ERROR_CODE = ".errorCode";
    private static final String JSON_PATH_NAME = JSON_PATH_ROOT + JSON_PATH_FIELD_NAME;
    private static final String JSON_PATH_ID = JSON_PATH_ROOT + JSON_PATH_FIELD_ID;
    private static final String JSON_PATH_QUERY = JSON_PATH_ROOT + JSON_PATH_FIELD_QUERY;
    private static final String JSON_PATH_CONFIRMATION_REQUIRED = JSON_PATH_ROOT
            + JSON_PATH_FIELD_CONFIRMATION_REQUIRED;
    private static final String JSON_PATH_AUTO_ASSIGN_DS = JSON_PATH_ROOT + JSON_PATH_FIELD_AUTO_ASSIGN_DS;
    private static final String JSON_PATH_AUTO_ASSIGN_ACTION_TYPE = JSON_PATH_ROOT
            + JSON_PATH_FIELD_AUTO_ASSIGN_ACTION_TYPE;
    private static final String JSON_PATH_EXCEPTION_CLASS = JSON_PATH_ROOT + JSON_PATH_FIELD_EXCEPTION_CLASS;
    private static final String JSON_PATH_ERROR_CODE = JSON_PATH_ROOT + JSON_PATH_FIELD_ERROR_CODE;

    /**
     * Handles the GET request of retrieving all target filter queries within SP.
     */
    @Test
    void getTargetFilterQueries() throws Exception {
        final String filterName = "filter_01";
        createSingleTargetFilterQuery(filterName, "name==test_01");
        mvc.perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print());
    }

    /**
     * Handles the GET request of retrieving all target filter queries within SP based by parameter. Required Permission: READ_TARGET.
     */
    @Test
    void getTargetFilterQueriesWithParameters() throws Exception {
        mvc.perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "?limit=10&sort=name:ASC&offset=0&q=name==*1"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print());
    }

    /**
     * Handles the POST request of creating a new target filter query within SP.
     */
    @Test
    void createTargetFilterQuery() throws Exception {
        final String name = "test_02";
        final String filterQuery = "name==test_02";
        final String body = new JSONObject()
                .put("name", name)
                .put("query", filterQuery).toString();

        mvc.perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());
    }

    /**
     * Ensures that deletion is executed if permitted.
     */
    @Test
    void deleteTargetFilterQueryReturnsOK() throws Exception {
        final String filterName = "filter_01";
        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery(filterName, "name==test_01");

        mvc.perform(delete(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + filterQuery.getId()))
                .andExpect(status().isNoContent());

        assertThat(targetFilterQueryManagement.find(filterQuery.getId())).isNotPresent();
    }

    /**
     * Ensures that deletion is refused with not found if target does not exist.
     */
    @Test
    void deleteTargetWhichDoesNotExistsLeadsToEntityNotFound() throws Exception {
        final String notExistingId = "4395";

        mvc.perform(delete(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + notExistingId))
                .andExpect(status().isNotFound());
    }

    /**
     * Ensures that update is refused with not found if target does not exist.
     */
    @Test
    void updateTargetWhichDoesNotExistsLeadsToEntityNotFound() throws Exception {
        final String notExistingId = "4395";
        mvc.perform(put(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + notExistingId).content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    /**
     * Ensures that update request is reflected by repository.
     */
    @Test
    void updateTargetFilterQueryQuery() throws Exception {
        final String filterName = "filter_02";
        final String filterQuery = "name==test_02";
        final String filterQuery2 = "name==test_02_changed";
        final String body = new JSONObject().put("query", filterQuery2).toString();

        // prepare
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(filterName, filterQuery);

        mvc.perform(put(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ID, equalTo(tfq.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_QUERY, equalTo(filterQuery2)))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(filterName)))
                .andExpect(jsonPath(JSON_PATH_CONFIRMATION_REQUIRED).doesNotExist());

        final TargetFilterQuery tfqCheck = targetFilterQueryManagement.find(tfq.getId()).get();
        assertThat(tfqCheck.getQuery()).isEqualTo(filterQuery2);
        assertThat(tfqCheck.getName()).isEqualTo(filterName);
    }

    /**
     * Ensures that update request is reflected by repository.
     */
    @Test
    void updateTargetFilterQueryName() throws Exception {
        final String filterName = "filter_03";
        final String filterName2 = "filter_03_changed";
        final String filterQuery = "name==test_03";
        final String body = new JSONObject().put("name", filterName2).toString();

        // prepare
        final TargetFilterQuery tfq = targetFilterQueryManagement.create(Create.builder().name(filterName).query(filterQuery).build());

        mvc.perform(put(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ID, equalTo(tfq.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_QUERY, equalTo(filterQuery)))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(filterName2)))
                .andExpect(jsonPath(JSON_PATH_CONFIRMATION_REQUIRED).doesNotExist());

        final TargetFilterQuery tfqCheck = targetFilterQueryManagement.find(tfq.getId()).get();
        assertThat(tfqCheck.getQuery()).isEqualTo(filterQuery);
        assertThat(tfqCheck.getName()).isEqualTo(filterName2);
    }

    /**
     * Ensures that request returns list of filters in defined format.
     */
    @Test
    void getTargetFilterQueryWithoutAdditionalRequestParameters() throws Exception {
        final int knownTargetAmount = 3;
        final String idA = "a";
        final String idB = "b";
        final String idC = "c";
        final String testQuery = "name==test";

        createSingleTargetFilterQuery(idA, testQuery);
        createSingleTargetFilterQuery(idB, testQuery);
        createSingleTargetFilterQuery(idC, testQuery);

        mvc.perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(knownTargetAmount)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].name", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].query", contains(testQuery)))
                // idB
                .andExpect(jsonPath("$.content.[?(@.name=='" + idB + "')].name", contains(idB)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idB + "')].query", contains(testQuery)))
                // idC
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].name", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].query", contains(testQuery)));
    }

    /**
     * Ensures that request returns list of filters in defined format in size reduced by given limit parameter.
     */
    @Test
    void getTargetWithPagingLimitRequestParameter() throws Exception {
        final int limitSize = 1;
        final int knownTargetAmount = 3;
        final String idA = "a";
        final String idB = "b";
        final String idC = "c";
        final String testQuery = "name==test";

        createSingleTargetFilterQuery(idA, testQuery);
        createSingleTargetFilterQuery(idB, testQuery);
        createSingleTargetFilterQuery(idC, testQuery);

        mvc.perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].name", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].query", contains(testQuery)));
    }

    @Test
    public void checkIfFullRepresentationInTargetFilterReturnsDistributionSetHrefWithFilter() throws Exception {
        final String testQuery = "name==test";

        final DistributionSet set = testdataFactory.createDistributionSet();
        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery("a", testQuery);
        final String hrefPrefix = "http://localhost" + MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/"
                + filterQuery.getId();
        final String distributionsetHrefPrefix = "http://localhost" + MgmtDistributionSetRestApi.DISTRIBUTIONSETS_V1;

        final String dsQuery = "?q=name==" + set.getName() + ";" + "version==" + set.getVersion() + "&offset=0&limit=50";
        mvc.perform(
                        post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + filterQuery.getId() + "/autoAssignDS")
                                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        final String result = mvc.perform(
                        get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + filterQuery.getId()))
                .andExpect(jsonPath("$._links.autoAssignDS.href", equalTo(hrefPrefix + "/autoAssignDS")))
                .andExpect(jsonPath("$._links.DS.href", startsWith(distributionsetHrefPrefix)))
                .andReturn().getResponse().getContentAsString();

        final String multipleResult = mvc.perform(
                        get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "?representation=full"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.total", equalTo(1)))
                .andExpect(jsonPath("$.content[0]._links.DS.href", startsWith(distributionsetHrefPrefix)))
                .andReturn().getResponse().getContentAsString();

        final JSONObject singleJson = new JSONObject(result);
        final JSONObject multipleJson = new JSONObject(multipleResult);

        final String resultDSURI = singleJson.getJSONObject("_links").getJSONObject("DS").getString("href");
        final String resultDSURIFromMultipleJson = multipleJson.getJSONArray("content").getJSONObject(0)
                .getJSONObject("_links").getJSONObject("DS").getString("href");

        Assertions.assertEquals(distributionsetHrefPrefix + dsQuery, UriUtils.decode(resultDSURI, StandardCharsets.UTF_8));
        Assertions.assertEquals(distributionsetHrefPrefix + dsQuery,
                UriUtils.decode(resultDSURIFromMultipleJson, StandardCharsets.UTF_8));
    }

    /**
     * Ensures that request returns list of filters in defined format in size reduced by given limit and offset parameter.
     */
    @Test
    void getTargetWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int knownTargetAmount = 5;
        final int offsetParam = 2;
        final int expectedSize = knownTargetAmount - offsetParam;
        final String idC = "c";
        final String idD = "d";
        final String idE = "e";
        final String testQuery = "name==test";

        createSingleTargetFilterQuery("a", testQuery);
        createSingleTargetFilterQuery("b", testQuery);
        createSingleTargetFilterQuery(idC, testQuery);
        createSingleTargetFilterQuery(idD, testQuery);
        createSingleTargetFilterQuery(idE, testQuery);

        mvc.perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(knownTargetAmount)))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].name", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].query", contains(testQuery)))
                // idB
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].name", contains(idD)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].query", contains(testQuery)))
                // idC
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].name", contains(idE)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].query", contains(testQuery)));
    }

    /**
     * Ensures that a single target filter query can be retrieved via its id.
     */
    @Test
    void getSingleTarget() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownQuery = "name==test01";
        final String knownName = "someName";
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);
        final String hrefPrefix = "http://localhost" + MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/"
                + tfq.getId();
        // test
        mvc.perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownName)))
                .andExpect(jsonPath(JSON_PATH_QUERY, equalTo(knownQuery)))
                .andExpect(jsonPath(JSON_PATH_CONFIRMATION_REQUIRED).doesNotExist())
                .andExpect(jsonPath("$._links.self.href", equalTo(hrefPrefix)))
                .andExpect(jsonPath("$._links.autoAssignDS.href", equalTo(hrefPrefix + "/autoAssignDS")));
    }

    /**
     * Ensures that the retrieval of a non-existing target filter query results in a HTTP Not found error (404).
     */
    @Test
    void getSingleTargetNoExistsResponseNotFound() throws Exception {
        final String targetIdNotExists = "546546";
        // test

        final MvcResult mvcResult = mvc
                .perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + targetIdNotExists))
                .andExpect(status().isNotFound())
                .andReturn();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_ENTITY_NOT_EXISTS.getKey());
    }

    /**
     * Ensures that the creation of a target filter query based on an invalid request payload results in a HTTP Bad Request error (400).
     */
    @Test
    void createTargetFilterQueryWithBadPayloadBadRequest() throws Exception {
        final String notJson = "abc";

        final MvcResult mvcResult = mvc
                .perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1).content(notJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(targetFilterQueryManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(MessageNotReadableException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getKey());
    }

    /**
     * Ensures that the creation of a target filter query based on an invalid RSQL query results in a HTTP Bad Request error (400).
     */
    @Test
    void createTargetFilterWithInvalidQuery() throws Exception {
        final String invalidQuery = "name=abc";
        final String body = new JSONObject().put("query", invalidQuery).put("name", "invalidFilter").toString();

        mvc.perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(targetFilterQueryManagement.count()).isZero();
    }

    /**
     * Ensures that the assignment of an auto-assign distribution set results in a HTTP Forbidden error (403)
     * if the (existing) query addresses too many targets.
     */
    @Test
    public void setAutoAssignDistributionSetOnFilterQueryThatExceedsQuota() throws Exception {

        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target");

        // create the filter query and the distribution set
        final DistributionSet set = testdataFactory.createDistributionSet();
        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery("1", "controllerId==target*");

        mvc.perform(
                        post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + filterQuery.getId() + "/autoAssignDS")
                                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isTooManyRequests())
                .andExpect(
                        jsonPath(JSON_PATH_EXCEPTION_CLASS, equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath(JSON_PATH_ERROR_CODE, equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));
    }

    /**
     * Ensures that the update of a target filter query results in a HTTP Forbidden error (403)
     * if the updated query addresses too many targets.
     */
    @Test
    public void updateTargetFilterQueryWithQueryThatExceedsQuota() throws Exception {
        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target");

        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery("1", "controllerId==target1");

        // create the filter query and the distribution set
        final DistributionSet set = testdataFactory.createDistributionSet();

        // assign the auto-assign distribution set, this should work
        mvc.perform(
                        post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + filterQuery.getId() + "/autoAssignDS")
                                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
        implicitLock(set);

        final TargetFilterQuery updatedFilterQuery = targetFilterQueryManagement.find(filterQuery.getId()).get();

        assertThat(updatedFilterQuery.getAutoAssignDistributionSet()).isEqualTo(set);
        assertThat(updatedFilterQuery.getAutoAssignActionType()).isEqualTo(ActionType.FORCED);

        // update the query of the filter query to trigger a quota hit
        mvc.perform(put(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + filterQuery.getId())
                        .content("{\"query\":\"controllerId==target*\"}").contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isTooManyRequests())
                .andExpect(
                        jsonPath(JSON_PATH_EXCEPTION_CLASS, equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath(JSON_PATH_ERROR_CODE, equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

    }

    /**
     * Ensures that the distribution set auto-assignment works as intended with distribution set, action type and confirmation validation
     */
    @ParameterizedTest
    @MethodSource("confirmationOptions")
    public void setAutoAssignDistributionSetToTargetFilterQuery(final boolean confirmationFlowActive,
            final Boolean confirmationRequired) throws Exception {
        final String knownQuery = "name==test05";
        final String knownName = "filter05";

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);

        // set will be locked after first assignment
        final DistributionSet set = testdataFactory.createDistributionSet();

        verifyAutoAssignmentWithoutActionType(tfq, set, confirmationRequired);
        verifyAutoAssignmentWithForcedActionType(tfq, set, confirmationRequired);
        verifyAutoAssignmentWithSoftActionType(tfq, set, confirmationRequired);
        verifyAutoAssignmentWithTimeForcedActionType(tfq, set);
        verifyAutoAssignmentWithDownloadOnlyActionType(tfq, set, confirmationRequired);
        verifyAutoAssignmentWithUnknownActionType(tfq, set);
        verifyAutoAssignmentWithIncompleteDs(tfq);
        verifyAutoAssignmentWithSoftDeletedDs(tfq);
    }

    /**
     * Handles the GET request of retrieving a the auto assign distribution set of a target filter query within SP.
     */
    @Test
    void getAssignDS() throws Exception {
        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery("filter_01", "name==test_01");
        final DistributionSet ds = testdataFactory.createDistributionSet("ds");
        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterQuery.getId()).ds(ds.getId()));

        mvc.perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/{targetFilterQueryId}/autoAssignDS",
                        filterQuery.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Handles the POST request of setting a distribution set for auto assignment within SP.
     */
    @Test
    void createAutoAssignDS() throws Exception {
        enableConfirmationFlow();

        final String filterName = "filter_01";
        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery(filterName, "name==test_01");
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("ds");
        final String autoAssignBody = new JSONObject().put("id", distributionSet.getId())
                .put("type", MgmtActionType.SOFT.getName()).put("weight", 200).toString();

        mvc
                .perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/{targetFilterQueryId}/autoAssignDS",
                        filterQuery.getId()).contentType(MediaType.APPLICATION_JSON).content(autoAssignBody))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Handles the DELETE request of deleting the auto assign distribution set from a target filter query within SP.
     */
    @Test
    void deleteAutoAssignDS() throws Exception {
        final String filterName = "filter_01";
        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery(filterName, "name==test_01");
        mvc
                .perform(delete(
                        MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/{targetFilterQueryId}/autoAssignDS",
                        filterQuery.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
    }

    /**
     * Ensures that the deletion of auto-assignment distribution set works as intended, deleting the auto-assignment action type as well
     */
    @Test
    void deleteAutoAssignDistributionSetOfTargetFilterQuery() throws Exception {
        final String knownQuery = "name==test06";
        final String knownName = "filter06";
        final String dsName = "testDS";

        final DistributionSet set = testdataFactory.createDistributionSet(dsName);
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);
        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(tfq.getId()).ds(set.getId()));
        implicitLock(set);

        final TargetFilterQuery updatedFilterQuery = targetFilterQueryManagement.find(tfq.getId()).orElseThrow();

        assertThat(updatedFilterQuery.getAutoAssignDistributionSet()).isEqualTo(set);
        assertThat(updatedFilterQuery.getAutoAssignActionType()).isEqualTo(ActionType.FORCED);

        mvc.perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId() + "/autoAssignDS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(dsName)));

        mvc.perform(delete(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId() + "/autoAssignDS"))
                .andExpect(status().isNoContent());

        final TargetFilterQuery filterQueryWithDeletedDs = targetFilterQueryManagement.find(tfq.getId()).get();

        assertThat(filterQueryWithDeletedDs.getAutoAssignDistributionSet()).isNull();
        assertThat(filterQueryWithDeletedDs.getAutoAssignActionType()).isNull();

        mvc.perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId() + "/autoAssignDS"))
                .andExpect(status().isNoContent());

    }

    /**
     * An auto assignment containing a weight is only accepted when weight is valide and multi assignment is on.
     */
    @Test
    void weightValidation() throws Exception {
        final Long filterId = createSingleTargetFilterQuery("filter1", "name==*").getId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final String invalideWeightRequest = new JSONObject().put("id", dsId).put("weight", Action.WEIGHT_MIN - 1)
                .toString();
        final String valideWeightRequest = new JSONObject().put("id", dsId).put("weight", 45).toString();

        mvc.perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/{targetFilterQueryId}/autoAssignDS",
                        filterId).content(valideWeightRequest).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        mvc.perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/{targetFilterQueryId}/autoAssignDS",
                        filterId).content(invalideWeightRequest).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.repo.constraintViolation")));
        mvc.perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/{targetFilterQueryId}/autoAssignDS",
                        filterId).content(valideWeightRequest).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        final List<? extends TargetFilterQuery> filters = targetFilterQueryManagement.findAll(PAGE).getContent();
        assertThat(filters).hasSize(1);
        assertThat(filters.get(0).getAutoAssignWeight()).contains(45);
    }

    /**
     * Verify the confirmation required flag will be set based on the feature state
     */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void verifyConfirmationStateIfNotProvided(final boolean confirmationFlowActive) throws Exception {
        final String knownQuery = "name==test05";
        final String knownName = "filter05";

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);
        final DistributionSet set = testdataFactory.createDistributionSet();

        // do not provide something about the confirmation
        verifyAutoAssignmentByActionType(tfq, set, null, null);

        assertThat(targetFilterQueryManagement.find(tfq.getId())).hasValueSatisfying(filter ->
                assertThat(filter.isConfirmationRequired()).isEqualTo(confirmationFlowActive));
    }

    private static Stream<Arguments> confirmationOptions() {
        return Stream.of(Arguments.of(true, false), Arguments.of(true, true), Arguments.of(false, true),
                Arguments.of(true, null), Arguments.of(false, null));
    }

    private void verifyAutoAssignmentWithoutActionType(final TargetFilterQuery tfq, final DistributionSet set,
            final Boolean confirmationRequired) throws Exception {
        verifyAutoAssignmentByActionType(tfq, set, null, confirmationRequired);
    }

    private void verifyAutoAssignmentWithForcedActionType(final TargetFilterQuery tfq, final DistributionSet set,
            final Boolean confirmationRequired) throws Exception {
        verifyAutoAssignmentByActionType(tfq, set, MgmtActionType.FORCED, confirmationRequired);
    }

    private void verifyAutoAssignmentWithSoftActionType(final TargetFilterQuery tfq, final DistributionSet set,
            final Boolean confirmationRequired) throws Exception {
        verifyAutoAssignmentByActionType(tfq, set, MgmtActionType.SOFT, confirmationRequired);
    }

    private void verifyAutoAssignmentByActionType(final TargetFilterQuery tfq, final DistributionSet set,
            final MgmtActionType actionType, final Boolean confirmationRequired) throws Exception {
        final String hrefPrefix = "http://localhost" + MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/"
                + tfq.getId();

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", set.getId());
        if (actionType != null) {
            jsonObject.put("type", actionType.getName());
        }
        if (confirmationRequired != null) {
            jsonObject.put("confirmationRequired", confirmationRequired);
        }

        mvc.perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId() + "/autoAssignDS")
                        .content(jsonObject.toString()).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
        implicitLock(set);

        final TargetFilterQuery updatedFilterQuery = targetFilterQueryManagement.find(tfq.getId()).get();
        final MgmtActionType expectedActionType = actionType != null ? actionType : MgmtActionType.FORCED;

        assertThat(updatedFilterQuery.getAutoAssignDistributionSet()).isEqualTo(set);
        assertThat(updatedFilterQuery.getAutoAssignActionType())
                .isEqualTo(MgmtRestModelMapper.convertActionType(expectedActionType));

        mvc.perform(get(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(tfq.getName())))
                .andExpect(jsonPath(JSON_PATH_QUERY, equalTo(tfq.getQuery())))
                .andExpect(isConfirmationFlowEnabled()
                        ? jsonPath(JSON_PATH_CONFIRMATION_REQUIRED,
                        equalTo(confirmationRequired == null || confirmationRequired))
                        : jsonPath(JSON_PATH_CONFIRMATION_REQUIRED).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_AUTO_ASSIGN_DS, equalTo(set.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_AUTO_ASSIGN_ACTION_TYPE, equalTo(expectedActionType.getName())))
                .andExpect(jsonPath("$._links.self.href", equalTo(hrefPrefix)))
                .andExpect(jsonPath("$._links.autoAssignDS.href", equalTo(hrefPrefix + "/autoAssignDS")));
    }

    private void verifyAutoAssignmentWithTimeForcedActionType(final TargetFilterQuery tfq, final DistributionSet set)
            throws Exception {
        mvc.perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId() + "/autoAssignDS")
                        .content("{\"id\":" + set.getId() + ", \"type\":\"" + MgmtActionType.TIMEFORCED.getName() + "\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_EXCEPTION_CLASS,
                        equalTo(InvalidAutoAssignActionTypeException.class.getName())))
                .andExpect(jsonPath(JSON_PATH_ERROR_CODE,
                        equalTo(SpServerError.SP_AUTO_ASSIGN_ACTION_TYPE_INVALID.getKey())));
    }

    private void verifyAutoAssignmentWithDownloadOnlyActionType(final TargetFilterQuery tfq, final DistributionSet set,
            final Boolean confirmationRequired) throws Exception {
        verifyAutoAssignmentByActionType(tfq, set, MgmtActionType.DOWNLOAD_ONLY, confirmationRequired);
    }

    private void verifyAutoAssignmentWithUnknownActionType(final TargetFilterQuery tfq, final DistributionSet set)
            throws Exception {
        mvc.perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId() + "/autoAssignDS")
                        .content("{\"id\":" + set.getId() + ", \"type\":\"unknown\"}").contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_EXCEPTION_CLASS, equalTo(MessageNotReadableException.class.getName())))
                .andExpect(jsonPath(JSON_PATH_ERROR_CODE, equalTo(SpServerError.SP_REST_BODY_NOT_READABLE.getKey())));
    }

    private void verifyAutoAssignmentWithIncompleteDs(final TargetFilterQuery tfq) throws Exception {
        final DistributionSet incompleteDistributionSet = distributionSetManagement
                .create(DistributionSetManagement.Create.builder()
                        .type(testdataFactory.findOrCreateDefaultTestDsType())
                        .name("incomplete").version("1")
                        .build());

        mvc.perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId() + "/autoAssignDS")
                        .content("{\"id\":" + incompleteDistributionSet.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_EXCEPTION_CLASS,
                        equalTo(IncompleteDistributionSetException.class.getName())))
                .andExpect(jsonPath(JSON_PATH_ERROR_CODE, equalTo(SpServerError.SP_DS_INCOMPLETE.getKey())));
    }

    private void verifyAutoAssignmentWithSoftDeletedDs(final TargetFilterQuery tfq) throws Exception {
        final DistributionSet softDeletedDs = testdataFactory.createDistributionSet("softDeleted");
        assignDistributionSet(softDeletedDs, testdataFactory.createTarget("forSoftDeletedDs"));
        distributionSetManagement.delete(softDeletedDs.getId());

        mvc.perform(post(MgmtTargetFilterQueryRestApi.TARGETFILTERS_V1 + "/" + tfq.getId() + "/autoAssignDS")
                        .content("{\"id\":" + softDeletedDs.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_PATH_EXCEPTION_CLASS, equalTo(DeletedException.class.getName())))
                .andExpect(jsonPath(JSON_PATH_ERROR_CODE, equalTo(SpServerError.SP_DELETED.getKey())));
    }

    private TargetFilterQuery createSingleTargetFilterQuery(final String name, final String query) {
        return targetFilterQueryManagement.create(Create.builder().name(name).query(query).build());
    }
}
/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.MockMvcResultPrinter;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.rest.resource.model.ExceptionInfo;
import org.eclipse.hawkbit.util.IpUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 *
 * Spring MVC Tests against the TargetResource.
 *
 *
 *
 */
@Features("Component Tests - Management RESTful API")
@Stories("Target Resource")
// TODO: fully document tests -> @Description for long text and reasonable
// method name as short text
public class TargetResourceTest extends AbstractIntegrationTest {

    private static final String TARGET_DESCRIPTION_TEST = "created in test";

    // json paths
    private static final String JSON_PATH_ROOT = "$";

    // fields, attributes
    private static final String JSON_PATH_FIELD_ID = ".id";
    private static final String JSON_PATH_FIELD_CONTROLLERID = ".controllerId";
    private static final String JSON_PATH_FIELD_NAME = ".name";
    private static final String JSON_PATH_FIELD_DESCRIPTION = ".description";
    private static final String JSON_PATH_FIELD_CONTENT = ".content";
    private static final String JSON_PATH_FIELD_SIZE = ".size";
    private static final String JSON_PATH_FIELD_TOTAL = ".total";

    // target
    // $.field
    static final String JSON_PATH_PAGED_LIST_CONTENT = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTENT;
    static final String JSON_PATH_PAGED_LIST_SIZE = JSON_PATH_ROOT + JSON_PATH_FIELD_SIZE;
    static final String JSON_PATH_PAGED_LIST_TOTAL = JSON_PATH_ROOT + JSON_PATH_FIELD_TOTAL;

    private static final String JSON_PATH_NAME = JSON_PATH_ROOT + JSON_PATH_FIELD_NAME;
    private static final String JSON_PATH_ID = JSON_PATH_ROOT + JSON_PATH_FIELD_ID;
    private static final String JSON_PATH_CONTROLLERID = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTROLLERID;
    private static final String JSON_PATH_DESCRIPTION = JSON_PATH_ROOT + JSON_PATH_FIELD_DESCRIPTION;

    // TODO kzimmerm: test *modified after entity change

    @Test
    // MECS-1064
    public void getActionStatusReturnsCorrectType() throws Exception {
        final int limitSize = 2;
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        actions.get(0).setStatus(Status.FINISHED);
        controllerManagament.addUpdateActionStatus(
                new ActionStatus(actions.get(0), Status.FINISHED, System.currentTimeMillis(), "testmessage"),
                actions.get(0));

        final PageRequest pageRequest = new PageRequest(0, 1000, Direction.ASC, ActionStatusFields.ID.getFieldName());

        // limit to 1 - first page -> standard cancel message
        final Long reportAt = deploymentManagement
                .findActionsByTarget(pageRequest, targetManagement.findTargetByControllerID(knownTargetId)).getContent()
                .get(0).getCreatedAt();
        final Long id = deploymentManagement
                .findActionsByTarget(pageRequest, targetManagement.findTargetByControllerID(knownTargetId)).getContent()
                .get(0).getId();
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actions.get(0).getId() + "/status")
                        .param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize))
                        .param(RestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(3)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                .andExpect(jsonPath("content.[0].id", equalTo(id.intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("finished")))
                .andExpect(jsonPath("content.[0].messages", hasSize(1)))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(reportAt)))
                .andExpect(jsonPath("content.[1].type", equalTo("canceling")));
    }

    @Test
    @WithUser(allSpPermissions = false, authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET })
    public void securityTokenIsNotInResponseIfMissingPermission() throws Exception {

        final String knownControllerId = "knownControllerId";
        targetManagement.createTarget(new Target(knownControllerId));
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", knownControllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("securityToken").doesNotExist());
    }

    @Test
    @WithUser(allSpPermissions = false, authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET,
            SpPermission.READ_TARGET_SEC_TOKEN })
    public void securityTokenIsInResponseWithCorrectPermission() throws Exception {

        final String knownControllerId = "knownControllerId";
        final Target createTarget = targetManagement.createTarget(new Target(knownControllerId));
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", knownControllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("securityToken", equalTo(createTarget.getSecurityToken())));
    }

    @Test
    public void addressAndIpAddressInTargetResult() throws Exception {
        // prepare targets with IP
        final String knownControllerId1 = "0815";
        final String knownControllerId2 = "4711";

        createTarget(knownControllerId1);
        createTarget(knownControllerId2);

        // test
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(2)))
                .andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("$content.[?(@.controllerId==" + knownControllerId1 + ")][0].ipAddress",
                        equalTo("127.0.0.1")))
                .andExpect(jsonPath("$content.[?(@.controllerId==" + knownControllerId2 + ")][0].ipAddress",
                        equalTo("127.0.0.1")))
                .andExpect(jsonPath("$content.[?(@.controllerId==" + knownControllerId1 + ")][0].address",
                        equalTo(IpUtil.createHttpUri("127.0.0.1").toString())))
                .andExpect(jsonPath("$content.[?(@.controllerId==" + knownControllerId2 + ")][0].address",
                        equalTo(IpUtil.createHttpUri("127.0.0.1").toString())));
    }

    private void createTarget(final String controllerId) {
        final Target target = new Target(controllerId);
        final TargetInfo targetInfo = new TargetInfo(target);
        targetInfo.setAddress(IpUtil.createHttpUri("127.0.0.1").toString());
        target.setTargetInfo(targetInfo);
        targetManagement.createTarget(target);
    }

    @Test
    public void searchActionsRsql() throws Exception {

        // prepare test
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        final Target createTarget = targetManagement.createTarget(new Target("knownTargetId"));

        deploymentManagement.assignDistributionSet(dsA, Lists.newArrayList(createTarget));

        final String rsqlPendingStatus = "status==pending";
        final String rsqlFinishedStatus = "status==finished";
        final String rsqlPendingOrFinishedStatus = rsqlFinishedStatus + "," + rsqlPendingStatus;
        // pending status one result
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions?q=" + rsqlPendingStatus,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].status", equalTo("pending")));

        // finished status none result
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions?q=" + rsqlFinishedStatus,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0))).andExpect(jsonPath("size", equalTo(0)));

        // pending or finished status one result
        mvc.perform(
                get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions?q=" + rsqlPendingOrFinishedStatus,
                        createTarget.getControllerId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1))).andExpect(jsonPath("content[0].status", equalTo("pending")));
    }

    @Test
    public void cancelActionOK() throws Exception {
        // prepare test
        final Target tA = createTargetAndStartAction();

        // test - cancel the active action
        mvc.perform(delete(RestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}",
                tA.getControllerId(), tA.getActions().get(0).getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        final Action action = deploymentManagement.findAction(tA.getActions().get(0).getId());
        // still active because in "canceling" state and waiting for controller
        // feedback
        assertThat(action.isActive()).isTrue();

        final Target queryTarget = targetManagement.findTargetByControllerID(tA.getControllerId());
        // action has not been cancelled confirmed from controller, so DS
        // remains assigned until
        // confirmation
        assertThat(queryTarget.getAssignedDistributionSet()).isNotNull();
        assertThat(queryTarget.getTargetInfo().getInstalledDistributionSet()).isNull();
    }

    @Test
    public void cancelAnCancelActionIsNotAllowed() throws Exception {
        // prepare test
        final Target tA = createTargetAndStartAction();

        // cancel the active action
        deploymentManagement.cancelAction(tA.getActions().get(0), tA);

        // find the current active action
        final List<Action> cancelActions = deploymentManagement.findActionsByTarget(new PageRequest(0, 100), tA)
                .getContent().stream().filter(action -> action.isCancelingOrCanceled()).collect(Collectors.toList());
        assertThat(cancelActions).hasSize(1);

        // test - cancel an cancel action returns forbidden
        mvc.perform(delete(RestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}",
                tA.getControllerId(), cancelActions.get(0).getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Description("Force Quit an Action, which is already canceled. Expected Result is an HTTP response code 204.")
    public void forceQuitAnCanceledActionReturnsOk() throws Exception {

        final Target tA = createTargetAndStartAction();

        // cancel the active action
        deploymentManagement.cancelAction(tA.getActions().get(0), tA);

        // find the current active action
        final List<Action> cancelActions = deploymentManagement.findActionsByTarget(new PageRequest(0, 100), tA)
                .getContent().stream().filter(action -> action.isCancelingOrCanceled()).collect(Collectors.toList());
        assertThat(cancelActions).hasSize(1);
        assertThat(cancelActions.get(0).isCancelingOrCanceled()).isTrue();

        // test - force quit an canceled action should return 204
        mvc.perform(delete(RestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}?force=true",
                tA.getControllerId(), cancelActions.get(0).getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
    }

    @Test
    @Description("Force Quit an Action, which is not canceled. Expected Result is an HTTP response code 405.")
    public void forceQuitAnNotCanceledActionReturnsMethodNotAllowed() throws Exception {

        final Target tA = createTargetAndStartAction();

        // test - cancel an cancel action returns forbidden
        mvc.perform(delete(RestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}?force=true",
                tA.getControllerId(), tA.getActions().get(0).getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void deleteTargetReturnsOK() throws Exception {
        final String knownControllerId = "knownControllerIdDelete";
        targetManagement.createTarget(new Target(knownControllerId));

        mvc.perform(delete(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId))
                .andExpect(status().isOk());

        final Target findTargetByControllerID = targetManagement.findTargetByControllerID(knownControllerId);
        assertThat(findTargetByControllerID).isNull();
    }

    @Test
    public void deleteTargetWhichDoesNotExistsLeadsToEntityNotFound() throws Exception {
        final String knownControllerId = "knownControllerIdDelete";

        mvc.perform(delete(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateTargetWhichDoesNotExistsLeadsToEntityNotFound() throws Exception {
        final String knownControllerId = "knownControllerIdUpdate";
        mvc.perform(put(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId).content("{}")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateTargetDescription() throws Exception {
        final String knownControllerId = "123";
        final String knownNewDescription = "a new desc updated over rest";
        final String knownNameNotModiy = "nameNotModiy";
        final String body = new JSONObject().put("description", knownNewDescription).toString();

        // prepare
        final Target t = new Target(knownControllerId);
        t.setDescription("old description");
        t.setName(knownNameNotModiy);
        targetManagement.createTarget(t);

        mvc.perform(put(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.description", equalTo(knownNewDescription)))
                .andExpect(jsonPath("$.name", equalTo(knownNameNotModiy)));

        final Target findTargetByControllerID = targetManagement.findTargetByControllerID(knownControllerId);
        assertThat(findTargetByControllerID.getDescription()).isEqualTo(knownNewDescription);
        assertThat(findTargetByControllerID.getName()).isEqualTo(knownNameNotModiy);
    }

    @Test
    public void getTargetWithoutAddtionalRequestParameters() throws Exception {
        final int knownTargetAmount = 3;
        final String idA = "a";
        final String idB = "b";
        final String idC = "c";
        final String linksHrefPrefix = "http://localhost/rest/v1/targets/";
        createTargetsAlphabetical(knownTargetAmount);
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(knownTargetAmount)))
                // idA
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0]._links.self.href",
                        equalTo(linksHrefPrefix + idA)))
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0].name", equalTo(idA)))
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0].description", equalTo(idA)))
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0].controllerId", equalTo(idA)))
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0].updateStatus", equalTo("unknown")))
                // idB
                .andExpect(jsonPath("$content.[?(@.name==" + idB + ")][0]._links.self.href",
                        equalTo(linksHrefPrefix + idB)))
                .andExpect(jsonPath("$content.[?(@.name==" + idB + ")][0].name", equalTo(idB)))
                .andExpect(jsonPath("$content.[?(@.name==" + idB + ")][0].description", equalTo(idB)))
                .andExpect(jsonPath("$content.[?(@.name==" + idB + ")][0].controllerId", equalTo(idB)))
                .andExpect(jsonPath("$content.[?(@.name==" + idB + ")][0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("$content.[?(@.name==" + idB + ")][0].updateStatus", equalTo("unknown")))
                // idC
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0]._links.self.href",
                        equalTo(linksHrefPrefix + idC)))
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0].name", equalTo(idC)))
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0].description", equalTo(idC)))
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0].controllerId", equalTo(idC)))
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0].updateStatus", equalTo("unknown")));
    }

    @Test
    public void getTargetWithPagingLimitRequestParameter() throws Exception {
        final int knownTargetAmount = 3;
        final int limitSize = 1;
        createTargetsAlphabetical(knownTargetAmount);
        final String idA = "a";
        final String linksHrefPrefix = "http://localhost/rest/v1/targets/";

        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING).param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT,
                String.valueOf(limitSize))).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                // idA
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0]._links.self.href",
                        equalTo(linksHrefPrefix + idA)))
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0].name", equalTo(idA)))
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0].description", equalTo(idA)))
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0].controllerId", equalTo(idA)))
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("$content.[?(@.name==" + idA + ")][0].updateStatus", equalTo("unknown")));

    }

    @Test
    public void getTargetWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int knownTargetAmount = 5;
        final int offsetParam = 2;
        final int expectedSize = knownTargetAmount - offsetParam;
        final String idC = "c";
        final String idD = "d";
        final String idE = "e";
        final String linksHrefPrefix = "http://localhost/rest/v1/targets/";

        createTargetsAlphabetical(knownTargetAmount);
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING)
                .param(RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                .param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(knownTargetAmount)))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)))
                // idA
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0]._links.self.href",
                        equalTo(linksHrefPrefix + idC)))
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0].name", equalTo(idC)))
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0].description", equalTo(idC)))
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0].controllerId", equalTo(idC)))
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("$content.[?(@.name==" + idC + ")][0].updateStatus", equalTo("unknown")))
                // idB
                .andExpect(jsonPath("$content.[?(@.name==" + idD + ")][0]._links.self.href",
                        equalTo(linksHrefPrefix + idD)))
                .andExpect(jsonPath("$content.[?(@.name==" + idD + ")][0].name", equalTo(idD)))
                .andExpect(jsonPath("$content.[?(@.name==" + idD + ")][0].description", equalTo(idD)))
                .andExpect(jsonPath("$content.[?(@.name==" + idD + ")][0].controllerId", equalTo(idD)))
                .andExpect(jsonPath("$content.[?(@.name==" + idD + ")][0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("$content.[?(@.name==" + idD + ")][0].updateStatus", equalTo("unknown")))
                // idC
                .andExpect(jsonPath("$content.[?(@.name==" + idE + ")][0]._links.self.href",
                        equalTo(linksHrefPrefix + idE)))
                .andExpect(jsonPath("$content.[?(@.name==" + idE + ")][0].name", equalTo(idE)))
                .andExpect(jsonPath("$content.[?(@.name==" + idE + ")][0].description", equalTo(idE)))
                .andExpect(jsonPath("$content.[?(@.name==" + idE + ")][0].controllerId", equalTo(idE)))
                .andExpect(jsonPath("$content.[?(@.name==" + idE + ")][0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("$content.[?(@.name==" + idE + ")][0].updateStatus", equalTo("unknown")));
    }

    @Test
    public void getSingleTarget() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);
        final String hrefPrefix = "http://localhost/rest/v1/targets/" + knownControllerId + "/";
        // test
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownName)))
                .andExpect(jsonPath(JSON_PATH_CONTROLLERID, equalTo(knownControllerId)))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(TARGET_DESCRIPTION_TEST)))
                .andExpect(jsonPath("$.pollStatus", hasKey("lastRequestAt")))
                .andExpect(jsonPath("$.pollStatus", hasKey("nextExpectedRequestAt")))
                .andExpect(jsonPath("$.pollStatus.overdue", equalTo(false)))
                .andExpect(jsonPath("$_links.assignedDS.href", equalTo(hrefPrefix + "assignedDS")))
                .andExpect(jsonPath("$_links.installedDS.href", equalTo(hrefPrefix + "installedDS")))
                .andExpect(jsonPath("$_links.actions.href",
                        equalTo(hrefPrefix + "actions" + "?offset=0&limit=50&sort=id:DESC")));
    }

    @Test
    public void getSingleTargetNoExistsResponseNotFound() throws Exception {
        final String targetIdNotExists = "bubu";
        // test

        final MvcResult mvcResult = mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetIdNotExists))
                .andExpect(status().isNotFound()).andReturn();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_ENTITY_NOT_EXISTS.getKey());
    }

    @Test
    public void getAssignedDistributionSetOfTargetIsEmpty() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);

        // test

        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId + "/assignedDS"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

    }

    @Test
    public void getAssignedDistributionSetOfTarget() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        deploymentManagement.assignDistributionSet(ds.getId(), knownControllerId);

        // test

        final SoftwareModule os = ds.findFirstModuleByType(osType);
        final SoftwareModule jvm = ds.findFirstModuleByType(runtimeType);
        final SoftwareModule bApp = ds.findFirstModuleByType(appType);
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId + "/assignedDS"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_ID, equalTo(ds.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(ds.getName())))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(ds.getDescription())))
                // os
                .andExpect(
                        jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].id", equalTo(os.getId().intValue())))
                .andExpect(jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].name", equalTo(os.getName())))
                .andExpect(jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].description",
                        equalTo(os.getDescription())))
                .andExpect(
                        jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].version", equalTo(os.getVersion())))
                .andExpect(jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].vendor", equalTo(os.getVendor())))
                .andExpect(jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].type", equalTo("os")))
                // jvm
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].id",
                        equalTo(jvm.getId().intValue())))
                .andExpect(
                        jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].name", equalTo(jvm.getName())))
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].description",
                        equalTo(jvm.getDescription())))
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].version",
                        equalTo(jvm.getVersion())))
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].vendor",
                        equalTo(jvm.getVendor())))
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].type", equalTo("runtime")))
                // baseApp
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].id",
                        equalTo(bApp.getId().intValue())))
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].name", equalTo(bApp.getName())))
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].description",
                        equalTo(bApp.getDescription())))
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].version",
                        equalTo(bApp.getVersion())))
                .andExpect(
                        jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].vendor", equalTo(bApp.getVendor())))
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].type", equalTo("application")));

    }

    @Test
    public void getInstalledDistributionSetOfTargetIsEmpty() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);

        // test

        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId + "/installedDS"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

    }

    @Test
    @Ignore
    public void getInstalledDistributionSetOfTarget() throws JSONException, Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);
        final DistributionSet ds = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        // assign ds to target
        deploymentManagement.assignDistributionSet(ds.getId(), knownControllerId);
        // give feedback, so installedDS is in SNYC
        feedbackToByInSync(knownControllerId, ds);
        // test

        final SoftwareModule os = ds.findFirstModuleByType(osType);
        final SoftwareModule jvm = ds.findFirstModuleByType(runtimeType);
        final SoftwareModule bApp = ds.findFirstModuleByType(appType);
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId + "/installedDS"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ID, equalTo(ds.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(ds.getName())))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(ds.getDescription())))
                // os
                .andExpect(
                        jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].id", equalTo(os.getId().intValue())))
                .andExpect(jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].name", equalTo(os.getName())))
                .andExpect(jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].description",
                        equalTo(os.getDescription())))
                .andExpect(
                        jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].version", equalTo(os.getVersion())))
                .andExpect(jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].vendor", equalTo(os.getVendor())))
                .andExpect(jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].type", equalTo("os")))
                // jvm
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].id",
                        equalTo(jvm.getId().intValue())))
                .andExpect(
                        jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].name", equalTo(jvm.getName())))
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].description",
                        equalTo(jvm.getDescription())))
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].version",
                        equalTo(jvm.getVersion())))
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].vendor",
                        equalTo(jvm.getVendor())))
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].type", equalTo("runtime")))
                // baseApp
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].id",
                        equalTo(bApp.getId().intValue())))
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].name", equalTo(bApp.getName())))
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].description",
                        equalTo(bApp.getDescription())))
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].version",
                        equalTo(bApp.getVersion())))
                .andExpect(
                        jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].vendor", equalTo(bApp.getVendor())))
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].type", equalTo("application")));
    }

    @Test
    public void createTargetWithoutPayloadBadRequest() throws Exception {

        final MvcResult mvcResult = mvc
                .perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetManagement.countTargetsAll()).isEqualTo(0);

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getKey());
        assertThat(exceptionInfo.getMessage()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getMessage());
    }

    @Test
    public void createTargetWithBadPayloadBadRequest() throws Exception {
        final String notJson = "abc";

        final MvcResult mvcResult = mvc
                .perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING).content(notJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetManagement.countTargetsAll()).isEqualTo(0);

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(MessageNotReadableException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getKey());
    }

    @Test
    public void createTargetsListReturnsSuccessful() throws Exception {
        final Target test1 = new Target("id1");
        test1.setDescription("testid1");
        test1.setName("testname1");
        final Target test2 = new Target("id2");
        test2.setDescription("testid2");
        test2.setName("testname2");
        final Target test3 = new Target("id3");
        test3.setName("testname3");
        test3.setDescription("testid3");

        final List<Target> targets = new ArrayList<>();
        targets.add(test1);
        targets.add(test2);
        targets.add(test3);

        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/targets/").content(JsonBuilder.targets(targets))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0].name", equalTo("testname1")))
                .andExpect(jsonPath("[0].controllerId", equalTo("id1")))
                .andExpect(jsonPath("[0].description", equalTo("testid1")))
                .andExpect(jsonPath("[0].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[1].name", equalTo("testname2")))
                .andExpect(jsonPath("[1].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[1].controllerId", equalTo("id2")))
                .andExpect(jsonPath("[1].description", equalTo("testid2")))
                .andExpect(jsonPath("[1].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[1].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[2].name", equalTo("testname3")))
                .andExpect(jsonPath("[2].controllerId", equalTo("id3")))
                .andExpect(jsonPath("[2].description", equalTo("testid3")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[2].createdBy", equalTo("bumlux"))).andReturn();

        assertThat(
                JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/targets/id1");
        assertThat(
                JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/targets/id2");
        assertThat(
                JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/targets/id3");

        assertThat(targetManagement.findTargetByControllerID("id1")).isNotNull();
        assertThat(targetManagement.findTargetByControllerID("id1").getName()).isEqualTo("testname1");
        assertThat(targetManagement.findTargetByControllerID("id1").getDescription()).isEqualTo("testid1");
        assertThat(targetManagement.findTargetByControllerID("id2")).isNotNull();
        assertThat(targetManagement.findTargetByControllerID("id2").getName()).isEqualTo("testname2");
        assertThat(targetManagement.findTargetByControllerID("id2").getDescription()).isEqualTo("testid2");
        assertThat(targetManagement.findTargetByControllerID("id3")).isNotNull();
        assertThat(targetManagement.findTargetByControllerID("id3").getName()).isEqualTo("testname3");
        assertThat(targetManagement.findTargetByControllerID("id3").getDescription()).isEqualTo("testid3");
    }

    @Test
    public void createTargetsSingleEntryListReturnsSuccessful() throws Exception {
        final String knownName = "someName";
        final String knownControllerId = "controllerId1";
        final String knownDescription = "someDescription";
        final String createTargetsJson = getCreateTargetsListJsonString(knownControllerId, knownName, knownDescription);

        mvc.perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING).content(createTargetsJson)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is2xxSuccessful());

        final Slice<Target> findTargetsAll = targetManagement.findTargetsAll(new PageRequest(0, 100));
        final Target target = findTargetsAll.getContent().get(0);
        assertThat(targetManagement.countTargetsAll()).isEqualTo(1);
        assertThat(target.getControllerId()).isEqualTo(knownControllerId);
        assertThat(target.getName()).isEqualTo(knownName);
        assertThat(target.getDescription()).isEqualTo(knownDescription);
    }

    @Test
    public void createTargetsSingleEntryListDoubleReturnConflict() throws Exception {
        final String knownName = "someName";
        final String knownControllerId = "controllerId1";
        final String knownDescription = "someDescription";
        final String createTargetsJson = getCreateTargetsListJsonString(knownControllerId, knownName, knownDescription);

        // create a taret first to provoke a already exists error

        mvc.perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING).content(createTargetsJson)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is2xxSuccessful());
        // create another one to retrieve the entity already exists exception
        final MvcResult mvcResult = mvc
                .perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING).content(createTargetsJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.CONFLICT.value())).andReturn();

        // verify only one entry
        assertThat(targetManagement.countTargetsAll()).isEqualTo(1);

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(EntityAlreadyExistsException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_ENTITY_ALRREADY_EXISTS.getKey());
        assertThat(exceptionInfo.getMessage()).isEqualTo(SpServerError.SP_REPO_ENTITY_ALRREADY_EXISTS.getMessage());
    }

    @Test
    public void createTargetsSingleEntryListWithAdditionalNotExistingTargetAttributeWillBeIgnored() throws Exception {
        final String knownName = "someName";
        final String knownControllerId = "controllerId1";
        final String knownDescription = "someDescription";
        final String createTargetsJson = getCreateTargetsListJsonStringWithAdditionalNotExistingAttribute(
                knownControllerId, knownName, knownDescription);

        mvc.perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING).content(createTargetsJson)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is2xxSuccessful());

        final Slice<Target> findTargetsAll = targetManagement.findTargetsAll(new PageRequest(0, 100));
        final Target target = findTargetsAll.getContent().get(0);
        assertThat(targetManagement.countTargetsAll()).isEqualTo(1);
        assertThat(target.getControllerId()).isEqualTo(knownControllerId);
        assertThat(target.getName()).isEqualTo(knownName);
        assertThat(target.getDescription()).isEqualTo(knownDescription);
    }

    @Test
    public void getActionWithEmptyResult() throws Exception {
        final String knownTargetId = "targetId";
        final Target target = new Target(knownTargetId);
        targetManagement.createTarget(target);

        mvc.perform(get(
                RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/" + RestConstants.TARGET_V1_ACTIONS))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(0)))
                .andExpect(jsonPath("content", hasSize(0))).andExpect(jsonPath("total", equalTo(0)));
    }

    @Test
    public void getCancelAction() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actions.get(0).getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("type", equalTo("cancel"))).andExpect(jsonPath("status", equalTo("pending")))
                .andExpect(jsonPath("_links.self.href", equalTo(generateActionSelfLink(knownTargetId, actions.get(0)))))
                .andExpect(jsonPath("_links.canceledaction.href",
                        equalTo(generateCanceledactionreferenceLink(knownTargetId, actions.get(0)))))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(knownTargetId, actions.get(0)))));
    }

    @Test
    public void getMultipleActions() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        mvc.perform(get(
                RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/" + RestConstants.TARGET_V1_ACTIONS))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[1].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("update")))
                .andExpect(jsonPath("content.[1].status", equalTo("pending")))
                .andExpect(jsonPath("content.[1]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(1)))))
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(0)))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    @Test
    @Description("Verfies that the API returns the status list with expected content.")
    public void getMultipleActionStatus() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        mvc.perform(get(
                RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/" + RestConstants.TARGET_V1_ACTIONS
                        + "/" + actions.get(0).getId() + "/" + RestConstants.TARGET_V1_ACTION_STATUS))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(
                        jsonPath("content.[0].id", equalTo(actions.get(0).getActionStatus().get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("canceling")))
                .andExpect(jsonPath("content.[0].messages", hasItem("manual cancelation requested")))
                .andExpect(jsonPath("content.[0].reportedAt",
                        equalTo(actions.get(0).getActionStatus().get(1).getCreatedAt())))
                .andExpect(
                        jsonPath("content.[1].id", equalTo(actions.get(0).getActionStatus().get(0).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("running")))
                .andExpect(jsonPath("content.[1].reportedAt",
                        equalTo(actions.get(0).getActionStatus().get(0).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    @Test
    @Description("Verfies that the API returns the status list with expected content split into two pages.")
    public void getMultipleActionStatusWithPagingLimitRequestParameter() throws Exception {
        final String knownTargetId = "targetId";

        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        // Page 1
        mvc.perform(get(
                RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/" + RestConstants.TARGET_V1_ACTIONS
                        + "/" + actions.get(0).getId() + "/" + RestConstants.TARGET_V1_ACTION_STATUS)
                                .param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(
                        jsonPath("content.[0].id", equalTo(actions.get(0).getActionStatus().get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("canceling")))
                .andExpect(jsonPath("content.[0].messages", hasItem("manual cancelation requested")))
                .andExpect(jsonPath("content.[0].reportedAt",
                        equalTo(actions.get(0).getActionStatus().get(1).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // Page 2
        mvc.perform(get(
                RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/" + RestConstants.TARGET_V1_ACTIONS
                        + "/" + actions.get(0).getId() + "/" + RestConstants.TARGET_V1_ACTION_STATUS)
                                .param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                                .param(RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(
                        jsonPath("content.[0].id", equalTo(actions.get(0).getActionStatus().get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("running")))
                .andExpect(jsonPath("content.[0].reportedAt",
                        equalTo(actions.get(0).getActionStatus().get(0).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    @Test
    public void getMultipleActionsWithPagingLimitRequestParameter() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        // page 1: one entry
        mvc.perform(get(
                RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/" + RestConstants.TARGET_V1_ACTIONS)
                        .param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(0)))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // page 2: one entry
        mvc.perform(get(
                RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/" + RestConstants.TARGET_V1_ACTIONS)
                        .param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                        .param(RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("update")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(1)))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    private String generateActionSelfLink(final String knownTargetId, final Action action) {
        return "http://localhost" + RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + action.getId();
    }

    private String generateCanceledactionreferenceLink(final String knownTargetId, final Action action) {
        return "http://localhost" + RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + action.getId();
    }

    private String generateStatusreferenceLink(final String knownTargetId, final Action action) {
        return "http://localhost" + RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + action.getId() + "/" + RestConstants.TARGET_V1_ACTION_STATUS
                + "?offset=0&limit=50&sort=id:DESC";
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverride(final String knownTargetId) {

        final PageRequest pageRequest = new PageRequest(0, 100, Direction.ASC, ActionStatusFields.ID.getFieldName());

        Target target = new Target(knownTargetId);
        target = targetManagement.createTarget(target);
        final List<Target> targets = new ArrayList<>();
        targets.add(target);

        final Iterator<DistributionSet> sets = TestDataUtil
                .generateDistributionSets(2, softwareManagement, distributionSetManagement).iterator();
        final DistributionSet one = sets.next();
        final DistributionSet two = sets.next();

        // Update
        final List<Target> updatedTargets = deploymentManagement.assignDistributionSet(one, targets)
                .getAssignedTargets();
        // 2nd update
        deploymentManagement.assignDistributionSet(two, updatedTargets);

        // two updates, one cancelation
        final List<Action> actions = actionRepository.findAll(pageRequest).getContent();

        assertThat(actions).hasSize(2);
        return actions;
    }

    @Test
    public void getUpdateAction() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actions.get(1).getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("type", equalTo("update"))).andExpect(jsonPath("status", equalTo("pending")))
                .andExpect(jsonPath("_links.self.href", equalTo(generateActionSelfLink(knownTargetId, actions.get(1)))))
                .andExpect(jsonPath("_links.distributionset.href",
                        equalTo("http://localhost/rest/v1/distributionsets/"
                                + actions.get(1).getDistributionSet().getId())))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(knownTargetId, actions.get(1)))));
    }

    @Test
    public void getActionStatusWithMultipleResultsWithPagingLimitRequestParameter() throws Exception {
        final int limitSize = 1;
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        actions.get(0).setStatus(Status.RUNNING);
        controllerManagament.addUpdateActionStatus(
                new ActionStatus(actions.get(0), Status.RUNNING, System.currentTimeMillis(), "testmessage"),
                actions.get(0));

        final PageRequest pageRequest = new PageRequest(0, 1000, Direction.ASC, ActionStatusFields.ID.getFieldName());

        // limit to 1 - first page -> standard cancel message
        Long reportAt = deploymentManagement
                .findActionsByTarget(pageRequest, targetManagement.findTargetByControllerID(knownTargetId)).getContent()
                .get(0).getCreatedAt();
        Long id = deploymentManagement
                .findActionsByTarget(pageRequest, targetManagement.findTargetByControllerID(knownTargetId)).getContent()
                .get(0).getId();
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actions.get(0).getId() + "/status")
                        .param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(3)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                .andExpect(jsonPath("content.[0].id", equalTo(id.intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("running")))
                .andExpect(jsonPath("content.[0].messages", hasSize(1)))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(reportAt)));

        // limit to 1 - first page -> added custom message
        reportAt = deploymentManagement
                .findActionsByTarget(pageRequest, targetManagement.findTargetByControllerID(knownTargetId)).getContent()
                .get(1).getCreatedAt();
        id = deploymentManagement
                .findActionsByTarget(pageRequest, targetManagement.findTargetByControllerID(knownTargetId)).getContent()
                .get(1).getCreatedAt();

        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actions.get(0).getId() + "/status")
                        .param(RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize))
                        .param(RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1)))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(3)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)));
    }

    @Test
    public void assignDistributionSetToTarget() throws Exception {

        final Target target = targetManagement.createTarget(new Target("fsdfsd"));
        final DistributionSet set = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);

        mvc.perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS")
                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(targetManagement.findTargetByControllerID("fsdfsd").getAssignedDistributionSet()).isEqualTo(set);
    }

    @Test
    public void assignDistributionSetToTargetWithActionTimeForcedAndTime() throws Exception {

        final Target target = targetManagement.createTarget(new Target("fsdfsd"));
        final DistributionSet set = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);

        final long forceTime = System.currentTimeMillis();
        final String body = new JSONObject().put("id", set.getId()).put("type", "timeforced")
                .put("forcetime", forceTime).toString();

        mvc.perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS").content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        final List<Action> findActiveActionsByTarget = deploymentManagement.findActiveActionsByTarget(target);
        assertThat(findActiveActionsByTarget).hasSize(1);
        assertThat(findActiveActionsByTarget.get(0).getActionType()).isEqualTo(ActionType.TIMEFORCED);
        assertThat(findActiveActionsByTarget.get(0).getForcedTime()).isEqualTo(forceTime);
        assertThat(targetManagement.findTargetByControllerID("fsdfsd").getAssignedDistributionSet()).isEqualTo(set);
    }

    @Test
    public void invalidRequestsOnAssignDistributionSetToTarget() throws Exception {

        final DistributionSet set = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);

        mvc.perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS")
                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        targetManagement.createTarget(new Target("fsdfsd"));

        mvc.perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS")
                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS").content("{\"id\":12345678}")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete(RestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(RestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS")
                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

    }

    @Test
    public void invalidRequestsOnActionResource() throws Exception {
        final String knownTargetId = "targetId";

        // target does not exist
        mvc.perform(get(
                RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/" + RestConstants.TARGET_V1_ACTIONS))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        final Long actionId = actions.get(0).getId();

        // should work now
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // action does not exist
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/12321")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(put(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void invalidRequestsOnActionStatusResource() throws Exception {
        final String knownTargetId = "targetId";

        // target does not exist
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/1/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        final Long actionId = actions.get(0).getId();

        // should work now
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actionId + "/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // action does not exist
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/12321/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(delete(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actionId + "/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actionId + "/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + RestConstants.TARGET_V1_ACTIONS + "/" + actionId + "/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void getControllerAttributesViaTargetResourceReturnsAttributesWithOk() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdWithAttributes";
        final Map<String, String> knownControllerAttrs = new HashMap<>();
        knownControllerAttrs.put("a", "1");
        knownControllerAttrs.put("b", "2");
        final Target target = new Target(knownTargetId);
        targetManagement.createTarget(target);
        controllerManagament.updateControllerAttributes(knownTargetId, knownControllerAttrs);

        // test query target over rest resource
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/attributes"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.a", equalTo("1"))).andExpect(jsonPath("$.b", equalTo("2")));
    }

    @Test
    public void getControllerEmptyAttributesReturnsNoContent() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdWithAttributes";
        final Target target = new Target(knownTargetId);
        targetManagement.createTarget(target);

        // test query target over rest resource
        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/attributes"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    public void searchTargetsUsingRsqlQuery() throws Exception {
        final int amountTargets = 10;
        createTargetsAlphabetical(amountTargets);

        final String rsqlFindAOrB = "controllerId==a,controllerId==b";

        mvc.perform(get(RestConstants.TARGET_V1_REQUEST_MAPPING + "?q=" + rsqlFindAOrB))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2))).andExpect(jsonPath("content[0].controllerId", equalTo("a")))
                .andExpect(jsonPath("content[1].controllerId", equalTo("b")));
    }

    private String getCreateTargetsListJsonString(final String controllerId, final String name,
            final String description) {
        return "[{\"name\":\"" + name + "\",\"controllerId\":\"" + controllerId + "\",\"description\":\"" + description
                + "\"}]";
    }

    private String getCreateTargetsListJsonStringWithAdditionalNotExistingAttribute(final String controllerId,
            final String name, final String description) {
        return "[{\"name\":\"" + name + "\",\"controllerId\":\"" + controllerId + "\",\"description\":\"" + description
                + "\"}]";
    }

    private void createSingleTarget(final String controllerId, final String name) {
        final Target target = new Target(controllerId);
        target.setName(name);
        target.setDescription(TARGET_DESCRIPTION_TEST);
        targetManagement.createTarget(target);
        controllerManagament.updateLastTargetQuery(controllerId, null);
    }

    /**
     * creating targets with the given amount by setting name, id etc from the
     * alphabet [a-z] using the ASCII.
     * 
     * @param amount
     */
    private void createTargetsAlphabetical(final int amount) {
        char character = 'a';
        for (int index = 0; index < amount; index++) {
            final String str = String.valueOf(character);
            final Target target = new Target(str);
            target.setName(str);
            target.setDescription(str);
            final Target savedTarget = targetManagement.createTarget(target);
            assertThat(savedTarget.getLastModifiedBy()).isNotNull();
            character++;
        }
    }

    /**
     * helper method to give feedback mark an target IN_SNCY
     * 
     * @param controllerId
     *            the controller id to give feedback to
     * @param savedSet
     *            the distribution set
     * @throws Exception
     * @throws JSONException
     */
    private void feedbackToByInSync(final String controllerId, final DistributionSet savedSet)
            throws Exception, JSONException {
        final Pageable pageReq = new PageRequest(0, 100);
        final Action action = actionRepository.findByDistributionSet(pageReq, savedSet).getContent().get(0);

        final ActionStatus actionStatus = new ActionStatus(action, Status.FINISHED, 0l);
        controllerManagement.addUpdateActionStatus(actionStatus, action);
    }

    /**
     * helper method to create a target and start an action on it.
     * 
     * @return The targetid of the created target.
     */
    private Target createTargetAndStartAction() {
        // prepare test
        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        final Target tA = targetManagement
                .createTarget(TestDataUtil.buildTargetFixture("target-id-A", "first description"));
        // assign a distribution set so we get an active update action
        deploymentManagement.assignDistributionSet(dsA, Lists.newArrayList(tA));
        // verify active action
        final Slice<Action> actionsByTarget = deploymentManagement.findActionsByTarget(new PageRequest(0, 100), tA);
        assertThat(actionsByTarget.getContent()).hasSize(1);
        return targetManagement.findTargetByControllerID(tA.getControllerId());
    }
}

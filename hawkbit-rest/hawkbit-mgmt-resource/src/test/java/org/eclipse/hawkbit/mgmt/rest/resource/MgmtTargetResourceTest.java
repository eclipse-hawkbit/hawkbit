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
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAutoConfirmUpdate;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.exception.MessageNotReadableException;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.util.IpUtil;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Spring MVC Tests against the MgmtTargetResource.
 *
 */
@Feature("Component Tests - Management API")
@Story("Target Resource")
class MgmtTargetResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String TARGET_DESCRIPTION_TEST = "created in test";

    private static final String JSON_PATH_ROOT = "$";

    // fields, attributes
    private static final String JSON_PATH_FIELD_ID = ".id";
    private static final String JSON_PATH_FIELD_CONTROLLERID = ".controllerId";
    private static final String JSON_PATH_FIELD_NAME = ".name";
    private static final String JSON_PATH_FIELD_DESCRIPTION = ".description";
    private static final String JSON_PATH_FIELD_CONTENT = ".content";
    private static final String JSON_PATH_FIELD_SIZE = ".size";
    private static final String JSON_PATH_FIELD_TOTAL = ".total";
    private static final String JSON_PATH_FIELD_LAST_REQUEST_AT = ".lastControllerRequestAt";
    private static final String JSON_PATH_FIELD_TARGET_TYPE = ".targetType";

    // target
    // $.field
    static final String JSON_PATH_PAGED_LIST_CONTENT = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTENT;
    static final String JSON_PATH_PAGED_LIST_SIZE = JSON_PATH_ROOT + JSON_PATH_FIELD_SIZE;
    static final String JSON_PATH_PAGED_LIST_TOTAL = JSON_PATH_ROOT + JSON_PATH_FIELD_TOTAL;

    private static final String JSON_PATH_NAME = JSON_PATH_ROOT + JSON_PATH_FIELD_NAME;
    private static final String JSON_PATH_ID = JSON_PATH_ROOT + JSON_PATH_FIELD_ID;
    private static final String JSON_PATH_CONTROLLERID = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTROLLERID;
    private static final String JSON_PATH_DESCRIPTION = JSON_PATH_ROOT + JSON_PATH_FIELD_DESCRIPTION;
    private static final String JSON_PATH_LAST_REQUEST_AT = JSON_PATH_ROOT + JSON_PATH_FIELD_LAST_REQUEST_AT;
    private static final String JSON_PATH_TYPE = JSON_PATH_ROOT + JSON_PATH_FIELD_TARGET_TYPE;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JpaProperties jpaProperties;

    @Test
    @Description("Ensures that actions list is in expected order.")
    void getActionStatusReturnsCorrectType() throws Exception {
        final int limitSize = 2;
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        assertThat(actions).hasSize(2);
        updateActionStatus(actions.get(0), Status.FINISHED, null, "test");

        final PageRequest pageRequest = PageRequest.of(0, 1000, Direction.ASC, ActionFields.ID.getFieldName());
        final Action action = deploymentManagement.findActionsByTarget(knownTargetId, pageRequest).getContent().get(0);

        final ActionStatus status = deploymentManagement.findActionStatusByAction(PAGE, action.getId()).getContent()
                .stream().sorted((e1, e2) -> Long.compare(e2.getId(), e1.getId())).collect(Collectors.toList()).get(0);

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actions.get(0).getId() + "/status")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:DESC"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(3)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                .andExpect(jsonPath("content.[0].id", equalTo(status.getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("finished")))
                .andExpect(jsonPath("content.[0].messages", hasSize(1)))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(status.getCreatedAt())))
                .andExpect(jsonPath("content.[1].type", equalTo("canceling")));
    }

    @Test
    @Description("Ensures that security token is not returned if user does not have READ_TARGET_SEC_TOKEN permission.")
    @WithUser(allSpPermissions = false, authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET })
    void securityTokenIsNotInResponseIfMissingPermission() throws Exception {

        final String knownControllerId = "knownControllerId";
        testdataFactory.createTarget(knownControllerId);
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", knownControllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("securityToken").doesNotExist());
    }

    @Test
    @Description("Ensures that security token is returned if user does have READ_TARGET_SEC_TOKEN permission.")
    @WithUser(allSpPermissions = false, authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET,
            SpPermission.READ_TARGET_SEC_TOKEN })
    void securityTokenIsInResponseWithCorrectPermission() throws Exception {

        final String knownControllerId = "knownControllerId";
        final Target createTarget = testdataFactory.createTarget(knownControllerId);
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", knownControllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("securityToken", equalTo(createTarget.getSecurityToken())));
    }

    @Test
    @Description("Ensures that that IP address is in result as stored in the repository.")
    void addressAndIpAddressInTargetResult() throws Exception {
        // prepare targets with IP
        final String knownControllerId1 = "0815";
        final String knownControllerId2 = "4711";

        createTarget(knownControllerId1);
        createTarget(knownControllerId2);

        // test
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(2)))
                .andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("$.content.[?(@.controllerId=='" + knownControllerId1 + "')].ipAddress",
                        contains("127.0.0.1")))
                .andExpect(jsonPath("$.content.[?(@.controllerId=='" + knownControllerId2 + "')].ipAddress",
                        contains("127.0.0.1")))
                .andExpect(jsonPath("$.content.[?(@.controllerId=='" + knownControllerId1 + "')].address",
                        contains(IpUtil.createHttpUri("127.0.0.1").toString())))
                .andExpect(jsonPath("$.content.[?(@.controllerId=='" + knownControllerId2 + "')].address",
                        contains(IpUtil.createHttpUri("127.0.0.1").toString())));
    }

    private void createTarget(final String controllerId) {
        targetManagement.create(entityFactory.target().create().controllerId(controllerId)
                .address(IpUtil.createHttpUri("127.0.0.1").toString()));
    }

    @Test
    @Description("Ensures that actions history is returned as defined by filter status==pending,status==finished.")
    void searchActionsRsql() throws Exception {

        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Target createTarget = testdataFactory.createTarget("knownTargetId");

        assignDistributionSet(dsA, Collections.singletonList(createTarget));

        final String rsqlPendingStatus = "status==pending";
        final String rsqlFinishedStatus = "status==finished";
        final String rsqlPendingOrFinishedStatus = rsqlFinishedStatus + "," + rsqlPendingStatus;
        // pending status one result
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions?q=" + rsqlPendingStatus,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].status", equalTo("pending")));

        // finished status none result
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions?q=" + rsqlFinishedStatus,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0))).andExpect(jsonPath("size", equalTo(0)));

        // pending or finished status one result
        mvc.perform(get(
                MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions?q=" + rsqlPendingOrFinishedStatus,
                createTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].status", equalTo("pending")));
    }

    @Test
    @Description("Ensures that a deletion of an active action results in cancellation triggered.")
    void cancelActionOK() throws Exception {
        // prepare test
        final Target tA = createTargetAndStartAction();

        // test - cancel the active action
        mvc.perform(
                delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}",
                        tA.getControllerId(), deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE)
                                .getContent().get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNoContent());

        final Action action = deploymentManagement.findAction(
                deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().get(0).getId()).get();
        // still active because in "canceling" state and waiting for controller
        // feedback
        assertThat(action.isActive()).isTrue();

        // action has not been cancelled confirmed from controller, so DS
        // remains assigned until
        // confirmation
        assertThat(deploymentManagement.getAssignedDistributionSet(tA.getControllerId())).isPresent();
        assertThat(deploymentManagement.getInstalledDistributionSet(tA.getControllerId())).isNotPresent();
    }

    @Test
    @Description("Ensures that method not allowed is returned if cancellation is triggered on already canceled action.")
    void cancelAndCancelActionIsNotAllowed() throws Exception {
        // prepare test
        final Target tA = createTargetAndStartAction();

        // cancel the active action
        deploymentManagement.cancelAction(
                deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().get(0).getId());

        // find the current active action
        final List<Action> cancelActions = deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE)
                .getContent().stream().filter(Action::isCancelingOrCanceled).collect(Collectors.toList());
        assertThat(cancelActions).hasSize(1);

        // test - cancel an cancel action returns forbidden
        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}",
                tA.getControllerId(), cancelActions.get(0).getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Description("Force Quit an Action, which is already canceled. Expected Result is an HTTP response code 204.")
    void forceQuitAnCanceledActionReturnsOk() throws Exception {

        final Target tA = createTargetAndStartAction();

        // cancel the active action
        deploymentManagement.cancelAction(
                deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().get(0).getId());

        // find the current active action
        final List<Action> cancelActions = deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE)
                .getContent().stream().filter(Action::isCancelingOrCanceled).collect(Collectors.toList());
        assertThat(cancelActions).hasSize(1);
        assertThat(cancelActions.get(0).isCancelingOrCanceled()).isTrue();

        // test - force quit: Canceled actions should return 204
        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}?force=true",
                tA.getControllerId(), cancelActions.get(0).getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
    }

    @Test
    @Description("Force Quit an Action, which is not canceled. Expected Result is an HTTP response code 405.")
    void forceQuitAnNotCanceledActionReturnsMethodNotAllowed() throws Exception {

        final Target tA = createTargetAndStartAction();

        // test - cancel an cancel action returns forbidden
        mvc.perform(
                delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}?force=true",
                        tA.getControllerId(), deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE)
                                .getContent().get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Description("Ensures that deletion is executed if permitted.")
    void deleteTargetReturnsOK() throws Exception {
        final String knownControllerId = "knownControllerIdDelete";
        testdataFactory.createTarget(knownControllerId);

        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId))
                .andExpect(status().isOk());

        assertThat(targetManagement.getByControllerID(knownControllerId)).isNotPresent();
    }

    @Test
    @Description("Ensures that deletion is refused with not found if target does not exist.")
    void deleteTargetWhichDoesNotExistsLeadsToNotFound() throws Exception {
        final String knownControllerId = "knownControllerIdDelete";

        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that update is refused with not found if target does not exist.")
    void updateTargetWhichDoesNotExistsLeadsToNotFound() throws Exception {
        final String knownControllerId = "knownControllerIdUpdate";
        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId).content("{}")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that target update request is reflected by repository.")
    void updateTargetDescription() throws Exception {
        final String knownControllerId = "123";
        final String knownNewDescription = "a new desc updated over rest";
        final String knownNameNotModify = "nameNotModify";
        final String body = new JSONObject().put("description", knownNewDescription).toString();

        // prepare
        targetManagement.create(entityFactory.target().create().controllerId(knownControllerId).name(knownNameNotModify)
                .description("old description"));

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.description", equalTo(knownNewDescription)))
                .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)));

        final Target findTargetByControllerID = targetManagement.getByControllerID(knownControllerId).get();
        assertThat(findTargetByControllerID.getDescription()).isEqualTo(knownNewDescription);
        assertThat(findTargetByControllerID.getName()).isEqualTo(knownNameNotModify);
    }

    @Test
    @Description("Ensures that target update request fails is updated value fails against a constraint.")
    void updateTargetDescriptionFailsIfInvalidLength() throws Exception {
        final String knownControllerId = "123";
        final String knownNewDescription = RandomStringUtils.randomAlphabetic(513);
        final String knownNameNotModify = "nameNotModify";
        final String body = new JSONObject().put("description", knownNewDescription).toString();

        // prepare
        targetManagement.create(entityFactory.target().create().controllerId(knownControllerId).name(knownNameNotModify)
                .description("old description"));

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final Target findTargetByControllerID = targetManagement.getByControllerID(knownControllerId).get();
        assertThat(findTargetByControllerID.getDescription()).isEqualTo("old description");
    }

    @Test
    @Description("Ensures that target update request is reflected by repository.")
    void updateTargetSecurityToken() throws Exception {
        final String knownControllerId = "123";
        final String knownNewToken = "6567576565";
        final String knownNameNotModify = "nameNotModify";
        final String body = new JSONObject().put("securityToken", knownNewToken).toString();

        // prepare
        targetManagement
                .create(entityFactory.target().create().controllerId(knownControllerId).name(knownNameNotModify));

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.securityToken", equalTo(knownNewToken)))
                .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)));

        final Target findTargetByControllerID = targetManagement.getByControllerID(knownControllerId).get();
        assertThat(findTargetByControllerID.getSecurityToken()).isEqualTo(knownNewToken);
        assertThat(findTargetByControllerID.getName()).isEqualTo(knownNameNotModify);
    }

    @Test
    @Description("Ensures that target update request is reflected by repository.")
    void updateTargetAddress() throws Exception {
        final String knownControllerId = "123";
        final String knownNewAddress = "amqp://test123/foobar";
        final String knownNameNotModify = "nameNotModify";
        final String body = new JSONObject().put("address", knownNewAddress).toString();

        // prepare
        targetManagement.create(entityFactory.target().create().controllerId(knownControllerId).name(knownNameNotModify)
                .address(knownNewAddress));

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.address", equalTo(knownNewAddress)))
                .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)));

        final Target findTargetByControllerID = targetManagement.getByControllerID(knownControllerId).get();
        assertThat(findTargetByControllerID.getAddress()).hasToString(knownNewAddress);
        assertThat(findTargetByControllerID.getName()).isEqualTo(knownNameNotModify);
    }

    @Test
    @Description("Ensures that when targetType value of -1 is provided the target type is unassigned from the target.")
    public void updateTargetAndUnnasignTargetType() throws Exception {
        final String knownControllerId = "123";
        final String knownNewAddress = "amqp://test123/foobar";
        final String knownNameNotModify = "controllerName";
        final Long unnasignTargetTypeValue = -1L;

        final TargetType targetType = targetTypeManagement.create(
            entityFactory.targetType().create().name("targettype1").description("targettypedes1"));

        final String body = new JSONObject().put("targetType", unnasignTargetTypeValue).toString();

        // create a target with the created TargetType
        targetManagement.create(entityFactory.target().create().controllerId(knownControllerId).name(knownNameNotModify)
            .address(knownNewAddress).targetType(targetType.getId()));

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
            .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
            .andExpect(jsonPath("$.address", equalTo(knownNewAddress)))
            .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)))
            .andExpect(jsonPath("$.targetType").exists());

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
            .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
            .andExpect(jsonPath("$.address", equalTo(knownNewAddress)))
            .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)))
            .andExpect(jsonPath("$.targetType").doesNotExist());

    }

    @Test
    @Description("Ensures that when targetType value of -1 is provided the target type is unassigned from the target when updating multiple fields in target object.")
    public void updateTargetNameAndUnnasignTargetType() throws Exception {
        final String knownControllerId = "123";
        final String knownNewAddress = "amqp://test123/foobar";
        final String knownNameNotModify = "controllerName";
        final Long unnasignTargetTypeValue = -1L;
        final String controllerNewName = "controllerNewName";

        final TargetType targetType = targetTypeManagement.create(
            entityFactory.targetType().create().name("targettype1").description("targettypedes1"));

        final String body = new JSONObject()
        .put("targetType", unnasignTargetTypeValue).put("name", "controllerNewName")
            .toString();

        // create a target with the created TargetType
        targetManagement.create(entityFactory.target().create().controllerId(knownControllerId).name(knownNameNotModify)
            .address(knownNewAddress).targetType(targetType.getId()));

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
            .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
            .andExpect(jsonPath("$.address", equalTo(knownNewAddress)))
            .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)))
            .andExpect(jsonPath("$.targetType").exists());

        //check if controller name is updated AND target type is missing (not assigned)
        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
            .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
            .andExpect(jsonPath("$.address", equalTo(knownNewAddress)))
            .andExpect(jsonPath("$.name", equalTo(controllerNewName)))
            .andExpect(jsonPath("$.targetType").doesNotExist());
    }

    @Test
    @Description("Ensures that target query returns list of targets in defined format.")
    void getTargetWithoutAdditionalRequestParameters() throws Exception {
        final int knownTargetAmount = 3;
        final String idA = "a";
        final String idB = "b";
        final String idC = "c";
        final String linksHrefPrefix = "http://localhost/rest/v1/targets/";
        createTargetsAlphabetical(knownTargetAmount);
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(knownTargetAmount)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')]._links.self.href",
                        contains(linksHrefPrefix + idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].name", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].description", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].controllerId", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].updateStatus", contains("registered")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].lastControllerRequestAt", notNullValue()))
                // idB
                .andExpect(jsonPath("$.content.[?(@.name=='" + idB + "')]._links.self.href",
                        contains(linksHrefPrefix + idB)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idB + "')].name", contains(idB)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idB + "')].description", contains(idB)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idB + "')].controllerId", contains(idB)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idB + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idB + "')].updateStatus", contains("registered")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].lastControllerRequestAt", notNullValue()))
                // idC
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')]._links.self.href",
                        contains(linksHrefPrefix + idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].name", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].description", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].controllerId", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].updateStatus", contains("registered")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].lastControllerRequestAt", notNullValue()));
    }

    @Test
    @Description("Ensures that target query returns list of targets in defined format in size reduced by given limit parameter.")
    void getTargetWithPagingLimitRequestParameter() throws Exception {
        final int knownTargetAmount = 3;
        final int limitSize = 1;
        createTargetsAlphabetical(knownTargetAmount);
        final String idA = "a";
        final String linksHrefPrefix = "http://localhost/rest/v1/targets/";

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')]._links.self.href",
                        contains(linksHrefPrefix + idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].name", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].description", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].controllerId", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].updateStatus", contains("registered")));
    }

    @Test
    @Description("Ensures that target query returns list of targets in defined format in size reduced by given limit and offset parameter.")
    void getTargetWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int knownTargetAmount = 5;
        final int offsetParam = 2;
        final int expectedSize = knownTargetAmount - offsetParam;
        final String idC = "c";
        final String idD = "d";
        final String idE = "e";
        final String linksHrefPrefix = "http://localhost/rest/v1/targets/";

        createTargetsAlphabetical(knownTargetAmount);
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(knownTargetAmount)))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')]._links.self.href",
                        contains(linksHrefPrefix + idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].name", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].description", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].controllerId", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].updateStatus", contains("registered")))
                // idB
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')]._links.self.href",
                        contains(linksHrefPrefix + idD)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].name", contains(idD)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].description", contains(idD)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].controllerId", contains(idD)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].updateStatus", contains("registered")))
                // idC
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')]._links.self.href",
                        contains(linksHrefPrefix + idE)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].name", contains(idE)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].description", contains(idE)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].controllerId", contains(idE)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].updateStatus", contains("registered")));
    }

    @Test
    @Description("Ensures that the get request for a target works.")
    void getSingleTarget() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        final Target target = createSingleTarget(knownControllerId, knownName);
        final String hrefPrefix = "http://localhost/rest/v1/targets/" + knownControllerId + "/";
        // test
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownName)))
                .andExpect(jsonPath(JSON_PATH_CONTROLLERID, equalTo(knownControllerId)))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(TARGET_DESCRIPTION_TEST)))
                .andExpect(jsonPath(JSON_PATH_LAST_REQUEST_AT, equalTo(target.getLastTargetQuery())))
                .andExpect(jsonPath("$.pollStatus", hasKey("lastRequestAt")))
                .andExpect(jsonPath("$.pollStatus", hasKey("nextExpectedRequestAt")))
                .andExpect(jsonPath("$.pollStatus.overdue", equalTo(false)))
                .andExpect(jsonPath("$._links.assignedDS.href", equalTo(hrefPrefix + "assignedDS")))
                .andExpect(jsonPath("$._links.installedDS.href", equalTo(hrefPrefix + "installedDS")))
                .andExpect(jsonPath("$._links.actions.href",
                        equalTo(hrefPrefix + "actions" + "?offset=0&limit=50&sort=id%3ADESC")));
    }

    @Test
    @Description("Ensures that target get request returns a not found if the target does not exits.")
    void getSingleTargetNoExistsResponseNotFound() throws Exception {

        final String targetIdNotExists = "bubu";

        // test
        final MvcResult mvcResult = mvc
                .perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetIdNotExists))
                .andExpect(status().isNotFound()).andReturn();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_ENTITY_NOT_EXISTS.getKey());
    }

    @Test
    @Description("Ensures that get request for asigned distribution sets returns no count if no distribution set has been assigned.")
    void getAssignedDistributionSetOfTargetIsEmpty() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);

        // test
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId + "/assignedDS"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

    }

    @Test
    @Description("Ensures that the get request for asigned distribution sets works.")
    void getAssignedDistributionSetOfTarget() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds.getId(), knownControllerId);

        // test

        final SoftwareModule os = ds.findFirstModuleByType(osType).get();
        final SoftwareModule jvm = ds.findFirstModuleByType(runtimeType).get();
        final SoftwareModule bApp = ds.findFirstModuleByType(appType).get();
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId + "/assignedDS"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_ID, equalTo(ds.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(ds.getName())))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(ds.getDescription())))
                // os
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(os.getId().intValue())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].name", contains(os.getName())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].description",
                        contains(os.getDescription())))
                .andExpect(
                        jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].version", contains(os.getVersion())))
                .andExpect(
                        jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].vendor", contains(os.getVendor())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].type", contains("os")))
                // jvm
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].id",
                        contains(jvm.getId().intValue())))
                .andExpect(
                        jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].name", contains(jvm.getName())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].description",
                        contains(jvm.getDescription())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].version",
                        contains(jvm.getVersion())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].vendor",
                        contains(jvm.getVendor())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].type", contains("runtime")))
                // baseApp
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(bApp.getId().intValue())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].name", contains(bApp.getName())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].description",
                        contains(bApp.getDescription())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].version",
                        contains(bApp.getVersion())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].vendor",
                        contains(bApp.getVendor())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].type", contains("application")));

    }

    @Test
    @Description("Ensures that get request for installed distribution sets returns no count if no distribution set has been installed.")
    void getInstalledDistributionSetOfTargetIsEmpty() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownControllerId + "/installedDS"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    @Description("Ensures that a target creation with empty name and a controllerId that exceeds the name length limitation is successful and the name gets truncated.")
    void createTargetWithEmptyNameAndLongControllerId() throws Exception {
        final String randomString = RandomStringUtils.randomAlphanumeric(JpaTarget.CONTROLLER_ID_MAX_SIZE);

        final Target target = entityFactory.target().create().controllerId(randomString).build();

        final String targetList = JsonBuilder.targets(Collections.singletonList(target), false);

        final String expectedTargetName = randomString.substring(0,
                Math.min(JpaTarget.CONTROLLER_ID_MAX_SIZE, NamedEntity.NAME_MAX_SIZE));

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING).content(targetList)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated())
                .andExpect(jsonPath("[0].controllerId", equalTo(randomString)))
                .andExpect(jsonPath("[0].name", equalTo(expectedTargetName)));

        assertThat(targetManagement.getByControllerID(randomString).get().getName()).isEqualTo(expectedTargetName);
    }

    @Test
    @Description("Ensures that post request for creating a target with no payload returns a bad request.")
    void createTargetWithoutPayloadBadRequest() throws Exception {

        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getKey());
        assertThat(exceptionInfo.getMessage()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getMessage());
    }

    @Test
    @Description("Ensures that post request for creating a target with invalid payload returns a bad request.")
    void createTargetWithBadPayloadBadRequest() throws Exception {
        final String notJson = "abc";

        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING).content(notJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(MessageNotReadableException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getKey());
    }

    @Test
    @Description("Verifies that a mandatory properties of new targets are validated as not null.")
    void createTargetWithMissingMandatoryPropertyBadRequest() throws Exception {
        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING).content("[{\"name\":\"id1\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(ConstraintViolationException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getKey());
    }

    @Test
    @Description("Verfies that a  properties of new targets are validated as in allowed size range.")
    void createTargetWithInvalidPropertyBadRequest() throws Exception {
        final Target test1 = entityFactory.target().create().controllerId("id1")
                .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1)).build();

        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)
                        .content(JsonBuilder.targets(Collections.singletonList(test1), true))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(ConstraintViolationException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getKey());
    }

    @Test
    @Description("Ensures that a post request for creating multiple targets works.")
    void createTargetsListReturnsSuccessful() throws Exception {
        final Target test1 = entityFactory.target().create().controllerId("id1").name("testname1")
                .securityToken("token").address("amqp://test123/foobar").description("testid1").build();
        final Target test2 = entityFactory.target().create().controllerId("id2").name("testname2")
                .description("testid2").build();
        final Target test3 = entityFactory.target().create().controllerId("id3").name("testname3")
                .description("testid3").build();

        final List<Target> targets = Arrays.asList(test1, test2, test3);

        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/targets").content(JsonBuilder.targets(targets, true))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo("testname1")))
                .andExpect(jsonPath("[0].controllerId", equalTo("id1")))
                .andExpect(jsonPath("[0].description", equalTo("testid1")))
                .andExpect(jsonPath("[0].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[0].securityToken", equalTo("token")))
                .andExpect(jsonPath("[0].address", equalTo("amqp://test123/foobar")))
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
                JsonPath.compile("[0]._links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/targets/id1");
        assertThat(
                JsonPath.compile("[1]._links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/targets/id2");
        assertThat(
                JsonPath.compile("[2]._links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/targets/id3");

        final Target t1 = assertTarget("id1", "testname1", "testid1");
        assertThat(t1.getSecurityToken()).isEqualTo("token");
        assertThat(t1.getAddress()).hasToString("amqp://test123/foobar");

        assertTarget("id2", "testname2", "testid2");
        assertTarget("id3", "testname3", "testid3");
    }

    private Target assertTarget(final String controllerId, final String name, final String description) {
        final Optional<Target> target1 = targetManagement.getByControllerID(controllerId);
        assertThat(target1).isPresent();
        final Target t = target1.get();
        assertThat(t.getName()).isEqualTo(name);
        assertThat(t.getDescription()).isEqualTo(description);
        return t;
    }

    @Test
    @Description("Ensures that a post request for creating one target within a list works.")
    void createTargetsSingleEntryListReturnsSuccessful() throws Exception {
        final String knownName = "someName";
        final String knownControllerId = "controllerId1";
        final String knownDescription = "someDescription";
        final String createTargetsJson = getCreateTargetsListJsonString(knownControllerId, knownName, knownDescription);

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING).content(createTargetsJson)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is2xxSuccessful());

        final Slice<Target> findTargetsAll = targetManagement.findAll(PageRequest.of(0, 100));
        final Target target = findTargetsAll.getContent().get(0);
        assertThat(targetManagement.count()).isEqualTo(1);
        assertThat(target.getControllerId()).isEqualTo(knownControllerId);
        assertThat(target.getName()).isEqualTo(knownName);
        assertThat(target.getDescription()).isEqualTo(knownDescription);
    }

    @Test
    @Description("Ensures that a post request for creating the same target again leads to a conflict response.")
    void createTargetsSingleEntryListDoubleReturnConflict() throws Exception {
        final String knownName = "someName";
        final String knownControllerId = "controllerId1";
        final String knownDescription = "someDescription";
        final String createTargetsJson = getCreateTargetsListJsonString(knownControllerId, knownName, knownDescription);

        // create a taret first to provoke a already exists error

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING).content(createTargetsJson)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is2xxSuccessful());
        // create another one to retrieve the entity already exists exception
        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING).content(createTargetsJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.CONFLICT.value())).andReturn();

        // verify only one entry
        assertThat(targetManagement.count()).isEqualTo(1);

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(EntityAlreadyExistsException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_ENTITY_ALREADY_EXISTS.getKey());
        assertThat(exceptionInfo.getMessage()).isEqualTo(SpServerError.SP_REPO_ENTITY_ALREADY_EXISTS.getMessage());
    }

    @Test
    @Description("Ensures that the get request for action of a target returns no actions if nothing has happened.")
    void getActionWithEmptyResult() throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTarget(knownTargetId);

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(0))).andExpect(jsonPath("content", hasSize(0)))
                .andExpect(jsonPath("total", equalTo(0)));
    }

    @Test
    @Description("Ensures that the expected response is returned for update action.")
    void getUpdateAction() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actions.get(1).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("type", equalTo("update"))).andExpect(jsonPath("status", equalTo("pending")))
                .andExpect(jsonPath("forceType", equalTo("forced")))
                .andExpect(jsonPath("maintenanceWindow").doesNotExist())
                .andExpect(jsonPath("_links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                .andExpect(jsonPath("_links.distributionset.href",
                        equalTo(generateActionDsLink(actions.get(1).getDistributionSet().getId()))))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(knownTargetId, actions.get(1).getId()))));
    }

    @Test
    @Description("Ensures that the expected response is returned for update action with maintenance window.")
    void getUpdateActionWithMaintenanceWindow() throws Exception {
        final String knownTargetId = "targetId";
        final String schedule = getTestSchedule(10);
        final String duration = getTestDuration(10);
        final String timezone = getTestTimeZone();
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(knownTargetId,
                schedule, duration, timezone);

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actions.get(1).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("type", equalTo("update"))).andExpect(jsonPath("status", equalTo("pending")))
                .andExpect(jsonPath("forceType", equalTo("forced")))
                .andExpect(jsonPath("maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("maintenanceWindow.nextStartAt",
                        equalTo(actions.get(1).getMaintenanceWindowStartTime().get().toInstant().toEpochMilli())))
                .andExpect(jsonPath("_links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                .andExpect(jsonPath("_links.distributionset.href",
                        equalTo(generateActionDsLink(actions.get(1).getDistributionSet().getId()))))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(knownTargetId, actions.get(1).getId()))));
    }

    @Test
    @Description("Ensures that the expected response is returned when update action was cancelled.")
    void getCancelAction() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actions.get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("type", equalTo("cancel"))).andExpect(jsonPath("status", equalTo("pending")))
                .andExpect(jsonPath("forceType", equalTo("forced")))
                .andExpect(jsonPath("maintenanceWindow").doesNotExist())
                .andExpect(jsonPath("_links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                .andExpect(jsonPath("_links.canceledaction.href",
                        equalTo(generateCanceledactionreferenceLink(knownTargetId, actions.get(0)))))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(knownTargetId, actions.get(0).getId()))));
    }

    @Test
    @Description("Ensures that the expected response is returned when update action with maintenance window was cancelled.")
    void getCancelActionWithMaintenanceWindow() throws Exception {
        final String knownTargetId = "targetId";
        final String schedule = getTestSchedule(10);
        final String duration = getTestDuration(10);
        final String timezone = getTestTimeZone();
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(knownTargetId,
                schedule, duration, timezone);

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actions.get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("forceType", equalTo("forced"))).andExpect(jsonPath("type", equalTo("cancel")))
                .andExpect(jsonPath("status", equalTo("pending")))
                .andExpect(jsonPath("maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("maintenanceWindow.nextStartAt",
                        equalTo(actions.get(0).getMaintenanceWindowStartTime().get().toInstant().toEpochMilli())))
                .andExpect(jsonPath("_links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                .andExpect(jsonPath("_links.canceledaction.href",
                        equalTo(generateCanceledactionreferenceLink(knownTargetId, actions.get(0)))))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(knownTargetId, actions.get(0).getId()))));
    }

    @Test
    @Description("Ensures that the expected response of getting actions of a target is returned.")
    void getActions() throws Exception {
        getActions(false);
    }

    @Test
    @Description("Ensures that the expected response of getting actions (with ext refs) of a target is returned.")
    void getActionsExtRef() throws Exception {
        getActions(true);
    }

    private void getActions(final boolean withExternalRef) throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        final List<String> externalRefs = new ArrayList<>(2);
        if (withExternalRef) {
            externalRefs.add("extRef#123_0");
            externalRefs.add("extRef#123_1");
            controllerManagement.updateActionExternalRef(actions.get(0).getId(), externalRefs.get(0));
            controllerManagement.updateActionExternalRef(actions.get(1).getId(), externalRefs.get(1));
        }

        final ResultActions resultActions =
                mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                        + MgmtRestConstants.TARGET_V1_ACTIONS).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                    .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                    .andExpect(jsonPath("content.[1].id", equalTo(actions.get(1).getId().intValue())))
                    .andExpect(jsonPath("content.[1].type", equalTo("update")))
                    .andExpect(jsonPath("content.[1].status", equalTo("pending")))
                    .andExpect(jsonPath("content.[1]._links.self.href",
                            equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                    .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                    .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                    .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                    .andExpect(jsonPath("content.[0]._links.self.href",
                            equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                    .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                    .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                    .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

        if (withExternalRef) {
            resultActions
                    .andExpect(jsonPath("content.[1].externalRef", equalTo(externalRefs.get(1))))
                    .andExpect(jsonPath("content.[0].externalRef", equalTo(externalRefs.get(0))));
        }
    }

    @Test
    @Description("Ensures that the expected response of getting actions with maintenance window of a target is returned.")
    void getActionsWithMaintenanceWindow() throws Exception {
        final String knownTargetId = "targetId";
        final String schedule = getTestSchedule(10);
        final String duration = getTestDuration(10);
        final String timezone = getTestTimeZone();
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(knownTargetId,
                schedule, duration, timezone);

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[1].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("update")))
                .andExpect(jsonPath("content.[1].status", equalTo("pending")))
                .andExpect(jsonPath("content.[1].maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("content.[1].maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("content.[1].maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("content.[1].maintenanceWindow.nextStartAt",
                        equalTo(actions.get(1).getMaintenanceWindowStartTime().get().toInstant().toEpochMilli())))
                .andExpect(jsonPath("content.[1]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0].maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("content.[0].maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("content.[0].maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("content.[0].maintenanceWindow.nextStartAt",
                        equalTo(actions.get(0).getMaintenanceWindowStartTime().get().toInstant().toEpochMilli())))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    @Test
    @Description("Verifies that the API returns the status list with expected content.")
    void getActionsStatus() throws Exception {
        final String knownTargetId = "targetId";
        final Action action = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId).get(0);
        // retrieve list in default descending order for actionstaus entries
        final List<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(PAGE, action.getId())
                .getContent().stream().sorted((e1, e2) -> Long.compare(e2.getId(), e1.getId()))
                .collect(Collectors.toList());

        // sort is default descending order, latest status first
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                + MgmtRestConstants.TARGET_V1_ACTION_STATUS)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("canceling")))
                .andExpect(jsonPath("content.[0].messages",
                        hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath("content.[1].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("running")))
                .andExpect(jsonPath("content.[1].reportedAt", equalTo(actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    @Test
    @Description("Verifies that the API returns the status list with expected content sorted by reportedAt field.")
    void getActionsStatusSortedByReportedAt() throws Exception {
        final String knownTargetId = "targetId";
        final Action action = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId).get(0);
        final List<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(PAGE, action.getId())
                .getContent().stream().sorted(Comparator.comparingLong(Identifiable::getId))
                .collect(Collectors.toList());

        // descending order
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                + MgmtRestConstants.TARGET_V1_ACTION_STATUS).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING,
                        "REPORTEDAT:DESC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("canceling")))
                .andExpect(jsonPath("content.[0].messages",
                        hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath("content.[1].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("running")))
                .andExpect(jsonPath("content.[1].reportedAt", equalTo(actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

        // ascending order
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                + MgmtRestConstants.TARGET_V1_ACTION_STATUS).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING,
                        "REPORTEDAT:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[1].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("canceling")))
                .andExpect(jsonPath("content.[1].messages",
                        hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[1].reportedAt", equalTo(actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("running")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    @Test
    @Description("Verifies that the API returns the status list with expected content split into two pages.")
    void getActionsStatusWithPagingLimitRequestParameter() throws Exception {
        final String knownTargetId = "targetId";

        final Action action = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId).get(0);
        final List<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(PAGE, action.getId())
                .getContent().stream().sorted((e1, e2) -> Long.compare(e1.getId(), e2.getId()))
                .collect(Collectors.toList());

        // Page 1
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                + MgmtRestConstants.TARGET_V1_ACTION_STATUS).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT,
                        String.valueOf(1)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("canceling")))
                .andExpect(jsonPath("content.[0].messages",
                        hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // Page 2
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                + MgmtRestConstants.TARGET_V1_ACTION_STATUS)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("running")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    @Test
    @Description("Verifies getting multiple actions with the paging request parameter.")
    void getActionsWithPagingLimitRequestParameter() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        // page 1: one entry
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // page 2: one entry
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("update")))
                .andExpect(jsonPath("content.[0].status", equalTo("pending")))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    private String generateActionSelfLink(final String knownTargetId, final Long actionId) {
        return "http://localhost" + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId;
    }

    private String generateActionDsLink(final Long dsId) {
        return "http://localhost" + MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + dsId;
    }

    private String generateCanceledactionreferenceLink(final String knownTargetId, final Action action) {
        return "http://localhost" + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + action.getId();
    }

    private String generateStatusreferenceLink(final String knownTargetId, final Long actionId) {
        return "http://localhost" + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId + "/" + MgmtRestConstants.TARGET_V1_ACTION_STATUS
                + "?offset=0&limit=50&sort=id%3ADESC";
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverride(final String knownTargetId) {
        return generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(knownTargetId, null, null, null);
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(final String knownTargetId,
            final String schedule, final String duration, final String timezone) {
        final Target target = testdataFactory.createTarget(knownTargetId);

        final Iterator<DistributionSet> sets = testdataFactory.createDistributionSets(2).iterator();
        final DistributionSet one = sets.next();
        final DistributionSet two = sets.next();

        // Update
        if (schedule == null) {
            final List<Target> updatedTargets = assignDistributionSet(one, Collections.singletonList(target))
                    .getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
            // 2nd update
            // sleep 10ms to ensure that we can sort by reportedAt
            Awaitility.await().atMost(Duration.ofMillis(100)).atLeast(5, TimeUnit.MILLISECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSet(two, updatedTargets);
        } else {
            final List<Target> updatedTargets = assignDistributionSetWithMaintenanceWindow(one.getId(),
                    target.getControllerId(), schedule, duration, timezone).getAssignedEntity().stream()
                            .map(Action::getTarget).collect(Collectors.toList());
            // 2nd update
            // sleep 10ms to ensure that we can sort by reportedAt
            Awaitility.await().atMost(Duration.ofMillis(100)).atLeast(5, TimeUnit.MILLISECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSetWithMaintenanceWindow(two.getId(), updatedTargets.get(0).getControllerId(), schedule,
                    duration, timezone);
        }

        // two updates, one cancellation
        final List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE)
                .getContent();

        assertThat(actions).hasSize(2);
        return actions;
    }

    @Test
    @Description("Verfies that an action is switched from soft to forced if requested by management API")
    void updateAction() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet set = testdataFactory.createDistributionSet();
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(set.getId(), target.getControllerId(), ActionType.SOFT));
        assertThat(deploymentManagement.findAction(actionId).get().getActionType()).isEqualTo(ActionType.SOFT);

        final String body = new JSONObject().put("forceType", "forced").toString();
        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actionId.intValue()))).andExpect(jsonPath("type", equalTo("update")))
                .andExpect(jsonPath("status", equalTo("pending"))).andExpect(jsonPath("forceType", equalTo("forced")))
                .andExpect(jsonPath("_links.self.href",
                        equalTo(generateActionSelfLink(target.getControllerId(), actionId))))
                .andExpect(jsonPath("_links.distributionset.href",
                        equalTo("http://localhost/rest/v1/distributionsets/" + set.getId())))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(target.getControllerId(), actionId))));

        assertThat(deploymentManagement.findAction(actionId).get().getActionType()).isEqualTo(ActionType.FORCED);
    }

    @Test
    @Description("Verifies that a DS to target assignment is reflected by the repository and that repeating "
            + "the assignment does not change the target.")
    void assignDistributionSetToTarget() throws Exception {
        Target target = testdataFactory.createTarget();
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/assignedDS")
                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(1))).andExpect(jsonPath("alreadyAssigned", equalTo(0)))
                .andExpect(jsonPath("total", equalTo(1)));
        implicitLock(set);

        assertThat(deploymentManagement.getAssignedDistributionSet(target.getControllerId()).get()).isEqualTo(set);
        target = targetManagement.getByControllerID(target.getControllerId()).get();

        // repeating DS assignment leads again to OK
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/assignedDS")
                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(0))).andExpect(jsonPath("alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1)));

        // ...but does not change the target
        assertThat(targetManagement.getByControllerID(target.getControllerId()).get()).isEqualTo(target);
    }

    @Autowired
    ActionRepository actionRepository;
    @ParameterizedTest
    @MethodSource("confirmationOptions")
    @Description("Ensures that confirmation option is considered in assignment request.")
    void assignDistributionSetToTargetWithConfirmationOptions(final boolean confirmationFlowActive,
            final Boolean confirmationRequired) throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        final JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("id", set.getId());
        if (confirmationRequired != null) {
            jsonPayload.put("confirmationRequired", confirmationRequired);
        }

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/assignedDS")
                .content(jsonPayload.toString()).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(1))).andExpect(jsonPath("alreadyAssigned", equalTo(0)))
                .andExpect(jsonPath("total", equalTo(1)));
        implicitLock(set);

        assertThat(deploymentManagement.getAssignedDistributionSet(target.getControllerId()).get()).isEqualTo(set);

        assertThat(
                actionRepository
                        .findAll(ActionSpecifications.byDistributionSetId(set.getId()), PAGE)
                        .map(Action.class::cast).getContent()).hasSize(1)
                .allMatch(action -> {
                    if (!confirmationFlowActive) {
                        return !action.isWaitingConfirmation();
                    }
                    return confirmationRequired == null ? action.isWaitingConfirmation()
                            : confirmationRequired == action.isWaitingConfirmation();
                });
    }

    private static Stream<Arguments> confirmationOptions() {
        return Stream.of(Arguments.of(true, true), Arguments.of(true, false), Arguments.of(false, true),
                Arguments.of(false, false), Arguments.of(true, null), Arguments.of(false, null));
    }

    @Test
    @Description("Verfies that a DOWNLOAD_ONLY DS to target assignment is properly handled")
    void assignDownloadOnlyDistributionSetToTarget() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/assignedDS")
                .content("{\"id\":" + set.getId() + ",\"type\": \"downloadonly\"}")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(1))).andExpect(jsonPath("alreadyAssigned", equalTo(0)))
                .andExpect(jsonPath("total", equalTo(1)));
        implicitLock(set);

        assertThat(deploymentManagement.getAssignedDistributionSet(target.getControllerId()).get()).isEqualTo(set);
        final Slice<Action> actions = deploymentManagement.findActionsByTarget("targetExist", PageRequest.of(0, 100));
        assertThat(actions).isNotEmpty().filteredOn(a -> a.getDistributionSet().equals(set))
                .extracting(Action::getActionType).containsOnly(ActionType.DOWNLOAD_ONLY);
    }

    @Test
    @Description("Verfies that an offline DS to target assignment is reflected by the repository and that repeating "
            + "the assignment does not change the target.")
    void offlineAssignDistributionSetToTarget() throws Exception {
        Target target = testdataFactory.createTarget();
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId()
                + "/assignedDS?offline=true").content("{\"id\":" + set.getId() + "}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(1))).andExpect(jsonPath("alreadyAssigned", equalTo(0)))
                .andExpect(jsonPath("total", equalTo(1)));
        implicitLock(set);

        assertThat(deploymentManagement.getAssignedDistributionSet(target.getControllerId()).get()).isEqualTo(set);
        assertThat(deploymentManagement.getInstalledDistributionSet(target.getControllerId()).get()).isEqualTo(set);
        target = targetManagement.getByControllerID(target.getControllerId()).get();
        assertThat(target.getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);

        // repeating DS assignment leads again to OK
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId()
                + "/assignedDS?offline=true").content("{\"id\":" + set.getId() + "}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(0))).andExpect(jsonPath("alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1)));

        // ...but does not change the target
        assertThat(targetManagement.getByControllerID(target.getControllerId()).get()).isEqualTo(target);
    }

    @Test
    void assignDistributionSetToTargetWithActionTimeForcedAndTime() throws Exception {
        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final long forceTime = System.currentTimeMillis();
        final String body = new JSONObject().put("id", set.getId()).put("type", "timeforced")
                .put("forcetime", forceTime).toString();

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS").content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        implicitLock(set);

        final List<Action> findActiveActionsByTarget = deploymentManagement
                .findActiveActionsByTarget(PAGE, target.getControllerId()).getContent();
        assertThat(findActiveActionsByTarget).hasSize(1);
        assertThat(findActiveActionsByTarget.get(0).getActionType()).isEqualTo(ActionType.TIMEFORCED);
        assertThat(findActiveActionsByTarget.get(0).getForcedTime()).isEqualTo(forceTime);
        assertThat(deploymentManagement.getAssignedDistributionSet("fsdfsd").get()).isEqualTo(set);
    }

    @Test
    @Description("Assigns distribution set to target with only maintenance schedule.")
    void assignDistributionSetToTargetWithMaintenanceWindowStartTimeOnly() throws Exception {

        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final String body = new JSONObject().put("id", set.getId()).put("type", "forced")
                .put("maintenanceWindow", new JSONObject().put("schedule", getTestSchedule(0))).toString();

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/assignedDS")
                .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Assigns distribution set to target with only maintenance window duration.")
    void assignDistributionSetToTargetWithMaintenanceWindowEndTimeOnly() throws Exception {

        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final String body = new JSONObject().put("id", set.getId()).put("type", "forced")
                .put("maintenanceWindow", new JSONObject().put("duration", getTestDuration(10))).toString();

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/assignedDS")
                .content(body).contentType(MediaTypes.HAL_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Assigns distribution set to target with valid maintenance window.")
    void assignDistributionSetToTargetWithValidMaintenanceWindow() throws Exception {

        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final String body = new JSONObject().put("id", set.getId()).put("type", "forced").put("forcetime", "0")
                .put("maintenanceWindow", new JSONObject().put("schedule", getTestSchedule(10))
                        .put("duration", getTestDuration(10)).put("timezone", getTestTimeZone()))
                .toString();

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/assignedDS")
                .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Assigns distribution set to target with maintenance window next execution start (should be ignored, calculated automaticaly based on schedule, duration and timezone)")
    void assignDistributionSetToTargetWithMaintenanceWindowNextExecutionStart() throws Exception {

        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final long nextExecutionStart = System.currentTimeMillis();

        final String body = new JSONObject().put("id", set.getId()).put("maintenanceWindow",
                new JSONObject().put("schedule", getTestSchedule(10)).put("duration", getTestDuration(10))
                        .put("timezone", getTestTimeZone()).put("nextStartAt", String.valueOf(nextExecutionStart)))
                .toString();

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/assignedDS")
                .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS)).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath("content.[0].maintenanceWindow.nextStartAt", not(nextExecutionStart)));
    }

    @Test
    @Description("Assigns distribution set to target with last maintenance window scheduled before current time.")
    void assignDistributionSetToTargetWithMaintenanceWindowEndTimeBeforeStartTime() throws Exception {

        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final String body = new JSONObject().put("id", set.getId()).put("type", "forced")
                .put("maintenanceWindow", new JSONObject().put("schedule", getTestSchedule(-30))
                        .put("duration", getTestDuration(5)).put("timezone", getTestTimeZone()))
                .toString();

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + target.getControllerId() + "/assignedDS")
                .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidRequestsOnAssignDistributionSetToTarget() throws Exception {

        final DistributionSet set = testdataFactory.createDistributionSet("one");

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS")
                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        testdataFactory.createTarget("fsdfsd");

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS")
                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS")
                .content("{\"id\":12345678}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS")
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/fsdfsd/assignedDS")
                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

    }

    @Test
    void invalidRequestsOnActionResource() throws Exception {
        final String knownTargetId = "targetId";

        // target does not exist
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        final Long actionId = actions.get(0).getId();

        // should work now
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // action does not exist
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/12321")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // Invalid content
        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void invalidRequestsOnActionStatusResource() throws Exception {
        final String knownTargetId = "targetId";

        // target does not exist
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/1/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        final Long actionId = actions.get(0).getId();

        // should work now
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId + "/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // action does not exist
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/12321/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId + "/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId + "/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/"
                + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId + "/status")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void getControllerAttributesReturnsAttributesWithOk() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdWithAttributes";
        final Map<String, String> knownControllerAttrs = new HashMap<>();
        knownControllerAttrs.put("a", "1");
        knownControllerAttrs.put("b", "2");
        testdataFactory.createTarget(knownTargetId);
        controllerManagement.updateControllerAttributes(knownTargetId, knownControllerAttrs, null);

        // test query target over rest resource
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/attributes"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.a", equalTo("1"))).andExpect(jsonPath("$.b", equalTo("2")));
    }

    @Test
    void getControllerEmptyAttributesReturnsNoContent() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdWithAttributes";
        testdataFactory.createTarget(knownTargetId);

        // test query target over rest resource
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/attributes"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    @Description("Request update of Controller Attributes.")
    void triggerControllerAttributesUpdate() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdNeedsUpdate";
        final Map<String, String> knownControllerAttrs = new HashMap<>();
        knownControllerAttrs.put("a.1", "1");
        knownControllerAttrs.put("b.2", "2");
        testdataFactory.createTarget(knownTargetId);
        controllerManagement.updateControllerAttributes(knownTargetId, knownControllerAttrs, null);
        assertThat(targetManagement.isControllerAttributesRequested(knownTargetId)).isFalse();

        verifyAttributeUpdateCanBeRequested(knownTargetId);

        verifyRequestAttributesAttributeIsOptional(knownTargetId);

        verifyResettingRequestAttributesIsNotAllowed(knownTargetId);
    }

    @Step
    private void verifyAttributeUpdateCanBeRequested(final String knownTargetId) throws Exception {
        final String body = new JSONObject().put("requestAttributes", true).toString();

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(targetManagement.isControllerAttributesRequested(knownTargetId)).isTrue();
    }

    @Step
    private void verifyRequestAttributesAttributeIsOptional(final String knownTargetId) throws Exception {
        final String body = new JSONObject().put("description", "verify attribute can be missing").toString();

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Step
    private void verifyResettingRequestAttributesIsNotAllowed(final String knownTargetId) throws Exception {
        final String body = new JSONObject().put("requestAttributes", false).toString();

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        assertThat(targetManagement.isControllerAttributesRequested(knownTargetId)).isTrue();
    }

    @Test
    @Description("Handles the GET request of retrieving all targets within SP..")
    public void getTargets() throws Exception {
        enableConfirmationFlow();
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print());
    }

    @Test
    @Description("Handles the GET request of retrieving all targets within SP based by parameter.")
    public void getTargetsWithParameters() throws Exception {
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "?limit=10&sort=name:ASC&offset=0&q=name==a"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print());
    }

    @Test
    void searchTargetsUsingRsqlQuery() throws Exception {
        final int amountTargets = 10;
        createTargetsAlphabetical(amountTargets);

        final String rsqlFindAOrB = "controllerId==a,controllerId==b";

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "?q=" + rsqlFindAOrB))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2))).andExpect(jsonPath("content[0].controllerId", equalTo("a")))
                .andExpect(jsonPath("content[1].controllerId", equalTo("b")));
    }

    private String getCreateTargetsListJsonString(final String controllerId, final String name,
            final String description) {
        return "[{\"name\":\"" + name + "\",\"controllerId\":\"" + controllerId + "\",\"description\":\"" + description
                + "\"}]";
    }

    private Target createSingleTarget(final String controllerId, final String name) {
        targetManagement.create(entityFactory.target().create().controllerId(controllerId).name(name)
                .description(TARGET_DESCRIPTION_TEST));
        return controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, LOCALHOST);
    }

    /**
     * Creating targets with the given amount by setting name, id etc from the
     * alphabet [a-z] using ASCII.
     *
     * @param amount
     *            The number of targets to create
     */
    private void createTargetsAlphabetical(final int amount) {
        char character = 'a';
        for (int index = 0; index < amount; index++) {
            final String str = String.valueOf(character);
            targetManagement.create(entityFactory.target().create().controllerId(str).name(str).description(str));
            controllerManagement.findOrRegisterTargetIfItDoesNotExist(str, LOCALHOST);
            character++;
        }
    }

    /**
     * helper method to create a target and start an action on it.
     *
     * @return The targetid of the created target.
     */
    private Target createTargetAndStartAction() {
        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Target tA = testdataFactory.createTarget("target-id-A");
        // assign a distribution set so we get an active update action
        assignDistributionSet(dsA, Arrays.asList(tA));
        // verify active action
        final Slice<Action> actionsByTarget = deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE);
        assertThat(actionsByTarget.getContent()).hasSize(1);
        return targetManagement.getByControllerID(tA.getControllerId()).get();
    }

    @Test
    void getControllerTagReturnsTagWithNoContent() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdWithNoTags";
        final Target target = testdataFactory.createTarget(knownTargetId);

        // test query target over rest resource
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/tags"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
    }

    @Test
    void getControllerTagReturnsTagWithOk() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdWithTags";
        final Target target = testdataFactory.createTarget(knownTargetId);
        final List<TargetTag> targetTags = testdataFactory.createTargetTags(2, "tag_getControllerTagReturnsTagWithOk");
        final List<String> tagNames = new ArrayList<>();
        for (final TargetTag targetTag : targetTags) {
            targetManagement.toggleTagAssignment(Collections.singletonList(target.getControllerId()), targetTag.getName());
            tagNames.add(targetTag.getName());
        }

        // test query target over rest resource
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + knownTargetId + "/tags"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.[0].name", in(tagNames)))
                .andExpect(jsonPath("$.[1].name", in(tagNames)));
    }

    @Test
    @Description("Ensures that the metadata creation through API is reflected by the repository.")
    void createMetadata() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";
        testdataFactory.createTarget(knownControllerId);

        final String knownKey1 = "known.key.1";
        final String knownKey2 = "knownKey2";

        final String knownValue1 = "knownValue1";
        final String knownValue2 = "knownValue2";

        final JSONArray metaData1 = new JSONArray();
        metaData1.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        metaData1.put(new JSONObject().put("key", knownKey2).put("value", knownValue2));

        mvc.perform(post("/rest/v1/targets/{targetId}/metadata", knownControllerId).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(metaData1.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].key", equalTo(knownKey1)))
                .andExpect(jsonPath("[0].value", equalTo(knownValue1)))
                .andExpect(jsonPath("[1].key", equalTo(knownKey2)))
                .andExpect(jsonPath("[1].value", equalTo(knownValue2)));

        final TargetMetadata metaKey1 = targetManagement.getMetaDataByControllerId(knownControllerId, knownKey1).get();
        final TargetMetadata metaKey2 = targetManagement.getMetaDataByControllerId(knownControllerId, knownKey2).get();

        assertThat(metaKey1.getValue()).isEqualTo(knownValue1);
        assertThat(metaKey2.getValue()).isEqualTo(knownValue2);

        // verify quota enforcement
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerTarget();

        final JSONArray metaData2 = new JSONArray();
        for (int i = 0; i < maxMetaData - metaData1.length() + 1; ++i) {
            metaData2.put(new JSONObject().put("key", knownKey1 + i).put("value", knownValue1 + i));
        }

        mvc.perform(post("/rest/v1/targets/{targetId}/metadata", knownControllerId).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(metaData2.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());

        // verify that the number of meta data entries has not changed
        // (we cannot use the PAGE constant here as it tries to sort by ID)
        assertThat(targetManagement.findMetaDataByControllerId(PageRequest.of(0, Integer.MAX_VALUE), knownControllerId)
                .getTotalElements()).isEqualTo(metaData1.length());

    }

    @Test
    @Description("Ensures that a metadata update through API is reflected by the repository.")
    void updateMetadata() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";

        // prepare and create metadata for update
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final String updateValue = "valueForUpdate";

        setupTargetWithMetadata(knownControllerId, knownKey, knownValue);

        final JSONObject jsonObject = new JSONObject().put("key", knownKey).put("value", updateValue);

        mvc.perform(put("/rest/v1/targets/{targetId}/metadata/{key}", knownControllerId, knownKey)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                .content(jsonObject.toString())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("key", equalTo(knownKey))).andExpect(jsonPath("value", equalTo(updateValue)));

        final TargetMetadata updatedTargetMetadata = targetManagement
                .getMetaDataByControllerId(knownControllerId, knownKey).get();
        assertThat(updatedTargetMetadata.getValue()).isEqualTo(updateValue);

    }

    private void setupTargetWithMetadata(final String knownControllerId, final String knownKey,
            final String knownValue) {
        testdataFactory.createTarget(knownControllerId);
        targetManagement.createMetaData(knownControllerId,
                Collections.singletonList(entityFactory.generateTargetMetadata(knownKey, knownValue)));
    }

    @Test
    @Description("Ensures that a metadata entry deletion through API is reflected by the repository.")
    void deleteMetadata() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";

        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        setupTargetWithMetadata(knownControllerId, knownKey, knownValue);

        mvc.perform(delete("/rest/v1/targets/{targetId}/metadata/{key}", knownControllerId, knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(targetManagement.getMetaDataByControllerId(knownControllerId, knownKey)).isNotPresent();
    }

    @Test
    @Description("Ensures that target metadata deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteMetadataThatDoesNotExistLeadsToNotFound() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";

        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        setupTargetWithMetadata(knownControllerId, knownKey, knownValue);

        mvc.perform(delete("/rest/v1/targets/{targetId}/metadata/XXX", knownControllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/targets/1234/metadata/{key}", knownKey)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        assertThat(targetManagement.getMetaDataByControllerId(knownControllerId, knownKey)).isPresent();
    }

    @Test
    @Description("Ensures that a metadata entry selection through API reflects the repository content.")
    void getMetadataKey() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";

        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        setupTargetWithMetadata(knownControllerId, knownKey, knownValue);

        mvc.perform(get("/rest/v1/targets/{targetId}/metadata/{key}", knownControllerId, knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("key", equalTo(knownKey))).andExpect(jsonPath("value", equalTo(knownValue)));
    }

    @Test
    @Description("Get a paged list of meta data for a target with standard page size.")
    public void getMetadata() throws Exception {
        final int totalMetadata = 4;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final Target testTarget = testdataFactory.createTarget("targetId");
        for (int index = 0; index < totalMetadata; index++) {
            targetManagement.createMetaData(testTarget.getControllerId(), List.of(
                    entityFactory.generateTargetMetadata(knownKeyPrefix + index, knownValuePrefix + index)));
        }

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata", testTarget.getControllerId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON));
    }

    @Test
    @Description("Ensures that a metadata entry paged list selection through API reflectes the repository content.")
    void getPagedListOfMetadata() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";

        final int totalMetadata = 10;
        final int limitParam = 5;
        final String offsetParam = "0";
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";

        setupTargetWithMetadata(knownControllerId, knownKeyPrefix, knownValuePrefix, totalMetadata);

        mvc.perform(get("/rest/v1/targets/{targetId}/metadata?offset=" + offsetParam + "&limit=" + limitParam,
                knownControllerId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(limitParam))).andExpect(jsonPath("total", equalTo(totalMetadata)))
                .andExpect(jsonPath("content[0].key", equalTo("knownKey0")))
                .andExpect(jsonPath("content[0].value", equalTo("knownValue0")));

    }

    private void setupTargetWithMetadata(final String knownControllerId, final String knownKeyPrefix,
            final String knownValuePrefix, final int totalMetadata) {
        testdataFactory.createTarget(knownControllerId);

        final List<MetaData> targetMetadataEntries = new LinkedList<>();
        for (int index = 0; index < totalMetadata; index++) {
            targetMetadataEntries
                    .add(entityFactory.generateTargetMetadata(knownKeyPrefix + index, knownValuePrefix + index));
        }
        targetManagement.createMetaData(knownControllerId, targetMetadataEntries);
    }

    @Test
    @Description("Ensures that a target metadata filtered query with value==knownValue1 parameter returns only the metadata entries with that value.")
    void searchDistributionSetMetadataRsql() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";

        final int totalMetadata = 10;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";

        setupTargetWithMetadata(knownControllerId, knownKeyPrefix, knownValuePrefix, totalMetadata);

        final String rsqlSearchValue1 = "value==knownValue1";

        mvc.perform(get("/rest/v1/targets/{targetId}/metadata?q=" + rsqlSearchValue1, knownControllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("content[0].key", equalTo("knownKey1")))
                .andExpect(jsonPath("content[0].value", equalTo("knownValue1")));
    }

    @Test
    @Description("A request for assigning multiple DS to a target results in a Bad Request when multiassignment in disabled.")
    void multiassignmentRequestNotAllowedIfDisabled() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final List<Long> dsIds = testdataFactory.createDistributionSets(2).stream().map(DistributionSet::getId)
                .collect(Collectors.toList());

        final JSONArray body = new JSONArray();
        dsIds.forEach(id -> body.put(getAssignmentObject(id, MgmtActionType.FORCED, 67)));

        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(body.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Passing an array in assignment request is allowed if multiassignment is disabled and array size in 1.")
    void multiassignmentRequestAllowedIfDisabledButHasSizeOne() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONArray body = new JSONArray().put(getAssignmentObject(dsId, MgmtActionType.FORCED));
        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(body.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Identical assignments in a single request are removed when multiassignment in disabled.")
    void identicalAssignmentInRequestAreRemovedIfMultiassignmentsDisabled() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONObject assignment = getAssignmentObject(dsId, MgmtActionType.FORCED);
        final JSONArray body = new JSONArray().put(assignment).put(assignment);

        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(body.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)));
    }

    @Test
    @Description("Assign multiple DSs to a target in one request with multiassignments enabled.")
    void multiAssignment() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final List<Long> dsIds = testdataFactory.createDistributionSets(2).stream().map(DistributionSet::getId)
                .collect(Collectors.toList());

        final JSONArray body = new JSONArray();
        dsIds.forEach(id -> body.put(getAssignmentObject(id, MgmtActionType.FORCED, 76)));

        enableMultiAssignments();
        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(body.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(2)));
    }

    @Test
    @Description("An assignment request containing a weight is only accepted when weight is valid and multi assignment is on.")
    void weightValidation() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int weight = 98;

        final JSONObject bodyValid = getAssignmentObject(dsId, MgmtActionType.FORCED, weight);
        final JSONObject bodyInvalid = getAssignmentObject(dsId, MgmtActionType.FORCED, Action.WEIGHT_MIN - 1);

        enableMultiAssignments();
        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(bodyInvalid.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.repo.constraintViolation")));
        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(bodyValid.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).get().collect(Collectors.toList());
        assertThat(actions).size().isEqualTo(1);
        assertThat(actions.get(0).getWeight()).get().isEqualTo(weight);
    }

    @Test
    @Description("An assignment request containing a valid weight when multi assignment is off.")
    void weightWithSingleAssignment() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONObject bodyValid = getAssignmentObject(dsId, MgmtActionType.FORCED, 98);

        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(bodyValid.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("An assignment request containing a valid weight when multi assignment is on.")
    void weightWithMultiAssignment() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int weight = 98;

        final JSONObject bodyValid = getAssignmentObject(dsId, MgmtActionType.FORCED, weight);

        enableMultiAssignments();
        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(bodyValid.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).get().collect(Collectors.toList());
        assertThat(actions).size().isEqualTo(1);
        assertThat(actions.get(0).getWeight()).get().isEqualTo(weight);
    }

    @Test
    @Description("Get weight of action")
    void getActionWeight() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int customWeightHigh = 800;
        final int customWeightLow = 300;
        assignDistributionSet(dsId, targetId); // default weight 1000
        enableMultiAssignments();
        assignDistributionSet(dsId, targetId, customWeightHigh);
        assignDistributionSet(dsId, targetId, customWeightLow);

        mvc.perform(get("/rest/v1/targets/{targetId}/actions", targetId)
                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "WEIGHT:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].weight", equalTo(customWeightLow)))
                .andExpect(jsonPath("content.[1].weight", equalTo(customWeightHigh)))
                .andExpect(jsonPath("content.[2].weight", equalTo(1000)));
    }

    @Test
    @Description("An action provides information of the rollout it was created for (if any).")
    void getActionWithRolloutInfo() throws Exception {

        // setup
        final int amountTargets = 10;
        final List<Target> targets = testdataFactory.createTargets(amountTargets, "trg", "trg");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Rollout rollout = testdataFactory.createRolloutByVariables("My Rollout", "My Rollout Description", 1,
                "name==trg*", ds, "50", "5");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // get all actions for the first target
        final Target target = targets.get(0);
        mvc.perform(get("/rest/v1/targets/{targetId}/actions", target.getControllerId())).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath("content.[0].rollout", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("content.[0].rolloutName", equalTo(rollout.getName())));

        // get the first action for the first target;
        // verify that also the rollout link is present
        final Slice<Action> action = deploymentManagement.findActionsByTarget(target.getControllerId(),
                PageRequest.of(0, 100));
        assertThat(action.getContent()).hasSize(1);
        mvc.perform(get("/rest/v1/targets/{targetId}/actions/{actionId}", target.getControllerId(),
                action.getContent().get(0).getId())).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath("$.rollout", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("$.rolloutName", equalTo(rollout.getName()))).andExpect(jsonPath(
                        "$._links.rollout.href", containsString("/rest/v1/rollouts/" + rollout.getId().intValue())));
    }

    @Test
    @Description("Ensures that a post request for creating targets with target type works.")
    void createTargetsWithTargetType() throws Exception {
        final TargetType type1 = testdataFactory.createTargetType("typeWithDs",
                Collections.singletonList(standardDsType));
        final TargetType type2 = testdataFactory.createTargetType("typeWithOutDs",
                Collections.singletonList(standardDsType));

        final Target test1 = entityFactory.target().create().controllerId("id1").name("targetWithoutType")
                .securityToken("token").address("amqp://test123/foobar").description("testid1").build();
        final Target test2 = entityFactory.target().create().controllerId("id2").name("targetOfType1")
                .targetType(type1.getId()).description("testid2").build();
        final Target test3 = entityFactory.target().create().controllerId("id3").name("targetOfType2")
                .targetType(type2.getId()).description("testid3").build();
        final String hrefType1 = "http://localhost/rest/v1/targettypes/" + type1.getId();

        final List<Target> targets = Arrays.asList(test1, test2, test3);

        final MvcResult mvcPostResult = mvc
                .perform(post("/rest/v1/targets").content(JsonBuilder.targets(targets, true))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo("targetWithoutType")))
                .andExpect(jsonPath("[0].controllerId", equalTo("id1")))
                .andExpect(jsonPath("[0].description", equalTo("testid1")))
                .andExpect(jsonPath("[0].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[0].securityToken", equalTo("token")))
                .andExpect(jsonPath("[0].address", equalTo("amqp://test123/foobar")))
                .andExpect(jsonPath("[0].targetType").doesNotExist())
                .andExpect(jsonPath("[1].name", equalTo("targetOfType1")))
                .andExpect(jsonPath("[1].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[1].controllerId", equalTo("id2")))
                .andExpect(jsonPath("[1].description", equalTo("testid2")))
                .andExpect(jsonPath("[1].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[1].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[1].targetType", equalTo(type1.getId().intValue())))
                .andExpect(jsonPath("[2].name", equalTo("targetOfType2")))
                .andExpect(jsonPath("[2].controllerId", equalTo("id3")))
                .andExpect(jsonPath("[2].description", equalTo("testid3")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[2].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[2].targetType", equalTo(type2.getId().intValue()))).andReturn();

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + test2.getControllerId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("targetOfType1")))
                .andExpect(jsonPath(JSON_PATH_CONTROLLERID, equalTo("id2")))
                .andExpect(jsonPath(JSON_PATH_TYPE, equalTo(type1.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo("testid2")))
                .andExpect(jsonPath("$._links.targetType.href", equalTo(hrefType1))).andReturn();

        final Target target1 = assertTarget("id1", "targetWithoutType", "testid1");
        assertThat(target1.getTargetType()).isNull();
        assertThat(target1.getSecurityToken()).isEqualTo("token");
        assertThat(target1.getAddress()).hasToString("amqp://test123/foobar");

        final Target target2 = assertTarget("id2", "targetOfType1", "testid2");
        assertThat(target2.getTargetType()).extracting(TargetType::getName).isEqualTo("typeWithDs");

        final Target target3 = assertTarget("id3", "targetOfType2", "testid3");
        assertThat(target3.getTargetType()).extracting(TargetType::getName).isEqualTo("typeWithOutDs");
    }

    @Test
    @Description("Ensures that a post request for creating target with target type works.")
    void createTargetWithExistingTargetType() throws Exception {
        // create target type
        final List<TargetType> targetTypes = testdataFactory.createTargetTypes("targettype", 1);
        assertThat(targetTypes).hasSize(1);

        final Target target = entityFactory.target().create().controllerId("targetcontroller").name("testtarget")
                .targetType(targetTypes.get(0).getId()).build();

        final String targetList = JsonBuilder.targets(Collections.singletonList(target), false);

        // test query target over rest resource
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING).content(targetList)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated())
                .andExpect(jsonPath("[0].controllerId", equalTo("targetcontroller")))
                .andExpect(jsonPath("[0].targetType", equalTo(targetTypes.get(0).getId().intValue())));

        assertThat(targetManagement.getByControllerID("targetcontroller").get().getTargetType().getId())
                .isEqualTo(targetTypes.get(0).getId());
    }

    @Test
    @Description("Ensures that a put request for updating targets with target type works.")
    void updateTargetTypeInTarget() throws Exception {
        // create target type
        final List<TargetType> targetTypes = testdataFactory.createTargetTypes("targettype", 2);
        assertThat(targetTypes).hasSize(2);

        final String controllerId = "targetcontroller";
        final Target target = testdataFactory.createTarget(controllerId, "testtarget", targetTypes.get(0).getId());

        assertThat(target).isNotNull();
        assertThat(target.getTargetType().getId()).isEqualTo(targetTypes.get(0).getId());

        // update target over rest resource
        final String body = new JSONObject().put("targetType", targetTypes.get(1).getId().intValue()).toString();

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + controllerId).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("controllerId", equalTo(controllerId)))
                .andExpect(jsonPath("targetType", equalTo(targetTypes.get(1).getId().intValue())));
    }

    @Test
    @Description("Ensures that a post request for creating targets with unknown target type fails.")
    void addingNonExistingTargetTypeInTargetShouldFail() throws Exception {
        final long unknownTargetTypeId = 999;
        final String errorMsg = String.format("TargetType with given identifier {%s} does not exist.",
                unknownTargetTypeId);

        final Optional<TargetType> targetType = targetTypeManagement.get(unknownTargetTypeId);
        assertThat(targetType).isNotPresent();

        final String controllerId = "targetcontroller";
        final Target target = entityFactory.target().create().controllerId(controllerId).name("testtarget").build();

        final String targetList = JsonBuilder.targets(Collections.singletonList(target), false, unknownTargetTypeId);

        // post target over rest resource
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING).content(targetList)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
                .andExpect(jsonPath("message", Matchers.containsString(errorMsg)));
    }

    @Test
    @Description("Ensures that a post request for assign target type to target works.")
    void assignTargetTypeToTarget() throws Exception {
        // create target type
        final TargetType targetType = testdataFactory.findOrCreateTargetType("targettype");
        assertThat(targetType).isNotNull();

        // create target
        final String targetControllerId = "targetcontroller";
        final Target target = testdataFactory.createTarget(targetControllerId, "testtarget");
        assertThat(target).isNotNull();

        // assign target type over rest resource
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetControllerId + "/targettype")
                .content("{\"id\":" + targetType.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(targetManagement.getByControllerID(targetControllerId).get().getTargetType().getId())
                .isEqualTo(targetType.getId());
    }

    @Test
    @Description("Ensures that a post request for assign a invalid target type to target fails.")
    void assignInvalidTargetTypeToTargetFails() throws Exception {
        // Invalid target type ID
        final long invalidTargetTypeId = 999;

        // create target
        final String targetControllerId = "targetcontroller";
        final Target target = testdataFactory.createTarget(targetControllerId, "testtarget");
        assertThat(target).isNotNull();

        // assign invalid target type over rest resource
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetControllerId + "/targettype")
                .content("{\"id\":" + invalidTargetTypeId + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // verify response json exception message if body does not include id
        // field
        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetControllerId + "/targettype")
                        .content("{\"unknownfield\":" + invalidTargetTypeId + "}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(ConstraintViolationException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getKey());
        assertThat(exceptionInfo.getMessage()).contains("targetTypeId");
    }

    @Test
    @Description("Ensures that a delete request for unassign target type from target works.")
    void unassignTargetTypeFromTarget() throws Exception {
        // create target type
        final List<TargetType> targetTypes = testdataFactory.createTargetTypes("targettype", 1);
        assertThat(targetTypes).hasSize(1);

        final String targetControllerId = "targetcontroller";
        final Target target = testdataFactory.createTarget(targetControllerId, "testtarget",
                targetTypes.get(0).getId());

        assertThat(target).isNotNull();
        assertThat(target.getTargetType().getId()).isEqualTo(targetTypes.get(0).getId());

        // unassign target type over rest resource
        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/" + targetControllerId + "/targettype")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

        assertThat(targetManagement.getByControllerID(targetControllerId).get().getTargetType()).isNull();
    }

    @Test
    void invalidRequestsOnTargetTypeResource() throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTarget(knownTargetId);
        testdataFactory.createTargetType("targettype", Collections.emptyList());

        // GET is not allowed
        mvc.perform(get(
                MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + MgmtRestConstants.TARGET_TARGET_TYPE_V1_REQUEST_MAPPING,
                knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // PUT is not allowed
        mvc.perform(put(
                MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + MgmtRestConstants.TARGET_TARGET_TYPE_V1_REQUEST_MAPPING,
                knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // POST does not exist with path parameter targettype
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING
                + MgmtRestConstants.TARGET_TARGET_TYPE_V1_REQUEST_MAPPING + "/123", knownTargetId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // DELETE does not exist with path parameter targettype
        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING
                + MgmtRestConstants.TARGET_TARGET_TYPE_V1_REQUEST_MAPPING + "/123", knownTargetId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // Invalid content
        mvc.perform(post(
                MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + MgmtRestConstants.TARGET_TARGET_TYPE_V1_REQUEST_MAPPING,
                knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isUnsupportedMediaType());

        // Bad request if id field is missing
        mvc.perform(post(
                MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + MgmtRestConstants.TARGET_TARGET_TYPE_V1_REQUEST_MAPPING,
                knownTargetId).content("{\"unknownfield\":123}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("possibleActiveStates")
    void getAutoConfirmActive(final String initiator, final String remark) throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTarget(knownTargetId);
        confirmationManagement.activateAutoConfirmation(knownTargetId, initiator, remark);

        // GET with all possible responses
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/" + TARGET_V1_AUTO_CONFIRM,
                knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("active", equalTo(Boolean.TRUE)))
                .andExpect(initiator == null ? jsonPath("initiator").doesNotExist()
                        : jsonPath("initiator", equalTo(initiator)))
                .andExpect(remark == null ? jsonPath("remark").doesNotExist() : jsonPath("remark", equalTo(remark)))
                .andExpect(jsonPath("_links.deactivate").exists())
                .andExpect(jsonPath("_links.activate").doesNotExist());
    }

    @Test
    void getAutoConfirmStateFromTargetsEndpoint() throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTarget(knownTargetId);

        // GET if active
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("content.[0].autoConfirmActive").doesNotExist());

        enableConfirmationFlow();

        // GET if not active
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].autoConfirmActive", equalTo(Boolean.FALSE)));

        confirmationManagement.activateAutoConfirmation(knownTargetId, "test", "remark");

        // GET if active
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("content.[0].autoConfirmActive", equalTo(Boolean.TRUE)));
    }

    @Test
    void getAutoConfirmNotActive() throws Exception {
        final String knownTargetId = "targetId";

        // GET for not existing target
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/" + TARGET_V1_AUTO_CONFIRM,
                knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        testdataFactory.createTarget(knownTargetId);

        // GET for auto-confirm not active
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/" + TARGET_V1_AUTO_CONFIRM,
                knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("active", equalTo(Boolean.FALSE))).andExpect(jsonPath("initiator").doesNotExist())
                .andExpect(jsonPath("remark").doesNotExist()).andExpect(jsonPath("_links.activate").exists());
    }

    @Test
    void autoConfirmStateReferenceOnTarget() throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTarget(knownTargetId);

        // GET with confirmation flow not active should not expose
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", knownTargetId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirmActive").doesNotExist())
                .andExpect(jsonPath("_links.autoConfirm").doesNotExist());

        enableConfirmationFlow();

        // GET with confirmation flow active should expose
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", knownTargetId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirmActive").exists()).andExpect(jsonPath("_links.autoConfirm").exists());
    }

    @Test
    @Description("Handles the POST request to activate auto-confirm on a target. Payload can be provided to specify more details about the operation.")
    public void postActivateAutoConfirm() throws Exception {
        final Target testTarget = testdataFactory.createTarget("targetId");

        final MgmtTargetAutoConfirmUpdate body = new MgmtTargetAutoConfirmUpdate("custom_initiator_value",
                "custom_remark_value");

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/" + TARGET_V1_AUTO_CONFIRM + "/"
                        + TARGET_V1_ACTIVATE_AUTO_CONFIRM, testTarget.getControllerId())
                        .content(objectMapper.writeValueAsString(body)).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Handles the POST request to deactivate auto-confirm on a target.")
    public void postDeactivateAutoConfirm() throws Exception {
        final Target testTarget = testdataFactory.createTarget("targetId");
        confirmationManagement.activateAutoConfirmation(testTarget.getControllerId(), null, null);

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/" + TARGET_V1_AUTO_CONFIRM + "/"
                        + TARGET_V1_DEACTIVATE_AUTO_CONFIRM, testTarget.getControllerId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Verifies that the status code that was reported in the last action status update is correctly exposed via the action.")
    void lastActionStatusCode() throws Exception {

        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Target target = testdataFactory.createTarget("knownTargetId");
        final Action action = getFirstAssignedAction(assignDistributionSet(dsA, Collections.singletonList(target)));

        // no status update yet -> no status code
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}",
                target.getControllerId(), action.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("lastStatusCode").doesNotExist());

        // update action status with status code
        updateActionStatus(action, Status.RUNNING, 100);
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}",
                target.getControllerId(), action.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("lastStatusCode", equalTo(100)))
                .andExpect(jsonPath("detailStatus", equalTo("running")));

        // update action status without a status code
        updateActionStatus(action, Status.RUNNING, null);
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}",
                target.getControllerId(), action.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("lastStatusCode").doesNotExist())
                .andExpect(jsonPath("detailStatus", equalTo("running")));

        // update action status with status code
        updateActionStatus(action, Status.ERROR, 432);
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}",
                target.getControllerId(), action.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("lastStatusCode", equalTo(432)))
                .andExpect(jsonPath("detailStatus", equalTo("error")));
    }

    private Action updateActionStatus(final Action action, final Status status, final Integer statusCode) {
        return updateActionStatus(action, status, statusCode, null);
    }

    private Action updateActionStatus(final Action action, final Status status, final Integer statusCode,
            final String message) {

        assertThat(action).isNotNull();
        assertThat(status).isNotNull();

        final ActionStatusCreate actionStatus = entityFactory.actionStatus().create(action.getId());
        actionStatus.status(status);
        if (statusCode != null) {
            actionStatus.code(statusCode);
        }
        if (message != null) {
            actionStatus.message(message);
        }

        return controllerManagement.addUpdateActionStatus(actionStatus);
    }

    private static Stream<Arguments> possibleActiveStates() {
        return Stream.of(Arguments.of("someInitiator", "someRemark"), Arguments.of(null, "someRemark"),
                Arguments.of("someInitiator", null), Arguments.of(null, null));
    }
}

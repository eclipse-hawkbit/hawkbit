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
import static org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget.AUTO_CONFIRM;
import static org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAutoConfirm.ACTIVATE;
import static org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAutoConfirm.DEACTIVATE;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi.TARGETS_V1;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionConfirmationRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAutoConfirmUpdate;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.util.ResourceUtility;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.TargetManagement.Create;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.qfields.ActionFields;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.exception.MessageNotReadableException;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.utils.IpUtil;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Spring MVC Tests against the MgmtTargetResource.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Target Resource
 */
class MgmtTargetResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String TARGET_DESCRIPTION_TEST = "created in test";
    private static final String JSON_PATH_ROOT = "$";
    // fields, attributes
    private static final String JSON_PATH_FIELD_ID = ".id";
    private static final String JSON_PATH_FIELD_CONTROLLERID = ".controllerId";
    private static final String JSON_PATH_FIELD_NAME = ".name";
    private static final String JSON_PATH_FIELD_DESCRIPTION = ".description";
    private static final String JSON_PATH_FIELD_CONTENT = ".content";

    private static final String JSON_PATH_KEY = "key";
    private static final String JSON_PATH_VALUE = "value";
    // target
    // $.field
    static final String JSON_PATH_PAGED_LIST_CONTENT = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTENT;
    private static final String JSON_PATH_FIELD_SIZE = ".size";
    static final String JSON_PATH_PAGED_LIST_SIZE = JSON_PATH_ROOT + JSON_PATH_FIELD_SIZE;
    private static final String JSON_PATH_FIELD_TOTAL = ".total";
    static final String JSON_PATH_PAGED_LIST_TOTAL = JSON_PATH_ROOT + JSON_PATH_FIELD_TOTAL;
    private static final String JSON_PATH_FIELD_LAST_REQUEST_AT = ".lastControllerRequestAt";
    private static final String JSON_PATH_FIELD_TARGET_TYPE = ".targetType";
    private static final String JSON_PATH_NAME = JSON_PATH_ROOT + JSON_PATH_FIELD_NAME;
    private static final String JSON_PATH_ID = JSON_PATH_ROOT + JSON_PATH_FIELD_ID;
    private static final String JSON_PATH_CONTROLLERID = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTROLLERID;
    private static final String JSON_PATH_DESCRIPTION = JSON_PATH_ROOT + JSON_PATH_FIELD_DESCRIPTION;
    private static final String JSON_PATH_LAST_REQUEST_AT = JSON_PATH_ROOT + JSON_PATH_FIELD_LAST_REQUEST_AT;
    private static final String JSON_PATH_TYPE = JSON_PATH_ROOT + JSON_PATH_FIELD_TARGET_TYPE;

    private static final String KNOWN_KEY = "knownKey";
    private static final String KNOWN_VALUE = "knownValue";

    @Autowired
    ActionRepository actionRepository;
    @Autowired
    private JpaProperties jpaProperties;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Ensures that when targetType value of -1 is provided the target type is unassigned from the target.
     */
    @Test
    void updateTargetAndUnassignTargetType() throws Exception {
        final String knownControllerId = "123";
        final String knownNewAddress = "amqp://test123/foobar";
        final String knownNameNotModify = "controllerName";
        final Long unassignTargetTypeValue = -1L;

        final TargetType targetType = targetTypeManagement.create(
                TargetTypeManagement.Create.builder().name("targettype1").description("targettypedes1").build());

        final String body = new JSONObject().put("targetType", unassignTargetTypeValue).toString();

        // create a target with the created TargetType
        targetManagement.create(Create.builder().controllerId(knownControllerId).name(knownNameNotModify)
                .address(knownNewAddress).targetType(targetType).build());

        mvc.perform(get(TARGETS_V1 + "/" + knownControllerId).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.address", equalTo(knownNewAddress)))
                .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)))
                .andExpect(jsonPath("$.targetType").exists());

        mvc.perform(put(TARGETS_V1 + "/" + knownControllerId).content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.address", equalTo(knownNewAddress)))
                .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)))
                .andExpect(jsonPath("$.targetType").doesNotExist());

    }

    /**
     * Ensures that when targetType value of -1 is provided the target type is unassigned from the target when updating multiple fields in target object.
     */
    @Test
    void updateTargetNameAndUnassignTargetType() throws Exception {
        final String knownControllerId = "123";
        final String knownNewAddress = "amqp://test123/foobar";
        final String knownNameNotModify = "controllerName";
        final Long unassignTargetTypeValue = -1L;
        final String controllerNewName = "controllerNewName";

        final TargetType targetType = targetTypeManagement.create(
                TargetTypeManagement.Create.builder().name("targettype1").description("targettypedes1").build());

        final String body = new JSONObject()
                .put("targetType", unassignTargetTypeValue).put("name", "controllerNewName")
                .toString();

        // create a target with the created TargetType
        targetManagement.create(Create.builder().controllerId(knownControllerId).name(knownNameNotModify)
                .address(knownNewAddress).targetType(targetType).build());

        mvc.perform(get(TARGETS_V1 + "/" + knownControllerId).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.address", equalTo(knownNewAddress)))
                .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)))
                .andExpect(jsonPath("$.targetType").exists());

        //check if controller name is updated AND target type is missing (not assigned)
        mvc.perform(put(TARGETS_V1 + "/" + knownControllerId).content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.address", equalTo(knownNewAddress)))
                .andExpect(jsonPath("$.name", equalTo(controllerNewName)))
                .andExpect(jsonPath("$.targetType").doesNotExist());
    }

    /**
     * Handles the GET request of retrieving all targets within SP..
     */
    @Test
    void getTargets() throws Exception {
        enableConfirmationFlow();
        mvc.perform(get(TARGETS_V1)).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print());
    }

    /**
     * Handles the GET request of retrieving all targets within SP based by parameter.
     */
    @Test
    void getTargetsWithParameters() throws Exception {
        mvc.perform(get(TARGETS_V1 + "?limit=10&sort=name:ASC&offset=0&q=name==a"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print());
    }

    /**
     * Handles the POST request to activate auto-confirm on a target. Payload can be provided to specify more details about the operation.
     */
    @Test
    void postActivateAutoConfirm() throws Exception {
        final Target testTarget = testdataFactory.createTarget("targetId");

        final MgmtTargetAutoConfirmUpdate body = new MgmtTargetAutoConfirmUpdate("custom_initiator_value", "custom_remark_value");
        mvc.perform(post(TARGETS_V1 + "/{targetId}/" + AUTO_CONFIRM + "/" + ACTIVATE,
                        testTarget.getControllerId())
                        .content(objectMapper.writeValueAsString(body)).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
    }

    /**
     * Handles the POST request to deactivate auto-confirm on a target.
     */
    @Test
    void postDeactivateAutoConfirm() throws Exception {
        final Target testTarget = testdataFactory.createTarget("targetId");
        confirmationManagement.activateAutoConfirmation(testTarget.getControllerId(), null, null);

        mvc.perform(post(TARGETS_V1 + "/{targetId}/" + AUTO_CONFIRM + "/" + DEACTIVATE,
                        testTarget.getControllerId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
    }

    /**
     * Test confirmation of single Action with confirm status. Check that Action goes into Running status with appropriate messages and status code
     */
    @Test
    void updateActionConfirmationWithConfirm() throws Exception {
        final int expectedStatusCode = 210;
        final String expectedStatusMessage1 = "some-custom-message1";
        final String expectedStatusMessage2 = "some-custom-message2";
        final Status expectedStatusAfterActionConfirmationCall = Status.RUNNING;
        final long actionId = doAssignmentAndTestConfirmation("targetId");
        testActionConfirmation("targetId", actionId, MgmtActionConfirmationRequestBodyPut.Confirmation.CONFIRMED, expectedStatusCode,
                new String[] { expectedStatusMessage1, expectedStatusMessage2 }, HttpStatus.NO_CONTENT, expectedStatusAfterActionConfirmationCall);
    }

    /**
     * Test confirmation of single Action with deny status. Check that Action stays in WAIT_FOR_CONFIRMATION status with appropriate messages and status code
     */
    @Test
    void updateActionConfirmationWithDeny() throws Exception {
        final int expectedStatusCode = 410;
        final String expectedStatusMessage1 = "some-error-custom-message1";
        final String expectedStatusMessage2 = "some-error-custom-message2";
        final Status expectedStatusAfterActionConfirmationCall = Status.WAIT_FOR_CONFIRMATION;
        final long actionId = doAssignmentAndTestConfirmation("targetId");
        testActionConfirmation("targetId", actionId, MgmtActionConfirmationRequestBodyPut.Confirmation.DENIED, expectedStatusCode,
                new String[] { expectedStatusMessage1, expectedStatusMessage2 }, HttpStatus.NO_CONTENT, expectedStatusAfterActionConfirmationCall);
    }

    /**
     * Test confirmation of single Action with wrong ControllerId - e.g. the given Action is not assigned to the given Target - confirmation call must fail.
     */
    @Test
    void updateActionConfirmationFailsIfActionNotAssignedToTarget() throws Exception {
        final int payloadCallCode = 200;
        final String payloadCallMessage1 = "random1";
        final String payloadCallMessage2 = "random2";
        final long controller1Action = doAssignmentAndTestConfirmation("controller1");
        final long controller2Action = doAssignmentAndTestConfirmation("controller2");
        // test that target id and action id are checked correctly and only actions assigned to given targets are confirmed/denied
        // if action is not assigned to the target, confirmation call must fail
        testActionConfirmation("controller1", controller2Action, MgmtActionConfirmationRequestBodyPut.Confirmation.CONFIRMED,
                payloadCallCode, new String[] { payloadCallMessage1, payloadCallMessage2 }, HttpStatus.NOT_FOUND,
                Status.WAIT_FOR_CONFIRMATION);
        testActionConfirmation("controller2", controller1Action, MgmtActionConfirmationRequestBodyPut.Confirmation.CONFIRMED,
                payloadCallCode, new String[] { payloadCallMessage1, payloadCallMessage2 }, HttpStatus.NOT_FOUND,
                Status.WAIT_FOR_CONFIRMATION);
    }

    long doAssignmentAndTestConfirmation(final String controllerId) {
        enableConfirmationFlow();

        final Target testTarget = testdataFactory.createTarget(controllerId);
        final DistributionSet dsA = testdataFactory.createDistributionSet(controllerId);
        assignDistributionSet(dsA, Collections.singletonList(testTarget));

        // check initial status after assignment is done with Confirmation Flow enabled
        // expected Actions to be in WAIT_FOR_CONFIRMATION status
        List<Action> actionHistory = deploymentManagement.findActionsByTarget(controllerId, PAGE).getContent();
        assertThat(actionHistory).hasSize(1);
        Action action = actionHistory.get(0);
        assertThat(action.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
        return action.getId();
    }

    void testActionConfirmation(final String controllerId, final long actionId,
            final MgmtActionConfirmationRequestBodyPut.Confirmation payloadConfirmation, final int payloadCode, final String[] payloadMessages,
            final HttpStatus expectedHttpResponseStatus, final Status expectedGeneratedStatus) throws Exception {
        String url = TARGETS_V1 + "/" + controllerId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId + "/confirmation";
        mvc.perform(put(url).content(String.format("{\"confirmation\":\"%s\",\"details\":[\"%s\",\"%s\"],\"code\":%d}",
                                payloadConfirmation.getName(),
                                payloadMessages[0],
                                payloadMessages[1],
                                payloadCode
                        ))
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is(expectedHttpResponseStatus.value()));

        // check status after confirmation is done (either confirmed or denied)
        final List<Action> actionHistory = deploymentManagement.findActionsByTarget(controllerId, PAGE).getContent();
        assertThat(actionHistory).hasSize(1);
        final JpaAction jpaAction = (JpaAction) actionHistory.get(0);
        final List<ActionStatus> actionStatuses = new ArrayList<>(jpaAction.getActionStatus());

        // confirmation call was successful, check if Action status ,status code and messages are updated appropriately
        if (expectedHttpResponseStatus == HttpStatus.NO_CONTENT) {
            assertThat(jpaAction.getStatus()).isEqualTo(expectedGeneratedStatus);
            assertThat(jpaAction.getLastActionStatusCode()).hasValue(payloadCode);

            actionStatuses.sort(Comparator.comparingLong(Identifiable::getId));
            assertThat(actionStatuses).hasSize(2);
            assertThat((actionStatuses.get(0)).getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
            assertThat((actionStatuses.get(0)).getCode()).isEmpty();
            assertThat((actionStatuses.get(1)).getStatus()).isEqualTo(expectedGeneratedStatus);
            assertThat((actionStatuses.get(1)).getCode()).hasValue(payloadCode);
            assertThat(((JpaActionStatus) actionStatuses.get(1)).getMessages()).contains(payloadMessages[0], payloadMessages[1]);
        } else { // confirmation call not successful, check if Action status is not updated, no new Action status added as well.
            assertThat(jpaAction.getStatus()).isEqualTo(Status.WAIT_FOR_CONFIRMATION);
            assertThat(jpaAction.getLastActionStatusCode()).isEmpty();
            assertThat(jpaAction.getActionStatus()).hasSize(1);
        }
    }

    /**
     * Ensures that actions list is in expected order.
     */
    @Test
    void getActionStatusReturnsCorrectType() throws Exception {
        final int limitSize = 2;
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        assertThat(actions).hasSize(2);
        updateActionStatus(actions.get(0), Status.FINISHED, null, "test");

        final PageRequest pageRequest = PageRequest.of(0, 1000, Direction.ASC, ActionFields.ID.getName());
        final Action action = deploymentManagement.findActionsByTarget(knownTargetId, pageRequest).getContent().get(0);

        final ActionStatus status = deploymentManagement.findActionStatusByAction(action.getId(), PAGE).getContent()
                .stream().sorted((e1, e2) -> Long.compare(e2.getId(), e1.getId())).toList().get(0);

        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS +
                        "/" + actions.get(0).getId() + "/status")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:DESC"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(3)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                .andExpect(jsonPath("content.[0].id", equalTo(status.getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("finished")))
                .andExpect(jsonPath("content.[0].messages", hasSize(1)))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(status.getCreatedAt())))
                .andExpect(jsonPath("content.[1].type", equalTo("canceling")));
    }

    /**
     * Ensures that security token is not returned if user does not have READ_TARGET_SEC_TOKEN permission.
     */
    @Test
    @WithUser(allSpPermissions = false, authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET })
    void securityTokenIsNotInResponseIfMissingPermission() throws Exception {

        final String knownControllerId = "knownControllerId";
        testdataFactory.createTarget(knownControllerId);
        mvc.perform(get(TARGETS_V1 + "/{targetId}", knownControllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("securityToken").doesNotExist());
    }

    /**
     * Ensures that security token is returned if user does have READ_TARGET_SEC_TOKEN permission.
     */
    @Test
    @WithUser(allSpPermissions = false, authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET,
            SpPermission.READ_TARGET_SECURITY_TOKEN })
    void securityTokenIsInResponseWithCorrectPermission() throws Exception {

        final String knownControllerId = "knownControllerId";
        final Target createTarget = testdataFactory.createTarget(knownControllerId);
        mvc.perform(get(TARGETS_V1 + "/{targetId}", knownControllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("securityToken", equalTo(createTarget.getSecurityToken())));
    }

    /**
     * Ensures that that IP address is in result as stored in the repository.
     */
    @Test
    void addressAndIpAddressInTargetResult() throws Exception {
        // prepare targets with IP
        final String knownControllerId1 = "0815";
        final String knownControllerId2 = "4711";

        createTarget(knownControllerId1);
        createTarget(knownControllerId2);

        // test
        mvc.perform(get(TARGETS_V1))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(2)))
                .andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("$.content.[?(@.controllerId=='" + knownControllerId1 + "')].ipAddress", contains("127.0.0.1")))
                .andExpect(jsonPath("$.content.[?(@.controllerId=='" + knownControllerId2 + "')].ipAddress", contains("127.0.0.1")))
                .andExpect(jsonPath("$.content.[?(@.controllerId=='" + knownControllerId1 + "')].address",
                        contains(IpUtil.createHttpUri("127.0.0.1").toString())))
                .andExpect(jsonPath("$.content.[?(@.controllerId=='" + knownControllerId2 + "')].address",
                        contains(IpUtil.createHttpUri("127.0.0.1").toString())));
    }

    /**
     * Ensures that actions history is returned as defined by filter status==pending,status==finished.
     */
    @Test
    void searchActionsRsql() throws Exception {
        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Target createTarget = testdataFactory.createTarget("knownTargetId");

        assignDistributionSet(dsA, Collections.singletonList(createTarget));

        final String rsqlPendingStatus = "active==true";
        final String rsqlFinishedStatus = "active==false";
        final String rsqlPendingOrFinishedStatus = rsqlFinishedStatus + "," + rsqlPendingStatus;
        // pending status one result
        mvc.perform(get(TARGETS_V1 + "/{targetId}/actions?q=" + rsqlPendingStatus, createTarget.getControllerId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].active", equalTo(true)));

        // finished status none result
        mvc.perform(get(TARGETS_V1 + "/{targetId}/actions?q=" + rsqlFinishedStatus,
                        createTarget.getControllerId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("size", equalTo(0)));

        // pending or finished status one result
        mvc.perform(get(
                        TARGETS_V1 + "/{targetId}/actions?q=" + rsqlPendingOrFinishedStatus,
                        createTarget.getControllerId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].active", equalTo(true)));
    }

    /**
     * Ensures that a deletion of an active action results in cancellation triggered.
     */
    @Test
    void cancelActionOK() throws Exception {
        // prepare test
        final Target tA = createTargetAndStartAction();

        // test - cancel the active action
        mvc.perform(delete(TARGETS_V1 + "/{targetId}/actions/{actionId}",
                        tA.getControllerId(), deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE)
                                .getContent().get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        final Action action = deploymentManagement.findAction(
                deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().get(0).getId()).get();
        // still active because in "canceling" state and waiting for controller feedback
        assertThat(action.isActive()).isTrue();

        // action has not been cancelled confirmed from controller, so DS remains assigned until confirmation
        assertThat(deploymentManagement.findAssignedDistributionSet(tA.getControllerId())).isPresent();
        assertThat(deploymentManagement.findInstalledDistributionSet(tA.getControllerId())).isNotPresent();
    }

    /**
     * Ensures that method not allowed is returned if cancellation is triggered on already canceled action.
     */
    @Test
    void cancelAndCancelActionIsNotAllowed() throws Exception {
        // prepare test
        final Target tA = createTargetAndStartAction();

        // cancel the active action
        deploymentManagement.cancelAction(deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().get(0).getId());

        // find the current active action
        final List<Action> cancelActions = deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().stream()
                .filter(Action::isCancelingOrCanceled).toList();
        assertThat(cancelActions).hasSize(1);

        // test - cancel an cancel action returns forbidden
        mvc.perform(delete(TARGETS_V1 + "/{targetId}/actions/{actionId}",
                        tA.getControllerId(), cancelActions.get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Force Quit an Action, which is already canceled. Expected Result is an HTTP response code 204.
     */
    @Test
    void forceQuitAnCanceledActionReturnsOk() throws Exception {
        final Target tA = createTargetAndStartAction();

        // cancel the active action
        deploymentManagement.cancelAction(deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().get(0).getId());

        // find the current active action
        final List<Action> cancelActions = deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().stream()
                .filter(Action::isCancelingOrCanceled).toList();
        assertThat(cancelActions).hasSize(1);
        assertThat(cancelActions.get(0).isCancelingOrCanceled()).isTrue();

        // test - force quit: Canceled actions should return 204
        mvc.perform(delete(TARGETS_V1 + "/{targetId}/actions/{actionId}?force=true",
                        tA.getControllerId(), cancelActions.get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
    }

    /**
     * Force Quit an Action, which is not canceled. Expected Result is an HTTP response code 405.
     */
    @Test
    void forceQuitAnNotCanceledActionReturnsMethodNotAllowed() throws Exception {
        final Target tA = createTargetAndStartAction();

        // test - cancel an cancel action returns forbidden
        mvc.perform(delete(TARGETS_V1 + "/{targetId}/actions/{actionId}?force=true",
                        tA.getControllerId(), deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE)
                                .getContent().get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Ensures that deletion is executed if permitted.
     */
    @Test
    void deleteTargetReturnsOK() throws Exception {
        final String knownControllerId = "knownControllerIdDelete";
        testdataFactory.createTarget(knownControllerId);

        mvc.perform(delete(TARGETS_V1 + "/" + knownControllerId)).andExpect(status().isNoContent());

        assertThat(targetManagement.findByControllerId(knownControllerId)).isNotPresent();
    }

    /**
     * Ensures that deletion is refused with not found if target does not exist.
     */
    @Test
    void deleteTargetWhichDoesNotExistsLeadsToNotFound() throws Exception {
        final String knownControllerId = "knownControllerIdDelete";
        mvc.perform(delete(TARGETS_V1 + "/" + knownControllerId)).andExpect(status().isNotFound());
    }

    /**
     * Ensures that update is refused with not found if target does not exist.
     */
    @Test
    void updateTargetWhichDoesNotExistsLeadsToNotFound() throws Exception {
        final String knownControllerId = "knownControllerIdUpdate";
        mvc.perform(put(TARGETS_V1 + "/" + knownControllerId).content("{}").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Ensures that target update request is reflected by repository.
     */
    @Test
    void updateTargetDescription() throws Exception {
        final String knownControllerId = "123";
        final String knownNewDescription = "a new desc updated over rest";
        final String knownNameNotModify = "nameNotModify";
        final String body = new JSONObject().put("description", knownNewDescription).toString();

        // prepare
        targetManagement.create(
                Create.builder().controllerId(knownControllerId).name(knownNameNotModify).description("old description").build());

        mvc.perform(put(TARGETS_V1 + "/" + knownControllerId).content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.description", equalTo(knownNewDescription)))
                .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)));

        assertThat(targetManagement.getByControllerId(knownControllerId)).satisfies(findTargetByControllerID -> {
            assertThat(findTargetByControllerID.getDescription()).isEqualTo(knownNewDescription);
            assertThat(findTargetByControllerID.getName()).isEqualTo(knownNameNotModify);
        });
    }

    /**
     * Ensures that target update request fails is updated value fails against a constraint.
     */
    @Test
    void updateTargetDescriptionFailsIfInvalidLength() throws Exception {
        final String knownControllerId = "123";
        final String knownNewDescription = randomString(513);
        final String knownNameNotModify = "nameNotModify";
        final String body = new JSONObject().put("description", knownNewDescription).toString();

        // prepare
        targetManagement.create(
                Create.builder().controllerId(knownControllerId).name(knownNameNotModify).description("old description").build());

        mvc.perform(put(TARGETS_V1 + "/" + knownControllerId).content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        assertThat(targetManagement.getByControllerId(knownControllerId).getDescription()).isEqualTo("old description");
    }

    /**
     * Ensures that target update request is reflected by repository.
     */
    @Test
    void updateTargetSecurityToken() throws Exception {
        final String knownControllerId = "123";
        final String knownNewToken = "6567576565";
        final String knownNameNotModify = "nameNotModify";
        final String body = new JSONObject().put("securityToken", knownNewToken).toString();

        // prepare
        targetManagement.create(Create.builder().controllerId(knownControllerId).name(knownNameNotModify).build());

        mvc.perform(put(TARGETS_V1 + "/" + knownControllerId).content(body)
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.securityToken", equalTo(knownNewToken)))
                .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)));

        assertThat(targetManagement.getByControllerId(knownControllerId)).satisfies(findTargetByControllerID -> {
            assertThat(findTargetByControllerID.getSecurityToken()).isEqualTo(knownNewToken);
            assertThat(findTargetByControllerID.getName()).isEqualTo(knownNameNotModify);
        });
    }

    /**
     * Ensures that target update request is reflected by repository.
     */
    @Test
    void updateTargetAddress() throws Exception {
        final String knownControllerId = "123";
        final String knownNewAddress = "amqp://test123/foobar";
        final String knownNameNotModify = "nameNotModify";
        final String body = new JSONObject().put("address", knownNewAddress).toString();

        // prepare
        targetManagement.create(Create.builder().controllerId(knownControllerId).name(knownNameNotModify).address(knownNewAddress).build());

        mvc.perform(put(TARGETS_V1 + "/" + knownControllerId).content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.address", equalTo(knownNewAddress)))
                .andExpect(jsonPath("$.name", equalTo(knownNameNotModify)));

        assertThat(targetManagement.getByControllerId(knownControllerId)).satisfies(findTargetByControllerID -> {
            assertThat(findTargetByControllerID.getAddress()).hasToString(knownNewAddress);
            assertThat(findTargetByControllerID.getName()).isEqualTo(knownNameNotModify);
        });
    }

    /**
     * Ensures that target query returns list of targets in defined format.
     */
    @Test
    void getTargetWithoutAdditionalRequestParameters() throws Exception {
        final int knownTargetAmount = 3;
        final String idA = "a";
        final String idB = "b";
        final String idC = "c";
        final String linksHrefPrefix = "http://localhost/rest/v1/targets/";
        createTargetsAlphabetical(knownTargetAmount);
        mvc.perform(get(TARGETS_V1))
                .andExpect(status().isOk())
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
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')]._links.self.href", contains(linksHrefPrefix + idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].name", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].description", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].controllerId", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].updateStatus", contains("registered")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].lastControllerRequestAt", notNullValue()));
    }

    /**
     * Ensures that target query returns list of targets in defined format in size reduced by given limit parameter.
     */
    @Test
    void getTargetWithPagingLimitRequestParameter() throws Exception {
        final int knownTargetAmount = 3;
        final int limitSize = 1;
        createTargetsAlphabetical(knownTargetAmount);
        final String idA = "a";
        final String linksHrefPrefix = "http://localhost/rest/v1/targets/";

        mvc.perform(get(TARGETS_V1).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')]._links.self.href", contains(linksHrefPrefix + idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].name", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].description", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].controllerId", contains(idA)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idA + "')].updateStatus", contains("registered")));
    }

    /**
     * Ensures that target query returns list of targets in defined format in size reduced by given limit and offset parameter.
     */
    @Test
    void getTargetWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int knownTargetAmount = 5;
        final int offsetParam = 2;
        final int expectedSize = knownTargetAmount - offsetParam;
        final String idC = "c";
        final String idD = "d";
        final String idE = "e";
        final String linksHrefPrefix = "http://localhost/rest/v1/targets/";

        createTargetsAlphabetical(knownTargetAmount);
        mvc.perform(get(TARGETS_V1)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(knownTargetAmount)))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)))
                // idA
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')]._links.self.href", contains(linksHrefPrefix + idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].name", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].description", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].controllerId", contains(idC)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idC + "')].updateStatus", contains("registered")))
                // idB
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')]._links.self.href", contains(linksHrefPrefix + idD)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].name", contains(idD)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].description", contains(idD)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].controllerId", contains(idD)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idD + "')].updateStatus", contains("registered")))
                // idC
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')]._links.self.href", contains(linksHrefPrefix + idE)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].name", contains(idE)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].description", contains(idE)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].controllerId", contains(idE)))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].createdBy", contains("bumlux")))
                .andExpect(jsonPath("$.content.[?(@.name=='" + idE + "')].updateStatus", contains("registered")));
    }

    /**
     * Ensures that the get request for a target works.
     */
    @Test
    void getSingleTarget() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        final Target target = createSingleTarget(knownControllerId, knownName);
        final String hrefPrefix = "http://localhost/rest/v1/targets/" + knownControllerId + "/";
        // test
        mvc.perform(get(TARGETS_V1 + "/" + knownControllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownName)))
                .andExpect(jsonPath(JSON_PATH_CONTROLLERID, equalTo(knownControllerId)))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(TARGET_DESCRIPTION_TEST)))
                .andExpect(jsonPath(JSON_PATH_LAST_REQUEST_AT, equalTo(target.getLastTargetQuery())))
                .andExpect(jsonPath("$.pollStatus", hasKey("lastRequestAt")))
                .andExpect(jsonPath("$.pollStatus", hasKey("nextExpectedRequestAt")))
                .andExpect(jsonPath("$.pollStatus.overdue", equalTo(false)))
                .andExpect(jsonPath("$._links.assignedDS.href", equalTo(hrefPrefix + "assignedDS")))
                .andExpect(jsonPath("$._links.installedDS.href", equalTo(hrefPrefix + "installedDS")))
                .andExpect(jsonPath("$._links.actions.href", equalTo(hrefPrefix + "actions" + "?offset=0&limit=50&sort=id%3ADESC")));
    }

    /**
     * Ensures that target get request returns a not found if the target does not exits.
     */
    @Test
    void getSingleTargetNoExistsResponseNotFound() throws Exception {
        final String targetIdNotExists = "bubu";

        // test
        final MvcResult mvcResult = mvc.perform(get(TARGETS_V1 + "/" + targetIdNotExists))
                .andExpect(status().isNotFound())
                .andReturn();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_ENTITY_NOT_EXISTS.getKey());
    }

    /**
     * Ensures that get request for asigned distribution sets returns no count if no distribution set has been assigned.
     */
    @Test
    void getAssignedDistributionSetOfTargetIsEmpty() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);

        // test
        mvc.perform(get(TARGETS_V1 + "/" + knownControllerId + "/assignedDS"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

    }

    /**
     * Ensures that the get request for asigned distribution sets works.
     */
    @Test
    void getAssignedDistributionSetOfTarget() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds.getId(), knownControllerId);

        // test
        final SoftwareModule os = findFirstModuleByType(ds, osType).orElseThrow();
        final SoftwareModule jvm = findFirstModuleByType(ds, runtimeType).orElseThrow();
        final SoftwareModule bApp = findFirstModuleByType(ds, appType).orElseThrow();
        mvc.perform(get(TARGETS_V1 + "/" + knownControllerId + "/assignedDS"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_ID, equalTo(ds.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(ds.getName())))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(ds.getDescription())))
                // os
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].id", contains(os.getId().intValue())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].name", contains(os.getName())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].description", contains(os.getDescription())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].version", contains(os.getVersion())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].vendor", contains(os.getVendor())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].type", contains("os")))
                // jvm
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].id", contains(jvm.getId().intValue())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].name", contains(jvm.getName())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].description", contains(jvm.getDescription())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].version", contains(jvm.getVersion())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].vendor", contains(jvm.getVendor())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].type", contains("runtime")))
                // baseApp
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].id", contains(bApp.getId().intValue())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].name", contains(bApp.getName())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].description", contains(bApp.getDescription())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].version", contains(bApp.getVersion())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].vendor", contains(bApp.getVendor())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].type", contains("application")));
    }

    /**
     * Ensures that get request for installed distribution sets returns no count if no distribution set has been installed.
     */
    @Test
    void getInstalledDistributionSetOfTargetIsEmpty() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = "someName";
        createSingleTarget(knownControllerId, knownName);

        mvc.perform(get(TARGETS_V1 + "/" + knownControllerId + "/installedDS"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    /**
     * Ensures that a target creation with empty name and a controllerId that exceeds the name length limitation is successful and the name gets truncated.
     */
    @Test
    void createTargetWithEmptyNameAndLongControllerId() throws Exception {
        final String randomString = randomString(JpaTarget.CONTROLLER_ID_MAX_SIZE);
        final Create target = Create.builder().controllerId(randomString).build();
        final String targetList = JsonBuilder.targets(List.of(target), false);
        final String expectedTargetName = randomString.substring(0, Math.min(JpaTarget.CONTROLLER_ID_MAX_SIZE, NamedEntity.NAME_MAX_SIZE));

        mvc.perform(post(TARGETS_V1).content(targetList).contentType(APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("[0].controllerId", equalTo(randomString)))
                .andExpect(jsonPath("[0].name", equalTo(expectedTargetName)));

        assertThat(targetManagement.getByControllerId(randomString).getName()).isEqualTo(expectedTargetName);
    }

    /**
     * Ensures that post request for creating a target with no payload returns a bad request.
     */
    @Test
    void createTargetWithoutPayloadBadRequest() throws Exception {
        final MvcResult mvcResult = mvc
                .perform(post(TARGETS_V1).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(targetManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getKey());
        assertThat(exceptionInfo.getMessage()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getMessage());
    }

    /**
     * Ensures that post request for creating a target with invalid payload returns a bad request.
     */
    @Test
    void createTargetWithBadPayloadBadRequest() throws Exception {
        final String notJson = "abc";

        final MvcResult mvcResult = mvc
                .perform(post(TARGETS_V1).content(notJson).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(targetManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(MessageNotReadableException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REST_BODY_NOT_READABLE.getKey());
    }

    /**
     * Verifies that a mandatory properties of new targets are validated as not null.
     */
    @Test
    void createTargetWithMissingMandatoryPropertyBadRequest() throws Exception {
        final MvcResult mvcResult = mvc
                .perform(post(TARGETS_V1).content("[{\"name\":\"id1\"}]").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(targetManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(ConstraintViolationException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getKey());
    }

    /**
     * Verifies that a  properties of new targets are validated as in allowed size range.
     */
    @Test
    void createTargetWithInvalidPropertyBadRequest() throws Exception {
        final Create test1 = Create.builder().controllerId("id1").name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();

        final MvcResult mvcResult = mvc
                .perform(post(TARGETS_V1).content(JsonBuilder.targets(List.of(test1), true)).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(targetManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(ConstraintViolationException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getKey());
    }

    /**
     * Ensures that a post request for creating multiple targets works.
     */
    @Test
    void createTargetsListReturnsSuccessful() throws Exception {
        final Create test1 = Create.builder().controllerId("id1").name("testname1")
                .securityToken("token").address("amqp://test123/foobar").description("testid1").build();
        final Create test2 = Create.builder().controllerId("id2").name("testname2")
                .description("testid2").build();
        final Create test3 = Create.builder().controllerId("id3").name("testname3")
                .description("testid3").build();

        final List<Create> targets = List.of(test1, test2, test3);

        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/targets").content(JsonBuilder.targets(targets, true))
                        .contentType(APPLICATION_JSON).accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
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
                .andExpect(jsonPath("[2].createdBy", equalTo("bumlux")))
                .andReturn();

        assertThat((Object) JsonPath.compile("[0]._links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/targets/id1");
        assertThat((Object) JsonPath.compile("[1]._links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/targets/id2");
        assertThat((Object) JsonPath.compile("[2]._links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/targets/id3");

        final Target t1 = assertTarget("id1", "testname1", "testid1");
        assertThat(t1.getSecurityToken()).isEqualTo("token");
        assertThat(t1.getAddress()).hasToString("amqp://test123/foobar");

        assertTarget("id2", "testname2", "testid2");
        assertTarget("id3", "testname3", "testid3");
    }

    /**
     * Ensures that a post request for creating one target within a list works.
     */
    @Test
    void createTargetsSingleEntryListReturnsSuccessful() throws Exception {
        final String knownName = "someName";
        final String knownControllerId = "controllerId1";
        final String knownDescription = "someDescription";
        final String createTargetsJson = getCreateTargetsListJsonString(knownControllerId, knownName, knownDescription);

        mvc.perform(post(TARGETS_V1).content(createTargetsJson).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is2xxSuccessful());

        final Page<? extends Target> findTargetsAll = targetManagement.findAll(PageRequest.of(0, 100));
        final Target target = findTargetsAll.getContent().get(0);
        assertThat(targetManagement.count()).isEqualTo(1);
        assertThat(target.getControllerId()).isEqualTo(knownControllerId);
        assertThat(target.getName()).isEqualTo(knownName);
        assertThat(target.getDescription()).isEqualTo(knownDescription);
    }

    /**
     * Ensures that a post request for creating the same target again leads to a conflict response.
     */
    @Test
    void createTargetsSingleEntryListDoubleReturnConflict() throws Exception {
        final String knownName = "someName";
        final String knownControllerId = "controllerId1";
        final String knownDescription = "someDescription";
        final String createTargetsJson = getCreateTargetsListJsonString(knownControllerId, knownName, knownDescription);

        // create a taret first to provoke a already exists error

        mvc.perform(post(TARGETS_V1).content(createTargetsJson).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is2xxSuccessful());
        // create another one to retrieve the entity already exists exception
        final MvcResult mvcResult = mvc.perform(post(TARGETS_V1).content(createTargetsJson).contentType(APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.CONFLICT.value()))
                .andReturn();

        // verify only one entry
        assertThat(targetManagement.count()).isEqualTo(1);

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(EntityAlreadyExistsException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_ENTITY_ALREADY_EXISTS.getKey());
        assertThat(exceptionInfo.getMessage()).isEqualTo(SpServerError.SP_REPO_ENTITY_ALREADY_EXISTS.getMessage());
    }

    /**
     * Ensures that the get request for action of a target returns no actions if nothing has happened.
     */
    @Test
    void getActionWithEmptyResult() throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTarget(knownTargetId);

        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(0)))
                .andExpect(jsonPath("content", hasSize(0)))
                .andExpect(jsonPath("total", equalTo(0)));
    }

    /**
     * Ensures that the expected response is returned for update action.
     */
    @Test
    void getUpdateAction() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        mvc.perform(get(TARGETS_V1 +
                        "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actions.get(1).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("type", equalTo("update")))
                .andExpect(jsonPath("active", equalTo(true)))
                .andExpect(jsonPath("forceType", equalTo("forced")))
                .andExpect(jsonPath("maintenanceWindow").doesNotExist())
                .andExpect(jsonPath("_links.self.href", equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                .andExpect(jsonPath("_links.distributionset.href", equalTo(generateActionDsLink(actions.get(1).getDistributionSet().getId()))))
                .andExpect(jsonPath("_links.status.href", equalTo(generateStatusreferenceLink(knownTargetId, actions.get(1).getId()))));
    }

    /**
     * Ensures that the expected response is returned for update action with maintenance window.
     */
    @Test
    void getUpdateActionWithMaintenanceWindow() throws Exception {
        final String knownTargetId = "targetId";
        final String schedule = getTestSchedule(10);
        final String duration = getTestDuration(10);
        final String timezone = getTestTimeZone();
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(
                knownTargetId, schedule, duration, timezone);

        mvc.perform(get(TARGETS_V1 +
                        "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actions.get(1).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("type", equalTo("update")))
                .andExpect(jsonPath("active", equalTo(true)))
                .andExpect(jsonPath("forceType", equalTo("forced")))
                .andExpect(jsonPath("maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("maintenanceWindow.nextStartAt",
                        equalTo(actions.get(1).getMaintenanceWindowStartTime().get().toInstant().toEpochMilli())))
                .andExpect(jsonPath("_links.self.href", equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                .andExpect(jsonPath("_links.distributionset.href", equalTo(generateActionDsLink(actions.get(1).getDistributionSet().getId()))))
                .andExpect(jsonPath("_links.status.href", equalTo(generateStatusreferenceLink(knownTargetId, actions.get(1).getId()))));
    }

    /**
     * Ensures that the expected response is returned when update action was cancelled.
     */
    @Test
    void getCancelAction() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        mvc.perform(get(TARGETS_V1 +
                        "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actions.get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("type", equalTo("cancel")))
                .andExpect(jsonPath("active", equalTo(true)))
                .andExpect(jsonPath("forceType", equalTo("forced")))
                .andExpect(jsonPath("maintenanceWindow").doesNotExist())
                .andExpect(jsonPath("_links.self.href", equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                .andExpect(jsonPath("_links.canceledaction.href", equalTo(generateCanceledactionreferenceLink(knownTargetId, actions.get(0)))))
                .andExpect(jsonPath("_links.status.href", equalTo(generateStatusreferenceLink(knownTargetId, actions.get(0).getId()))));
    }

    /**
     * Ensures that the expected response is returned when update action with maintenance window was cancelled.
     */
    @Test
    void getCancelActionWithMaintenanceWindow() throws Exception {
        final String knownTargetId = "targetId";
        final String schedule = getTestSchedule(10);
        final String duration = getTestDuration(10);
        final String timezone = getTestTimeZone();
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(
                knownTargetId, schedule, duration, timezone);

        mvc.perform(get(TARGETS_V1 +
                        "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actions.get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("forceType", equalTo("forced")))
                .andExpect(jsonPath("type", equalTo("cancel")))
                .andExpect(jsonPath("active", equalTo(true)))
                .andExpect(jsonPath("maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("maintenanceWindow.nextStartAt",
                        equalTo(actions.get(0).getMaintenanceWindowStartTime().get().toInstant().toEpochMilli())))
                .andExpect(jsonPath("_links.self.href", equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                .andExpect(jsonPath("_links.canceledaction.href", equalTo(generateCanceledactionreferenceLink(knownTargetId, actions.get(0)))))
                .andExpect(jsonPath("_links.status.href", equalTo(generateStatusreferenceLink(knownTargetId, actions.get(0).getId()))));
    }

    /**
     * Ensures that the expected response of getting actions of a target is returned.
     */
    @Test
    void getActions() throws Exception {
        getActions(false);
    }

    /**
     * Ensures that the expected response of getting actions (with ext refs) of a target is returned.
     */
    @Test
    void getActionsExtRef() throws Exception {
        getActions(true);
    }

    /**
     * Ensures that the expected response of getting actions with maintenance window of a target is returned.
     */
    @Test
    void getActionsWithMaintenanceWindow() throws Exception {
        final String knownTargetId = "targetId";
        final String schedule = getTestSchedule(10);
        final String duration = getTestDuration(10);
        final String timezone = getTestTimeZone();
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(knownTargetId,
                schedule, duration, timezone);

        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[1].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("update")))
                .andExpect(jsonPath("content.[1].active", equalTo(true)))
                .andExpect(jsonPath("content.[1].maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("content.[1].maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("content.[1].maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("content.[1].maintenanceWindow.nextStartAt",
                        equalTo(actions.get(1).getMaintenanceWindowStartTime().get().toInstant().toEpochMilli())))
                .andExpect(jsonPath("content.[1]._links.self.href", equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].active", equalTo(true)))
                .andExpect(jsonPath("content.[0].maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("content.[0].maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("content.[0].maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("content.[0].maintenanceWindow.nextStartAt",
                        equalTo(actions.get(0).getMaintenanceWindowStartTime().get().toInstant().toEpochMilli())))
                .andExpect(jsonPath("content.[0]._links.self.href", equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    /**
     * Verifies that the API returns the status list with expected content.
     */
    @Test
    void getActionsStatus() throws Exception {
        final String knownTargetId = "targetId";
        final Action action = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId).get(0);
        // retrieve list in default descending order for action status entries
        final List<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(action.getId(), PAGE)
                .getContent().stream().sorted((e1, e2) -> Long.compare(e2.getId(), e1.getId()))
                .toList();

        // sort is default descending order, latest status first
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/"
                        + MgmtTarget.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                        + MgmtTarget.TARGET_V1_ACTION_STATUS))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("canceling")))
                .andExpect(jsonPath("content.[0].messages", hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath("content.[1].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("running")))
                .andExpect(jsonPath("content.[1].reportedAt", equalTo(actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    /**
     * Verifies that the API returns the status list with expected content sorted by reportedAt field.
     */
    @Test
    void getActionsStatusSortedByReportedAt() throws Exception {
        final String knownTargetId = "targetId";
        final Action action = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId).get(0);
        final List<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(action.getId(), PAGE)
                .getContent().stream().sorted(Comparator.comparingLong(Identifiable::getId))
                .toList();

        // descending order
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/"
                        + MgmtTarget.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                        + MgmtTarget.TARGET_V1_ACTION_STATUS)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "CREATEDAT:DESC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("canceling")))
                .andExpect(jsonPath("content.[0].messages", hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath("content.[1].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("running")))
                .andExpect(jsonPath("content.[1].reportedAt", equalTo(actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

        // ascending order
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/"
                        + MgmtTarget.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                        + MgmtTarget.TARGET_V1_ACTION_STATUS).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING,
                        "CREATEDAT:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
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

    /**
     * Verifies that the API returns the status list with expected content split into two pages.
     */
    @Test
    void getActionsStatusWithPagingLimitRequestParameter() throws Exception {
        final String knownTargetId = "targetId";

        final Action action = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId).get(0);
        final List<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(action.getId(), PAGE)
                .getContent().stream().sorted((e1, e2) -> Long.compare(e1.getId(), e2.getId()))
                .toList();

        // Page 1
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/"
                        + MgmtTarget.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                        + MgmtTarget.TARGET_V1_ACTION_STATUS).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT,
                        String.valueOf(1)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("canceling")))
                .andExpect(jsonPath("content.[0].messages", hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // Page 2
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/"
                        + MgmtTarget.TARGET_V1_ACTIONS + "/" + action.getId() + "/"
                        + MgmtTarget.TARGET_V1_ACTION_STATUS)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("running")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo(actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    /**
     * Verifies getting multiple actions with the paging request parameter.
     */
    @Test
    void getActionsWithPagingLimitRequestParameter() throws Exception {
        final String knownTargetId = "targetId";
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);

        // page 1: one entry
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/"
                        + MgmtTarget.TARGET_V1_ACTIONS)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                .andExpect(jsonPath("content.[0].active", equalTo(true)))
                .andExpect(jsonPath("content.[0]._links.self.href", equalTo(generateActionSelfLink(knownTargetId, actions.get(0).getId()))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // page 2: one entry
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/"
                        + MgmtTarget.TARGET_V1_ACTIONS)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("update")))
                .andExpect(jsonPath("content.[0].active", equalTo(true)))
                .andExpect(jsonPath("content.[0]._links.self.href", equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    /**
     * Verifies that an action is switched from soft to forced if requested by management API
     */
    @Test
    void updateAction() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet set = testdataFactory.createDistributionSet();
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(set.getId(), target.getControllerId(), ActionType.SOFT));
        assertThat(deploymentManagement.findAction(actionId).get().getActionType()).isEqualTo(ActionType.SOFT);

        final String body = new JSONObject().put("forceType", "forced").toString();
        mvc.perform(put(TARGETS_V1 + "/" + target.getControllerId() + "/"
                        + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId).content(body)
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actionId.intValue())))
                .andExpect(jsonPath("type", equalTo("update")))
                .andExpect(jsonPath("active", equalTo(true)))
                .andExpect(jsonPath("forceType", equalTo("forced")))
                .andExpect(jsonPath("_links.self.href", equalTo(generateActionSelfLink(target.getControllerId(), actionId))))
                .andExpect(jsonPath("_links.distributionset.href", equalTo("http://localhost/rest/v1/distributionsets/" + set.getId())))
                .andExpect(jsonPath("_links.status.href", equalTo(generateStatusreferenceLink(target.getControllerId(), actionId))));

        assertThat(deploymentManagement.findAction(actionId).get().getActionType()).isEqualTo(ActionType.FORCED);
    }

    /**
     * Verifies that a DS to target assignment is reflected by the repository and that repeating
     * the assignment does not change the target.
     */
    @Test
    void assignDistributionSetToTarget() throws Exception {
        Target target = testdataFactory.createTarget();
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS")
                        .content("{\"id\":" + set.getId() + "}").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(1)))
                .andExpect(jsonPath("alreadyAssigned", equalTo(0)))
                .andExpect(jsonPath("total", equalTo(1)));
        implicitLock(set);

        assertThat(deploymentManagement.findAssignedDistributionSet(target.getControllerId()).get()).isEqualTo(set);
        target = targetManagement.getByControllerId(target.getControllerId());

        // repeating DS assignment leads again to OK
        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS")
                        .content("{\"id\":" + set.getId() + "}").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(0)))
                .andExpect(jsonPath("alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1)));

        // ...but does not change the target
        assertThat(targetManagement.findByControllerId(target.getControllerId()).get()).isEqualTo(target);
    }

    /**
     * Ensures that confirmation option is considered in assignment request.
     */
    @ParameterizedTest
    @MethodSource("confirmationOptions")
    void assignDistributionSetToTargetWithConfirmationOptions(final boolean confirmationFlowActive, final Boolean confirmationRequired)
            throws Exception {
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

        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS")
                        .content(jsonPayload.toString()).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(1)))
                .andExpect(jsonPath("alreadyAssigned", equalTo(0)))
                .andExpect(jsonPath("total", equalTo(1)));
        implicitLock(set);

        assertThat(deploymentManagement.findAssignedDistributionSet(target.getControllerId()).get()).isEqualTo(set);

        assertThat(actionRepository.findAll(byDistributionSetId(set.getId()), PAGE).map(Action.class::cast).getContent())
                .hasSize(1)
                .allMatch(action -> {
                    if (!confirmationFlowActive) {
                        return !action.isWaitingConfirmation();
                    }
                    return confirmationRequired == null
                            ? action.isWaitingConfirmation()
                            : confirmationRequired == action.isWaitingConfirmation();
                });
    }

    /**
     * Verifies that a DOWNLOAD_ONLY DS to target assignment is properly handled
     */
    @Test
    void assignDownloadOnlyDistributionSetToTarget() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS")
                        .content("{\"id\":" + set.getId() + ",\"type\": \"downloadonly\"}")
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(1)))
                .andExpect(jsonPath("alreadyAssigned", equalTo(0)))
                .andExpect(jsonPath("total", equalTo(1)));
        implicitLock(set);

        assertThat(deploymentManagement.findAssignedDistributionSet(target.getControllerId()).get()).isEqualTo(set);
        final Slice<Action> actions = deploymentManagement.findActionsByTarget("targetExist", PageRequest.of(0, 100));
        assertThat(actions).isNotEmpty().filteredOn(a -> a.getDistributionSet().equals(set))
                .extracting(Action::getActionType).containsOnly(ActionType.DOWNLOAD_ONLY);
    }

    /**
     * Verifies that an offline DS to target assignment is reflected by the repository and that repeating
     * the assignment does not change the target.
     */
    @Test
    void offlineAssignDistributionSetToTarget() throws Exception {
        Target target = testdataFactory.createTarget();
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS?offline=true")
                        .content("{\"id\":" + set.getId() + "}")
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(1)))
                .andExpect(jsonPath("alreadyAssigned", equalTo(0)))
                .andExpect(jsonPath("total", equalTo(1)));
        implicitLock(set);

        assertThat(deploymentManagement.findAssignedDistributionSet(target.getControllerId()).get()).isEqualTo(set);
        assertThat(deploymentManagement.findInstalledDistributionSet(target.getControllerId()).get()).isEqualTo(set);
        target = targetManagement.getByControllerId(target.getControllerId());
        assertThat(target.getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);

        // repeating DS assignment leads again to OK
        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS?offline=true")
                        .content("{\"id\":" + set.getId() + "}")
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(0)))
                .andExpect(jsonPath("alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1)));

        // ...but does not change the target
        assertThat(targetManagement.getByControllerId(target.getControllerId())).isEqualTo(target);
    }

    @Test
    void assignDistributionSetToTargetWithActionTimeForcedAndTime() throws Exception {
        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final long forceTime = System.currentTimeMillis();
        final String body = new JSONObject()
                .put("id", set.getId())
                .put("type", "timeforced").put("forcetime", forceTime)
                .toString();

        mvc.perform(post(TARGETS_V1 + "/fsdfsd/assignedDS").content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        implicitLock(set);

        final List<Action> findActiveActionsByTarget = deploymentManagement
                .findActiveActionsByTarget(target.getControllerId(), PAGE).getContent();
        assertThat(findActiveActionsByTarget).hasSize(1);
        assertThat(findActiveActionsByTarget.get(0).getActionType()).isEqualTo(ActionType.TIMEFORCED);
        assertThat(findActiveActionsByTarget.get(0).getForcedTime()).isEqualTo(forceTime);
        assertThat(deploymentManagement.findAssignedDistributionSet("fsdfsd").get()).isEqualTo(set);
    }

    /**
     * Assigns distribution set to target with only maintenance schedule.
     */
    @Test
    void assignDistributionSetToTargetWithMaintenanceWindowStartTimeOnly() throws Exception {
        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final String body = new JSONObject()
                .put("id", set.getId())
                .put("type", "forced")
                .put("maintenanceWindow", new JSONObject().put("schedule", getTestSchedule(0)))
                .toString();

        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS")
                        .content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    /**
     * Assigns distribution set to target with only maintenance window duration.
     */
    @Test
    void assignDistributionSetToTargetWithMaintenanceWindowEndTimeOnly() throws Exception {
        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final String body = new JSONObject().put("id", set.getId())
                .put("type", "forced")
                .put("maintenanceWindow", new JSONObject().put("duration", getTestDuration(10)))
                .toString();

        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS")
                        .content(body).contentType(MediaTypes.HAL_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    /**
     * Assigns distribution set to target with valid maintenance window.
     */
    @Test
    void assignDistributionSetToTargetWithValidMaintenanceWindow() throws Exception {
        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final String body = new JSONObject()
                .put("id", set.getId())
                .put("type", "forced")
                .put("forcetime", "0")
                .put("maintenanceWindow", new JSONObject()
                        .put("schedule", getTestSchedule(10))
                        .put("duration", getTestDuration(10))
                        .put("timezone", getTestTimeZone()))
                .toString();

        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS")
                        .content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Assigns distribution set to target with maintenance window next execution start (should be ignored, calculated automaticaly based on schedule, duration and timezone)
     */
    @Test
    void assignDistributionSetToTargetWithMaintenanceWindowNextExecutionStart() throws Exception {
        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final long nextExecutionStart = System.currentTimeMillis();

        final String body = new JSONObject().put("id", set.getId()).put("maintenanceWindow",
                        new JSONObject()
                                .put("schedule", getTestSchedule(10))
                                .put("duration", getTestDuration(10))
                                .put("timezone", getTestTimeZone())
                                .put("nextStartAt", String.valueOf(nextExecutionStart)))
                .toString();

        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS")
                        .content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(get(TARGETS_V1 + "/" + target.getControllerId() + "/"
                        + MgmtTarget.TARGET_V1_ACTIONS))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath("content.[0].maintenanceWindow.nextStartAt", not(nextExecutionStart)));
    }

    /**
     * Assigns distribution set to target with last maintenance window scheduled before current time.
     */
    @Test
    void assignDistributionSetToTargetWithMaintenanceWindowEndTimeBeforeStartTime() throws Exception {
        final Target target = testdataFactory.createTarget("fsdfsd");
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final String body = new JSONObject()
                .put("id", set.getId())
                .put("type", "forced")
                .put("maintenanceWindow", new JSONObject()
                        .put("schedule", getTestSchedule(-30))
                        .put("duration", getTestDuration(5))
                        .put("timezone", getTestTimeZone()))
                .toString();

        mvc.perform(post(TARGETS_V1 + "/" + target.getControllerId() + "/assignedDS")
                        .content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidRequestsOnAssignDistributionSetToTarget() throws Exception {
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        mvc.perform(post(TARGETS_V1 + "/fsdfsd/assignedDS")
                        .content("{\"id\":" + set.getId() + "}").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        testdataFactory.createTarget("fsdfsd");

        mvc.perform(post(TARGETS_V1 + "/fsdfsd/assignedDS")
                        .content("{\"id\":" + set.getId() + "}").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(post(TARGETS_V1 + "/fsdfsd/assignedDS")
                        .content("{\"id\":12345678}").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete(TARGETS_V1 + "/fsdfsd/assignedDS")
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(TARGETS_V1 + "/fsdfsd/assignedDS")
                        .content("{\"id\":" + set.getId() + "}").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void invalidRequestsOnActionResource() throws Exception {
        final String knownTargetId = "targetId";

        // target does not exist
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        final Long actionId = actions.get(0).getId();

        // should work now
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        // action does not exist
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/12321"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        // not allowed methods
        mvc.perform(post(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        // Invalid content
        mvc.perform(put(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void invalidRequestsOnActionStatusResource() throws Exception {
        final String knownTargetId = "targetId";

        // target does not exist
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/1/status"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride(knownTargetId);
        final Long actionId = actions.get(0).getId();

        // should work now
        mvc.perform(get(TARGETS_V1 +
                        "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId + "/status"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // action does not exist
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/12321/status"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(delete(TARGETS_V1 +
                        "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId + "/status"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(TARGETS_V1 +
                        "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId + "/status"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post(TARGETS_V1 +
                        "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId + "/status"))
                .andDo(MockMvcResultPrinter.print())
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
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/attributes"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.a", equalTo("1")))
                .andExpect(jsonPath("$.b", equalTo("2")));
    }

    @Test
    void getControllerEmptyAttributesReturnsNoContent() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdWithAttributes";
        testdataFactory.createTarget(knownTargetId);

        // test query target over rest resource
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/attributes"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    /**
     * Request update of Controller Attributes.
     */
    @Test
    void triggerControllerAttributesUpdate() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdNeedsUpdate";
        final Map<String, String> knownControllerAttrs = new HashMap<>();
        knownControllerAttrs.put("a.1", "1");
        knownControllerAttrs.put("b.2", "2");
        testdataFactory.createTarget(knownTargetId);
        controllerManagement.updateControllerAttributes(knownTargetId, knownControllerAttrs, null);
        assertThat(targetManagement.getByControllerId(knownTargetId).isRequestControllerAttributes()).isFalse();

        verifyAttributeUpdateCanBeRequested(knownTargetId);
        verifyRequestAttributesAttributeIsOptional(knownTargetId);
        verifyResettingRequestAttributesIsNotAllowed(knownTargetId);
    }

    @Test
    void searchTargetsUsingRsqlQuery() throws Exception {
        final int amountTargets = 10;
        createTargetsAlphabetical(amountTargets);

        final String rsqlFindAOrB = "controllerId==a,controllerId==b";

        mvc.perform(get(TARGETS_V1 + "?q=" + rsqlFindAOrB))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2)))
                .andExpect(jsonPath("content[0].controllerId", equalTo("a")))
                .andExpect(jsonPath("content[1].controllerId", equalTo("b")));
    }

    @Test
    void getControllerTagReturnsTagsWithOk() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdWithTags";
        final Target target = testdataFactory.createTarget(knownTargetId);

        // test query target over rest resource with no tags - expect OK with empty list
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/tags"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));

        final List<? extends TargetTag> targetTags = testdataFactory.createTargetTags(2, "tag_getControllerTagReturnsTagWithOk");
        final List<String> tagNames = new ArrayList<>();
        for (final TargetTag targetTag : targetTags) {
            targetManagement.assignTag(Collections.singletonList(target.getControllerId()), targetTag.getId());
            tagNames.add(targetTag.getName());
        }

        // test query target over rest resource with 2 tags - expect OK with 2 tags the target is tagged with
        mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/tags"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.[0].name", in(tagNames)))
                .andExpect(jsonPath("$.[1].name", in(tagNames)));
    }

    /**
     * Ensures that the metadata creation through API is reflected by the repository.
     */
    @Test
    void createMetadata() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";
        testdataFactory.createTarget(knownControllerId);

        final String knownKey1 = "known.key.1";
        final String knownKey2 = "knownKey2";

        final String knownValue1 = "knownValue1";
        final String knownValue2 = "knownValue2";

        final JSONArray metaData1 = new JSONArray();
        metaData1.put(new JSONObject().put(JSON_PATH_KEY, knownKey1).put(JSON_PATH_VALUE, knownValue1));
        metaData1.put(new JSONObject().put(JSON_PATH_KEY, knownKey2).put(JSON_PATH_VALUE, knownValue2));

        mvc.perform(post("/rest/v1/targets/{targetId}/metadata", knownControllerId).accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON).content(metaData1.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        assertThat(targetManagement.getMetadata(knownControllerId).get(knownKey1)).isEqualTo(knownValue1);
        assertThat(targetManagement.getMetadata(knownControllerId).get(knownKey2)).isEqualTo(knownValue2);

        // verify quota enforcement
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerTarget();

        final JSONArray metaData2 = new JSONArray();
        for (int i = 0; i < maxMetaData - metaData1.length() + 1; ++i) {
            metaData2.put(new JSONObject().put(JSON_PATH_KEY, knownKey1 + i).put(JSON_PATH_VALUE, knownValue1 + i));
        }

        mvc.perform(post("/rest/v1/targets/{targetId}/metadata", knownControllerId)
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON).content(metaData2.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isTooManyRequests());

        // verify that the number of meta-data entries has not changed (we cannot use the PAGE constant here as it tries to sort by ID)
        assertThat(targetManagement.getMetadata(knownControllerId)).hasSize(metaData1.length());
    }

    /**
     * Ensures that a metadata update through API is reflected by the repository.
     */
    @Test
    void updateMetadata() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";
        final String updateValue = "valueForUpdate";

        setupTargetWithMetadata(knownControllerId, KNOWN_KEY, KNOWN_VALUE);

        final JSONObject jsonObject = new JSONObject().put(JSON_PATH_KEY, KNOWN_KEY).put(JSON_PATH_VALUE, updateValue);

        mvc.perform(put("/rest/v1/targets/{targetId}/metadata/{key}", knownControllerId, KNOWN_KEY)
                        .accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
                        .content(jsonObject.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(targetManagement.getMetadata(knownControllerId).get(KNOWN_KEY)).isEqualTo(updateValue);
    }

    /**
     * Ensures that a metadata entry deletion through API is reflected by the repository.
     */
    @Test
    void deleteMetadata() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";

        setupTargetWithMetadata(knownControllerId, KNOWN_KEY, KNOWN_VALUE);

        mvc.perform(delete("/rest/v1/targets/{targetId}/metadata/{key}", knownControllerId, KNOWN_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        // already deleted
        mvc.perform(delete("/rest/v1/targets/{targetId}/metadata/{key}", knownControllerId, KNOWN_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        assertThat(targetManagement.getMetadata(knownControllerId).get(KNOWN_KEY)).isNull();
    }

    /**
     * Ensures that target metadata deletion request to API on an entity that does not exist results in NOT_FOUND.
     */
    @Test
    void deleteMetadataThatDoesNotExistLeadsToNotFound() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";

        setupTargetWithMetadata(knownControllerId, KNOWN_KEY, KNOWN_VALUE);

        mvc.perform(delete("/rest/v1/targets/{targetId}/metadata/XXX", knownControllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/targets/1234/metadata/{key}", KNOWN_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        assertThat(targetManagement.getMetadata(knownControllerId).get(KNOWN_KEY)).isNotNull();
    }

    /**
     * Ensures that a metadata entry selection through API reflects the repository content.
     */
    @Test
    void getMetadataKey() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";

        setupTargetWithMetadata(knownControllerId, KNOWN_KEY, KNOWN_VALUE);

        mvc.perform(get("/rest/v1/targets/{targetId}/metadata/{key}", knownControllerId, KNOWN_KEY))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_KEY, equalTo(KNOWN_KEY)))
                .andExpect(jsonPath(JSON_PATH_VALUE, equalTo(KNOWN_VALUE)));
    }

    /**
     * Ensures that a metadata entry paged list selection through API reflectes the repository content.
     */
    @Test
    void getMetadata() throws Exception {
        final String knownControllerId = "targetIdWithMetadata";

        final int totalMetadata = 10;
        final String knownKeyPrefix = "knownKeyPrefix";
        final String knownValuePrefix = "knownValuePrefix";

        testdataFactory.createTarget(knownControllerId);

        // test call of a target without metadata & support for legacy offset/limit
        mvc.perform(get("/rest/v1/targets/{targetId}/metadata?offset=0&limit=50", knownControllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(0)))
                .andExpect(jsonPath("total", equalTo(0)))
                .andExpect(jsonPath("content.length()", equalTo(0)));

        // set metadata
        final Map<String, String> metadataEntries = new HashMap<>();
        for (int index = 0; index < totalMetadata; index++) {
            metadataEntries.put(knownKeyPrefix + index, knownValuePrefix + index);
        }
        targetManagement.createMetadata(knownControllerId, metadataEntries);

        // test call of a target with metadata
        mvc.perform(get("/rest/v1/targets/{targetId}/metadata", knownControllerId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(totalMetadata)))
                .andExpect(jsonPath("total", equalTo(totalMetadata)))
                .andExpect(jsonPath("content[0].key", equalTo(knownKeyPrefix + "0")))
                .andExpect(jsonPath("content[0].value", equalTo(knownValuePrefix + "0")));
    }

    /**
     * A request for assigning multiple DS to a target results in a Bad Request when multiassignment in disabled.
     */
    @Test
    void multiAssignmentRequestNotAllowedIfDisabled() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final List<Long> dsIds = testdataFactory.createDistributionSets(2).stream().map(DistributionSet::getId)
                .toList();

        final JSONArray body = new JSONArray();
        dsIds.forEach(id -> body.put(getAssignmentObject(id, MgmtActionType.FORCED, 67)));

        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(body.toString())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    /**
     * Passing an array in assignment request is allowed if multiassignment is disabled and array size in 1.
     */
    @Test
    void multiAssignmentRequestAllowedIfDisabledButHasSizeOne() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONArray body = new JSONArray().put(getAssignmentObject(dsId, MgmtActionType.FORCED));
        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(body.toString()).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Identical assignments in a single request are removed when multiassignment in disabled.
     */
    @Test
    void identicalAssignmentInRequestAreRemovedIfMultiassignmentsDisabled() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONObject assignment = getAssignmentObject(dsId, MgmtActionType.FORCED);
        final JSONArray body = new JSONArray().put(assignment).put(assignment);

        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(body.toString()).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)));
    }

    /**
     * An assignment request containing a weight is only accepted when weight is valid and multi assignment is on.
     */
    @Test
    void weightValidation() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int weight = 98;

        final JSONObject bodyValid = getAssignmentObject(dsId, MgmtActionType.FORCED, weight);
        final JSONObject bodyInvalid = getAssignmentObject(dsId, MgmtActionType.FORCED, Action.WEIGHT_MIN - 1);

        // enableMultiAssignments(); TODO:
        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(bodyInvalid.toString()).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.repo.constraintViolation")));
        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(bodyValid.toString()).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).get().toList();
        assertThat(actions).size().isEqualTo(1);
        assertThat(actions.get(0).getWeight()).get().isEqualTo(weight);
    }

    /**
     * An assignment request containing a valid weight when multi assignment is off.
     */
    @Test
    void weightWithSingleAssignment() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONObject bodyValid = getAssignmentObject(dsId, MgmtActionType.FORCED, 98);

        mvc.perform(post("/rest/v1/targets/{targetId}/assignedDS", targetId).content(bodyValid.toString()).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Get weight of action
     */
    @Test
    void getActionWeight() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId1 = testdataFactory.createDistributionSet().getId();
        final Long dsId2 = testdataFactory.createDistributionSet().getId();
        final int customWeight = 800;
        assignDistributionSet(dsId1, targetId); // default weight 1000
        assignDistributionSet(dsId2, targetId, customWeight);

        mvc.perform(get("/rest/v1/targets/{targetId}/actions", targetId)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "WEIGHT:ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].weight", equalTo(customWeight)))
                .andExpect(jsonPath("content.[1].weight", equalTo(1000)));
    }

    /**
     * An action provides information of the rollout it was created for (if any).
     */
    @Test
    void getActionWithRolloutInfo() throws Exception {
        // setup
        final int amountTargets = 10;
        final List<Target> targets = testdataFactory.createTargets(amountTargets, "trg", "trg");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Rollout rollout = testdataFactory.createRolloutByVariables(
                "My Rollout", "My Rollout Description", 1, "name==trg*", ds, "50", "5");
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // get all actions for the first target
        final Target target = targets.get(0);
        mvc.perform(get("/rest/v1/targets/{targetId}/actions", target.getControllerId()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath("content.[0].rollout", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("content.[0].rolloutName", equalTo(rollout.getName())));

        // get the first action for the first target;
        // verify that also the rollout link is present
        final Slice<Action> action = deploymentManagement.findActionsByTarget(target.getControllerId(),
                PageRequest.of(0, 100));
        assertThat(action.getContent()).hasSize(1);
        mvc.perform(get("/rest/v1/targets/{targetId}/actions/{actionId}", target.getControllerId(), action.getContent().get(0).getId()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath("$.rollout", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("$.rolloutName", equalTo(rollout.getName())))
                .andExpect(jsonPath("$._links.rollout.href", containsString("/rest/v1/rollouts/" + rollout.getId().intValue())));
    }

    /**
     * Ensures that a post request for creating targets with target type works.
     */
    @Test
    void createTargetsWithTargetType() throws Exception {
        final TargetType type1 = testdataFactory.createTargetType("typeWithDs", Set.of(standardDsType));
        final TargetType type2 = testdataFactory.createTargetType("typeWithOutDs", Set.of(standardDsType));

        final Create test1 = Create.builder().controllerId("id1").name("targetWithoutType")
                .securityToken("token").address("amqp://test123/foobar").description("testid1").build();
        final Create test2 = Create.builder().controllerId("id2").name("targetOfType1")
                .targetType(type1).description("testid2").build();
        final Create test3 = Create.builder().controllerId("id3").name("targetOfType2")
                .targetType(type2).description("testid3").build();
        final String hrefType1 = "http://localhost/rest/v1/targettypes/" + type1.getId();

        final List<Create> targets = List.of(test1, test2, test3);

        mvc.perform(post("/rest/v1/targets").content(JsonBuilder.targets(targets, true)).contentType(APPLICATION_JSON).accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
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
                .andExpect(jsonPath("[2].targetType", equalTo(type2.getId().intValue())))
                .andReturn();

        mvc.perform(get(TARGETS_V1 + "/" + test2.getControllerId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("targetOfType1")))
                .andExpect(jsonPath(JSON_PATH_CONTROLLERID, equalTo("id2")))
                .andExpect(jsonPath(JSON_PATH_TYPE, equalTo(type1.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo("testid2")))
                .andExpect(jsonPath("$._links.targetType.href", equalTo(hrefType1)))
                .andReturn();

        final Target target1 = assertTarget("id1", "targetWithoutType", "testid1");
        assertThat(target1.getTargetType()).isNull();
        assertThat(target1.getSecurityToken()).isEqualTo("token");
        assertThat(target1.getAddress()).hasToString("amqp://test123/foobar");

        final Target target2 = assertTarget("id2", "targetOfType1", "testid2");
        assertThat(target2.getTargetType()).extracting(TargetType::getName).isEqualTo("typeWithDs");

        final Target target3 = assertTarget("id3", "targetOfType2", "testid3");
        assertThat(target3.getTargetType()).extracting(TargetType::getName).isEqualTo("typeWithOutDs");
    }

    /**
     * Ensures that a post request for creating target with target type works.
     */
    @Test
    void createTargetWithExistingTargetType() throws Exception {
        // create target type
        final List<? extends TargetType> targetTypes = testdataFactory.createTargetTypes("targettype", 1);
        assertThat(targetTypes).hasSize(1);

        final Create target = Create.builder().controllerId("targetcontroller").name("testtarget").targetType(targetTypes.get(0)).build();

        final String targetList = JsonBuilder.targets(List.of(target), false);

        // test query target over rest resource
        mvc.perform(post(TARGETS_V1).content(targetList).contentType(APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("[0].controllerId", equalTo("targetcontroller")))
                .andExpect(jsonPath("[0].targetType", equalTo(targetTypes.get(0).getId().intValue())));

        assertThat(targetManagement.getByControllerId("targetcontroller").getTargetType().getId()).isEqualTo(targetTypes.get(0).getId());
    }

    /**
     * Ensures that a put request for updating targets with target type works.
     */
    @Test
    void updateTargetTypeInTarget() throws Exception {
        // create target type
        final List<? extends TargetType> targetTypes = testdataFactory.createTargetTypes("targettype", 2);
        assertThat(targetTypes).hasSize(2);

        final String controllerId = "targetcontroller";
        final Target target = testdataFactory.createTarget(controllerId, "testtarget", targetTypes.get(0));

        assertThat(target).isNotNull();
        assertThat(target.getTargetType().getId()).isEqualTo(targetTypes.get(0).getId());

        // update target over rest resource
        final String body = new JSONObject().put("targetType", targetTypes.get(1).getId().intValue()).toString();

        mvc.perform(put(TARGETS_V1 + "/" + controllerId).content(body).contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("controllerId", equalTo(controllerId)))
                .andExpect(jsonPath("targetType", equalTo(targetTypes.get(1).getId().intValue())));
    }

    /**
     * Ensures that a post request for creating targets with unknown target type fails.
     */
    @Test
    void addingNonExistingTargetTypeInTargetShouldFail() throws Exception {
        final long unknownTargetTypeId = 999;
        final String errorMsg = String.format("TargetType with given identifier {%s} does not exist.", unknownTargetTypeId);

        final Optional<? extends TargetType> targetType = targetTypeManagement.find(unknownTargetTypeId);
        assertThat(targetType).isNotPresent();

        final String controllerId = "targetcontroller";
        final Create target = Create.builder().controllerId(controllerId).name("testtarget").build();

        final String targetList = JsonBuilder.targets(List.of(target), false, unknownTargetTypeId);

        // post target over rest resource
        mvc.perform(post(TARGETS_V1).content(targetList).contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("message", Matchers.containsString(errorMsg)));
    }

    /**
     * Ensures that a post request for assign target type to target works.
     */
    @Test
    void assignTargetTypeToTarget() throws Exception {
        // create target type
        final TargetType targetType = testdataFactory.findOrCreateTargetType("targettype");
        assertThat(targetType).isNotNull();

        // create target
        final String targetControllerId = "targetcontroller";
        final Target target = testdataFactory.createTarget(targetControllerId, "testtarget");
        assertThat(target).isNotNull();

        // assign target type over rest resource
        mvc.perform(post(TARGETS_V1 + "/" + targetControllerId + "/targettype")
                        .content("{\"id\":" + targetType.getId() + "}").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(targetManagement.getByControllerId(targetControllerId).getTargetType().getId()).isEqualTo(targetType.getId());
    }

    /**
     * Ensures that a post request for assign a invalid target type to target fails.
     */
    @Test
    void assignInvalidTargetTypeToTargetFails() throws Exception {
        // Invalid target type ID
        final long invalidTargetTypeId = 999;

        // create target
        final String targetControllerId = "targetcontroller";
        final Target target = testdataFactory.createTarget(targetControllerId, "testtarget");
        assertThat(target).isNotNull();

        // assign invalid target type over rest resource
        mvc.perform(post(TARGETS_V1 + "/" + targetControllerId + "/targettype")
                        .content("{\"id\":" + invalidTargetTypeId + "}").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // verify response json exception message if body does not include id
        // field
        final MvcResult mvcResult = mvc
                .perform(post(TARGETS_V1 + "/" + targetControllerId + "/targettype")
                        .content("{\"unknownfield\":" + invalidTargetTypeId + "}")
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andReturn();
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getExceptionClass()).isEqualTo(ConstraintViolationException.class.getName());
        assertThat(exceptionInfo.getErrorCode()).isEqualTo(SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getKey());
        assertThat(exceptionInfo.getMessage()).contains("targetTypeId");
    }

    /**
     * Ensures that a delete request for unassign target type from target works.
     */
    @Test
    void unassignTargetTypeFromTarget() throws Exception {
        // create target type
        final List<? extends TargetType> targetTypes = testdataFactory.createTargetTypes("targettype", 1);
        assertThat(targetTypes).hasSize(1);

        final String targetControllerId = "targetcontroller";
        final Target target = testdataFactory.createTarget(targetControllerId, "testtarget", targetTypes.get(0));

        assertThat(target).isNotNull();
        assertThat(target.getTargetType().getId()).isEqualTo(targetTypes.get(0).getId());

        // unassign target type over rest resource
        mvc.perform(delete(TARGETS_V1 + "/" + targetControllerId + "/targettype").contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(targetManagement.getByControllerId(targetControllerId).getTargetType()).isNull();
    }

    @Test
    void invalidRequestsOnTargetTypeResource() throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTarget(knownTargetId);
        testdataFactory.createTargetType("targettype", Set.of());

        // GET is not allowed
        mvc.perform(get(TARGETS_V1 + MgmtTargetRestApi.TARGET_ID_TARGETTYPE, knownTargetId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // PUT is not allowed
        mvc.perform(put(TARGETS_V1 + MgmtTargetRestApi.TARGET_ID_TARGETTYPE, knownTargetId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // POST does not exist with path parameter targettype
        mvc.perform(post(TARGETS_V1 + MgmtTargetRestApi.TARGET_ID_TARGETTYPE + "/123", knownTargetId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // DELETE does not exist with path parameter targettype
        mvc.perform(delete(TARGETS_V1 + MgmtTargetRestApi.TARGET_ID_TARGETTYPE + "/123", knownTargetId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // Invalid content
        mvc.perform(post(TARGETS_V1 + MgmtTargetRestApi.TARGET_ID_TARGETTYPE, knownTargetId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // Bad request if id field is missing
        mvc.perform(post(TARGETS_V1 + MgmtTargetRestApi.TARGET_ID_TARGETTYPE, knownTargetId)
                        .content("{\"unknownfield\":123}").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("possibleActiveStates")
    void getAutoConfirmActive(final String initiator, final String remark) throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTarget(knownTargetId);
        confirmationManagement.activateAutoConfirmation(knownTargetId, initiator, remark);

        // GET with all possible responses
        mvc.perform(get(TARGETS_V1 + "/{targetId}/" + AUTO_CONFIRM, knownTargetId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("active", equalTo(true)))
                .andExpect(initiator == null ? jsonPath("initiator").doesNotExist() : jsonPath("initiator", equalTo(initiator)))
                .andExpect(remark == null ? jsonPath("remark").doesNotExist() : jsonPath("remark", equalTo(remark)))
                .andExpect(jsonPath("_links.deactivate").exists())
                .andExpect(jsonPath("_links.activate").doesNotExist());
    }

    @Test
    void getAutoConfirmStateFromTargetsEndpoint() throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTarget(knownTargetId);

        // GET if active
        mvc.perform(get(TARGETS_V1))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].autoConfirmActive").doesNotExist());

        enableConfirmationFlow();

        // GET if not active
        mvc.perform(get(TARGETS_V1))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].autoConfirmActive", equalTo(false)));

        confirmationManagement.activateAutoConfirmation(knownTargetId, "test", "remark");

        // GET if active
        mvc.perform(get(TARGETS_V1))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].autoConfirmActive", equalTo(true)));
    }

    @Test
    void getAutoConfirmNotActive() throws Exception {
        final String knownTargetId = "targetId";

        // GET for not existing target
        mvc.perform(get(TARGETS_V1 + "/{targetId}/" + AUTO_CONFIRM, knownTargetId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        testdataFactory.createTarget(knownTargetId);

        // GET for auto-confirm not active
        mvc.perform(get(TARGETS_V1 + "/{targetId}/" + AUTO_CONFIRM, knownTargetId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("active", equalTo(false)))
                .andExpect(jsonPath("initiator").doesNotExist())
                .andExpect(jsonPath("remark").doesNotExist())
                .andExpect(jsonPath("_links.activate").exists());
    }

    @Test
    void autoConfirmStateReferenceOnTarget() throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTarget(knownTargetId);

        // GET with confirmation flow not active should not expose
        mvc.perform(get(TARGETS_V1 + "/{targetId}", knownTargetId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirmActive").doesNotExist())
                .andExpect(jsonPath("_links.autoConfirm").doesNotExist());

        enableConfirmationFlow();

        // GET with confirmation flow active should expose
        mvc.perform(get(TARGETS_V1 + "/{targetId}", knownTargetId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirmActive").exists())
                .andExpect(jsonPath("_links.autoConfirm").exists());
    }

    /**
     * Verifies that the status code that was reported in the last action status update is correctly exposed via the action.
     */
    @Test
    void lastActionStatusCode() throws Exception {
        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Target target = testdataFactory.createTarget("knownTargetId");
        final Action action = getFirstAssignedAction(assignDistributionSet(dsA, Collections.singletonList(target)));

        // no status update yet -> no status code
        mvc.perform(get(TARGETS_V1 + "/{targetId}/actions/{actionId}", target.getControllerId(), action.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("lastStatusCode").doesNotExist());

        // update action status with status code
        updateActionStatus(action, Status.RUNNING, 100);
        mvc.perform(get(TARGETS_V1 + "/{targetId}/actions/{actionId}", target.getControllerId(), action.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("lastStatusCode", equalTo(100)))
                .andExpect(jsonPath("status", equalTo("running")));

        // update action status without a status code
        updateActionStatus(action, Status.RUNNING, null);
        mvc.perform(get(TARGETS_V1 + "/{targetId}/actions/{actionId}", target.getControllerId(), action.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("lastStatusCode").doesNotExist())
                .andExpect(jsonPath("status", equalTo("running")));

        // update action status with status code
        updateActionStatus(action, Status.ERROR, 432);
        mvc.perform(get(TARGETS_V1 + "/{targetId}/actions/{actionId}", target.getControllerId(), action.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("lastStatusCode", equalTo(432)))
                .andExpect(jsonPath("status", equalTo("error")));
    }

    @Test
    void testDeletionOfLastNTargetActions() throws Exception {
        final Target testTarget = testdataFactory.createTarget("testTarget");

        for (int i = 0; i < 10; i++) {
            final DistributionSet distributionSet = testdataFactory.createDistributionSet();
            assignDistributionSet(distributionSet.getId(), testTarget.getControllerId());
        }

        long actionsPerTarget = actionRepository.countByTargetId(testTarget.getId());
        Assertions.assertEquals(10, actionsPerTarget);
        List<Action> oldActions = deploymentManagement.findActionsByTarget(testTarget.getControllerId(), PAGE).getContent();

        mvc.perform(delete(TARGETS_V1 + "/{targetId}/actions", testTarget.getControllerId()).param("keepLast", "5"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        //the last 5 actions should be left
        List<Action> actions = deploymentManagement.findActionsByTarget(testTarget.getControllerId(), PAGE).getContent();
        Assertions.assertEquals(5, actions.size());
        for (int i = 0; i < 5; i++) {
            // last 5 actions should remain
            Assertions.assertEquals(oldActions.get(i + 5), actions.get(i));
        }
    }

    @Test
    void testThatDeletionOfLastNTargetActionsReturnsBadRequestWhenNeeded() throws Exception {
        final Target testTarget = testdataFactory.createTarget();
        // either numberOfActions or actionIds list should be present
        mvc.perform(delete(TARGETS_V1 + "/{targetId}/actions", testTarget.getControllerId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // both parameters present should also lead to bad request
        mvc.perform(delete(TARGETS_V1 + "/{targetId}/actions", testTarget.getControllerId())
                        .param("keepLast", "5")
                        .content(toJson(List.of(1, 2, 3)))
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeletionOfTargetActionsById() throws Exception {
        final Target testTarget = testdataFactory.createTarget("testTarget");
        for (int i = 0; i < 10; i++) {
            final DistributionSet distributionSet = testdataFactory.createDistributionSet();
            long dsId = distributionSet.getId();
            assignDistributionSet(dsId, testTarget.getControllerId());
        }

        final List<Long> evenActionIds = deploymentManagement.findActionsByTarget(testTarget.getControllerId(), PAGE).getContent()
                .stream()
                .filter(action -> action.getId() % 2 == 0)
                .map(Identifiable::getId).toList();

        mvc.perform(delete(TARGETS_V1 + "/{targetId}/actions", testTarget.getControllerId())
                        .content(toJson(evenActionIds))
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        long remaining = actionRepository.countByTargetId(testTarget.getId());
        Assertions.assertEquals(10 - evenActionIds.size(), remaining);
        List<Action> remainingActions = deploymentManagement.findActionsByTarget(testTarget.getControllerId(), PAGE).getContent();
        remainingActions.forEach(action -> Assertions.assertTrue(action.getId() % 2 != 0));
    }

    private static Stream<Arguments> confirmationOptions() {
        return Stream.of(Arguments.of(true, true), Arguments.of(true, false), Arguments.of(false, true),
                Arguments.of(false, false), Arguments.of(true, null), Arguments.of(false, null));
    }

    private static Stream<Arguments> possibleActiveStates() {
        return Stream.of(Arguments.of("someInitiator", "someRemark"), Arguments.of(null, "someRemark"),
                Arguments.of("someInitiator", null), Arguments.of(null, null));
    }

    private void createTarget(final String controllerId) {
        targetManagement.create(Create.builder().controllerId(controllerId).address(IpUtil.createHttpUri("127.0.0.1").toString()).build());
    }

    private Target assertTarget(final String controllerId, final String name, final String description) {
        final Target target = targetManagement.getByControllerId(controllerId);
        assertThat(target).isNotNull();
        assertThat(target.getName()).isEqualTo(name);
        assertThat(target.getDescription()).isEqualTo(description);
        return target;
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
                mvc.perform(get(TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS)
                                .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                        .andDo(MockMvcResultPrinter.print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("content.[1].id", equalTo(actions.get(1).getId().intValue())))
                        .andExpect(jsonPath("content.[1].type", equalTo("update")))
                        .andExpect(jsonPath("content.[1].active", equalTo(true)))
                        .andExpect(jsonPath("content.[1]._links.self.href",
                                equalTo(generateActionSelfLink(knownTargetId, actions.get(1).getId()))))
                        .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                        .andExpect(jsonPath("content.[0].type", equalTo("cancel")))
                        .andExpect(jsonPath("content.[0].active", equalTo(true)))
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

    private String generateActionSelfLink(final String knownTargetId, final Long actionId) {
        return "http://localhost" + TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId;
    }

    private String generateActionDsLink(final Long dsId) {
        return "http://localhost" + MgmtDistributionSetRestApi.DISTRIBUTIONSETS_V1 + "/" + dsId;
    }

    private String generateCanceledactionreferenceLink(final String knownTargetId, final Action action) {
        return "http://localhost" + TARGETS_V1 + "/" + knownTargetId + "/" + MgmtTarget.TARGET_V1_ACTIONS + "/" + action.getId();
    }

    private String generateStatusreferenceLink(final String knownTargetId, final Long actionId) {
        return "http://localhost" + TARGETS_V1 + "/" + knownTargetId + "/"
                + MgmtTarget.TARGET_V1_ACTIONS + "/" + actionId + "/" + MgmtTarget.TARGET_V1_ACTION_STATUS
                + "?offset=0&limit=50&sort=id%3ADESC";
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverride(final String knownTargetId) {
        return generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(knownTargetId, null, null, null);
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(
            final String knownTargetId, final String schedule, final String duration, final String timezone) {
        final Target target = testdataFactory.createTarget(knownTargetId);

        final Iterator<DistributionSet> sets = testdataFactory.createDistributionSets(2).iterator();
        final DistributionSet one = sets.next();
        final DistributionSet two = sets.next();

        // Update
        if (schedule == null) {
            final List<Target> updatedTargets = assignDistributionSet(one, Collections.singletonList(target))
                    .getAssignedEntity().stream().map(Action::getTarget).toList();
            // 2nd update
            // sleep 10ms to ensure that we can sort by reportedAt
            await().until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSet(two, updatedTargets);
        } else {
            final List<Target> updatedTargets = assignDistributionSetWithMaintenanceWindow(
                    one.getId(), target.getControllerId(), schedule, duration, timezone).getAssignedEntity().stream()
                    .map(Action::getTarget).toList();
            // 2nd update
            // sleep 10ms to ensure that we can sort by reportedAt
            await().until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSetWithMaintenanceWindow(two.getId(), updatedTargets.get(0).getControllerId(), schedule, duration, timezone);
        }

        // two updates, one cancellation
        final List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent();

        assertThat(actions).hasSize(2);
        return actions;
    }

    private void verifyAttributeUpdateCanBeRequested(final String knownTargetId) throws Exception {
        final String body = new JSONObject().put("requestAttributes", true).toString();

        mvc.perform(put(TARGETS_V1 + "/" + knownTargetId).content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(targetManagement.getByControllerId(knownTargetId).isRequestControllerAttributes()).isTrue();
    }

    private void verifyRequestAttributesAttributeIsOptional(final String knownTargetId) throws Exception {
        final String body = new JSONObject().put("description", "verify attribute can be missing").toString();

        mvc.perform(put(TARGETS_V1 + "/" + knownTargetId).content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    private void verifyResettingRequestAttributesIsNotAllowed(final String knownTargetId) throws Exception {
        final String body = new JSONObject().put("requestAttributes", false).toString();

        mvc.perform(put(TARGETS_V1 + "/" + knownTargetId).content(body).contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        assertThat(targetManagement.getByControllerId(knownTargetId).isRequestControllerAttributes()).isTrue();
    }

    private String getCreateTargetsListJsonString(final String controllerId, final String name, final String description) {
        return "[{\"name\":\"" + name + "\",\"controllerId\":\"" + controllerId + "\",\"description\":\"" + description + "\"}]";
    }

    private Target createSingleTarget(final String controllerId, final String name) {
        targetManagement.create(Create.builder().controllerId(controllerId).name(name).description(TARGET_DESCRIPTION_TEST).build());
        return controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, LOCALHOST);
    }

    /**
     * Creating targets with the given amount by setting name, id etc from the
     * alphabet [a-z] using ASCII.
     *
     * @param amount The number of targets to create
     */
    private void createTargetsAlphabetical(final int amount) {
        char character = 'a';
        for (int index = 0; index < amount; index++) {
            final String str = String.valueOf(character);
            targetManagement.create(Create.builder().controllerId(str).name(str).description(str).build());
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
        assignDistributionSet(dsA, Collections.singletonList(tA));
        // verify active action
        final Slice<Action> actionsByTarget = deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE);
        assertThat(actionsByTarget.getContent()).hasSize(1);
        return targetManagement.getByControllerId(tA.getControllerId());
    }

    private void setupTargetWithMetadata(final String knownControllerId, final String knownKey, final String knownValue) {
        testdataFactory.createTarget(knownControllerId);
        targetManagement.createMetadata(knownControllerId, Map.of(knownKey, knownValue));
    }

    private Action updateActionStatus(final Action action, final Status status, final Integer statusCode) {
        return updateActionStatus(action, status, statusCode, null);
    }

    private Action updateActionStatus(final Action action, final Status status, final Integer statusCode, final String message) {
        assertThat(action).isNotNull();
        assertThat(status).isNotNull();

        final Action.ActionStatusCreate.ActionStatusCreateBuilder actionStatus = Action.ActionStatusCreate.builder().actionId(action.getId());
        actionStatus.status(status);
        if (statusCode != null) {
            actionStatus.code(statusCode);
        }
        if (message != null) {
            actionStatus.messages(List.of(message));
        }

        return controllerManagement.addUpdateActionStatus(actionStatus.build());
    }
}
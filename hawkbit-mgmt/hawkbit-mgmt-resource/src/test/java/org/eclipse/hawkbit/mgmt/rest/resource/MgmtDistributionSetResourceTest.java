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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.jayway.jsonpath.JsonPath;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssignment;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.resource.util.ResourceUtility;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement.Update;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.Create;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.Assert;

/**
 * Feature: Component Tests - Management API<br/>
 * Story: Distribution Set Resource
 */
class MgmtDistributionSetResourceTest extends AbstractManagementApiIntegrationTest {

    @Autowired
    ActionRepository actionRepository;

    /**
     * This test verifies the call of all Software Modules that are assigned to a Distribution Set through the RESTful API.
     */
    @Test
    void getSoftwareModules() throws Exception {
        // Create DistributionSet with three software modules
        final DistributionSet set = testdataFactory.createDistributionSet("SMTest");
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(set.getModules().size())));
    }

    /**
     * Handles the GET request of retrieving assigned software modules of a single distribution set within SP with given page size and offset including sorting by version descending and filter down to all sets which name starts with 'one'.
     */
    @Test
    void getSoftwareModulesWithParameters() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        // post assignment
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM")
                        .param("offset", "1").param("limit", "2").param("sort", "version:DESC").param("q", "name==one*")
                        .accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    /**
     * This test verifies the deletion of a assigned Software Module of a Distribution Set can not be achieved when that Distribution Set has been assigned or installed to a target.
     */
    @Test
    void failToDeleteWhenDistributionSetInUse() throws Exception {
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules("Eris", "560a");
        final List<Long> smIDs = new ArrayList<>();
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        smIDs.add(sm.getId());
        final JSONArray smList = new JSONArray();
        for (final Long smID : smIDs) {
            smList.put(new JSONObject().put("id", smID));
        }
        // post assignment
        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                        .contentType(APPLICATION_JSON).content(smList.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        // create targets and assign DisSet to target
        final String[] knownTargetIds = new String[] { "1", "2" };
        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIds) {
            testdataFactory.createTarget(targetId);
            list.put(new JSONObject().put("id", Long.valueOf(targetId)));
        }
        assignDistributionSet(disSet.getId(), knownTargetIds[0]);
        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", equalTo(knownTargetIds.length - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(knownTargetIds.length)));

        // try to delete the Software Module from DistSet that has been assigned to the target - hence locked.
        mvc.perform(delete(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM/" + smIDs.get(0))
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_LOCKED.getKey())));
    }

    /**
     * This test verifies that the assignment of a Software Module to a Distribution Set can not be achieved when that Distribution Set has been assigned or installed to a target.
     */
    @Test
    void failToAssignWhenAssigningToUsedDistributionSet() throws Exception {
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules("Mars", "686,980");
        final List<Long> smIDs = new ArrayList<>();
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        smIDs.add(sm.getId());
        final JSONArray smList = new JSONArray();
        for (final Long smID : smIDs) {
            smList.put(new JSONObject().put("id", smID));
        }
        // post assignment
        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                        .contentType(APPLICATION_JSON).content(smList.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        // create Targets
        final String[] knownTargetIds = new String[] { "1", "2" };
        final JSONArray list = createTargetAndJsonArray(null, null, null, null, null, knownTargetIds);
        // assign DisSet to target and test assignment
        assignDistributionSet(disSet.getId(), knownTargetIds[0]);
        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", equalTo(knownTargetIds.length - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(knownTargetIds.length)));

        // Create another SM and post assignment
        final SoftwareModule sm2 = testdataFactory.createSoftwareModuleApp();
        final JSONArray smList2 = new JSONArray();
        smList2.put(new JSONObject().put("id", sm2.getId()));

        // fail because locked
        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                        .contentType(APPLICATION_JSON).content(smList2.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_LOCKED.getKey())));
    }

    /**
     * This test verifies the assignment of Software Modules to a Distribution Set through the RESTful API.
     */
    @Test
    void assignSoftwareModuleToDistributionSet() throws Exception {
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules("Jupiter", "398,88");
        // Test if size is 0
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(disSet.getModules().size())));
        // create Software Modules
        final List<Long> smIDs = Arrays.asList(testdataFactory.createSoftwareModuleOs().getId(),
                testdataFactory.createSoftwareModuleApp().getId());

        // post assignment
        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                        .contentType(APPLICATION_JSON).content(toJson(smIDs.stream().map(MgmtId::new).toList())))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
        // Test if size is 3
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(smIDs.size())));

        // verify quota enforcement
        final int maxSoftwareModules = quotaManagement.getMaxSoftwareModulesPerDistributionSet();
        final List<Long> moduleIDs = new ArrayList<>();
        for (int i = 0; i < maxSoftwareModules + 1; ++i) {
            moduleIDs.add(testdataFactory.createSoftwareModuleApp("sm" + i).getId());
        }

        // post assignment
        final String jsonIDs = toJson(moduleIDs.subList(0, maxSoftwareModules - smIDs.size()).stream().map(MgmtId::new).toList());
        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                        .contentType(APPLICATION_JSON).content(jsonIDs))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
        // test if size corresponds with quota
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM?limit={limit}", maxSoftwareModules * 2))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(maxSoftwareModules)));

        // post one more to cause the quota to be exceeded
        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                        .contentType(APPLICATION_JSON)
                        .content(toJson(Stream.of(moduleIDs.get(moduleIDs.size() - 1)).map(MgmtId::new).toList())))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

        // verify quota is also enforced for bulk uploads
        final DistributionSet disSet2 = testdataFactory.createDistributionSetWithNoSoftwareModules("Saturn", "4.0");
        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet2.getId() + "/assignedSM")
                        .contentType(APPLICATION_JSON).content(toJson(moduleIDs.stream().map(MgmtId::new).toList())))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

        // verify size is still 0
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet2.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(0)));
    }

    /**
     * This test verifies the removal of Software Modules of a Distribution Set through the RESTful API.
     */
    @Test
    void unassignSoftwareModuleFromDistributionSet() throws Exception {
        // Create DistributionSet with three software modules
        final DistributionSet set = testdataFactory.createDistributionSet("Venus");
        int amountOfSM = set.getModules().size();
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(amountOfSM)));
        // test the removal of all software modules one by one
        for (final SoftwareModule softwareModule : set.getModules()) {
            final Long smId = softwareModule.getId();
            mvc.perform(delete(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM/" + smId))
                    .andExpect(status().isNoContent());
            mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM"))
                    .andDo(MockMvcResultPrinter.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size", equalTo(--amountOfSM)));
        }
    }

    /**
     * Ensures that multi target assignment through API is reflected by the repository.
     */
    @Test
    void assignMultipleTargetsToDistributionSet() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(null, null, null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", equalTo(knownTargetIds.length - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(knownTargetIds.length)));

        assertThat(targetManagement.findByAssignedDistributionSet(createdDs.getId(), PAGE).getContent())
                .as("Five targets in repository have DS assigned").hasSize(5);
    }

    /**
     * Ensures that targets can be assigned even if the specified controller IDs are in different case (e.g. 'TARGET1' instead of 'target1'.
     */
    @Test
    void assignTargetsToDistributionSetIgnoreCase() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        // prepare targets
        final String[] knownTargetIds = new String[] { "64-da-a0-02-43-8b", "Trg1", "target2", "target4" };
        final String[] knownTargetIdDifferentCase = new String[] { "64-DA-A0-02-43-8b", "TRG1", "TarGET2", "target4" };
        for (final String targetId : knownTargetIds) {
            testdataFactory.createTarget(targetId);
        }
        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIdDifferentCase) {
            list.put(new JSONObject().put("id", targetId).put("type", "forced"));
        }

        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk());
        // we just need to make sure that no error 500 is returned
    }

    /**
     * Trying to create a DS from already marked as deleted type - should get as response 400 Bad Request
     */
    @Test
    void createDsFromAlreadyMarkedAsDeletedType() throws Exception {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModule("exampleKey");
        final DistributionSetType type = testdataFactory.findOrCreateDistributionSetType(
                "testKey", "testType", List.of(softwareModule.getType()), List.of());
        final DistributionSet ds = testdataFactory.createDistributionSet("dsName", "dsVersion", type, List.of(softwareModule));
        final Target target = testdataFactory.createTarget("exampleControllerId");

        assignDistributionSet(ds, target);

        //soft delete ds type
        distributionSetTypeManagement.delete(type.getId());

        // check if the ds type is marked as deleted
        final Optional<? extends DistributionSetType> opt = distributionSetTypeManagement.findByKey(type.getKey());
        if (opt.isEmpty()) {
            throw new AssertionError("The Optional object of distribution set type should not be empty!");
        }
        final DistributionSetType reloaded = opt.get();
        Assert.isTrue(reloaded.isDeleted(), "Distribution Set Type not marked as deleted!");

        //request for ds creation of type which is already marked as deleted - should return bad request
        final DistributionSetManagement.Create generated = testdataFactory.generateDistributionSet(
                "stanTest", "2", reloaded, Collections.singletonList(softwareModule));
        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/distributionsets")
                        .content(toJson(toMgmtDistributionSetPost(List.of(generated))))
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertEquals("jakarta.validation.ValidationException", exceptionInfo.getExceptionClass());
        assertTrue(exceptionInfo.getMessage().contains("Distribution Set Type already deleted"));
    }

    /**
     * Ensures that multi target assignment is protected by our getMaxTargetDistributionSetAssignmentsPerManualAssignment quota.
     */
    @Test
    void assignMultipleTargetsToDistributionSetUntilQuotaIsExceeded() throws Exception {
        final int maxActions = quotaManagement.getMaxTargetDistributionSetAssignmentsPerManualAssignment();
        final List<Target> targets = testdataFactory.createTargets(maxActions + 1);
        final DistributionSet ds = testdataFactory.createDistributionSet();

        final JSONArray payload = new JSONArray();
        targets.forEach(trg -> {
            try {
                payload.put(new JSONObject().put("id", trg.getId()));
            } catch (final JSONException e) {
                throw new IllegalStateException(e);
            }
        });

        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + ds.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(payload.toString()))
                .andExpect(status().isTooManyRequests());

        assertThat(targetManagement.findByAssignedDistributionSet(ds.getId(), PAGE).getContent()).isEmpty();
    }

    /**
     * Ensures that the 'max actions per target' quota is enforced if the distribution set assignment of a target is changed permanently
     */
    @Test
    void changeDistributionSetAssignmentForTargetUntilQuotaIsExceeded() throws Exception {
        // create one target
        final Target testTarget = testdataFactory.createTarget("trg1");
        final int maxActions = quotaManagement.getMaxActionsPerTarget();

        // create a set of distribution sets
        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");
        final DistributionSet ds3 = testdataFactory.createDistributionSet("ds3");

        IntStream.range(0, maxActions).forEach(i -> {
            // toggle the distribution set
            assignDistributionSet(i % 2 == 0 ? ds1 : ds2, testTarget);
        });

        // assign our test target to another distribution set and verify that
        // the 'max actions per target' quota is exceeded
        final String json = new JSONArray().put(new JSONObject().put("id", testTarget.getControllerId())).toString();
        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + ds3.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(json))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * Ensures that offline reported multi target assignment through API is reflected by the repository.
     */
    @Test
    void offlineAssignmentOfMultipleTargetsToDistributionSet() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5);
        final JSONArray list = new JSONArray();
        targets.forEach(target -> {
            try {
                list.put(new JSONObject().put("id", target.getControllerId()));
            } catch (final JSONException e) {
                throw new IllegalStateException(e);
            }
        });

        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), targets.get(0).getControllerId());

        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId()
                        + "/assignedTargets?offline=true").contentType(APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", equalTo(targets.size() - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(targets.size())));

        assertThat(targetManagement.findByAssignedDistributionSet(createdDs.getId(), PAGE).getContent())
                .as("Five targets in repository have DS assigned").hasSize(5);

        assertThat(targetManagement.findByInstalledDistributionSet(createdDs.getId(), PAGE).getContent()).hasSize(4);
    }

    /**
     * Assigns multiple targets to distribution set with only maintenance schedule.
     */
    @Test
    void assignMultipleTargetsToDistributionSetWithMaintenanceWindowStartOnly() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(getTestSchedule(0), null, null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Assigns multiple targets to distribution set with only maintenance window duration.
     */
    @Test
    void assignMultipleTargetsToDistributionSetWithMaintenanceWindowEndOnly() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(null, getTestDuration(10), null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Assigns multiple targets to distribution set with valid maintenance window.
     */
    @Test
    void assignMultipleTargetsToDistributionSetWithValidMaintenanceWindow() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(getTestSchedule(10), getTestDuration(10), getTestTimeZone(),
                null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk());
    }

    /**
     * Assigns multiple targets to distribution set with last maintenance window scheduled before current time.
     */
    @Test
    void assignMultipleTargetsToDistributionSetWithMaintenanceWindowEndTimeBeforeStartTime() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(getTestSchedule(-30), getTestDuration(5), getTestTimeZone(),
                null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Assigns multiple targets to distribution set with and without maintenance window.
     */
    @Test
    void assignMultipleTargetsToDistributionSetWithAndWithoutMaintenanceWindow() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIds) {
            testdataFactory.createTarget(targetId);
            if (Integer.parseInt(targetId) % 2 == 0) {
                list.put(new JSONObject().put("id", Long.valueOf(targetId)).put("maintenanceWindow",
                        new JSONObject().put("schedule", getTestSchedule(10)).put("duration", getTestDuration(5))
                                .put("timezone", getTestTimeZone())));
            } else {
                list.put(new JSONObject().put("id", Long.valueOf(targetId)));
            }
        }
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk());
    }

    /**
     * Assigning distribution set to the list of targets with a non-existing one leads to successful assignment of valid targets, while not found targets are silently ignored.
     */
    @Test
    void assignNotExistingTargetToDistributionSet() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        final String[] knownTargetIds = new String[] { "1", "2", "3" };
        final JSONArray assignTargetJson = createTargetAndJsonArray(null, null, null, "forced", null, knownTargetIds);
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        assignTargetJson.put(new JSONObject().put("id", "notexistingtarget").put("type", "forced"));

        mvc.perform(post(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(APPLICATION_JSON).content(assignTargetJson.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.assigned", equalTo(2)))
                .andExpect(jsonPath("$.total", equalTo(3)));
    }

    /**
     * Ensures that assigned targets of DS are returned as reflected by the repository.
     */
    @Test
    void getAssignedTargetsOfDistributionSet() throws Exception {
        // prepare distribution set
        final String knownTargetId = "knownTargetId1";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        testdataFactory.createTarget(knownTargetId);
        assignDistributionSet(createdDs.getId(), knownTargetId);

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.content[0].controllerId", equalTo(knownTargetId)));
    }

    /**
     * Handles the GET request for retrieving assigned targets of a single distribution set with a defined page size and offset, sorted by name in descending order and filtered down to all targets which controllerID starts with 'target'.
     */
    @Test
    void getAssignedTargetsOfDistributionSetWithParameters() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        assignDistributionSet(set, testdataFactory.createTargets(5, "targetMisc", "Test targets for query"))
                .getAssignedEntity();

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedTargets")
                        .param("offset", "1").param("limit", "2").param("sort", "name:DESC")
                        .param("q", "controllerId==target*").accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));

    }

    /**
     * Ensures that assigned targets of DS are returned as persisted in the repository.
     */
    @Test
    void getAssignedTargetsOfDistributionSetIsEmpty() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(0)))
                .andExpect(jsonPath("$.total", equalTo(0)));
    }

    /**
     * Ensures that installed targets of DS are returned as persisted in the repository.
     */
    @Test
    void getInstalledTargetsOfDistributionSet() throws Exception {
        // prepare distribution set
        final String knownTargetId = "knownTargetId1";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final Target createTarget = testdataFactory.createTarget(knownTargetId);
        // create some dummy targets which are not assigned or installed
        testdataFactory.createTarget("dummy1");
        testdataFactory.createTarget("dummy2");
        // assign knownTargetId to distribution set
        assignDistributionSet(createdDs.getId(), knownTargetId);
        // make it in install state
        testdataFactory.sendUpdateActionStatusToTargets(Collections.singletonList(createTarget), Status.FINISHED,
                Collections.singletonList("some message"));

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/installedTargets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.content[0].controllerId", equalTo(knownTargetId)));
    }

    /**
     * Handles the GET request for retrieving installed targets of a single distribution set with a defined page size and offset, sortet by name in descending order and filtered down to all targets which controllerID starts with 'target'.
     */
    @Test
    void getInstalledTargetsOfDistributionSetWithParameters() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        final List<Target> targets = assignDistributionSet(set,
                testdataFactory.createTargets(5, "targetMisc", "Test targets for query")).getAssignedEntity().stream()
                .map(Action::getTarget).toList();
        testdataFactory.sendUpdateActionStatusToTargets(targets, Status.FINISHED, "some message");

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/installedTargets")
                        .param("offset", "1").param("limit", "2").param("sort", "name:DESC")
                        .param("q", "controllerId==target*").accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    /**
     * Ensures that target filters with auto assign DS are returned as persisted in the repository.
     */
    @Test
    void getAutoAssignTargetFiltersOfDistributionSet() throws Exception {
        // prepare distribution set
        final String knownFilterName = "a";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        targetFilterQueryManagement.create(
                Create.builder().name(knownFilterName).query("name==y").autoAssignDistributionSet(createdDs).build());

        // create some dummy target filter queries
        targetFilterQueryManagement.create(Create.builder().name("b").query("name==y").build());
        targetFilterQueryManagement.create(Create.builder().name("c").query("name==y").build());

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/autoAssignTargetFilters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.content[0].name", equalTo(knownFilterName)));
    }

    /**
     * Handles the GET request for retrieving assigned target filter queries of a single distribution set with a defined page size and offset, sorted by name in descending order and filtered down to all targets with a name that ends with '1'.
     */
    @Test
    void ggetAutoAssignTargetFiltersOfDistributionSetWithParameters() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();
        targetFilterQueryManagement.create(Create.builder().name("filter1").query("name==a").autoAssignDistributionSet(set).build());

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/autoAssignTargetFilters")
                        .param("offset", "1").param("limit", "2").param("sort", "name:DESC").param("q", "name==*1")
                        .accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    /**
     * Ensures that an error is returned when the query is invalid.
     */
    @Test
    void getAutoAssignTargetFiltersOfDSWithInvalidFilter() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final String invalidQuery = "unknownField=le=42";

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/autoAssignTargetFilters")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, invalidQuery))
                .andExpect(status().isBadRequest());
    }

    /**
     * Ensures that target filters with auto assign DS are returned according to the query.
     */
    @Test
    void getMultipleAutoAssignTargetFiltersOfDistributionSet() throws Exception {
        final String filterNamePrefix = "filter-";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final String query = "name==" + filterNamePrefix + "*";

        prepareTestFilters(filterNamePrefix, createdDs);

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/autoAssignTargetFilters")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(2)))
                .andExpect(jsonPath("$.content[0].name", equalTo(filterNamePrefix + "1")))
                .andExpect(jsonPath("$.content[1].name", equalTo(filterNamePrefix + "2")));
    }

    /**
     * Ensures that no target filters are returned according to the non matching query.
     */
    @Test
    void getEmptyAutoAssignTargetFiltersOfDistributionSet() throws Exception {
        final String filterNamePrefix = "filter-";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final String query = "name==doesNotExist";

        prepareTestFilters(filterNamePrefix, createdDs);

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/autoAssignTargetFilters")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(0)));
    }

    /**
     * Ensures that DS in repository are listed with proper paging properties.
     */
    @Test
    void getDistributionSetsWithoutAdditionalRequestParameters() throws Exception {
        final int sets = 5;
        createDistributionSetsAlphabetical(sets);
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(sets)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(sets)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(sets)));
    }

    /**
     * Ensures that DS in repository are listed with proper paging results with paging limit parameter.
     */
    @Test
    void getDistributionSetsWithPagingLimitRequestParameter() throws Exception {
        final int sets = 5;
        final int limitSize = 1;
        createDistributionSetsAlphabetical(sets);
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(sets)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    /**
     * Ensures that DS in repository are listed with proper paging results with paging limit and offset parameter.
     */
    @Test
    void getDistributionSetsWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int sets = 5;
        final int offsetParam = 2;
        final int expectedSize = sets - offsetParam;
        createDistributionSetsAlphabetical(sets);
        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(sets)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(sets)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    /**
     * Ensures that multiple DS requested are listed with expected payload.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void getDistributionSets() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findAll(PAGE)).isEmpty();

        DistributionSet set = testdataFactory.createDistributionSet("one");
        set = distributionSetManagement.update(Update.builder().id(set.getId()).version("anotherVersion").requiredMigrationStep(true).build());

        // load also lazy stuff
        set = distributionSetManagement.getWithDetails(set.getId());

        assertThat(distributionSetManagement.findAll(PAGE)).hasSize(1);

        // perform request
        mvc.perform(get("/rest/v1/distributionsets").accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content.[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/distributionsets/" + set.getId())))
                .andExpect(jsonPath("$.content.[0].id", equalTo(set.getId().intValue())))
                .andExpect(jsonPath("$.content.[0].name", equalTo(set.getName())))
                .andExpect(jsonPath("$.content.[0].requiredMigrationStep", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("$.content.[0].description", equalTo(set.getDescription())))
                .andExpect(jsonPath("$.content.[0].type", equalTo(set.getType().getKey())))
                .andExpect(jsonPath("$.content.[0].createdBy", equalTo(set.getCreatedBy())))
                .andExpect(jsonPath("$.content.[0].createdAt", equalTo(set.getCreatedAt())))
                .andExpect(jsonPath("$.content.[0].complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("$.content.[0].lastModifiedBy", equalTo(set.getLastModifiedBy())))
                .andExpect(jsonPath("$.content.[0].lastModifiedAt", equalTo(set.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[0].version", equalTo(set.getVersion())))
                .andExpect(jsonPath("$.content.[0].modules.[?(@.type=='" + runtimeType.getKey() + "')].id",
                        contains(findFirstModuleByType(set, runtimeType).orElseThrow().getId().intValue())))
                .andExpect(jsonPath("$.content.[0].modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(findFirstModuleByType(set, appType).orElseThrow().getId().intValue())))
                .andExpect(jsonPath("$.content.[0].modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(getOsModule(set).intValue())));
    }

    /**
     * Ensures that single DS requested by ID is listed with expected payload.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void getDistributionSet() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        // perform request
        mvc.perform(get("/rest/v1/distributionsets/{dsId}", set.getId()).accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$._links.self.href",
                        equalTo("http://localhost/rest/v1/distributionsets/" + set.getId())))
                .andExpect(jsonPath("$.id", equalTo(set.getId().intValue())))
                .andExpect(jsonPath("$.name", equalTo(set.getName())))
                .andExpect(jsonPath("$.type", equalTo(set.getType().getKey())))
                .andExpect(jsonPath("$.description", equalTo(set.getDescription())))
                .andExpect(jsonPath("$.requiredMigrationStep", equalTo(set.isRequiredMigrationStep())))
                .andExpect(jsonPath("$.createdBy", equalTo(set.getCreatedBy())))
                .andExpect(jsonPath("$.complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("$.deleted", equalTo(set.isDeleted())))
                .andExpect(jsonPath("$.createdAt", equalTo(set.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(set.getLastModifiedBy())))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo(set.getLastModifiedAt())))
                .andExpect(jsonPath("$.version", equalTo(set.getVersion())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + runtimeType.getKey() + "')].id",
                        contains(findFirstModuleByType(set, runtimeType).orElseThrow().getId().intValue())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(findFirstModuleByType(set, appType).orElseThrow().getId().intValue())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(getOsModule(set).intValue())));

    }

    /**
     * Ensures that multiple DS posted to API are created in the repository.
     */
    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    void createDistributionSets() throws Exception {
        assertThat(distributionSetManagement.findAll(PAGE)).isEmpty();
        final SoftwareModule ah = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_APP);
        final SoftwareModule jvm = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_RT);
        final SoftwareModule os = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_OS);

        final long current = System.currentTimeMillis();

        final MvcResult mvcResult = executeMgmtTargetPost(
                testdataFactory.generateDistributionSet("one", "one", standardDsType, List.of(os, jvm, ah)),
                testdataFactory.generateDistributionSet("two", "two", standardDsType, List.of(os, jvm, ah)),
                testdataFactory.generateDistributionSet("three", "three", standardDsType, List.of(os, jvm, ah), true));

        final DistributionSet one = distributionSetManagement
                .getWithDetails(distributionSetManagement.findByRsql("name==one", PAGE).getContent().get(0).getId());
        final DistributionSet two = distributionSetManagement
                .getWithDetails(distributionSetManagement.findByRsql("name==two", PAGE).getContent().get(0).getId());
        final DistributionSet three = distributionSetManagement
                .getWithDetails(distributionSetManagement.findByRsql("name==three", PAGE).getContent().get(0).getId());

        assertThat((Object) JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/distributionsets/" + one.getId());

        assertThat((Object) JsonPath.compile("[0]id").read(mvcResult.getResponse().getContentAsString()))
                .hasToString(String.valueOf(one.getId()));
        assertThat((Object) JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/distributionsets/" + two.getId());

        assertThat((Object) JsonPath.compile("[1]id").read(mvcResult.getResponse().getContentAsString()))
                .hasToString(String.valueOf(two.getId()));
        assertThat((Object) JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()))
                .hasToString("http://localhost/rest/v1/distributionsets/" + three.getId());

        assertThat((Object) JsonPath.compile("[2]id").read(mvcResult.getResponse().getContentAsString()))
                .hasToString(String.valueOf(three.getId()));

        // check in database
        assertThat(distributionSetManagement.findAll(PAGE)).hasSize(3);
        assertThat(one.isRequiredMigrationStep()).isFalse();
        assertThat(two.isRequiredMigrationStep()).isFalse();
        assertThat(three.isRequiredMigrationStep()).isTrue();

        assertThat(one.getCreatedAt()).isGreaterThanOrEqualTo(current);
        assertThat(two.getCreatedAt()).isGreaterThanOrEqualTo(current);
        assertThat(three.getCreatedAt()).isGreaterThanOrEqualTo(current);
    }

    /**
     * Ensures that DS deletion request to API is reflected by the repository.
     */
    @Test
    void deleteUnassignedistributionSet() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findAll(PAGE)).isEmpty();
        final DistributionSet set = testdataFactory.createDistributionSet("one");
        assertThat(distributionSetManagement.findAll(PAGE)).hasSize(1);

        // perform request
        mvc.perform(delete("/rest/v1/distributionsets/{smId}", set.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        // check repository content
        assertThat(distributionSetManagement.findAll(PAGE)).isEmpty();
        assertThat(distributionSetManagement.count()).isZero();
    }

    /**
     * Ensures that DS deletion request to API on an entity that does not exist results in NOT_FOUND.
     */
    @Test
    void deleteDistributionSetThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete("/rest/v1/distributionsets/1234"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * Ensures that assigned DS deletion request to API is reflected by the repository by means of deleted flag set.
     */
    @Test
    void deleteAssignedDistributionSet() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findAll(PAGE)).isEmpty();

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        testdataFactory.createTarget("test");
        assignDistributionSet(set.getId(), "test");

        assertThat(distributionSetManagement.findAll(PAGE)).hasSize(1);

        mvc.perform(get("/rest/v1/distributionsets/{dsId}", set.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", equalTo(false)));

        mvc.perform(delete("/rest/v1/distributionsets/{dsId}", set.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        mvc.perform(get("/rest/v1/distributionsets/{dsId}", set.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", equalTo(true)));

        // check repository content
        assertThat(distributionSetManagement.findAll(PAGE)).isEmpty();
    }

    /**
     * Ensures that DS property update request to API is reflected by the repository.
     */
    @Test
    void updateDistributionSet() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findAll(PAGE)).isEmpty();

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        assertThat(distributionSetManagement.count()).isEqualTo(1);

        final String body = new JSONObject()
                .put("version", "anotherVersion")
                .put("requiredMigrationStep", true)
                .put("deleted", true)
                .toString();

        mvc.perform(put("/rest/v1/distributionsets/{dsId}", set.getId()).content(body)
                        .contentType(APPLICATION_JSON).accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", equalTo("anotherVersion")))
                .andExpect(jsonPath("$.requiredMigrationStep", equalTo(true)))
                .andExpect(jsonPath("$.locked", equalTo(false)))
                .andExpect(jsonPath("$.deleted", equalTo(false)));

        final DistributionSet setupdated = distributionSetManagement.find(set.getId()).get();

        assertThat(setupdated.isRequiredMigrationStep()).isTrue();
        assertThat(setupdated.getVersion()).isEqualTo("anotherVersion");
        assertThat(setupdated.getName()).isEqualTo(set.getName());
        assertThat(setupdated.isDeleted()).isFalse();
    }

    /**
     * Ensures that DS property update on requiredMigrationStep fails if DS is assigned to a target.
     */
    @Test
    void updateRequiredMigrationStepFailsIfDistributionSetIsInUse() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findAll(PAGE)).isEmpty();

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        assignDistributionSet(set.getId(), testdataFactory.createTarget().getControllerId());

        assertThat(distributionSetManagement.count()).isEqualTo(1);

        mvc.perform(put("/rest/v1/distributionsets/{dsId}", set.getId())
                        .content("{\"version\":\"anotherVersion\",\"requiredMigrationStep\":\"true\"}")
                        .contentType(APPLICATION_JSON).accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isLocked());

        final DistributionSet setUpdated = distributionSetManagement.find(set.getId()).get();

        assertThat(setUpdated.isRequiredMigrationStep()).isFalse();
        assertThat(setUpdated.getVersion()).isEqualTo(set.getVersion());
        assertThat(setUpdated.getName()).isEqualTo(set.getName());
    }

    /**
     * Ensures that the server reacts properly to invalid requests (URI, Media Type, Methods) with correct reponses.
     */
    @Test
    void invalidRequestsOnDistributionSetsResource() throws Exception {
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final List<DistributionSetManagement.Create> sets = new ArrayList<>();
        sets.add(DistributionSetManagement.Create.builder()
                .type(set.getType())
                .name(set.getName())
                .version(set.getVersion())
                .build());

        // SM does not exist
        mvc.perform(get("/rest/v1/distributionsets/12345678"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/distributionsets/12345678"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(post("/rest/v1/distributionsets").contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post("/rest/v1/distributionsets").content("sdfjsdlkjfskdjf".getBytes())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final DistributionSetManagement.Create missingName = DistributionSetManagement.Create.builder().build();
        mvc.perform(post("/rest/v1/distributionsets").content(toJson(List.of(missingName)))
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final DistributionSetManagement.Create toLongName =
                testdataFactory.generateDistributionSet(randomString(NamedEntity.NAME_MAX_SIZE + 1));
        mvc.perform(post("/rest/v1/distributionsets").content(toJson(List.of(toLongName)))
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // unsupported media type
        mvc.perform(post("/rest/v1/distributionsets").content(toJson(sets))
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // not allowed methods
        mvc.perform(post("/rest/v1/distributionsets/{smId}", set.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/rest/v1/distributionsets"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/distributionsets"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Ensures that the metadata creation through API is reflected by the repository.
     */
    @Test
    void createMetadata() throws Exception {
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");

        final String knownKey1 = "known.key.1.1";
        final String knownKey2 = "knownKey2";

        final String knownValue1 = "knownValue1";
        final String knownValue2 = "knownValue2";

        final JSONArray metaData1 = new JSONArray();
        metaData1.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        metaData1.put(new JSONObject().put("key", knownKey2).put("value", knownValue2));

        mvc.perform(post("/rest/v1/distributionsets/{dsId}/metadata", testDS.getId())
                        .contentType(APPLICATION_JSON)
                        .content(metaData1.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());

        assertThat(distributionSetManagement.getMetadata(testDS.getId()).get(knownKey1)).isEqualTo(knownValue1);
        assertThat(distributionSetManagement.getMetadata(testDS.getId()).get(knownKey2)).isEqualTo(knownValue2);

        // verify quota enforcement
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerDistributionSet();

        final JSONArray metaData2 = new JSONArray();
        for (int i = 0; i < maxMetaData - metaData1.length() + 1; ++i) {
            metaData2.put(new JSONObject().put("key", knownKey1 + i).put("value", knownValue1 + i));
        }

        mvc.perform(post("/rest/v1/distributionsets/{dsId}/metadata", testDS.getId())
                        .contentType(APPLICATION_JSON)
                        .content(metaData2.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isTooManyRequests());

        // verify that the number of meta-data entries has not changed (we cannot use the PAGE constant here as it tries to sort by ID)
        assertThat(distributionSetManagement.getMetadata(testDS.getId())).hasSize(metaData1.length());
    }

    /**
     * Ensures that a metadata update through API is reflected by the repository.
     */
    @Test
    void updateMetadata() throws Exception {
        // prepare and create metadata for update
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final String updateValue = "valueForUpdate";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        distributionSetManagement.createMetadata(testDS.getId(), Map.of(knownKey, knownValue));

        final JSONObject jsonObject = new JSONObject().put("key", knownKey).put("value", updateValue);

        mvc.perform(put("/rest/v1/distributionsets/{dsId}/metadata/{key}", testDS.getId(), knownKey)
                        .contentType(APPLICATION_JSON)
                        .content(jsonObject.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(distributionSetManagement.getMetadata(testDS.getId()).get(knownKey)).isEqualTo(updateValue);
    }

    /**
     * Ensures that a metadata entry deletion through API is reflected by the repository.
     */
    @Test
    void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        distributionSetManagement.createMetadata(testDS.getId(), Map.of(knownKey, knownValue));

        mvc.perform(delete("/rest/v1/distributionsets/{dsId}/metadata/{key}", testDS.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        // already deleted
        mvc.perform(delete("/rest/v1/distributionsets/{dsId}/metadata/{key}", testDS.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        assertThat(distributionSetManagement.getMetadata(testDS.getId()).get(knownKey)).isNull();
    }

    /**
     * Ensures that DS metadata deletion request to API on an entity that does not exist results in NOT_FOUND.
     */
    @Test
    void deleteMetadataThatDoesNotExistLeadsToNotFound() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        distributionSetManagement.createMetadata(testDS.getId(), Map.of(knownKey, knownValue));

        mvc.perform(delete("/rest/v1/distributionsets/{dsId}/metadata/XXX", testDS.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/distributionsets/1234/metadata/{key}", knownKey))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        assertThat(distributionSetManagement.getMetadata(testDS.getId()).get(knownKey)).isNotNull();
    }

    /**
     * Ensures that a metadata entry selection through API reflects the repository content.
     */
    @Test
    void getMetadataKey() throws Exception {
        // prepare and create metadata
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        distributionSetManagement.createMetadata(testDS.getId(), Map.of(knownKey, knownValue));

        mvc.perform(get("/rest/v1/distributionsets/{dsId}/metadata/{key}", testDS.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("key", equalTo(knownKey)))
                .andExpect(jsonPath("value", equalTo(knownValue)));
    }

    /**
     * Get a paged list of meta data for a distribution set with standard page size.
     */
    @Test
    void getMetadata() throws Exception {
        final int totalMetadata = 4;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        for (int index = 0; index < totalMetadata; index++) {
            distributionSetManagement.createMetadata(testDS.getId(), Map.of(knownKeyPrefix + index, knownValuePrefix + index));
        }

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata", testDS.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON));
    }

    /**
     * Ensures that a DS search with query parameters returns the expected result.
     */
    @Test
    void searchDistributionSetRsql() throws Exception {
        final String dsSuffix = "test";
        final int amount = 10;
        testdataFactory.createDistributionSets(dsSuffix, amount);
        testdataFactory.createDistributionSet("DS1test");
        testdataFactory.createDistributionSet("DS2test");

        final String rsqlFindLikeDs1OrDs2 = "name==DS1test,name==DS2test";

        mvc.perform(get("/rest/v1/distributionsets?q=" + rsqlFindLikeDs1OrDs2))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2)))
                .andExpect(jsonPath("content[0].name", equalTo("DS1test")))
                .andExpect(jsonPath("content[1].name", equalTo("DS2test")));
    }

    /**
     * Ensures that a DS search with complete==true parameter returns only DS that are actually completely filled with mandatory modules.
     */
    @Test
    void filterDistributionSetComplete() throws Exception {
        final int amount = 10;
        final List<String> dsIds = testdataFactory.createDistributionSets(amount).stream()
                .map(Identifiable::getId).map(String::valueOf).toList();
        distributionSetManagement
                .create(DistributionSetManagement.Create.builder()
                        .type(distributionSetTypeManagement.findByKey("os").orElseThrow())
                        .name("incomplete").version("2")
                        .build());

        mvc.perform(get("/rest/v1/distributionsets?q=complete==true"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(10)))
                .andExpect(jsonPath("total", equalTo(10)));

        // and more complex (logical and to comparison conversion) query
        mvc.perform(get("/rest/v1/distributionsets?q=complete==true;valid==true"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(10)))
                .andExpect(jsonPath("total", equalTo(10)));

        // and more complex (case-insensitive) query
        mvc.perform(get("/rest/v1/distributionsets?q=complete==true;valid==true;id=IN=(" + String.join(",", dsIds) + ")"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(10)))
                .andExpect(jsonPath("total", equalTo(10)));
    }

    /**
     * Ensures that a DS assigned target search with controllerId==1 parameter returns only the target with the given ID.
     */
    @Test
    void searchDistributionSetAssignedTargetsRsql() throws Exception {
        // prepare distribution set
        final Set<DistributionSet> createDistributionSetsAlphabetical = createDistributionSetsAlphabetical(1);
        final DistributionSet createdDs = createDistributionSetsAlphabetical.iterator().next();
        // prepare targets
        final Collection<String> knownTargetIds = Arrays.asList("1", "2", "3", "4", "5");

        knownTargetIds.forEach(controllerId -> targetManagement.create(TargetManagement.Create.builder().controllerId(controllerId).build()));

        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds.iterator().next());

        final String rsqlFindTargetId1 = "controllerId==1";

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets?q=" + rsqlFindTargetId1)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].controllerId", equalTo("1")));
    }

    /**
     * Ensures that multi target assignment through API is reflected by the repository in the case of  DOWNLOAD_ONLY.
     */
    @Test
    void assignMultipleTargetsToDistributionSetAsDownloadOnly() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(null, null, null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0], Action.ActionType.DOWNLOAD_ONLY);

        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", createdDs.getId())
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", equalTo(knownTargetIds.length - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(knownTargetIds.length)));

        assertThat(targetManagement.findByAssignedDistributionSet(createdDs.getId(), PAGE).getContent())
                .as("Five targets in repository have DS assigned").hasSize(5);
    }

    /**
     * Ensures that confirmation option is considered in assignment request.
     */
    @ParameterizedTest
    @MethodSource("confirmationOptions")
    void assignTargetsToDistributionSetWithConfirmationOptions(final boolean confirmationFlowActive, final Boolean confirmationRequired)
            throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        // prepare targets
        final String targetId = "1";
        final JSONArray list = createTargetAndJsonArray(null, null, null, null, confirmationRequired, targetId);

        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", createdDs.getId())
                        .contentType(APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", equalTo(1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(0)))
                .andExpect(jsonPath("$.total", equalTo(1)));

        assertThat(
                actionRepository
                        .findAll(byDistributionSetId(createdDs.getId()), PAGE)
                        .map(Action.class::cast).getContent()).hasSize(1)
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
     * A request for assigning a target multiple times results in a Bad Request when multiassignment is disabled.
     */
    @Test
    void multiAssignmentRequestNotAllowedIfDisabled() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONArray body = new JSONArray();
        body.put(getAssignmentObject(targetId, MgmtActionType.SOFT));
        body.put(getAssignmentObject(targetId, MgmtActionType.FORCED));

        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(body.toString())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    /**
     * Identical assignments in a single request are removed when multiassignment is disabled.
     */
    @Test
    void identicalAssignmentInRequestAreRemovedIfMultiassignmentsDisabled() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONArray body = new JSONArray();
        body.put(getAssignmentObject(targetId, MgmtActionType.FORCED));
        body.put(getAssignmentObject(targetId, MgmtActionType.FORCED));

        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(body.toString())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)));
    }

    /**
     * Assigning targets multiple times to a DS in one request works in multi-assignment mode.
     */
    @Test
    void multiAssignment() throws Exception {
        final List<String> targetIds = testdataFactory.createTargets(2).stream().map(Target::getControllerId).toList();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONArray body = new JSONArray();
        body.put(getAssignmentObject(targetIds.get(0), MgmtActionType.FORCED, 56));
        body.put(getAssignmentObject(targetIds.get(0), MgmtActionType.FORCED, 78));
        body.put(getAssignmentObject(targetIds.get(1), MgmtActionType.FORCED, 67));
        body.put(getAssignmentObject(targetIds.get(1), MgmtActionType.SOFT, 34));

        enableMultiAssignments();
        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(body.toString())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(body.length())));
    }

    /**
     * An assignment request containing a weight is only accepted when weight is valide and multi assignment is on.
     */
    @Test
    void weightValidation() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int weight = 78;

        final JSONArray bodyValide = new JSONArray().put(getAssignmentObject(targetId, MgmtActionType.FORCED, weight));
        final JSONArray bodyInvalide = new JSONArray()
                .put(getAssignmentObject(targetId, MgmtActionType.FORCED, Action.WEIGHT_MIN - 1));

        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(bodyValide.toString())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        enableMultiAssignments();
        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(bodyInvalide.toString())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.repo.constraintViolation")));
        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(bodyValide.toString())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).get().toList();
        assertThat(actions).size().isEqualTo(2);
        assertThat(actions.get(0).getWeight()).get().isEqualTo(weight);
    }

    /**
     * Request to get the count of all Rollouts by status for specific Distribution set
     */
    @Test
    void statisticsForRolloutsCountByStatus() throws Exception {
        testdataFactory.createTargets("targets", 4);
        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");

        testdataFactory.createRolloutByVariables("rollout1", "description", 1, "name==targets*", ds1, "50", "5", false);
        Rollout rollout = testdataFactory.createRolloutByVariables("rollout2", "description", 1, "name==targets*", ds1, "50", "5", false);
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{ds}/statistics/rollouts", ds1.getId()).contentType(
                        APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("rollouts.READY", equalTo(1)))
                .andExpect(jsonPath("rollouts.RUNNING", equalTo(1)))
                .andExpect(jsonPath("rollouts.total", equalTo(2)))
                .andExpect(jsonPath("actions").doesNotExist())
                .andExpect(jsonPath("totalAutoAssignments").doesNotExist());

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{ds}/statistics/rollouts", ds2.getId())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("rollouts").doesNotExist())
                .andExpect(jsonPath("actions").doesNotExist())
                .andExpect(jsonPath("totalAutoAssignments").doesNotExist());
    }

    /**
     * Request to get the count of all Actions by status for specific Distribution set
     */
    @Test
    void statisticsForActionsCountByStatus() throws Exception {
        testdataFactory.createTargets("targets", 4);

        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");

        Rollout rollout = testdataFactory.createRolloutByVariables("rollout", "description",
                1, "name==targets*", ds1, "50", "5", false);
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{ds}/statistics/actions", ds1.getId())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("actions.RUNNING", equalTo(4)))
                .andExpect(jsonPath("actions.total", equalTo(4)))
                .andExpect(jsonPath("rollouts").doesNotExist())
                .andExpect(jsonPath("totalAutoAssignments").doesNotExist());

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{ds}/statistics/actions", ds2.getId())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("rollouts").doesNotExist())
                .andExpect(jsonPath("actions").doesNotExist())
                .andExpect(jsonPath("totalAutoAssignments").doesNotExist());
    }

    /**
     * Request to get the count of all Auto Assignments for specific Distribution set
     */
    @Test
    void statisticsForAutoAssignmentsCount() throws Exception {
        testdataFactory.createTargets("targets", 4);
        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");

        targetFilterQueryManagement.create(
                Create.builder().name("test filter 1").autoAssignDistributionSet(ds1).query("name==targets*").build());

        targetFilterQueryManagement.create(
                Create.builder().name("test filter 2").autoAssignDistributionSet(ds1).query("name==targets*").build());

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{ds}/statistics/autoassignments", ds1.getId()).contentType(
                        APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("totalAutoAssignments", equalTo(2)))
                .andExpect(jsonPath("rollouts").doesNotExist())
                .andExpect(jsonPath("actions").doesNotExist());

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{ds}/statistics/autoassignments", ds2.getId()).contentType(
                        APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("rollouts").doesNotExist())
                .andExpect(jsonPath("actions").doesNotExist())
                .andExpect(jsonPath("totalAutoAssignments").doesNotExist());
    }

    /**
     * Request to get full Statistics for specific Distribution set
     */
    @Test
    void statisticsForDistributionSet() throws Exception {
        testdataFactory.createTargets("targets", 4);
        testdataFactory.createTargets("autoAssignments", 4);
        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");

        targetFilterQueryManagement.create(
                Create.builder().name("test filter 1").autoAssignDistributionSet(ds1).query("name==autoAssignments*").build());

        Rollout rollout = testdataFactory.createRolloutByVariables("rollout", "description", 1, "name==targets*", ds1, "50", "5", false);
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{ds}/statistics", ds1.getId())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("totalAutoAssignments", equalTo(1)))
                .andExpect(jsonPath("actions.RUNNING", equalTo(4)))
                .andExpect(jsonPath("actions.total", equalTo(4)))
                .andExpect(jsonPath("rollouts.RUNNING", equalTo(1)))
                .andExpect(jsonPath("rollouts.total", equalTo(1)));

        mvc.perform(get(DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{ds}/statistics/autoassignments", ds2.getId())
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("rollouts").doesNotExist())
                .andExpect(jsonPath("actions").doesNotExist())
                .andExpect(jsonPath("totalAutoAssignments").doesNotExist());
    }

    /**
     * Verify invalidation of distribution sets that removes distribution sets from auto assignments, stops rollouts and cancels assignments
     */
    @Test
    void softInvalidateDistributionSet() throws Exception {
        DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5, "invalidateDistributionSet");
        // the distribution set is locked and the old instance become stale
        distributionSet = assignDistributionSet(distributionSet, targets).getDistributionSet();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                Create.builder().name("invalidateDistributionSet").query("name==*").autoAssignDistributionSet(distributionSet).build());
        final Rollout rollout = testdataFactory.createRolloutByVariables(
                "invalidateDistributionSet", "desc", 2, "name==*", distributionSet, "50", "80");

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("actionCancelationType", "soft");

        mvc.perform(post("/rest/v1/distributionsets/{ds}/invalidate", distributionSet.getId())
                        .contentType(APPLICATION_JSON)
                        .content(jsonObject.toString()))
                .andExpect(status().isNoContent());

        assertThat(targetFilterQueryManagement.find(targetFilterQuery.getId()).get().getAutoAssignDistributionSet())
                .isNull();
        assertThat(rolloutManagement.get(rollout.getId()).getStatus()).isIn(RolloutStatus.STOPPING,
                RolloutStatus.STOPPED);
        //then enforce executor to stop the rollout and check
        rolloutHandler.handleAll();
        assertThat(rolloutManagement.get(rollout.getId()).getStatus()).isIn(RolloutStatus.STOPPED);

        for (final Target target : targets) {
            assertThat(targetManagement.find(target.getId()).get().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.PENDING);
            assertThat(deploymentManagement.findActionsByTarget(target.getControllerId(), PageRequest.of(0, 100))
                    .getNumberOfElements()).isEqualTo(1);
            assertThat(deploymentManagement.findActionsByTarget(target.getControllerId(), PageRequest.of(0, 100))
                    .getContent().get(0).getStatus()).isEqualTo(Status.CANCELING);
        }
    }

    @Test
    void forceInvalidateDistributionSet() throws Exception {
        DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5, "invalidateDistributionSet");
        distributionSet = assignDistributionSet(distributionSet, targets).getDistributionSet();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                Create.builder().name("invalidateDistributionSet").query("name==*").autoAssignDistributionSet(distributionSet).build());
        final Rollout rollout = testdataFactory.createRolloutByVariables("invalidateDistributionSet", "desc", 2,
                "name==*", distributionSet, "50", "80");

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("actionCancelationType", "force");

        mvc.perform(post("/rest/v1/distributionsets/{ds}/invalidate", distributionSet.getId())
                        .content(jsonObject.toString()).contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(targetFilterQueryManagement.find(targetFilterQuery.getId()).get().getAutoAssignDistributionSet()).isNull();
        final Long rolloutId = rollout.getId();
        assertThat(rolloutManagement.get(rolloutId).getStatus()).isIn(RolloutStatus.DELETING, RolloutStatus.DELETED);
        //then enforce executor to stop the rollout and check
        rolloutHandler.handleAll();
        // assert rollout is deleted
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> rolloutManagement.get(rolloutId));

        for (final Target target : targets) {
            assertThat(targetManagement.get(target.getId()).getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.IN_SYNC);
            assertThat(deploymentManagement.findActionsByTarget(target.getControllerId(), PageRequest.of(0, 100))
                    .getNumberOfElements()).isEqualTo(1);
            assertThat(deploymentManagement.findActionsByTarget(target.getControllerId(), PageRequest.of(0, 100))
                    .getContent().get(0).getStatus()).isEqualTo(Status.CANCELED);
        }
    }

    @Test
    void invalidateDistributionSetWithNoneCancellation() throws Exception {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5, "invalidateDistributionSet");
        Rollout rollout = testdataFactory.createRolloutByVariables("invalidateDistributionSet", "desc", 1,
                "name==*", distributionSet, "50", "80");
        rollout = testdataFactory.startRollout(rollout);

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("actionCancelationType", "none");

        mvc.perform(post("/rest/v1/distributionsets/{ds}/invalidate", distributionSet.getId())
                        .content(jsonObject.toString()).contentType(APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(rolloutManagement.get(rollout.getId()).getStatus()).isIn(RolloutStatus.RUNNING);

        for (final Target target : targets) {
            assertThat(targetManagement.get(target.getId()).getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
            assertThat(deploymentManagement.findActionsByTarget(target.getControllerId(), PageRequest.of(0, 100))
                    .getNumberOfElements()).isEqualTo(1);
            assertThat(deploymentManagement.findActionsByTarget(target.getControllerId(), PageRequest.of(0, 100))
                    .getContent().get(0).getStatus()).isEqualTo(Status.RUNNING);
        }
    }

    /**
     * Tests the lock. It is verified that the distribution set can be marked as locked through update operation.
     */
    @Test
    void lockDistributionSet() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findAll(PAGE)).isEmpty();

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        assertThat(distributionSetManagement.count()).isEqualTo(1);
        assertThat(set.isLocked()).as("Created distribution set should not be locked").isFalse();

        // lock
        final String body = new JSONObject().put("locked", true).toString();
        mvc.perform(put("/rest/v1/distributionsets/{dsId}", set.getId()).content(body)
                        .contentType(APPLICATION_JSON).accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked", equalTo(true)));

        final DistributionSet updatedSet = distributionSetManagement.find(set.getId()).get();
        assertThat(updatedSet.isLocked()).isTrue();
    }

    /**
     * Tests the unlock.
     */
    @Test
    void unlockDistributionSet() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findAll(PAGE)).isEmpty();

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        assertThat(distributionSetManagement.count()).isEqualTo(1);
        distributionSetManagement.lock(set);
        assertThat(distributionSetManagement.find(set.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, set.getId())).isLocked())
                .as("Distribution set should be locked")
                .isTrue();

        // unlock
        final String body = new JSONObject().put("locked", false).toString();
        mvc.perform(put("/rest/v1/distributionsets/{dsId}", set.getId()).content(body)
                        .contentType(APPLICATION_JSON).accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked", equalTo(false)));

        final DistributionSet updatedSet = distributionSetManagement.find(set.getId()).get();
        assertThat(updatedSet.isLocked()).isFalse();
    }

    private static Stream<Arguments> confirmationOptions() {
        return Stream.of(Arguments.of(true, true), Arguments.of(true, false), Arguments.of(false, true),
                Arguments.of(false, false), Arguments.of(true, null), Arguments.of(false, null));
    }

    private JSONArray createTargetAndJsonArray(final String schedule, final String duration, final String timezone,
            final String type, final Boolean confirmationRequired, final String... targetIds) throws Exception {
        final JSONArray result = new JSONArray();
        for (final String targetId : targetIds) {
            testdataFactory.createTarget(targetId);

            final JSONObject targetJsonObject = new JSONObject();
            result.put(targetJsonObject);
            targetJsonObject.put("id", Long.valueOf(targetId));
            if (type != null) {
                targetJsonObject.put("type", type);
            }

            if (schedule != null || duration != null || timezone != null) {
                final JSONObject maintenanceJsonObject = new JSONObject();
                if (schedule != null) {
                    maintenanceJsonObject.put("schedule", schedule);
                }
                if (duration != null) {
                    maintenanceJsonObject.put("duration", duration);
                }
                if (timezone != null) {
                    maintenanceJsonObject.put("timezone", timezone);
                }
                targetJsonObject.put("maintenanceWindow", maintenanceJsonObject);
            }
            if (confirmationRequired != null) {
                targetJsonObject.put("confirmationRequired", confirmationRequired);
            }
        }
        return result;
    }

    private void prepareTestFilters(final String filterNamePrefix, final DistributionSet createdDs) {
        // create target filter queries that should be found
        targetFilterQueryManagement.create(Create.builder().name(filterNamePrefix + "1")
                .query("name==y").autoAssignDistributionSet(createdDs).build());
        targetFilterQueryManagement.create(Create.builder().name(filterNamePrefix + "2")
                .query("name==y").autoAssignDistributionSet(createdDs).build());
        // create some dummy target filter queries
        targetFilterQueryManagement.create(Create.builder().name(filterNamePrefix + "b").query("name==y").build());
        targetFilterQueryManagement.create(Create.builder().name(filterNamePrefix + "c").query("name==y").build());
    }

    private MvcResult executeMgmtTargetPost(
            final DistributionSetManagement.Create one,
            final DistributionSetManagement.Create two,
            final DistributionSetManagement.Create three) throws Exception {
        return mvc
                .perform(post("/rest/v1/distributionsets").content(toJson(toMgmtDistributionSetPost(List.of(one, two, three))))
                        .contentType(APPLICATION_JSON).accept(APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0]name", equalTo(one.getName())))
                .andExpect(jsonPath("[0]description", equalTo(one.getDescription())))
                .andExpect(jsonPath("[0]type", equalTo(standardDsType.getKey())))
                .andExpect(jsonPath("[0]createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[0]version", equalTo(one.getVersion())))
                .andExpect(jsonPath("[0]complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("[0]requiredMigrationStep", equalTo(one.getRequiredMigrationStep())))
                .andExpect(jsonPath("[0].modules.[?(@.type=='" + runtimeType.getKey() + "')].id",
                        contains(findFirstModuleByType(one, runtimeType).get().getId().intValue())))
                .andExpect(jsonPath("[0].modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(findFirstModuleByType(one, appType).get().getId().intValue())))
                .andExpect(jsonPath("[0].modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(findFirstModuleByType(one, osType).get().getId().intValue())))
                .andExpect(jsonPath("[1]name", equalTo(two.getName())))
                .andExpect(jsonPath("[1]description", equalTo(two.getDescription())))
                .andExpect(jsonPath("[1]complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("[1]type", equalTo(standardDsType.getKey())))
                .andExpect(jsonPath("[1]createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1]version", equalTo(two.getVersion())))
                .andExpect(jsonPath("[1].modules.[?(@.type=='" + runtimeType.getKey() + "')].id",
                        contains(findFirstModuleByType(two, runtimeType).get().getId().intValue())))
                .andExpect(jsonPath("[1].modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(findFirstModuleByType(two, appType).get().getId().intValue())))
                .andExpect(jsonPath("[1].modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(findFirstModuleByType(two, osType).get().getId().intValue())))
                .andExpect(jsonPath("[1]requiredMigrationStep", equalTo(two.getRequiredMigrationStep())))
                .andExpect(jsonPath("[2]name", equalTo(three.getName())))
                .andExpect(jsonPath("[2]description", equalTo(three.getDescription())))
                .andExpect(jsonPath("[2]complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("[2]type", equalTo(standardDsType.getKey())))
                .andExpect(jsonPath("[2]createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2]version", equalTo(three.getVersion())))
                .andExpect(jsonPath("[2].modules.[?(@.type=='" + runtimeType.getKey() + "')].id",
                        contains(findFirstModuleByType(three, runtimeType).get().getId().intValue())))
                .andExpect(jsonPath("[2].modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(findFirstModuleByType(three, appType).get().getId().intValue())))
                .andExpect(jsonPath("[2].modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(findFirstModuleByType(three, osType).get().getId().intValue())))
                .andExpect(jsonPath("[2]requiredMigrationStep", equalTo(three.getRequiredMigrationStep())))
                .andReturn();
    }

    private static List<MgmtDistributionSetRequestBodyPut> toMgmtDistributionSetPost(final List<DistributionSetManagement.Create> creates) {
        return creates.stream()
                .map(create ->
                        new MgmtDistributionSetRequestBodyPost()
                                .setType(create.getType().getKey())
                                .setModules(create.getModules().stream()
                                        .map(module -> new MgmtSoftwareModuleAssignment().setId(module.getId()))
                                        .map(MgmtSoftwareModuleAssignment.class::cast)
                                        .toList())
                                .setName(create.getName())
                                .setDescription(create.getDescription())
                                .setVersion(create.getVersion())
                                .setRequiredMigrationStep(create.getRequiredMigrationStep()))
                .toList();
    }

    private Set<DistributionSet> createDistributionSetsAlphabetical(final int amount) {
        char character = 'a';
        final Set<DistributionSet> created = new HashSet<>(amount);
        for (int index = 0; index < amount; index++) {
            final String str = String.valueOf(character);
            created.add(testdataFactory.createDistributionSet(str));
            character++;
        }
        return created;
    }
}
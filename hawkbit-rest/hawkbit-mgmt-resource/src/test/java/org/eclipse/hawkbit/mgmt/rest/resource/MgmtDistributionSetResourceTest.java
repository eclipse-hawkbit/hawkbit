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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.JsonPath;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

@Feature("Component Tests - Management API")
@Story("Distribution Set Resource")
public class MgmtDistributionSetResourceTest extends AbstractManagementApiIntegrationTest {

    @Test
    @Description("This test verifies the call of all Software Modules that are assiged to a Distribution Set through the RESTful API.")
    public void getSoftwaremodules() throws Exception {
        // Create DistributionSet with three software modules
        final DistributionSet set = testdataFactory.createDistributionSet("SMTest");
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(set.getModules().size())));
    }

    @Test
    @Description("This test verifies the deletion of a assigned Software Module of a Distribution Set can not be achieved when that Distribution Set has been assigned or installed to a target.")
    public void deleteFailureWhenDistributionSetInUse() throws Exception {

        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules("Eris", "560a");
        final List<Long> smIDs = new ArrayList<>();
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        smIDs.add(sm.getId());
        final JSONArray smList = new JSONArray();
        for (final Long smID : smIDs) {
            smList.put(new JSONObject().put("id", Long.valueOf(smID)));
        }
        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                .contentType(MediaType.APPLICATION_JSON).content(smList.toString())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // create targets and assign DisSet to target
        final String[] knownTargetIds = new String[] { "1", "2" };
        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIds) {
            testdataFactory.createTarget(targetId);
            list.put(new JSONObject().put("id", Long.valueOf(targetId)));
        }
        assignDistributionSet(disSet.getId(), knownTargetIds[0]);
        mvc.perform(
                post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", equalTo(knownTargetIds.length - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(knownTargetIds.length)));

        // try to delete the Software Module from DistSet that has been assigned
        // to the target.
        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM/"
                + smIDs.get(0)).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.entityreadonly")));
    }

    @Test
    @Description("This test verifies that the assignment of a Software Module to a Distribution Set can not be achieved when that Distribution Set has been assigned or installed to a target.")
    public void assignmentFailureWhenAssigningToUsedDistributionSet() throws Exception {

        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules("Mars", "686,980");
        final List<Long> smIDs = new ArrayList<>();
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        smIDs.add(sm.getId());
        final JSONArray smList = new JSONArray();
        for (final Long smID : smIDs) {
            smList.put(new JSONObject().put("id", Long.valueOf(smID)));
        }
        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                .contentType(MediaType.APPLICATION_JSON).content(smList.toString())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // create Targets
        final String[] knownTargetIds = new String[] { "1", "2" };
        final JSONArray list = createTargetAndJsonArray(null, null, null, null, knownTargetIds);
        // assign DisSet to target and test assignment
        assignDistributionSet(disSet.getId(), knownTargetIds[0]);
        mvc.perform(
                post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", equalTo(knownTargetIds.length - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(knownTargetIds.length)));

        // Create another SM and post assignment
        final SoftwareModule sm2 = testdataFactory.createSoftwareModuleApp();
        final JSONArray smList2 = new JSONArray();
        smList2.put(new JSONObject().put("id", sm2.getId()));

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                .contentType(MediaType.APPLICATION_JSON).content(smList2.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.entityreadonly")));
    }

    @Test
    @Description("This test verifies the assignment of Software Modules to a Distribution Set through the RESTful API.")
    public void assignSoftwaremoduleToDistributionSet() throws Exception {

        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules("Jupiter", "398,88");
        // Test if size is 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(disSet.getModules().size())));
        // create Software Modules
        final List<Long> smIDs = Arrays.asList(testdataFactory.createSoftwareModuleOs().getId(),
                testdataFactory.createSoftwareModuleApp().getId());

        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                .contentType(MediaType.APPLICATION_JSON).content(JsonBuilder.ids(smIDs)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        // Test if size is 3
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(smIDs.size())));

        // verify quota enforcement
        final int maxSoftwareModules = quotaManagement.getMaxSoftwareModulesPerDistributionSet();
        final List<Long> moduleIDs = Lists.newArrayList();
        for (int i = 0; i < maxSoftwareModules + 1; ++i) {
            moduleIDs.add(testdataFactory.createSoftwareModuleApp("sm" + i).getId());
        }

        // post assignment
        final String jsonIDs = JsonBuilder.ids(moduleIDs.subList(0, maxSoftwareModules - smIDs.size()));
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                .contentType(MediaType.APPLICATION_JSON).content(jsonIDs)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        // test if size corresponds with quota
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId()
                + "/assignedSM?limit={limit}", maxSoftwareModules * 2)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.size", equalTo(maxSoftwareModules)));

        // post one more to cause the quota to be exceeded
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonBuilder.ids(Collections.singletonList(moduleIDs.get(moduleIDs.size() - 1)))))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

        // verify quota is also enforced for bulk uploads
        final DistributionSet disSet2 = testdataFactory.createDistributionSetWithNoSoftwareModules("Saturn", "4.0");
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet2.getId() + "/assignedSM")
                .contentType(MediaType.APPLICATION_JSON).content(JsonBuilder.ids(moduleIDs)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exceptionClass", equalTo(AssignmentQuotaExceededException.class.getName())))
                .andExpect(jsonPath("$.errorCode", equalTo(SpServerError.SP_QUOTA_EXCEEDED.getKey())));

        // verify size is still 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet2.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(0)));

    }

    @Test
    @Description("This test verifies the removal of Software Modules of a Distribution Set through the RESTful API.")
    public void unassignSoftwaremoduleFromDistributionSet() throws Exception {

        // Create DistributionSet with three software modules
        final DistributionSet set = testdataFactory.createDistributionSet("Venus");
        int amountOfSM = set.getModules().size();
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(amountOfSM)));
        // test the removal of all software modules one by one
        for (final Iterator<SoftwareModule> iter = set.getModules().iterator(); iter.hasNext();) {
            final Long smId = iter.next().getId();
            mvc.perform(delete(
                    MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM/" + smId))
                    .andExpect(status().isOk());
            mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM"))
                    .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.size", equalTo(--amountOfSM)));
        }
    }

    @Test
    @Description("Ensures that multi target assignment through API is reflected by the repository.")
    public void assignMultipleTargetsToDistributionSet() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(null, null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assigned", equalTo(knownTargetIds.length - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(knownTargetIds.length)));

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, createdDs.getId()).getContent())
                .as("Five targets in repository have DS assigned").hasSize(5);
    }

    @Test
    @Description("Ensures that targets can be assigned even if the specified controller IDs are in different case (e.g. 'TARGET1' instead of 'target1'.")
    public void assignTargetsToDistributionSetIgnoreCase() throws Exception {
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

        mvc.perform(post(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk());
        // we just need to make sure that no error 500 is returned
    }

    @Test
    @Description("Ensures that multi target assignment is protected by our getMaxTargetDistributionSetAssignmentsPerManualAssignment quota.")
    public void assignMultipleTargetsToDistributionSetUntilQuotaIsExceeded() throws Exception {
        final int maxActions = quotaManagement.getMaxTargetDistributionSetAssignmentsPerManualAssignment();
        final List<Target> targets = testdataFactory.createTargets(maxActions + 1);
        final DistributionSet ds = testdataFactory.createDistributionSet();

        final JSONArray payload = new JSONArray();
        targets.forEach(trg -> {
            try {
                payload.put(new JSONObject().put("id", trg.getId()));
            } catch (final JSONException e) {
                e.printStackTrace();
            }
        });

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + ds.getId() + "/assignedTargets")
                .contentType(MediaType.APPLICATION_JSON).content(payload.toString())).andExpect(status().isForbidden());

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, ds.getId()).getContent()).isEmpty();
    }

    @Test
    @Description("Ensures that the 'max actions per target' quota is enforced if the distribution set assignment of a target is changed permanently")
    public void changeDistributionSetAssignmentForTargetUntilQuotaIsExceeded() throws Exception {

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
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + ds3.getId() + "/assignedTargets")
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isForbidden());
    }

    @Test
    @Description("Ensures that offline reported multi target assignment through API is reflected by the repository.")
    public void offlineAssignmentOfMultipleTargetsToDistributionSet() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5);
        final JSONArray list = new JSONArray();
        targets.forEach(target -> {
            try {
                list.put(new JSONObject().put("id", target.getControllerId()));
            } catch (final JSONException e) {
                e.printStackTrace();
            }
        });

        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), targets.get(0).getControllerId());

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId()
                + "/assignedTargets?offline=true").contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assigned", equalTo(targets.size() - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(targets.size())));

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, createdDs.getId()).getContent())
                .as("Five targets in repository have DS assigned").hasSize(5);

        assertThat(targetManagement.findByInstalledDistributionSet(PAGE, createdDs.getId()).getContent()).hasSize(4);
    }

    @Test
    @Description("Assigns multiple targets to distribution set with only maintenance schedule.")
    public void assignMultipleTargetsToDistributionSetWithMaintenanceWindowStartOnly() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(getTestSchedule(0), null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Assigns multiple targets to distribution set with only maintenance window duration.")
    public void assignMultipleTargetsToDistributionSetWithMaintenanceWindowEndOnly() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(null, getTestDuration(10), null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Assigns multiple targets to distribution set with valid maintenance window.")
    public void assignMultipleTargetsToDistributionSetWithValidMaintenanceWindow() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(getTestSchedule(10), getTestDuration(10), getTestTimeZone(),
                null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Assigns multiple targets to distribution set with last maintenance window scheduled before current time.")
    public void assignMultipleTargetsToDistributionSetWithMaintenanceWindowEndTimeBeforeStartTime() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(getTestSchedule(-30), getTestDuration(5), getTestTimeZone(),
                null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Assigns multiple targets to distribution set with and without maintenance window.")
    public void assignMultipleTargetsToDistributionSetWithAndWithoutMaintenanceWindow() throws Exception {
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

        mvc.perform(post(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Assigning distribution set to the list of targets with a non-existing one leads to successful assignment of valid targets, while not found targets are silently ignored.")
    public void assignNotExistingTargetToDistributionSet() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        final String[] knownTargetIds = new String[] { "1", "2", "3" };
        final JSONArray assignTargetJson = createTargetAndJsonArray(null, null, null, "forced", knownTargetIds);
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        assignTargetJson.put(new JSONObject().put("id", "notexistingtarget").put("type", "forced"));

        mvc.perform(post(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(assignTargetJson.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.assigned", equalTo(2))).andExpect(jsonPath("$.total", equalTo(3)));
    }

    private JSONArray createTargetAndJsonArray(final String schedule, final String duration, final String timezone,
            final String type, final String... targetIds) throws Exception {
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
                targetJsonObject.put("maintenanceWindow", maintenanceJsonObject);
                if (schedule != null) {
                    maintenanceJsonObject.put("schedule", schedule);
                }
                if (duration != null) {
                    maintenanceJsonObject.put("duration", duration);
                }
                if (timezone != null) {
                    maintenanceJsonObject.put("timezone", timezone);
                }
            }
        }
        return result;
    }

    @Test
    @Description("Ensures that assigned targets of DS are returned as reflected by the repository.")
    public void getAssignedTargetsOfDistributionSet() throws Exception {
        // prepare distribution set
        final String knownTargetId = "knownTargetId1";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        testdataFactory.createTarget(knownTargetId);
        assignDistributionSet(createdDs.getId(), knownTargetId);

        mvc.perform(get(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.content[0].controllerId", equalTo(knownTargetId)));
    }

    @Test
    @Description("Ensures that assigned targets of DS are returned as persisted in the repository.")
    public void getAssignedTargetsOfDistributionSetIsEmpty() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        mvc.perform(get(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size", equalTo(0)))
                .andExpect(jsonPath("$.total", equalTo(0)));
    }

    @Test
    @Description("Ensures that installed targets of DS are returned as persisted in the repository.")
    public void getInstalledTargetsOfDistributionSet() throws Exception {
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
        testdataFactory.sendUpdateActionStatusToTargets(Arrays.asList(createTarget), Status.FINISHED,
                Collections.singletonList("some message"));

        mvc.perform(get(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/installedTargets"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.content[0].controllerId", equalTo(knownTargetId)));
    }

    @Test
    @Description("Ensures that target filters with auto assign DS are returned as persisted in the repository.")
    public void getAutoAssignTargetFiltersOfDistributionSet() throws Exception {
        // prepare distribution set
        final String knownFilterName = "a";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(knownFilterName)
                .query("name==y").autoAssignDistributionSet(createdDs.getId()));

        // create some dummy target filter queries
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("b").query("name==y"));
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("c").query("name==y"));

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId()
                + "/autoAssignTargetFilters")).andExpect(status().isOk()).andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.content[0].name", equalTo(knownFilterName)));
    }

    @Test
    @Description("Ensures that an error is returned when the query is invalid.")
    public void getAutoAssignTargetFiltersOfDSWithInvalidFilter() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final String invalidQuery = "unknownField=le=42";

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId()
                + "/autoAssignTargetFilters").param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, invalidQuery))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that target filters with auto assign DS are returned according to the query.")
    public void getMultipleAutoAssignTargetFiltersOfDistributionSet() throws Exception {
        final String filterNamePrefix = "filter-";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final String query = "name==" + filterNamePrefix + "*";

        prepareTestFilters(filterNamePrefix, createdDs);

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId()
                + "/autoAssignTargetFilters").param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, query))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size", equalTo(2)))
                .andExpect(jsonPath("$.content[0].name", equalTo(filterNamePrefix + "1")))
                .andExpect(jsonPath("$.content[1].name", equalTo(filterNamePrefix + "2")));
    }

    @Test
    @Description("Ensures that no target filters are returned according to the non matching query.")
    public void getEmptyAutoAssignTargetFiltersOfDistributionSet() throws Exception {
        final String filterNamePrefix = "filter-";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final String query = "name==doesNotExist";

        prepareTestFilters(filterNamePrefix, createdDs);

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId()
                + "/autoAssignTargetFilters").param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, query))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size", equalTo(0)));
    }

    private void prepareTestFilters(final String filterNamePrefix, final DistributionSet createdDs) {
        // create target filter queries that should be found
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(filterNamePrefix + "1")
                .query("name==y").autoAssignDistributionSet(createdDs.getId()));
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(filterNamePrefix + "2")
                .query("name==y").autoAssignDistributionSet(createdDs.getId()));
        // create some dummy target filter queries
        targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterNamePrefix + "b").query("name==y"));
        targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterNamePrefix + "c").query("name==y"));
    }

    @Test
    @Description("Ensures that DS in repository are listed with proper paging properties.")
    public void getDistributionSetsWithoutAddtionalRequestParameters() throws Exception {
        final int sets = 5;
        createDistributionSetsAlphabetical(sets);
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(sets)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(sets)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(sets)));
    }

    @Test
    @Description("Ensures that DS in repository are listed with proper paging results with paging limit parameter.")
    public void getDistributionSetsWithPagingLimitRequestParameter() throws Exception {
        final int sets = 5;
        final int limitSize = 1;
        createDistributionSetsAlphabetical(sets);
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(sets)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    @Description("Ensures that DS in repository are listed with proper paging results with paging limit and offset parameter.")
    public void getDistributionSetsWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int sets = 5;
        final int offsetParam = 2;
        final int expectedSize = sets - offsetParam;
        createDistributionSetsAlphabetical(sets);
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING)
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(sets)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(sets)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Ensures that multiple DS requested are listed with expected payload.")
    public void getDistributionSets() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(0);

        DistributionSet set = testdataFactory.createDistributionSet("one");
        set = distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                .version("anotherVersion").requiredMigrationStep(true));

        // load also lazy stuff
        set = distributionSetManagement.getWithDetails(set.getId()).get();

        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(1);

        // perform request
        mvc.perform(get("/rest/v1/distributionsets").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
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
                        contains(set.findFirstModuleByType(runtimeType).get().getId().intValue())))
                .andExpect(jsonPath("$.content.[0].modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(set.findFirstModuleByType(appType).get().getId().intValue())))
                .andExpect(jsonPath("$.content.[0].modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(getOsModule(set).intValue())));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Ensures that single DS requested by ID is listed with expected payload.")
    public void getDistributionSet() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        // perform request
        mvc.perform(get("/rest/v1/distributionsets/{dsId}", set.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
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
                        contains(set.findFirstModuleByType(runtimeType).get().getId().intValue())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(set.findFirstModuleByType(appType).get().getId().intValue())))
                .andExpect(jsonPath("$.modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(getOsModule(set).intValue())));

    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Ensures that multipe DS posted to API are created in the repository.")
    public void createDistributionSets() throws Exception {
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(0);

        final SoftwareModule ah = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_APP);
        final SoftwareModule jvm = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_RT);
        final SoftwareModule os = testdataFactory.createSoftwareModule(TestdataFactory.SM_TYPE_OS);

        DistributionSet one = testdataFactory.generateDistributionSet("one", "one", standardDsType,
                Arrays.asList(os, jvm, ah));
        DistributionSet two = testdataFactory.generateDistributionSet("two", "two", standardDsType,
                Arrays.asList(os, jvm, ah));
        DistributionSet three = testdataFactory.generateDistributionSet("three", "three", standardDsType,
                Arrays.asList(os, jvm, ah), true);

        final long current = System.currentTimeMillis();

        final MvcResult mvcResult = executeMgmtTargetPost(one, two, three);

        one = distributionSetManagement
                .getWithDetails(distributionSetManagement.findByRsql(PAGE, "name==one").getContent().get(0).getId())
                .get();
        two = distributionSetManagement
                .getWithDetails(distributionSetManagement.findByRsql(PAGE, "name==two").getContent().get(0).getId())
                .get();
        three = distributionSetManagement
                .getWithDetails(distributionSetManagement.findByRsql(PAGE, "name==three").getContent().get(0).getId())
                .get();

        assertThat(
                JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsets/" + one.getId());

        assertThat(JsonPath.compile("[0]id").read(mvcResult.getResponse().getContentAsString()).toString())
                .isEqualTo(String.valueOf(one.getId()));
        assertThat(
                JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsets/" + two.getId());

        assertThat(JsonPath.compile("[1]id").read(mvcResult.getResponse().getContentAsString()).toString())
                .isEqualTo(String.valueOf(two.getId()));
        assertThat(
                JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsets/" + three.getId());

        assertThat(JsonPath.compile("[2]id").read(mvcResult.getResponse().getContentAsString()).toString())
                .isEqualTo(String.valueOf(three.getId()));

        // check in database
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(3);
        assertThat(one.isRequiredMigrationStep()).isEqualTo(false);
        assertThat(two.isRequiredMigrationStep()).isEqualTo(false);
        assertThat(three.isRequiredMigrationStep()).isEqualTo(true);

        assertThat(one.getCreatedAt()).isGreaterThanOrEqualTo(current);
        assertThat(two.getCreatedAt()).isGreaterThanOrEqualTo(current);
        assertThat(three.getCreatedAt()).isGreaterThanOrEqualTo(current);
    }

    @Step
    private MvcResult executeMgmtTargetPost(final DistributionSet one, final DistributionSet two,
            final DistributionSet three) throws Exception {
        return mvc
                .perform(post("/rest/v1/distributionsets/")
                        .content(JsonBuilder.distributionSets(Arrays.asList(one, two, three)))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0]name", equalTo(one.getName())))
                .andExpect(jsonPath("[0]description", equalTo(one.getDescription())))
                .andExpect(jsonPath("[0]type", equalTo(standardDsType.getKey())))
                .andExpect(jsonPath("[0]createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[0]version", equalTo(one.getVersion())))
                .andExpect(jsonPath("[0]complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("[0]requiredMigrationStep", equalTo(one.isRequiredMigrationStep())))
                .andExpect(jsonPath("[0].modules.[?(@.type=='" + runtimeType.getKey() + "')].id",
                        contains(one.findFirstModuleByType(runtimeType).get().getId().intValue())))
                .andExpect(jsonPath("[0].modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(one.findFirstModuleByType(appType).get().getId().intValue())))
                .andExpect(jsonPath("[0].modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(one.findFirstModuleByType(osType).get().getId().intValue())))
                .andExpect(jsonPath("[1]name", equalTo(two.getName())))
                .andExpect(jsonPath("[1]description", equalTo(two.getDescription())))
                .andExpect(jsonPath("[1]complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("[1]type", equalTo(standardDsType.getKey())))
                .andExpect(jsonPath("[1]createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1]version", equalTo(two.getVersion())))
                .andExpect(jsonPath("[1].modules.[?(@.type=='" + runtimeType.getKey() + "')].id",
                        contains(two.findFirstModuleByType(runtimeType).get().getId().intValue())))
                .andExpect(jsonPath("[1].modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(two.findFirstModuleByType(appType).get().getId().intValue())))
                .andExpect(jsonPath("[1].modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(two.findFirstModuleByType(osType).get().getId().intValue())))
                .andExpect(jsonPath("[1]requiredMigrationStep", equalTo(two.isRequiredMigrationStep())))
                .andExpect(jsonPath("[2]name", equalTo(three.getName())))
                .andExpect(jsonPath("[2]description", equalTo(three.getDescription())))
                .andExpect(jsonPath("[2]complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("[2]type", equalTo(standardDsType.getKey())))
                .andExpect(jsonPath("[2]createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2]version", equalTo(three.getVersion())))
                .andExpect(jsonPath("[2].modules.[?(@.type=='" + runtimeType.getKey() + "')].id",
                        contains(three.findFirstModuleByType(runtimeType).get().getId().intValue())))
                .andExpect(jsonPath("[2].modules.[?(@.type=='" + appType.getKey() + "')].id",
                        contains(three.findFirstModuleByType(appType).get().getId().intValue())))
                .andExpect(jsonPath("[2].modules.[?(@.type=='" + osType.getKey() + "')].id",
                        contains(three.findFirstModuleByType(osType).get().getId().intValue())))
                .andExpect(jsonPath("[2]requiredMigrationStep", equalTo(three.isRequiredMigrationStep()))).andReturn();
    }

    @Test
    @Description("Ensures that DS deletion request to API is reflected by the repository.")
    public void deleteUnassignedistributionSet() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(0);

        final DistributionSet set = testdataFactory.createDistributionSet("one");

        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(1);

        // perform request
        mvc.perform(delete("/rest/v1/distributionsets/{smId}", set.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check repository content
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).isEmpty();
        assertThat(distributionSetManagement.count()).isEqualTo(0);
    }

    @Test
    @Description("Ensures that DS deletion request to API on an entity that does not exist results in NOT_FOUND.")
    public void deleteDistributionSetThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete("/rest/v1/distributionsets/1234")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that assigned DS deletion request to API is reflected by the repository by means of deleted flag set.")
    public void deleteAssignedDistributionSet() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(0);

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        testdataFactory.createTarget("test");
        assignDistributionSet(set.getId(), "test");

        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(1);

        mvc.perform(get("/rest/v1/distributionsets/{dsId}", set.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted", equalTo(false)));

        mvc.perform(delete("/rest/v1/distributionsets/{dsId}", set.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(get("/rest/v1/distributionsets/{dsId}", set.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted", equalTo(true)));

        // check repository content
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(0);
    }

    @Test
    @Description("Ensures that DS property update request to API is reflected by the repository.")
    public void updateDistributionSet() throws Exception {

        // prepare test data
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(0);

        final DistributionSet set = testdataFactory.createDistributionSet("one");

        assertThat(distributionSetManagement.count()).isEqualTo(1);

        final String body = new JSONObject().put("version", "anotherVersion").put("requiredMigrationStep", true)
                .put("deleted", true).toString();

        mvc.perform(put("/rest/v1/distributionsets/{dsId}", set.getId()).content(body)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.version", equalTo("anotherVersion")))
                .andExpect(jsonPath("$.requiredMigrationStep", equalTo(true)))
                .andExpect(jsonPath("$.deleted", equalTo(false)));

        final DistributionSet setupdated = distributionSetManagement.get(set.getId()).get();

        assertThat(setupdated.isRequiredMigrationStep()).isEqualTo(true);
        assertThat(setupdated.getVersion()).isEqualTo("anotherVersion");
        assertThat(setupdated.getName()).isEqualTo(set.getName());
        assertThat(setupdated.isDeleted()).isEqualTo(false);
    }

    @Test
    @Description("Ensures that DS property update on requiredMigrationStep fails if DS is assigned to a target.")
    public void updateRequiredMigrationStepFailsIfDistributionSetisInUse() throws Exception {

        // prepare test data
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(0);

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        assignDistributionSet(set.getId(), testdataFactory.createTarget().getControllerId());

        assertThat(distributionSetManagement.count()).isEqualTo(1);

        mvc.perform(put("/rest/v1/distributionsets/{dsId}", set.getId())
                .content("{\"version\":\"anotherVersion\",\"requiredMigrationStep\":\"true\"}")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());

        final DistributionSet setupdated = distributionSetManagement.get(set.getId()).get();

        assertThat(setupdated.isRequiredMigrationStep()).isEqualTo(false);
        assertThat(setupdated.getVersion()).isEqualTo(set.getVersion());
        assertThat(setupdated.getName()).isEqualTo(set.getName());
    }

    @Test
    @Description("Ensures that the server reacts properly to invalid requests (URI, Media Type, Methods) with correct reponses.")
    public void invalidRequestsOnDistributionSetsResource() throws Exception {
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final List<DistributionSet> sets = new ArrayList<>();
        sets.add(set);

        // SM does not exist
        mvc.perform(get("/rest/v1/distributionsets/12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/distributionsets/12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(post("/rest/v1/distributionsets").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post("/rest/v1/distributionsets").content("sdfjsdlkjfskdjf".getBytes())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final DistributionSet missingName = entityFactory.distributionSet().create().build();
        mvc.perform(post("/rest/v1/distributionsets").content(JsonBuilder.distributionSets(Arrays.asList(missingName)))
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final DistributionSet toLongName = testdataFactory
                .generateDistributionSet(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1));
        mvc.perform(post("/rest/v1/distributionsets").content(JsonBuilder.distributionSets(Arrays.asList(toLongName)))
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // unsupported media type
        mvc.perform(post("/rest/v1/distributionsets").content(JsonBuilder.distributionSets(sets))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // not allowed methods
        mvc.perform(post("/rest/v1/distributionsets/{smId}", set.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/rest/v1/distributionsets")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/rest/v1/distributionsets")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Description("Ensures that the metadata creation through API is reflected by the repository.")
    public void createMetadata() throws Exception {
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");

        final String knownKey1 = "knownKey1";
        final String knownKey2 = "knownKey2";

        final String knownValue1 = "knownValue1";
        final String knownValue2 = "knownValue2";

        final JSONArray metaData1 = new JSONArray();
        metaData1.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        metaData1.put(new JSONObject().put("key", knownKey2).put("value", knownValue2));

        mvc.perform(post("/rest/v1/distributionsets/{dsId}/metadata", testDS.getId()).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(metaData1.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0]key", equalTo(knownKey1))).andExpect(jsonPath("[0]value", equalTo(knownValue1)))
                .andExpect(jsonPath("[1]key", equalTo(knownKey2)))
                .andExpect(jsonPath("[1]value", equalTo(knownValue2)));

        final DistributionSetMetadata metaKey1 = distributionSetManagement
                .getMetaDataByDistributionSetId(testDS.getId(), knownKey1).get();
        final DistributionSetMetadata metaKey2 = distributionSetManagement
                .getMetaDataByDistributionSetId(testDS.getId(), knownKey2).get();

        assertThat(metaKey1.getValue()).isEqualTo(knownValue1);
        assertThat(metaKey2.getValue()).isEqualTo(knownValue2);

        // verify quota enforcement
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerDistributionSet();

        final JSONArray metaData2 = new JSONArray();
        for (int i = 0; i < maxMetaData - metaData1.length() + 1; ++i) {
            metaData2.put(new JSONObject().put("key", knownKey1 + i).put("value", knownValue1 + i));
        }

        mvc.perform(post("/rest/v1/distributionsets/{dsId}/metadata", testDS.getId()).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(metaData2.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());

        // verify that the number of meta data entries has not changed
        // (we cannot use the PAGE constant here as it tries to sort by ID)
        assertThat(distributionSetManagement
                .findMetaDataByDistributionSetId(PageRequest.of(0, Integer.MAX_VALUE), testDS.getId())
                .getTotalElements()).isEqualTo(metaData1.length());

    }

    @Test
    @Description("Ensures that a metadata update through API is reflected by the repository.")
    public void updateMetadata() throws Exception {
        // prepare and create metadata for update
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final String updateValue = "valueForUpdate";

        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        createDistributionSetMetadata(testDS.getId(), entityFactory.generateDsMetadata(knownKey, knownValue));

        final JSONObject jsonObject = new JSONObject().put("key", knownKey).put("value", updateValue);

        mvc.perform(put("/rest/v1/distributionsets/{dsId}/metadata/{key}", testDS.getId(), knownKey)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                .content(jsonObject.toString())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("key", equalTo(knownKey))).andExpect(jsonPath("value", equalTo(updateValue)));

        final DistributionSetMetadata assertDS = distributionSetManagement
                .getMetaDataByDistributionSetId(testDS.getId(), knownKey).get();
        assertThat(assertDS.getValue()).isEqualTo(updateValue);

    }

    @Test
    @Description("Ensures that a metadata entry deletion through API is reflected by the repository.")
    public void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        createDistributionSetMetadata(testDS.getId(), entityFactory.generateDsMetadata(knownKey, knownValue));

        mvc.perform(delete("/rest/v1/distributionsets/{dsId}/metadata/{key}", testDS.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(distributionSetManagement.getMetaDataByDistributionSetId(testDS.getId(), knownKey)).isNotPresent();
    }

    @Test
    @Description("Ensures that DS metadata deletion request to API on an entity that does not exist results in NOT_FOUND.")
    public void deleteMetadataThatDoesNotExistLeadsToNotFound() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        createDistributionSetMetadata(testDS.getId(), entityFactory.generateDsMetadata(knownKey, knownValue));

        mvc.perform(delete("/rest/v1/distributionsets/{dsId}/metadata/XXX", testDS.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete("/rest/v1/distributionsets/1234/metadata/{key}", knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        assertThat(distributionSetManagement.getMetaDataByDistributionSetId(testDS.getId(), knownKey)).isPresent();
    }

    @Test
    @Description("Ensures that a metadata entry selection through API reflectes the repository content.")
    public void getSingleMetadata() throws Exception {
        // prepare and create metadata
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        createDistributionSetMetadata(testDS.getId(), entityFactory.generateDsMetadata(knownKey, knownValue));

        mvc.perform(get("/rest/v1/distributionsets/{dsId}/metadata/{key}", testDS.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("key", equalTo(knownKey))).andExpect(jsonPath("value", equalTo(knownValue)));
    }

    @Test
    @Description("Ensures that a metadata entry paged list selection through API reflectes the repository content.")
    public void getPagedListofMetadata() throws Exception {

        final int totalMetadata = 10;
        final int limitParam = 5;
        final String offsetParam = "0";
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        for (int index = 0; index < totalMetadata; index++) {
            createDistributionSetMetadata(testDS.getId(),
                    entityFactory.generateDsMetadata(knownKeyPrefix + index, knownValuePrefix + index));
        }

        mvc.perform(get("/rest/v1/distributionsets/{dsId}/metadata?offset=" + offsetParam + "&limit=" + limitParam,
                testDS.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(limitParam))).andExpect(jsonPath("total", equalTo(totalMetadata)))
                .andExpect(jsonPath("content[0].key", equalTo("knownKey0")))
                .andExpect(jsonPath("content[0].value", equalTo("knownValue0")));

    }

    @Test
    @Description("Ensures that a DS search with query parameters returns the expected result.")
    public void searchDistributionSetRsql() throws Exception {
        final String dsSuffix = "test";
        final int amount = 10;
        testdataFactory.createDistributionSets(dsSuffix, amount);
        testdataFactory.createDistributionSet("DS1test");
        testdataFactory.createDistributionSet("DS2test");

        final String rsqlFindLikeDs1OrDs2 = "name==DS1test,name==DS2test";

        mvc.perform(get("/rest/v1/distributionsets?q=" + rsqlFindLikeDs1OrDs2)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2))).andExpect(jsonPath("content[0].name", equalTo("DS1test")))
                .andExpect(jsonPath("content[1].name", equalTo("DS2test")));

    }

    @Test
    @Description("Ensures that a DS search with complete==true parameter returns only DS that are actually completely filled with mandatory modules.")
    public void filterDistributionSetComplete() throws Exception {
        final int amount = 10;
        testdataFactory.createDistributionSets(amount);
        distributionSetManagement
                .create(entityFactory.distributionSet().create().name("incomplete").version("2").type("os"));

        final String rsqlFindLikeDs1OrDs2 = "complete==" + Boolean.TRUE;

        mvc.perform(get("/rest/v1/distributionsets?q=" + rsqlFindLikeDs1OrDs2)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(10)))
                .andExpect(jsonPath("total", equalTo(10)));
    }

    @Test
    @Description("Ensures that a DS assigned target search with controllerId==1 parameter returns only the target with the given ID.")
    public void searchDistributionSetAssignedTargetsRsql() throws Exception {
        // prepare distribution set
        final Set<DistributionSet> createDistributionSetsAlphabetical = createDistributionSetsAlphabetical(1);
        final DistributionSet createdDs = createDistributionSetsAlphabetical.iterator().next();
        // prepare targets
        final Collection<String> knownTargetIds = Arrays.asList("1", "2", "3", "4", "5");

        knownTargetIds.forEach(
                controllerId -> targetManagement.create(entityFactory.target().create().controllerId(controllerId)));

        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds.iterator().next());

        final String rsqlFindTargetId1 = "controllerId==1";

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId()
                + "/assignedTargets?q=" + rsqlFindTargetId1).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1))).andExpect(jsonPath("content[0].controllerId", equalTo("1")));
    }

    @Test
    @Description("Ensures that a DS metadata filtered query with value==knownValue1 parameter returns only the metadata entries with that value.")
    public void searchDistributionSetMetadataRsql() throws Exception {
        final int totalMetadata = 10;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        for (int index = 0; index < totalMetadata; index++) {
            createDistributionSetMetadata(testDS.getId(),
                    entityFactory.generateDsMetadata(knownKeyPrefix + index, knownValuePrefix + index));
        }

        final String rsqlSearchValue1 = "value==knownValue1";

        mvc.perform(get("/rest/v1/distributionsets/{dsId}/metadata?q=" + rsqlSearchValue1, testDS.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("content[0].key", equalTo("knownKey1")))
                .andExpect(jsonPath("content[0].value", equalTo("knownValue1")));
    }

    private Set<DistributionSet> createDistributionSetsAlphabetical(final int amount) {
        char character = 'a';
        final Set<DistributionSet> created = Sets.newHashSetWithExpectedSize(amount);
        for (int index = 0; index < amount; index++) {
            final String str = String.valueOf(character);
            created.add(testdataFactory.createDistributionSet(str));
            character++;
        }
        return created;
    }

    @Test
    @Description("Ensures that multi target assignment through API is reflected by the repository in the case of "
            + "DOWNLOAD_ONLY.")
    public void assignMultipleTargetsToDistributionSetAsDownloadOnly() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = createTargetAndJsonArray(null, null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0], Action.ActionType.DOWNLOAD_ONLY);

        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", createdDs.getId())
                .contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", equalTo(knownTargetIds.length - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(knownTargetIds.length)));

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, createdDs.getId()).getContent())
                .as("Five targets in repository have DS assigned").hasSize(5);
    }

    @Test
    @Description("A request for assigning a target multiple times results in a Bad Request when multiassignment is disabled.")
    public void multiassignmentRequestNotAllowedIfDisabled() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONArray body = new JSONArray();
        body.put(getAssignmentObject(targetId, MgmtActionType.SOFT));
        body.put(getAssignmentObject(targetId, MgmtActionType.FORCED));

        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(body.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Identical assignments in a single request are removed when multiassignment is disabled.")
    public void identicalAssignmentInRequestAreRemovedIfMultiassignmentsDisabled() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONArray body = new JSONArray();
        body.put(getAssignmentObject(targetId, MgmtActionType.FORCED));
        body.put(getAssignmentObject(targetId, MgmtActionType.FORCED));

        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(body.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1)));
    }

    @Test
    @Description("Assigning targets multiple times to a DS in one request works in multiassignment mode.")
    public void multiAssignment() throws Exception {
        final List<String> targetIds = testdataFactory.createTargets(2).stream().map(Target::getControllerId)
                .collect(Collectors.toList());
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONArray body = new JSONArray();
        body.put(getAssignmentObject(targetIds.get(0), MgmtActionType.FORCED, 56));
        body.put(getAssignmentObject(targetIds.get(0), MgmtActionType.FORCED, 78));
        body.put(getAssignmentObject(targetIds.get(1), MgmtActionType.FORCED, 67));
        body.put(getAssignmentObject(targetIds.get(1), MgmtActionType.SOFT, 34));

        enableMultiAssignments();
        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(body.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(body.length())));
    }

    @Test
    @Description("An assignment request containing a weight is only accepted when weight is valide and multi assignment is on.")
    public void weightValidation() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int weight = 78;

        final JSONArray bodyValide = new JSONArray().put(getAssignmentObject(targetId, MgmtActionType.FORCED, weight));
        final JSONArray bodyInvalide = new JSONArray()
                .put(getAssignmentObject(targetId, MgmtActionType.FORCED, Action.WEIGHT_MIN - 1));

        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(bodyValide.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.multiassignmentNotEnabled")));
        enableMultiAssignments();
        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(bodyInvalide.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.repo.constraintViolation")));
        mvc.perform(post("/rest/v1/distributionsets/{ds}/assignedTargets", dsId).content(bodyValide.toString())
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).get().collect(Collectors.toList());
        assertThat(actions).size().isEqualTo(1);
        assertThat(actions.get(0).getWeight()).get().isEqualTo(weight);
    }
}

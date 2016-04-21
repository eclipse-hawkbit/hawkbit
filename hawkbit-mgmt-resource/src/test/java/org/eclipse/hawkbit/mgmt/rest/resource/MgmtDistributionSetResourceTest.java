/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.context.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Management API")
@Stories("Distribution Set Resource")
public class MgmtDistributionSetResourceTest extends AbstractIntegrationTest {

    @Test
    @Description("This test verifies the call of all Software Modules that are assiged to a Distribution Set through the RESTful API.")
    public void getSoftwaremodules() throws Exception {
        // Create DistributionSet with three software modules
        final DistributionSet set = TestDataUtil.generateDistributionSet("SMTest", softwareManagement,
                distributionSetManagement);
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(set.getModules().size())));
    }

    @Test
    @Description("This test verifies the deletion of a assigned Software Module of a Distribution Set can not be achieved when that Distribution Set has been assigned or installed to a target.")
    public void deleteFailureWhenDistributionSetInUse() throws Exception {

        // create DisSet
        final DistributionSet disSet = TestDataUtil.generateDistributionSetWithNoSoftwareModules("Eris", "560a",
                distributionSetManagement);
        final List<Long> smIDs = new ArrayList<Long>();
        SoftwareModule sm = new SoftwareModule(osType, "Dysnomia ", "15,772", null, null);
        sm = softwareManagement.createSoftwareModule(sm);
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
            targetManagement.createTarget(new Target(targetId));
            list.put(new JSONObject().put("id", Long.valueOf(targetId)));
        }
        deploymentManagement.assignDistributionSet(disSet.getId(), knownTargetIds[0]);
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
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.entitiylocked")));
    }

    @Test
    @Description("This test verifies that the assignment of a Software Module to a Distribution Set can not be achieved when that Distribution Set has been assigned or installed to a target.")
    public void assignmentFailureWhenAssigningToUsedDistributionSet() throws Exception {

        // create DisSet
        final DistributionSet disSet = TestDataUtil.generateDistributionSetWithNoSoftwareModules("Mars", "686,980",
                distributionSetManagement);
        final List<Long> smIDs = new ArrayList<>();
        SoftwareModule sm = new SoftwareModule(osType, "Phobos", "0,3189", null, null);
        sm = softwareManagement.createSoftwareModule(sm);
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
        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIds) {
            targetManagement.createTarget(new Target(targetId));
            list.put(new JSONObject().put("id", Long.valueOf(targetId)));
        }
        // assign DisSet to target and test assignment
        deploymentManagement.assignDistributionSet(disSet.getId(), knownTargetIds[0]);
        mvc.perform(
                post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned", equalTo(knownTargetIds.length - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(knownTargetIds.length)));

        // Create another SM and post assignment
        final List<Long> smID2s = new ArrayList<Long>();
        SoftwareModule sm2 = new SoftwareModule(appType, "Deimos", "1,262", null, null);
        sm2 = softwareManagement.createSoftwareModule(sm2);
        smID2s.add(sm2.getId());
        final JSONArray smList2 = new JSONArray();
        for (final Long smID : smID2s) {
            smList2.put(new JSONObject().put("id", Long.valueOf(smID)));
        }

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                .contentType(MediaType.APPLICATION_JSON).content(smList2.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode", equalTo("hawkbit.server.error.entitiylocked")));
    }

    @Test
    @Description("This test verifies the assignment of Software Modules to a Distribution Set through the RESTful API.")
    public void assignSoftwaremoduleToDistributionSet() throws Exception {

        // create DisSet
        final DistributionSet disSet = TestDataUtil.generateDistributionSetWithNoSoftwareModules("Jupiter", "398,88",
                distributionSetManagement);
        // Test if size is 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(disSet.getModules().size())));
        // create Software Modules
        final List<Long> smIDs = new ArrayList<Long>();
        SoftwareModule sm = new SoftwareModule(osType, "Europa", "3,551", null, null);
        sm = softwareManagement.createSoftwareModule(sm);
        smIDs.add(sm.getId());
        SoftwareModule sm2 = new SoftwareModule(appType, "Ganymed", "7,155", null, null);
        sm2 = softwareManagement.createSoftwareModule(sm2);
        smIDs.add(sm2.getId());
        SoftwareModule sm3 = new SoftwareModule(runtimeType, "Kallisto", "16,689", null, null);
        sm3 = softwareManagement.createSoftwareModule(sm3);
        smIDs.add(sm3.getId());
        final JSONArray list = new JSONArray();
        for (final Long smID : smIDs) {
            list.put(new JSONObject().put("id", Long.valueOf(smID)));
        }
        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM")
                .contentType(MediaType.APPLICATION_JSON).content(list.toString())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        // Test if size is 3
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + "/assignedSM"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(smIDs.size())));
    }

    @Test
    @Description("This test verifies the removal of Software Modules of a Distribution Set through the RESTful API.")
    public void unassignSoftwaremoduleFromDistributionSet() throws Exception {

        // Create DistributionSet with three software modules
        final DistributionSet set = TestDataUtil.generateDistributionSet("Venus", softwareManagement,
                distributionSetManagement);
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
        // prepare distribution set
        final Set<DistributionSet> createDistributionSetsAlphabetical = createDistributionSetsAlphabetical(1);
        final DistributionSet createdDs = createDistributionSetsAlphabetical.iterator().next();
        // prepare targets
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIds) {
            targetManagement.createTarget(new Target(targetId));
            list.put(new JSONObject().put("id", Long.valueOf(targetId)));
        }
        // assign already one target to DS
        deploymentManagement.assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets")
                        .contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assigned", equalTo(knownTargetIds.length - 1)))
                .andExpect(jsonPath("$.alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("$.total", equalTo(knownTargetIds.length)));

        assertThat(targetManagement.findTargetByAssignedDistributionSet(createdDs.getId(), pageReq).getContent())
                .as("Five targets in repository have DS assigned").hasSize(5);
    }

    @Test
    @Description("Ensures that assigned targets of DS are returned as reflected by the repository.")
    public void getAssignedTargetsOfDistributionSet() throws Exception {
        // prepare distribution set
        final String knownTargetId = "knownTargetId1";
        final Set<DistributionSet> createDistributionSetsAlphabetical = createDistributionSetsAlphabetical(1);
        final DistributionSet createdDs = createDistributionSetsAlphabetical.iterator().next();
        targetManagement.createTarget(new Target(knownTargetId));
        deploymentManagement.assignDistributionSet(createdDs.getId(), knownTargetId);

        mvc.perform(get(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/assignedTargets"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.content[0].controllerId", equalTo(knownTargetId)));
    }

    @Test
    @Description("Ensures that assigned targets of DS are returned as persisted in the repository.")
    public void getAssignedTargetsOfDistributionSetIsEmpty() throws Exception {
        final Set<DistributionSet> createDistributionSetsAlphabetical = createDistributionSetsAlphabetical(1);
        final DistributionSet createdDs = createDistributionSetsAlphabetical.iterator().next();
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
        final Set<DistributionSet> createDistributionSetsAlphabetical = createDistributionSetsAlphabetical(1);
        final DistributionSet createdDs = createDistributionSetsAlphabetical.iterator().next();
        final Target createTarget = targetManagement.createTarget(new Target(knownTargetId));
        // create some dummy targets which are not assigned or installed
        targetManagement.createTarget(new Target("dummy1"));
        targetManagement.createTarget(new Target("dummy2"));
        // assign knownTargetId to distribution set
        deploymentManagement.assignDistributionSet(createdDs.getId(), knownTargetId);
        // make it in install state
        TestDataUtil.sendUpdateActionStatusToTargets(controllerManagament, targetManagement, actionRepository,
                createdDs, Lists.newArrayList(createTarget), Status.FINISHED, "some message");

        mvc.perform(get(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId() + "/installedTargets"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.content[0].controllerId", equalTo(knownTargetId)));
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
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(0);

        DistributionSet set = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);
        set.setRequiredMigrationStep(set.isRequiredMigrationStep());
        set = distributionSetManagement.updateDistributionSet(set);

        set.setVersion("anotherVersion");
        set = distributionSetManagement.updateDistributionSet(set);

        // load also lazy stuff
        set = distributionSetManagement.findDistributionSetByIdWithDetails(set.getId());

        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(1);

        // perform request
        mvc.perform(get("/rest/v1/distributionsets").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$content.[0]._links.self.href",
                        equalTo("http://localhost/rest/v1/distributionsets/" + set.getId())))
                .andExpect(jsonPath("$content.[0].id", equalTo(set.getId().intValue())))
                .andExpect(jsonPath("$content.[0].name", equalTo(set.getName())))
                .andExpect(jsonPath("$content.[0].requiredMigrationStep", equalTo(set.isRequiredMigrationStep())))
                .andExpect(jsonPath("$content.[0].description", equalTo(set.getDescription())))
                .andExpect(jsonPath("$content.[0].type", equalTo(set.getType().getKey())))
                .andExpect(jsonPath("$content.[0].createdBy", equalTo(set.getCreatedBy())))
                .andExpect(jsonPath("$content.[0].createdAt", equalTo(set.getCreatedAt())))
                .andExpect(jsonPath("$content.[0].complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("$content.[0].lastModifiedBy", equalTo(set.getLastModifiedBy())))
                .andExpect(jsonPath("$content.[0].lastModifiedAt", equalTo(set.getLastModifiedAt())))
                .andExpect(jsonPath("$content.[0].version", equalTo(set.getVersion())))
                .andExpect(jsonPath("$content.[0].modules.[?(@.type==" + runtimeType.getKey() + ")][0].id",
                        equalTo(set.findFirstModuleByType(runtimeType).getId().intValue())))
                .andExpect(jsonPath("$content.[0].modules.[?(@.type==" + appType.getKey() + ")][0].id",
                        equalTo(set.findFirstModuleByType(appType).getId().intValue())))
                .andExpect(jsonPath("$content.[0].modules.[?(@.type==" + osType.getKey() + ")][0].id",
                        equalTo(set.findFirstModuleByType(osType).getId().intValue())));
    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Ensures that single DS requested by ID is listed with expected payload.")
    public void getDistributionSet() throws Exception {
        final DistributionSet set = TestDataUtil.createTestDistributionSet(softwareManagement,
                distributionSetManagement);

        // perform request
        mvc.perform(get("/rest/v1/distributionsets/{dsId}", set.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$_links.self.href",
                        equalTo("http://localhost/rest/v1/distributionsets/" + set.getId())))
                .andExpect(jsonPath("$id", equalTo(set.getId().intValue())))
                .andExpect(jsonPath("$name", equalTo(set.getName())))
                .andExpect(jsonPath("$type", equalTo(set.getType().getKey())))
                .andExpect(jsonPath("$description", equalTo(set.getDescription())))
                .andExpect(jsonPath("$requiredMigrationStep", equalTo(set.isRequiredMigrationStep())))
                .andExpect(jsonPath("$createdBy", equalTo(set.getCreatedBy())))
                .andExpect(jsonPath("$complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("$createdAt", equalTo(set.getCreatedAt())))
                .andExpect(jsonPath("$lastModifiedBy", equalTo(set.getLastModifiedBy())))
                .andExpect(jsonPath("$lastModifiedAt", equalTo(set.getLastModifiedAt())))
                .andExpect(jsonPath("$version", equalTo(set.getVersion())))
                .andExpect(jsonPath("$modules.[?(@.type==" + runtimeType.getKey() + ")][0].id",
                        equalTo(set.findFirstModuleByType(runtimeType).getId().intValue())))
                .andExpect(jsonPath("$modules.[?(@.type==" + appType.getKey() + ")][0].id",
                        equalTo(set.findFirstModuleByType(appType).getId().intValue())))
                .andExpect(jsonPath("$modules.[?(@.type==" + osType.getKey() + ")][0].id",
                        equalTo(set.findFirstModuleByType(osType).getId().intValue())));

    }

    @Test
    @WithUser(principal = "uploadTester", allSpPermissions = true)
    @Description("Ensures that multipe DS posted to API are created in the repository.")
    public void createDistributionSets() throws JSONException, Exception {
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(0);

        final SoftwareModule ah = softwareManagement
                .createSoftwareModule(new SoftwareModule(appType, "agent-hub", "1.0.1", null, ""));
        final SoftwareModule jvm = softwareManagement
                .createSoftwareModule(new SoftwareModule(runtimeType, "oracle-jre", "1.7.2", null, ""));
        final SoftwareModule os = softwareManagement
                .createSoftwareModule(new SoftwareModule(osType, "poky", "3.0.2", null, ""));

        DistributionSet one = TestDataUtil.buildDistributionSet("one", "one", standardDsType, os, jvm, ah);
        DistributionSet two = TestDataUtil.buildDistributionSet("two", "two", standardDsType, os, jvm, ah);
        DistributionSet three = TestDataUtil.buildDistributionSet("three", "three", standardDsType, os, jvm, ah);
        three.setRequiredMigrationStep(true);

        final List<DistributionSet> sets = new ArrayList<>();
        sets.add(one);
        sets.add(two);
        sets.add(three);

        final long current = System.currentTimeMillis();

        final MvcResult mvcResult = mvc
                .perform(post("/rest/v1/distributionsets/").content(JsonBuilder.distributionSets(sets))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0]name", equalTo(one.getName())))
                .andExpect(jsonPath("[0]description", equalTo(one.getDescription())))
                .andExpect(jsonPath("[0]type", equalTo(standardDsType.getKey())))
                .andExpect(jsonPath("[0]createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[0]version", equalTo(one.getVersion())))
                .andExpect(jsonPath("[0]complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("[0]requiredMigrationStep", equalTo(one.isRequiredMigrationStep())))
                .andExpect(jsonPath("[0].modules.[?(@.type==" + runtimeType.getKey() + ")][0].id",
                        equalTo(one.findFirstModuleByType(runtimeType).getId().intValue())))
                .andExpect(jsonPath("[0].modules.[?(@.type==" + appType.getKey() + ")][0].id",
                        equalTo(one.findFirstModuleByType(appType).getId().intValue())))
                .andExpect(jsonPath("[0].modules.[?(@.type==" + osType.getKey() + ")][0].id",
                        equalTo(one.findFirstModuleByType(osType).getId().intValue())))
                .andExpect(jsonPath("[1]name", equalTo(two.getName())))
                .andExpect(jsonPath("[1]description", equalTo(two.getDescription())))
                .andExpect(jsonPath("[1]complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("[1]type", equalTo(standardDsType.getKey())))
                .andExpect(jsonPath("[1]createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[1]version", equalTo(two.getVersion())))
                .andExpect(jsonPath("[1].modules.[?(@.type==" + runtimeType.getKey() + ")][0].id",
                        equalTo(two.findFirstModuleByType(runtimeType).getId().intValue())))
                .andExpect(jsonPath("[1].modules.[?(@.type==" + appType.getKey() + ")][0].id",
                        equalTo(two.findFirstModuleByType(appType).getId().intValue())))
                .andExpect(jsonPath("[1].modules.[?(@.type==" + osType.getKey() + ")][0].id",
                        equalTo(two.findFirstModuleByType(osType).getId().intValue())))
                .andExpect(jsonPath("[1]requiredMigrationStep", equalTo(two.isRequiredMigrationStep())))
                .andExpect(jsonPath("[2]name", equalTo(three.getName())))
                .andExpect(jsonPath("[2]description", equalTo(three.getDescription())))
                .andExpect(jsonPath("[2]complete", equalTo(Boolean.TRUE)))
                .andExpect(jsonPath("[2]type", equalTo(standardDsType.getKey())))
                .andExpect(jsonPath("[2]createdBy", equalTo("uploadTester")))
                .andExpect(jsonPath("[2]version", equalTo(three.getVersion())))
                .andExpect(jsonPath("[2].modules.[?(@.type==" + runtimeType.getKey() + ")][0].id",
                        equalTo(three.findFirstModuleByType(runtimeType).getId().intValue())))
                .andExpect(jsonPath("[2].modules.[?(@.type==" + appType.getKey() + ")][0].id",
                        equalTo(three.findFirstModuleByType(appType).getId().intValue())))
                .andExpect(jsonPath("[2].modules.[?(@.type==" + osType.getKey() + ")][0].id",
                        equalTo(three.findFirstModuleByType(osType).getId().intValue())))
                .andExpect(jsonPath("[2]requiredMigrationStep", equalTo(three.isRequiredMigrationStep()))).andReturn();

        one = distributionSetManagement.findDistributionSetByIdWithDetails(
                distributionSetManagement.findDistributionSetByNameAndVersion("one", "one").getId());
        two = distributionSetManagement.findDistributionSetByIdWithDetails(
                distributionSetManagement.findDistributionSetByNameAndVersion("two", "two").getId());
        three = distributionSetManagement.findDistributionSetByIdWithDetails(
                distributionSetManagement.findDistributionSetByNameAndVersion("three", "three").getId());

        assertThat(
                JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsets/" + one.getId());
        assertThat(
                JsonPath.compile("[0]_links.type.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsettypes/" + one.getType().getId());

        assertThat(JsonPath.compile("[0]id").read(mvcResult.getResponse().getContentAsString()).toString())
                .isEqualTo(String.valueOf(one.getId()));
        assertThat(
                JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsets/" + two.getId());
        assertThat(
                JsonPath.compile("[1]_links.type.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsettypes/" + two.getType().getId());

        assertThat(JsonPath.compile("[1]id").read(mvcResult.getResponse().getContentAsString()).toString())
                .isEqualTo(String.valueOf(two.getId()));
        assertThat(
                JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsets/" + three.getId());
        assertThat(
                JsonPath.compile("[2]_links.type.href").read(mvcResult.getResponse().getContentAsString()).toString())
                        .isEqualTo("http://localhost/rest/v1/distributionsettypes/" + three.getType().getId());

        assertThat(JsonPath.compile("[2]id").read(mvcResult.getResponse().getContentAsString()).toString())
                .isEqualTo(String.valueOf(three.getId()));

        // check in database
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(3);
        assertThat(one.isRequiredMigrationStep()).isEqualTo(false);
        assertThat(two.isRequiredMigrationStep()).isEqualTo(false);
        assertThat(three.isRequiredMigrationStep()).isEqualTo(true);

        assertThat(one.getCreatedAt()).isGreaterThanOrEqualTo(current);
        assertThat(two.getCreatedAt()).isGreaterThanOrEqualTo(current);
        assertThat(three.getCreatedAt()).isGreaterThanOrEqualTo(current);
    }

    @Test
    @Description("Ensures that DS deletion request to API is reflected by the repository.")
    public void deleteUnassignedistributionSet() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(0);

        final DistributionSet set = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);

        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(1);

        // perform request
        mvc.perform(delete("/rest/v1/distributionsets/{smId}", set.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check repository content
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).isEmpty();
        assertThat(distributionSetRepository.findAll()).isEmpty();
    }

    @Test
    @Description("Ensures that assigned DS deletion request to API is reflected by the repository by means of deleted flag set.")
    public void deleteAssignedDistributionSet() throws Exception {
        // prepare test data
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(0);

        final DistributionSet set = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);
        targetManagement.createTarget(new Target("test"));
        deploymentManagement.assignDistributionSet(set.getId(), "test");

        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(1);

        // perform request
        mvc.perform(delete("/rest/v1/distributionsets/{smId}", set.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // check repository content
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(0);
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, true, true)).hasSize(1);
    }

    @Test
    @Description("Ensures that DS property update request to API is reflected by the repository.")
    public void updateDistributionSet() throws Exception {

        // prepare test data
        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(0);

        final DistributionSet set = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);

        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true)).hasSize(1);

        final DistributionSet update = new DistributionSet();
        update.setVersion("anotherVersion");
        update.setName(null);
        update.setType(standardDsType);

        mvc.perform(put("/rest/v1/distributionsets/{dsId}", set.getId()).content(JsonBuilder.distributionSet(update))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(distributionSetManagement.findDistributionSetsAll(pageReq, false, true).getContent().get(0)
                .getVersion()).isEqualTo("anotherVersion");
        assertThat(
                distributionSetManagement.findDistributionSetsAll(pageReq, false, true).getContent().get(0).getName())
                        .isEqualTo(set.getName());
    }

    @Test
    @Description("Ensures that the server reacts properly to invalid requests (URI, Media Type, Methods) with correct reponses.")
    public void invalidRequestsOnDistributionSetsResource() throws Exception {
        final DistributionSet set = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);

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
        final DistributionSet testDS = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);

        final String knownKey1 = "knownKey1";
        final String knownKey2 = "knownKey2";

        final String knownValue1 = "knownValue1";
        final String knownValue2 = "knownValue2";

        final JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        jsonArray.put(new JSONObject().put("key", knownKey2).put("value", knownValue2));

        mvc.perform(post("/rest/v1/distributionsets/{dsId}/metadata", testDS.getId())
                .contentType(MediaType.APPLICATION_JSON).content(jsonArray.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("[0]key", equalTo(knownKey1))).andExpect(jsonPath("[0]value", equalTo(knownValue1)))
                .andExpect(jsonPath("[1]key", equalTo(knownKey2)))
                .andExpect(jsonPath("[1]value", equalTo(knownValue2)));

        final DistributionSetMetadata metaKey1 = distributionSetManagement
                .findOne(new DsMetadataCompositeKey(testDS, knownKey1));
        final DistributionSetMetadata metaKey2 = distributionSetManagement
                .findOne(new DsMetadataCompositeKey(testDS, knownKey2));

        assertThat(metaKey1.getValue()).isEqualTo(knownValue1);
        assertThat(metaKey2.getValue()).isEqualTo(knownValue2);
    }

    @Test
    @Description("Ensures that a metadata update through API is reflected by the repository.")
    public void updateMetadata() throws Exception {
        // prepare and create metadata for update
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final String updateValue = "valueForUpdate";

        final DistributionSet testDS = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);
        distributionSetManagement
                .createDistributionSetMetadata(new DistributionSetMetadata(knownKey, testDS, knownValue));

        final JSONObject jsonObject = new JSONObject().put("key", knownKey).put("value", updateValue);

        mvc.perform(put("/rest/v1/distributionsets/{dsId}/metadata/{key}", testDS.getId(), knownKey)
                .contentType(MediaType.APPLICATION_JSON).content(jsonObject.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("key", equalTo(knownKey))).andExpect(jsonPath("value", equalTo(updateValue)));

        final DistributionSetMetadata assertDS = distributionSetManagement
                .findOne(new DsMetadataCompositeKey(testDS, knownKey));
        assertThat(assertDS.getValue()).isEqualTo(updateValue);

    }

    @Test
    @Description("Ensures that a metadata entry deletion through API is reflected by the repository.")
    public void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final DistributionSet testDS = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);
        distributionSetManagement
                .createDistributionSetMetadata(new DistributionSetMetadata(knownKey, testDS, knownValue));

        mvc.perform(delete("/rest/v1/distributionsets/{dsId}/metadata/{key}", testDS.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        try {
            distributionSetManagement.findOne(new DsMetadataCompositeKey(testDS, knownKey));
            fail("expected EntityNotFoundException but didn't throw");
        } catch (final EntityNotFoundException e) {
            // ok as expected
        }
    }

    @Test
    @Description("Ensures that a metadata entry selection through API reflectes the repository content.")
    public void getSingleMetadata() throws Exception {
        // prepare and create metadata
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final DistributionSet testDS = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);
        distributionSetManagement
                .createDistributionSetMetadata(new DistributionSetMetadata(knownKey, testDS, knownValue));

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
        final DistributionSet testDS = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);
        for (int index = 0; index < totalMetadata; index++) {
            distributionSetManagement.createDistributionSetMetadata(new DistributionSetMetadata(knownKeyPrefix + index,
                    distributionSetManagement.findDistributionSetById(testDS.getId()), knownValuePrefix + index));
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
        TestDataUtil.generateDistributionSets(dsSuffix, amount, softwareManagement, distributionSetManagement);
        TestDataUtil.generateDistributionSet("DS1test", softwareManagement, distributionSetManagement);
        TestDataUtil.generateDistributionSet("DS2test", softwareManagement, distributionSetManagement);

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
        TestDataUtil.generateDistributionSets(amount, softwareManagement, distributionSetManagement);
        distributionSetManagement.createDistributionSet(new DistributionSet("incomplete", "2", "incomplete",
                distributionSetManagement.findDistributionSetTypeByKey("ecl_os"), null));

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
        final String[] knownTargetIds = new String[] { "1", "2", "3", "4", "5" };
        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIds) {
            targetManagement.createTarget(new Target(targetId));
            list.put(new JSONObject().put("id", Long.valueOf(targetId)));
        }

        // assign already one target to DS
        deploymentManagement.assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        final String rsqlFindTargetId1 = "controllerId==1";

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + createdDs.getId()
                + "/assignedTargets?q=" + rsqlFindTargetId1).contentType(MediaType.APPLICATION_JSON)
                        .content(list.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("total", equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1))).andExpect(jsonPath("content[0].controllerId", equalTo("1")));
    }

    @Test
    @Description("Ensures that a DS metadata filtered query with value==knownValue1 parameter returns only the metadata entries with that value.")
    public void searchDistributionSetMetadataRsql() throws Exception {
        final int totalMetadata = 10;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final DistributionSet testDS = TestDataUtil.generateDistributionSet("one", softwareManagement,
                distributionSetManagement);
        for (int index = 0; index < totalMetadata; index++) {
            distributionSetManagement.createDistributionSetMetadata(new DistributionSetMetadata(knownKeyPrefix + index,
                    distributionSetManagement.findDistributionSetById(testDS.getId()), knownValuePrefix + index));
        }

        final String rsqlSearchValue1 = "value==knownValue1";

        mvc.perform(get("/rest/v1/distributionsets/{dsId}/metadata?q=" + rsqlSearchValue1, testDS.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("content[0].key", equalTo("knownKey1")))
                .andExpect(jsonPath("content[0].value", equalTo("knownValue1")));
    }

    private Set<DistributionSet> createDistributionSetsAlphabetical(final int amount) {
        char character = 'a';
        final Set<DistributionSet> created = new HashSet<>();
        for (int index = 0; index < amount; index++) {
            final String str = String.valueOf(character);
            created.add(TestDataUtil.generateDistributionSet(str, softwareManagement, distributionSetManagement));
            character++;
        }
        return created;
    }

}

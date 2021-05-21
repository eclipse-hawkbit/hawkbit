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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.documentation.AbstractApiRestDocumentation;
import org.eclipse.hawkbit.rest.documentation.ApiModelPropertiesGeneric;
import org.eclipse.hawkbit.rest.documentation.MgmtApiModelProperties;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import com.google.common.collect.Lists;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Documentation generation for Management API for {@link DistributionSet}.
 */
@Feature("Spring Rest Docs Tests - DistributionSet")
@Story("DistributionSet Resource")
public class DistributionSetsDocumentationTest extends AbstractApiRestDocumentation {

    @Override
    public String getResourceName() {
        return "distributionsets";
    }

    @Test
    @Description("Get Distribution Set. Handles the GET request of retrieving a single distribution set within SP. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getDistributionSet() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}", set.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        getResponseFieldsDistributionSet(false)));
    }

    @Test
    @Description("Get paged list of Distribution Sets. Required Permission: " + SpPermission.READ_REPOSITORY)
    public void getDistributionSets() throws Exception {
        testdataFactory.createUpdatedDistributionSet();

        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(responseFields(
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("content").description(MgmtApiModelProperties.DS_LIST),
                        fieldWithPath("content[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                        fieldWithPath("content[].name").description(ApiModelPropertiesGeneric.NAME),
                        fieldWithPath("content[].description").description(ApiModelPropertiesGeneric.DESCRPTION),
                        fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                        fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                        fieldWithPath("content[].lastModifiedBy")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                        fieldWithPath("content[].lastModifiedAt")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                        fieldWithPath("content[].type").description(MgmtApiModelProperties.DS_TYPE),
                        fieldWithPath("content[].requiredMigrationStep")
                                .description(MgmtApiModelProperties.DS_REQUIRED_STEP),
                        fieldWithPath("content[].complete").description(MgmtApiModelProperties.DS_COMPLETE),
                        fieldWithPath("content[].deleted").description(ApiModelPropertiesGeneric.DELETED),
                        fieldWithPath("content[].version").description(MgmtApiModelProperties.VERSION),
                        fieldWithPath("content[]._links.self").ignored())));
    }

    @Test
    @Description("Get paged list of Distribution Sets with given page size and offset including sorting by name descending and filter down to all sets which name starts with 'testDs'.  Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getDistributionSetsWithParameters() throws Exception {

        final List<DistributionSet> sets = testdataFactory.createDistributionSets("testDS", 3);

        sets.forEach(set -> distributionSetManagement
                .update(entityFactory.distributionSet().update(set.getId()).description("updated description")));

        mockMvc.perform(
                get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING).param("offset", "1").param("limit", "2")
                        .param("sort", "version:DESC").param("q", "name==testDS*").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(getFilterRequestParamter()));

    }

    @Test
    @Description("Handles the DELETE request for a single Distribution Set within SP. Required Permission: "
            + SpPermission.DELETE_REPOSITORY)
    public void deleteDistributionSet() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        this.mockMvc
                .perform(delete(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}",
                        set.getId()))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Create Distribution Sets. Handles the POST request of creating new distribution sets within SP. The request body must always be a list of sets. Required Permission: "
            + SpPermission.CREATE_REPOSITORY)
    public void createDistributionSets() throws Exception {

        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        final DistributionSet one = testdataFactory.generateDistributionSet("one", "one", standardDsType,
                Arrays.asList(os, ah));
        final DistributionSet two = testdataFactory.generateDistributionSet("two", "two", standardDsType,
                Arrays.asList(os, ah));
        final DistributionSet three = testdataFactory.generateDistributionSet("three", "three", standardDsType,
                Arrays.asList(os, ah), true);

        final List<DistributionSet> sets = Arrays.asList(one, two, three);

        this.mockMvc
                .perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/")
                        .content(JsonBuilder.distributionSetsCreateValidFieldsOnly(sets))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        requestFields(requestFieldWithPath("[]name").description(ApiModelPropertiesGeneric.NAME),
                                requestFieldWithPath("[]description").description(ApiModelPropertiesGeneric.DESCRPTION),
                                requestFieldWithPath("[]version").description(MgmtApiModelProperties.VERSION),
                                optionalRequestFieldWithPath("[]requiredMigrationStep")
                                        .description(MgmtApiModelProperties.DS_REQUIRED_STEP),
                                requestFieldWithPath("[]type").description(MgmtApiModelProperties.DS_TYPE),
                                optionalRequestFieldWithPath("[]modules").ignored()),
                        getResponseFieldsDistributionSet(true)));
    }

    @Test
    @Description("Handles the UPDATE request for a single Distribution Set within SP. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY)
    public void updateDistributionSet() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        final DistributionSet update = entityFactory.distributionSet().create().name("another Name")
                .version("another Version").description("a new description").requiredMigrationStep(true).build();

        mockMvc.perform(put(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}", set.getId())
                .content(JsonBuilder.distributionSetUpdateValidFieldsOnly(update))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(optionalRequestFieldWithPath("name").description(ApiModelPropertiesGeneric.NAME),
                                optionalRequestFieldWithPath("version").description(MgmtApiModelProperties.VERSION),
                                optionalRequestFieldWithPath("description")
                                        .description(ApiModelPropertiesGeneric.DESCRPTION),
                                optionalRequestFieldWithPath("requiredMigrationStep")
                                        .description(MgmtApiModelProperties.DS_REQUIRED_STEP)),
                        getResponseFieldsDistributionSet(false)));
    }

    @Test
    @Description("Handles the GET request for retrieving assigned targets of a single distribution set. Required Permission: "
            + SpPermission.READ_REPOSITORY + " and " + SpPermission.READ_TARGET)
    public void getAssignedTargets() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        final Target target = testdataFactory.createTarget();
        // assign knownTargetId to distribution set
        assignDistributionSet(set.getId(), target.getControllerId());

        mockMvc.perform(
                get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/assignedTargets",
                        set.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("content").description(MgmtApiModelProperties.TARGET_LIST)
                                        .type("Array[Object]"))));

    }

    @Test
    @Description("Handles the GET request for retrieving assigned target filter queries of a single distribution set. Required Permission: "
            + SpPermission.READ_REPOSITORY + " and " + SpPermission.READ_TARGET)
    public void getAutoAssignTargetFilterQueries() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("filter1").query("name==a")
                .autoAssignDistributionSet(set));
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("filter2").query("name==b")
                .autoAssignDistributionSet(set));

        mockMvc.perform(get(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/autoAssignTargetFilters",
                set.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("content").description(MgmtApiModelProperties.TARGET_FILTER_QUERIES_LIST)
                                        .type("Array[Object]"))));

    }

    @Test
    @Description("Handles the GET request for retrieving assigned target filter queries of a single distribution set with a defined page size and offset, sorted by name in descending order and filtered down to all targets with a name that ends with '1'. Required Permission: "
            + SpPermission.READ_REPOSITORY + " and " + SpPermission.READ_TARGET)
    public void getAutoAssignTargetFilterQueriesWithParameters() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("filter1").query("name==a")
                .autoAssignDistributionSet(set));

        mockMvc.perform(get(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/autoAssignTargetFilters")
                        .param("offset", "1").param("limit", "2").param("sort", "name:DESC").param("q", "name==*1")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(requestParameters(
                        parameterWithName("limit").attributes(key("type").value("query"))
                                .description(ApiModelPropertiesGeneric.LIMIT),
                        parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                        parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                        parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL))));

    }

    @Test
    @Description("Handles the GET request for retrieving assigned targets of a single distribution set with a defined page size and offset, sorted by name in descending order and filtered down to all targets which controllerID starts with 'target'. Required Permission: "
            + SpPermission.READ_REPOSITORY + " and " + SpPermission.READ_TARGET)
    public void getAssignedTargetsWithParameters() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        assignDistributionSet(set, testdataFactory.createTargets(5, "targetMisc", "Test targets for query"))
                .getAssignedEntity();

        mockMvc.perform(
                get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedTargets")
                        .param("offset", "1").param("limit", "2").param("sort", "name:DESC")
                        .param("q", "controllerId==target*").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(requestParameters(
                        parameterWithName("limit").attributes(key("type").value("query"))
                                .description(ApiModelPropertiesGeneric.LIMIT),
                        parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                        parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                        parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL))));

    }

    @Test
    @Description("Handles the GET request for retrieving installed targets of a single distribution set with a defined page size and offset, sortet by name in descending order and filtered down to all targets which controllerID starts with 'target'. Required Permission: "
            + SpPermission.READ_REPOSITORY + " and " + SpPermission.READ_TARGET)
    public void getInstalledTargetsWithParameters() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        final List<Target> targets = assignDistributionSet(set,
                testdataFactory.createTargets(5, "targetMisc", "Test targets for query")).getAssignedEntity().stream()
                        .map(Action::getTarget).collect(Collectors.toList());
        testdataFactory.sendUpdateActionStatusToTargets(targets, Status.FINISHED, "some message");

        mockMvc.perform(
                get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/installedTargets")
                        .param("offset", "1").param("limit", "2").param("sort", "name:DESC")
                        .param("q", "controllerId==target*").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(requestParameters(
                        parameterWithName("limit").attributes(key("type").value("query"))
                                .description(ApiModelPropertiesGeneric.LIMIT),
                        parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                        parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                        parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL))));
    }

    @Test
    @Description("Handles the GET request for retrieving installed targets of a single distribution set. Required Permission: "
            + SpPermission.READ_REPOSITORY + " and " + SpPermission.READ_TARGET)
    public void getInstalledTargets() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        final Target target = testdataFactory.createTarget();
        // assign knownTargetId to distribution set
        assignDistributionSet(set.getId(), target.getControllerId());
        // make it in install state
        testdataFactory.sendUpdateActionStatusToTargets(Arrays.asList(target), Status.FINISHED, "some message");

        mockMvc.perform(
                get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/installedTargets",
                        set.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("content").description(MgmtApiModelProperties.TARGET_LIST)
                                        .type("Array[Object]"))));
    }

    @Test
    @Description("Handles the POST request for assigning multiple targets to a distribution set.The request body must always be a list of target IDs."
            + " Required Permission: " + SpPermission.READ_REPOSITORY + " and " + SpPermission.UPDATE_TARGET)
    public void createAssignedTarget() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        // prepare targets
        final long forceTime = System.currentTimeMillis();
        final String[] knownTargetIds = new String[] { "target1", "target2", "target3", "target4", "target5" };
        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIds) {
            targetManagement.create(entityFactory.target().create().controllerId(targetId));
            list.put(new JSONObject().put("id", targetId).put("type", "timeforced").put("forcetime", forceTime)
                    .put("maintenanceWindow", new JSONObject().put("schedule", getTestSchedule(100))
                            .put("duration", getTestDuration(10)).put("timezone", getTestTimeZone())));
        }

        // assign already one target to DS
        assignDistributionSet(set.getId(), knownTargetIds[0]);

        this.mockMvc
                .perform(post(
                        MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/assignedTargets",
                        set.getId()).content(list.toString()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestParameters(parameterWithName("offline")
                                .description(MgmtApiModelProperties.OFFLINE_UPDATE).optional()),
                        requestFields(
                                requestFieldWithPath("[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                requestFieldWithPathMandatoryInMultiAssignMode("[].weight")
                                        .description(MgmtApiModelProperties.ASSIGNMENT_WEIGHT)
                                        .type(JsonFieldType.NUMBER).attributes(key("value").value("0 - 1000")),
                                optionalRequestFieldWithPath("[].forcetime")
                                        .description(MgmtApiModelProperties.FORCETIME),
                                optionalRequestFieldWithPath("[].maintenanceWindow")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW),
                                optionalRequestFieldWithPath("[].maintenanceWindow.schedule")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_SCHEDULE),
                                optionalRequestFieldWithPath("[].maintenanceWindow.duration")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_DURATION),
                                optionalRequestFieldWithPath("[].maintenanceWindow.timezone")
                                        .description(MgmtApiModelProperties.MAINTENANCE_WINDOW_TIMEZONE),
                                optionalRequestFieldWithPath("[].type")
                                        .description(MgmtApiModelProperties.ASSIGNMENT_TYPE)
                                        .attributes(key("value").value("['soft', 'forced','timeforced', 'downloadonly']"))),
                        responseFields(
                                fieldWithPath("assigned").description(MgmtApiModelProperties.DS_NEW_ASSIGNED_TARGETS),
                                fieldWithPath("alreadyAssigned").type(JsonFieldType.NUMBER)
                                        .description(MgmtApiModelProperties.DS_ALREADY_ASSIGNED_TARGETS),
                                fieldWithPath("assignedActions").type(JsonFieldType.ARRAY)
                                        .description(MgmtApiModelProperties.DS_NEW_ASSIGNED_ACTIONS),
                                fieldWithPath("assignedActions.[].id").type(JsonFieldType.NUMBER)
                                        .description(MgmtApiModelProperties.ACTION_ID),
                                fieldWithPath("assignedActions.[]._links.self").type(JsonFieldType.OBJECT)
                                        .description(MgmtApiModelProperties.LINK_TO_ACTION),
                                fieldWithPath("total").type(JsonFieldType.NUMBER)
                                        .description(MgmtApiModelProperties.DS_TOTAL_ASSIGNED_TARGETS))));
    }

    @Test
    @Description("Handles the POST request for assigning multiple software modules to a distribution set. The request body must always be a list of software module IDs."
            + " Required Permission: " + SpPermission.READ_REPOSITORY + " and " + SpPermission.UPDATE_REPOSITORY)
    public void assignSoftwareModules() throws Exception {
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules("Jupiter", "398,88");

        // create Software Modules
        final List<Long> smIDs = Arrays.asList(testdataFactory.createSoftwareModuleOs().getId(),
                testdataFactory.createSoftwareModuleApp().getId());
        final JSONArray list = new JSONArray();
        for (final Long smID : smIDs) {
            list.put(new JSONObject().put("id", Long.valueOf(smID)));
        }

        // post assignment
        mockMvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/assignedSM",
                disSet.getId()).contentType(MediaType.APPLICATION_JSON).content(list.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(requestFieldWithPath("[]id").description(ApiModelPropertiesGeneric.ITEM_ID))));

    }

    @Test
    @Description("Delete a software module assignment." + " Required Permission: " + SpPermission.UPDATE_REPOSITORY)
    public void deleteAssignSoftwareModules() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        mockMvc.perform(delete(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING
                        + "/{distributionSetId}/assignedSM/{softwareModuleId}",
                set.getId(), set.findFirstModuleByType(osType).get().getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("softwareModuleId").description(ApiModelPropertiesGeneric.ITEM_ID))));
    }

    @Test
    @Description("Handles the GET request of retrieving assigned software modules of a single distribution set within SP. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getAssignedSoftwareModules() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        // post assignment
        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/assignedSM",
                set.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("content").description(MgmtApiModelProperties.SM_LIST),
                                fieldWithPath("content[].id").description(ApiModelPropertiesGeneric.ITEM_ID),
                                fieldWithPath("content[].name").description(ApiModelPropertiesGeneric.NAME),
                                fieldWithPath("content[].description")
                                        .description(ApiModelPropertiesGeneric.DESCRPTION),
                                fieldWithPath("content[].vendor").description(MgmtApiModelProperties.VENDOR),
                                fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                                fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                                fieldWithPath("content[].lastModifiedBy")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY),
                                fieldWithPath("content[].lastModifiedAt")
                                        .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT),
                                fieldWithPath("content[].type").description(MgmtApiModelProperties.SM_TYPE),
                                fieldWithPath("content[].version").description(MgmtApiModelProperties.VERSION),
                                fieldWithPath("content[]._links.self").ignored())));
    }

    @Test
    @Description("Handles the GET request of retrieving assigned software modules of a single distribution set within SP with given page size and offset including sorting by version descending and filter down to all sets which name starts with 'one'. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getAssignedSoftwareModulesWithParameters() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();

        // post assignment
        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/assignedSM")
                .param("offset", "1").param("limit", "2").param("sort", "version:DESC").param("q", "name==one*")
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(this.document.document(requestParameters(
                        parameterWithName("limit").attributes(key("type").value("query"))
                                .description(ApiModelPropertiesGeneric.LIMIT),
                        parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                        parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                        parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL))));
    }

    @Test
    @Description("Get a paged list of meta data for a distribution set with standard page size."
            + " Required Permission: " + SpPermission.READ_REPOSITORY)
    public void getMetadata() throws Exception {
        final int totalMetadata = 4;
        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        for (int index = 0; index < totalMetadata; index++) {
            distributionSetManagement.createMetaData(testDS.getId(), Lists
                    .newArrayList(entityFactory.generateDsMetadata(knownKeyPrefix + index, knownValuePrefix + index)));
        }

        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata",
                testDS.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("content").description(MgmtApiModelProperties.META_DATA),
                                fieldWithPath("content[].key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("content[].value").description(MgmtApiModelProperties.META_DATA_VALUE))));
    }

    @Test
    @Description("Get a paged list of meta data for a distribution set with defined page size and sorting by name descending and key starting with 'known'."
            + " Required Permission: " + SpPermission.READ_REPOSITORY)
    public void getMetadataWithParameters() throws Exception {
        final int totalMetadata = 4;

        final String knownKeyPrefix = "knownKey";
        final String knownValuePrefix = "knownValue";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        for (int index = 0; index < totalMetadata; index++) {
            distributionSetManagement.createMetaData(testDS.getId(), Lists
                    .newArrayList(entityFactory.generateDsMetadata(knownKeyPrefix + index, knownValuePrefix + index)));
        }

        mockMvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{dsId}/metadata", testDS.getId())
                .param("offset", "1").param("limit", "2").param("sort", "key:DESC").param("q", "key==known*"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        requestParameters(
                                parameterWithName("limit").attributes(key("type").value("query"))
                                        .description(ApiModelPropertiesGeneric.LIMIT),
                                parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                                parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                                parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL)),
                        responseFields(fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                                fieldWithPath("size").type(JsonFieldType.NUMBER)
                                        .description(ApiModelPropertiesGeneric.SIZE),
                                fieldWithPath("content").description(MgmtApiModelProperties.META_DATA),
                                fieldWithPath("content[].key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("content[].value").description(MgmtApiModelProperties.META_DATA_VALUE))));
    }

    @Test
    @Description("Get a single meta data value for a meta data key." + " Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public void getMetadataValue() throws Exception {

        // prepare and create metadata
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        distributionSetManagement.createMetaData(testDS.getId(),
                Arrays.asList(entityFactory.generateDsMetadata(knownKey, knownValue)));

        mockMvc.perform(get(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata/{metadatakey}",
                testDS.getId(), knownKey)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("metadatakey").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        responseFields(fieldWithPath("key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("value").description(MgmtApiModelProperties.META_DATA_VALUE))));
    }

    @Test
    @Description("Update a single meta data value for specific key." + " Required Permission: "
            + SpPermission.UPDATE_REPOSITORY)
    public void updateMetadata() throws Exception {
        // prepare and create metadata for update
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";
        final String updateValue = "valueForUpdate";

        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        distributionSetManagement.createMetaData(testDS.getId(),
                Arrays.asList(entityFactory.generateDsMetadata(knownKey, knownValue)));

        final JSONObject jsonObject = new JSONObject().put("key", knownKey).put("value", updateValue);

        mockMvc.perform(put(
                MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata/{metadatakey}",
                testDS.getId(), knownKey).contentType(MediaType.APPLICATION_JSON).content(jsonObject.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                                parameterWithName("metadatakey").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(requestFieldWithPath("key").description(MgmtApiModelProperties.META_DATA_KEY),
                                requestFieldWithPath("value").description(MgmtApiModelProperties.META_DATA_VALUE)),
                        responseFields(fieldWithPath("key").description(MgmtApiModelProperties.META_DATA_KEY),
                                fieldWithPath("value").description(MgmtApiModelProperties.META_DATA_VALUE))));

    }

    @Test
    @Description("Delete a single meta data." + " Required Permission: " + SpPermission.UPDATE_REPOSITORY)
    public void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = "knownKey";
        final String knownValue = "knownValue";

        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        distributionSetManagement.createMetaData(testDS.getId(),
                Arrays.asList(entityFactory.generateDsMetadata(knownKey, knownValue)));

        mockMvc.perform(
                delete(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata/{key}",
                        testDS.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(this.document.document(pathParameters(
                        parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID),
                        parameterWithName("key").description(ApiModelPropertiesGeneric.ITEM_ID))));

    }

    @Test
    @Description("Create a list of meta data entries" + " Required Permission: " + SpPermission.READ_REPOSITORY
            + " and " + SpPermission.UPDATE_TARGET)
    public void createMetadata() throws Exception {

        final DistributionSet testDS = testdataFactory.createDistributionSet("one");

        final String knownKey1 = "knownKey1";
        final String knownKey2 = "knownKey2";

        final String knownValue1 = "knownValue1";
        final String knownValue2 = "knownValue2";

        final JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        jsonArray.put(new JSONObject().put("key", knownKey2).put("value", knownValue2));

        mockMvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata",
                testDS.getId()).contentType(MediaType.APPLICATION_JSON).content(jsonArray.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("distributionSetId").description(ApiModelPropertiesGeneric.ITEM_ID)),
                        requestFields(requestFieldWithPath("[]key").description(MgmtApiModelProperties.META_DATA_KEY),
                                optionalRequestFieldWithPath("[]value")
                                        .description(MgmtApiModelProperties.META_DATA_VALUE))));
    }
}

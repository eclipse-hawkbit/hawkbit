/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class MgmtTargetGroupResourceTest extends AbstractManagementApiIntegrationTest {


    @Test
    void shouldRetrieveDistinctTargetGroups() throws Exception {

        final List<String> expectedGroups = List.of("Europe", "Asia");
        targetManagement.create(entityFactory.target().create().controllerId("target1").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("target2").group("Asia"));
        targetManagement.create(entityFactory.target().create().controllerId("target3").group("Europe"));

        mvc.perform(get(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0]", Matchers.in(expectedGroups)))
                .andExpect(jsonPath("$.[1]", Matchers.in(expectedGroups)));
    }

    @Test
    void shouldRetrieveTargetsFilteredByGroupAndParentGroupCorrectly() throws Exception {
        targetManagement.create(entityFactory.target().create().controllerId("target1").group("Europe/West"));
        targetManagement.create(entityFactory.target().create().controllerId("target2").group("Europe/East"));
        targetManagement.create(entityFactory.target().create().controllerId("target3").group("Europe"));

        mvc.perform(get(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                .param("group", "Europe/East")
                .param("subgroups", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(1)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target2")));

        mvc.perform(get(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                        .param("group", "Europe")
                        .param("subgroups", "true")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(3)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target1")))
                .andExpect(jsonPath("content.[1].controllerId", Matchers.equalTo("target2")))
                .andExpect(jsonPath("content.[2].controllerId", Matchers.equalTo("target3")));
    }

    @Test
    void shouldGetAssignedTargetsToSpecificGroup() throws Exception {
        targetManagement.create(entityFactory.target().create().controllerId("target1").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("target2").group("US"));
        targetManagement.create(entityFactory.target().create().controllerId("target3").group("Europe"));

        mvc.perform(get(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/Europe/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(2)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target1")))
                .andExpect(jsonPath("content.[1].controllerId", Matchers.equalTo("target3")));
    }

    @Test
    void shouldAssignListOfTargetsToASpecificGroup() throws Exception {
        targetManagement.create(entityFactory.target().create().controllerId("target1").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("target2"));
        targetManagement.create(entityFactory.target().create().controllerId("target3").group("Europe"));

        mvc.perform(put(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/newGroup/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Arrays.asList("target1", "target2", "target3"))))
                .andExpect(status().isOk());


        mvc.perform(get(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/newGroup/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(3)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target1")))
                .andExpect(jsonPath("content.[1].controllerId", Matchers.equalTo("target2")))
                .andExpect(jsonPath("content.[2].controllerId", Matchers.equalTo("target3")));
    }

    @Test
    void shouldReturnBadRequestWhenProvidingAnEmptyListOfControllerIds() throws Exception {
        mvc.perform(put(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/someGroup/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Collections.emptyList())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAssignListOfTargetsToProvidedGroupWithSubgroup() throws Exception {
        targetManagement.create(entityFactory.target().create().controllerId("target1").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("target2"));
        targetManagement.create(entityFactory.target().create().controllerId("target3").group("US"));

        mvc.perform(put(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Arrays.asList("target1", "target2", "target3")))
                        .param("group", "Europe/East"))
                .andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .param("group", "Europe/East")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(3)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target1")))
                .andExpect(jsonPath("content.[1].controllerId", Matchers.equalTo("target2")))
                .andExpect(jsonPath("content.[2].controllerId", Matchers.equalTo("target3")));

        // expect bad request if empty controllerIds
        mvc.perform(put(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Collections.emptyList()))
                        .param("group", "doesNotMatter"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAssignTargetsToProvidedGroupByRsql() throws Exception {
        targetManagement.create(entityFactory.target().create().controllerId("target1").group("A"));
        targetManagement.create(entityFactory.target().create().controllerId("target2"));
        targetManagement.create(entityFactory.target().create().controllerId("shouldNotAssign").group("B"));

        mvc.perform(put(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/C")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("q", "controllerId==target*"))
                .andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .param("group", "C")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(2)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target1")))
                .andExpect(jsonPath("content.[1].controllerId", Matchers.equalTo("target2")));
    }

    @Test
    void shouldUnassignTargetsFromGroup() throws Exception {
        targetManagement.create(entityFactory.target().create().controllerId("target1").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("target2").group("Europe"));

        mvc.perform(delete(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Arrays.asList("target1", "target2"))))
                .andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .param("group", "Europe")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(0)));

        // expect bad request if empty
        mvc.perform(delete(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Collections.emptyList())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUnassignTargetsFromGroupByRsqlFilter() throws Exception {
        targetManagement.create(entityFactory.target().create().controllerId("target1").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("target2").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("nonMatchingTarget").group("Europe"));

        mvc.perform(delete(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("q", "controllerId==target*"))
                .andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .param("group", "Europe")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(1)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("nonMatchingTarget")));
    }

    @Test
    void shouldUpdateTargetGroupsOfTargetsMatchingTheRsqlFilter() throws Exception {
        targetManagement.create(entityFactory.target().create().controllerId("target1").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("target2").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("target3").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("shouldNotBeUpdated1").group("Europe"));
        targetManagement.create(entityFactory.target().create().controllerId("shouldNotBeUpdated2").group("Europe"));

        mvc.perform(put(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("group", "Europe/East")
                        .param("q", "controllerId==target*"))
                .andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
                        .param("group", "Europe/East")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(3)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target1")))
                .andExpect(jsonPath("content.[1].controllerId", Matchers.equalTo("target2")))
                .andExpect(jsonPath("content.[2].controllerId", Matchers.equalTo("target3")));


    }
}

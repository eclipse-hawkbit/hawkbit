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

import static org.eclipse.hawkbit.repository.TargetManagement.Create.builder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetGroupRestApi;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

public class MgmtTargetGroupResourceTest extends AbstractManagementApiIntegrationTest {

    @Test
    void shouldRetrieveDistinctTargetGroups() throws Exception {

        final List<String> expectedGroups = List.of("Europe", "Asia");
        targetManagement.create(builder().controllerId("target1").group("Europe").build());
        targetManagement.create(builder().controllerId("target2").group("Asia").build());
        targetManagement.create(builder().controllerId("target3").group("Europe").build());

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0]", Matchers.in(expectedGroups)))
                .andExpect(jsonPath("$.[1]", Matchers.in(expectedGroups)));
    }

    @Test
    void shouldRetrieveTargetsFilteredByGroupAndParentGroupCorrectly() throws Exception {
        targetManagement.create(builder().controllerId("target1").group("Europe/West").build());
        targetManagement.create(builder().controllerId("target2").group("Europe/East").build());
        targetManagement.create(builder().controllerId("target3").group("Europe").build());

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
                        .param("group", "Europe/East")
                        .param("subgroups", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(1)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target2")));

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
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
        targetManagement.create(builder().controllerId("target1").group("Europe").build());
        targetManagement.create(builder().controllerId("target2").group("US").build());
        targetManagement.create(builder().controllerId("target3").group("Europe").build());

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/Europe/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(2)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target1")))
                .andExpect(jsonPath("content.[1].controllerId", Matchers.equalTo("target3")));
    }

    @Test
    void shouldAssignListOfTargetsToASpecificGroup() throws Exception {
        targetManagement.create(builder().controllerId("target1").group("Europe").build());
        targetManagement.create(builder().controllerId("target2").build());
        targetManagement.create(builder().controllerId("target3").group("Europe").build());

        mvc.perform(put(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/newGroup/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Arrays.asList("target1", "target2", "target3"))))
                .andExpect(status().isNoContent());

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/newGroup/assigned")
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
        mvc.perform(put(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/someGroup/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Collections.emptyList())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAssignListOfTargetsToProvidedGroupWithSubgroup() throws Exception {
        targetManagement.create(builder().controllerId("target1").group("Europe").build());
        targetManagement.create(builder().controllerId("target2").build());
        targetManagement.create(builder().controllerId("target3").group("US").build());

        mvc.perform(put(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Arrays.asList("target1", "target2", "target3")))
                        .param("group", "Europe/East"))
                .andExpect(status().isNoContent());

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .param("group", "Europe/East")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(3)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target1")))
                .andExpect(jsonPath("content.[1].controllerId", Matchers.equalTo("target2")))
                .andExpect(jsonPath("content.[2].controllerId", Matchers.equalTo("target3")));

        // expect bad request if empty controllerIds
        mvc.perform(put(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Collections.emptyList()))
                        .param("group", "doesNotMatter"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAssignTargetsToProvidedGroupByRsql() throws Exception {
        targetManagement.create(builder().controllerId("target1").group("A").build());
        targetManagement.create(builder().controllerId("target2").build());
        targetManagement.create(builder().controllerId("shouldNotAssign").group("B").build());

        mvc.perform(put(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/C")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("q", "controllerId==target*"))
                .andExpect(status().isNoContent());

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
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
        targetManagement.create(builder().controllerId("target1").group("Europe").build());
        targetManagement.create(builder().controllerId("target2").group("Europe").build());

        mvc.perform(delete(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Arrays.asList("target1", "target2"))))
                .andExpect(status().isNoContent());

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .param("group", "Europe")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(0)));

        // expect bad request if empty
        mvc.perform(delete(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Collections.emptyList())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUnassignTargetsFromGroupByRsqlFilter() throws Exception {
        targetManagement.create(builder().controllerId("target1").group("Europe").build());
        targetManagement.create(builder().controllerId("target2").group("Europe").build());
        targetManagement.create(builder().controllerId("nonMatchingTarget").group("Europe").build());

        mvc.perform(delete(MgmtTargetGroupRestApi.TARGETGROUPS_V1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("q", "controllerId==target*"))
                .andExpect(status().isNoContent());

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .param("group", "Europe")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(1)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("nonMatchingTarget")));
    }

    @Test
    void shouldUpdateTargetGroupsOfTargetsMatchingTheRsqlFilter() throws Exception {
        targetManagement.create(builder().controllerId("target1").group("Europe").build());
        targetManagement.create(builder().controllerId("target2").group("Europe").build());
        targetManagement.create(builder().controllerId("target3").group("Europe").build());
        targetManagement.create(builder().controllerId("shouldNotBeUpdated1").group("Europe").build());
        targetManagement.create(builder().controllerId("shouldNotBeUpdated2").group("Europe").build());

        mvc.perform(put(MgmtTargetGroupRestApi.TARGETGROUPS_V1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("group", "Europe/East")
                        .param("q", "controllerId==target*"))
                .andExpect(status().isNoContent());

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/assigned")
                        .param("group", "Europe/East")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(3)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target1")))
                .andExpect(jsonPath("content.[1].controllerId", Matchers.equalTo("target2")))
                .andExpect(jsonPath("content.[2].controllerId", Matchers.equalTo("target3")));
    }

    @Test
    void shouldAssignGroupToTargetsFilteredByTagNotEqual() throws Exception {
        targetManagement.create(builder().controllerId("target1").build());
        targetManagement.create(builder().controllerId("target2").build());
        targetManagement.create(builder().controllerId("target3").build());

        final TargetTag tag1 = targetTagManagement.create(TargetTagManagement.Create.builder().name("tag1").build());
        final TargetTag tag2 = targetTagManagement.create(TargetTagManagement.Create.builder().name("tag2").build());

        targetManagement.assignTag(List.of("target1"), tag1.getId());
        targetManagement.assignTag(List.of("target2"), tag1.getId());
        targetManagement.assignTag(List.of("target3"), tag2.getId());

        mvc.perform(put(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/FilteredGroup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("q", "tag!=tag1"))
                .andExpect(status().isNoContent());

        mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/FilteredGroup/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content", Matchers.hasSize(1)))
                .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("target3")));
    }

    @Test
    void shouldAssignGroupInChunksWhenTargetCountExceedsChunkSize() throws Exception {
        // create 5 targets with tag "exclude", 2 without — chunk size 2 forces multiple batches
        final TargetTag excludeTag = targetTagManagement.create(TargetTagManagement.Create.builder().name("exclude").build());
        for (int i = 1; i <= 5; i++) {
            targetManagement.create(builder().controllerId("chunked-" + i).build());
        }
        targetManagement.create(builder().controllerId("excluded-1").build());
        targetManagement.create(builder().controllerId("excluded-2").build());
        targetManagement.assignTag(List.of("excluded-1", "excluded-2"), excludeTag.getId());

        // override chunk size to 2 for this test
        ReflectionTestUtils.setField(targetManagement, "assignTargetGroupChunkSize", 2);
        try {
            // tag!=exclude triggers chunked path and should assign only non-tagged targets
            mvc.perform(put(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/ChunkedGroup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("q", "tag!=exclude"))
                    .andExpect(status().isNoContent());

            mvc.perform(get(MgmtTargetGroupRestApi.TARGETGROUPS_V1 + "/ChunkedGroup/assigned")
                            .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("content", Matchers.hasSize(5)))
                    .andExpect(jsonPath("content.[0].controllerId", Matchers.equalTo("chunked-1")))
                    .andExpect(jsonPath("content.[1].controllerId", Matchers.equalTo("chunked-2")))
                    .andExpect(jsonPath("content.[2].controllerId", Matchers.equalTo("chunked-3")))
                    .andExpect(jsonPath("content.[3].controllerId", Matchers.equalTo("chunked-4")))
                    .andExpect(jsonPath("content.[4].controllerId", Matchers.equalTo("chunked-5")));
        } finally {
            // restore default
            ReflectionTestUtils.setField(targetManagement, "assignTargetGroupChunkSize", 1000);
        }
    }
}

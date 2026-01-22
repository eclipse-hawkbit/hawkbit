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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.resource.util.ResourceUtility;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Feature: Component Tests - Management API<br/>
 * Story: Distribution Set Tag Resource
 */
class MgmtDistributionSetTagResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String DISTRIBUTIONSETTAGS_ROOT = "http://localhost" + MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/";

    /**
     * Verifies that a paged result list of DS tags reflects the content on the repository side.
     */
    @Test
    @ExpectEvents({ @Expect(type = DistributionSetTagCreatedEvent.class, count = 2) })
    void getDistributionSetTags() throws Exception {
        final List<DistributionSetTag> tags = testdataFactory.createDistributionSetTags(2);
        final DistributionSetTag assigned = tags.get(0);
        final DistributionSetTag unassigned = tags.get(1);

        mvc.perform(get(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyBaseEntityMatcherOnPagedResult(assigned))
                .andExpect(applyBaseEntityMatcherOnPagedResult(unassigned))
                .andExpect(applySelfLinkMatcherOnPagedResult(assigned, DISTRIBUTIONSETTAGS_ROOT + assigned.getId()))
                .andExpect(applySelfLinkMatcherOnPagedResult(unassigned, DISTRIBUTIONSETTAGS_ROOT + unassigned.getId()))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));
    }

    /**
     * Handles the GET request of retrieving all distribution set tags based by parameter
     */
    @Test
    void getDistributionSetTagsWithParameters() throws Exception {
        testdataFactory.createDistributionSetTags(2);
        mvc.perform(get(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "?limit=10&sort=name:ASC&offset=0&q=name==DsTag"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    /**
     * Verifies that a paged result list of DS tags reflects the content on the repository side when filtered by distribution set id.
     */
    @Test
    void getDistributionSetTagsByDistributionSetId() throws Exception {
        final List<DistributionSetTag> tags = testdataFactory.createDistributionSetTags(2);
        final DistributionSetTag tag1 = tags.get(0);
        final DistributionSetTag tag2 = tags.get(1);

        final DistributionSet distributionSet1 = testdataFactory.createDistributionSet();
        final DistributionSet distributionSet2 = testdataFactory.createDistributionSet();
        distributionSetManagement.assignTag(List.of(distributionSet1.getId(), distributionSet2.getId()), tag1.getId());
        distributionSetManagement.assignTag(List.of(distributionSet1.getId()), tag2.getId());

        mvc.perform(get(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1)
                        .queryParam(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "distributionset.id==" + distributionSet1.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyBaseEntityMatcherOnPagedResult(tag1))
                .andExpect(applyBaseEntityMatcherOnPagedResult(tag2))
                .andExpect(applySelfLinkMatcherOnPagedResult(tag1, DISTRIBUTIONSETTAGS_ROOT + tag1.getId()))
                .andExpect(applySelfLinkMatcherOnPagedResult(tag2, DISTRIBUTIONSETTAGS_ROOT + tag2.getId()))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

        mvc.perform(get(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1)
                        .queryParam(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "distributionset.id==" + distributionSet2.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyBaseEntityMatcherOnPagedResult(tag1))
                .andExpect(applySelfLinkMatcherOnPagedResult(tag1, DISTRIBUTIONSETTAGS_ROOT + tag1.getId()))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(1)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    /**
     * Verifies that a paged result list of DS tags reflects the content on the repository side when filtered by distribution set id field AND tag field.
     */
    @Test
    void getDistributionSetTagsByDistributionSetIdAndTagDescription() throws Exception {
        final List<DistributionSetTag> tags = testdataFactory.createDistributionSetTags(2);
        final DistributionSetTag tag1 = tags.get(0);
        final DistributionSetTag tag2 = tags.get(1);

        final DistributionSet distributionSet1 = testdataFactory.createDistributionSet();
        final DistributionSet distributionSet2 = testdataFactory.createDistributionSet();
        distributionSetManagement.assignTag(List.of(distributionSet1.getId(), distributionSet2.getId()), tag1.getId());
        distributionSetManagement.assignTag(List.of(distributionSet1.getId()), tag2.getId());

        // pass here q directly as a pure string because .queryParam method delimiters the parameters in q with ,
        // which is logical OR, we want AND here
        mvc.perform(get(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1
                        + "?" + MgmtRestConstants.REQUEST_PARAMETER_SEARCH +
                        "=distributionset.id==" + distributionSet1.getId() + ";description==" + tag1.getDescription())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyBaseEntityMatcherOnPagedResult(tag1))
                .andExpect(applySelfLinkMatcherOnPagedResult(tag1, DISTRIBUTIONSETTAGS_ROOT + tag1.getId()))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(1)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    /**
     * Verifies that a single result of a DS tag reflects the content on the repository side.
     */
    @Test
    @ExpectEvents({ @Expect(type = DistributionSetTagCreatedEvent.class, count = 2) })
    void getDistributionSetTag() throws Exception {
        final List<DistributionSetTag> tags = testdataFactory.createDistributionSetTags(2);
        final DistributionSetTag assigned = tags.get(0);

        mvc.perform(get(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + assigned.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyTagMatcherOnSingleResult(assigned))
                .andExpect(applySelfLinkMatcherOnSingleResult(DISTRIBUTIONSETTAGS_ROOT + assigned.getId()))
                .andExpect(jsonPath("_links.assignedDistributionSets.href",
                        equalTo(DISTRIBUTIONSETTAGS_ROOT + assigned.getId() + "/assigned?offset=0&limit=50")));
    }

    /**
     * Verifies that created DS tags are stored in the repository as send to the API.
     */
    @Test
    @ExpectEvents({ @Expect(type = DistributionSetTagCreatedEvent.class, count = 2) })
    void createDistributionSetTags() throws Exception {
        final DistributionSetTagManagement.Create tagOne = DistributionSetTagManagement.Create.builder()
                .colour("testcol1").description("its a test1").name("thetest1")
                .build();
        final DistributionSetTagManagement.Create tagTwo = DistributionSetTagManagement.Create.builder()
                .colour("testcol2").description("its a test2").name("thetest2")
                .build();

        final ResultActions result = mvc
                .perform(post(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1)
                        .content(toJson(List.of(tagOne, tagTwo)))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        final Tag createdOne = distributionSetTagManagement.findByRsql("name==thetest1", PAGE).getContent().get(0);
        assertThat(createdOne.getName()).isEqualTo(tagOne.getName());
        assertThat(createdOne.getDescription()).isEqualTo(tagOne.getDescription());
        assertThat(createdOne.getColour()).isEqualTo(tagOne.getColour());
        final Tag createdTwo = distributionSetTagManagement.findByRsql("name==thetest2", PAGE).getContent().get(0);
        assertThat(createdTwo.getName()).isEqualTo(tagTwo.getName());
        assertThat(createdTwo.getDescription()).isEqualTo(tagTwo.getDescription());
        assertThat(createdTwo.getColour()).isEqualTo(tagTwo.getColour());

        result.andExpect(applyTagMatcherOnArrayResult(createdOne)).andExpect(applyTagMatcherOnArrayResult(createdTwo));
    }

    /**
     * Verifies that an updated DS tag is stored in the repository as send to the API.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetTagUpdatedEvent.class, count = 1) })
    void updateDistributionSetTag() throws Exception {
        final List<DistributionSetTag> tags = testdataFactory.createDistributionSetTags(1);
        final DistributionSetTag original = tags.get(0);

        final DistributionSetTagManagement.Update update = DistributionSetTagManagement.Update.builder()
                .name("updatedName").colour("updatedCol").description("updatedDesc")
                .build();

        final ResultActions result = mvc
                .perform(put(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + original.getId())
                        .content(toJson(update)).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        final Tag updated = distributionSetTagManagement.findByRsql("name==updatedName", PAGE).getContent().get(0);
        assertThat(updated.getName()).isEqualTo(update.getName());
        assertThat(updated.getDescription()).isEqualTo(update.getDescription());
        assertThat(updated.getColour()).isEqualTo(update.getColour());

        result.andExpect(applyTagMatcherOnArrayResult(updated))
                .andExpect(applyTagMatcherOnArrayResult(updated));
    }

    /**
     * Verifies that the delete call is reflected by the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetTagDeletedEvent.class, count = 1) })
    void deleteDistributionSetTag() throws Exception {
        final List<DistributionSetTag> tags = testdataFactory.createDistributionSetTags(1);
        final DistributionSetTag original = tags.get(0);

        mvc.perform(delete(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + original.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(distributionSetTagManagement.find(original.getId())).isNotPresent();
    }

    /**
     * Ensures that assigned DS to tag in repository are listed with proper paging results.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 5),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 5) })
    void getAssignedDistributionSets() throws Exception {
        final DistributionSetTag tag = testdataFactory.createDistributionSetTags(1).get(0);
        final int setsAssigned = 5;
        final List<DistributionSet> sets = testdataFactory.createDistributionSetsWithoutModules(setsAssigned);
        distributionSetManagement.assignTag(sets.stream().map(BaseEntity::getId).toList(), tag.getId());

        mvc.perform(get(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + tag.getId() + "/assigned"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(setsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(setsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(setsAssigned)));
    }

    /**
     * Ensures that assigned DS to tag in repository are listed with proper paging results with paging limit parameter.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 5),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 5) })
    void getAssignedDistributionSetsWithPagingLimitRequestParameter() throws Exception {
        final DistributionSetTag tag = testdataFactory.createDistributionSetTags(1).get(0);
        final int setsAssigned = 5;
        final int limitSize = 1;
        final List<DistributionSet> sets = testdataFactory.createDistributionSetsWithoutModules(setsAssigned);
        distributionSetManagement.assignTag(sets.stream().map(BaseEntity::getId).toList(), tag.getId());

        mvc.perform(get(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(setsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    /**
     * Ensures that assigned DS to tag in repository are listed with proper paging results with paging limit and offset parameter.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 5),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 5) })
    void getAssignedDistributionSetsWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final DistributionSetTag tag = testdataFactory.createDistributionSetTags(1).get(0);
        final int setsAssigned = 5;
        final int offsetParam = 2;
        final int expectedSize = setsAssigned - offsetParam;

        final List<DistributionSet> sets = testdataFactory.createDistributionSetsWithoutModules(setsAssigned);
        distributionSetManagement.assignTag(sets.stream().map(BaseEntity::getId).toList(), tag.getId());

        mvc.perform(get(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(setsAssigned)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(setsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    /**
     * Verifies that tag assignments done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 1) })
    void assignDistributionSet() throws Exception {
        final DistributionSetTag tag = testdataFactory.createDistributionSetTags(1).get(0);
        final DistributionSet set = testdataFactory.createDistributionSetsWithoutModules(1).get(0);

        mvc.perform(post(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + tag.getId() + "/assigned/" +
                        set.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        final List<? extends DistributionSet> updated = distributionSetManagement.findByTag(tag.getId(), PAGE).getContent();
        assertThat(updated.stream().map(DistributionSet::getId).toList()).containsOnly(set.getId());
    }

    /**
     * Verifies that tag assignments done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2) })
    void assignDistributionSets() throws Exception {
        final DistributionSetTag tag = testdataFactory.createDistributionSetTags(1).get(0);
        final List<DistributionSet> sets = testdataFactory.createDistributionSetsWithoutModules(2);

        mvc.perform(post(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .content(toJson(sets.stream().map(DistributionSet::getId).toList()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        final List<? extends DistributionSet> updated = distributionSetManagement.findByTag(tag.getId(), PAGE).getContent();
        assertThat(updated.stream().map(DistributionSet::getId).toList())
                .containsAll(sets.stream().map(DistributionSet::getId).toList());
    }

    /**
     * Verifies that tag unassignments done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 3) })
    void unassignDistributionSet() throws Exception {
        final DistributionSetTag tag = testdataFactory.createDistributionSetTags(1).get(0);
        final int setsAssigned = 2;
        final List<DistributionSet> sets = testdataFactory.createDistributionSetsWithoutModules(setsAssigned);
        final DistributionSet assigned = sets.get(0);
        final DistributionSet unassigned = sets.get(1);

        distributionSetManagement.assignTag(sets.stream().map(BaseEntity::getId).toList(), tag.getId());

        mvc.perform(delete(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + tag.getId() + "/assigned/" +
                        unassigned.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        final List<? extends DistributionSet> updated = distributionSetManagement.findByTag(tag.getId(), PAGE).getContent();
        assertThat(updated.stream().map(DistributionSet::getId).toList())
                .containsOnly(assigned.getId());
    }

    /**
     * Verifies that tag unassignments done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 5) })
    void unassignDistributionSets() throws Exception {
        final DistributionSetTag tag = testdataFactory.createDistributionSetTags(1).get(0);
        final List<DistributionSet> sets = testdataFactory.createDistributionSetsWithoutModules(3);
        final DistributionSet assigned = sets.get(0);
        final DistributionSet unassigned0 = sets.get(1);
        final DistributionSet unassigned1 = sets.get(2);

        distributionSetManagement.assignTag(sets.stream().map(DistributionSet::getId).toList(), tag.getId());

        mvc.perform(delete(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .content(toJson(List.of(unassigned0.getId(), unassigned1.getId())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        final List<? extends DistributionSet> updated = distributionSetManagement.findByTag(tag.getId(), PAGE).getContent();
        assertThat(updated.stream().map(DistributionSet::getId).toList())
                .containsOnly(assigned.getId());
    }

    /**
     * Verifies that tag assignments (multi targets) done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2) })
    void assignDistributionSetsNotFound() throws Exception {
        final DistributionSetTag tag = testdataFactory.createDistributionSetTags(1).get(0);
        final List<Long> sets = testdataFactory.createDistributionSetsWithoutModules(2).stream().map(DistributionSet::getId).toList();
        final List<Long> missing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            while (true) {
                final long id = Math.abs(RND.nextLong());
                if (!sets.contains(id)) {
                    missing.add(id);
                    break;
                }
            }
        }
        Collections.sort(missing);
        final List<Long> withMissing = new ArrayList<>(sets);
        withMissing.addAll(missing);

        mvc.perform(
                        post(MgmtDistributionSetTagRestApi.DISTRIBUTIONSETTAGS_V1 + "/" + tag.getId() + "/assigned")
                                .content(toJson(withMissing))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound())
                .andExpect(handler -> {
                    final ExceptionInfo exceptionInfo = ResourceUtility.convertException(handler.getResponse().getContentAsString());
                    final Map<String, Object> info = exceptionInfo.getInfo();
                    assertThat(info).isNotNull();
                    assertThat(info.get(EntityNotFoundException.TYPE)).isEqualTo(DistributionSet.class.getSimpleName());
                    final List<String> notFound = (List<String>) info.get(EntityNotFoundException.ENTITY_ID);
                    Collections.sort(notFound);
                    assertThat(notFound).isEqualTo(missing);
                });
    }
}
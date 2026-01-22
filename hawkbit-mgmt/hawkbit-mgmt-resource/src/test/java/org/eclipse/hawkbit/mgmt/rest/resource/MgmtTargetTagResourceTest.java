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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.util.ResourceUtility;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.event.remote.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Spring MVC Tests against the MgmtTargetTagResource.
 * <p/>
 * Feature: Component Tests - Management API<br/>
 * Story: Target Tag Resource
 */
public class MgmtTargetTagResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String TARGETTAGS_ROOT = "http://localhost" + MgmtTargetTagRestApi.TARGETTAGS_V1 + "/";

    /**
     * Verifies that a paged result list of target tags reflects the content on the repository side.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetTagCreatedEvent.class, count = 2) })
    public void getTargetTags() throws Exception {
        final List<? extends TargetTag> tags = testdataFactory.createTargetTags(2, "");
        final TargetTag assigned = tags.get(0);
        final TargetTag unassigned = tags.get(1);

        mvc.perform(get(MgmtTargetTagRestApi.TARGETTAGS_V1).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyTagMatcherOnPagedResult(assigned))
                .andExpect(applyTagMatcherOnPagedResult(unassigned))
                .andExpect(applySelfLinkMatcherOnPagedResult(assigned, TARGETTAGS_ROOT + assigned.getId()))
                .andExpect(applySelfLinkMatcherOnPagedResult(unassigned, TARGETTAGS_ROOT + unassigned.getId()))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

    }

    /**
     * Handles the GET request of retrieving all targets tags within SP based by parameter
     */
    @Test
     void getTargetTagsWithParameters() throws Exception {
        testdataFactory.createTargetTags(2, "");
        mvc.perform(get(MgmtTargetTagRestApi.TARGETTAGS_V1 + "?limit=10&sort=name:ASC&offset=0&q=name==targetTag"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print());
    }

    /**
     * Verifies that a page result when listing tags reflects on the content in the repository when filtered by 2 fields - one tag field and one target field
     */
    @Test
     void getTargetTagsFilteredByColor() throws Exception {
        final String controllerId1 = "controllerTestId1";
        final String controllerId2 = "controllerTestId2";
        testdataFactory.createTarget(controllerId1);
        testdataFactory.createTarget(controllerId2);

        final List<? extends TargetTag> tags = testdataFactory.createTargetTags(2, "");
        final TargetTag tag1 = tags.get(0);
        final TargetTag tag2 = tags.get(1);

        targetManagement.assignTag(List.of(controllerId1, controllerId2), tag1.getId());
        targetManagement.assignTag(List.of(controllerId2), tag2.getId());

        // pass here q directly as a pure string because .queryParam method delimiters the parameters in q with ,
        // which is logical OR, we want AND here
        mvc.perform(get(MgmtTargetTagRestApi.TARGETTAGS_V1 +
                        "?" + MgmtRestConstants.REQUEST_PARAMETER_SEARCH + "=colour==" + tag2.getColour())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyTagMatcherOnPagedResult(tag2))
                .andExpect(applySelfLinkMatcherOnPagedResult(tag2, TARGETTAGS_ROOT + tag2.getId()))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(1)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    /**
     * Verifies that a single result of a target tag reflects the content on the repository side.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetTagCreatedEvent.class, count = 2) })
    public void getTargetTag() throws Exception {
        final List<? extends TargetTag> tags = testdataFactory.createTargetTags(2, "");
        final TargetTag assigned = tags.get(0);

        mvc.perform(get(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + assigned.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyTagMatcherOnSingleResult(assigned))
                .andExpect(applySelfLinkMatcherOnSingleResult(TARGETTAGS_ROOT + assigned.getId()))
                .andExpect(jsonPath("_links.assignedTargets.href",
                        equalTo(TARGETTAGS_ROOT + assigned.getId() + "/assigned?offset=0&limit=50")));

    }

    /**
     * Verifies that created target tags are stored in the repository as send to the API.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetTagCreatedEvent.class, count = 2) })
    public void createTargetTags() throws Exception {
        final TargetTagManagement.Create tagOne = TargetTagManagement.Create.builder()
                .colour("testcol1").description("its a test1").name("thetest1")
                .build();
        final TargetTagManagement.Create tagTwo = TargetTagManagement.Create.builder()
                .colour("testcol2").description("its a test2").name("thetest2")
                .build();

        final ResultActions result = mvc
                .perform(post(MgmtTargetTagRestApi.TARGETTAGS_V1)
                        .content(toJson(List.of(tagOne, tagTwo)))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        final Tag createdOne = targetTagManagement.findByRsql("name==thetest1", PAGE).getContent().get(0);
        assertThat(createdOne.getName()).isEqualTo(tagOne.getName());
        assertThat(createdOne.getDescription()).isEqualTo(tagOne.getDescription());
        assertThat(createdOne.getColour()).isEqualTo(tagOne.getColour());
        final Tag createdTwo = targetTagManagement.findByRsql("name==thetest2", PAGE).getContent().get(0);
        assertThat(createdTwo.getName()).isEqualTo(tagTwo.getName());
        assertThat(createdTwo.getDescription()).isEqualTo(tagTwo.getDescription());
        assertThat(createdTwo.getColour()).isEqualTo(tagTwo.getColour());

        result.andExpect(applyTagMatcherOnArrayResult(createdOne)).andExpect(applyTagMatcherOnArrayResult(createdTwo));
    }

    /**
     * Verifies that an updated target tag is stored in the repository as send to the API.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetTagUpdatedEvent.class, count = 1) })
    public void updateTargetTag() throws Exception {
        final List<? extends TargetTag> tags = testdataFactory.createTargetTags(1, "");
        final TargetTag original = tags.get(0);

        final TargetTagManagement.Update update = TargetTagManagement.Update.builder()
                .name("updatedName").colour("updatedCol").description("updatedDesc")
                .build();

        final ResultActions result = mvc
                .perform(put(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + original.getId())
                        .content(toJson(update)).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        final Tag updated = targetTagManagement.findByRsql("name==updatedName", PAGE).getContent().get(0);
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
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetTagDeletedEvent.class, count = 1) })
    public void deleteTargetTag() throws Exception {
        final List<? extends TargetTag> tags = testdataFactory.createTargetTags(1, "");
        final TargetTag original = tags.get(0);

        mvc.perform(delete(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + original.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        assertThat(targetTagManagement.find(original.getId())).isNotPresent();
    }

    /**
     * Ensures that assigned targets to tag in repository are listed with proper paging results.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 5), 
            @Expect(type = TargetUpdatedEvent.class, count = 5) })
    public void getAssignedTargets() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 5;
        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);
        targetManagement.assignTag(targets.stream().map(Target::getControllerId).toList(), tag.getId());

        mvc.perform(get(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(targetsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(targetsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(targetsAssigned)));
    }

    /**
     * Ensures that assigned DS to tag in repository are listed with proper paging results with paging limit parameter.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 5),
            @Expect(type = TargetUpdatedEvent.class, count = 5) })
    public void getAssignedTargetsWithPagingLimitRequestParameter() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 5;
        final int limitSize = 1;
        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);
        targetManagement.assignTag(targets.stream().map(Target::getControllerId).toList(), tag.getId());

        mvc.perform(get(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(targetsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    /**
     * Ensures that assigned targets to tag in repository are listed with proper paging results with paging limit and offset parameter.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 5),
            @Expect(type = TargetUpdatedEvent.class, count = 5) })
    public void getAssignedTargetsWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 5;
        final int offsetParam = 2;
        final int expectedSize = targetsAssigned - offsetParam;

        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);
        targetManagement.assignTag(targets.stream().map(Target::getControllerId).toList(), tag.getId());

        mvc.perform(get(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(targetsAssigned)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(targetsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    /**
     * Verifies that tag assignments done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1) })
    public void assignTarget() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final Target assigned = testdataFactory.createTargets(1).get(0);

        mvc.perform(post(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned/" +
                        assigned.getControllerId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        final List<Target> updated = targetManagement.findByTag(tag.getId(), PAGE).getContent();
        assertThat(updated.stream().map(Target::getControllerId).toList()).containsOnly(assigned.getControllerId());
    }

    /**
     * Verifies that tag assignments (multi targets) done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2) })
    public void assignTargets() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final List<Target> targets = testdataFactory.createTargets(2);
        final Target assigned0 = targets.get(0);
        final Target assigned1 = targets.get(1);

        mvc.perform(post(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .content(toJson(List.of(assigned0.getControllerId(), assigned1.getControllerId())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        final List<Target> updated = targetManagement.findByTag(tag.getId(), PAGE).getContent();
        assertThat(updated.stream().map(Target::getControllerId).toList())
                .containsOnly(assigned0.getControllerId(), assigned1.getControllerId());
    }

    /**
     * Verifies that tag assignments (multi targets) done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2) })
    public void assignTargetsNotFound() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final List<String> targets = testdataFactory.createTargets(2).stream().map(Target::getControllerId).toList();
        final List<String> missing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            while (true) {
                final String id = String.valueOf(Math.abs(RND.nextLong()));
                if (!targets.contains(id)) {
                    missing.add(id);
                    break;
                }
            }
        }
        Collections.sort(missing);
        final List<String> withMissing = new ArrayList<>(targets);
        withMissing.addAll(missing);

        mvc.perform(post(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .content(toJson(withMissing))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound())
                .andExpect(handler -> {
                    final ExceptionInfo exceptionInfo = ResourceUtility.convertException(handler.getResponse().getContentAsString());
                    final Map<String, Object> info = exceptionInfo.getInfo();
                    assertThat(info).isNotNull();
                    assertThat(info.get(EntityNotFoundException.TYPE)).isEqualTo(Target.class.getSimpleName());
                    final List<String> notFound = (List<String>) info.get(EntityNotFoundException.ENTITY_ID);
                    Collections.sort(notFound);
                    assertThat(notFound).isEqualTo(missing);
                });
        assertThat(targetManagement.findByTag(tag.getId(), PAGE).getContent()).isEmpty();
    }

    /**
     * Verifies that tag assignments (multi targets) done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2) })
    public void assignTargetsNotFoundTagAndFail() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final List<String> targets = testdataFactory.createTargets(2).stream().map(Target::getControllerId).toList();
        final List<String> missing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            while (true) {
                final String id = String.valueOf(Math.abs(RND.nextLong()));
                if (!targets.contains(id)) {
                    missing.add(id);
                    break;
                }
            }
        }
        Collections.sort(missing);
        final List<String> withMissing = new ArrayList<>(targets);
        withMissing.addAll(missing);

        mvc.perform(post(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .param("onNotFoundPolicy", MgmtTargetTagRestApi.OnNotFoundPolicy.ON_WHAT_FOUND_AND_FAIL.name())
                        .content(toJson(withMissing))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound())
                .andExpect(handler -> {
                    final ExceptionInfo exceptionInfo = ResourceUtility.convertException(handler.getResponse().getContentAsString());
                    final Map<String, Object> info = exceptionInfo.getInfo();
                    assertThat(info).isNotNull();
                    assertThat(info.get(EntityNotFoundException.TYPE)).isEqualTo(Target.class.getSimpleName());
                    final List<String> notFound = (List<String>) info.get(EntityNotFoundException.ENTITY_ID);
                    Collections.sort(notFound);
                    assertThat(notFound).isEqualTo(missing);
                });
        assertThat(targetManagement.findByTag(tag.getId(), PAGE).getContent().stream().map(Target::getControllerId).sorted().toList())
                .isEqualTo(targets.stream().sorted().toList());
    }

    /**
     * Verifies that tag assignments (multi targets) done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2) })
    public void assignTargetsNotFoundTagAndSuccess() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final List<String> targets = testdataFactory.createTargets(2).stream().map(Target::getControllerId).toList();
        final List<String> missing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            while (true) {
                final String id = String.valueOf(Math.abs(RND.nextLong()));
                if (!targets.contains(id)) {
                    missing.add(id);
                    break;
                }
            }
        }
        Collections.sort(missing);
        final List<String> withMissing = new ArrayList<>(targets);
        withMissing.addAll(missing);

        mvc.perform(post(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .param("onNotFoundPolicy", MgmtTargetTagRestApi.OnNotFoundPolicy.ON_WHAT_FOUND_AND_SUCCESS.name())
                        .content(toJson(withMissing))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
        assertThat(targetManagement.findByTag(tag.getId(), PAGE).getContent().stream().map(Target::getControllerId).sorted().toList())
                .isEqualTo(targets.stream().sorted().toList());
    }

    /**
     * Verifies that tag unassignments done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 3) })
    public void unassignTarget() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final List<Target> targets = testdataFactory.createTargets(2);
        final Target assigned = targets.get(0);
        final Target unassigned = targets.get(1);

        targetManagement.assignTag(targets.stream().map(Target::getControllerId).toList(), tag.getId());

        mvc.perform(delete(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned/" +
                        unassigned.getControllerId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        final List<Target> updated = targetManagement.findByTag(tag.getId(), PAGE).getContent();
        assertThat(updated.stream().map(Target::getControllerId).toList())
                .containsOnly(assigned.getControllerId());
    }

    /**
     * Verifies that tag unassignments (multi targets) done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 3),
            @Expect(type = TargetUpdatedEvent.class, count = 5) })
    public void unassignTargets() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final List<Target> targets = testdataFactory.createTargets(3);
        final Target assigned = targets.get(0);
        final Target unassigned0 = targets.get(1);
        final Target unassigned1 = targets.get(2);

        targetManagement.assignTag(targets.stream().map(Target::getControllerId).toList(), tag.getId());

        mvc.perform(delete(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .content(toJson(Arrays.asList(unassigned0.getControllerId(), unassigned1.getControllerId())))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

        final List<Target> updated = targetManagement.findByTag(tag.getId(), PAGE).getContent();
        assertThat(updated.stream().map(Target::getControllerId).toList())
                .containsOnly(assigned.getControllerId());
    }

    /**
     * Verifies that tag assignments (multi targets) done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2) })
    public void unassignTargetsNotFound() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final List<String> targets = testdataFactory.createTargets(2).stream().map(Target::getControllerId).toList();
        final List<String> missing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            while (true) {
                final String id = String.valueOf(Math.abs(RND.nextLong()));
                if (!targets.contains(id)) {
                    missing.add(id);
                    break;
                }
            }
        }
        Collections.sort(missing);
        final List<String> withMissing = new ArrayList<>(targets);
        withMissing.addAll(missing);

        targetManagement.assignTag(targets, tag.getId());

        mvc.perform(delete(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .content(toJson(withMissing))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound())
                .andExpect(handler -> {
                    final ExceptionInfo exceptionInfo = ResourceUtility.convertException(handler.getResponse().getContentAsString());
                    final Map<String, Object> info = exceptionInfo.getInfo();
                    assertThat(info).isNotNull();
                    assertThat(info.get(EntityNotFoundException.TYPE)).isEqualTo(Target.class.getSimpleName());
                    final List<String> notFound = (List<String>) info.get(EntityNotFoundException.ENTITY_ID);
                    Collections.sort(notFound);
                    assertThat(notFound).isEqualTo(missing);
                });
        assertThat(targetManagement.findByTag(tag.getId(), PAGE).getContent().stream().map(Target::getControllerId).sorted().toList())
                .isEqualTo(targets.stream().sorted().toList());
    }

    /**
     * Verifies that tag assignments (multi targets) done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 4) })
    public void unassignTargetsNotFoundUntagAndFail() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final List<String> targets = testdataFactory.createTargets(2).stream().map(Target::getControllerId).toList();
        final List<String> missing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            while (true) {
                final String id = String.valueOf(Math.abs(RND.nextLong()));
                if (!targets.contains(id)) {
                    missing.add(id);
                    break;
                }
            }
        }
        Collections.sort(missing);
        final List<String> withMissing = new ArrayList<>(targets);
        withMissing.addAll(missing);

        targetManagement.assignTag(targets, tag.getId());

        mvc.perform(delete(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .param("onNotFoundPolicy", MgmtTargetTagRestApi.OnNotFoundPolicy.ON_WHAT_FOUND_AND_FAIL.name())
                        .content(toJson(withMissing))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound())
                .andExpect(handler -> {
                    final ExceptionInfo exceptionInfo = ResourceUtility.convertException(handler.getResponse().getContentAsString());
                    final Map<String, Object> info = exceptionInfo.getInfo();
                    assertThat(info).isNotNull();
                    assertThat(info.get(EntityNotFoundException.TYPE)).isEqualTo(Target.class.getSimpleName());
                    final List<String> notFound = (List<String>) info.get(EntityNotFoundException.ENTITY_ID);
                    Collections.sort(notFound);
                    assertThat(notFound).isEqualTo(missing);
                });
        assertThat(targetManagement.findByTag(tag.getId(), PAGE).getContent()).isEmpty();
    }

    /**
     * Verifies that tag assignments (multi targets) done through tag API command are correctly stored in the repository.
     */
    @Test
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 4) })
    public void unassignTargetsNotFoundUntagAndSuccess() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final List<String> targets = testdataFactory.createTargets(2).stream().map(Target::getControllerId).toList();
        final List<String> missing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            while (true) {
                final String id = String.valueOf(Math.abs(RND.nextLong()));
                if (!targets.contains(id)) {
                    missing.add(id);
                    break;
                }
            }
        }
        Collections.sort(missing);
        final List<String> withMissing = new ArrayList<>(targets);
        withMissing.addAll(missing);

        targetManagement.assignTag(targets, tag.getId());

        mvc.perform(delete(MgmtTargetTagRestApi.TARGETTAGS_V1 + "/" + tag.getId() + "/assigned")
                        .param("onNotFoundPolicy", MgmtTargetTagRestApi.OnNotFoundPolicy.ON_WHAT_FOUND_AND_SUCCESS.name())
                        .content(toJson(withMissing))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());
        assertThat(targetManagement.findByTag(tag.getId(), PAGE).getContent()).isEmpty();
    }
}
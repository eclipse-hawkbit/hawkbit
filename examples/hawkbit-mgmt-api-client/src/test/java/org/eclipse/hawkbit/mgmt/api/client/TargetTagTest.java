/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.api.client;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.app.Start;
import org.eclipse.hawkbit.rest.resource.model.tag.AssignedTargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagsRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TargetTagAssigmentResultRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetsRest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Description;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import feign.Feign;
import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

@Features("Example Tests - Management RESTful API Client")
@Stories("Target Tag Resource")
public class TargetTagTest {

    private TargetTagResource targetTagResource;

    private static List<AssignedTargetRequestBody> assignedTargetRequestBodies;

    @BeforeClass
    public static void startupServer() {
        SpringApplication.run(Start.class, new String[0]);
        createTargetsAssignment();
        assignedTargetRequestBodies.add(new AssignedTargetRequestBody().setControllerId("NotExist"));
    }

    @Before
    public void setup() {
        this.targetTagResource = createTargetTagResource();
    }

    // disabled as this runs not on CI environments.
    @Test
    @Description("Simple request of target tag by ID")
    @Ignore
    public void getTargetTag() {
        final TagsRest result = createTargetTags(2);

        assertThat(targetTagResource.getTargetTag(result.get(0).getTagId()).getName()).isEqualTo("Tag0");

        deleteTargets(result);
    }

    // disabled as this runs not on CI environments.
    @Test
    @Description("Simple update of a target tag")
    @Ignore
    public void updateTargetTag() {
        final TagsRest created = createTargetTags(10);

        targetTagResource.updateTagretTag(created.get(0).getTagId(), new TagRequestBodyPut().setDescription("Test")
                .setName("Test").setColour("Green"));

        final TagRest targetTag = targetTagResource.getTargetTag(created.get(0).getTagId());
        assertThat(targetTag.getName()).isEqualTo("Test");
        assertThat(targetTag.getDescription()).isEqualTo("Test");
        assertThat(targetTag.getColour()).isEqualTo("Green");

        deleteTargets(created);
    }

    // disabled as this runs not on CI environments.
    @Test
    @Description("Simple request of all assigned targets by a target tag.")
    @Ignore
    public void getTargetsByTargetTagId() {
        final TagsRest created = createTargetTags(10);
        final Long tagId = created.get(2).getTagId();
        targetTagResource.assignTargets(tagId, assignedTargetRequestBodies);

        final TagRest targetTag = targetTagResource.getTargetTag(tagId);
        assertThat(targetTagResource.getAssignedTargets(tagId)).hasSize(5);

        targetTagResource.unassignTargets(targetTag.getTagId());
        deleteTargets(created);
    }

    @Test
    @Description("Toggle request to unassigned all assigned targets and assign all unassigned targets.")
    @Ignore
    public void toggleTagAssignment() {
        final TagsRest created = createTargetTags(10);
        final Long id = created.get(2).getTagId();

        targetTagResource.assignTargets(id, assignedTargetRequestBodies);
        targetTagResource.unassignTarget(id, assignedTargetRequestBodies.get(0).getControllerId());

        TargetTagAssigmentResultRest assigmentResultRest = targetTagResource.toggleTagAssignment(id,
                assignedTargetRequestBodies);

        final TagRest targetTag = targetTagResource.getTargetTag(created.get(2).getTagId());
        assertThat(assigmentResultRest.getAssignedTargets()).hasSize(1);
        assertThat(assigmentResultRest.getUnassignedTargets()).hasSize(0);

        assigmentResultRest = targetTagResource.toggleTagAssignment(id, assignedTargetRequestBodies);
        assertThat(assigmentResultRest.getAssignedTargets()).hasSize(0);
        assertThat(assigmentResultRest.getUnassignedTargets()).hasSize(5);

        targetTagResource.unassignTargets(targetTag.getTagId());
        deleteTargets(created);
    }

    private void deleteTargets(final List<TagRest> tags) {
        for (final TagRest tag : tags) {
            targetTagResource.deleteTargetTag(tag.getTagId());
        }
    }

    private TagsRest createTargetTags(final int number) {

        final List<TagRequestBodyPut> tags = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            tags.add(new TagRequestBodyPut().setDescription("Tag " + i).setName("Tag" + i).setColour("Red"));
        }

        final TagsRest result = targetTagResource.createTargetTag(tags);

        assertThat(result).hasSize(number);
        return result;
    }

    private TargetTagResource createTargetTagResource() {
        final TargetTagResource targetResource = Feign.builder().logger(new Logger.ErrorLogger())
                .logLevel(Logger.Level.BASIC).decoder(new JacksonDecoder()).encoder(new JacksonEncoder())
                .requestInterceptor(new BasicAuthRequestInterceptor("admin", "admin"))
                .target(TargetTagResource.class, "http://localhost:8080");
        return targetResource;
    }

    private static void createTargetsAssignment() {

        final List<TargetRequestBody> targets = new ArrayList<>();
        assignedTargetRequestBodies = new ArrayList<>();
        for (int i = 0; i < 5; i++) {

            targets.add(new TargetRequestBody().setControllerId("test" + i).setName("testDevice"));
        }

        final TargetsRest result = createTargetResource().createTargets(targets);
        for (final TargetRest rest : result) {
            assignedTargetRequestBodies.add(new AssignedTargetRequestBody().setControllerId(rest.getControllerId()));
        }

    }

    private static TargetResource createTargetResource() {
        final TargetResource targetResource = Feign.builder().logger(new Logger.ErrorLogger())
                .logLevel(Logger.Level.BASIC).decoder(new JacksonDecoder()).encoder(new JacksonEncoder())
                .requestInterceptor(new BasicAuthRequestInterceptor("admin", "admin"))
                .target(TargetResource.class, "http://localhost:8080");
        return targetResource;
    }

}

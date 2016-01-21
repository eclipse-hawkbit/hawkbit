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
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRest;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetsRest;
import org.eclipse.hawkbit.rest.resource.model.tag.AssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.tag.DistributionSetTagAssigmentResultRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagsRest;
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
@Stories("DistrubutionSet Tag Resource")
public class DistributionSetTagTest {

    private DistrubutionSetTagResource distrubutionSetTagResource;

    private static List<AssignedDistributionSetRequestBody> assignedTargetRequestBodies;

    @BeforeClass
    public static void startupServer() {
        SpringApplication.run(Start.class, new String[0]);
        createTargetsAssignment();
        assignedTargetRequestBodies.add(new AssignedDistributionSetRequestBody().setDistributionSetId(100L));
    }

    @Before
    public void setup() {
        this.distrubutionSetTagResource = createDistrubutionSetTagResource();
    }

    // disabled as this runs not on CI environments.
    @Test
    @Description("Simple request of distrubutionset tag by ID")
    @Ignore
    public void getDistributionSetTag() {
        final TagsRest result = createDistributionSetTags(2);

        assertThat(distrubutionSetTagResource.getDistributionSetTag(result.get(0).getTagId()).getName()).isEqualTo(
                "Tag0");

        deleteDistributionSets(result);
    }

    // disabled as this runs not on CI environments.
    @Test
    @Description("Simple update of a distrubutionset tag")
    @Ignore
    public void updateDistributionSetTag() {
        final TagsRest created = createDistributionSetTags(10);

        distrubutionSetTagResource.updateDistributionSetTag(created.get(0).getTagId(), new TagRequestBodyPut()
                .setDescription("Test").setName("Test").setColour("Green"));

        final TagRest targetTag = distrubutionSetTagResource.getDistributionSetTag(created.get(0).getTagId());
        assertThat(targetTag.getName()).isEqualTo("Test");
        assertThat(targetTag.getDescription()).isEqualTo("Test");
        assertThat(targetTag.getColour()).isEqualTo("Green");

        deleteDistributionSets(created);
    }

    // disabled as this runs not on CI environments.
    @Test
    @Description("Simple request of all assigned distrubutionsets by a distrubutionset tag.")
    @Ignore
    public void getDistributionSetByTagId() {
        final TagsRest created = createDistributionSetTags(10);
        distrubutionSetTagResource.assignDistributionSets(created.get(2).getTagId(), assignedTargetRequestBodies);

        final DistributionSetsRest distributionSetsRest = distrubutionSetTagResource
                .getAssignedDistributionSets(created.get(2).getTagId());
        assertThat(distributionSetsRest).hasSize(5);

        distrubutionSetTagResource.unassignDistributionSets(created.get(2).getTagId());
        deleteDistributionSets(created);
    }

    @Test
    @Description("Toggle request to unassigned all assigned distrubutionset and assign all unassigned distrubutionset.")
    @Ignore
    public void toggleTagAssignment() {
        final TagsRest created = createDistributionSetTags(10);
        final Long id = created.get(2).getTagId();

        distrubutionSetTagResource.assignDistributionSets(id, assignedTargetRequestBodies);
        distrubutionSetTagResource.unassignDistributionSet(id, assignedTargetRequestBodies.get(0)
                .getDistributionSetId());

        DistributionSetTagAssigmentResultRest assigmentResultRest = distrubutionSetTagResource.toggleTagAssignment(id,
                assignedTargetRequestBodies);

        final TagRest targetTag = distrubutionSetTagResource.getDistributionSetTag(created.get(2).getTagId());
        assertThat(assigmentResultRest.getAssignedDistributionSets()).hasSize(1);
        assertThat(assigmentResultRest.getUnassignedDistributionSets()).hasSize(0);

        assigmentResultRest = distrubutionSetTagResource.toggleTagAssignment(id, assignedTargetRequestBodies);
        assertThat(assigmentResultRest.getAssignedDistributionSets()).hasSize(0);
        assertThat(assigmentResultRest.getUnassignedDistributionSets()).hasSize(5);

        distrubutionSetTagResource.unassignDistributionSets(targetTag.getTagId());
        deleteDistributionSets(created);
    }

    private void deleteDistributionSets(final List<TagRest> tags) {
        for (final TagRest tag : tags) {
            distrubutionSetTagResource.deleteDistributionSetTag(tag.getTagId());
        }
    }

    private TagsRest createDistributionSetTags(final int number) {

        final List<TagRequestBodyPut> tags = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            tags.add(new TagRequestBodyPut().setDescription("Tag " + i).setName("Tag" + i).setColour("Red"));
        }

        final TagsRest result = distrubutionSetTagResource.createDistributionSetTags(tags);

        assertThat(result).hasSize(number);
        return result;
    }

    private DistrubutionSetTagResource createDistrubutionSetTagResource() {
        final DistrubutionSetTagResource distrubutionSetTagResource = Feign.builder().logger(new Logger.ErrorLogger())
                .logLevel(Logger.Level.BASIC).decoder(new JacksonDecoder()).encoder(new JacksonEncoder())
                .requestInterceptor(new BasicAuthRequestInterceptor("admin", "admin"))
                .target(DistrubutionSetTagResource.class, "http://localhost:8080");
        return distrubutionSetTagResource;
    }

    private static void createTargetsAssignment() {

        final List<DistributionSetRequestBodyPost> sets = new ArrayList<>();
        assignedTargetRequestBodies = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final DistributionSetRequestBodyPost bodyPost = (DistributionSetRequestBodyPost) new DistributionSetRequestBodyPost()
                    .setName("Ds" + i).setDescription("Ds" + i).setVersion("" + i);
            sets.add(bodyPost);
        }

        final DistributionSetsRest result = createDistributionSetResource().createDistributionSets(sets);
        for (final DistributionSetRest rest : result) {
            assignedTargetRequestBodies.add(new AssignedDistributionSetRequestBody().setDistributionSetId(rest
                    .getDsId()));
        }

    }

    private static DistributionSetResource createDistributionSetResource() {
        final DistributionSetResource distributionSetResource = Feign.builder().logger(new Logger.ErrorLogger())
                .logLevel(Logger.Level.BASIC).decoder(new JacksonDecoder()).encoder(new JacksonEncoder())
                .requestInterceptor(new BasicAuthRequestInterceptor("admin", "admin"))
                .target(DistributionSetResource.class, "http://localhost:8080");
        return distributionSetResource;
    }

}

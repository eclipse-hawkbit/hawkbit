/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.deprecated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.resource.AbstractManagementApiIntegrationTest;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

@Feature("Component Tests - Management API")
@Story("Distribution Set Tag Resource")
class MgmtDeprecatedResourceTest extends AbstractManagementApiIntegrationTest {

    @Test
    @Description("Verifies that tag assignments done through toggle API command are correctly assigned or unassigned.")
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 4) })
    void toggleDistributionSetTagAssignment() throws Exception {
        final DistributionSetTag tag = testdataFactory.createDistributionSetTags(1).get(0);
        final int setsAssigned = 2;
        final List<DistributionSet> sets = testdataFactory.createDistributionSetsWithoutModules(setsAssigned);

        // 2 DistributionSetUpdateEvent
        ResultActions result = toggle(tag, sets);

        List<DistributionSet> updated = distributionSetManagement.findByTag(tag.getId(), PAGE).getContent();

        assertThat(updated.stream().map(DistributionSet::getId).toList())
                .containsAll(sets.stream().map(DistributionSet::getId).toList());

        result.andExpect(applyBaseEntityMatcherOnArrayResult(updated.get(0), "assignedDistributionSets"))
                .andExpect(applyBaseEntityMatcherOnArrayResult(updated.get(1), "assignedDistributionSets"));

        // 2 DistributionSetUpdateEvent
        result = toggle(tag, sets);

        updated = distributionSetManagement.findAll(PAGE).getContent();

        result.andExpect(applyBaseEntityMatcherOnArrayResult(updated.get(0), "unassignedDistributionSets"))
                .andExpect(applyBaseEntityMatcherOnArrayResult(updated.get(1), "unassignedDistributionSets"));

        assertThat(distributionSetManagement.findByTag(tag.getId(), PAGE)).isEmpty();
    }

    @Test
    @Description("Verifies that tag assignments done through tag API command are correctly stored in the repository.")
    @ExpectEvents({
            @Expect(type = DistributionSetTagCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 2) })
    void assignDistributionSetsWithRequestBody() throws Exception {
        final DistributionSetTag tag = testdataFactory.createDistributionSetTags(1).get(0);
        final int setsAssigned = 2;
        final List<DistributionSet> sets = testdataFactory.createDistributionSetsWithoutModules(setsAssigned);

        final ResultActions result = mvc
                .perform(
                        post(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + "/" + tag.getId() + "/assigned")
                                .content(JsonBuilder
                                        .ids(sets.stream().map(DistributionSet::getId).toList()))
                                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        final List<DistributionSet> updated = distributionSetManagement.findByTag(tag.getId(), PAGE).getContent();

        assertThat(updated.stream().map(DistributionSet::getId).toList())
                .containsAll(sets.stream().map(DistributionSet::getId).toList());

        result.andExpect(applyBaseEntityMatcherOnArrayResult(updated.get(0)))
                .andExpect(applyBaseEntityMatcherOnArrayResult(updated.get(1)));
    }

    @Test
    @Description("Verifes that tag assignments done through toggle API command are correctly assigned or unassigned.")
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 4) })
    void toggleTargetTagAssignment() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 2;
        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);

        ResultActions result = toggle(tag, targets);

        List<Target> updated = targetManagement.findByTag(PAGE, tag.getId()).getContent();

        assertThat(updated.stream().map(Target::getControllerId).toList())
                .containsAll(targets.stream().map(Target::getControllerId).toList());

        result.andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(0), "assignedTargets"))
                .andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(1), "assignedTargets"));

        result = toggle(tag, targets);

        updated = targetManagement.findAll(PAGE).getContent();

        result.andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(0), "unassignedTargets"))
                .andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(1), "unassignedTargets"));

        assertThat(targetManagement.findByTag(PAGE, tag.getId())).isEmpty();
    }

    @Test
    @Description("Verifies that tag assignments done through tag API command are correctly stored in the repository.")
    @ExpectEvents({
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2) })
    void assignTargetsByRequestBody() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 2;
        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);

        final ResultActions result = mvc
                .perform(post(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING + "/" + tag.getId() + "/assigned")
                        .content(controllerIdsOld(
                                targets.stream().map(Target::getControllerId).toList()))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        final List<Target> updated = targetManagement.findByTag(PAGE, tag.getId()).getContent();

        assertThat(updated.stream().map(Target::getControllerId).toList())
                .containsAll(targets.stream().map(Target::getControllerId).toList());

        result.andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(0)))
                .andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(1)));
    }

    private ResultActions toggle(final DistributionSetTag tag, final List<DistributionSet> sets) throws Exception {
        return mvc
                .perform(post(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + "/" + tag.getId()
                        + "/assigned/toggleTagAssignment").content(
                                JsonBuilder.ids(sets.stream().map(DistributionSet::getId).toList()))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    private static String controllerIdsOld(final Collection<String> ids) throws JSONException {
        final JSONArray list = new JSONArray();
        for (final String smID : ids) {
            list.put(new JSONObject().put("controllerId", smID));
        }

        return list.toString();
    }

    private ResultActions toggle(final TargetTag tag, final List<Target> targets) throws Exception {
        return mvc
                .perform(post(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING + "/" + tag.getId()
                        + "/assigned/toggleTagAssignment")
                        .content(controllerIdsOld(
                                targets.stream().map(Target::getControllerId).toList()))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }
}
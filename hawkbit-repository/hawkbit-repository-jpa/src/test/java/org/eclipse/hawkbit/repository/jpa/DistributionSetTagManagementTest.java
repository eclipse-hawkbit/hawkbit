/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * {@link DistributionSetTagManagement} tests.
 *
 */
@Feature("Component Tests - Repository")
@Story("DistributionSet Tag Management")
public class DistributionSetTagManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(distributionSetTagManagement.getByName(NOT_EXIST_ID)).isNotPresent();
        assertThat(distributionSetTagManagement.get(NOT_EXIST_IDL)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = DistributionSetTagUpdatedEvent.class, count = 0),
            @Expect(type = TargetTagUpdatedEvent.class, count = 0) })
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        verifyThrownExceptionBy(() -> distributionSetTagManagement.delete(NOT_EXIST_ID), "DistributionSetTag");

        verifyThrownExceptionBy(() -> distributionSetTagManagement.findByDistributionSet(PAGE, NOT_EXIST_IDL),
                "DistributionSet");

        verifyThrownExceptionBy(() -> distributionSetTagManagement.update(entityFactory.tag().update(NOT_EXIST_IDL)),
                "DistributionSetTag");
    }

    @Test
    @Description("Full DS tag lifecycle tested. Create tags, assign them to sets and delete the tags.")
    public void createAndAssignAndDeleteDistributionSetTags() {
        final Collection<DistributionSet> dsAs = testdataFactory.createDistributionSets("DS-A", 20);
        final Collection<DistributionSet> dsBs = testdataFactory.createDistributionSets("DS-B", 10);
        final Collection<DistributionSet> dsCs = testdataFactory.createDistributionSets("DS-C", 25);
        final Collection<DistributionSet> dsABs = testdataFactory.createDistributionSets("DS-AB", 5);
        final Collection<DistributionSet> dsACs = testdataFactory.createDistributionSets("DS-AC", 11);
        final Collection<DistributionSet> dsBCs = testdataFactory.createDistributionSets("DS-BC", 13);
        final Collection<DistributionSet> dsABCs = testdataFactory.createDistributionSets("DS-ABC", 9);

        final DistributionSetTag tagA = distributionSetTagManagement.create(entityFactory.tag().create().name("A"));
        final DistributionSetTag tagB = distributionSetTagManagement.create(entityFactory.tag().create().name("B"));
        final DistributionSetTag tagC = distributionSetTagManagement.create(entityFactory.tag().create().name("C"));
        final DistributionSetTag tagX = distributionSetTagManagement.create(entityFactory.tag().create().name("X"));
        final DistributionSetTag tagY = distributionSetTagManagement.create(entityFactory.tag().create().name("Y"));

        toggleTagAssignment(dsAs, tagA);
        toggleTagAssignment(dsBs, tagB);
        toggleTagAssignment(dsCs, tagC);

        toggleTagAssignment(dsABs, distributionSetTagManagement.getByName(tagA.getName()).get());
        toggleTagAssignment(dsABs, distributionSetTagManagement.getByName(tagB.getName()).get());

        toggleTagAssignment(dsACs, distributionSetTagManagement.getByName(tagA.getName()).get());
        toggleTagAssignment(dsACs, distributionSetTagManagement.getByName(tagC.getName()).get());

        toggleTagAssignment(dsBCs, distributionSetTagManagement.getByName(tagB.getName()).get());
        toggleTagAssignment(dsBCs, distributionSetTagManagement.getByName(tagC.getName()).get());

        toggleTagAssignment(dsABCs, distributionSetTagManagement.getByName(tagA.getName()).get());
        toggleTagAssignment(dsABCs, distributionSetTagManagement.getByName(tagB.getName()).get());
        toggleTagAssignment(dsABCs, distributionSetTagManagement.getByName(tagC.getName()).get());

        DistributionSetFilterBuilder distributionSetFilterBuilder;

        // search for not deleted
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true)
                .setTagNames(Arrays.asList(tagA.getName()));
        assertEquals(dsAs.spliterator().getExactSizeIfKnown() + dsABs.spliterator().getExactSizeIfKnown()
                        + dsACs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findByDistributionSetFilter(PAGE, distributionSetFilterBuilder.build())
                        .getTotalElements(),
                "filter works not correct");

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true)
                .setTagNames(Arrays.asList(tagB.getName()));
        assertEquals(
                dsBs.spliterator().getExactSizeIfKnown() + dsABs.spliterator().getExactSizeIfKnown()
                        + dsBCs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findByDistributionSetFilter(PAGE, distributionSetFilterBuilder.build())
                        .getTotalElements(),
                "filter works not correct");

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true)
                .setTagNames(Arrays.asList(tagC.getName()));
        assertEquals(dsCs.spliterator().getExactSizeIfKnown() + dsACs.spliterator().getExactSizeIfKnown()
                        + dsBCs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findByDistributionSetFilter(PAGE, distributionSetFilterBuilder.build())
                        .getTotalElements(),
                "filter works not correct");

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true)
                .setTagNames(Arrays.asList(tagX.getName()));
        assertEquals(0, distributionSetManagement
                .findByDistributionSetFilter(PAGE, distributionSetFilterBuilder.build()).getTotalElements(),
                "filter works not correct");

        assertEquals(5, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown(), "wrong tag size");

        distributionSetTagManagement.delete(tagY.getName());
        assertEquals(4, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown(), "wrong tag size");
        distributionSetTagManagement.delete(tagX.getName());
        assertEquals(3, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown(), "wrong tag size");

        distributionSetTagManagement.delete(tagB.getName());
        assertEquals(2, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown(), "wrong tag size");

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setTagNames(Arrays.asList(tagA.getName()));
        assertEquals(dsAs.spliterator().getExactSizeIfKnown() + dsABs.spliterator().getExactSizeIfKnown()
                        + dsACs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findByDistributionSetFilter(PAGE, distributionSetFilterBuilder.build())
                        .getTotalElements(),
                "filter works not correct");

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setTagNames(Arrays.asList(tagB.getName()));
        assertEquals(0, distributionSetManagement
                .findByDistributionSetFilter(PAGE, distributionSetFilterBuilder.build()).getTotalElements(),
                "filter works not correct");

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setTagNames(Arrays.asList(tagC.getName()));
        assertEquals(dsCs.spliterator().getExactSizeIfKnown() + dsACs.spliterator().getExactSizeIfKnown()
                        + dsBCs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findByDistributionSetFilter(PAGE, distributionSetFilterBuilder.build())
                        .getTotalElements(),
                "filter works not correct");
    }

    @Test
    @Description("Verifies the toogle mechanism by means on assigning tag if at least on DS in the list does not have"
            + "the tag yet. Unassign if all of them have the tag already.")
    public void assignAndUnassignDistributionSetTags() {
        final Collection<DistributionSet> groupA = testdataFactory.createDistributionSets(20);
        final Collection<DistributionSet> groupB = testdataFactory.createDistributionSets("unassigned", 20);

        final DistributionSetTag tag = distributionSetTagManagement
                .create(entityFactory.tag().create().name("tag1").description("tagdesc1"));

        // toggle A only -> A is now assigned
        DistributionSetTagAssignmentResult result = toggleTagAssignment(groupA, tag);
        assertThat(result.getAlreadyAssigned()).isEqualTo(0);
        assertThat(result.getAssigned()).isEqualTo(20);
        assertThat(result.getAssignedEntity()).containsAll(distributionSetManagement
                .get(groupA.stream().map(DistributionSet::getId).collect(Collectors.toList())));
        assertThat(result.getUnassigned()).isEqualTo(0);
        assertThat(result.getUnassignedEntity()).isEmpty();
        assertThat(result.getDistributionSetTag()).isEqualTo(tag);

        // toggle A+B -> A is still assigned and B is assigned as well
        result = toggleTagAssignment(concat(groupA, groupB), tag);
        assertThat(result.getAlreadyAssigned()).isEqualTo(20);
        assertThat(result.getAssigned()).isEqualTo(20);
        assertThat(result.getAssignedEntity()).containsAll(distributionSetManagement
                .get(groupB.stream().map(DistributionSet::getId).collect(Collectors.toList())));
        assertThat(result.getUnassigned()).isEqualTo(0);
        assertThat(result.getUnassignedEntity()).isEmpty();
        assertThat(result.getDistributionSetTag()).isEqualTo(tag);

        // toggle A+B -> both unassigned
        result = toggleTagAssignment(concat(groupA, groupB), tag);
        assertThat(result.getAlreadyAssigned()).isEqualTo(0);
        assertThat(result.getAssigned()).isEqualTo(0);
        assertThat(result.getAssignedEntity()).isEmpty();
        assertThat(result.getUnassigned()).isEqualTo(40);
        assertThat(result.getUnassignedEntity()).containsAll(distributionSetManagement
                .get(concat(groupB, groupA).stream().map(DistributionSet::getId).collect(Collectors.toList())));
        assertThat(result.getDistributionSetTag()).isEqualTo(tag);

    }

    @Test
    @Description("Ensures that a created tag is persisted in the repository as defined.")
    public void createDistributionSetTag() {
        final Tag tag = distributionSetTagManagement
                .create(entityFactory.tag().create().name("kai1").description("kai2").colour("colour"));

        assertThat(distributionSetTagRepository.findByNameEquals("kai1").get().getDescription()).as("wrong tag found")
                .isEqualTo("kai2");
        assertThat(distributionSetTagManagement.getByName("kai1").get().getColour()).as("wrong tag found")
                .isEqualTo("colour");
        assertThat(distributionSetTagManagement.get(tag.getId()).get().getColour()).as("wrong tag found")
                .isEqualTo("colour");
    }

    @Test
    @Description("Ensures that a deleted tag is removed from the repository as defined.")
    public void deleteDistributionSetTag() {
        // create test data
        final Iterable<DistributionSetTag> tags = createDsSetsWithTags();
        final DistributionSetTag toDelete = tags.iterator().next();

        for (final DistributionSet set : distributionSetRepository.findAll()) {
            assertThat(distributionSetRepository.findById(set.getId()).get().getTags()).as("Wrong tag found")
                    .contains(toDelete);
        }

        // delete
        distributionSetTagManagement.delete(tags.iterator().next().getName());

        // check
        assertThat(distributionSetTagRepository.findById(toDelete.getId())).as("Deleted tag should be null")
                .isNotPresent();
        assertThat(distributionSetTagManagement.findAll(PAGE).getContent()).as("Wrong size of tags after deletion")
                .hasSize(19);

        for (final DistributionSet set : distributionSetRepository.findAll()) {
            assertThat(distributionSetRepository.findById(set.getId()).get().getTags()).as("Wrong found tags")
                    .doesNotContain(toDelete);
        }
    }

    @Test
    @Description("Ensures that a tag cannot be created if one exists already with that name (ecpects EntityAlreadyExistsException).")
    public void failedDuplicateDsTagNameException() {
        final TagCreate tag = entityFactory.tag().create().name("A");
        distributionSetTagManagement.create(tag);

        assertThatExceptionOfType(EntityAlreadyExistsException.class).as("should not have worked as tag already exists")
                .isThrownBy(() -> distributionSetTagManagement.create(tag));
    }

    @Test
    @Description("Ensures that a tag cannot be updated to a name that already exists on another tag (ecpects EntityAlreadyExistsException).")
    public void failedDuplicateDsTagNameExceptionAfterUpdate() {
        distributionSetTagManagement.create(entityFactory.tag().create().name("A"));
        final DistributionSetTag tag = distributionSetTagManagement.create(entityFactory.tag().create().name("B"));

        assertThatExceptionOfType(EntityAlreadyExistsException.class).as("should not have worked as tag already exists")
                .isThrownBy(
                        () -> distributionSetTagManagement.update(entityFactory.tag().update(tag.getId()).name("A")));
    }

    @Test
    @Description("Tests the name update of a target tag.")
    public void updateDistributionSetTag() {

        // create test data
        final List<DistributionSetTag> tags = createDsSetsWithTags();

        // change data
        final DistributionSetTag savedAssigned = tags.iterator().next();

        // persist
        distributionSetTagManagement.update(entityFactory.tag().update(savedAssigned.getId()).name("test123"));

        // check data
        assertThat(distributionSetTagManagement.findAll(PAGE).getContent()).as("Wrong size of ds tags")
                .hasSize(tags.size());
        assertThat(distributionSetTagRepository.findById(savedAssigned.getId()).get().getName())
                .as("Wrong ds tag found").isEqualTo("test123");
    }

    @Test
    @Description("Ensures that all tags are retrieved through repository.")
    public void findDistributionSetTagsAll() {
        final List<DistributionSetTag> tags = createDsSetsWithTags();

        // test
        assertThat(distributionSetTagManagement.findAll(PAGE).getContent()).as("Wrong size of tags")
                .hasSize(tags.size());
        assertThat(distributionSetTagRepository.findAll()).as("Wrong size of tags").hasSize(20);
    }

    @Test
    @Description("Ensures that a created tags are persisted in the repository as defined.")
    public void createDistributionSetTags() {
        final List<DistributionSetTag> tags = createDsSetsWithTags();

        assertThat(distributionSetTagRepository.findAll()).as("Wrong size of tags created").hasSize(tags.size());
    }

    private List<DistributionSetTag> createDsSetsWithTags() {

        final Collection<DistributionSet> sets = testdataFactory.createDistributionSets(20);
        final Iterable<DistributionSetTag> tags = testdataFactory.createDistributionSetTags(20);

        tags.forEach(tag -> toggleTagAssignment(sets, tag));

        return distributionSetTagManagement.findAll(PAGE).getContent();
    }

    private DistributionSetFilterBuilder getDistributionSetFilterBuilder() {
        return new DistributionSetFilterBuilder();
    }

    @SafeVarargs
    private final <T> Collection<T> concat(final Collection<T>... targets) {
        final List<T> result = new ArrayList<>();
        Arrays.asList(targets).forEach(result::addAll);
        return result;
    }

}

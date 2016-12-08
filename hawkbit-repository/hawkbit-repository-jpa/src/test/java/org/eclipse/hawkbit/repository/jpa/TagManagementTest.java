/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test class for {@link TagManagement}.
 *
 */
@Features("Component Tests - Repository")
@Stories("Tag Management")
public class TagManagementTest extends AbstractJpaIntegrationTest {

    @Before
    public void setup() {
        assertThat(targetTagRepository.findAll()).as("Not tags should be available").isEmpty();
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

        final DistributionSetTag tagA = tagManagement.createDistributionSetTag(entityFactory.tag().create().name("A"));
        final DistributionSetTag tagB = tagManagement.createDistributionSetTag(entityFactory.tag().create().name("B"));
        final DistributionSetTag tagC = tagManagement.createDistributionSetTag(entityFactory.tag().create().name("C"));
        final DistributionSetTag tagX = tagManagement.createDistributionSetTag(entityFactory.tag().create().name("X"));
        final DistributionSetTag tagY = tagManagement.createDistributionSetTag(entityFactory.tag().create().name("Y"));

        distributionSetManagement.toggleTagAssignment(dsAs, tagA);
        distributionSetManagement.toggleTagAssignment(dsBs, tagB);
        distributionSetManagement.toggleTagAssignment(dsCs, tagC);

        distributionSetManagement.toggleTagAssignment(dsABs, tagManagement.findDistributionSetTag(tagA.getName()));
        distributionSetManagement.toggleTagAssignment(dsABs, tagManagement.findDistributionSetTag(tagB.getName()));

        distributionSetManagement.toggleTagAssignment(dsACs, tagManagement.findDistributionSetTag(tagA.getName()));
        distributionSetManagement.toggleTagAssignment(dsACs, tagManagement.findDistributionSetTag(tagC.getName()));

        distributionSetManagement.toggleTagAssignment(dsBCs, tagManagement.findDistributionSetTag(tagB.getName()));
        distributionSetManagement.toggleTagAssignment(dsBCs, tagManagement.findDistributionSetTag(tagC.getName()));

        distributionSetManagement.toggleTagAssignment(dsABCs, tagManagement.findDistributionSetTag(tagA.getName()));
        distributionSetManagement.toggleTagAssignment(dsABCs, tagManagement.findDistributionSetTag(tagB.getName()));
        distributionSetManagement.toggleTagAssignment(dsABCs, tagManagement.findDistributionSetTag(tagC.getName()));

        DistributionSetFilterBuilder distributionSetFilterBuilder;

        // search for not deleted
        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true)
                .setTagNames(Lists.newArrayList(tagA.getName()));
        assertEquals("filter works not correct",
                dsAs.spliterator().getExactSizeIfKnown() + dsABs.spliterator().getExactSizeIfKnown()
                        + dsACs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true)
                .setTagNames(Lists.newArrayList(tagB.getName()));
        assertEquals("filter works not correct",
                dsBs.spliterator().getExactSizeIfKnown() + dsABs.spliterator().getExactSizeIfKnown()
                        + dsBCs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true)
                .setTagNames(Lists.newArrayList(tagC.getName()));
        assertEquals("filter works not correct",
                dsCs.spliterator().getExactSizeIfKnown() + dsACs.spliterator().getExactSizeIfKnown()
                        + dsBCs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(true)
                .setTagNames(Lists.newArrayList(tagX.getName()));
        assertEquals("filter works not correct", 0, distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getTotalElements());

        assertEquals("wrong tag size", 5, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown());

        tagManagement.deleteDistributionSetTag(tagY.getName());
        assertEquals("wrong tag size", 4, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown());
        tagManagement.deleteDistributionSetTag(tagX.getName());
        assertEquals("wrong tag size", 3, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown());

        tagManagement.deleteDistributionSetTag(tagB.getName());
        assertEquals("wrong tag size", 2, distributionSetTagRepository.findAll().spliterator().getExactSizeIfKnown());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setTagNames(Lists.newArrayList(tagA.getName()));
        assertEquals("filter works not correct",
                dsAs.spliterator().getExactSizeIfKnown() + dsABs.spliterator().getExactSizeIfKnown()
                        + dsACs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setTagNames(Lists.newArrayList(tagB.getName()));
        assertEquals("filter works not correct", 0, distributionSetManagement
                .findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build()).getTotalElements());

        distributionSetFilterBuilder = getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setTagNames(Lists.newArrayList(tagC.getName()));
        assertEquals("filter works not correct",
                dsCs.spliterator().getExactSizeIfKnown() + dsACs.spliterator().getExactSizeIfKnown()
                        + dsBCs.spliterator().getExactSizeIfKnown() + dsABCs.spliterator().getExactSizeIfKnown(),
                distributionSetManagement.findDistributionSetsByFilters(pageReq, distributionSetFilterBuilder.build())
                        .getTotalElements());
    }

    private DistributionSetFilterBuilder getDistributionSetFilterBuilder() {
        return new DistributionSetFilterBuilder();
    }

    @Test
    @Description("Verifies the toogle mechanism by means on assigning tag if at least on DS in the list does not have"
            + "the tag yet. Unassign if all of them have the tag already.")
    public void assignAndUnassignDistributionSetTags() {
        final Collection<DistributionSet> groupA = testdataFactory.createDistributionSets(20);
        final Collection<DistributionSet> groupB = testdataFactory.createDistributionSets("unassigned", 20);

        final DistributionSetTag tag = tagManagement
                .createDistributionSetTag(entityFactory.tag().create().name("tag1").description("tagdesc1"));

        // toggle A only -> A is now assigned
        DistributionSetTagAssignmentResult result = distributionSetManagement.toggleTagAssignment(groupA, tag);
        assertThat(result.getAlreadyAssigned()).isEqualTo(0);
        assertThat(result.getAssigned()).isEqualTo(20);
        assertThat(result.getAssignedEntity()).containsAll(distributionSetManagement
                .findDistributionSetsAll(groupA.stream().map(set -> set.getId()).collect(Collectors.toList())));
        assertThat(result.getUnassigned()).isEqualTo(0);
        assertThat(result.getUnassignedEntity()).isEmpty();
        assertThat(result.getDistributionSetTag()).isEqualTo(tag);

        // toggle A+B -> A is still assigned and B is assigned as well
        result = distributionSetManagement.toggleTagAssignment(concat(groupA, groupB), tag);
        assertThat(result.getAlreadyAssigned()).isEqualTo(20);
        assertThat(result.getAssigned()).isEqualTo(20);
        assertThat(result.getAssignedEntity()).containsAll(distributionSetManagement
                .findDistributionSetsAll(groupB.stream().map(set -> set.getId()).collect(Collectors.toList())));
        assertThat(result.getUnassigned()).isEqualTo(0);
        assertThat(result.getUnassignedEntity()).isEmpty();
        assertThat(result.getDistributionSetTag()).isEqualTo(tag);

        // toggle A+B -> both unassigned
        result = distributionSetManagement.toggleTagAssignment(concat(groupA, groupB), tag);
        assertThat(result.getAlreadyAssigned()).isEqualTo(0);
        assertThat(result.getAssigned()).isEqualTo(0);
        assertThat(result.getAssignedEntity()).isEmpty();
        assertThat(result.getUnassigned()).isEqualTo(40);
        assertThat(result.getUnassignedEntity()).containsAll(distributionSetManagement.findDistributionSetsAll(
                concat(groupB, groupA).stream().map(set -> set.getId()).collect(Collectors.toList())));
        assertThat(result.getDistributionSetTag()).isEqualTo(tag);

    }

    @Test
    @Description("Verifies the toogle mechanism by means on assigning tag if at least on target in the list does not have"
            + "the tag yet. Unassign if all of them have the tag already.")
    public void assignAndUnassignTargetTags() {
        final List<Target> groupA = testdataFactory.createTargets(20);
        final List<Target> groupB = testdataFactory.createTargets(20, "groupb", "groupb");

        final TargetTag tag = tagManagement
                .createTargetTag(entityFactory.tag().create().name("tag1").description("tagdesc1"));

        // toggle A only -> A is now assigned
        TargetTagAssignmentResult result = toggleTagAssignment(groupA, tag);
        assertThat(result.getAlreadyAssigned()).isEqualTo(0);
        assertThat(result.getAssigned()).isEqualTo(20);
        assertThat(result.getAssignedEntity()).containsAll(targetManagement.findTargetsByControllerIDsWithTags(
                groupA.stream().map(target -> target.getControllerId()).collect(Collectors.toList())));
        assertThat(result.getUnassigned()).isEqualTo(0);
        assertThat(result.getUnassignedEntity()).isEmpty();
        assertThat(result.getTargetTag()).isEqualTo(tag);

        // toggle A+B -> A is still assigned and B is assigned as well
        result = toggleTagAssignment(concat(groupA, groupB), tag);
        assertThat(result.getAlreadyAssigned()).isEqualTo(20);
        assertThat(result.getAssigned()).isEqualTo(20);
        assertThat(result.getAssignedEntity()).containsAll(targetManagement.findTargetsByControllerIDsWithTags(
                groupB.stream().map(target -> target.getControllerId()).collect(Collectors.toList())));
        assertThat(result.getUnassigned()).isEqualTo(0);
        assertThat(result.getUnassignedEntity()).isEmpty();
        assertThat(result.getTargetTag()).isEqualTo(tag);

        // toggle A+B -> both unassigned
        result = toggleTagAssignment(concat(groupA, groupB), tag);
        assertThat(result.getAlreadyAssigned()).isEqualTo(0);
        assertThat(result.getAssigned()).isEqualTo(0);
        assertThat(result.getAssignedEntity()).isEmpty();
        assertThat(result.getUnassigned()).isEqualTo(40);
        assertThat(result.getUnassignedEntity()).containsAll(targetManagement.findTargetsByControllerIDsWithTags(
                concat(groupB, groupA).stream().map(target -> target.getControllerId()).collect(Collectors.toList())));
        assertThat(result.getTargetTag()).isEqualTo(tag);

    }

    @SafeVarargs
    private final <T> Collection<T> concat(final Collection<T>... targets) {
        final List<T> result = new ArrayList<>();
        Arrays.asList(targets).forEach(result::addAll);
        return result;
    }

    @Test
    @Description("Ensures that all tags are retrieved through repository.")
    public void findAllTargetTags() {
        final List<JpaTargetTag> tags = createTargetsWithTags();

        assertThat(targetTagRepository.findAll()).isEqualTo(targetTagRepository.findAll()).isEqualTo(tags)
                .as("Wrong tag size").hasSize(20);
    }

    @Test
    @Description("Ensures that a created tag is persisted in the repository as defined.")
    public void createTargetTag() {
        final Tag tag = tagManagement
                .createTargetTag(entityFactory.tag().create().name("kai1").description("kai2").colour("colour"));

        assertThat(targetTagRepository.findByNameEquals("kai1").getDescription()).as("wrong tag ed").isEqualTo("kai2");
        assertThat(tagManagement.findTargetTag("kai1").getColour()).as("wrong tag found").isEqualTo("colour");
        assertThat(tagManagement.findTargetTagById(tag.getId()).getColour()).as("wrong tag found").isEqualTo("colour");
    }

    @Test
    @Description("Ensures that a deleted tag is removed from the repository as defined.")
    public void deleteTargetTags() {

        // create test data
        final Iterable<JpaTargetTag> tags = createTargetsWithTags();
        final TargetTag toDelete = tags.iterator().next();

        for (final Target target : targetRepository.findAll()) {
            assertThat(targetManagement.findTargetByControllerID(target.getControllerId()).getTags())
                    .contains(toDelete);
        }

        // delete
        tagManagement.deleteTargetTag(toDelete.getName());

        // check
        for (final Target target : targetRepository.findAll()) {
            assertThat(targetManagement.findTargetByControllerID(target.getControllerId()).getTags())
                    .doesNotContain(toDelete);
        }
        assertThat(targetTagRepository.findOne(toDelete.getId())).as("No tag should be found").isNull();
        assertThat(targetTagRepository.findAll()).as("Wrong target tag size").hasSize(19);
    }

    @Test
    @Description("Tests the name update of a target tag.")
    public void updateTargetTag() {
        final List<JpaTargetTag> tags = createTargetsWithTags();

        // change data
        final TargetTag savedAssigned = tags.iterator().next();

        // persist
        tagManagement.updateTargetTag(entityFactory.tag().update(savedAssigned.getId()).name("test123"));

        // check data
        assertThat(targetTagRepository.findAll()).as("Wrong target tag size").hasSize(tags.size());
        assertThat(targetTagRepository.findOne(savedAssigned.getId()).getName()).as("wrong target tag is saved")
                .isEqualTo("test123");
        assertThat(targetTagRepository.findOne(savedAssigned.getId()).getOptLockRevision())
                .as("wrong target tag is saved").isEqualTo(2);
    }

    @Test
    @Description("Ensures that a created tag is persisted in the repository as defined.")
    public void createDistributionSetTag() {
        final Tag tag = tagManagement.createDistributionSetTag(
                entityFactory.tag().create().name("kai1").description("kai2").colour("colour"));

        assertThat(distributionSetTagRepository.findByNameEquals("kai1").getDescription()).as("wrong tag found")
                .isEqualTo("kai2");
        assertThat(tagManagement.findDistributionSetTag("kai1").getColour()).as("wrong tag found").isEqualTo("colour");
        assertThat(tagManagement.findDistributionSetTagById(tag.getId()).getColour()).as("wrong tag found")
                .isEqualTo("colour");
    }

    @Test
    @Description("Ensures that a created tags are persisted in the repository as defined.")
    public void createDistributionSetTags() {
        final List<DistributionSetTag> tags = createDsSetsWithTags();

        assertThat(distributionSetTagRepository.findAll()).as("Wrong size of tags created").hasSize(tags.size());
    }

    @Test
    @Description("Ensures that a deleted tag is removed from the repository as defined.")
    public void deleteDistributionSetTag() {
        // create test data
        final Iterable<DistributionSetTag> tags = createDsSetsWithTags();
        final DistributionSetTag toDelete = tags.iterator().next();

        for (final DistributionSet set : distributionSetRepository.findAll()) {
            assertThat(distributionSetManagement.findDistributionSetByIdWithDetails(set.getId()).getTags())
                    .as("Wrong tag found").contains(toDelete);
        }

        // delete
        tagManagement.deleteDistributionSetTag(tags.iterator().next().getName());

        // check
        assertThat(distributionSetTagRepository.findOne(toDelete.getId())).as("Deleted tag should be null").isNull();
        assertThat(tagManagement.findAllDistributionSetTags()).as("Wrong size of tags after deletion").hasSize(19);
        for (final DistributionSet set : distributionSetRepository.findAll()) {
            assertThat(distributionSetManagement.findDistributionSetByIdWithDetails(set.getId()).getTags())
                    .as("Wrong found tags").doesNotContain(toDelete);
        }
    }

    @Test
    @Description("Ensures that a tag cannot be created if one exists already with that name (ecpects EntityAlreadyExistsException).")
    public void failedDuplicateTargetTagNameException() {
        tagManagement.createTargetTag(entityFactory.tag().create().name("A"));

        try {
            tagManagement.createTargetTag(entityFactory.tag().create().name("A"));
            fail("should not have worked as tag already exists");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Ensures that a tag cannot be updated to a name that already exists on another tag (ecpects EntityAlreadyExistsException).")
    public void failedDuplicateTargetTagNameExceptionAfterUpdate() {
        tagManagement.createTargetTag(entityFactory.tag().create().name("A"));
        final TargetTag tag = tagManagement.createTargetTag(entityFactory.tag().create().name("B"));

        try {
            tagManagement.updateTargetTag(entityFactory.tag().update(tag.getId()).name("A"));
            fail("should not have worked as tag already exists");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Ensures that a tag cannot be created if one exists already with that name (ecpects EntityAlreadyExistsException).")
    public void failedDuplicateDsTagNameException() {
        tagManagement.createDistributionSetTag(entityFactory.tag().create().name("A"));
        try {
            tagManagement.createDistributionSetTag(entityFactory.tag().create().name("A"));
            fail("should not have worked as tag already exists");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Ensures that a tag cannot be updated to a name that already exists on another tag (ecpects EntityAlreadyExistsException).")
    public void failedDuplicateDsTagNameExceptionAfterUpdate() {
        tagManagement.createDistributionSetTag(entityFactory.tag().create().name("A"));
        final DistributionSetTag tag = tagManagement.createDistributionSetTag(entityFactory.tag().create().name("B"));

        try {
            tagManagement.updateDistributionSetTag(entityFactory.tag().update(tag.getId()).name("A"));
            fail("should not have worked as tag already exists");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Tests the name update of a target tag.")
    public void updateDistributionSetTag() {

        // create test data
        final List<DistributionSetTag> tags = createDsSetsWithTags();

        // change data
        final DistributionSetTag savedAssigned = tags.iterator().next();

        // persist
        tagManagement.updateDistributionSetTag(entityFactory.tag().update(savedAssigned.getId()).name("test123"));

        // check data
        assertThat(tagManagement.findAllDistributionSetTags()).as("Wrong size of ds tags").hasSize(tags.size());
        assertThat(distributionSetTagRepository.findOne(savedAssigned.getId()).getName()).as("Wrong ds tag found")
                .isEqualTo("test123");
    }

    @Test
    @Description("Ensures that all tags are retrieved through repository.")
    public void findDistributionSetTagsAll() {
        final List<DistributionSetTag> tags = createDsSetsWithTags();

        // test
        assertThat(tagManagement.findAllDistributionSetTags()).as("Wrong size of tags").hasSize(tags.size());
        assertThat(distributionSetTagRepository.findAll()).as("Wrong size of tags").hasSize(20);
    }

    private List<JpaTargetTag> createTargetsWithTags() {
        final List<Target> targets = testdataFactory.createTargets(20);
        final Iterable<TargetTag> tags = testdataFactory.createTargetTags(20, "");

        tags.forEach(tag -> toggleTagAssignment(targets, tag));

        return targetTagRepository.findAll();
    }

    private List<DistributionSetTag> createDsSetsWithTags() {

        final Collection<DistributionSet> sets = testdataFactory.createDistributionSets(20);
        final Iterable<DistributionSetTag> tags = testdataFactory.createDistributionSetTags(20);

        tags.forEach(tag -> distributionSetManagement.toggleTagAssignment(sets, tag));

        return tagManagement.findAllDistributionSetTags();
    }
}

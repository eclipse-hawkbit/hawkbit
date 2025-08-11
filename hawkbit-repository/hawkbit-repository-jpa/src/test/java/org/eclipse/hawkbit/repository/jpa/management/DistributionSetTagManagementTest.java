/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * {@link DistributionSetTagManagement} tests.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: DistributionSet Tag Management
 */
class DistributionSetTagManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Verifies that management get access reacts as specified on calls for non existing entities by means of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(distributionSetTagManagement.get(NOT_EXIST_IDL)).isNotPresent();
    }

    /**
     * Verifies that management queries react as specified on calls for non existing entities by means of throwing
     * EntityNotFoundException.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagUpdatedEvent.class, count = 0),
            @Expect(type = TargetTagUpdatedEvent.class, count = 0) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        verifyThrownExceptionBy(() -> distributionSetTagManagement.delete(NOT_EXIST_IDL), "DistributionSetTag");
        verifyThrownExceptionBy(() -> distributionSetTagManagement.update(
                DistributionSetTagManagement.Update.builder().id(NOT_EXIST_IDL).build()), "DistributionSetTag");
    }

    /**
     * Full DS tag lifecycle tested. Create tags, assign them to sets and delete the tags.
     */
    @Test
    void createAndAssignAndDeleteDistributionSetTags() {
        final Collection<DistributionSet> dsAs = testdataFactory.createDistributionSets("DS-A", 20);
        final Collection<DistributionSet> dsBs = testdataFactory.createDistributionSets("DS-B", 10);
        final Collection<DistributionSet> dsCs = testdataFactory.createDistributionSets("DS-C", 25);
        final Collection<DistributionSet> dsABs = testdataFactory.createDistributionSets("DS-AB", 5);
        final Collection<DistributionSet> dsACs = testdataFactory.createDistributionSets("DS-AC", 11);
        final Collection<DistributionSet> dsBCs = testdataFactory.createDistributionSets("DS-BC", 13);
        final Collection<DistributionSet> dsABCs = testdataFactory.createDistributionSets("DS-ABC", 9);

        final DistributionSetTag tagA = distributionSetTagManagement.create(DistributionSetTagManagement.Create.builder().name("A").build());
        final DistributionSetTag tagB = distributionSetTagManagement.create(DistributionSetTagManagement.Create.builder().name("B").build());
        final DistributionSetTag tagC = distributionSetTagManagement.create(DistributionSetTagManagement.Create.builder().name("C").build());
        final DistributionSetTag tagX = distributionSetTagManagement.create(DistributionSetTagManagement.Create.builder().name("X").build());
        final DistributionSetTag tagY = distributionSetTagManagement.create(DistributionSetTagManagement.Create.builder().name("Y").build());

        assignTag(dsAs, tagA);
        assignTag(dsBs, tagB);
        assignTag(dsCs, tagC);

        assignTag(dsABs, distributionSetTagManagement.get(tagA.getId()).orElseThrow());
        assignTag(dsABs, distributionSetTagManagement.get(tagB.getId()).orElseThrow());

        assignTag(dsACs, distributionSetTagManagement.get(tagA.getId()).orElseThrow());
        assignTag(dsACs, distributionSetTagManagement.get(tagC.getId()).orElseThrow());

        assignTag(dsBCs, distributionSetTagManagement.get(tagB.getId()).orElseThrow());
        assignTag(dsBCs, distributionSetTagManagement.get(tagC.getId()).orElseThrow());

        assignTag(dsABCs, distributionSetTagManagement.get(tagA.getId()).orElseThrow());
        assignTag(dsABCs, distributionSetTagManagement.get(tagB.getId()).orElseThrow());
        assignTag(dsABCs, distributionSetTagManagement.get(tagC.getId()).orElseThrow());

        // search for not deleted
        final DistributionSetFilter.DistributionSetFilterBuilder distributionSetFilterBuilder = getDistributionSetFilterBuilder()
                .isComplete(true);
        verifyExpectedFilteredDistributionSets(distributionSetFilterBuilder.tagNames(Arrays.asList(tagA.getName())),
                Stream.of(dsAs, dsABs, dsACs, dsABCs));
        verifyExpectedFilteredDistributionSets(distributionSetFilterBuilder.tagNames(Arrays.asList(tagB.getName())),
                Stream.of(dsBs, dsABs, dsBCs, dsABCs));
        verifyExpectedFilteredDistributionSets(distributionSetFilterBuilder.tagNames(Arrays.asList(tagC.getName())),
                Stream.of(dsCs, dsACs, dsBCs, dsABCs));
        verifyExpectedFilteredDistributionSets(distributionSetFilterBuilder.tagNames(Arrays.asList(tagX.getName())),
                Stream.empty());

        assertThat(distributionSetTagRepository.findAll()).hasSize(5);
        distributionSetTagManagement.delete(tagY.getId());
        assertThat(distributionSetTagRepository.findAll()).hasSize(4);
        distributionSetTagManagement.delete(tagX.getId());
        assertThat(distributionSetTagRepository.findAll()).hasSize(3);
        distributionSetTagManagement.delete(tagB.getId());
        assertThat(distributionSetTagRepository.findAll()).hasSize(2);

        verifyExpectedFilteredDistributionSets(distributionSetFilterBuilder.tagNames(Arrays.asList(tagA.getName())),
                Stream.of(dsAs, dsABs, dsACs, dsABCs));
        verifyExpectedFilteredDistributionSets(distributionSetFilterBuilder.tagNames(Arrays.asList(tagB.getName())),
                Stream.empty());
        verifyExpectedFilteredDistributionSets(distributionSetFilterBuilder.tagNames(Arrays.asList(tagC.getName())),
                Stream.of(dsCs, dsACs, dsBCs, dsABCs));
    }

    /**
     * Verifies assign/unassign.
     */
    @Test
    void assignAndUnassignDistributionSetTags() {
        final Collection<DistributionSet> groupA = testdataFactory.createDistributionSets(20);
        final Collection<DistributionSet> groupB = testdataFactory.createDistributionSets("unassigned", 20);

        final DistributionSetTag tag = distributionSetTagManagement
                .create(DistributionSetTagManagement.Create.builder().name("tag1").description("tagdesc1").build());

        // toggle A only -> A is now assigned
        List<? extends DistributionSet> result = assignTag(groupA, tag);
        assertThat(result)
                .hasSize(20)
                .containsAll((Collection) distributionSetManagement.get(groupA.stream().map(DistributionSet::getId).toList()));
        assertThat(
                distributionSetManagement.findByTag(tag.getId(), Pageable.unpaged()).getContent().stream()
                        .map(DistributionSet::getId)
                        .sorted()
                        .toList())
                .isEqualTo(groupA.stream().map(DistributionSet::getId).sorted().toList());

        final Collection<DistributionSet> groupAB = concat(groupA, groupB);
        // toggle A+B -> A is still assigned and B is assigned as well
        result = assignTag(groupAB, tag);
        assertThat((List) result)
                .hasSize(40)
                .containsAll(distributionSetManagement.get(groupAB.stream().map(DistributionSet::getId).toList()));
        assertThat(
                distributionSetManagement.findByTag(
                        tag.getId(), Pageable.unpaged()).getContent().stream().map(DistributionSet::getId).sorted().toList())
                .isEqualTo(groupAB.stream().map(DistributionSet::getId).sorted().toList());

        // toggle A+B -> both unassigned
        result = unassignTag(concat(groupA, groupB), tag);
        assertThat(result)
                .hasSize(40)
                .containsAll((List) distributionSetManagement.get(concat(groupB, groupA).stream().map(DistributionSet::getId).toList()));
        assertThat(distributionSetManagement.findByTag(tag.getId(), Pageable.unpaged()).getContent()).isEmpty();
    }

    /**
     * Verifies that tagging of set containing missing DS throws meaningful and correct exception.
     */
    @Test
    void failOnMissingDs() {
        final Collection<Long> group = testdataFactory.createDistributionSets(5).stream().map(DistributionSet::getId).toList();
        final DistributionSetTag tag = distributionSetTagManagement.create(
                DistributionSetTagManagement.Create.builder().name("tag1").description("tagdesc1").build());
        final List<Long> missing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            while (true) {
                final Long id = Math.abs(RND.nextLong());
                if (!group.contains(id)) {
                    missing.add(id);
                    break;
                }
            }
        }
        Collections.sort(missing);
        final Collection<Long> withMissing = concat(group, missing);
        assertThatThrownBy(() -> distributionSetManagement.assignTag(withMissing, tag.getId()))
                .matches(e -> {
                    if (e instanceof EntityNotFoundException enfe &&
                            enfe.getInfo().get(EntityNotFoundException.TYPE).equals(DistributionSet.class.getSimpleName()) &&
                            enfe.getInfo().get(EntityNotFoundException.ENTITY_ID) instanceof Collection<?> entityId) {
                        return entityId.stream().sorted().toList().equals(missing);
                    }
                    return false;
                });
    }

    /**
     * Ensures that a created tag is persisted in the repository as defined.
     */
    @Test
    void createDistributionSetTag() {
        final Tag tag = distributionSetTagManagement
                .create(DistributionSetTagManagement.Create.builder().name("kai1").description("kai2").colour("colour").build());

        assertThat(distributionSetTagRepository.findById(tag.getId()).orElseThrow().getDescription()).as("wrong tag found")
                .isEqualTo("kai2");
        assertThat(distributionSetTagManagement.get(tag.getId()).orElseThrow().getColour()).as("wrong tag found")
                .isEqualTo("colour");
        assertThat(distributionSetTagManagement.get(tag.getId()).orElseThrow().getColour()).as("wrong tag found")
                .isEqualTo("colour");
    }

    /**
     * Ensures that a deleted tag is removed from the repository as defined.
     */
    @Test
    void deleteDistributionSetTag() {
        // create test data
        final Iterable<DistributionSetTag> tags = createDsSetsWithTags();
        final DistributionSetTag toDelete = tags.iterator().next();

        for (final DistributionSet set : distributionSetRepository.findAll()) {
            assertThat(distributionSetRepository.findById(set.getId()).get().getTags()).as("Wrong tag found")
                    .contains(toDelete);
        }

        // delete
        distributionSetTagManagement.delete(tags.iterator().next().getId());

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

    /**
     * Ensures that a tag cannot be created if one exists already with that name (ecpects EntityAlreadyExistsException).
     */
    @Test
    void failedDuplicateDsTagNameException() {
        final DistributionSetTagManagement.Create tag = DistributionSetTagManagement.Create.builder().name("A").build();
        distributionSetTagManagement.create(tag);

        assertThatExceptionOfType(EntityAlreadyExistsException.class).as("should not have worked as tag already exists")
                .isThrownBy(() -> distributionSetTagManagement.create(tag));
    }

    /**
     * Ensures that a tag cannot be updated to a name that already exists on another tag (ecpects EntityAlreadyExistsException).
     */
    @Test
    void failedDuplicateDsTagNameExceptionAfterUpdate() {
        distributionSetTagManagement.create(DistributionSetTagManagement.Create.builder().name("A").build());
        final DistributionSetTag tag = distributionSetTagManagement.create(DistributionSetTagManagement.Create.builder().name("B").build());

        final DistributionSetTagManagement.Update tagUpdate = DistributionSetTagManagement.Update.builder().id(tag.getId()).name("A").build();
        assertThatExceptionOfType(EntityAlreadyExistsException.class).as("should not have worked as tag already exists")
                .isThrownBy(() -> distributionSetTagManagement.update(tagUpdate));
    }

    /**
     * Tests the name update of a target tag.
     */
    @Test
    void updateDistributionSetTag() {
        // create test data
        final List<DistributionSetTag> tags = createDsSetsWithTags();
        // change data
        final DistributionSetTag savedAssigned = tags.iterator().next();
        // persist
        distributionSetTagManagement.update(DistributionSetTagManagement.Update.builder().id(savedAssigned.getId()).name("test123").build());
        // check data
        assertThat(distributionSetTagManagement.findAll(PAGE).getContent()).as("Wrong size of ds tags")
                .hasSize(tags.size());
        assertThat(distributionSetTagRepository.findById(savedAssigned.getId()).get().getName())
                .as("Wrong ds tag found").isEqualTo("test123");
    }

    /**
     * Ensures that all tags are retrieved through repository.
     */
    @Test
    void findDistributionSetTagsAll() {
        final List<DistributionSetTag> tags = createDsSetsWithTags();

        // test
        assertThat(distributionSetTagManagement.findAll(PAGE).getContent()).as("Wrong size of tags")
                .hasSize(tags.size());
        assertThat(distributionSetTagRepository.findAll()).as("Wrong size of tags").hasSize(20);
    }

    /**
     * Ensures that a created tags are persisted in the repository as defined.
     */
    @Test
    void createDistributionSetTags() {
        final List<DistributionSetTag> tags = createDsSetsWithTags();
        assertThat(distributionSetTagRepository.findAll()).as("Wrong size of tags created").hasSize(tags.size());
    }

    private void verifyExpectedFilteredDistributionSets(final DistributionSetFilter.DistributionSetFilterBuilder distributionSetFilterBuilder,
            final Stream<Collection<DistributionSet>> expectedFilteredDistributionSets) {
        final Collection<Long> retrievedFilteredDsIds = findDsByDistributionSetFilter(distributionSetFilterBuilder.build(), PAGE).stream()
                .map(DistributionSet::getId).toList();
        final Collection<Long> expectedFilteredDsIds = expectedFilteredDistributionSets.flatMap(Collection::stream)
                .map(DistributionSet::getId).toList();
        assertThat(retrievedFilteredDsIds).hasSameElementsAs(expectedFilteredDsIds);
    }

    private List<DistributionSetTag> createDsSetsWithTags() {
        final Collection<DistributionSet> sets = testdataFactory.createDistributionSets(20);
        final Iterable<DistributionSetTag> tags = testdataFactory.createDistributionSetTags(20);

        tags.forEach(tag -> assignTag(sets, tag));

        return distributionSetTagManagement.findAll(PAGE).getContent().stream().map(DistributionSetTag.class::cast).toList();
    }

    private DistributionSetFilter.DistributionSetFilterBuilder getDistributionSetFilterBuilder() {
        return DistributionSetFilter.builder();
    }

    @SafeVarargs
    private <T> Collection<T> concat(final Collection<T>... targets) {
        final List<T> result = new ArrayList<>();
        Arrays.asList(targets).forEach(result::addAll);
        return result;
    }
}
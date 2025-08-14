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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement.Create;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement.Update;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.jpa.DistributionSetFilter;
import org.eclipse.hawkbit.repository.jpa.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.junit.jupiter.api.Test;

/**
 * {@link DistributionSetTagManagement} tests.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: DistributionSet Tag Management
 */
class DistributionSetTagManagementTest extends AbstractRepositoryManagementTest<DistributionSetTag, Create, Update> {

    private static final String TAG_1 = "tag1";
    private static final String TAG_DESCRIPTION_1 = "tag_description_1";

    @Test
    void createAssignAndDeleteTags() {
        final Collection<DistributionSet> dsAs = testdataFactory.createDistributionSets("DS-A", 1);
        final Collection<DistributionSet> dsBs = testdataFactory.createDistributionSets("DS-B", 2);
        final Collection<DistributionSet> dsCs = testdataFactory.createDistributionSets("DS-C", 4);
        final Collection<DistributionSet> dsABs = testdataFactory.createDistributionSets("DS-AB", 8);
        final Collection<DistributionSet> dsACs = testdataFactory.createDistributionSets("DS-AC", 16);
        final Collection<DistributionSet> dsBCs = testdataFactory.createDistributionSets("DS-BC", 32);
        final Collection<DistributionSet> dsABCs = testdataFactory.createDistributionSets("DS-ABC", 64);

        final DistributionSetTag tagA = distributionSetTagManagement.create(Create.builder().name("A").build());
        final DistributionSetTag tagB = distributionSetTagManagement.create(Create.builder().name("B").build());
        final DistributionSetTag tagC = distributionSetTagManagement.create(Create.builder().name("C").build());
        final DistributionSetTag tagX = distributionSetTagManagement.create(Create.builder().name("X").build());
        final DistributionSetTag tagY = distributionSetTagManagement.create(Create.builder().name("Y").build());

        assignTag(dsAs, tagA);
        assignTag(dsBs, tagB);
        assignTag(dsCs, tagC);
        assignTags(dsABs, tagA, tagB);
        assignTags(dsACs, tagA, tagC);
        assignTags(dsBCs, tagB, tagC);
        assignTags(dsABCs, tagA, tagB, tagC);

        // search for not deleted
        final DistributionSetFilterBuilder distributionSetFilterBuilder = DistributionSetFilter.builder().isComplete(true);
        verifyExpectedFilteredDistributionSets(
                distributionSetFilterBuilder.tagNames(List.of(tagA.getName())), Stream.of(dsAs, dsABs, dsACs, dsABCs));
        verifyExpectedFilteredDistributionSets(
                distributionSetFilterBuilder.tagNames(List.of(tagB.getName())), Stream.of(dsBs, dsABs, dsBCs, dsABCs));
        verifyExpectedFilteredDistributionSets(
                distributionSetFilterBuilder.tagNames(List.of(tagC.getName())), Stream.of(dsCs, dsACs, dsBCs, dsABCs));
        verifyExpectedFilteredDistributionSets(distributionSetFilterBuilder.tagNames(List.of(tagX.getName())), Stream.empty());

        assertThat(distributionSetTagRepository.findAll()).hasSize(5);
        distributionSetTagManagement.delete(tagY.getId());
        assertThat(distributionSetTagRepository.findAll()).hasSize(4);
        distributionSetTagManagement.delete(tagX.getId());
        assertThat(distributionSetTagRepository.findAll()).hasSize(3);
        distributionSetTagManagement.delete(tagB.getId());
        assertThat(distributionSetTagRepository.findAll()).hasSize(2);

        verifyExpectedFilteredDistributionSets(
                distributionSetFilterBuilder.tagNames(List.of(tagA.getName())), Stream.of(dsAs, dsABs, dsACs, dsABCs));
        verifyExpectedFilteredDistributionSets(
                distributionSetFilterBuilder.tagNames(List.of(tagB.getName())), Stream.empty());
        verifyExpectedFilteredDistributionSets(
                distributionSetFilterBuilder.tagNames(List.of(tagC.getName())), Stream.of(dsCs, dsACs, dsBCs, dsABCs));
    }

    /**
     * Verifies assign/unassign.
     */
    @Test
    void assignAndUnassign() {
        final DistributionSetTag tag = distributionSetTagManagement.create(Create.builder().name(TAG_1).description(TAG_DESCRIPTION_1).build());

        final Collection<DistributionSet> groupA = testdataFactory.createDistributionSets("A_", 5);
        final Collection<DistributionSet> groupB = testdataFactory.createDistributionSets("B_", 5);
        final Collection<DistributionSet> groupAB = concat(groupA, groupB);

        // set A only -> A is now assigned
        List<? extends DistributionSet> result = assignTag(groupA, tag);
        Assertions.<DistributionSet> assertThat(result)
                .hasSameSizeAs(groupA)
                .containsAll(distributionSetManagement.get(groupA.stream().map(DistributionSet::getId).toList()));
        assertThat(
                distributionSetManagement.findByTag(tag.getId(), UNPAGED).getContent().stream()
                        .map(DistributionSet::getId)
                        .sorted()
                        .toList())
                .isEqualTo(groupA.stream().map(DistributionSet::getId).sorted().toList());

        // set to A+B -> A is still assigned and B is assigned as well
        result = assignTag(groupAB, tag);
        Assertions.<DistributionSet> assertThat(result)
                .hasSameSizeAs(groupAB)
                .containsAll(distributionSetManagement.get(groupAB.stream().map(DistributionSet::getId).toList()));
        assertThat(
                distributionSetManagement.findByTag(tag.getId(), UNPAGED).getContent().stream()
                        .map(DistributionSet::getId)
                        .sorted()
                        .toList())
                .isEqualTo(groupAB.stream().map(DistributionSet::getId).sorted().toList());

        // toggle A+B -> both unassigned
        result = unassignTag(groupAB, tag);
        Assertions.<DistributionSet> assertThat(result)
                .hasSameSizeAs(groupAB)
                .containsAll(distributionSetManagement.get(groupAB.stream().map(DistributionSet::getId).toList()));
        assertThat(distributionSetManagement.findByTag(tag.getId(), UNPAGED).getContent()).isEmpty();
    }

    @Test
    void failToAssignTagIfMissingDs() {
        final Collection<Long> group = testdataFactory.createDistributionSets(5).stream().map(DistributionSet::getId).toList();
        final DistributionSetTag tag = distributionSetTagManagement.create(Create.builder().name(TAG_1).description(TAG_DESCRIPTION_1).build());
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

    @Test
    void failedToCreateIfNameAlreadyExists() {
        final Create tag = Create.builder().name("A").build();
        distributionSetTagManagement.create(tag);
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("should not have worked as tag already exists")
                .isThrownBy(() -> distributionSetTagManagement.create(tag));
    }

    @Test
    void failedToUpdateIfNameAlreadyExists() {
        distributionSetTagManagement.create(Create.builder().name("A").build());
        final DistributionSetTag tag = distributionSetTagManagement.create(Create.builder().name("B").build());
        final Update tagUpdate = Update.builder().id(tag.getId()).name("A").build();
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("Constraint not applied - tag with same name already exists")
                .isThrownBy(() -> distributionSetTagManagement.update(tagUpdate));
    }

    private void verifyExpectedFilteredDistributionSets(
            final DistributionSetFilterBuilder distributionSetFilterBuilder,
            final Stream<Collection<DistributionSet>> expectedFilteredDistributionSets) {
        final Collection<Long> retrievedFilteredDsIds = findDsByDistributionSetFilter(distributionSetFilterBuilder.build(), PAGE).stream()
                .map(DistributionSet::getId).toList();
        final Collection<Long> expectedFilteredDsIds = expectedFilteredDistributionSets.flatMap(Collection::stream)
                .map(DistributionSet::getId).toList();
        assertThat(retrievedFilteredDsIds).hasSameElementsAs(expectedFilteredDsIds);
    }
}
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
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * Test class for {@link TargetTagManagement}.
 */
@Feature("Component Tests - Repository")
@Story("Target Tag Management")
class TargetTagManagementTest extends AbstractJpaIntegrationTest {

    private static final Random RND = new Random();

    @Test
    @Description("Verifies that tagging of set containing missing DS throws meaningful and correct exception.")
    void failOnMissingDs() {
        final Collection<String> group = testdataFactory.createTargets(5).stream()
                .map(Target::getControllerId)
                .collect(Collectors.toList());
        final TargetTag tag = targetTagManagement.create(entityFactory.tag().create().name("tag1").description("tagdesc1"));

        final List<String> missing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            while (true) {
                final String id = String.valueOf(Math.abs(RND.nextLong()));
                if (!group.contains(id)) {
                    missing.add(id);
                    break;
                }
            }
        }
        Collections.sort(missing);
        final Collection<String> withMissing = concat(group, missing);
        assertThatThrownBy(() -> targetManagement.assignTag(withMissing, tag.getId()))
                .matches(e -> {
                    if (e instanceof EntityNotFoundException enfe) {
                        if (enfe.getInfo().get(EntityNotFoundException.TYPE).equals(Target.class.getSimpleName())) {
                            if (enfe.getInfo().get(EntityNotFoundException.ENTITY_ID) instanceof Collection entityId) {
                                return entityId.stream().sorted().toList().equals(missing);
                            }
                        }
                    }
                    return false;
                });
    }

    @Test
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means " +
            "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(targetTagManagement.getByName(NOT_EXIST_ID)).isNotPresent();
        assertThat(targetTagManagement.get(NOT_EXIST_IDL)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities " +
            " by means of throwing EntityNotFoundException.")
    @ExpectEvents({
            @Expect(type = DistributionSetTagUpdatedEvent.class), 
            @Expect(type = TargetTagUpdatedEvent.class) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        verifyThrownExceptionBy(() -> targetTagManagement.delete(NOT_EXIST_ID), "TargetTag");
        verifyThrownExceptionBy(() -> targetTagManagement.update(entityFactory.tag().update(NOT_EXIST_IDL)), "TargetTag");
        verifyThrownExceptionBy(() -> getTargetTags(NOT_EXIST_ID), "Target");
    }

    @Test
    @Description("Verify that a tag with with invalid properties cannot be created or updated")
    void createAndUpdateTagWithInvalidFields() {
        final TargetTag tag = targetTagManagement.create(entityFactory.tag().create().name("tag1").description("tagdesc1"));
        createAndUpdateTagWithInvalidDescription(tag);
        createAndUpdateTagWithInvalidColour(tag);
        createAndUpdateTagWithInvalidName(tag);
    }

    @Test
    @Description("Verifies assign/unassign.")
    void assignAndUnassignTargetTags() {
        final List<Target> groupA = testdataFactory.createTargets(20);
        final List<Target> groupB = testdataFactory.createTargets(20, "groupb", "groupb");

        final TargetTag tag = targetTagManagement.create(entityFactory.tag().create().name("tag1").description("tagdesc1"));

        // toggle A only -> A is now assigned
        List<Target> result = assignTag(groupA, tag);
        assertThat(result).size().isEqualTo(20);
        assertThat(result).containsAll(
                targetManagement.getByControllerID(groupA.stream().map(Target::getControllerId).collect(Collectors.toList())));
        assertThat(targetManagement.findByTag(Pageable.unpaged(), tag.getId()).getContent().stream().map(Target::getControllerId).sorted()
                .toList())
                .isEqualTo(groupA.stream().map(Target::getControllerId).sorted().toList());

        // toggle A+B -> A is still assigned and B is assigned as well
        final Collection<Target> groupAB = concat(groupA, groupB);
        result = assignTag(groupAB, tag);
        assertThat(result).size().isEqualTo(40);
        assertThat(result).containsAll(
                targetManagement.getByControllerID(groupAB.stream().map(Target::getControllerId).collect(Collectors.toList())));
        assertThat(targetManagement.findByTag(Pageable.unpaged(), tag.getId()).getContent().stream().map(Target::getControllerId).sorted()
                .toList())
                .isEqualTo(groupAB.stream().map(Target::getControllerId).sorted().toList());

        // toggle A+B -> both unassigned
        result = unassignTag(groupAB, tag);
        assertThat(result).size().isEqualTo(40);
        assertThat(result).containsAll(
                targetManagement.getByControllerID(groupAB.stream().map(Target::getControllerId).collect(Collectors.toList())));
        assertThat(targetManagement.findByTag(Pageable.unpaged(), tag.getId()).getContent()).isEmpty();
    }

    @Test
    @Description("Ensures that all tags are retrieved through repository.")
    void findAllTargetTags() {
        final List<JpaTargetTag> tags = createTargetsWithTags();

        assertThat(targetTagRepository.findAll()).isEqualTo(targetTagRepository.findAll()).isEqualTo(tags)
                .as("Wrong tag size").hasSize(20);
    }

    @Test
    @Description("Ensures that a created tag is persisted in the repository as defined.")
    void createTargetTag() {
        final Tag tag = targetTagManagement
                .create(entityFactory.tag().create().name("kai1").description("kai2").colour("colour"));

        assertThat(targetTagRepository.findByNameEquals("kai1").get().getDescription()).as("wrong tag ed")
                .isEqualTo("kai2");
        assertThat(targetTagManagement.getByName("kai1").get().getColour()).as("wrong tag found").isEqualTo("colour");
        assertThat(targetTagManagement.get(tag.getId()).get().getColour()).as("wrong tag found").isEqualTo("colour");
    }

    @Test
    @Description("Ensures that a deleted tag is removed from the repository as defined.")
    void deleteTargetTags() {
        // create test data
        final Iterable<JpaTargetTag> tags = createTargetsWithTags();
        final TargetTag toDelete = tags.iterator().next();

        for (final Target target : targetRepository.findAll()) {
            assertThat(getTargetTags(target.getControllerId())).contains(toDelete);
        }

        // delete
        targetTagManagement.delete(toDelete.getName());

        // check
        for (final Target target : targetRepository.findAll()) {
            assertThat(getTargetTags(target.getControllerId()))
                    .doesNotContain(toDelete);
        }
        assertThat(targetTagRepository.findById(toDelete.getId())).as("No tag should be found").isNotPresent();
        assertThat(targetTagRepository.findAll()).as("Wrong target tag size").hasSize(19);
    }

    @Test
    @Description("Tests the name update of a target tag.")
    void updateTargetTag() {
        final List<JpaTargetTag> tags = createTargetsWithTags();

        // change data
        final TargetTag savedAssigned = tags.iterator().next();

        // persist
        targetTagManagement.update(entityFactory.tag().update(savedAssigned.getId()).name("test123"));

        // check data
        assertThat(targetTagRepository.findAll()).as("Wrong target tag size").hasSize(tags.size());
        assertThat(targetTagRepository.findById(savedAssigned.getId()).get().getName()).as("wrong target tag is saved")
                .isEqualTo("test123");
        assertThat(targetTagRepository.findById(savedAssigned.getId()).get().getOptLockRevision())
                .as("wrong target tag is saved")
                .isEqualTo(2);
    }

    @Test
    @Description("Ensures that a tag cannot be created if one exists already with that name (expects EntityAlreadyExistsException).")
    void failedDuplicateTargetTagNameException() {
        targetTagManagement.create(entityFactory.tag().create().name("A"));
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> targetTagManagement.create(entityFactory.tag().create().name("A")));
    }

    @Test
    @Description("Ensures that a tag cannot be updated to a name that already exists on another tag (expects EntityAlreadyExistsException).")
    void failedDuplicateTargetTagNameExceptionAfterUpdate() {
        targetTagManagement.create(entityFactory.tag().create().name("A"));
        final TargetTag tag = targetTagManagement.create(entityFactory.tag().create().name("B"));

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> targetTagManagement.update(entityFactory.tag().update(tag.getId()).name("A")));
    }

    @Step
    private void createAndUpdateTagWithInvalidDescription(final Tag tag) {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long description should not be created")
                .isThrownBy(() -> targetTagManagement.create(
                        entityFactory.tag().create().name("a").description(RandomStringUtils.randomAlphanumeric(513))));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid description should not be created").isThrownBy(() -> targetTagManagement
                        .create(entityFactory.tag().create().name("a").description(INVALID_TEXT_HTML)));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long description should not be updated")
                .isThrownBy(() -> targetTagManagement.update(
                        entityFactory.tag().update(tag.getId())
                                .description(RandomStringUtils.randomAlphanumeric(513))));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid description should not be updated")
                .isThrownBy(() -> targetTagManagement
                        .update(entityFactory.tag().update(tag.getId()).description(INVALID_TEXT_HTML)));
    }

    @Step
    private void createAndUpdateTagWithInvalidColour(final Tag tag) {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long colour should not be created")
                .isThrownBy(() -> targetTagManagement.create(
                        entityFactory.tag().create().name("a").colour(RandomStringUtils.randomAlphanumeric(17))));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid colour should not be created").isThrownBy(() -> targetTagManagement
                        .create(entityFactory.tag().create().name("a").colour(INVALID_TEXT_HTML)));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long colour should not be updated")
                .isThrownBy(() -> targetTagManagement.update(
                        entityFactory.tag().update(tag.getId()).colour(RandomStringUtils.randomAlphanumeric(17))));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid colour should not be updated").isThrownBy(() -> targetTagManagement
                        .update(entityFactory.tag().update(tag.getId()).colour(INVALID_TEXT_HTML)));
    }

    @Step
    private void createAndUpdateTagWithInvalidName(final Tag tag) {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long name should not be created")
                .isThrownBy(() -> targetTagManagement
                        .create(entityFactory.tag().create().name(RandomStringUtils.randomAlphanumeric(
                                NamedEntity.NAME_MAX_SIZE + 1))));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalidname should not be created")
                .isThrownBy(() -> targetTagManagement.create(entityFactory.tag().create().name(INVALID_TEXT_HTML)));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long name should not be updated")
                .isThrownBy(() -> targetTagManagement
                        .update(entityFactory.tag().update(tag.getId()).name(RandomStringUtils.randomAlphanumeric(
                                NamedEntity.NAME_MAX_SIZE + 1))));
        assertThatExceptionOfType(ConstraintViolationException.class).as("tag with invalid name should not be updated")
                .isThrownBy(() -> targetTagManagement
                        .update(entityFactory.tag().update(tag.getId()).name(INVALID_TEXT_HTML)));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too short name should not be updated")
                .isThrownBy(() -> targetTagManagement.update(entityFactory.tag().update(tag.getId()).name("")));
    }

    @SafeVarargs
    private <T> Collection<T> concat(final Collection<T>... targets) {
        final List<T> result = new ArrayList<>();
        Arrays.asList(targets).forEach(result::addAll);
        return result;
    }

    private List<JpaTargetTag> createTargetsWithTags() {
        final List<Target> targets = testdataFactory.createTargets(20);
        final Iterable<TargetTag> tags = testdataFactory.createTargetTags(20, "");

        tags.forEach(tag -> assignTag(targets, tag));

        return targetTagRepository.findAll();
    }
}

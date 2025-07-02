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

import jakarta.validation.ConstraintViolationException;

import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
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
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Target Tag Management
 */
class TargetTagManagementTest extends AbstractJpaIntegrationTest {

    private static final Random RND = new Random();

    /**
     * Verifies that tagging of set containing missing DS throws meaningful and correct exception.
     */
    @Test
    void failOnMissingDs() {
        final Collection<String> group = testdataFactory.createTargets(5).stream()
                .map(Target::getControllerId)
                .toList();
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
                    if (e instanceof EntityNotFoundException enfe
                            && enfe.getInfo().get(EntityNotFoundException.TYPE).equals(Target.class.getSimpleName())
                            && enfe.getInfo().get(EntityNotFoundException.ENTITY_ID) instanceof Collection entityId) {
                        return entityId.stream().sorted().toList().equals(missing);
                    }
                    return false;
                });
    }

    /**
     * Verifies that management get access reacts as specfied on calls for non existing entities by means 
     * of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(targetTagManagement.getByName(NOT_EXIST_ID)).isNotPresent();
        assertThat(targetTagManagement.get(NOT_EXIST_IDL)).isNotPresent();
    }

    /**
     * Verifies that management queries react as specfied on calls for non existing entities 
     *  by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagUpdatedEvent.class),
            @Expect(type = TargetTagUpdatedEvent.class) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        verifyThrownExceptionBy(() -> targetTagManagement.delete(NOT_EXIST_ID), "TargetTag");
        verifyThrownExceptionBy(() -> targetTagManagement.update(entityFactory.tag().update(NOT_EXIST_IDL)), "TargetTag");
        verifyThrownExceptionBy(() -> getTargetTags(NOT_EXIST_ID), "Target");
    }

    /**
     * Verify that a tag with with invalid properties cannot be created or updated
     */
    @Test
    void createAndUpdateTagWithInvalidFields() {
        final TargetTag tag = targetTagManagement.create(entityFactory.tag().create().name("tag1").description("tagdesc1"));
        createAndUpdateTagWithInvalidDescription(tag);
        createAndUpdateTagWithInvalidColour(tag);
        createAndUpdateTagWithInvalidName(tag);
    }

    /**
     * Verifies assign/unassign.
     */
    @Test
    void assignAndUnassignTargetTags() {
        final List<Target> groupA = testdataFactory.createTargets(20);
        final List<Target> groupB = testdataFactory.createTargets(20, "groupb", "groupb");

        final TargetTag tag = targetTagManagement.create(entityFactory.tag().create().name("tag1").description("tagdesc1"));

        // toggle A only -> A is now assigned
        List<Target> result = assignTag(groupA, tag);
        assertThat(result)
                .containsAll(
                        targetManagement.getByControllerID(groupA.stream().map(Target::getControllerId).toList()))
                .size().isEqualTo(20);
        assertThat(targetManagement.findByTag(tag.getId(), Pageable.unpaged()).getContent().stream().map(Target::getControllerId).sorted()
                .toList())
                .isEqualTo(groupA.stream().map(Target::getControllerId).sorted().toList());

        // toggle A+B -> A is still assigned and B is assigned as well
        final Collection<Target> groupAB = concat(groupA, groupB);
        result = assignTag(groupAB, tag);
        assertThat(result)
                .containsAll(
                        targetManagement.getByControllerID(groupAB.stream().map(Target::getControllerId).toList()))
                .size().isEqualTo(40);
        assertThat(targetManagement.findByTag(tag.getId(), Pageable.unpaged()).getContent().stream().map(Target::getControllerId).sorted()
                .toList())
                .isEqualTo(groupAB.stream().map(Target::getControllerId).sorted().toList());

        // toggle A+B -> both unassigned
        result = unassignTag(groupAB, tag);
        assertThat(result)
                .containsAll(targetManagement.getByControllerID(groupAB.stream().map(Target::getControllerId).toList()))
                .size().isEqualTo(40);
        assertThat(targetManagement.findByTag(tag.getId(), Pageable.unpaged()).getContent()).isEmpty();
    }

    /**
     * Ensures that all tags are retrieved through repository.
     */
    @Test
    void findAllTargetTags() {
        final List<JpaTargetTag> tags = createTargetsWithTags();

        assertThat(targetTagRepository.findAll()).isEqualTo(targetTagRepository.findAll()).isEqualTo(tags)
                .as("Wrong tag size").hasSize(20);
    }

    /**
     * Ensures that a created tag is persisted in the repository as defined.
     */
    @Test
    void createTargetTag() {
        final Tag tag = targetTagManagement
                .create(entityFactory.tag().create().name("kai1").description("kai2").colour("colour"));

        assertThat(targetTagRepository.findByNameEquals("kai1").get().getDescription()).as("wrong tag ed")
                .isEqualTo("kai2");
        assertThat(targetTagManagement.getByName("kai1").get().getColour()).as("wrong tag found").isEqualTo("colour");
        assertThat(targetTagManagement.get(tag.getId()).get().getColour()).as("wrong tag found").isEqualTo("colour");
    }

    /**
     * Ensures that a deleted tag is removed from the repository as defined.
     */
    @Test
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

    /**
     * Tests the name update of a target tag.
     */
    @Test
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

    /**
     * Ensures that a tag cannot be created if one exists already with that name (expects EntityAlreadyExistsException).
     */
    @Test
    void failedDuplicateTargetTagNameException() {
        final TagCreate tagCreate = entityFactory.tag().create().name("A");
        targetTagManagement.create(tagCreate);
        assertThatExceptionOfType(EntityAlreadyExistsException.class).isThrownBy(() -> targetTagManagement.create(tagCreate));
    }

    /**
     * Ensures that a tag cannot be updated to a name that already exists on another tag (expects EntityAlreadyExistsException).
     */
    @Test
    void failedDuplicateTargetTagNameExceptionAfterUpdate() {
        targetTagManagement.create(entityFactory.tag().create().name("A"));
        final TargetTag tag = targetTagManagement.create(entityFactory.tag().create().name("B"));

        final TagUpdate tagUpdate = entityFactory.tag().update(tag.getId()).name("A");
        assertThatExceptionOfType(EntityAlreadyExistsException.class).isThrownBy(() -> targetTagManagement.update(tagUpdate));
    }

    private void createAndUpdateTagWithInvalidDescription(final Tag tag) {
        final TagCreate tagCraeteTooLong = entityFactory.tag().create().name("a").description(randomString(513));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long description should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCraeteTooLong));
        final TagCreate tagCreateInvalidHtml = entityFactory.tag().create().name("a").description(INVALID_TEXT_HTML);
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid description should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCreateInvalidHtml));
        final TagUpdate tagUpdateTooLong = entityFactory.tag().update(tag.getId()).description(randomString(513));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long description should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateTooLong));
        final TagUpdate tagUpdateInvalidHtml = entityFactory.tag().update(tag.getId()).description(INVALID_TEXT_HTML);
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid description should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateInvalidHtml));
    }

    private void createAndUpdateTagWithInvalidColour(final Tag tag) {
        final TagCreate tagCreateTooLong = entityFactory.tag().create().name("a").colour(randomString(17));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long colour should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCreateTooLong));
        final TagCreate tagCraeteInvalidHtml = entityFactory.tag().create().name("a").colour(INVALID_TEXT_HTML);
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid colour should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCraeteInvalidHtml));
        final TagUpdate tagUpdateTooLong = entityFactory.tag().update(tag.getId()).colour(randomString(17));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long colour should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateTooLong));
        final TagUpdate tagUpdateInvalidHtml = entityFactory.tag().update(tag.getId()).colour(INVALID_TEXT_HTML);
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid colour should not be updated")
                .isThrownBy(() -> targetTagManagement
                        .update(tagUpdateInvalidHtml));
    }

    private void createAndUpdateTagWithInvalidName(final Tag tag) {
        final TagCreate tagCreateTooLong = entityFactory.tag().create().name(randomString(NamedEntity.NAME_MAX_SIZE + 1));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long name should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCreateTooLong));
        final TagCreate tagCreateInvalidHtml = entityFactory.tag().create().name(INVALID_TEXT_HTML);
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid name should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCreateInvalidHtml));
        final TagUpdate tagUpdateTooLong = entityFactory.tag().update(tag.getId()).name(randomString(NamedEntity.NAME_MAX_SIZE + 1));
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long name should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateTooLong));
        final TagUpdate tagUpdateInvalidHtml = entityFactory.tag().update(tag.getId()).name(INVALID_TEXT_HTML);
        assertThatExceptionOfType(ConstraintViolationException.class).as("tag with invalid name should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateInvalidHtml));
        final TagUpdate tagUpdateEmpty = entityFactory.tag().update(tag.getId()).name("");
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too short name should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateEmpty));
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

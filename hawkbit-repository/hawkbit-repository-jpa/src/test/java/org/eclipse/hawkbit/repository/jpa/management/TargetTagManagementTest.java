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

import jakarta.validation.ConstraintViolationException;

import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement.Create;
import org.eclipse.hawkbit.repository.TargetTagManagement.Update;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
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
class TargetTagManagementTest extends AbstractRepositoryManagementTest<TargetTag, Create, Update> {

    @Test
    void assignAndUnassignTargetTags() {
        final List<Target> groupA = testdataFactory.createTargets(20);
        final List<Target> groupB = testdataFactory.createTargets(20, "groupb", "groupb");

        final TargetTag tag = targetTagManagement.create(Create.builder().name("tag1").description("tagdesc1").build());

        // toggle A only -> A is now assigned
        List<Target> result = assignTag(groupA, tag);
        assertThat(result)
                .containsAll(targetManagement.findByControllerId(groupA.stream().map(Target::getControllerId).toList()))
                .size().isEqualTo(20);
        assertThat(targetManagement.findByTag(tag.getId(), Pageable.unpaged()).getContent().stream().map(Target::getControllerId).sorted()
                .toList())
                .isEqualTo(groupA.stream().map(Target::getControllerId).sorted().toList());

        // toggle A+B -> A is still assigned and B is assigned as well
        final Collection<Target> groupAB = concat(groupA, groupB);
        result = assignTag(groupAB, tag);
        assertThat(result)
                .containsAll(targetManagement.findByControllerId(groupAB.stream().map(Target::getControllerId).toList()))
                .size().isEqualTo(40);
        assertThat(targetManagement.findByTag(tag.getId(), Pageable.unpaged()).getContent().stream().map(Target::getControllerId).sorted()
                .toList())
                .isEqualTo(groupAB.stream().map(Target::getControllerId).sorted().toList());

        // toggle A+B -> both unassigned
        result = unassignTag(groupAB, tag);
        assertThat(result)
                .containsAll(targetManagement.findByControllerId(groupAB.stream().map(Target::getControllerId).toList()))
                .size().isEqualTo(40);
        assertThat(targetManagement.findByTag(tag.getId(), Pageable.unpaged()).getContent()).isEmpty();
    }

    @Test
    void failToAssignOnMissingTargetTag() {
        final Collection<String> group = testdataFactory.createTargets(5).stream()
                .map(Target::getControllerId)
                .toList();
        final TargetTag tag = targetTagManagement.create(Create.builder().name("tag1").description("tagdesc1").build());

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
                            && enfe.getInfo().get(EntityNotFoundException.ENTITY_ID) instanceof Collection<?> entityId) {
                        return entityId.stream().sorted().toList().equals(missing);
                    }
                    return false;
                });
    }

    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetTagUpdatedEvent.class),
            @Expect(type = TargetTagUpdatedEvent.class) })
    void failIfReferNotExistingEntity() {
        verifyThrownExceptionBy(() -> targetTagManagement.delete(NOT_EXIST_IDL), "TargetTag");
        verifyThrownExceptionBy(() -> targetTagManagement.update(Update.builder().id(NOT_EXIST_IDL).build()), "TargetTag");
        verifyThrownExceptionBy(() -> getTargetTags(NOT_EXIST_ID), "Target");
    }

    @Test
    void failToCreateAndUpdateTagWithInvalidFields() {
        final TargetTag tag = targetTagManagement.create(Create.builder().name("tag1").description("tagdesc1").build());
        createAndUpdateTagWithInvalidDescription(tag);
        createAndUpdateTagWithInvalidColour(tag);
        createAndUpdateTagWithInvalidName(tag);
    }

    /**
     * Ensures that a tag cannot be updated to a name that already exists on another tag (expects EntityAlreadyExistsException).
     */
    @Test
    void failToDuplicateTargetTagNameOnUpdate() {
        targetTagManagement.create(Create.builder().name("A").build());
        final TargetTag tag = targetTagManagement.create(Create.builder().name("B").build());

        final Update tagUpdate = Update.builder().id(tag.getId()).name("A").build();
        assertThatExceptionOfType(EntityAlreadyExistsException.class).isThrownBy(() -> targetTagManagement.update(tagUpdate));
    }

    private void createAndUpdateTagWithInvalidDescription(final Tag tag) {
        final Create tagCreateTooLong = Create.builder().name("a").description(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long description should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCreateTooLong));
        final Create tagCreateInvalidHtml = Create.builder().name("a").description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid description should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCreateInvalidHtml));
        final Update tagUpdateTooLong = Update.builder().id(tag.getId()).description(randomString(513)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long description should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateTooLong));
        final Update tagUpdateInvalidHtml = Update.builder().id(tag.getId()).description(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid description should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateInvalidHtml));
    }

    private void createAndUpdateTagWithInvalidColour(final Tag tag) {
        final Create tagCreateTooLong = Create.builder().name("a").colour(randomString(17)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long colour should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCreateTooLong));
        final Create tagCreateInvalidHtml = Create.builder().name("a").colour(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid colour should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCreateInvalidHtml));
        final Update tagUpdateTooLong = Update.builder().id(tag.getId()).colour(randomString(17)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long colour should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateTooLong));
        final Update tagUpdateInvalidHtml = Update.builder().id(tag.getId()).colour(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid colour should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateInvalidHtml));
    }

    private void createAndUpdateTagWithInvalidName(final Tag tag) {
        final Create tagCreateTooLong = Create.builder().name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long name should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCreateTooLong));
        final Create tagCreateInvalidHtml = Create.builder().name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with invalid name should not be created")
                .isThrownBy(() -> targetTagManagement.create(tagCreateInvalidHtml));
        final Update tagUpdateTooLong = Update.builder().id(tag.getId()).name(randomString(NamedEntity.NAME_MAX_SIZE + 1)).build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too long name should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateTooLong));
        final Update tagUpdateInvalidHtml = Update.builder().id(tag.getId()).name(INVALID_TEXT_HTML).build();
        assertThatExceptionOfType(ConstraintViolationException.class).as("tag with invalid name should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateInvalidHtml));
        final Update tagUpdateEmpty = Update.builder().id(tag.getId()).name("").build();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("tag with too short name should not be updated")
                .isThrownBy(() -> targetTagManagement.update(tagUpdateEmpty));
    }
}
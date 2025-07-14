/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.hawkbit.im.authentication.SpPermission.DELETE_TARGET_TYPE;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_TARGET_TYPE;
import static org.eclipse.hawkbit.im.authentication.SpPermission.UPDATE_TARGET_TYPE;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;

import java.util.Arrays;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.builder.TargetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

/**
 * Feature: Component Tests - Access Control<br/>
 * Story: Test Target Type Access Controller
 */
@ContextConfiguration(classes = { DefaultAccessControllerConfiguration.class })
class TargetTypeAccessControllerTest extends AbstractJpaIntegrationTest {

    /**
     * Verifies read access rules for target types
     */
    @Test
    void verifyTargetTypeReadOperations() {
        final TargetType permittedTargetType = targetTypeManagement.create(entityFactory.targetType().create().name("type1"));
        final TargetType hiddenTargetType = targetTypeManagement.create(entityFactory.targetType().create().name("type2"));

        runAs(withUser("user", READ_TARGET_TYPE + "/id==" + permittedTargetType.getId()), () -> {
            // verify targetTypeManagement#findAll
            assertThat(targetTypeManagement.findAll(Pageable.unpaged()).get().map(Identifiable::getId).toList())
                    .containsOnly(permittedTargetType.getId());

            // verify targetTypeManagement#findByRsql
            assertThat(targetTypeManagement.findByRsql("name==*", Pageable.unpaged()).get().map(Identifiable::getId).toList())
                    .containsOnly(permittedTargetType.getId());

            // verify targetTypeManagement#findByName
            assertThat(targetTypeManagement.findByName(permittedTargetType.getName(), Pageable.unpaged()).getContent())
                    .hasSize(1).satisfies(results ->
                            assertThat(results.get(0).getId()).isEqualTo(permittedTargetType.getId()));
            assertThat(targetTypeManagement.findByName(hiddenTargetType.getName(), Pageable.unpaged())).isEmpty();

            // verify targetTypeManagement#count
            assertThat(targetTypeManagement.count()).isEqualTo(1);

            // verify targetTypeManagement#countByName
            assertThat(targetTypeManagement.countByName(permittedTargetType.getName())).isEqualTo(1);
            assertThat(targetTypeManagement.countByName(hiddenTargetType.getName())).isZero();

            // verify targetTypeManagement#countByName
            assertThat(targetTypeManagement.countByName(permittedTargetType.getName())).isEqualTo(1);
            assertThat(targetTypeManagement.countByName(hiddenTargetType.getName())).isZero();

            // verify targetTypeManagement#get by id
            assertThat(targetTypeManagement.get(permittedTargetType.getId())).isPresent();
            final Long hiddenTargetTypeId = hiddenTargetType.getId();
            assertThat(targetTypeManagement.get(hiddenTargetTypeId)).isEmpty();

            // verify targetTypeManagement#getByName
            assertThat(targetTypeManagement.getByName(permittedTargetType.getName())).isPresent();
            assertThat(targetTypeManagement.getByName(hiddenTargetType.getName())).isEmpty();

            // verify targetTypeManagement#get by ids
            assertThat(targetTypeManagement.get(Arrays.asList(permittedTargetType.getId(), hiddenTargetTypeId))
                    .stream().map(Identifiable::getId).toList()).containsOnly(permittedTargetType.getId());

            // verify targetTypeManagement#update is not possible. Assert exception thrown.
            final TargetTypeUpdate targetTypeUpdate = entityFactory.targetType().update(hiddenTargetTypeId)
                    .name(hiddenTargetType.getName() + "/new").description("newDesc");
            assertThatThrownBy(() -> targetTypeManagement.update(targetTypeUpdate))
                    .as("Target type update shouldn't be allowed since the target type is not visible.")
                    .isInstanceOf(InsufficientPermissionException.class);

            // verify targetTypeManagement#delete is not possible. Assert exception thrown.
            assertThatThrownBy(() -> targetTypeManagement.delete(hiddenTargetTypeId))
                    .as("Target type delete shouldn't be allowed since the target type is not visible.")
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }

    /**
     * Verifies delete access rules for target types
     */
    @Test
    void verifyTargetTypeDeleteOperations() {
        final TargetType manageableTargetType = targetTypeManagement.create(entityFactory.targetType().create().name("type1"));
        final TargetType readOnlyTargetType = targetTypeManagement.create(entityFactory.targetType().create().name("type2"));

        runAs(withUser("user",
                        READ_TARGET_TYPE + "/id==" + manageableTargetType.getId() + " or id==" + readOnlyTargetType.getId(),
                        DELETE_TARGET_TYPE + "/id==" + manageableTargetType.getId()), () -> {
            // delete the manageableTargetType
            targetTypeManagement.delete(manageableTargetType.getId());

            // verify targetTypeManagement#delete for readOnlyTargetType is not possible
            final Long readOnlyTargetTypeId = readOnlyTargetType.getId();
            assertThatThrownBy(() -> targetTypeManagement.delete(readOnlyTargetTypeId))
                    .isInstanceOfAny(InsufficientPermissionException.class, EntityNotFoundException.class);
        });
    }

    /**
     * Verifies update operation for target types
     */
    @Test
    void verifyTargetTypeUpdateOperations() {
        final TargetType manageableTargetType = targetTypeManagement.create(entityFactory.targetType().create().name("type1"));
        final TargetType readOnlyTargetType = targetTypeManagement.create(entityFactory.targetType().create().name("type2"));

        runAs(withUser("user",
                        READ_TARGET_TYPE + "/id==" + manageableTargetType.getId() + " or id==" + readOnlyTargetType.getId(),
                        UPDATE_TARGET_TYPE + "/id==" + manageableTargetType.getId()), () -> {
            // update the manageableTargetType
            targetTypeManagement.update(entityFactory.targetType().update(manageableTargetType.getId())
                    .name(manageableTargetType.getName() + "/new").description("newDesc"));

            // verify targetTypeManagement#update for readOnlyTargetType is not possible
            final TargetTypeUpdate targetTypeUpdate = entityFactory.targetType().update(readOnlyTargetType.getId())
                    .name(readOnlyTargetType.getName() + "/new").description("newDesc");
            assertThatThrownBy(() -> targetTypeManagement.update(targetTypeUpdate))
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }

    /**
     * Verifies create operation blocked by controller
     */
    @Test
    void verifyTargetTypeCreationBlockedByAccessController() {
        runAs(withUser("user", READ_TARGET_TYPE, UPDATE_TARGET_TYPE), () -> {
            // verify targetTypeManagement#create for any type
            final TargetTypeCreate targetTypeCreate = entityFactory.targetType().create().name("type1");
            assertThatThrownBy(() -> targetTypeManagement.create(targetTypeCreate))
                    .as("Target type create shouldn't be allowed since the target type is not visible.")
                    .isInstanceOf(InsufficientPermissionException.class);
        });
    }
}
/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.jpa.acm.controller.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetTypeSpecification;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Access Control")
@Story("Test Target Type Access Controlling")
class TargetTypeAccessControllingTest extends AbstractAccessControllingTest {

    @Test
    @Description("Verifies read access rules for target types")
    void verifyTargetTypeReadOperations() {
        final TargetType permittedTargetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("type1"));

        final TargetType unseeableTargetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("type2"));

        // create target and assign with unseeable target type
        final Target targetWithUnseeableTargetType = targetManagement.create(entityFactory.target().create()
                .controllerId("targetWithUnseeableTargetType").targetType(unseeableTargetType.getId()));

        // create target and assign with permitted target type
        final Target targetWithPermittedTargetType = targetManagement.create(entityFactory.target().create()
                .controllerId("targetWithPermittedTargetType").targetType(permittedTargetType.getId()));

        // define access controlling rule
        testAccessControlManger.defineAccessRule(JpaTargetType.class, AccessController.Operation.READ,
                TargetTypeSpecification.hasId(permittedTargetType.getId()));

        // verify targetTypeManagement#findAll
        assertThat(targetTypeManagement.findAll(Pageable.unpaged()).get().map(Identifiable::getId).toList())
                .containsOnly(permittedTargetType.getId());

        // verify targetTypeManagement#findByRsql
        assertThat(targetTypeManagement.findByRsql(Pageable.unpaged(), "id==*").get().map(Identifiable::getId).toList())
                .containsOnly(permittedTargetType.getId());

        // verify targetTypeManagement#findByTargetControllerId
        assertThat(targetTypeManagement.findByTargetControllerId(targetWithPermittedTargetType.getControllerId()))
                .hasValueSatisfying(foundType -> assertThat(foundType.getId()).isEqualTo(permittedTargetType.getId()));
        assertThat(targetTypeManagement.findByTargetControllerId(targetWithUnseeableTargetType.getControllerId()))
                .isEmpty();

        // verify targetTypeManagement#findByTargetControllerIds
        assertThat(
                targetTypeManagement
                        .findByTargetControllerIds(Arrays.asList(targetWithPermittedTargetType.getControllerId(),
                                targetWithUnseeableTargetType.getControllerId()))
                        .stream().map(Identifiable::getId).toList())
                .hasSize(1).containsOnly(permittedTargetType.getId());

        // verify targetTypeManagement#findByTargetId
        assertThat(targetTypeManagement.findByTargetId(targetWithPermittedTargetType.getId()))
                .hasValueSatisfying(foundType -> assertThat(foundType.getId()).isEqualTo(permittedTargetType.getId()));
        assertThat(targetTypeManagement.findByTargetId(targetWithUnseeableTargetType.getId())).isEmpty();

        // verify targetTypeManagement#findByTargetIds
        assertThat(targetTypeManagement
                .findByTargetIds(
                        Arrays.asList(targetWithPermittedTargetType.getId(), targetWithUnseeableTargetType.getId()))
                .stream().map(Identifiable::getId).toList()).hasSize(1).containsOnly(permittedTargetType.getId());

        // verify targetTypeManagement#findByName
        assertThat(targetTypeManagement.findByName(Pageable.unpaged(), permittedTargetType.getName()).getContent())
                .hasSize(1).satisfies(results -> {
                    assertThat(results.get(0).getId()).isEqualTo(permittedTargetType.getId());
                });
        assertThat(targetTypeManagement.findByName(Pageable.unpaged(), unseeableTargetType.getName())).isEmpty();

        // verify targetTypeManagement#count
        assertThat(targetTypeManagement.count()).isEqualTo(1);

        // verify targetTypeManagement#countByName
        assertThat(targetTypeManagement.countByName(permittedTargetType.getName())).isEqualTo(1);
        assertThat(targetTypeManagement.countByName(unseeableTargetType.getName())).isZero();

        // verify targetTypeManagement#countByName
        assertThat(targetTypeManagement.countByName(permittedTargetType.getName())).isEqualTo(1);
        assertThat(targetTypeManagement.countByName(unseeableTargetType.getName())).isZero();

        // verify targetTypeManagement#get by id
        assertThat(targetTypeManagement.get(permittedTargetType.getId())).isPresent();
        assertThat(targetTypeManagement.get(unseeableTargetType.getId())).isEmpty();

        // verify targetTypeManagement#getByName
        assertThat(targetTypeManagement.getByName(permittedTargetType.getName())).isPresent();
        assertThat(targetTypeManagement.getByName(unseeableTargetType.getName())).isEmpty();

        // verify targetTypeManagement#get by ids
        assertThat(targetTypeManagement.get(Arrays.asList(permittedTargetType.getId(), unseeableTargetType.getId()))
                .stream().map(Identifiable::getId).toList()).containsOnly(permittedTargetType.getId());
    }

}

/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Slf4j
@Feature("SecurityTests - TargetTypeManagement")
@Story("SecurityTests TargetTypeManagement")
public class TargetTypeManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getByKeyWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.getByKey("key"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getByKeyWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.getByKey("key"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getByNameWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.getByName("name"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.getByName("name"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.count());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.count());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countByNameWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.countByName("name"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.countByName("name"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET })
    void createWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.create(entityFactory.targetType().create().name("name")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.create(entityFactory.targetType().create()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET })
    void createCollectionWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.create(List.of(entityFactory.targetType().create().name("name"))));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createCollectionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.create(List.of(entityFactory.targetType().create())));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_TARGET })
    void deleteWithPermissionWorks() {
        assertPermissionWorks(() -> {
            targetTypeManagement.delete(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.READ_TARGET })
    void deleteWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetTypeManagement.delete(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findAllWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.findAll(PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findAllWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.findAll(PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.findByRsql(PAGE, "name==tag"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.findByRsql(PAGE, "name==tag"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByNameWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.findByName(PAGE, "name"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.findByName(PAGE, "name"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.get(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.get(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getCollectionWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.get(List.of(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getCollectionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.get(List.of(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void updateWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.update(entityFactory.targetType().update(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.READ_TARGET, SpPermission.DELETE_TARGET })
    void updateWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.update(entityFactory.targetType().update(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY })
    void assignCompatibleDistributionSetTypesWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.assignCompatibleDistributionSetTypes(1L, List.of(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void assignCompatibleDistributionSetTypesWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.assignCompatibleDistributionSetTypes(1L, List.of(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY })
    void unassignDistributionSetTypeWithPermissionWorks() {
        assertPermissionWorks(() -> targetTypeManagement.unassignDistributionSetType(1L, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void unassignDistributionSetTypeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTypeManagement.unassignDistributionSetType(1L, 1L));
    }

}

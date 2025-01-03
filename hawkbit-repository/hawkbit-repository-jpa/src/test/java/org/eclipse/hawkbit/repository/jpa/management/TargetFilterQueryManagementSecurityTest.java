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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - TargetFilterQueryManagement")
@Story("SecurityTests TargetFilterQueryManagement")
public class TargetFilterQueryManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET })
    void createWithPermissionWorks() {
        assertPermissionWorks(
                () -> targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("name").query("controllerId==id")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_TARGET })
    void deleteWithPermissionWorks() {
        assertPermissionWorks(() -> {
            targetFilterQueryManagement.delete(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.READ_REPOSITORY })
    void deleteWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetFilterQueryManagement.delete(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void verifyTargetFilterQuerySyntaxWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.verifyTargetFilterQuerySyntax("controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void verifyTargetFilterQuerySyntaxWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.verifyTargetFilterQuerySyntax("controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")

    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findAllWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.findAll(PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findAllWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.findAll(PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.count());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.count());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countByAutoAssignDistributionSetIdWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.countByAutoAssignDistributionSetId(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByAutoAssignDistributionSetIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.countByAutoAssignDistributionSetId(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByNameWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.findByName(PAGE, "filterName"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.findByName(PAGE, "filterName"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countByNameWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.countByName("filterName"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.countByName("filterName"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.findByRsql(PAGE, "name==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.findByRsql(PAGE, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByQueryWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.findByQuery(PAGE, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByQueryWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.findByQuery(PAGE, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void findByAutoAssignDistributionSetIdWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.findByAutoAssignDistributionSetId(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByAutoAssignDistributionSetIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.findByAutoAssignDistributionSetId(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void findByAutoAssignDSAndRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.findByAutoAssignDSAndRsql(PAGE, 1L, "rsqlParam"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByAutoAssignDSAndRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.findByAutoAssignDSAndRsql(PAGE, 1L, "rsqlParam"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findWithAutoAssignDSWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.findWithAutoAssignDS(PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findWithAutoAssignDSWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.findWithAutoAssignDS(PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getTargetFilterQueryByIdWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.get(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getTargetFilterQueryByIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.get(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getTargetFilterQueryByNameWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.getByName("filterName"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getTargetFilterQueryByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.getByName("filterName"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void updateWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.update(entityFactory.targetFilterQuery().update(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.READ_TARGET, SpPermission.DELETE_TARGET })
    void updateWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.update(entityFactory.targetFilterQuery().update(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void updateAutoAssignDSWithPermissionWorks() {
        assertPermissionWorks(() -> targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(1L).weight(1)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void updateAutoAssignDSWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(1L).weight(1)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void cancelAutoAssignmentForDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> {
            targetFilterQueryManagement.cancelAutoAssignmentForDistributionSet(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void cancelAutoAssignmentForDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetFilterQueryManagement.cancelAutoAssignmentForDistributionSet(1L);
            return null;
        });
    }
}

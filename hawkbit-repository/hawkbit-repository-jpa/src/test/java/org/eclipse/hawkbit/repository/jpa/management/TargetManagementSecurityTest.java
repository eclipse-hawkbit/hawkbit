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
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Slf4j
@Feature("SecurityTests - TargetManagement")
@Story("SecurityTests TargetManagement")
public class TargetManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void countByAssignedDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByAssignedDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByAssignedDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByAssignedDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countByFiltersWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByFilters(new FilterParams(null, null, null, null, null, null)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByFiltersWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByFilters(new FilterParams(null, null, null, null, null, null)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void countByInstalledDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByInstalledDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByInstalledDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByInstalledDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void existsByInstalledOrAssignedDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.existsByInstalledOrAssignedDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void existsByInstalledOrAssignedDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.existsByInstalledOrAssignedDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countByRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByRsql("controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsql("controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countByRsqlAndUpdatableWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByRsqlAndUpdatable("controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsqlAndUpdatable("controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countByRsqlAndCompatibleWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByRsqlAndCompatible("controllerId==id", 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlAndCompatibleWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsqlAndCompatible("controllerId==id", 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countByRsqlAndCompatibleAndUpdatableWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByRsqlAndCompatibleAndUpdatable("controllerId==id", 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsqlAndCompatibleAndUpdatable("controllerId==id", 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countByFailedInRolloutWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByFailedInRollout("1", 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByFailedInRolloutWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByFailedInRollout("1", 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.count());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.count());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET })
    void createWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.create(entityFactory.target().create().controllerId("controller").name("name")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.create(entityFactory.target().create().controllerId("controller").name("name")));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET })
    void createCollectionWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.create(List.of(entityFactory.target().create().controllerId("controller").name("name"))));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createCollectionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.create(List.of(entityFactory.target().create().controllerId("controller").name("name"))));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_TARGET })
    void deleteWithPermissionWorks() {
        assertPermissionWorks(() -> {
            targetManagement.delete(List.of(1L));
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.CREATE_TARGET })
    void deleteWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetManagement.delete(List.of(1L));
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_TARGET })
    void deleteByControllerIDWithPermissionWorks() {
        assertPermissionWorks(() -> {
            targetManagement.deleteByControllerID("controllerId");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.CREATE_TARGET })
    void deleteByControllerIDWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetManagement.deleteByControllerID("controllerId");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countByTargetFilterQueryWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByTargetFilterQuery(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByTargetFilterQueryWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByTargetFilterQuery(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatableWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(PAGE, 1L, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(PAGE, 1L, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void countByRsqlAndNonDSAndCompatibleAndUpdatableWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByRsqlAndNonDSAndCompatibleAndUpdatable(1L, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlAndNonDSAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsqlAndNonDSAndCompatibleAndUpdatable(1L, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT })
    void findByTargetFilterQueryAndNotInRolloutGroupsAndCompatibleAndUpdatableWithPermissionWorks() {
        assertPermissionWorks(
                () -> targetManagement.findByTargetFilterQueryAndNotInRolloutGroupsAndCompatibleAndUpdatable(PAGE, List.of(1L), "controllerId==id",
                        entityFactory.distributionSetType().create().build()));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void findByTargetFilterQueryAndNotInRolloutGroupsAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(
                () -> targetManagement.findByTargetFilterQueryAndNotInRolloutGroupsAndCompatibleAndUpdatable(PAGE, List.of(1L), "controllerId==id",
                        entityFactory.distributionSetType().create().build()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT })
    void countByActionsInRolloutGroupWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByActionsInRolloutGroup(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByActionsInRolloutGroupWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByActionsInRolloutGroup(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT })
    void countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatableWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(List.of(1L), "controllerId==id",
                entityFactory.distributionSetType().create().build()));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(List.of(1L), "controllerId==id",
                entityFactory.distributionSetType().create().build()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT })
    void findByFailedRolloutAndNotInRolloutGroupsWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByFailedRolloutAndNotInRolloutGroups(PAGE, List.of(1L), "1"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByFailedRolloutAndNotInRolloutGroupsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByFailedRolloutAndNotInRolloutGroups(PAGE, List.of(1L), "1"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT })
    void countByFailedRolloutAndNotInRolloutGroupsWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countByFailedRolloutAndNotInRolloutGroups(List.of(1L), "1"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByFailedRolloutAndNotInRolloutGroupsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByFailedRolloutAndNotInRolloutGroups(List.of(1L), "1"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByInRolloutGroupWithoutActionWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByInRolloutGroupWithoutAction(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByInRolloutGroupWithoutActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByInRolloutGroupWithoutAction(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void findByAssignedDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByAssignedDistributionSet(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByAssignedDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByAssignedDistributionSet(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void findByAssignedDistributionSetAndRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByAssignedDistributionSetAndRsql(PAGE, 1L, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByAssignedDistributionSetAndRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByAssignedDistributionSetAndRsql(PAGE, 1L, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getByControllerCollectionIDWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.getByControllerID(List.of("controllerId")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getByControllerCollectionIDWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.getByControllerID(List.of("controllerId")));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getByControllerIDWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.getByControllerID("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getByControllerIDWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.getByControllerID("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByFiltersWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByFilters(PAGE, new FilterParams(null, null, null, null, null, null)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByFiltersWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByFilters(PAGE, new FilterParams(null, null, null, null, null, null)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void findByInstalledDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByInstalledDistributionSet(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByInstalledDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByInstalledDistributionSet(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void findByInstalledDistributionSetAndRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByInstalledDistributionSetAndRsql(PAGE, 1L, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByInstalledDistributionSetAndRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByInstalledDistributionSetAndRsql(PAGE, 1L, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByUpdateStatusWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByUpdateStatus(PAGE, TargetUpdateStatus.IN_SYNC));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByUpdateStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByUpdateStatus(PAGE, TargetUpdateStatus.IN_SYNC));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findAllWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findAll(PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findAllWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findAll(PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByRsql(PAGE, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByRsql(PAGE, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByTargetFilterQueryWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByTargetFilterQuery(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByTargetFilterQueryWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByTargetFilterQuery(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByTagWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByTag(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByTag(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByRsqlAndTagWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByRsqlAndTag(PAGE, "controllerId==id", 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByRsqlAndTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByRsqlAndTag(PAGE, "controllerId==id", 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void assignTypeWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.assignType(List.of("controllerId"), 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void assignTypeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.assignType(List.of("controllerId"), 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void unassignTypeByIdWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.unassignType("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.DELETE_TARGET })
    void unassignTypeByIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.unassignType("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY })
    void assignTagWithHandlerWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.assignTag(List.of("controllerId"), 1L, strings -> {}));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void assignTagWithHandlerWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.assignTag(List.of("controllerId"), 1L, strings -> {}));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY })
    void assignTagWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.assignTag(List.of("controllerId"), 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void assignTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.assignTag(List.of("controllerId"), 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void unassignTagWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.unassignTag(List.of("controllerId"), 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void unassignTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.unassignTag(List.of("controllerId"), 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY })
    void unassignTagWithHandlerWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.unassignTag(List.of("controllerId"), 1L, strings -> {}));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void unassignTagWithHandlerWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.unassignTag(List.of("controllerId"), 1L, strings -> {}));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void unassignTypeWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.unassignType(List.of("controllerId")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void unassignTypeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.unassignType(List.of("controllerId")));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void assignTypeByIdWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.assignType("controllerId", 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void assignTypeByIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.assignType("controllerId", 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void updateWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.update(entityFactory.target().update("controllerId")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void updateWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.update(entityFactory.target().update("controllerId")));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.get(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.get(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getCollectionWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.get(List.of(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void getCollectionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.get(List.of(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getControllerAttributesWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.getControllerAttributes("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getControllerAttributesWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.getControllerAttributes("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void requestControllerAttributesWithPermissionWorks() {
        assertPermissionWorks(() -> {
            targetManagement.requestControllerAttributes("controllerId");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void requestControllerAttributesWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetManagement.requestControllerAttributes("controllerId");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void isControllerAttributesRequestedWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.isControllerAttributesRequested("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void isControllerAttributesRequestedWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.isControllerAttributesRequested("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByControllerAttributesRequestedWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findByControllerAttributesRequested(PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByControllerAttributesRequestedWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByControllerAttributesRequested(PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void existsByControllerIdWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.existsByControllerId("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void existsByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.existsByControllerId("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY })
    void isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatableWithPermissionWorks() {
        assertPermissionWorks(
                () -> targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable("controllerId", 1L, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(
                () -> targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable("controllerId", 1L, "controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getTagsByControllerIdWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.getTagsByControllerId("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getTagsByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.getTagsByControllerId("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void createMetaDataWithPermissionWorks() {
        assertPermissionWorks(
                () -> targetManagement.createMetaData("controllerId", List.of(entityFactory.generateTargetMetadata("key", "value"))));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createMetaDataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(
                () -> targetManagement.createMetaData("controllerId", List.of(entityFactory.generateTargetMetadata("key", "value"))));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void deleteMetaDataWithPermissionWorks() {
        assertPermissionWorks(() -> {
            targetManagement.deleteMetaData("controllerId", "key");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void deleteMetaDataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetManagement.deleteMetaData("controllerId", "key");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countMetaDataByControllerIdWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.countMetaDataByControllerId("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countMetaDataByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countMetaDataByControllerId("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findMetaDataByControllerIdAndRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findMetaDataByControllerIdAndRsql(PAGE, "controllerId", "controllerId==id"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findMetaDataByControllerIdAndRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findMetaDataByControllerIdAndRsql(PAGE, "controllerId", "controllerId==id"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getMetaDataByControllerIdWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.getMetaDataByControllerId("controllerId", "key"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getMetaDataByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.getMetaDataByControllerId("controllerId", "key"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findMetaDataByControllerIdWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.findMetaDataByControllerId(PAGE, "controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findMetaDataByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findMetaDataByControllerId(PAGE, "controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void updateMetadataWithPermissionWorks() {
        assertPermissionWorks(() -> targetManagement.updateMetadata("controllerId", entityFactory.generateTargetMetadata("key", "value")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void updateMetadataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(
                () -> targetManagement.updateMetadata("controllerId", entityFactory.generateTargetMetadata("key", "value")));
    }

}

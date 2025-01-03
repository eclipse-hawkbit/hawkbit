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

import jakarta.validation.ConstraintDeclarationException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.builder.DynamicRolloutGroupTemplate;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

@Slf4j
@Feature("SecurityTests - RolloutManagement")
@Story("SecurityTests RolloutManagement")
public class RolloutManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void getWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.get(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void getWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.get(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void getByNameWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.getByName("name"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void getByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.getByName("name"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void getWithDetailedStatusWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.getWithDetailedStatus(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void getWithDetailedStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.getWithDetailedStatus(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT })
    void approveOrDenyWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.approveOrDeny(1L, Rollout.ApprovalDecision.APPROVED));
        assertPermissionWorks(() -> rolloutManagement.approveOrDeny(1L, Rollout.ApprovalDecision.APPROVED, "comment"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void approveOrDenyWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.approveOrDeny(1L, Rollout.ApprovalDecision.APPROVED));
        assertInsufficientPermission(() -> rolloutManagement.approveOrDeny(1L, Rollout.ApprovalDecision.APPROVED, "comment"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.HANDLE_ROLLOUT })
    void pauseRolloutWithPermissionWorks() {
        assertPermissionWorks(() -> {
            rolloutManagement.pauseRollout(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.READ_ROLLOUT })
    void pauseRolloutWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            rolloutManagement.pauseRollout(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.HANDLE_ROLLOUT })
    void resumeRolloutWithPermissionWorks() {
        assertPermissionWorks(() -> {
            rolloutManagement.resumeRollout(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.READ_ROLLOUT })
    void resumeRolloutWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            rolloutManagement.resumeRollout(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findActiveRolloutsWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.findActiveRollouts());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void findActiveRolloutsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.findActiveRollouts());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_ROLLOUT })
    void cancelRolloutsForDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> {
            rolloutManagement.cancelRolloutsForDistributionSet(entityFactory.distributionSet().create().build());
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.READ_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void cancelRolloutsForDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            rolloutManagement.cancelRolloutsForDistributionSet(entityFactory.distributionSet().create().build());
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void countWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.count());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void countWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.count());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void countByDistributionSetIdAndRolloutIsStoppableWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.countByDistributionSetIdAndRolloutIsStoppable(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void countByDistributionSetIdAndRolloutIsStoppableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.countByDistributionSetIdAndRolloutIsStoppable(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void countByFiltersWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.countByFilters("searchFilter"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void countByFiltersWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.countByFilters("searchFilter"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.READ_REPOSITORY })
    void createWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.create(entityFactory.rollout().create().distributionSetId(1L), 1, false,
                new RolloutGroupConditionBuilder().withDefaults().build()));
        assertPermissionWorks(() -> rolloutManagement.create(entityFactory.rollout().create().distributionSetId(1L), 1, false,
                new RolloutGroupConditionBuilder().withDefaults().build(), DynamicRolloutGroupTemplate.builder().build()));
        assertPermissionWorks(
                () -> rolloutManagement.create(entityFactory.rollout().create().distributionSetId(1L),
                        List.of(entityFactory.rolloutGroup().create()),
                        new RolloutGroupConditionBuilder().withDefaults().build()));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT, SpPermission.READ_REPOSITORY })
    void createWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.create(entityFactory.rollout().create().distributionSetId(1L), 1, false,
                new RolloutGroupConditionBuilder().withDefaults().build()));
        assertInsufficientPermission(() -> rolloutManagement.create(entityFactory.rollout().create().distributionSetId(1L), 1, false,
                new RolloutGroupConditionBuilder().withDefaults().build(), DynamicRolloutGroupTemplate.builder().build()));
        assertInsufficientPermission(
                () -> rolloutManagement.create(entityFactory.rollout().create().distributionSetId(1L),
                        List.of(entityFactory.rolloutGroup().create()),
                        new RolloutGroupConditionBuilder().withDefaults().build()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findAllWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.findAll(PAGE, false));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void findAllWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.findAll(PAGE, false));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findByRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.findByRsql(PAGE, "id==1", false));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void findByRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.findByRsql(PAGE, "rsql", false));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findAllWithDetailedStatusWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.findAllWithDetailedStatus(PAGE, false));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void findAllWithDetailedStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.findAllWithDetailedStatus(PAGE, false));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findByFiltersWithDetailedStatusWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.findByFiltersWithDetailedStatus(PAGE, "searchFilter", false));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void findByFiltersWithDetailedStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.findByFiltersWithDetailedStatus(PAGE, "searchFilter", false));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_ROLLOUT, SpPermission.READ_REPOSITORY })
    void setRolloutStatusDetailsWithPermissionWorks() {
        assertPermissionWorks(() -> {
            rolloutManagement.setRolloutStatusDetails(new PageImpl<>(List.of(entityFactory.rollout().create().distributionSetId(1L).build())));
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT })
    void setRolloutStatusDetailsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            rolloutManagement.setRolloutStatusDetails(new PageImpl<>(List.of(entityFactory.rollout().create().distributionSetId(1L).build())));
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.HANDLE_ROLLOUT })
    void startWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.start(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.READ_ROLLOUT })
    void startWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.start(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_ROLLOUT })
    void updateWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutManagement.update(entityFactory.rollout().update(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.HANDLE_ROLLOUT })
    void updateWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutManagement.update(entityFactory.rollout().update(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_ROLLOUT })
    void deleteWithPermissionWorks() {
        assertPermissionWorks(() -> {
            rolloutManagement.delete(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.READ_ROLLOUT, SpPermission.UPDATE_ROLLOUT,
            SpPermission.HANDLE_ROLLOUT })
    void deleteWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            rolloutManagement.delete(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_ROLLOUT })
    void triggerNextGroupWithPermissionWorks() {
        assertPermissionWorks(() -> {
            rolloutManagement.triggerNextGroup(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.READ_REPOSITORY,
            SpPermission.HANDLE_ROLLOUT })
    void triggerNextGroupWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            rolloutManagement.triggerNextGroup(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.CREATE_ROLLOUT, SpPermission.READ_ROLLOUT,
            SpPermission.READ_TARGET })
    void validateTargetsInGroupsWithPermissionWorks() {
        try {
            assertPermissionWorks(
                    () -> rolloutManagement.validateTargetsInGroups(List.of(entityFactory.rolloutGroup().create()), "name==dummy", 1L, 1L));
        } catch (Error e) {
            if (e.getCause() instanceof ConstraintDeclarationException) {
                log.info("ConstraintDeclarationException thrown expected");
            } else {
                throw e;
            }
        }
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_ROLLOUT, SpPermission.READ_TARGET })
    void validateTargetsInGroupsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(
                () -> rolloutManagement.validateTargetsInGroups(List.of(entityFactory.rolloutGroup().create()), "filter", 1L, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findByRolloutAndRsqlWithDetailedStatusWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(1L, "name==*", PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void findByRolloutAndRsqlWithDetailedStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(1L, "name==*", PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findByRolloutWithDetailedStatusWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(1L, PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void findByRolloutWithDetailedStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(1L, PAGE));
    }
}

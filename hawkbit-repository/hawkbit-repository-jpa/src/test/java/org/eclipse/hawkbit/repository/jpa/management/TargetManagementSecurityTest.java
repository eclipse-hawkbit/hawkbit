package org.eclipse.hawkbit.repository.jpa.management;

import java.util.List;

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
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByAssignedDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByAssignedDistributionSet(1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByFiltersWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByFilters(new FilterParams(null, null, null, null, null, null)));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByInstalledDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByInstalledDistributionSet(1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void existsByInstalledOrAssignedDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.existsByInstalledOrAssignedDistributionSet(1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsql("rsql"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsqlAndUpdatable("rsql"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlAndCompatibleWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsqlAndCompatible("rsql", 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsqlAndCompatibleAndUpdatable("rsql", 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByFailedInRolloutWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByFailedInRollout("rolloutId", 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.count());
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.create(entityFactory.target().create()));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createCollectionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.create(List.of(entityFactory.target().create())));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.CREATE_TARGET })
    void deleteWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetManagement.delete(List.of(1L));
            return null;
        });
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.CREATE_TARGET })
    void deleteByControllerIDWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetManagement.deleteByControllerID("controllerId");
            return null;
        });
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByTargetFilterQueryWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByTargetFilterQuery(1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(PAGE, 1L, "rsql"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlAndNonDSAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsqlAndNonDSAndCompatibleAndUpdatable(1L, "rsql"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void findByTargetFilterQueryAndNotInRolloutGroupsAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(
                () -> targetManagement.findByTargetFilterQueryAndNotInRolloutGroupsAndCompatibleAndUpdatable(PAGE, List.of(1L), "rsql",
                        entityFactory.distributionSetType().create().build()));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByActionsInRolloutGroupWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByActionsInRolloutGroup(1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(List.of(1L), "rsql",
                entityFactory.distributionSetType().create().build()));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByFailedRolloutAndNotInRolloutGroupsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByFailedRolloutAndNotInRolloutGroups(PAGE, List.of(1L), "rolloutId"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countByFailedRolloutAndNotInRolloutGroupsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countByFailedRolloutAndNotInRolloutGroups(List.of(1L), "rolloutId"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByInRolloutGroupWithoutActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByInRolloutGroupWithoutAction(PAGE, 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByAssignedDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByAssignedDistributionSet(PAGE, 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByAssignedDistributionSetAndRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByAssignedDistributionSetAndRsql(PAGE, 1L, "rsql"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getByControllerCollectionIDWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.getByControllerID(List.of("controllerId")));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getByControllerIDWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.getByControllerID("controllerId"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByFiltersWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByFilters(PAGE, new FilterParams(null, null, null, null, null, null)));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByInstalledDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByInstalledDistributionSet(PAGE, 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByInstalledDistributionSetAndRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByInstalledDistributionSetAndRsql(PAGE, 1L, "rsql"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByUpdateStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByUpdateStatus(PAGE, TargetUpdateStatus.IN_SYNC));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findAllWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findAll(PAGE));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByRsql(PAGE, "rsql"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByTargetFilterQueryWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByTargetFilterQuery(PAGE, 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByTag(PAGE, 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByRsqlAndTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByRsqlAndTag(PAGE, "rsql", 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void assignTypeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.assignType(List.of("controllerId"), 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.DELETE_TARGET })
    void unassignTypeByIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.unassignType("controllerId"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void assignTagWithHandlerWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.assignTag(List.of("controllerId"), 1L, strings -> {}));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void assignTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.assignTag(List.of("controllerId"), 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void unassignTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.unassignTag(List.of("controllerId"), 1L));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void unassignTagWithHandlerWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.unassignTag(List.of("controllerId"), 1L, strings -> {}));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void unassignTypeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.unassignType(List.of("controllerId")));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void assignTypeByIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.assignType("controllerId", 1L));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void updateWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.update(entityFactory.target().update("controllerId")));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.get(1L));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void getCollectionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.get(List.of(1L)));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void getControllerAttributesWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.getControllerAttributes("controllerId"));
    }


    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET })
    void requestControllerAttributesWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetManagement.requestControllerAttributes("controllerId");
            return null;
        });
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void isControllerAttributesRequestedWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.isControllerAttributesRequested("controllerId"));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByControllerAttributesRequestedWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findByControllerAttributesRequested(PAGE));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void existsByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.existsByControllerId("controllerId"));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatableWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable("controllerId", 1L, "rsql"));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getTagsByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.getTagsByControllerId("controllerId"));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createMetaDataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.createMetaData("controllerId", List.of(entityFactory.generateTargetMetadata("key", "value"))));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void deleteMetaDataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetManagement.deleteMetaData("controllerId", "key");
            return null;
        });
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countMetaDataByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.countMetaDataByControllerId("controllerId"));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findMetaDataByControllerIdAndRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findMetaDataByControllerIdAndRsql(PAGE, "controllerId", "rsql"));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getMetaDataByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.getMetaDataByControllerId("controllerId","key"));
    }
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findMetaDataByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.findMetaDataByControllerId(PAGE, "controllerId"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void updateMetadataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetManagement.updateMetadata("controllerId", entityFactory.generateTargetMetadata("key", "value")));
    }

}

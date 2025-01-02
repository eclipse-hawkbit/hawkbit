package org.eclipse.hawkbit.repository.jpa.management;

import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - DistributionSetManagement")
@Story("SecurityTests DistributionSetManagement")
class DistributionSetManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<DistributionSet, DistributionSetCreate, DistributionSetUpdate> {

    @Override
    protected RepositoryManagement<DistributionSet, DistributionSetCreate, DistributionSetUpdate> getRepositoryManagement() {
        return distributionSetManagement;
    }

    @Override
    protected DistributionSetCreate getCreateObject() {
        return entityFactory.distributionSet().create().name("name").version("1.0.0").type("type");
    }

    @Override
    protected DistributionSetUpdate getUpdateObject() {
        return entityFactory.distributionSet().update(0L).name("a new name")
                .description("a new description").version("a new version").requiredMigrationStep(true);
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    public void assignSoftwareModulesWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.assignSoftwareModules(1L, List.of(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    public void assignSoftwareModulesWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.assignSoftwareModules(1L, List.of(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void assignTagWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.assignTag(List.of(1L), 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void assignTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.assignTag(List.of(1L), 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void unassignTagWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.unassignTag(List.of(1L), 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void unassignTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.unassignTag(List.of(1L), 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void createMetaDataWithPermissionWorks() {
        assertPermissionWorks(
                () -> distributionSetManagement.createMetaData(1L, List.of(entityFactory.generateTargetMetadata("key", "value"))));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void createMetaDataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.createMetaData(1L, List.of()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void deleteMetaDataWithPermissionWorks() {
        assertPermissionWorks(() -> {
            distributionSetManagement.deleteMetaData(1L, "key");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void deleteMetaDataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            distributionSetManagement.deleteMetaData(1L, "key");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void lockWithPermissionWorks() {
        assertPermissionWorks(() -> {
            distributionSetManagement.lock(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void lockWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            distributionSetManagement.lock(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void unlockWithPermissionWorks() {
        assertPermissionWorks(() -> {
            distributionSetManagement.unlock(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void unlockWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            distributionSetManagement.unlock(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getByActionWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.getByAction(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void getByActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.getByAction(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getWithDetailsWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.getWithDetails(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void getWithDetailsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.getWithDetails(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getByNameAndVersionWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.getByNameAndVersion("name", "version"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void getByNameAndVersionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.getByNameAndVersion("name", "version"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getValidAndCompleteWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.getValidAndComplete(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void getValidAndCompleteWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.getValidAndComplete(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getValidWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.getValid(1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void getValidWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.getValid(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getOrElseThrowExceptionWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.getOrElseThrowException(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void getOrElseThrowExceptionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.getOrElseThrowException(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findMetaDataByDistributionSetIdWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.findMetaDataByDistributionSetId(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findMetaDataByDistributionSetIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.findMetaDataByDistributionSetId(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countMetaDataByDistributionSetIdWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.countMetaDataByDistributionSetId(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void countMetaDataByDistributionSetIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.countMetaDataByDistributionSetId(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findMetaDataByDistributionSetIdAndRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.findMetaDataByDistributionSetIdAndRsql(PAGE, 1L, "rsql"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findMetaDataByDistributionSetIdAndRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.findMetaDataByDistributionSetIdAndRsql(PAGE, 1L, "rsql"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findByCompletedWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.findByCompleted(PAGE, true));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findByCompletedWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.findByCompleted(PAGE, true));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countByCompletedWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.countByCompleted(true));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void countByCompletedWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.countByCompleted(true));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findByDistributionSetFilterWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.findByDistributionSetFilter(PAGE, DistributionSetFilter.builder().build()));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findByDistributionSetFilterWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(
                () -> distributionSetManagement.findByDistributionSetFilter(PAGE, DistributionSetFilter.builder().build()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countByDistributionSetFilterWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.countByDistributionSetFilter(DistributionSetFilter.builder().build()));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void countByDistributionSetFilterWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.countByDistributionSetFilter(null));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findByTagWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.findByTag(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findByTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.findByTag(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findByRsqlAndTagWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.findByRsqlAndTag(PAGE, "rsql", 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findByRsqlAndTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.findByRsqlAndTag(null, "rsql", 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getMetaDataByDistributionSetIdWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.getMetaDataByDistributionSetId(1L, "key"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void getMetaDataByDistributionSetIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.getMetaDataByDistributionSetId(1L, "key"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void isInUseWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.isInUse(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void isInUseWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.isInUse(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void unassignSoftwareModuleWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.unassignSoftwareModule(1L, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void unassignSoftwareModuleWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.unassignSoftwareModule(1L, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")

    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void updateMetaDataWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.updateMetaData(1L, entityFactory.generateDsMetadata("key", "value")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void updateMetaDataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.updateMetaData(1L, null));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")

    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countByTypeIdWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.countByTypeId(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void countByTypeIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.countByTypeId(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")

    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countRolloutsByStatusForDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.countRolloutsByStatusForDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void countRolloutsByStatusForDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.countRolloutsByStatusForDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")

    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countActionsByStatusForDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.countActionsByStatusForDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void countActionsByStatusForDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.countActionsByStatusForDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")

    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countAutoAssignmentsForDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetManagement.countAutoAssignmentsForDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void countAutoAssignmentsForDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetManagement.countAutoAssignmentsForDistributionSet(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")

    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void invalidateWithPermissionWorks() {
        assertPermissionWorks(() -> {
            distributionSetManagement.invalidate(entityFactory.distributionSet().create().name("name").version("1.0").type("type").build());
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void invalidateWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            distributionSetManagement.invalidate(null);
            return null;
        });
    }

}
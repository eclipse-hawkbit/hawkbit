package org.eclipse.hawkbit.repository.jpa.management;

import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - SoftwareManagement")
@Story("SecurityTests SoftwareManagement")
public class SoftwareManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<SoftwareModule, SoftwareModuleCreate, SoftwareModuleUpdate> {

    @Override
    protected RepositoryManagement<SoftwareModule, SoftwareModuleCreate, SoftwareModuleUpdate> getRepositoryManagement() {
        return softwareModuleManagement;
    }

    @Override
    protected SoftwareModuleCreate getCreateObject() {
        return entityFactory.softwareModule().create().name("name").version("version").type("type");
    }

    @Override
    protected SoftwareModuleUpdate getUpdateObject() {
        return entityFactory.softwareModule().update(1L).locked(true);
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void createMetaDataWithPermissionWorks() {
        assertPermissionWorks(
                () -> softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(1L).key("key").value("value")));
        assertPermissionWorks(() -> softwareModuleManagement.createMetaData(
                List.of(entityFactory.softwareModuleMetadata().create(1L).key("key").value("value"))));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_ROLLOUT, SpPermission.READ_REPOSITORY, SpPermission.DELETE_REPOSITORY })
    void createMetaDataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(
                () -> softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(1L).key("key").value("value")));
        assertInsufficientPermission(() -> softwareModuleManagement.createMetaData(
                List.of(entityFactory.softwareModuleMetadata().create(1L).key("key").value("value"))));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void deleteMetaDataWithPermissionWorks() {
        assertPermissionWorks(() -> {
            softwareModuleManagement.deleteMetaData(1L, "key");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.READ_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void deleteMetaDataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            softwareModuleManagement.deleteMetaData(1L, "key");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findByAssignedToWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.findByAssignedTo(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findByAssignedToWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.findByAssignedTo(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countByAssignedToWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.countByAssignedTo(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void countByAssignedToWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.countByAssignedTo(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findByTextAndTypeWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.findByTextAndType(PAGE, "text", 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findByTextAndTypeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.findByTextAndType(PAGE, "text", 1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getByNameAndVersionAndTypeWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.getByNameAndVersionAndType("name", "version", 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void getByNameAndVersionAndTypeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.getByNameAndVersionAndType("name", "version", 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getMetaDataBySoftwareModuleIdWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.getMetaDataBySoftwareModuleId(1L, "key"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void getMetaDataBySoftwareModuleIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.getMetaDataBySoftwareModuleId(1L, "key"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findMetaDataBySoftwareModuleIdWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.findMetaDataBySoftwareModuleId(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findMetaDataBySoftwareModuleIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.findMetaDataBySoftwareModuleId(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void countMetaDataBySoftwareModuleIdWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.countMetaDataBySoftwareModuleId(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void countMetaDataBySoftwareModuleIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.countMetaDataBySoftwareModuleId(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findMetaDataBySoftwareModuleIdAndTargetVisibleWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.findMetaDataBySoftwareModuleIdAndTargetVisible(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findMetaDataBySoftwareModuleIdAndTargetVisibleWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.findMetaDataBySoftwareModuleIdAndTargetVisible(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findMetaDataByRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.findMetaDataByRsql(PAGE, 1L, "key==value"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findMetaDataByRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.findMetaDataByRsql(PAGE, 1L, "key==value"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findByTypeWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.findByType(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findByTypeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.findByType(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void lockWithPermissionWorks() {
        assertPermissionWorks(() -> {
            softwareModuleManagement.lock(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.READ_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void lockWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            softwareModuleManagement.lock(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void unlockWithPermissionWorks() {
        assertPermissionWorks(() -> {
            softwareModuleManagement.unlock(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.READ_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void unlockWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            softwareModuleManagement.unlock(1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void updateMetaDataWithPermissionWorks() {
        assertPermissionWorks(
                () -> softwareModuleManagement.updateMetaData(entityFactory.softwareModuleMetadata().update(1L, "key").value("value")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.READ_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void updateMetaDataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(
                () -> softwareModuleManagement.updateMetaData(entityFactory.softwareModuleMetadata().update(1L, "key").value("value")));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findMetaDataBySoftwareModuleIdsAndTargetVisibleWithPermissionWorks() {
        assertPermissionWorks(() -> softwareModuleManagement.findMetaDataBySoftwareModuleIdsAndTargetVisible(List.of(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.UPDATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findMetaDataBySoftwareModuleIdsAndTargetVisibleWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> softwareModuleManagement.findMetaDataBySoftwareModuleIdsAndTargetVisible(List.of(1L)));
    }
}

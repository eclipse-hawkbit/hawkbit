package org.eclipse.hawkbit.repository.jpa.management;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Slf4j
@Feature("SecurityTests - SystemManagement")
@Story("SecurityTests SystemManagement")
public class SystemManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SYSTEM_ADMIN })
    void findTenantsPermissionWorks() {
        assertPermissionWorks(() -> systemManagement.findTenants(PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void findTenantsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> systemManagement.findTenants(PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SYSTEM_ADMIN })
    void deleteTenantWithPermissionWorks() {
        assertPermissionWorks(() -> {
            systemManagement.deleteTenant("tenant");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void deleteTenantWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            systemManagement.deleteTenant("tenant");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.SYSTEM_ROLE })
    void forEachTenantTenantWithPermissionWorks() {
        assertPermissionWorks(() -> {
            systemManagement.forEachTenant(log::info);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void forEachTenantTenantWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            systemManagement.forEachTenant(log::info);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SYSTEM_ADMIN })
    void getSystemUsageStatisticsWithTenantsWithPermissionWorks() {
        assertPermissionWorks(() -> systemManagement.getSystemUsageStatisticsWithTenants());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void getSystemUsageStatisticsWithTenantsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> systemManagement.getSystemUsageStatisticsWithTenants());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SYSTEM_ADMIN })
    void getSystemUsageStatisticsWithPermissionWorks() {
        assertPermissionWorks(() -> systemManagement.getSystemUsageStatistics());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void getSystemUsageStatisticsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> systemManagement.getSystemUsageStatistics());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getTenantMetadataWithPermissionWorks() {
        assertPermissionWorks(() -> systemManagement.getTenantMetadata());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getTenantMetadataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> systemManagement.getTenantMetadata());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.SYSTEM_ROLE })
    void getTenantMetadataByTenantWithPermissionWorks() {
        assertPermissionWorks(() -> systemManagement.getTenantMetadata(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void getTenantMetadataByTenantWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> systemManagement.getTenantMetadata(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.SYSTEM_ROLE })
    void createTenantMetadataWithPermissionWorks() {
        assertPermissionWorks(() -> systemManagement.createTenantMetadata("tenant"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void createTenantMetadataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> systemManagement.createTenantMetadata("tenant"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.TENANT_CONFIGURATION })
    void updateTenantMetadataWithPermissionWorks() {
        assertPermissionWorks(() -> systemManagement.updateTenantMetadata(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    void updateTenantMetadataWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> systemManagement.updateTenantMetadata(1L));
    }
}

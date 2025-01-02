package org.eclipse.hawkbit.repository.jpa.management;

import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Slf4j
@Feature("SecurityTests - TargetManagement")
@Story("SecurityTests TargetManagement")
public class TenantConfigurationManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.TENANT_CONFIGURATION })
    void addOrUpdateConfigurationWithPermissionWorks() {
        assertPermissionWorks(() -> tenantConfigurationManagement.addOrUpdateConfiguration("authentication.header.enabled", true));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void addOrUpdateConfigurationWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> tenantConfigurationManagement.addOrUpdateConfiguration("authentication.header.enabled", true));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.TENANT_CONFIGURATION })
    void addOrUpdateConfigurationWithMapWithPermissionWorks() {
        assertPermissionWorks(() -> tenantConfigurationManagement.addOrUpdateConfiguration(Map.of("authentication.header.enabled", true)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void addOrUpdateConfigurationWithMapWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> tenantConfigurationManagement.addOrUpdateConfiguration(Map.of("authentication.header.enabled", true)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.TENANT_CONFIGURATION })
    void deleteConfigurationWithPermissionWorks() {
        assertPermissionWorks(() -> {
            tenantConfigurationManagement.deleteConfiguration("authentication.header.enabled");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void deleteConfigurationWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            tenantConfigurationManagement.deleteConfiguration("key");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TENANT_CONFIGURATION })
    void getConfigurationValueWithPermissionWorks() {
        assertPermissionWorks(() -> tenantConfigurationManagement.getConfigurationValue("authentication.header.enabled"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getConfigurationValueWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> tenantConfigurationManagement.getConfigurationValue("authentication.header.enabled"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TENANT_CONFIGURATION })
    void getConfigurationValueWithTypeWithPermissionWorks() {
        assertPermissionWorks(() -> tenantConfigurationManagement.getConfigurationValue("authentication.header.enabled", Boolean.class));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getConfigurationValueWithTypeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> tenantConfigurationManagement.getConfigurationValue("key", Boolean.class));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TENANT_CONFIGURATION })
    void getGlobalConfigurationValueWithPermissionWorks() {
        assertPermissionWorks(() -> tenantConfigurationManagement.getGlobalConfigurationValue("authentication.header.enabled", Boolean.class));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getGlobalConfigurationValueWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> tenantConfigurationManagement.getGlobalConfigurationValue("authentication.header.enabled", Boolean.class));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void pollStatusResolverWithPermissionWorks() {
        assertPermissionWorks(() -> tenantConfigurationManagement.pollStatusResolver());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void pollStatusResolverWithoutPermissionThrowsAccessDenied() {
        Assertions.setMaxStackTraceElementsDisplayed(5000);
        assertInsufficientPermission(() -> tenantConfigurationManagement.pollStatusResolver());
    }
}

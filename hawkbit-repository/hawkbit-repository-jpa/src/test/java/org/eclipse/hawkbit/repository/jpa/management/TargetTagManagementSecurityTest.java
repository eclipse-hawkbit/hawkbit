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
@Feature("SecurityTests - TargetTagManagement")
@Story("SecurityTests TargetTagManagement")
public class TargetTagManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countWithPermissionWorks() {
        assertPermissionWorks(() -> targetTagManagement.count());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void countWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTagManagement.count());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET })
    void createWithPermissionWorks() {
        assertPermissionWorks(() -> targetTagManagement.create(entityFactory.tag().create().name("name")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTagManagement.create(entityFactory.tag().create()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET })
    void createCollectionWithPermissionWorks() {
        assertPermissionWorks(() -> targetTagManagement.create(List.of(entityFactory.tag().create().name("name"))));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void createCollectionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTagManagement.create(List.of(entityFactory.tag().create())));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_TARGET })
    void deleteWithPermissionWorks() {
        assertPermissionWorks(() -> {
            targetTagManagement.delete("tag");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.UPDATE_TARGET, SpPermission.CREATE_TARGET })
    void deleteWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            targetTagManagement.delete("tag");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findAllWithPermissionWorks() {
        assertPermissionWorks(() -> targetTagManagement.findAll(PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findAllWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTagManagement.findAll(PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findByRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> targetTagManagement.findByRsql(PAGE, "name==tag"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void findByRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTagManagement.findByRsql(PAGE, "name==tag"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getByNameWithPermissionWorks() {
        assertPermissionWorks(() -> targetTagManagement.getByName("tag"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTagManagement.getByName("tag"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getWithPermissionWorks() {
        assertPermissionWorks(() -> targetTagManagement.get(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTagManagement.get(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getCollectionWithPermissionWorks() {
        assertPermissionWorks(() -> targetTagManagement.get(List.of(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.UPDATE_TARGET, SpPermission.DELETE_TARGET })
    void getCollectionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTagManagement.get(List.of(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void updateWithPermissionWorks() {
        assertPermissionWorks(() -> targetTagManagement.update(entityFactory.tag().update(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.READ_TARGET, SpPermission.DELETE_TARGET })
    void updateWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> targetTagManagement.update(entityFactory.tag().update(1L)));
    }
}

package org.eclipse.hawkbit.repository.jpa.management;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

@Feature("SecurityTests - DistributionSetTagManagement")
@Story("SecurityTests DistributionSetTagManagement")
public class DistributionSetTagManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<DistributionSetTag, TagCreate, TagUpdate> {

    @Override
    protected RepositoryManagement<DistributionSetTag, TagCreate, TagUpdate> getRepositoryManagement() {
        return distributionSetTagManagement;
    }

    @Override
    protected TagCreate getCreateObject() {
        return entityFactory.tag().create().name("tag");
    }

    @Override
    protected TagUpdate getUpdateObject() {
        return entityFactory.tag().update(1L).name("tag");
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void getByNameWitPermissionWorks() {
        assertPermissionWorks(() -> distributionSetTagManagement.getByName("tagName"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void getByNameWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetTagManagement.getByName("tagName"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void findByDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> distributionSetTagManagement.findByDistributionSet(Pageable.unpaged(), 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.DELETE_REPOSITORY })
    void findByDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> distributionSetTagManagement.findByDistributionSet(Pageable.unpaged(), 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.DELETE_REPOSITORY })
    void deleteDistributionSetTagWithPermissionWorks() {
        assertPermissionWorks(() -> {
            distributionSetTagManagement.delete("tagName");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void deleteDistributionSetTagWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            distributionSetTagManagement.delete("tagName");
            return null;
        });
    }
}

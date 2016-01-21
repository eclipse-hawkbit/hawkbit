/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.MockMvcResultPrinter;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 *
 *
 */
@Features("Component Tests - System Management RESTful API")
@Stories("System Management Resource")
public class SystemManagementResourceTest extends AbstractIntegrationTest {

    @Test
    @WithUser(tenantId = "mytenant", allSpPermissions = true)
    @Description("Tests that a tenant can be deletd by API.")
    public void deleteTenant() throws Exception {

        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        assertThat(distributionSetManagement.findDistributionSetById(dsA.getId())).isNotNull();

        mvc.perform(delete("/system/admin/tenants/mytenant")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(distributionSetManagement.findDistributionSetById(dsA.getId())).isNull();

    }

    @Test
    @WithUser(tenantId = "mytenant", authorities = { SpPermission.DELETE_TARGET, SpPermission.DELETE_REPOSITORY,
            SpPermission.CREATE_REPOSITORY, SpPermission.READ_REPOSITORY })
    @Description("Tenant deletion is only possible for SYSTEM_ADMINs. Repository or target delete is not sufficient.")
    public void deleteTenantFailsMissingPermission() throws Exception {

        final DistributionSet dsA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        assertThat(distributionSetManagement.findDistributionSetById(dsA.getId())).isNotNull();

        mvc.perform(delete("/system/admin/tenants/mytenant")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        assertThat(distributionSetManagement.findDistributionSetById(dsA.getId())).isNotNull();
    }

    @Test
    public void getCachesReturnStatus200() throws Exception {
        mvc.perform(get("/system/admin/caches")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    @Test
    public void invalidateCachesReturnStatus200() throws Exception {
        mvc.perform(delete("/system/admin/caches")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

}

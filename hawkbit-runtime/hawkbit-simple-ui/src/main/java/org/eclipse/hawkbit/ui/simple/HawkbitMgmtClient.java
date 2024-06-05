/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple;

import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import feign.FeignException;
import lombok.Getter;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTenantManagementRestApi;
import org.springframework.http.ResponseEntity;
import java.util.function.Supplier;

@Getter
public class HawkbitMgmtClient {

    private final Tenant tenant;
    private final HawkbitClient hawkbitClient;

    private final MgmtSoftwareModuleRestApi softwareModuleRestApi;
    private final MgmtSoftwareModuleTypeRestApi softwareModuleTypeRestApi;
    private final MgmtDistributionSetRestApi distributionSetRestApi;
    private final MgmtDistributionSetTypeRestApi distributionSetTypeRestApi;
    private final MgmtDistributionSetTagRestApi distributionSetTagRestApi;
    private final MgmtTargetRestApi targetRestApi;
    private final MgmtTargetTypeRestApi targetTypeRestApi;
    private final MgmtTargetTagRestApi targetTagRestApi;
    private final MgmtTargetFilterQueryRestApi targetFilterQueryRestApi;
    private final MgmtRolloutRestApi rolloutRestApi;
    private final MgmtTenantManagementRestApi tenantManagementRestApi;

    HawkbitMgmtClient(final Tenant tenant, final HawkbitClient hawkbitClient) {
        this.tenant = tenant;
        this.hawkbitClient = hawkbitClient;

        softwareModuleRestApi = service(MgmtSoftwareModuleRestApi .class);
        softwareModuleTypeRestApi = service(MgmtSoftwareModuleTypeRestApi.class);
        distributionSetRestApi = service(MgmtDistributionSetRestApi.class);
        distributionSetTypeRestApi = service(MgmtDistributionSetTypeRestApi.class);
        distributionSetTagRestApi = service(MgmtDistributionSetTagRestApi.class);
        targetRestApi = service(MgmtTargetRestApi.class);
        targetTypeRestApi = service(MgmtTargetTypeRestApi.class);
        targetTagRestApi = service(MgmtTargetTagRestApi.class);
        targetFilterQueryRestApi = service(MgmtTargetFilterQueryRestApi.class);
        rolloutRestApi = service(MgmtRolloutRestApi.class);
        tenantManagementRestApi = service(MgmtTenantManagementRestApi.class);
    }

    boolean hasSoftwareModulesRead() {
        return hasRead(() -> softwareModuleRestApi.getSoftwareModule(-1L));
    }

    boolean hasRolloutRead() {
        return hasRead(() -> rolloutRestApi.getRollout(-1L));
    }

    boolean hasDistributionSetRead() {
        return hasRead(() -> distributionSetRestApi.getDistributionSet(-1L));
    }

    boolean hasTargetRead() {
        return hasRead(() -> targetRestApi.getTarget("_#ETE$ER"));
    }

    private boolean hasRead(final Supplier<ResponseEntity<?>> doCall) {
        try {
            final int statusCode = doCall.get().getStatusCode().value();
            return statusCode != 401 && statusCode != 403;
        } catch (final FeignException e) {
            return !(e instanceof FeignException.Unauthorized) && !(e instanceof FeignException.Forbidden);
        }
    }

    private <T> T service(final Class<T> serviceType) {
        return hawkbitClient.mgmtService(serviceType, tenant);
    }
}

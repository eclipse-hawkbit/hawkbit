/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.auth.MgmtUserInfo;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtBasicAuthRestApi;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling basic auth validation.
 */
@RestController
public class MgmtBasicAuthResource implements MgmtBasicAuthRestApi {

    private final TenantAware tenantAware;

    public MgmtBasicAuthResource(final TenantAware tenantAware) {
        this.tenantAware = tenantAware;
    }

    @Override
    @AuditLog(entity = "BasicAuth", message = "Validate Basic Auth")
    public ResponseEntity<MgmtUserInfo> validateBasicAuth() {
        final MgmtUserInfo userInfo = new MgmtUserInfo();
        userInfo.setUsername(tenantAware.getCurrentUsername());
        userInfo.setTenant(tenantAware.getCurrentTenant());
        return ResponseEntity.ok(userInfo);
    }
}
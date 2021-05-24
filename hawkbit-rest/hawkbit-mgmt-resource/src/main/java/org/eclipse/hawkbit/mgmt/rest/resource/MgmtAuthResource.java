/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import org.eclipse.hawkbit.im.authentication.UserPrincipal;
import org.eclipse.hawkbit.mgmt.json.model.auth.MgmtUserInfo;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtAuthRestApi;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling basic auth validation.
 */
@RestController
public class MgmtAuthResource implements MgmtAuthRestApi {
    @Override
    public ResponseEntity<MgmtUserInfo> validateBasicAuth() {
        MgmtUserInfo userInfo = new MgmtUserInfo();
        String username = "";
        String tenant = "";
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
           username =  ((UserPrincipal)principal).getUsername();
           tenant = ((UserPrincipal) principal).getTenant();
        } else {
            username = principal.toString();
        }
        userInfo.setUsername(username);
        userInfo.setTenant(tenant);
        return ResponseEntity.ok(userInfo);
    }
}

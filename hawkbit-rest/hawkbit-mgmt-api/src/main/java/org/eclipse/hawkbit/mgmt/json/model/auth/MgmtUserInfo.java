/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.auth;

/**
 * A json annotated rest model for Userinfo to RESTful API representation.
 *
 */
public class MgmtUserInfo {

    private String username;
    private String tenant;

    /**
     * @return Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username Username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return Tenant
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * @param tenant Tenant
     */
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

}
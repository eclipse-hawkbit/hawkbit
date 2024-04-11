/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@ConfigurationProperties("hawkbit.tenant")
@Data
public class Tenant {

    // id of the tenant
    @NonNull
    private String tenantId = "DEFAULT";

    // basic auth user, to access management api
    @Nullable
    private String username = "admin";
    @ToString.Exclude
    @Nullable
    private String password = "admin";

    // gateway token
    @Nullable
    private String gatewayToken;

    // amqp settings (if DMF is used)
    @Nullable
    private DMF dmf;

    private boolean downloadAuthenticationEnabled = true;

    @Data
    @ToString
    public static class DMF {

        @Nullable
        private String virtualHost;
        @Nullable
        private String username;
        @Nullable
        @ToString.Exclude
        private String password;
    }
}

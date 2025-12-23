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

import java.security.cert.X509Certificate;

import lombok.Data;
import lombok.ToString;
import org.eclipse.hawkbit.sdk.ca.CA;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@ConfigurationProperties("hawkbit.tenant")
@Data
public class Tenant {

    // id of the tenant
    @NonNull
    private String tenantId = "DEFAULT";

    // basic authentication user, to access management api
    @Nullable
    private String username = "admin";
    @ToString.Exclude
    @Nullable
    private String password = "admin";

    // gateway token
    @Nullable
    private String gatewayToken;
    // gateway token
    @Nullable
    private String[] certificateFingerprints;

    // the tenant DDI / Mgmt server certificates CA - it shall be trusted by controllers connecting via HTTPS
    @Nullable
    private X509Certificate[] tenantCA;
    // Certificate Authority for the tenant that is used to sign the target certificates. It shall be trusted by the DDI server
    @Nullable
    private CA ddiCA;

    // amqp settings (if DMF is used)
    @Nullable
    private DMF dmf;

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

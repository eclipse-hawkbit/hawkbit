/**
 * Copyright (c) 2019 devolo AG and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for hawkBit static users.
 */
@Data
@ToString
@ConfigurationProperties("hawkbit.security")
public class TenantAwareUserProperties {

    private Map<String, User> user = new HashMap<>();

    @Data
    @ToString
    public static class User {

        private String tenant;
        @ToString.Exclude
        private String password;
        private List<String> roles = new ArrayList<>();
        private List<String> permissions = new ArrayList<>();
    }
}
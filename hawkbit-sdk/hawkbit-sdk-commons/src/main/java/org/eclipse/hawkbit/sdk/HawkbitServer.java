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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.jspecify.annotations.NonNull;

@ConfigurationProperties(prefix = "hawkbit.server")
@Data
public class HawkbitServer {

    @NonNull
    private String mgmtUrl = "http://localhost:8080";
    @NonNull
    private String ddiUrl = "http://localhost:8081";
}
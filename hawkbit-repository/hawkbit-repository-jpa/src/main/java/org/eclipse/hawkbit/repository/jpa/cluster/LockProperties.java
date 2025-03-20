/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.cluster;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "hawkbit.repository.cluster.lock")
@Validated
public class LockProperties {

    private int ttl = 5 * 60_000; // 5 minutes

    // when less than that time (in milliseconds) remains to lock expiration a refresh is triggered
    private int refreshOnRemainMS = 4 * 60_000; // refresh after a minute, 4 minutes before expiration
    // when less than that time (in percent of expiration) remains to lock expiration a refresh is triggered
    private int refreshOnRemainPercent = 80; // refresh after a minute, 4 minutes before expiration
}
/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.throttle;

import java.io.Serial;

/**
 * Thrown by {@link TenantThrottle#acquire} when a tenant is over its fair share under contention
 * and no slot became available within the requested timeout. Mapped to HTTP 429 at the REST layer.
 */
public class ThrottledException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ThrottledException(final String message) {
        super(message);
    }
}

/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.io.Serial;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Thrown when a soft deleted tenant is accessed in its own context. Extends {@link EntityNotFoundException}, so callers not
 * aware of soft delete keep the not found semantics.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
public class TenantSoftDeletedException extends EntityNotFoundException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String tenant;

    public TenantSoftDeletedException(final String tenant) {
        super("Tenant {" + tenant + "} is soft deleted.");
        this.tenant = tenant;
    }
}

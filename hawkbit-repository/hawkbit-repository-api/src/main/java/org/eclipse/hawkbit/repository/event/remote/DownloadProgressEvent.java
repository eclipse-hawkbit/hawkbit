/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.ActionStatus;

/**
 * TenantAwareEvent that contains an updated download progress for a given
 * ActionStatus that was written for a download request.
 */
@NoArgsConstructor(force = true) // for serialization libs like jackson
@Data
@EqualsAndHashCode(callSuper = true)
public class DownloadProgressEvent extends RemoteTenantAwareEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long shippedBytesSinceLast;

    /**
     * Constructor.
     *
     * @param tenant the tenant
     * @param actionStatusId of the {@link ActionStatus} the download belongs to
     * @param shippedBytesSinceLast the shippedBytesSinceLast
     */
    public DownloadProgressEvent(final String tenant, final Long actionStatusId, final long shippedBytesSinceLast) {
        super(tenant, actionStatusId);
        this.shippedBytesSinceLast = shippedBytesSinceLast;
    }
}
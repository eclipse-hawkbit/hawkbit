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
import java.util.UUID;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;

/**
 * A distributed tenant aware event. It's the base class of the other
 * distributed events. All the necessary information of distributing events to
 * other nodes.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true) // for serialization libs like jackson
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RemoteTenantAwareEvent extends AbstractRemoteEvent implements TenantAwareEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String tenant;

    /**
     * Constructor.
     *
     * @param source the for the remote event.
     * @param tenant the tenant
     */
    public RemoteTenantAwareEvent(final String tenant, final Object source) {
        super(source == null ? UUID.randomUUID() : source);
        this.tenant = tenant;
    }
}
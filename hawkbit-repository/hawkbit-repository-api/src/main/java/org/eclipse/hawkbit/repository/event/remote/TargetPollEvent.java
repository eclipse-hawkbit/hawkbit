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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Event is sent in case a target polls either through DDI or DMF.
 */
@NoArgsConstructor // for serialization libs like jackson
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TargetPollEvent extends RemoteTenantAwareEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private String controllerId;
    private String targetAddress;

    public TargetPollEvent(final String controllerId, final String tenant) {
        super(tenant, controllerId);
        this.controllerId = controllerId;
    }

    public TargetPollEvent(final Target target) {
        this(target.getControllerId(), target.getTenant());
        this.targetAddress = target.getAddress();
    }
}
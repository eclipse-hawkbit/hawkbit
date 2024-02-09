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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.Target;

import java.io.Serial;

/**
 * Event is send in case a target polls either through DDI or DMF.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TargetPollEvent extends RemoteTenantAwareEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private String controllerId;
    private String targetAdress;

    /**
     * Default constructor.
     */
    public TargetPollEvent() {
        // for serialization libs like jackson
    }

    public TargetPollEvent(final String controllerId, final String tenant, final String applicationId) {
        super(controllerId, tenant, applicationId);
        this.controllerId = controllerId;
    }

    public TargetPollEvent(final Target target, final String applicationId) {
        super(target.getControllerId(), target.getTenant(), applicationId);
        this.controllerId = target.getControllerId();
        this.targetAdress = target.getAddress().toString();
    }
}
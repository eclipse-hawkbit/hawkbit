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
import java.util.Collections;
import java.util.List;

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

    private String targetAddress;
    private List<String> controllerIds;

    public TargetPollEvent(final List<String> controllerIds,final String tenant) {
        super(tenant, tenant); // source is tenant
        this.controllerIds = Collections.unmodifiableList(controllerIds);
    }

    public TargetPollEvent(final String controllerId, final String tenant) {
        this(List.of(controllerId), tenant);
    }

    public TargetPollEvent(final Target target) {
        this(List.of(target.getControllerId()), target.getTenant());
        this.targetAddress = target.getAddress();
    }
}
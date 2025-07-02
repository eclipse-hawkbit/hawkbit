/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;
import java.util.Collection;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Event that is published when a rollout is stopped due to invalidation of a
 * {@link DistributionSet}.
 */
@NoArgsConstructor // for serialization libs like jackson
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RolloutStoppedEvent extends RemoteTenantAwareEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private Collection<Long> rolloutGroupIds;
    private long rolloutId;

    public RolloutStoppedEvent(final String tenant, final long rolloutId, final Collection<Long> rolloutGroupIds) {
        super(tenant, rolloutId);
        this.rolloutId = rolloutId;
        this.rolloutGroupIds = rolloutGroupIds;
    }
}
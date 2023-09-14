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

import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

import com.cronutils.utils.StringUtils;

/**
 * A distributed tenant aware event. It's the base class of the other
 * distributed events. All the necessary information of distributing events to
 * other nodes.
 *
 */
public class RemoteTenantAwareEvent extends RemoteApplicationEvent implements TenantAwareEvent {
    private static final long serialVersionUID = 1L;

    private String tenant;

    /**
     * Default constructor.
     */
    protected RemoteTenantAwareEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param source
     *            the for the remote event.
     * @param tenant
     *            the tenant
     * @param applicationId
     *            the applicationId
     */
    public RemoteTenantAwareEvent(final Object source, final String tenant, final String applicationId) {
        // due to a bug in Spring Cloud, we cannot pass null for applicationId
        super(source, applicationId != null ? applicationId : StringUtils.EMPTY);
        this.tenant = tenant;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

}

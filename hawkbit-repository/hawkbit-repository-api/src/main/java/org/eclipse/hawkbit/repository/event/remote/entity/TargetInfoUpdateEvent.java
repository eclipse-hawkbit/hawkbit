/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;

/**
 * TenantAwareEvent for update the targets info.
 */
public class TargetInfoUpdateEvent extends RemoteEntityEvent<Target> {

    private static final long serialVersionUID = 1L;

    private transient TargetInfo targetInfo;

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param entityClass
     *            the entity entityClassName
     * @param applicationId
     *            the origin application id
     */

    protected TargetInfoUpdateEvent(final String tenant, final Long entityId, final String entityClass,
            final String applicationId) {
        super(tenant, entityId, entityClass, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param targetInfo
     *            the target info
     * @param applicationId
     *            the origin application id
     */
    public TargetInfoUpdateEvent(final TargetInfo targetInfo, final String applicationId) {
        super(targetInfo.getTarget(), applicationId);
        this.targetInfo = targetInfo;
    }

    public TargetInfo getTargetInfo() {
        if (targetInfo == null && getEntity() != null) {
            targetInfo = getEntity().getTargetInfo();
        }
        return targetInfo;
    }

}

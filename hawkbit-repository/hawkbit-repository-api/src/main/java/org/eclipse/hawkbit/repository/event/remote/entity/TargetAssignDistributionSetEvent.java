/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TenantAwareEvent that gets sent when a distribution set gets assigned to a
 * target.
 *
 */
public class TargetAssignDistributionSetEvent extends RemoteEntityEvent<Action> {

    private static final long serialVersionUID = 1L;

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
    @JsonCreator
    public TargetAssignDistributionSetEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entityId") final Long entityId,
            @JsonProperty("entityClass") final Class<? extends Action> entityClass,
            @JsonProperty("originService") final String applicationId) {
        super(tenant, entityId, entityClass, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            the target
     * @param applicationId
     *            the origin application id
     */
    public TargetAssignDistributionSetEvent(final Action baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

    /**
     * @return the {@link Target} which has been assigned to the distribution
     *         set
     */
    @JsonIgnore
    public Target getTarget() {
        return getEntity().getTarget();
    }

    /**
     * TODO: korrekt?
     * 
     * @return the software modules which have been assigned to the target
     */
    @JsonIgnore
    public Collection<SoftwareModule> getSoftwareModules() {
        final DistributionSet assignedDistributionSet = getEntity().getDistributionSet();
        if (assignedDistributionSet == null) {
            return Collections.emptyList();
        }
        return assignedDistributionSet.getModules();
    }
}
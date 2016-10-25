/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * The {@link Target} is the target of all provisioning operations. It contains
 * the currently installed {@link DistributionSet} (i.e. current state). In
 * addition it holds the target {@link DistributionSet} that has to be
 * provisioned next (i.e. target state).
 * </p>
 */
public interface Target extends NamedEntity {

    /**
     * @return currently assigned {@link DistributionSet}.
     */
    DistributionSet getAssignedDistributionSet();

    /**
     * @return business identifier of the {@link Target}
     */
    String getControllerId();

    /**
     * @return immutable set of assigned {@link TargetTag}s.
     */
    Set<TargetTag> getTags();

    /**
     * @return immutable {@link Action} history of the {@link Target}.
     */
    List<Action> getActions();

    /**
     * @return {@link TargetIdName} view of the {@link Target}.
     */
    default TargetIdName getTargetIdName() {
        return new TargetIdName(getId(), getControllerId(), getName());
    }

    /**
     * @return the targetInfo object
     */
    TargetInfo getTargetInfo();

    /**
     * @return the securityToken
     */
    String getSecurityToken();

}

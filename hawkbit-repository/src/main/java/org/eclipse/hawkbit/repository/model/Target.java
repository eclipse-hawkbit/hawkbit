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

public interface Target extends NamedEntity {

    DistributionSet getAssignedDistributionSet();

    String getControllerId();

    Set<TargetTag> getTags();

    void setAssignedDistributionSet(DistributionSet assignedDistributionSet);

    void setControllerId(String controllerId);

    void setTags(Set<TargetTag> tags);

    List<Action> getActions();

    TargetIdName getTargetIdName();

    /**
     * @return the targetInfo
     */
    TargetInfo getTargetInfo();

    /**
     * @param targetInfo
     *            the targetInfo to set
     */
    void setTargetInfo(TargetInfo targetInfo);

    /**
     * @return the securityToken
     */
    String getSecurityToken();

    /**
     * @param securityToken
     *            the securityToken to set
     */
    void setSecurityToken(String securityToken);

}
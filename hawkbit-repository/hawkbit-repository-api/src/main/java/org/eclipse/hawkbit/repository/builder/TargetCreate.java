/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Builder to create a new {@link Target} entry. Defines all fields that can be
 * set at creation time. Other fields are set by the repository automatically,
 * e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface TargetCreate {

    /**
     * @param controllerId
     *            for {@link Target#getControllerId()}
     * @return updated builder instance
     */
    TargetCreate controllerId(@NotEmpty String controllerId);

    /**
     * @param name
     *            for {@link Target#getName()}
     * @return updated builder instance
     */
    TargetCreate name(@NotEmpty String name);

    /**
     * @param description
     *            for {@link Target#getDescription()}
     * @return updated builder instance
     */
    TargetCreate description(String description);

    /**
     * @param securityToken
     *            for {@link Target#getSecurityToken()}
     * @return updated builder instance
     */
    TargetCreate securityToken(String securityToken);

    /**
     * @param address
     *            for {@link Target#getAddress()}
     *
     * @throws IllegalArgumentException
     *             If the given string violates RFC&nbsp;2396
     * 
     * @return updated builder instance
     */
    TargetCreate address(String address);

    /**
     * @param lastTargetQuery
     *            for {@link Target#getLastTargetQuery()}
     * @return updated builder instance
     */
    TargetCreate lastTargetQuery(Long lastTargetQuery);

    /**
     * @param status
     *            for {@link Target#getUpdateStatus()}
     * @return updated builder instance
     */
    TargetCreate status(TargetUpdateStatus status);

    /**
     * @return peek on current state of {@link Target} in the builder
     */
    Target build();

}

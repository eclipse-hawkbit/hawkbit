/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Builder to update an existing {@link Target} entry. Defines all fields that
 * can be updated.
 *
 */
public interface TargetUpdate {

    /**
     * @param name
     *            for {@link Target#getName()}
     * @return updated builder instance
     */
    TargetUpdate name(@NotEmpty String name);

    /**
     * @param description
     *            for {@link Target#getDescription()}
     * @return updated builder instance
     */
    TargetUpdate description(String description);

    /**
     * @param securityToken
     *            for {@link Target#getSecurityToken()}
     * @return updated builder instance
     */
    TargetUpdate securityToken(@NotEmpty String securityToken);

    /**
     * @param address
     *            for {@link Target#getAddress()}
     *
     * @throws IllegalArgumentException
     *             If the given string violates RFC&nbsp;2396
     * 
     * @return updated builder instance
     */
    TargetUpdate address(String address);

    /**
     * @param lastTargetQuery
     *            for {@link Target#getLastTargetQuery()}
     * @return updated builder instance
     */
    TargetUpdate lastTargetQuery(Long lastTargetQuery);

    /**
     * @param status
     *            for {@link Target#getUpdateStatus()}
     * @return updated builder instance
     */
    TargetUpdate status(TargetUpdateStatus status);
}

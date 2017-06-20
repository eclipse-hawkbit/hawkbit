/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

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
    TargetUpdate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description
     *            for {@link Target#getDescription()}
     * @return updated builder instance
     */
    TargetUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param securityToken
     *            for {@link Target#getSecurityToken()}
     * @return updated builder instance
     */
    TargetUpdate securityToken(@Size(min = 1, max = Target.SECURITY_TOKEN_MAX_SIZE) @NotNull String securityToken);

    /**
     * @param address
     *            for {@link Target#getAddress()}
     *
     * @throws IllegalArgumentException
     *             If the given string violates RFC&nbsp;2396
     * 
     * @return updated builder instance
     */
    TargetUpdate address(@Size(max = Target.ADDRESS_MAX_SIZE) String address);

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
    TargetUpdate status(@NotNull TargetUpdateStatus status);
}

/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import jakarta.annotation.Nullable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Update implementation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(fluent = true)
public class GenericDistributionSetUpdate extends AbstractDistributionSetUpdateCreate<DistributionSetUpdate> implements DistributionSetUpdate {

    @Nullable
    protected Boolean locked;

    public GenericDistributionSetUpdate(final Long id) {
        super.id = id;
    }
}
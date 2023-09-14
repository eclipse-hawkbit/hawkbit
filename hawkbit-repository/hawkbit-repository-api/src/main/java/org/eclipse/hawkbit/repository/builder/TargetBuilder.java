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

import javax.validation.constraints.NotEmpty;

import org.eclipse.hawkbit.repository.model.Target;

/**
 * Builder for {@link Target}.
 *
 */
public interface TargetBuilder {

    /**
     * @param controllerId
     *            of the updatable entity
     * @return builder instance
     */
    TargetUpdate update(@NotEmpty String controllerId);

    /**
     * @return builder instance
     */
    TargetCreate create();
}

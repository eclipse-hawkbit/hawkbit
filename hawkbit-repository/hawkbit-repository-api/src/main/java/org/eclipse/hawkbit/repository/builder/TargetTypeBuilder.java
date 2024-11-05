/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * Builder for {@link TargetType}.
 */
public interface TargetTypeBuilder {

    /**
     * @param id of the updatable entity
     * @return builder instance
     */
    TargetTypeUpdate update(long id);

    /**
     * @return builder instance
     */
    TargetTypeCreate create();
}

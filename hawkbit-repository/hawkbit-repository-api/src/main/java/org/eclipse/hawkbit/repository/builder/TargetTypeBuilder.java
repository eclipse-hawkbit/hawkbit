/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * Builder for {@link TargetType}.
 *
 */
public interface TargetTypeBuilder {
    /**
     * @param id
     *            of the updatable entity
     * @return builder instance
     */
    TargetTypeUpdate update(long id);

    /**
     * @return builder instance
     */
    TargetTypeCreate create();
}

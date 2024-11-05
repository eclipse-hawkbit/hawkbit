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

import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Builder for {@link SoftwareModule}.
 */
public interface SoftwareModuleBuilder {

    /**
     * @param id of the updatable entity
     * @return builder instance
     */
    SoftwareModuleUpdate update(long id);

    /**
     * @return builder instance
     */
    SoftwareModuleCreate create();
}
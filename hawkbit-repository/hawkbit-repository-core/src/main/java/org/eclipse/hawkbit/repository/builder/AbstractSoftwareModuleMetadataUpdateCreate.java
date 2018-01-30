/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractSoftwareModuleMetadataUpdateCreate<T> extends AbstractMetadataUpdateCreate<T> {
    protected Boolean targetVisible;
    protected long softwareModuleId;

    public T softwareModuleId(final long softwareModuleId) {
        this.softwareModuleId = softwareModuleId;
        return (T) this;
    }

    public long getSoftwareModuleId() {
        return softwareModuleId;
    }

    public Optional<Boolean> isTargetVisible() {
        return Optional.ofNullable(targetVisible);
    }

    public T targetVisible(final Boolean targetVisible) {
        this.targetVisible = targetVisible;
        return (T) this;
    }

}

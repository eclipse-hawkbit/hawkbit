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

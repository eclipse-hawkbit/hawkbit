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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Type;

/**
 * Builder to create a new {@link SoftwareModule} entry. Defines all fields that
 * can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 */
public interface SoftwareModuleCreate<T extends SoftwareModule> extends RepositoryManagement.Builder<T> {

    /**
     * @param name for {@link SoftwareModule#getName()}
     * @return updated builder instance
     */
    SoftwareModuleCreate<T> name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param version for {@link SoftwareModule#getVersion()}
     * @return updated builder instance
     */
    SoftwareModuleCreate<T> version(@Size(min = 1, max = NamedVersionedEntity.VERSION_MAX_SIZE) @NotNull String version);

    /**
     * @param description for {@link SoftwareModule#getDescription()}
     * @return updated builder instance
     */
    SoftwareModuleCreate<T> description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param vendor for {@link SoftwareModule#getVendor()}
     * @return updated builder instance
     */
    SoftwareModuleCreate<T> vendor(@Size(max = SoftwareModule.VENDOR_MAX_SIZE) String vendor);

    /**
     * @param typeKey for {@link SoftwareModule#getType()}
     * @return updated builder instance
     */
    SoftwareModuleCreate<T> type(@Size(min = 1, max = Type.KEY_MAX_SIZE) @NotNull String typeKey);

    /**
     * @param type for {@link SoftwareModule#getType()}
     * @return updated builder instance
     */
    default SoftwareModuleCreate<T> type(@NotNull final SoftwareModuleType type) {
        return type(type.getKey());
    }

    /**
     * @param encrypted if should be encrypted
     * @return updated builder instance
     */
    SoftwareModuleCreate<T> encrypted(boolean encrypted);

    /**
     * @return peek on current state of {@link SoftwareModule} in the builder
     */
    T build();
}
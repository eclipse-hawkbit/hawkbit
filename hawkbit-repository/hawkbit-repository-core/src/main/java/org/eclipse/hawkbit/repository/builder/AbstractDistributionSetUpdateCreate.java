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

import java.util.Collection;
import java.util.Optional;

import lombok.Getter;
import org.eclipse.hawkbit.repository.ValidString;

/**
 * Create and update builder DTO.
 *
 * @param <T> update or create builder interface
 */
public abstract class AbstractDistributionSetUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {

    @ValidString
    protected String version;
    protected Boolean requiredMigrationStep;
    @Getter
    protected Collection<Long> modules;

    public T modules(final Collection<Long> modules) {
        this.modules = modules;
        return (T) this;
    }

    public T requiredMigrationStep(final Boolean requiredMigrationStep) {
        this.requiredMigrationStep = requiredMigrationStep;
        return (T) this;
    }

    public Boolean isRequiredMigrationStep() {
        return requiredMigrationStep;
    }

    public T version(final String version) {
        this.version = strip(version);
        return (T) this;
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }
}
/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Collection;
import java.util.Optional;

import org.eclipse.hawkbit.repository.ValidString;
import org.springframework.util.StringUtils;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractDistributionSetUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {
    @ValidString
    protected String version;
    protected Boolean requiredMigrationStep;

    @ValidString
    protected String type;
    protected Collection<Long> modules;

    public T modules(final Collection<Long> modules) {
        this.modules = modules;
        return (T) this;
    }

    public Collection<Long> getModules() {
        return modules;
    }

    public T type(final String type) {
        this.type = StringUtils.trimWhitespace(type);
        return (T) this;
    }

    public String getType() {
        return type;
    }

    public T requiredMigrationStep(final Boolean requiredMigrationStep) {
        this.requiredMigrationStep = requiredMigrationStep;
        return (T) this;
    }

    public Boolean isRequiredMigrationStep() {
        return requiredMigrationStep;
    }

    public T version(final String version) {
        this.version = StringUtils.trimWhitespace(version);
        return (T) this;
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

}

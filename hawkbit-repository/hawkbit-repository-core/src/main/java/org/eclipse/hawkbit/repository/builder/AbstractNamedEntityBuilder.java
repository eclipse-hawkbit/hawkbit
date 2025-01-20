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

import org.eclipse.hawkbit.repository.ValidString;

public abstract class AbstractNamedEntityBuilder<T> extends AbstractBaseEntityBuilder {

    @ValidString
    protected String name;
    @ValidString
    protected String description;

    public T name(final String name) {
        this.name = AbstractBaseEntityBuilder.strip(name);
        return (T) this;
    }

    public T description(final String description) {
        this.description = AbstractBaseEntityBuilder.strip(description);
        return (T) this;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }
}
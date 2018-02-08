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

import org.eclipse.hawkbit.repository.ValidString;
import org.springframework.util.StringUtils;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractTargetFilterQueryUpdateCreate<T> extends AbstractBaseEntityBuilder {
    @ValidString
    protected String name;

    @ValidString
    protected String query;

    protected Long set;

    public T set(final long set) {
        this.set = set;
        return (T) this;
    }

    public Optional<Long> getSet() {
        return Optional.ofNullable(set);
    }

    public T name(final String name) {
        this.name = StringUtils.trimWhitespace(name);
        return (T) this;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public T query(final String query) {
        this.query = StringUtils.trimWhitespace(query);
        return (T) this;
    }

    public Optional<String> getQuery() {
        return Optional.ofNullable(query);
    }
}

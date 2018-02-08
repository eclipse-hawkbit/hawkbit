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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractDistributionSetTypeUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {
    @ValidString
    protected String colour;
    @ValidString
    protected String key;

    protected Collection<Long> mandatory;
    protected Collection<Long> optional;

    public T mandatory(final Collection<Long> mandatory) {
        this.mandatory = mandatory;
        return (T) this;
    }

    public T optional(final Collection<Long> optional) {
        this.optional = optional;
        return (T) this;
    }

    public Optional<Collection<Long>> getMandatory() {
        if (CollectionUtils.isEmpty(mandatory)) {
            return Optional.empty();
        }

        return Optional.ofNullable(mandatory);
    }

    public Optional<Collection<Long>> getOptional() {
        if (CollectionUtils.isEmpty(optional)) {
            return Optional.empty();
        }

        return Optional.ofNullable(optional);
    }

    public T colour(final String colour) {
        this.colour = StringUtils.trimWhitespace(colour);
        return (T) this;
    }

    public Optional<String> getColour() {
        return Optional.ofNullable(colour);
    }

    public T key(final String key) {
        this.key = StringUtils.trimWhitespace(key);
        return (T) this;
    }

    public Optional<String> getKey() {
        return Optional.ofNullable(key);
    }

}
